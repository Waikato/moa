/*
 *    OzaBag.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifierMiniBatch;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.options.ClassOption;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Incremental on-line bagging of Oza and Russell.
 *
 * <p>Oza and Russell developed online versions of bagging and boosting for
 * Data Streams. They show how the process of sampling bootstrap replicates
 * from training data can be simulated in a data stream context. They observe
 * that the probability that any individual example will be chosen for a
 * replicate tends to a Poisson(1) distribution.</p>
 *
 * <p>[OR] N. Oza and S. Russell. Online bagging and boosting.
 * In Artiﬁcial Intelligence and Statistics 2001, pages 105–112.
 * Morgan Kaufmann, 2001.</p>
 *
 * <p>Parameters:</p> <ul>
 * <li>-l : Classiﬁer to train</li>
 * <li>-s : The number of models in the bag</li> </ul>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class OzaBagMB extends AbstractClassifierMiniBatch implements MultiClassClassifier {

    @Override
    public String getPurposeString() {
        return "Incremental on-line bagging of Oza and Russell using parallelism.";
    }
        
    private static final long serialVersionUID = 2L;

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "trees.HoeffdingTree");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models in the bag.", 10, 1, Integer.MAX_VALUE);

    protected ArrayList<TrainingRunnable> trainers;

    @Override
    public void resetLearningImpl() {
        this.trainers = new ArrayList<>();
        int seed = this.randomSeedOption.getValue();
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        for (int i = 0; i < this.ensembleSizeOption.getValue(); i++) {
            trainers.add(new TrainingRunnable(baseLearner.copy(), seed));
            seed++;
        }
    }

    @Override
    public void trainOnInstances(ArrayList<Instance> instances) {
        for (TrainingRunnable t : trainers)
            t.instances = new ArrayList<>(instances);
        if (this.threadpool != null) {
            try {
                this.threadpool.invokeAll(trainers);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Could not call invokeAll() on training threads.");
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
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
                    this.trainers != null ? this.trainers.size() : 0)};
    }

    @Override
    public Classifier[] getSubClassifiers() {
        Classifier[] ensemble = new Classifier[this.ensembleSizeOption.getValue()];
        for (int i = 0; i < this.trainers.size(); i++) {
            ensemble[i] = this.trainers.get(i).learner;
        }
        return ensemble.clone();
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == OzaBagMB.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }

    /***
     * Inner class to assist with the multi-thread execution.
     */

    protected class TrainingRunnable implements Runnable, Callable<Integer> {
        // TODO: Fix bug that makes seed initialized random objects not give the same result in MOA
        private Classifier learner;
        private ArrayList<Instance> instances;
        private Random trRandom;
        public int localSeed;

        public TrainingRunnable(Classifier learner, int seed) {
            this.learner = learner;
            this.instances = new ArrayList<>();
            this.localSeed = seed;
            this.trRandom = new Random();
            this.trRandom.setSeed(this.localSeed);
        }

        @Override
        public void run() {
            for (Instance inst : this.instances) {
                int k = MiscUtils.poisson(1.0, this.trRandom);
                Instance weightedInst = inst.copy();
                weightedInst.setWeight(inst.weight() * k);
                this.learner.trainOnInstance(weightedInst);
            }
        }

        @Override
        public Integer call() {
            run();
            return 0;
        }
    }

}
