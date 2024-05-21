/*
 *    LeveragingBag.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.classifiers.meta.minibatch;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.Capabilities;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifierMiniBatch;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.options.ClassOption;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Leveraging Bagging for evolving data streams using ADWIN. Leveraging Bagging
 * and Leveraging Bagging MC using Random Output Codes ( -o option).
 *
 * <p>See details in:<br /> Albert Bifet, Geoffrey Holmes, Bernhard Pfahringer.
 * Leveraging Bagging for Evolving Data Streams Machine Learning and Knowledge
 * Discovery in Databases, European Conference, ECML PKDD}, 2010.</p>
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class LeveragingBagMB extends AbstractClassifierMiniBatch implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Leveraging Bagging for evolving data streams using ADWIN.";
    }

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "trees.HoeffdingTree");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models in the bag.", 10, 1, Integer.MAX_VALUE);

    public FloatOption weightShrinkOption = new FloatOption("weightShrink", 'w',
            "The number to use to compute the weight of new instances.", 6, 0.0, Float.MAX_VALUE);

    public FloatOption deltaAdwinOption = new FloatOption("deltaAdwin", 'a',
            "Delta of Adwin change detection", 0.002, 0.0, 1.0);

    // Leveraging Bagging MC: uses this option to use Output Codes
    public FlagOption outputCodesOption = new FlagOption("outputCodes", 'o',
            "Use Output Codes to use binary classifiers.");

    public MultiChoiceOption leveraginBagAlgorithmOption = new MultiChoiceOption(
            "leveraginBagAlgorithm", 'm', "Leveraging Bagging to use.", new String[]{
                "LeveragingBag", "LeveragingBagME", "LeveragingBagHalf", "LeveragingBagWT", "LeveragingSubag"},
            new String[]{"Leveraging Bagging for evolving data streams using ADWIN",
                "Leveraging Bagging ME using weight 1 if misclassified, otherwise error/(1-error)",
                "Leveraging Bagging Half using resampling without replacement half of the instances",
                "Leveraging Bagging WT without taking out all instances.",
                "Leveraging Subagging using resampling without replacement."
            }, 0);

    protected ArrayList<TrainingRunnable> trainers;

    protected int numberOfChangesDetected;

    protected int[][] matrixCodes;

    protected boolean initMatrixCodes = false;

    protected boolean _Change;

    @Override
    public void resetLearningImpl() {
        this.trainers = new ArrayList<>();
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        this.numberOfChangesDetected = 0;
        boolean ocos = this.outputCodesOption.isSet();
        double wso = this.weightShrinkOption.getValue();
        int lao = this.leveraginBagAlgorithmOption.getChosenIndex();
        if (ocos) {
            this.initMatrixCodes = true;
        }
        int seed = this.randomSeedOption.getValue();
        for (int i = 0; i < this.ensembleSizeOption.getValue(); i++) {
            this.trainers.add(new TrainingRunnable(baseLearner.copy(), new ADWIN((double) this.deltaAdwinOption.getValue()), ocos, wso, lao, seed));
        }
    }

    @Override
    public void trainOnInstances(ArrayList<Instance> instances) {
        for (int i = 0; i < instances.size(); i++) {
            Instance inst = instances.get(i).copy();
            int numClasses = inst.numClasses();
            if (this.initMatrixCodes) {
                this.matrixCodes = new int[this.trainers.size()][inst.numClasses()];
                for (int j = 0; j < this.trainers.size(); j++) {
                    int numberOnes;
                    int numberZeros;

                    do { // until we have the same number of zeros and ones
                        numberOnes = 0;
                        numberZeros = 0;
                        for (int k = 0; k < numClasses; k++) {
                            int result = 0;
                            if (k == 1 && numClasses == 2) {
                                result = 1 - this.matrixCodes[j][0];
                            } else {
                                result = (this.classifierRandom.nextBoolean() ? 1 : 0);
                            }
                            this.matrixCodes[j][k] = result;
                            if (result == 1) {
                                numberOnes++;
                            } else {
                                numberZeros++;
                            }
                        }
                    } while ((numberOnes - numberZeros) * (numberOnes - numberZeros) > (this.trainers.size() % 2));
                }
            }
        }

        for (TrainingRunnable t : trainers)
            t.instances = new ArrayList<>(instances);
        if (this.threadpool != null) {
            try {
                this.threadpool.invokeAll(trainers);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Could not call invokeAll() on training threads.");
            }
        }

        for (TrainingRunnable l : this.trainers)
            l.instances = new ArrayList<>(instances);
        if (this.threadpool != null) {
            try {
                this.threadpool.invokeAll(this.trainers);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Could not call invokeAll() on training threads.");
            }
        }
        if (_Change) {
            numberOfChangesDetected++;
            double max = 0.0;
            int imax = -1;
            for (int i = 0; i < this.trainers.size(); i++) {
                if (max < this.trainers.get(i).ADError.getEstimation()) {
                    max = this.trainers.get(i).ADError.getEstimation();
                    imax = i;
                }
            }
            if (imax != -1) {
                this.trainers.get(imax).learner.resetLearning();
                this.trainers.get(imax).ADError = new ADWIN((double) this.deltaAdwinOption.getValue());
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (this.outputCodesOption.isSet()) {
            return getVotesForInstanceBinary(inst);
        }
        DoubleVector combinedVote = new DoubleVector();
        for (TrainingRunnable trainer : this.trainers) {
            DoubleVector vote = new DoubleVector(trainer.learner.getVotesForInstance(inst));
            if (vote.sumOfValues() > 0.0) {
                vote.normalize();
                combinedVote.addValues(vote);
            }
        }
        return combinedVote.getArrayRef();
    }

    public double[] getVotesForInstanceBinary(Instance inst) {
        double[] combinedVote = new double[(int) inst.numClasses()];
        Instance weightedInst = (Instance) inst.copy();
        if (!this.initMatrixCodes) {
            for (int i = 0; i < this.trainers.size(); i++) {
                //Replace class by OC
                weightedInst.setClassValue((double) this.matrixCodes[i][(int) inst.classValue()]);

                double[] vote;
                vote = this.trainers.get(i).learner.getVotesForInstance(weightedInst);
                //Binary Case
                int voteClass = 0;
                if (vote.length == 2) {
                    voteClass = (vote[1] > vote[0] ? 1 : 0);
                }
                //Update votes
                for (int j = 0; j < inst.numClasses(); j++) {
                    if (this.matrixCodes[i][j] == voteClass) {
                        combinedVote[j] += 1;
                    }
                }
            }
        }
        return combinedVote;
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{new Measurement("ensemble size",
                    this.trainers != null ? this.trainers.size() : 0),
                    new Measurement("change detections", this.numberOfChangesDetected)
                };
    }

    @Override
    public Classifier[] getSubClassifiers() {
        Classifier[] ensemble = new Classifier[this.trainers.size()];
        for (int i = 0; i < this.trainers.size(); i++) {
            ensemble[i] = trainers.get(i).learner;
        }
        return ensemble.clone();
    }

    @Override
    public Capabilities getCapabilities() {
        return super.getCapabilities();
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == LeveragingBagMB.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }

    /***
     * Inner class to assist with the multi-thread execution.
     */
    protected class TrainingRunnable implements Runnable, Callable<Integer> {
        private Classifier learner;
        private ArrayList<Instance> instances;
        protected int LevAlgOption;
        protected double w;
        protected ADWIN ADError;
        protected boolean outputCodesOptionIsSet;
        protected int[] matrixCodes;
        private int localSeed;
        private Random trRandom;

        public TrainingRunnable(Classifier learner, ADWIN ADError, boolean ocos, double wso, int lao, int seed) {
            this.learner = learner;
            this.ADError = ADError;
            this.outputCodesOptionIsSet = ocos;
            this.w = wso;
            this.LevAlgOption = lao;
            this.localSeed = seed;
            this.trRandom = new Random();
            this.trRandom.setSeed(this.localSeed);
        }

        @Override
        public void run() {
            for (int i = 0; i < this.instances.size(); i++) {
                double k = 0.0;
                switch (this.LevAlgOption) {
                    case 0: //LBagMC
                        k = MiscUtils.poisson(w, this.trRandom);
                        break;
                    case 1: //LeveragingBagME
                        double error = this.ADError.getEstimation();
                        k = !this.learner.correctlyClassifies(instances.get(i).copy()) ?
                                1.0 : (this.trRandom.nextDouble() < (error / (1.0 - error)) ? 1.0 : 0.0);
                        break;
                    case 2: //LeveragingBagHalf
                        w = 1.0;
                        k = this.trRandom.nextBoolean() ? 0.0 : w;
                        break;
                    case 3: //LeveragingBagWT
                        w = 1.0;
                        k = 1.0 + MiscUtils.poisson(w, this.trRandom);
                        break;
                    case 4: //LeveragingSubag
                        w = 1.0;
                        k = MiscUtils.poisson(1, this.trRandom);
                        k = (k > 0) ? w : 0;
                        break;
                }
                Instance weightedInst = this.instances.get(i).copy();
                if (this.outputCodesOptionIsSet) {
                    weightedInst.setClassValue((double) this.matrixCodes[(int) weightedInst.classValue()]);
                }
                weightedInst.setWeight(this.instances.get(i).weight() * k);
                this.learner.trainOnInstance(weightedInst);
                boolean correctlyClassifies = this.learner.correctlyClassifies(this.instances.get(i));
                double ErrEstim = this.ADError.getEstimation();
                if (this.ADError.setInput(correctlyClassifies ? 0 : 1)) {
                    if (this.ADError.getEstimation() > ErrEstim) {
                        _Change = true;
                    }
                }
            }
        }

        @Override
        public Integer call() {
            run();
            return 0;
        }
    }
}

