/*
 *    AMRules.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author A. Bifet, J. Duarte, J. Gama (jgama@fep.up.pt)
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
 * @version $Revision: 2.0 $* 
 * 
 * This algorithm learn ordered and unordered rule set from data stream. Each rule in AMRules use a 
 * Page-Hinkley test to detect changes in the processing generating data and react to changes by pruning the rule set.
 * This algorithm also does the detection of anomalies.
 * 
 * <p>Learning Adaptive Model Rules from High-Speed Data Streams, ECML 2013, J. Duarte, A. Bifet, J. Gama; </p>
 * Project Knowledge Discovery from Data Streams, FCT LIAAD-INESC TEC.
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
 * <li> -v: Verbosity level 1 to 4<li>
 * </ul>
 */

import moa.classifiers.rules.core.Rule;
import moa.classifiers.rules.core.RuleActiveClassifierNode;
import moa.classifiers.rules.core.RuleActiveLearningNode;
import moa.classifiers.rules.core.Rule.Builder;
import moa.classifiers.rules.core.voting.ErrorWeightedVote;
import moa.classifiers.rules.core.voting.OneMinusErrorWeightedVote;
import moa.core.StringUtils;
import com.github.javacliparser.MultiChoiceOption;


public class AMRulesClassifier extends AbstractAMRules {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7482084482011748106L;
	
	public MultiChoiceOption predictionFunctionOption = new MultiChoiceOption(
			"predictionFunctionOption", 'P', "The prediction function to use.", new String[]{
					"Adaptative","NaiveBayes", "MajorityClass"}, new String[]{
					"Adaptative","Naive Bayes", "Majority Class"}, 0);
	
	//============================== Classes ====================================//


	@Override
	public RuleActiveLearningNode newRuleActiveLearningNode(Builder builder) {
		return new RuleActiveClassifierNode(builder);
	}
   
	@Override
	public RuleActiveLearningNode newRuleActiveLearningNode(double[] initialClassObservations) {
    	return new RuleActiveClassifierNode(initialClassObservations);
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
		this.statistics = new double[0];
		this.ruleNumberID=0;
		this.defaultRule = newRule(this.statistics,++ruleNumberID); 
	}

	@Override //JD - TODO - Test function - learningNode not used (drop statistics and use learningNode?)
	protected Rule newRule(int ID,RuleActiveLearningNode learningNode, double [] statistics) {
		if (statistics!=null)
			return  newRule(statistics, ID);
		else
			return  newRule(learningNode.getNodeStatistics().getArrayCopy(), ID);
	}
	
	protected Rule newRule(double[] statistics, int ID) {
		return  new Rule.Builder().
				threshold(this.pageHinckleyThresholdOption.getValue()).
				alpha(this.pageHinckleyAlphaOption.getValue()).
				changeDetection(this.DriftDetectionOption.isSet()).
				predictionFunction(this.predictionFunctionOption.getChosenIndex()).
	            statistics(this.statistics).
				id(ID).
				amRules(this).
				build();
	}

	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public ErrorWeightedVote newErrorWeightedVote() {
		return new OneMinusErrorWeightedVote() ;
	}

}
		
		


