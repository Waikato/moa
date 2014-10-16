/*
 *    AMRulesMultiLabel.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
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

package moa.classifiers.rules.multilabel;
/**
 * Adaptive Model Rules for MultiLabel problems(AMRulesML), the streaming rule learning algorithm.
 * 
 * @author  J. Duarte, J. Gama (jgama@fep.up.pt)
 * @version $Revision: 1$* 
 * 
 * This algorithm learn ordered and unordered rule set from data stream. 
 * Each rule  detect changes in the processing generating data and react to changes by pruning the rule set.
 * This algorithm also does the detection of anomalies.
 * 
 **/

import java.util.Iterator;

import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.rules.core.anomalydetection.AnomalyDetector;
import moa.classifiers.rules.core.anomalydetection.OddsRatioScore;
import moa.classifiers.rules.multilabel.attributeclassobservers.NominalStatisticsObserver;
import moa.classifiers.rules.multilabel.attributeclassobservers.NumericStatisticsObserver;
import moa.classifiers.rules.multilabel.core.MultiLabelRule;
import moa.classifiers.rules.multilabel.core.MultiLabelRuleSet;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.rules.multilabel.core.voting.ErrorWeightedVoteMultiLabel;
import moa.classifiers.rules.multilabel.core.voting.MultiLabelVote;
import moa.classifiers.rules.multilabel.errormeasurers.MultiLabelErrorMeasurer;
import moa.classifiers.rules.multilabel.outputselectors.OutputAttributesSelector;
import moa.classifiers.rules.multilabel.outputselectors.SelectAllOutputs;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.options.ClassOption;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;


public abstract class AMRulesMultiLabelLearner extends AbstractMultiLabelLearner implements MultiLabelLearner{

	private static final long serialVersionUID = 1L;
	protected MultiLabelRuleSet ruleSet = new MultiLabelRuleSet();
	protected MultiLabelRule defaultRule;
	protected int ruleNumberID=1;
	protected double[] statistics;
	//public static final double NORMAL_CONSTANT = Math.sqrt(2 * Math.PI);
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

	public ClassOption learnerOption;

	public FlagOption unorderedRulesOption = new FlagOption("setUnorderedRulesOn", 'U',
			"unorderedRules.");

	public ClassOption changeDetector = new ClassOption("changeDetector",
			'H', "Change Detector.", 
			ChangeDetector.class,
			"PageHinkleyDM -d 0.05 -l 35.0");

	public ClassOption anomalyDetector = new ClassOption("anomalyDetector",
			'A', "Anomaly Detector.", 
			AnomalyDetector.class,
			OddsRatioScore.class.getName());

	public ClassOption splitCriterionOption;

	public ClassOption errorMeasurerOption;

	public ClassOption weightedVoteOption;



	public ClassOption numericObserverOption = new ClassOption("numericObserver",
			'y', "Numeric observer.", 
			NumericStatisticsObserver.class,
			"MultiLabelBSTree");
	
	public ClassOption nominalObserverOption = new ClassOption("nominalObserver",
			'z', "Nominal observer.", 
			NominalStatisticsObserver.class,
			"MultiLabelNominalAttributeObserver");

	public IntOption VerbosityOption = new IntOption(
			"verbosity",
			'v',
			"Output Verbosity Control Level. 1 (Less) to 5 (More)",
			1, 1, 5);
	
	public ClassOption outputSelectorOption = new ClassOption("outputSelector",
			'O', "Output attributes selector", 
			OutputAttributesSelector.class,
			//"StdDevThreshold");
			SelectAllOutputs.class.getName());


	protected double attributesPercentage;

	public double getAttributesPercentage() {
		return attributesPercentage;
	}

	public void setAttributesPercentage(double attributesPercentage) {
		this.attributesPercentage = attributesPercentage;
	}

	public AMRulesMultiLabelLearner() {
		super();
		attributesPercentage=100;
	}

	public AMRulesMultiLabelLearner(double attributesPercentage) {
		this();
		this.attributesPercentage=attributesPercentage;
	}


	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance inst) {
		/*MultiLabelVote vote=getVotes(inst);
		if(vote!=null)	
			return vote.getVote();
		else
			return null;*/
		ErrorWeightedVoteMultiLabel vote=getVotes(inst);
		if(vote!=null)	
			return vote.getPrediction();
		else
			return null;
	}

	/**
	 * getVotes extension of the instance method getVotesForInstance 
	 * in moa.classifier.java
	 * returns the prediction of the instance.
	 * Called in WeightedRandomRules
	 */
	public ErrorWeightedVoteMultiLabel getVotes(MultiLabelInstance instance) {
		ErrorWeightedVoteMultiLabel errorWeightedVote=newErrorWeightedVote();
		//DoubleVector combinedVote = new DoubleVector();
		debug("Test",3);    
		int numberOfRulesCovering = 0;

		VerboseToConsole(instance); // Verbose to console Dataset name.
		for (MultiLabelRule rule : ruleSet) {
			if (rule.isCovering(instance) == true){
				numberOfRulesCovering++;
				//DoubleVector vote = new DoubleVector(rule.getPrediction(instance));
				Prediction vote=rule.getPredictionForInstance(instance);
				if (vote!=null){ //should only happen for first instance
					double [] errors= rule.getCurrentErrors();
					debug("Rule No"+ rule.getRuleNumberID() + " Vote: " + vote.toString() + " Error: " + errors + " Y: " + instance.classValue(),3); //predictionValueForThisRule);
					errorWeightedVote.addVote(vote,errors);
				}
				//combinedVote.addValues(vote);
				if (!this.unorderedRulesOption.isSet()) { // Ordered Rules Option. //TODO: Only break if all outputs have values assigned.Complete Prediction only with the missing values
					break; // Only one rule cover the instance.
				}
			}
		}

		if (numberOfRulesCovering == 0) { //TODO: Change to "if all outputs have a value assigned. Complete Prediction only with the missing values
			//combinedVote = new DoubleVector(defaultRule.getPrediction(instance));
			Prediction vote=defaultRule.getPredictionForInstance(instance);
			if (vote!=null){ //should only happen for first instance
				double [] errors= defaultRule.getCurrentErrors();
				errorWeightedVote.addVote(vote,errors);
				debug("Default Rule Vote " + vote.toString() + "\n Error " + errors + "  Y: " + instance,3);
			} 
		} 	
		errorWeightedVote.computeWeightedVote();
		return errorWeightedVote;
		/*Prediction weightedVote=errorWeightedVote.computeWeightedVote();
		if(weightedVote!=null){
			double weightedError=errorWeightedVote.getWeightedError();
			debug("Weighted Rule - Vote: " + weightedVote.toString() + " Weighted Error: " + weightedError + " Y:" + instance.classValue(),3);
			return new MultiLabelVote(weightedVote, weightedError);
		}
		else 
			return new MultiLabelVote(null , Double.MAX_VALUE);*/

	}

	@Override
	public boolean isRandomizable(){
		return true;
	}



	/**
	 * Rule.Builder() to build an object with the parameters.
	 * If you have an algorithm with many parameters, especially if some of them are optional, 
	 * it can be beneficial to define an object that represents all of the parameters.
	 * @return
	 */
	//abstract protected Rule newRule(int ID, RuleActiveLearningNode learningNode, double [] statistics); //Learning node and statistics can be null

	/**
	 * AMRules Algorithm.
	 * Method for updating (training) the AMRules model using a new instance
	 */

	private double numChangesDetected; //Just for statistics 
	private double numAnomaliesDetected; //Just for statistics 
	private double numInstances; //Just for statistics

	@Override
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
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
		Iterator<MultiLabelRule> ruleIterator= this.ruleSet.iterator();
		while (ruleIterator.hasNext()) { 
			MultiLabelRule rule = ruleIterator.next();
			if (rule.isCovering(instance) == true) {
				rulesCoveringInstance = true; //TODO: JD use different strategies for this validation (first rule, first complete rule (all out attributes covered), voted complete rule, etc)
				if (!rule.updateAnomalyDetection(instance)) {
					if (rule.updateChangeDetection(instance)) {
						debug("I) Drift Detected. Exa. : " +  this.numInstances + " (" + rule.getWeightSeenSinceExpansion() +") Remove Rule: " +rule.getRuleNumberID(),1);
						ruleIterator.remove();
						this.numChangesDetected+=instance.weight();  //Just for statistics 
					} else {
						rule.trainOnInstance(instance);
						if (rule.getWeightSeenSinceExpansion()  % this.gracePeriodOption.getValue() == 0.0) {
							if (rule.tryToExpand(this.splitConfidenceOption.getValue(), this.tieThresholdOption.getValue()) ) 
							{
								setRuleOptions(rule);
								//rule.split();
								debug("Rule Expanded:",2);
								debug(rule.toString(),2);
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
			defaultRule.trainOnInstance(instance);
			if (defaultRule.getWeightSeenSinceExpansion() % this.gracePeriodOption.getValue() == 0.0) {
				debug("Nr. examples "+defaultRule.getWeightSeenSinceExpansion(), 4);

				if (defaultRule.tryToExpand(this.splitConfidenceOption.getValue(), this.tieThresholdOption.getValue()) == true) {
					//Rule newDefaultRule=newRule(defaultRule.getRuleNumberID(),defaultRule.getLearningNode(),defaultRule.getLearningNode().getStatisticsOtherBranchSplit()); //other branch
					//defaultRule.split();
					//create new default rule
					MultiLabelRule newDefaultRule=defaultRule.getNewRuleFromOtherBranch();
					newDefaultRule.setRuleNumberID(++ruleNumberID);
					setRuleOptions(newDefaultRule);
					
					//Add expanded rule to ruleset
					setRuleOptions(defaultRule);
					ruleSet.add(this.defaultRule);
					

					debug("Default rule expanded! New Rule:",2);
					debug(defaultRule.toString(),2);
					debug("New default rule:", 3);	
					debug(newDefaultRule.toString(),3);
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
	 *//*
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
	}*/



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
		if(!this.unorderedRulesOption.isSet()){
			StringUtils.appendIndented(out, indent, "Method Ordered");
			StringUtils.appendNewline(out);
		}else{
			StringUtils.appendIndented(out, indent, "Method Unordered");
			StringUtils.appendNewline(out);
		}
		StringUtils.appendIndented(out, indent, "Number of Rules: " + (this.ruleSet.size()+1));
		StringUtils.appendNewline(out);
		
		StringUtils.appendIndented(out, indent, "Default rule :");
		this.defaultRule.getDescription(out, indent);
		
		StringUtils.appendIndented(out, indent, "Rules in ruleSet:");
		StringUtils.appendNewline(out);
		for (MultiLabelRule rule: ruleSet) {
			rule.getDescription(out, indent);
		}
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

	protected void VerboseToConsole(MultiLabelInstance inst) {
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
		debug("Default rule :",2);
		debug(this.defaultRule.toString(),2);
		
		debug("Rules in ruleSet:",2);
		for (MultiLabelRule rule: ruleSet) {
			debug(rule.toString(),2);
		}
	}

	//	abstract public RuleActiveLearningNode newRuleActiveLearningNode(Builder builder);

	//	abstract public RuleActiveLearningNode newRuleActiveLearningNode(double[] initialClassObservations);



	@Override
	public void resetLearningImpl() {
		defaultRule=newDefaultRule();
		defaultRule.setLearner((MultiLabelLearner)((MultiLabelLearner)getPreparedClassOption(learnerOption)).copy());
		setRuleOptions(defaultRule);
	}
	

	protected void setRuleOptions(MultiLabelRule rule){
		rule.setSplitCriterion((MultiLabelSplitCriterion)((MultiLabelSplitCriterion)getPreparedClassOption(splitCriterionOption)).copy());
		rule.setChangeDetector((ChangeDetector)((ChangeDetector)getPreparedClassOption(changeDetector)).copy());
		rule.setAnomalyDetector((AnomalyDetector)((AnomalyDetector)getPreparedClassOption(anomalyDetector)).copy());
		rule.setNumericObserverOption((NumericStatisticsObserver)((NumericStatisticsObserver)getPreparedClassOption(numericObserverOption)).copy());
		rule.setNominalObserverOption((NominalStatisticsObserver)((NominalStatisticsObserver)getPreparedClassOption(nominalObserverOption)).copy());
		rule.setErrorMeasurer((MultiLabelErrorMeasurer)((MultiLabelErrorMeasurer)getPreparedClassOption(errorMeasurerOption)).copy());
		rule.setOutputAttributesSelector((OutputAttributesSelector)((OutputAttributesSelector)getPreparedClassOption(outputSelectorOption)).copy());
		rule.setRandomGenerator(this.classifierRandom);
		rule.setAttributesPercentage(this.attributesPercentage);
	}

	abstract protected MultiLabelRule newDefaultRule();

	abstract public ErrorWeightedVoteMultiLabel newErrorWeightedVote(); 



	public void setRandomSeed(int randomSeed){
		this.classifierRandom.setSeed(randomSeed);
	}


}