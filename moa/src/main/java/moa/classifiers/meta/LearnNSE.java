/*
 *    LearnNSE.java
 *    Copyright (C) 2016 Instituto Federal de Pernambuco
 *    @author Paulo Gonçalves (paulogoncalves@recife.ifpe.edu.br)
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

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import java.util.ArrayList;
import java.util.List;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.options.ClassOption;

/**
 * <p>Ensemble of classifiers-based approach for incremental learning of concept 
 * drift, characterized by nonstationary environments (NSEs), where the 
 * underlying data distributions change over time. It learns from consecutive 
 * batches of data that experience constant or variable rate of drift, 
 * addition or deletion of concept classes, as well as cyclical drift.</p>
 *
 * <p>Based on:
 * Ryan Elwell and Robi Polikar. Incremental learning of concept drift in
 * non-stationary environments. IEEE Transactions on Neural Networks,
 * 22(10):1517-1531, October 2011. ISSN 1045-9227. URL
 * http://dx.doi.org/10.1109/TNN.2011.2160459
 * </p>
 *
 * @author Paulo Gonçalves (paulogoncalves@recife.ifpe.edu.br)
 * @author Dariusz Brzezinski
 *
 *
 */
public class LearnNSE extends AbstractClassifier implements MultiClassClassifier {

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "bayes.NaiveBayes");

    public IntOption periodOption = new IntOption("period", 'p',
            "Size of the environments.", 250, 1, Integer.MAX_VALUE);

    public FloatOption sigmoidSlopeOption = new FloatOption(
            "sigmoidSlope",
            'a',
            "Slope of the sigmoid function controlling the number "
            + "of previous periods taken into account during weighting.",
            0.5, 0, Float.MAX_VALUE);

    public FloatOption sigmoidCrossingPointOption = new FloatOption(
            "sigmoidCrossingPoint",
            'b',
            "Halfway crossing point of the sigmoid function controlling the number of previous "
            + "periods taken into account during weighting.", 10, 0,
            Float.MAX_VALUE);

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 'e',
            "Ensemble size.", 15, 1, Integer.MAX_VALUE);

    public MultiChoiceOption pruningStrategyOption = new MultiChoiceOption(
            "pruningStrategy", 's', "Classifiers pruning strategy to be used.",
            new String[]{"NO", "AGE", "ERROR"}, new String[]{
                "Don't prune classifiers", "Age-based", "Error-based"}, 0);

    protected List<Classifier> ensemble;
    protected List<Double> ensembleWeights;
    protected List<ArrayList<Double>> bkts, wkts;
    protected Instances buffer;
    protected long index;
    protected double slope, crossingPoint;
    protected int pruning, ensembleSize;

    @Override
    public void resetLearningImpl() {
        this.ensemble = new ArrayList<>();
        this.ensembleWeights = new ArrayList<>();
        this.bkts = new ArrayList<>();
        this.wkts = new ArrayList<>();
        this.index = 0;
        this.buffer = null;
        this.slope = this.sigmoidSlopeOption.getValue();
        this.crossingPoint = this.sigmoidCrossingPointOption.getValue();
        this.pruning = this.pruningStrategyOption.getChosenIndex();
        this.ensembleSize = this.ensembleSizeOption.getValue();
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        this.index++;
        // Store instance in the buffer
        if (this.buffer == null) {
            this.buffer = new Instances(inst.dataset());
        }
        this.buffer.add(inst);

        if (this.index % this.periodOption.getValue() == 0) {
            this.index = 0;
            double mt = this.buffer.numInstances();
            Classifier classifier = ((Classifier) getPreparedClassOption(this.baseLearnerOption));
            classifier.resetLearning();

            if (this.ensemble.size() > 0) {
                double et = 0;
                // Reading all data chunk instances
                for (int i = 0; i < mt; i++) {
                    // Compute error of the existing ensemble on new data
                    boolean vote = this.correctlyClassifies(this.buffer
                            .instance(i));
                    if (!vote) {
                        et += 1.0 / mt;
                    }
                }
                // Normalizing error
                double weightSum = 0.0;
                // Reading all data chunk instances
                for (int i = 0; i < mt; i++) {
                    Instance instance = this.buffer.instance(i);
                    // Updating instance weights
                    boolean vote = this.correctlyClassifies(instance);
                    double error = (1.0 / mt) * (vote ? et : 1.0);
                    instance.setWeight(error);
                    weightSum += error;
                }
                // Reading all data chunk instances
                for (int i = 0; i < mt; i++) {
                    Instance instance = this.buffer.instance(i);
                    // Normalize weights
                    instance.setWeight(instance.weight() / weightSum);

                    // Call base classifier
                    Instance trainingInstance = (Instance) instance.copy();
                    trainingInstance.setWeight(1);
                    classifier.trainOnInstance(trainingInstance);
                }
            } else {
                // First run! Iterating through all instances in the data chunk
                for (int i = 0; i < mt; i++) {
                    Instance instance = this.buffer.instance(i);

                    // Initialize weights
                    instance.setWeight(1.0 / mt);

                    // Call base classifier
                    Instance trainingInstance = (Instance) instance.copy();
                    trainingInstance.setWeight(1);
                    classifier.trainOnInstance(trainingInstance);
                }
            }
            this.ensemble.add(classifier);
            this.bkts.add(new ArrayList());
            this.wkts.add(new ArrayList());
            this.ensembleWeights.clear();
            int t = this.ensemble.size();
            double maxError = Double.NEGATIVE_INFINITY;
            int errorIndex = Integer.MIN_VALUE;
            // Evaluate all existing classifiers on new data set
            for (int k = 1; k <= t; k++) {
                double ekt = 0;
                // Reading all data chunk instances
                for (int i = 0; i < mt; i++) {
                    Instance instance = this.buffer.instance(i);
                    if (!this.ensemble.get(k - 1).correctlyClassifies(instance)) {
                        // Ensemble incorrectly classifies this instance
                        ekt += instance.weight();
                    }
                }
                if (k == t && ekt > 0.5) {
                    // Generate a new classifier
                    Classifier c = (Classifier) getPreparedClassOption(this.baseLearnerOption);
                    c.resetLearning();
                    this.ensemble.set(k - 1, c);
                } else if (ekt > 0.5) {
                    // Remove voting power of this classifier
                    ekt = 0.5;
                }
				// Storing the index of the classifier with higher error in case
                // of error-based pruning
                if (ekt > maxError) {
                    maxError = ekt;
                    errorIndex = k;
                }
                // Normalizing errors
                double bkt = ekt / (1.0 - ekt);
                // Retrieving normalized errors for this classifier
                ArrayList<Double> nbkt = this.bkts.get(k - 1);
                nbkt.add(bkt);
				// Compute the weighted average of all normalized errors for kth
                // classifier h_k
                double wkt = 1.0 / (1.0 + Math.exp(-this.slope
                        * (t - k - this.crossingPoint)));
                List<Double> weights = this.wkts.get(k - 1);
                double sum = 0;
                for (Double weight : weights) {
                    sum += weight;
                }
                weights.add(wkt / (sum + wkt));
                double sbkt = 0.0;
                for (int j = 0; j < weights.size(); j++) {
                    sbkt += weights.get(j) * nbkt.get(j);
                }
                // Calculate classifier voting weights
                this.ensembleWeights.add(Math.log(1.0 / sbkt));
            }
            // Ensemble pruning strategy				
            if (pruning == 1 && t > ensembleSize) { // Age-based
                this.ensemble.remove(0);
                this.ensembleWeights.remove(0);
                this.bkts.remove(0);
                this.wkts.remove(0);
            } else if (pruning == 2 && t > ensembleSize) { // Error-based
                this.ensemble.remove(errorIndex - 1);
                this.ensembleWeights.remove(errorIndex - 1);
                this.bkts.remove(errorIndex - 1);
                this.wkts.remove(errorIndex - 1);
            }
            this.buffer = new Instances(this.getModelContext());
        }
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        DoubleVector combinedVote = new DoubleVector();
        if (this.trainingWeightSeenByModel > 0.0) {
            for (int i = 0; i < this.ensemble.size(); i++) {
                if (this.ensembleWeights.get(i) > 0.0) {
                    DoubleVector vote = new DoubleVector(this.ensemble.get(i)
                            .getVotesForInstance(inst));
                    if (vote.sumOfValues() > 0.0) {
                        vote.normalize();
                        vote.scaleValues(this.ensembleWeights.get(i));
                        combinedVote.addValues(vote);
                    }
                }
            }
        }
        return combinedVote.getArrayRef();
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        Measurement[] measurements = null;
        if (this.ensembleWeights != null) {
            measurements = new Measurement[this.ensembleWeights.size()];
            for (int i = 0; i < this.ensembleWeights.size(); i++) {
                measurements[i] = new Measurement("member weight " + (i + 1),
                        this.ensembleWeights.get(i));
            }
        }
        return measurements;
    }
}
