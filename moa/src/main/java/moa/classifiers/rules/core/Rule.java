/*
 *    Rule.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author A. Bifet, J. Duarte, J. Gama
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
package moa.classifiers.rules.core;

/**
 * Class that stores an arrayList of predicates of a rule and the observers
 * (statistics). This class implements a function that evaluates a rule.
 *
 * <p>
 * Learning Decision Rules from Data Streams, IJCAI 2011, J. Gama, P. Kosina
 * </p>
 *
 * @author  A. Bifet, J. Duarte, J. Gama
 * @version $Revision: 2 $
 *
 *
 */
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import moa.AbstractMOAObject;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.rules.AbstractAMRules;
import moa.classifiers.rules.core.conditionaltests.NumericAttributeBinaryRulePredicate;
import moa.core.DoubleVector;
import moa.core.StringUtils;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;


public class Rule extends AbstractMOAObject {

	private static final long serialVersionUID = 1L;

	protected List<RuleSplitNode> nodeList = new LinkedList<RuleSplitNode>();

	protected RuleActiveLearningNode learningNode;

	protected int ruleNumberID;

	public int getRuleNumberID() {
		return ruleNumberID;
	}

	public void setRuleNumberID(int ruleNumberID) {
		this.ruleNumberID = ruleNumberID;
	}


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
		builder.setOwner(this);
		this.setBuilder(builder);
		this.amRules = builder.getAMRules();
		this.learningNode = newRuleActiveLearningNode(builder);
		//JD - use builder ID to set ruleNumberID
		this.ruleNumberID=builder.id;        
	}


	protected AbstractAMRules amRules;

	private RuleActiveLearningNode newRuleActiveLearningNode(Builder builder) {
		return amRules.newRuleActiveLearningNode(builder);
	}

	/*private RuleActiveLearningNode newRuleActiveLearningNode(double[] initialClassObservations) {
    	return amRules.newRuleActiveLearningNode(initialClassObservations);
    }*/


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
		private Rule owner; //jd

		public FlagOption constantLearningRatioDecayOption;
		public FloatOption learningRatioOption;
		public int id;
		public AbstractAMRules amRules;

		public AbstractAMRules getAMRules() {
			return amRules;
		}

		public Builder() {
		}

		public Builder changeDetection(boolean changeDetection) {
			this.changeDetection = changeDetection;
			return this;
		}

		public Builder threshold(double threshold) {
			this.threshold = threshold;
			return this;
		}

		public Builder alpha(double alpha) {
			this.alpha = alpha;
			return this;
		}

		public Builder predictionFunction(int predictionFunction) {
			this.predictionFunction = predictionFunction;
			return this;
		}

		public Builder statistics(double[] statistics) {
			this.statistics = statistics;
			return this;
		}

		public Builder owner(Rule owner) { 
			this.setOwner(owner);
			return this;
		}

		public Builder amRules(AbstractAMRules amRules) {
			this.amRules = amRules;
			return this;
		}

		public Builder id(int id) {
			this.id = id;
			return this;
		}
		public Rule build() {
			return new Rule(this);
		}

		public Rule getOwner() {
			return owner;
		}

		public void setOwner(Rule owner) {
			this.owner = owner;
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

		boolean shouldSplit= this.learningNode.tryToExpand(splitConfidence, tieThreshold);
		return shouldSplit;

	}
	//JD: Only call after tryToExpand returning true
	public void split()
	{

		//this.statisticsOtherBranchSplit  = this.learningNode.getStatisticsOtherBranchSplit(); 
		//create a split node,
		int splitIndex = this.learningNode.getSplitIndex();
		InstanceConditionalTest st=this.learningNode.getBestSuggestion().splitTest;
		if(st instanceof NumericAttributeBinaryTest ) {
			NumericAttributeBinaryTest splitTest = (NumericAttributeBinaryTest) st;
			NumericAttributeBinaryRulePredicate predicate = new NumericAttributeBinaryRulePredicate(
					splitTest.getAttsTestDependsOn()[0], splitTest.getSplitValue(),
					splitIndex + 1);
			RuleSplitNode ruleSplitNode = new RuleSplitNode(predicate, this.learningNode.getStatisticsBranchSplit() );



			if (this.nodeListAdd(ruleSplitNode) == true) {
				// create a new learning node
				RuleActiveLearningNode newLearningNode = newRuleActiveLearningNode(this.getBuilder().statistics(this.learningNode.getStatisticsNewRuleActiveLearningNode())); 
				newLearningNode.initialize(this.learningNode);
				this.learningNode = newLearningNode;
			}
		}
		else
			throw new UnsupportedOperationException("AMRules (currently) only supports numerical attributes.");

	}


	private boolean nodeListAdd(RuleSplitNode ruleSplitNode) {
		//Check that the node is not already in the list
		boolean isIncludedInNodeList = false;
		boolean isUpdated=false;
		for (RuleSplitNode node : nodeList) {
			NumericAttributeBinaryRulePredicate nodeTest = (NumericAttributeBinaryRulePredicate) node.getSplitTest();
			NumericAttributeBinaryRulePredicate ruleSplitNodeTest = (NumericAttributeBinaryRulePredicate) ruleSplitNode.getSplitTest();
			if (nodeTest.isUsingSameAttribute(ruleSplitNodeTest)) {
				isIncludedInNodeList = true;
				if (nodeTest.isIncludedInRuleNode(ruleSplitNodeTest) == true) { //remove this line to keep the most recent attribute value
					//replace the value
					nodeTest.setAttributeValue(ruleSplitNodeTest);
					isUpdated=true; //if is updated (i.e. an expansion happened) a new learning node should be created
				}
			}
		}
		if (isIncludedInNodeList == false) {
			this.nodeList.add(ruleSplitNode);
		}
		return (!isIncludedInNodeList || isUpdated); 
	}

	public double[] statisticsOtherBranchSplit() {
		return this.statisticsOtherBranchSplit;
	}


	public String printRule() {
		StringBuilder out = new StringBuilder();
		int indent = 1;
		StringUtils.appendIndented(out, indent, "Rule Nr." + this.ruleNumberID + " Instances seen:" + this.learningNode.getInstancesSeen() + "\n"); // AC
		for (RuleSplitNode node : nodeList) {
			StringUtils.appendIndented(out, indent, node.getSplitTest().toString());
			StringUtils.appendIndented(out, indent, " ");
			StringUtils.appendIndented(out, indent, node.toString());
		}
		DoubleVector pred = new DoubleVector(this.learningNode.getSimplePrediction());
		StringUtils.appendIndented(out, 0, " --> y: " + pred.toString());
		StringUtils.appendNewline(out);

		if (this.learningNode instanceof RuleActiveRegressionNode) {
			if(((RuleActiveRegressionNode)this.learningNode).perceptron!=null){
				((RuleActiveRegressionNode)this.learningNode).perceptron.getModelDescription(out,0 );
				StringUtils.appendNewline(out);
			}
		}
		return(out.toString());
	}



	protected void debug(String string, int level) {
		if (this.amRules.VerbosityOption.getValue()>=level) {
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

	public double[] getPrediction(Instance instance, int mode) {
		return this.learningNode.getPrediction(instance, mode);
	}

	public double[] getPrediction(Instance instance) {
		return this.learningNode.getPrediction(instance);
	}

	public Builder getBuilder() {
		return builder;
	}

	public void setBuilder(Builder builder) {
		this.builder = builder;
	}

	public double getCurrentError() {
		return this.learningNode.getCurrentError();
	}
	

}
