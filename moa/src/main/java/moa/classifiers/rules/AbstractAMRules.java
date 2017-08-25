/*
 *    AbstractAMRules.java
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
 *  @author  A. Bifet, J. Duarte, J. Gama (jgama@fep.up.pt)
 * @version $Revision: 2 $* 
 * 
 * This algorithm learn ordered and unordered rule set from data stream. Each rule in AMRules use a 
 * Page-Hinkley test to detect changes in the processing generating data and react to changes by pruning the rule set.
 * This algorithm also does the detection of anomalies.
 * 
 **/

import java.util.Arrays;
import java.util.Iterator;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.rules.core.Rule;
import moa.classifiers.rules.core.Rule.Builder;
import moa.classifiers.rules.core.RuleActiveLearningNode;
import moa.classifiers.rules.core.RuleSet;
import moa.classifiers.rules.core.attributeclassobservers.FIMTDDNumericAttributeClassLimitObserver;
import moa.classifiers.rules.core.voting.ErrorWeightedVote;
import moa.classifiers.rules.core.voting.Vote;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.options.ClassOption;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;


public abstract class AbstractAMRules extends AbstractClassifier {

	private static final long serialVersionUID = 1L;
	protected RuleSet ruleSet = new RuleSet();
	protected Rule defaultRule;
	protected int ruleNumberID;
	protected double[] statistics;
	public static final double NORMAL_CONSTANT = Math.sqrt(2 * Math.PI);
	public FloatOption splitConfidenceOption = new FloatOption(
			"splitConfidence",
			'c',
			"Hoeffding Bound Parameter. The allowable error in split decision, values closer to 0 will take longer to decide.",
			0.0000001, 0.0, 1.0);
	public FloatOption tieThresholdOption = new FloatOption("tieThreshold",
			't', "Hoeffding Bound Parameter. Threshold below which a split will be forced to break ties.",
			0.05, 0.0, 1.0);
	public IntOption gracePeriodOption = new IntOption("gracePeriod",
			'g', "Hoeffding Bound Parameter. The number of instances a leaf should observe between split attempts.",
			200, 1, Integer.MAX_VALUE);
	public FlagOption DriftDetectionOption = new FlagOption("DoNotDetectChanges", 'H',
			"Drift Detection. Page-Hinkley.");
	public FloatOption pageHinckleyAlphaOption = new FloatOption(
			"pageHinckleyAlpha",
			'a',
			"The alpha value to use in the Page Hinckley change detection tests.",
			0.005, 0.0, 1.0);
	public IntOption pageHinckleyThresholdOption = new IntOption(
			"pageHinckleyThreshold",
			'l',
			"The threshold value (Lambda) to be used in the Page Hinckley change detection tests.",
			35, 0, Integer.MAX_VALUE);
	public FlagOption noAnomalyDetectionOption = new FlagOption("noAnomalyDetection", 'A',
			"Disable anomaly Detection.");
	public FloatOption multivariateAnomalyProbabilityThresholdOption = new FloatOption(
			"multivariateAnomalyProbabilityThresholdd",
			'm',
			"Multivariate anomaly threshold value.",
			0.99, 0.0, 1.0);
	public FloatOption univariateAnomalyprobabilityThresholdOption = new FloatOption(
			"univariateAnomalyprobabilityThreshold",
			'u',
			"Univariate anomaly threshold value.",
			0.10, 0.0, 1.0);
	public IntOption anomalyNumInstThresholdOption = new IntOption(
			"anomalyThreshold",
			'n',
			"The threshold value of anomalies to be used in the anomaly detection.",
			30, 0, Integer.MAX_VALUE);
	public FlagOption unorderedRulesOption = new FlagOption("setUnorderedRulesOn", 'U',
			"unorderedRules.");

	public IntOption VerbosityOption = new IntOption(
			"verbosity",
			'v',
			"Output Verbosity Control Level. 1 (Less) to 5 (More)",
			1, 1, 5);

	public ClassOption numericObserverOption = new ClassOption("numericObserver",
			'z', "Numeric observer.", 
			FIMTDDNumericAttributeClassLimitObserver.class,
			"FIMTDDNumericAttributeClassLimitObserver");
	

	protected double attributesPercentage;
	
	public double getAttributesPercentage() {
		return attributesPercentage;
	}

	public void setAttributesPercentage(double attributesPercentage) {
		this.attributesPercentage = attributesPercentage;
	}

	public AbstractAMRules() {
		super();
		attributesPercentage=100;
	}
	
	public AbstractAMRules(double attributesPercentage) {
		this();
		this.attributesPercentage=attributesPercentage;
	}

	/**
	 * description of the Methods used.
	 * isRandomizable
	 * resetLearningImpl
	 * newRule // to build an object with the parameters.
	 * trainOnInstanceImpl
	 * isAnomaly
	 * getVotesForInstance
	 * getModelMeasurementsImpl
	 * getModelDescription // to printout to MOA GUI
	 * debug // use debug('string') to printout to console
	 */
	@Override
	public abstract boolean isRandomizable();



	/**
	 * Rule.Builder() to build an object with the parameters.
	 * If you have an algorithm with many parameters, especially if some of them are optional, 
	 * it can be beneficial to define an object that represents all of the parameters.
	 * @return
	 */
	abstract protected Rule newRule(int ID, RuleActiveLearningNode learningNode, double [] statistics); //Learning node and statistics can be null

	/**
	 * AMRules Algorithm.
	 * Method for updating (training) the AMRules model using a new instance
	 */

	private double numChangesDetected; //Just for statistics 
	private double numAnomaliesDetected; //Just for statistics 
	private double numInstances; ////Just for statistics

	@Override
	public void trainOnInstanceImpl(Instance instance) {
		/**
		 * AMRules Algorithm
		 * 
		 //For each rule in the rule set
			//If rule covers the instance
				//if the instance is not an anomaly	
					//Update Change Detection Tests
				    	//Compute prediction error
				    	//Call PHTest
						//If change is detected then
							//Remove rule
						//Else
							//Update sufficient statistics of rule
							//If number of examples in rule  > Nmin
								//Expand rule
						//If ordered set then
							//break
			//If none of the rule covers the instance
				//Update sufficient statistics of default rule
				//If number of examples in default rule is multiple of Nmin
					//Expand default rule and add it to the set of rules
					//Reset the default rule
		 */
		numInstances+=instance.weight();
		debug("Train",3);
		debug("Nº instance "+numInstances + " - " + instance.toString(),3);
		boolean rulesCoveringInstance = false;
		Iterator<Rule> ruleIterator= this.ruleSet.iterator();
		while (ruleIterator.hasNext()) { 
			Rule rule = ruleIterator.next();
			if (rule.isCovering(instance) == true) {
				rulesCoveringInstance = true;
				if (isAnomaly(instance, rule) == false) {
					//Update Change Detection Tests
					double error = rule.computeError(instance); //Use adaptive mode error
					boolean changeDetected = rule.getLearningNode().updateChangeDetection(error);
					if (changeDetected == true) {
						debug("I) Drift Detected. Exa. : " +  this.numInstances + " (" + rule.getInstancesSeen() +") Remove Rule: " +rule.getRuleNumberID(),1);

						ruleIterator.remove();
						this.numChangesDetected+=instance.weight();  //Just for statistics 
					} else {
						rule.updateStatistics(instance);
						if (rule.getInstancesSeen()  % this.gracePeriodOption.getValue() == 0.0) {
							if (rule.tryToExpand(this.splitConfidenceOption.getValue(), this.tieThresholdOption.getValue()) ) 
							{
								rule.split();
								debug("Rule Expanded:",2);
								debug(rule.printRule(),2);
							}	
						}
					}
				}
				else {
					debug("Anomaly Detected: " + this.numInstances + " Rule: " +rule.getRuleNumberID() ,1);
					this.numAnomaliesDetected+=instance.weight();//Just for statistics
				}
				if (!this.unorderedRulesOption.isSet()) 
					break;
			}
		}	

		if (rulesCoveringInstance == false){ 
			defaultRule.updateStatistics(instance);
			if (defaultRule.getInstancesSeen() % this.gracePeriodOption.getValue() == 0.0) {
				debug("Nr. examples "+defaultRule.getInstancesSeen(), 4);

				if (defaultRule.tryToExpand(this.splitConfidenceOption.getValue(), this.tieThresholdOption.getValue()) == true) {
					Rule newDefaultRule=newRule(defaultRule.getRuleNumberID(),defaultRule.getLearningNode(),defaultRule.getLearningNode().getStatisticsOtherBranchSplit()); //other branch
					defaultRule.split();
					defaultRule.setRuleNumberID(++ruleNumberID);
					this.ruleSet.add(this.defaultRule);

					debug("Default rule expanded! New Rule:",2);
					debug(defaultRule.printRule(),2);
					debug("New default rule:", 3);	
					debug(newDefaultRule.printRule(),3);
					defaultRule=newDefaultRule;

				}
			}
		}
	}



	/**
	 * Method to verify if the instance is an anomaly.
	 * @param instance
	 * @param rule
	 * @return
	 */
	private boolean isAnomaly(Instance instance, Rule rule) {
		//AMRUles is equipped with anomaly detection. If on, compute the anomaly value. 			
		boolean isAnomaly = false;	
		if (this.noAnomalyDetectionOption.isSet() == false){
			if (rule.getInstancesSeen() >= this.anomalyNumInstThresholdOption.getValue()) {
				isAnomaly = rule.isAnomaly(instance, 
						this.univariateAnomalyprobabilityThresholdOption.getValue(),
						this.multivariateAnomalyProbabilityThresholdOption.getValue(),
						this.anomalyNumInstThresholdOption.getValue());
			}
		}
		return isAnomaly;
	}

	/**
	 * getVotesForInstance extension of the instance method getVotesForInstance 
	 * in moa.classifier.java
	 * returns the prediction of the instance.
	 * Called in EvaluateModelRegression
	 */
	@Override
	public double[] getVotesForInstance(Instance instance) {
		return getVotes(instance).getVote();
	}

	/**
	 * print GUI evaluate model	
	 */
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[]{
				new Measurement("anomaly detections", this.numAnomaliesDetected),
				new Measurement("change detections", this.numChangesDetected), 
				new Measurement("rules (number)", this.ruleSet.size()+1)}; 
	}

	/**
	 * print GUI learn model	
	 */
	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		indent=0;
		if(!this.unorderedRulesOption.isSet()){
			StringUtils.appendIndented(out, indent, "Method Ordered");
			StringUtils.appendNewline(out);
		}else{
			StringUtils.appendIndented(out, indent, "Method Unordered");
			StringUtils.appendNewline(out);
		}
		if(this.DriftDetectionOption.isSet()){
			StringUtils.appendIndented(out, indent, "Change Detection OFF");
			StringUtils.appendNewline(out);
		}else{
			StringUtils.appendIndented(out, indent, "Change Detection ON");
			StringUtils.appendNewline(out);
		}
		if(this.noAnomalyDetectionOption.isSet()){
			StringUtils.appendIndented(out, indent, "Anomaly Detection OFF");
			StringUtils.appendNewline(out);
		}else{
			StringUtils.appendIndented(out, indent, "Anomaly Detection ON");
			StringUtils.appendNewline(out);
		}
		StringUtils.appendIndented(out, indent, "Number of Rules: " + (this.ruleSet.size()+1));
		StringUtils.appendNewline(out);		
	}

	/**
	 * Print to console
	 * @param string
	 */
	protected void debug(String string, int level) {
		if (VerbosityOption.getValue()>=level){
			System.out.println(string); 
		}
	}

	protected void VerboseToConsole(Instance inst) {
		if(VerbosityOption.getValue()>=5){	
			System.out.println(); 
			System.out.println("I) Dataset: "+inst.dataset().getRelationName()); 

			if(!this.unorderedRulesOption.isSet()){ 
				System.out.println("I) Method Ordered");
			}else{
				System.out.println("I) Method Unordered");
			}
		}    	
	}

	public void PrintRuleSet() {    	
		debug("Rule in RuleSet:",2);
		for (Rule rule: ruleSet) {
			debug(rule.printRule(),2);
		}

		debug("Default rule :",2);
		debug(this.defaultRule.printRule(),2);
	}

	abstract public RuleActiveLearningNode newRuleActiveLearningNode(Builder builder);

	abstract public RuleActiveLearningNode newRuleActiveLearningNode(double[] initialClassObservations);

	public int getModelAttIndexToInstanceAttIndex(int index, Instance inst){
		return modelAttIndexToInstanceAttIndex(index, inst);
	}

	@Override
	public void resetLearningImpl() {
		

	}

	/**
	 * Gets the index of the attribute in the instance,
	 * given the index of the attribute in the learner.
	 *
	 * @param index the index of the attribute in the learner
	 * @param inst the instance
	 * @return the index in the instance
	 */
	public static int modelAttIndexToInstanceAttIndex(int index,
			Instance inst) {
		return  index<= inst.classIndex() ? index : index + 1;
	}
	
	
	abstract public ErrorWeightedVote newErrorWeightedVote(); 

	
	/**
	 * getVotes extension of the instance method getVotesForInstance 
	 * in moa.classifier.java
	 * returns the prediction of the instance.
	 * Called in WeightedRandomRules
	 */
	public Vote getVotes(Instance instance) {
		ErrorWeightedVote errorWeightedVote=newErrorWeightedVote();
		//DoubleVector combinedVote = new DoubleVector();
		debug("Test",3);    
		int numberOfRulesCovering = 0;

		VerboseToConsole(instance); // Verbose to console Dataset name.
		for (Rule rule : ruleSet) {
			if (rule.isCovering(instance) == true){
				numberOfRulesCovering++;
				//DoubleVector vote = new DoubleVector(rule.getPrediction(instance));
				double [] vote=rule.getPrediction(instance);
				double error= rule.getCurrentError();
				debug("Rule No"+ rule.getRuleNumberID() + " Vote: " + Arrays.toString(vote) + " Error: " + error + " Y: " + instance.classValue(),3); //predictionValueForThisRule);
				errorWeightedVote.addVote(vote,error);
				//combinedVote.addValues(vote);
				if (!this.unorderedRulesOption.isSet()) { // Ordered Rules Option.
					break; // Only one rule cover the instance.
				}
			}
		}

		if (numberOfRulesCovering == 0) {
			//combinedVote = new DoubleVector(defaultRule.getPrediction(instance));
			double [] vote=defaultRule.getPrediction(instance);
			double error= defaultRule.getCurrentError();
			errorWeightedVote.addVote(vote,error);
			
			debug("Default Rule Vote " + Arrays.toString(vote) + " Error " + error + "  Y: " + instance.classValue(),3);
		} 	
		double[] weightedVote=errorWeightedVote.computeWeightedVote();
		double weightedError=errorWeightedVote.getWeightedError();
		
		debug("Weighted Rule - Vote: " + Arrays.toString(weightedVote) + " Weighted Error: " + weightedError + " Y:" + instance.classValue(),3);
		return new Vote(weightedVote, weightedError);
	}
	
	public void setRandomSeed(int randomSeed){
		//this.randomSeed=randomSeed;
		this.classifierRandom.setSeed(randomSeed);
	}
	
	//public int getRandomSeed(){
		//return this.randomSeed;
	//}
	


}