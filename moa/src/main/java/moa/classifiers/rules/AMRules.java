/*
 *    AMRules.java
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

/**
 * Adaptive Model Rules (AMRules), the streaming rule learning algorithm for regression problems.
 * 
 * @author E. Almeida, J. Gama (jgama@fep.up.pt)
 * @version $Revision: 1.101 $* 
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
 * <li> -v: Verbosity level 1 to 4<li>
 * </ul>
 */

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.Iterator;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Regressor;
import moa.core.Measurement;
import moa.core.StringUtils;



public class AMRules extends AbstractClassifier implements Regressor{
	
	private static final long serialVersionUID = 1L;
	
	protected boolean verbose;
	
	protected boolean verboseflag = true;
	
	protected RuleSet ruleSet = new RuleSet();
	
	protected Rule defaultRule;
	
	protected int ruleNumberID;	 // AC ALbert Bifet good practice (do not initialize)
	
	protected double[] statistics;
	
	//============================= SET OPTIONS ==============================//
	
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
			0.5, 0.0, 1.0); 
	
	public IntOption pageHinckleyThresholdOption = new IntOption(
			"pageHinckleyThreshold",
			'l',
			"The threshold value (Lambda) to be used in the Page Hinckley change detection tests.",
			50, 0, Integer.MAX_VALUE);
	
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
			30, 0, Integer.MAX_VALUE); // num minimum of instances to detect anomalies. 15.
	
	public FlagOption orderedRulesOption = new FlagOption("setOrderedRulesOn", 'O',
			"orderedRules.");
	
	public MultiChoiceOption predictionFunctionOption = new MultiChoiceOption(
			"predictionFunctionOption", 'P', "The prediction function to use.", new String[]{
					"Adaptative","Perceptron", "Target Mean"}, new String[]{
					"Adaptative","Perceptron", "Target Mean"}, 0);
	
	public FlagOption constantLearningRatioDecayOption = new FlagOption(
			"learningRatio_Decay_set_constant", 'd',
			"Learning Ratio Decay in Perceptron set to be constant. (The next parameter).");
	
	public FloatOption learningRatioOption = new FloatOption(
			"learningRatio", 's', 
			"Constante Learning Ratio to use for training the Perceptrons in the leaves.", 0.01);
		
	public IntOption VerbosityOption = new IntOption(
			"verbosity",
			'v',
			"Output Verbosity Control Level. 1 (Less) to 4 (More)",
			1, 1, 4); 

	//============================= END SET OPTIONS ==============================//

	
	//============================== Classes ====================================//
	
	/**
	 * description of the classes used.
	 * External files.
	 * Rule
	 * - RuleActiveLearningNode
	 * - RuleSet
	 *    - RuleSplitNode
	 * Predicate
	 *  - Perceptron
	 *  - PageHinckleyTest
	 */
	//============================== Methods ====================================// 
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
	//===========================================================================//
	
	
	@Override
	public boolean isRandomizable() {
		return false; // AC it was true for perceptron but as a classifier it has it own isRandomizable().
	}
	
	/**
	 * This method initializes and resets the algorithm.
	 */
	@Override
	public void resetLearningImpl() {
		this.statistics= new double[]{1.0,0,0};	// AC lastTargetMean=0.0
		this.ruleNumberID=0;
		this.defaultRule = newRule(this.statistics,++ruleNumberID);
		verbose=false;
	}
	
	/**
	 * Rule.Builder() to build an object with the parameters.
	 * If you have an algorithm with many parameters, especially if some of them are optional, 
	 * it can be beneficial to define an object that represents all of the parameters.
	 * @return
	 */	
        protected Rule newRule(double[] statistics, int ID) {
		return new Rule.Builder().
				threshold(this.pageHinckleyThresholdOption.getValue()).
				alpha(this.pageHinckleyAlphaOption.getValue()).
				changeDetection(this.DriftDetectionOption.isSet()).
				usePerceptron(this.predictionFunctionOption.getChosenIndex() != 2). // Adaptive=0 Perceptron=1 TargetMean=2
				predictionFunction(this.predictionFunctionOption.getChosenIndex()).
				constantLearningRatioDecayOption(this.constantLearningRatioDecayOption).
				learningRatioOption(this.learningRatioOption).
                statistics(this.statistics).
                lastTargetMean(this.statistics).
				id(ID).
				build();
	}
        
	/**
	 * AMRules Algorithm.
     * Method for updating (training) the AMRules model using a new instance
     */
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
				
		boolean rulesCoveringInstance = false;
		Iterator<Rule> ruleIterator= this.ruleSet.iterator();
		while (ruleIterator.hasNext()) { 
			//debug("Checking rule");
			Rule rule = ruleIterator.next();
			//if(verbose){rule.printRule();}
			if (rule.isCovering(instance) == true) {
				rulesCoveringInstance = true;
				if (isAnomaly(instance, rule) == false) {
					//Update Change Detection Tests
						double error = rule.computeError(instance); //Use adaptive mode error
						boolean changeDetected = rule.updatePageHinckleyTest(error);	
						if (changeDetected == true) {
							System.out.println("I) Drift Detected: " + rule.getInstancesSeen() +" Remove Rule: " +ruleNumberID);
							ruleIterator.remove();
						} else {
							rule.updateStatistics(instance);
							if (rule.getInstancesSeen()  % this.gracePeriodOption.getValue() == 0.0) {
								debug("Trying expansion rule:");
								rule.tryToExpand(this.splitConfidenceOption.getValue(), this.tieThresholdOption.getValue());
								if(verbose){PrintDefaultRule();}
							}
						}
					    if (this.orderedRulesOption.isSet()) 
					    	break;
						}
				}
			}	
			
		if (rulesCoveringInstance == false){
			defaultRule.updateStatistics(instance);
		    if (defaultRule.getInstancesSeen() % this.gracePeriodOption.getValue() == 0.0) {
		    	debug("Nr. examples "+defaultRule.getInstancesSeen());
		    	if (defaultRule.tryToExpand(this.splitConfidenceOption.getValue(), this.tieThresholdOption.getValue()) == true) {
		    		debug("Expansion default rule:");		    		
		    		this.ruleSet.add(this.defaultRule);
		    		defaultRule = newRule(defaultRule.statisticsOtherBranchSplit(),++ruleNumberID);
		    	}
		    }
		}
		//if(verbose){this.ruleSet.toString(); this.defaultRule.printRule();}
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
//				debug("I) Checking Anomaly.\n");
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
	@Override // extension of the instance method
	public double[] getVotesForInstance(Instance instance) {
		double[] votes = new double[1];
		int numberOfRulesCovering = 0;
		double cumulativeSumPredictedValues = 0.0;
		
		VerboseToConsole(instance); // Verbose to console Dataset name.
		//int i = 0; // only for print.out
		for (Rule rule: ruleSet) {
			//i++; // only for print.out
			if (rule.isCovering(instance) == true){
				numberOfRulesCovering++;
				double predictionValueForThisRule = rule.getPrediction(instance);
				debug(" Rule "+ rule.ruleNumberID +" " + predictionValueForThisRule);
				cumulativeSumPredictedValues = cumulativeSumPredictedValues + predictionValueForThisRule;
				if (this.orderedRulesOption.isSet()) { // Ordered Rules Option.
					break; // Only one rule cover the instance. cumulativeSumPredictedValues is equal to predictionValueForThisRule.
				}
			}
		}
		
		if (numberOfRulesCovering == 0) {
			votes[0] = defaultRule.getPrediction(instance, 0); // use Adaptive Mode
			//votes[0] = defaultRule.getTargetMean();
			 debug(" Default Rule " +votes[0]);
		} else {
			votes[0] = cumulativeSumPredictedValues/numberOfRulesCovering;
		}		
		// System.out.println();
		return votes;
	}
	
	/**
	 * print GUI evaluate model	
	 */
	@Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{
                    new Measurement("rules (number)", this.ruleSet.size()+1)}; // AC rules + default rule
    }

    /**
	 * print GUI learn model	
	 */
	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		//StringUtils.appendIndented("Dataset: "+inst.dataset().relationName()); // AC. CF. 02-01-2013
		indent=0;
		if(this.orderedRulesOption.isSet()){
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
		if(this.predictionFunctionOption.getChosenIndex()==0){
			StringUtils.appendIndented(out, indent, "The prediction function used: Adaptive");
			StringUtils.appendNewline(out);
		}
		if(this.predictionFunctionOption.getChosenIndex()==1){
			StringUtils.appendIndented(out, indent, "The prediction function used: Perceptron");
			StringUtils.appendNewline(out);
		}
		if(this.predictionFunctionOption.getChosenIndex()==2){
			StringUtils.appendIndented(out, indent, "The prediction function used: Target Mean");
			StringUtils.appendNewline(out);
		}
		StringUtils.appendIndented(out, indent, "Number of Rules: " + this.ruleSet.size()+1);
		StringUtils.appendNewline(out);		
	}
	
	/**
	 * Print to console
	 * @param string
	 */
    protected void debug(String string) {
    	if (verbose){
    		System.out.println(string); 
    	}
    }
    
    protected void VerboseToConsole(Instance inst){
    	if (verboseflag) 	{ // console print out number of rules. JG 30-12-2013.
			System.out.println(); // AC. 02-01-2013	
			//System.out.println("I) Dataset: "+inst.dataset().relationName()); // AC. CF. 02-01-2013
			
			if(this.orderedRulesOption.isSet()){ // AC. 02-01-2013
				System.out.println("I) Method Ordered");
			}else{
				System.out.println("I) Method Unordered");
			}
			//System.out.println("I) Number of Rules in the model: "+this.ruleSet.size()); 
			//System.out.println(); // AC. 02-01-2013			
			verboseflag = false;
		}    	
    }
    
    protected void PrintRuleSet(){    	
    	//int i = 0; // only for print.out
		for (Rule rule: ruleSet) {
			//i++; // only for print.out
			debug("rule in RuleSet:");
			rule.printRule();
			}
		}
    
    protected void PrintDefaultRule(){    	
		debug("DefaultRule:");
		defaultRule.printRule();		
		}
    
    
}
		
		


