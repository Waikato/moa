/*
 *    RuleActiveLearningNode.java
 *    Copyright (C) 2013 University of Porto, Portugal
 *    @author E. Almeida, A. Carvalho, J. Gama
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */
package moa.classifiers.rules;

import com.yahoo.labs.samoa.instances.Instance;
import java.util.LinkedList;
import java.util.List;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.trees.HoeffdingTree;
import moa.classifiers.trees.HoeffdingTree.ActiveLearningNode;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;

/**
 * A modified ActiveLearningNode that uses a Perceptron as the leaf node model,
 * and ensures that the class values sent to the attribute observers are not
 * truncated to ints if regression is being performed
 */
public class RuleActiveLearningNode extends ActiveLearningNode {

    protected Perceptron perceptron;

    protected PageHinckeyTest pageHinckleyTest;

    protected int predictionFunction;

    protected double sumErrorTargetMean; // Sum Error Target Mean (SETM) for each Rule. 03-01-2014 JG,AC.

    protected boolean changeDetection;

    protected int ruleNumberID;

    private static final long serialVersionUID = 9129659494380381126L;

    // The statistics for this node:
    // Number of instances that have reached it
    // Sum of y values
    // Sum of squared y values
    protected DoubleVector nodeStatistics;

    /**
     * Create a new RuleActiveLearningNode
     */
    public RuleActiveLearningNode(double[] initialClassObservations) {
        super(initialClassObservations);
        this.nodeStatistics = new DoubleVector(initialClassObservations); //new DoubleVector();
        this.sumErrorTargetMean = 0.0;
    }

    public RuleActiveLearningNode() {
        this(new double[0]);
    }

    public RuleActiveLearningNode(Rule.Builder builder) {
        this(builder.statistics);
        this.changeDetection = builder.changeDetection;
        if (builder.changeDetection == false) { // AC you need to select the option not to detected changes.
            this.pageHinckleyTest = new PageHinckeyTest(builder.threshold, builder.alpha);
        }

        this.perceptron = new Perceptron();
        this.perceptron.prepareForUse();
        this.perceptron.learningRatioOption = builder.learningRatioOption;
        this.perceptron.constantLearningRatioDecayOption = builder.constantLearningRatioDecayOption;

        this.predictionFunction = builder.predictionFunction;
        this.ruleNumberID = builder.id;
    }

    @Override
    public double getWeightSeen() {
        if (nodeStatistics != null) {
            return this.nodeStatistics.getValue(0);
        } else {
            return 0;
        }
    }

    /**
     * Method to learn from an instance that passes the new instance to the
     * perceptron learner, and also prevents the class value from being
     * truncated to an int when it is passed to the attribute observer
     */
    public void learnFromInstance(Instance inst) {
   // this.observedClassDistribution.addToValue((int)inst.classValue(),inst.weight());

        // Update the statistics for this node
        // number of instances passing through the node
        nodeStatistics.addToValue(0, 1);
        // sum of y values
        nodeStatistics.addToValue(1, inst.classValue());
        // sum of squared y values

        for (int i = 0; i < inst.numAttributes() - 1; i++) {
            //int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
            AttributeClassObserver obs = this.attributeObservers.get(i);
            if (obs == null) {
                // At this stage all nominal attributes are ignored
                if (inst.attribute(i).isNumeric()) //instAttIndex
                {
                    obs = newNumericClassObserver();
                    this.attributeObservers.set(i, obs);
                }
            }
            if (obs != null) {
                ((FIMTDDNumericAttributeClassObserver) obs).observeAttributeClass(inst.value(i), inst.classValue(), inst.weight());
            }
        }
    }

    @Override
    public void learnFromInstance(Instance inst, HoeffdingTree ht) {
        learnFromInstance(inst);
    }

    private AttributeClassObserver newNumericClassObserver() {
        return new FIMTDDNumericAttributeClassObserver();
    }

    /**
     * Return the best split suggestions for this node using the given split
     * criteria
     */
    public AttributeSplitSuggestion[] getBestSplitSuggestions(SplitCriterion criterion) {

        List<AttributeSplitSuggestion> bestSuggestions = new LinkedList<AttributeSplitSuggestion>();

        // Set the nodeStatistics up as the preSplitDistribution, rather than the observedClassDistribution
        double[] nodeSplitDist = this.nodeStatistics.getArrayCopy();

        for (int i = 0; i < this.attributeObservers.size(); i++) {
            AttributeClassObserver obs = this.attributeObservers.get(i);
            if (obs != null) {

                // AT THIS STAGE NON-NUMERIC ATTRIBUTES ARE IGNORED
                AttributeSplitSuggestion bestSuggestion = null;
                if (obs instanceof FIMTDDNumericAttributeClassObserver) {
                    bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion, nodeSplitDist, i, true);
                }

                if (bestSuggestion != null) {
                    bestSuggestions.add(bestSuggestion);
                }
            }
        }
        return bestSuggestions.toArray(new AttributeSplitSuggestion[bestSuggestions.size()]);
    }

    public void updateStatistics(Instance instance) {
        learnFromInstance(instance);
        if (this.predictionFunction != 2) {
            this.perceptron.trainOnInstance(instance);
        }
        this.sumErrorTargetMean += Math.abs(instance.classValue() - this.getTargetMean());

    }

    public AutoExpandVector<AttributeClassObserver> getAttributeObservers() {
        return this.attributeObservers;
    }

    protected void debug(String string) {
        boolean verbose = false;
        if (verbose) {
            System.out.print(string);
        }
    }

    public double getPrediction(Instance instance) {
        int predictionMode = this.getLearnerToUse(instance, this.predictionFunction);
        return getPrediction(instance, predictionMode);
    }

    public double getPrediction(Instance instance, int predictionMode) {
        return (predictionMode == 1) ? this.perceptron.prediction(instance)
                : targetMeanPrediction();
    }

    public double getNormalizedPrediction(Instance instance) {
        int predictionMode = this.getLearnerToUse(instance, 0);
        return (predictionMode == 1) ? this.perceptron.normalizedPrediction(instance)
                : normalize(targetMeanPrediction());
    }

    public int getLearnerToUse(Instance instance, int predictionMode) {

        if (this.perceptron != null && this.perceptron.getInstancesSeen() <= 100) { // AC 100.
            predictionMode = 2; // Target mean strategy.
        }
        if (predictionMode == 0) {
            double perceptronError = this.perceptron.getAccumulatedError() / this.perceptron.getInstancesSeen();
            double meanTargetError = this.sumErrorTargetMean / this.getInstancesSeen();
            debug("\n Check P:" + perceptronError + " M:" + meanTargetError);
            debug("\n Check P:" + this.perceptron.prediction(instance) + " M:" + targetMeanPrediction());
            debug("Observed Value: " + instance.classValue());
            if (perceptronError < meanTargetError) {
                predictionMode = 1; //PERCEPTRON
            } else {
                predictionMode = 2; //TARGET MEAN
            }
        }
        if (this.perceptron != null && this.getInstancesSeen() > 100 && this.getInstancesSeen() < 200) {
            debug("\nCheck" + this.perceptron.prediction(instance) + " " + targetMeanPrediction());
        }
        return predictionMode;
    }

    private double targetMeanPrediction() {
        return this.getTargetMean();
    }

    private double normalize(double value) {
        double meanY = this.getTargetMean();
        double sdY = computeSD(this.getTargetSquaredSum(), this.getTargetSum(), this.getInstancesSeen());

        double normalizedY = 0.0;
        if (sdY > 0.0000001) {
            normalizedY = (value - meanY) / (sdY);
        }
        return normalizedY;
    }

    public double computeSD(double squaredVal, double val, long size) {
        if (size > 1) {
            return Math.sqrt((squaredVal - ((val * val) / size)) / (size - 1.0));
        }
        return 0.0;
    }

    public double computeError(Instance instance) {
        double normalizedPrediction = getNormalizedPrediction(instance); // option JG AC 24-01-2014
        //double normalizedPrediction = normalize(targetMeanPrediction()); // option JG AC 24-01-2014
        double normalizedClassValue = normalize(instance.classValue());
        return Math.abs(normalizedClassValue - normalizedPrediction);
    }

    /**
     *
     * @param error
     * @return
     */
    public boolean updatePageHinckleyTest(double error) {
        boolean changeDetected = false;
        if (this.changeDetection == false) { // AC you need to select the option not to detected changes.
            //System.out.print("W) RuleError Rule "+ error +"\n");
            changeDetected = pageHinckleyTest.update(error);
        }
        return changeDetected;
    }

    public long getInstancesSeen() {
        return (long) this.getWeightSeen();
    }

    private double getTargetSum() {
        return this.nodeStatistics.getValue(1);
    }

    public double getTargetMean() {
        return this.getTargetSum() / this.getInstancesSeen();
    }

    private double getTargetSquaredSum() {
        return this.nodeStatistics.getValue(2);
    }

    public boolean isAnomaly(Instance instance,
            double uniVariateAnomalyProbabilityThreshold,
            double multiVariateAnomalyProbabilityThreshold,
            int numberOfInstanceesForAnomaly) {
        //AMRUles is equipped with anomaly detection. If on, compute the anomaly value.
        if (perceptron == null) {
            return false;
        }
        if (this.perceptron.perceptronInstancesSeen >= numberOfInstanceesForAnomaly) {
            double atribSum = 0.0;
            double atribSquredSum = 0.0;
            double D = 0.0;
            double N = 0.0;
            double anomaly = 0.0;

            for (int x = 0; x < instance.numAttributes() - 1; x++) {
            	// Perceptron is initialized each rule.
                // this is a local anomaly.
                atribSum = this.perceptron.perceptronattributeStatistics.getValue(x);
                atribSquredSum = this.perceptron.squaredperceptronattributeStatistics.getValue(x);
                double mean = atribSum / this.perceptron.perceptronInstancesSeen;
                double sd = computeSD(atribSquredSum, atribSum, this.perceptron.perceptronInstancesSeen);

                double probability = computeProbability(mean, sd, instance.value(x));

                if (probability > 0.0) {
                    D = D + Math.abs(Math.log(probability));
                    if (probability < uniVariateAnomalyProbabilityThreshold) {//0.10
                        N = N + Math.abs(Math.log(probability));
                    }
                } else {
                    debug("Anomaly with probability 0 in atribute : " + x);
                }
            }

            anomaly = 0.0;
            if (D != 0.0) {
                anomaly = N / D;
            }
            if (anomaly >= multiVariateAnomalyProbabilityThreshold) {
                debuganomaly(instance,
                        uniVariateAnomalyProbabilityThreshold,
                        multiVariateAnomalyProbabilityThreshold,
                        anomaly);
                return true;
            }
        }
        return false;
    }

    //Attribute probability
    public double computeProbability(double mean, double sd, double value) {
        double probability = 0.0;
        double diff = value - mean; // AC normalized values!

        if (sd > 0.0) {
            double k = (Math.abs(value - mean) / sd);
            if (k > 1.0) {
                probability = 1.0 / (k * k); // Chebyshev's inequality
            } else {
                probability = Math.exp(-(diff * diff / (2.0 * sd * sd)));
            }
        }
        return probability;
    }

    protected void debuganomaly(Instance instance, double uni, double multi, double probability) {
        double atribSum = 0.0;
        double atribSquredSum = 0.0;

        System.out.println("E) Anomaly detected. Instance : "
                + this.getInstancesSeen() + " is an anomaly in dataset : "
                //+ instance.dataset().relationName()
        );

        for (int x = 0; x < instance.numAttributes() - 1; x++) {
            atribSum = perceptron.perceptronattributeStatistics.getValue(x);
            atribSquredSum = perceptron.squaredperceptronattributeStatistics.getValue(x);
            double mean = atribSum / perceptron.perceptronInstancesSeen;
            double sd = computeSD(
                    atribSquredSum,
                    atribSum,
                    perceptron.perceptronInstancesSeen
            );
            debug("Attribute : " + x);
            debug("Value : " + instance.value(x));
            debug("Mean : " + mean);
            debug("SD : " + sd);
            debug("Probability : " + probability);
            debug("Univariate : " + uni);
            debug("Multivariate : " + multi);
            debug("Anomaly in rule :" + ruleNumberID);
        }
    }

}
