/*
 *    AccuracyUpdatedEnsemble1.java
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

import moa.classifiers.Classifier;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;
import weka.core.Instances;

/**
 * The Accuracy Updated Ensemble classifier as proposed by Brzezinski and
 * Stefanowski in "Accuracy Updated Ensemble for Data Streams with Concept
 * Drift", HAIS 2011.  A newer/revised version of this algorithm exists and 
 * is called AccuracyUpdatedEnsemble2.
 */
public class AccuracyUpdatedEnsemble1 extends AccuracyWeightedEnsemble {

    private static final long serialVersionUID = 1L;

    public AccuracyUpdatedEnsemble1()
    {
    	this.learnerOption = new ClassOption("learner", 'l', "Classifier to train.", Classifier.class, "trees.HoeffdingTree -e 1000 -g 100 -c 0.01");
    }
    
    @Override
    public String getPurposeString() {
        return "Accuracy Updated Ensemble classifier as proposed by Brzezinski and Stefanowski in " +
        		"'Accuracy Updated Ensemble for Data Streams with Concept Drift', HAIS 2011. A newer/revised " +
        		"version of this algorithm exists and is called AccuracyUpdatedEnsemble2.";
    }

    @Override
    public void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        super.prepareForUseImpl(monitor, repository);
    }

    @Override
    protected void processChunk() {
        Classifier addedClassifier = null;

        // Compute weights
        double candidateClassifierWeight = this.computeCandidateWeight(
                this.candidateClassifier, this.currentChunk, this.numFolds);

        for (int i = 0; i < this.storedLearners.length; i++) {
            this.storedWeights[i][0] = this.computeWeight(
                    this.storedLearners[(int) this.storedWeights[i][1]], this.currentChunk);
        }

        if (this.storedLearners.length < this.maxStoredCount) {
            // Train and add classifier
            this.trainOnChunk(this.candidateClassifier);
            addedClassifier = this.addToStored(this.candidateClassifier,
                    candidateClassifierWeight);
        } else {
            // Substitute poorest classifier
            java.util.Arrays.sort(this.storedWeights, weightComparator);

            if (this.storedWeights[0][0] < candidateClassifierWeight) {
                this.trainOnChunk(this.candidateClassifier);
                this.storedWeights[0][0] = candidateClassifierWeight;
                addedClassifier = this.candidateClassifier.copy();
                this.storedLearners[(int) this.storedWeights[0][1]] = addedClassifier;
            }
        }

        int ensembleSize = java.lang.Math.min(this.storedLearners.length,
                this.maxMemberCount);
        this.ensemble = new Classifier[ensembleSize];
        this.ensembleWeights = new double[ensembleSize];

        // Sort learners according to their weights
        java.util.Arrays.sort(this.storedWeights, weightComparator);

        double mse_r = this.computeMseR();

        // Select top k classifiers to construct the ensemble
        int storeSize = this.storedLearners.length;
        for (int i = 0; i < ensembleSize; i++) {
            this.ensembleWeights[i] = this.storedWeights[storeSize - i - 1][0];
            this.ensemble[i] = this.storedLearners[(int) this.storedWeights[storeSize
                    - i - 1][1]];

            if (this.ensemble[i] != addedClassifier) {
                if (mse_r > 0 && this.ensembleWeights[i] > 1 / mse_r) {
                    this.trainOnChunk(this.ensemble[i]);
                }
            }
        }

        this.classDistributions = null;
        this.currentChunk = null;
        this.candidateClassifier = (Classifier) getPreparedClassOption(this.learnerOption);
        this.candidateClassifier.resetLearning();
    }

    @Override
    protected double computeWeight(Classifier learner, Instances chunk) {
        double mse_i = 0;

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

        if (mse_i > 0) {
            return 1.0 / mse_i;
        } else {
            return Double.MAX_VALUE;
        }
    }

    /**
     * Trains a component classifier on the most recent chunk of data.
     *
     * @param classifierToTrain Classifier being trained.
     */
    private void trainOnChunk(Classifier classifierToTrain) {
        for (int num = 0; num < this.chunkSize; num++) {
            classifierToTrain.trainOnInstance(this.currentChunk.instance(num));
        }
    }

    /**
     * Determines whether the classifier is randomizable.
     */
    public boolean isRandomizable() {
        return false;
    }
}
