/*
 *    AccuracyWeightedEnsemble.java
 *    Copyright (C) 2010 Poznan University of Technology, Poznan, Poland
 *    @author Dariusz Brzezinski (dariusz.brzezinski@cs.put.poznan.pl)
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
package moa.classifiers.meta;

import java.util.Random;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.tasks.TaskMonitor;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.core.Utils;

/**
 * The Accuracy Weighted Ensemble classifier as proposed by Wang et al. in
 * "Mining concept-drifting data streams using ensemble classifiers", KDD 2003.
 */
public class AccuracyWeightedEnsemble extends AbstractClassifier implements MultiClassClassifier {

    @Override
    public String getPurposeString() {
        return "Accuracy Weighted Ensemble classifier as proposed by Wang et al. in 'Mining concept-drifting data streams using ensemble classifiers', KDD 2003";
    }

    /**
     * Simple weight comparator. Needed for sorting component classifiers.
     */
    private static final class ClassifierWeightComparator implements java.util.Comparator<double[]> {

        @Override
        public int compare(double[] o1, double[] o2) {
            if (o1[0] > o2[0]) {
                return 1;
            } else if (o1[0] < o2[0]) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private static final long serialVersionUID = 1L;

    /**
     * Simple weight comparator.
     */
    protected static java.util.Comparator<double[]> weightComparator = new ClassifierWeightComparator();

    /**
     * Type of classifier to use as a component classifier.
     */
    public ClassOption learnerOption = new ClassOption("learner", 'l', "Classifier to train.", Classifier.class, "trees.HoeffdingTree -l NB -e 1000 -g 100 -c 0.01");

    /**
     * Number of component classifiers.
     */
    public FloatOption memberCountOption = new FloatOption("memberCount", 'n', "The maximum number of classifier in an ensemble.", 15, 1, Integer.MAX_VALUE);

    /**
     * Number of classifiers remembered and available for ensemble construction.
     */
    public FloatOption storedCountOption = new FloatOption("storedCount", 'r', "The maximum number of classifiers to store and choose from when creating an ensemble.", 30, 1, Integer.MAX_VALUE);

    /**
     * Chunk size.
     */
    public IntOption chunkSizeOption = new IntOption("chunkSize", 'c', "The chunk size used for classifier creation and evaluation.", 500, 1, Integer.MAX_VALUE);

    /**
     * Number of folds in candidate classifier cross-validation.
     */
    public IntOption numFoldsOption = new IntOption("numFolds", 'f', "Number of cross-validation folds for candidate classifier testing.", 10, 1, Integer.MAX_VALUE);

    protected long[] classDistributions;

    protected Classifier[] ensemble;

    protected Classifier[] storedLearners;

    protected double[] ensembleWeights;

    /**
     * The weights of stored classifiers. storedWeights[x][0] = weight
     * storedWeights[x][1] = classifier
     */
    protected double[][] storedWeights;

    protected int processedInstances;

    protected int chunkSize;

    protected int numFolds;

    protected int maxMemberCount;

    protected int maxStoredCount;

    protected Classifier candidateClassifier;

    protected Instances currentChunk;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.maxMemberCount = (int) memberCountOption.getValue();
        this.maxStoredCount = (int) storedCountOption.getValue();

        if (this.maxMemberCount > this.maxStoredCount) {
            this.maxStoredCount = this.maxMemberCount;
        }

        this.chunkSize = this.chunkSizeOption.getValue();
        this.numFolds = this.numFoldsOption.getValue();
        this.candidateClassifier = (Classifier) getPreparedClassOption(this.learnerOption);
        this.candidateClassifier.resetLearning();

        super.prepareForUseImpl(monitor, repository);
    }

    @Override
    public void resetLearningImpl() {
        this.currentChunk = null;
        this.classDistributions = null;
        this.processedInstances = 0;
        this.ensemble = new Classifier[0];
        this.storedLearners = new Classifier[0];

        this.candidateClassifier = (Classifier) getPreparedClassOption(this.learnerOption);
        this.candidateClassifier.resetLearning();
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        this.initVariables();

        this.classDistributions[(int) inst.classValue()]++;
        this.currentChunk.add(inst);
        this.processedInstances++;

        if (this.processedInstances % this.chunkSize == 0) {
            this.processChunk();
        }
    }

    /**
     * Initiates the current chunk and class distribution variables.
     */
    private void initVariables() {
        if (this.currentChunk == null) {
            this.currentChunk = new Instances(this.getModelContext());
        }

        if (this.classDistributions == null) {
            this.classDistributions = new long[this.getModelContext().classAttribute().numValues()];

            for (int i = 0; i < this.classDistributions.length; i++) {
                this.classDistributions[i] = 0;
            }
        }
    }

    /**
     * Processes a chunk.
     *
     * @param useMseR Determines whether to use the MSEr threshold.
     */
    protected void processChunk() {
        // Compute weights
        double candidateClassifierWeight = this.computeCandidateWeight(this.candidateClassifier, this.currentChunk, this.numFolds);

        for (int i = 0; i < this.storedLearners.length; i++) {
            this.storedWeights[i][0] = this.computeWeight(this.storedLearners[(int) this.storedWeights[i][1]], this.currentChunk);
        }

        if (this.storedLearners.length < this.maxStoredCount) {
            // Train and add classifier
            for (int num = 0; num < this.chunkSize; num++) {
                this.candidateClassifier.trainOnInstance(this.currentChunk.instance(num));
            }

            this.addToStored(this.candidateClassifier, candidateClassifierWeight);
        } else {
            // Substitute poorest classifier
            java.util.Arrays.sort(this.storedWeights, weightComparator);

            if (this.storedWeights[0][0] < candidateClassifierWeight) {
                for (int num = 0; num < this.chunkSize; num++) {
                    this.candidateClassifier.trainOnInstance(this.currentChunk.instance(num));
                }

                this.storedWeights[0][0] = candidateClassifierWeight;
                this.storedLearners[(int) this.storedWeights[0][1]] = this.candidateClassifier.copy();
            }
        }

        int ensembleSize = java.lang.Math.min(this.storedLearners.length, this.maxMemberCount);
        this.ensemble = new Classifier[ensembleSize];
        this.ensembleWeights = new double[ensembleSize];

        // Sort learners according to their weights
        java.util.Arrays.sort(this.storedWeights, weightComparator);

        // Select top k classifiers to construct the ensemble
        int storeSize = this.storedLearners.length;
        for (int i = 0; i < ensembleSize; i++) {
            this.ensembleWeights[i] = this.storedWeights[storeSize - i - 1][0];
            this.ensemble[i] = this.storedLearners[(int) this.storedWeights[storeSize - i - 1][1]];
        }

        this.classDistributions = null;
        this.currentChunk = null;
        this.candidateClassifier = (Classifier) getPreparedClassOption(this.learnerOption);
        this.candidateClassifier.resetLearning();
    }

    /**
     * Computes the weight of a candidate classifier.
     *
     * @param candidate Candidate classifier.
     * @param chunk Data chunk of examples.
     * @param numFolds Number of folds in candidate classifier cross-validation.
     * @param useMseR Determines whether to use the MSEr threshold.
     * @return Candidate classifier weight.
     */
    protected double computeCandidateWeight(Classifier candidate, Instances chunk, int numFolds) {
        double candidateWeight = 0.0;
        Random random = new Random(1);
        Instances randData = new Instances(chunk);
        randData.randomize(random);
        if (randData.classAttribute().isNominal()) {
            randData.stratify(numFolds);
        }

        for (int n = 0; n < numFolds; n++) {
            Instances train = randData.trainCV(numFolds, n, random);
            Instances test = randData.testCV(numFolds, n);

            Classifier learner = candidate.copy();

            for (int num = 0; num < train.numInstances(); num++) {
                learner.trainOnInstance(train.instance(num));
            }

            candidateWeight += computeWeight(learner, test);
        }

        double resultWeight = candidateWeight / numFolds;

        if (Double.isInfinite(resultWeight)) {
            return Double.MAX_VALUE;
        } else {
            return resultWeight;
        }
    }

    /**
     * Computes the weight of a given classifie.
     *
     * @param learner Classifier to calculate weight for.
     * @param chunk Data chunk of examples.
     * @param useMseR Determines whether to use the MSEr threshold.
     * @return The given classifier's weight.
     */
    protected double computeWeight(Classifier learner, Instances chunk) {
        double mse_i = 0;
        double mse_r = 0;

        double f_ci;
        double voteSum;

        for (int i = 0; i < chunk.numInstances(); i++) {
            try {
                voteSum = 0;
                for (double element : learner.getVotesForInstance(chunk.instance(i))) {
                    voteSum += element;
                }

                if (voteSum > 0) {
                    f_ci = learner.getVotesForInstance(chunk.instance(i))[(int) chunk.instance(i).classValue()] / voteSum;
                    mse_i += (1 - f_ci) * (1 - f_ci);
                } else {
                    mse_i += 1;
                }
            } catch (Exception e) {
                mse_i += 1;
            }
        }

        mse_i /= this.chunkSize;
        mse_r = this.computeMseR();

        return java.lang.Math.max(mse_r - mse_i, 0);
    }

    /**
     * Computes the MSEr threshold.
     *
     * @return The MSEr threshold.
     */
    protected double computeMseR() {
        double p_c;
        double mse_r = 0;

        for (int i = 0; i < this.classDistributions.length; i++) {
            p_c = (double) this.classDistributions[i] / (double) this.chunkSize;
            mse_r += p_c * ((1 - p_c) * (1 - p_c));
        }

        return mse_r;
    }

    /**
     * Predicts a class for an example.
     */
    public double[] getVotesForInstance(Instance inst) {
        DoubleVector combinedVote = new DoubleVector();

        if (this.trainingWeightSeenByModel > 0.0) {
            for (int i = 0; i < this.ensemble.length; i++) {
                if (this.ensembleWeights[i] > 0.0) {
                    DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(inst));

                    if (vote.sumOfValues() > 0.0) {
                        vote.normalize();
                        //scale weight and prevent overflow
                        vote.scaleValues(this.ensembleWeights[i] / (1.0 * this.ensemble.length + 1));
                        combinedVote.addValues(vote);
                    }
                }
            }
        }
        combinedVote.normalize();
        return combinedVote.getArrayRef();
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    /**
     * Adds ensemble weights to the measurements.
     */
    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        Measurement[] measurements = new Measurement[this.maxStoredCount];

        for (int m = 0; m < this.maxMemberCount; m++) {
            measurements[m] = new Measurement("Member weight " + (m + 1), -1);
        }

        for (int s = this.maxMemberCount; s < this.maxStoredCount; s++) {
            measurements[s] = new Measurement("Stored member weight " + (s + 1), -1);
        }

        if (this.storedWeights != null) {
            int storeSize = this.storedWeights.length;

            for (int i = 0; i < storeSize; i++) {
                if (i < this.ensemble.length) {
                    measurements[i] = new Measurement("Member weight " + (i + 1), this.storedWeights[storeSize - i - 1][0]);
                } else {
                    measurements[i] = new Measurement("Stored member weight " + (i + 1), this.storedWeights[storeSize - i - 1][0]);
                }
            }
        }
        return measurements;
    }

    /**
     * Determines whether the classifier is randomizable.
     */
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public Classifier[] getSubClassifiers() {
        return this.ensemble.clone();
    }

    /**
     * Adds a classifier to the storage.
     *
     * @param newClassifier The classifier to add.
     * @param newClassifiersWeight The new classifiers weight.
     */
    protected Classifier addToStored(Classifier newClassifier, double newClassifiersWeight) {
        Classifier addedClassifier = null;
        Classifier[] newStored = new Classifier[this.storedLearners.length + 1];
        double[][] newStoredWeights = new double[newStored.length][2];

        for (int i = 0; i < newStored.length; i++) {
            if (i < this.storedLearners.length) {
                newStored[i] = this.storedLearners[i];
                newStoredWeights[i][0] = this.storedWeights[i][0];
                newStoredWeights[i][1] = this.storedWeights[i][1];
            } else {
                newStored[i] = addedClassifier = newClassifier.copy();
                newStoredWeights[i][0] = newClassifiersWeight;
                newStoredWeights[i][1] = i;
            }
        }
        this.storedLearners = newStored;
        this.storedWeights = newStoredWeights;

        return addedClassifier;
    }

    /**
     * Removes the poorest classifier from the model, thus decreasing the models
     * size.
     *
     * @return the size of the removed classifier.
     */
    protected int removePoorestModelBytes() {
        int poorestIndex = Utils.minIndex(this.ensembleWeights);
        int byteSize = this.ensemble[poorestIndex].measureByteSize();
        discardModel(poorestIndex);
        return byteSize;
    }

    /**
     * Removes the classifier at a given index from the model, thus decreasing
     * the models size.
     *
     * @param index
     */
    protected void discardModel(int index) {
        Classifier[] newEnsemble = new Classifier[this.ensemble.length - 1];
        double[] newEnsembleWeights = new double[newEnsemble.length];
        int oldPos = 0;
        for (int i = 0; i < newEnsemble.length; i++) {
            if (oldPos == index) {
                oldPos++;
            }
            newEnsemble[i] = this.ensemble[oldPos];
            newEnsembleWeights[i] = this.ensembleWeights[oldPos];
            oldPos++;
        }
        this.ensemble = newEnsemble;
        this.ensembleWeights = newEnsembleWeights;
    }
}
