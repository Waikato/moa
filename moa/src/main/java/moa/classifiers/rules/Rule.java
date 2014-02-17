/*
 *    Rule.java
 *    Copyright (C) 2012 University of Porto, Portugal
 *    @author P. Kosina, E. Almeida, A. Carvalho, J. Gama
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

/**
 * Class that stores an arrayList of predicates of a rule and the observers
 * (statistics). This class implements a function that evaluates a rule.
 *
 * <p>
 * Learning Decision Rules from Data Streams, IJCAI 2011, J. Gama, P. Kosina
 * </p>
 *
 * @author P. Kosina, E. Almeida, J. Gama
 * @version $Revision: 2 $
 *
 *
 */
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import moa.AbstractMOAObject;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.splitcriteria.SDRSplitCriterion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.StringUtils;

public class Rule extends AbstractMOAObject {

    private static final long serialVersionUID = 1L;

    protected List<RuleSplitNode> nodeList = new LinkedList<RuleSplitNode>();

    protected RuleActiveLearningNode learningNode;

    protected boolean verbose;

    protected int ruleNumberID;

    private double[] statisticsOtherBranchSplit;

    private Builder builder;

    /**
     * getLearningNode Method This is the way to pass info for other classes.
     * Implements getLearningNode() in class RuleActiveLearningNode
     *
     * @return
     */
    public RuleActiveLearningNode getLearningNode() {
        return learningNode;
    }

    public void setLearningNode(RuleActiveLearningNode learningNode) {
        this.learningNode = learningNode;
    }

    public List<RuleSplitNode> getNodeList() {
        return nodeList;
    }

    public long getInstancesSeen() {
        return this.learningNode.getInstancesSeen();
    }

    public void setNodeList(List<RuleSplitNode> nodeList) {
        this.nodeList = nodeList;
    }

    public Rule(Builder builder) {
        this.builder = builder;
        this.learningNode = new RuleActiveLearningNode(builder);
    }

    public boolean isCovering(Instance inst) {
        boolean isCovering = true;
        for (RuleSplitNode node : nodeList) {
            if (node.evaluate(inst) == false) {
                isCovering = false;
                break;
            }
        }
        return isCovering;
    }

    /**
     * MOA GUI output
     */
    @Override
    public void getDescription(StringBuilder sb, int indent) {
    }

    public static class Builder implements Serializable {

        private static final long serialVersionUID = 1712887264918475622L;
        protected boolean changeDetection;
        protected boolean usePerceptron;
        protected double threshold;
        protected double alpha;
        protected int predictionFunction;

        protected double[] statistics;

        protected double lastTargetMean;

        public FlagOption constantLearningRatioDecayOption;
        public FloatOption learningRatioOption;
        public int id;

        Builder() {
        }

        Builder changeDetection(boolean changeDetection) {
            this.changeDetection = changeDetection;
            return this;
        }

        Builder usePerceptron(boolean usePerceptron) {
            this.usePerceptron = usePerceptron;
            return this;
        }

        Builder threshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        Builder alpha(double alpha) {
            this.alpha = alpha;
            return this;
        }

        Builder constantLearningRatioDecayOption(FlagOption constantLearningRatioDecayOption) {
            this.constantLearningRatioDecayOption = constantLearningRatioDecayOption;
            return this;
        }

        Builder learningRatioOption(FloatOption learningRatioOption) {
            this.learningRatioOption = learningRatioOption;
            return this;
        }

        Builder predictionFunction(int predictionFunction) {
            this.predictionFunction = predictionFunction;
            return this;
        }

        Builder statistics(double[] statistics) {
            this.statistics = statistics;
            return this;
        }

        Builder lastTargetMean(double[] statistics) {
            this.lastTargetMean = statistics[1] / statistics[0];
            return this;
        }

        Builder id(int id) {
            this.id = id;
            return this;
        }

        Rule build() {
            return new Rule(this);
        }

    }

    public void updateStatistics(Instance instance) {
        this.learningNode.updateStatistics(instance);
    }

    /**
     *  Try to Expand method.
     * @param splitConfidence
     * @param tieThreshold
     * @return
     */
    public boolean tryToExpand(double splitConfidence, double tieThreshold) {

    	// splitConfidence. Hoeffding Bound test parameter.
        // tieThreshold. Hoeffding Bound test parameter.
       // Set the split criterion to use to the SDR split criterion as described by Ikonomovska et al. 
        SplitCriterion splitCriterion = new SDRSplitCriterion();

        // Using this criterion, find the best split per attribute and rank the results
        AttributeSplitSuggestion[] bestSplitSuggestions
                = this.learningNode.getBestSplitSuggestions(splitCriterion);
        Arrays.sort(bestSplitSuggestions);

        // Declare a variable to determine if any of the splits should be performed
        boolean shouldSplit = false;
        AttributeSplitSuggestion bestSuggestion = null;

        // If only one split was returned, use it
        if (bestSplitSuggestions.length < 2) {
            shouldSplit = ((bestSplitSuggestions.length > 0) && (bestSplitSuggestions[0].merit > 0));  //JG AC 22-01-2014
            bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
        } // Otherwise, consider which of the splits proposed may be worth trying
        else {
            // Determine the hoeffding bound value, used to select how many instances should be used to make a test decision
            // to feel reasonably confident that the test chosen by this sample is the same as what would be chosen using infinite examples
            double hoeffdingBound = computeHoeffdingBound(1, splitConfidence, learningNode.getWeightSeen());
            debug("Hoeffeding bound " + hoeffdingBound);
            // Determine the top two ranked splitting suggestions
            bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
            AttributeSplitSuggestion secondBestSuggestion
                    = bestSplitSuggestions[bestSplitSuggestions.length - 2];

            debug("Merits: " + secondBestSuggestion.merit + " " + bestSuggestion.merit);

            // If the upper bound of the sample mean for the ratio of SDR(best suggestion) to SDR(second best suggestion),
            // as determined using the hoeffding bound, is less than 1, then the true mean is also less than 1, and thus at this
            // particular moment of observation the bestSuggestion is indeed the best split option with confidence 1-delta, and
            // splitting should occur.
            // Alternatively, if two or more splits are very similar or identical in terms of their splits, then a threshold limit
            // (default 0.05) is applied to the hoeffding bound; if the hoeffding bound is smaller than this limit then the two
            // competing attributes are equally good, and the split will be made on the one with the higher SDR value.
            /*
             if ((((splitRatioStatistics.getValue(1)/splitRatioStatistics.getValue(0)) + hoeffdingBound)  < 1) 
             || (hoeffdingBound < tieThreshold)) {		
             System.out.println("Expanded ");
             shouldSplit = true;    
             }		
             */
            if (bestSuggestion.merit > 0) {
                if ((((secondBestSuggestion.merit / bestSuggestion.merit) + hoeffdingBound) < 1)
                        || (hoeffdingBound < tieThreshold)) {
                    debug("Expanded ");
                    shouldSplit = true;
                    //}		

                }
            }
        }

        if (shouldSplit == true) {
            AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];

            // Decide best node to keep: one with lower variance
            double minSDR = Double.MAX_VALUE;
            int splitIndex = 0;
            double[] statisticsNewRuleActiveLearningNode = null;
            double nr = 0.0;
            for (int i = 0; i < bestSuggestion.numSplits(); i++) {
                nr += bestSuggestion.resultingClassDistributionFromSplit(i)[0];
            }

            for (int i = 0; i < bestSuggestion.numSplits(); i++) {
                double[] dist = bestSuggestion.resultingClassDistributionFromSplit(i);
                double sdr = dist[0] / nr * SDRSplitCriterion.computeSD(dist);

                if (sdr < minSDR) {
                    minSDR = sdr;
                    splitIndex = i;
                    statisticsNewRuleActiveLearningNode = dist;
                }
            }

            this.statisticsOtherBranchSplit = bestSuggestion.resultingClassDistributionFromSplit(splitIndex == 0 ? 1 : 0);

            //create a split node,
            NumericAttributeBinaryTest splitTest = (NumericAttributeBinaryTest) bestSuggestion.splitTest;
            NumericAttributeBinaryRulePredicate predicate = new NumericAttributeBinaryRulePredicate(
                    splitTest.getAttsTestDependsOn()[0], splitTest.getSplitValue(),
                    splitIndex + 1);

            RuleSplitNode ruleSplitNode = new RuleSplitNode(predicate, splitDecision.resultingClassDistributionFromSplit(splitIndex));
            
            if (this.nodeListAdd(ruleSplitNode) == true) {
                // create a new learning node
                double[] oldPerceptronWeights = this.learningNode.perceptron.getWeights();
                this.learningNode = new RuleActiveLearningNode(this.builder.statistics(statisticsNewRuleActiveLearningNode));
                this.learningNode.perceptron.setWeights(oldPerceptronWeights);
                if (this.learningNode.perceptron.weightAttribute != null) {
                    this.learningNode.perceptron.normalizeWeights();
                }

                this.printRule();
            }

        }

        return shouldSplit;
    }

    private boolean nodeListAdd(RuleSplitNode ruleSplitNode) {
        //Check that the node is not already in the list
        boolean isIncludedInNodeList = false;
        for (RuleSplitNode node : nodeList) {
            NumericAttributeBinaryRulePredicate nodeTest = (NumericAttributeBinaryRulePredicate) node.getSplitTest();
            NumericAttributeBinaryRulePredicate ruleSplitNodeTest = (NumericAttributeBinaryRulePredicate) ruleSplitNode.getSplitTest();
            if (nodeTest.isUsingSameAttribute(ruleSplitNodeTest)) {
                isIncludedInNodeList = true;
                if (nodeTest.isIncludedInRuleNode(ruleSplitNodeTest) == true) { //remove this line to keep the most recent attribute value
                    //replace the value
                    nodeTest.setAttributeValue(ruleSplitNodeTest);
                }
            }
        }
        if (isIncludedInNodeList == false) {
            this.nodeList.add(ruleSplitNode);
        }
        return !isIncludedInNodeList;
    }

    public double[] statisticsOtherBranchSplit() {
        return this.statisticsOtherBranchSplit;
    }


    public void printRule() {
        StringBuilder out = new StringBuilder();
        int indent = 1;
        StringUtils.appendIndented(out, indent, "Rule Nr." + this.ruleNumberID + " Instances seen:" + this.learningNode.getInstancesSeen() + "\n"); // AC
        for (RuleSplitNode node : nodeList) {
            StringUtils.appendIndented(out, indent, node.getSplitTest().toString());
            StringUtils.appendIndented(out, indent, " ");
            StringUtils.appendIndented(out, indent, node.toString());
        }
        StringUtils.appendIndented(out, 0, " --> y: " + this.learningNode.getTargetMean());
        StringUtils.appendNewline(out);

        debug(out.toString());
    }

    public static double computeHoeffdingBound(double range, double confidence,
            double n) {
        return Math.sqrt(((range * range) * Math.log(1.0 / confidence))
                / (2.0 * n));
    }

    protected void debug(String string) {
        verbose = false;
        if (verbose) {
            System.out.println(string);
        }
    }

    public boolean isAnomaly(Instance instance,
            double uniVariateAnomalyProbabilityThreshold,
            double multiVariateAnomalyProbabilityThreshold,
            int numberOfInstanceesForAnomaly) {
        return this.learningNode.isAnomaly(instance, uniVariateAnomalyProbabilityThreshold,
                multiVariateAnomalyProbabilityThreshold,
                numberOfInstanceesForAnomaly);
    }

    public double computeError(Instance instance) {
        return this.learningNode.computeError(instance);
    }

    public boolean updatePageHinckleyTest(double error) {
        return this.learningNode.updatePageHinckleyTest(error);
    }

    public double getPrediction(Instance instance, int mode) {
        return this.learningNode.getPrediction(instance, mode);
    }

    public double getPrediction(Instance instance) {
        return this.learningNode.getPrediction(instance);
    }

    public double getTargetMean() {
        return this.learningNode.getTargetMean();
    }

}
