/*
 *    AMRulesRegressor.java
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

package moa.classifiers.rules;

/**
 * Adaptive Model Rules (AMRules), the streaming rule learning algorithm for regression problems.
 * 
 * @author  A. Bifet, J. Duarte, J. Gama (jgama@fep.up.pt)
 * @version $Revision: 2 $* 
 * 
 * This algorithm learn ordered and unordered rule set from data stream. Each rule in AMRules use a 
 * Page-Hinkley test to detect changes in the processing generating data and react to changes by pruning the rule set.
 * This algorithm also does the detection of anomalies.
 * 
 * <p>Learning Adaptive Model Rules from High-Speed Data Streams, ECML 2013, E. Almeida, C. Ferreira, and J. Gama; </p>
 * Project Knowledge Discovery from Data Streams, FCT LIAAD-INESC TEC,.
 *  
 * <p>Parameters:</p>
 * <ul>
 * <li> Hoeffding Bound parameters</li>
 * <li> -c: split Confidence </li>
 * <li> -t: Tie Threshold </li>
 * <li> -g: GracePeriod, the number of instances a leaf should observe between split attempts </li>
 * <li> Page-Hinckley parameters</li>
 * <li> -H: Drift detection OFF</li>
 * <li> -a: The alpha value to use in the Page Hinckley change detection tests.</li> 
 * <li> -l: The threshold value (Lambda) to be used in the Page Hinckley change detection tests.</li>
 * <li> Anomaly Detection parameters</li>
 * <li> -A: Anomaly detection ON</li>
 * <li> -u: Univariate Threshold</li>
 * <li> -m: Multivariate Threshold</li>
 * <li> Method parameters</li>
 * <li> -P: Prediction function to use. Adaptive / Perceptron / Target Mean. Adaptive predefined </li>
 * <li> -O: Learn ordered rules. Unordered rule predefined</li>
 * <li> Perceptron parameters</li>
 * <li> -s: randomSeed</li>
 * <li> -r: learning Ratio </li>
 * <li> -d: learning Ratio Decay</li>
 * <li> Output Verbose Level </li> 
 * <li> -v: Verbosity level 1 to 5<li>
 * </ul>
 */

import moa.classifiers.Regressor;
import moa.classifiers.rules.core.Rule;
import moa.classifiers.rules.core.Rule.Builder;
import moa.classifiers.rules.core.RuleActiveLearningNode;
import moa.classifiers.rules.core.RuleActiveRegressionNode;
import moa.classifiers.rules.core.splitcriteria.AMRulesSplitCriterion;
import moa.classifiers.rules.core.voting.ErrorWeightedVote;
import moa.classifiers.rules.functions.Perceptron;
import moa.core.StringUtils;
import moa.options.ClassOption;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.MultiChoiceOption;



public class AMRulesRegressorOld extends AbstractAMRules implements Regressor{
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5988040868275521928L;

	
	public FlagOption constantLearningRatioDecayOption = new FlagOption(
			"learningRatio_Decay_set_constant", 'd',
			"Learning Ratio Decay in Perceptron set to be constant. (The next parameter).");
	
	public FloatOption learningRatioOption = new FloatOption(
			"learningRatio", 's', 
			"Constante Learning Ratio to use for training the Perceptrons in the leaves.", 0.025); 
	
	public MultiChoiceOption predictionFunctionOption = new MultiChoiceOption(
			"predictionFunctionOption", 'P', "The prediction function to use.", new String[]{
					"Adaptative","Perceptron", "Target Mean"}, new String[]{
					"Adaptative","Perceptron", "Target Mean"}, 0);
	
	public ClassOption votingTypeOption = new ClassOption("votingType",
			'V', "Voting Type.", 
			ErrorWeightedVote.class,
			"InverseErrorWeightedVote");
	
	public ClassOption splitCriterionOption = new ClassOption("splitCriterionOption",
			'S', "Split Criterion", 
			AMRulesSplitCriterion.class,
			"VRSplitCriterion");


	protected Rule newRule(int ID, RuleActiveLearningNode node, double[] statistics) {
		Rule r=newRule(ID);
		
		if (node!=null)
		{
			if(((RuleActiveRegressionNode)node).getPerceptron()!=null)
			{
				((RuleActiveRegressionNode)r.getLearningNode()).setPerceptron(new Perceptron(((RuleActiveRegressionNode)node).getPerceptron()));
				((RuleActiveRegressionNode)r.getLearningNode()).getPerceptron().setLearningRatio(this.learningRatioOption.getValue());
			}
			if (statistics==null)
			{
				double mean;
		        if(node.getNodeStatistics().getValue(0)>0){
		        	mean=node.getNodeStatistics().getValue(1)/node.getNodeStatistics().getValue(0);
		        	((RuleActiveRegressionNode)r.getLearningNode()).getTargetMean().reset(mean, 1); 
		        }
			}  
		}
		if (statistics!=null && ((RuleActiveRegressionNode)r.getLearningNode()).getTargetMean()!=null)
		{
			double mean;
	        if(statistics[0]>0){
	        	mean=statistics[1]/statistics[0];
	        	((RuleActiveRegressionNode)r.getLearningNode()).getTargetMean().reset(mean, (long)statistics[0]); 
	        }
		}
		return r;
	}


	public RuleActiveLearningNode newRuleActiveLearningNode(Builder builder) {
			return new RuleActiveRegressionNode(builder);
		}
	    
	public RuleActiveLearningNode newRuleActiveLearningNode(double[] initialClassObservations) {
	    	return new RuleActiveRegressionNode(initialClassObservations);
	}
	
	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		super.getModelDescription(out, indent);
		StringUtils.appendIndented(out, indent, "The prediction function used: "+this.predictionFunctionOption.getChosenLabel());
		StringUtils.appendNewline(out);
	}
	
	/**
	 * This method initializes and resets the algorithm.
	 */
	@Override
	public void resetLearningImpl() {
		this.statistics= new double[]{0.0,0,0};	
		this.ruleNumberID=0;
		this.defaultRule = newRule(++this.ruleNumberID);
	}
 

	private Rule newRule(int ID) {
		Rule r=new Rule.Builder().
				threshold(this.pageHinckleyThresholdOption.getValue()).
				alpha(this.pageHinckleyAlphaOption.getValue()).
				changeDetection(this.DriftDetectionOption.isSet()).
				predictionFunction(this.predictionFunctionOption.getChosenIndex()).
		        statistics(new double[3]).
				id(ID).
				amRules(this).build();
		r.getBuilder().setOwner(r);
		return r;
	}


	@Override
	public boolean isRandomizable() {
		return true;
	}


	@Override
	public ErrorWeightedVote newErrorWeightedVote() {
		return  (ErrorWeightedVote)((ErrorWeightedVote) votingTypeOption.getPreMaterializedObject()).copy();
	}

}
		
		


