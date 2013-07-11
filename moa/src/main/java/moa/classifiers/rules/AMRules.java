/*
 *    AMRules.java
 *    Copyright (C) 2013 University of Porto, Portugal
 *    @author E. Almeida, J. Gama
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
 *    
 */

package moa.classifiers.rules;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.core.attributeclassobservers.*;
import moa.classifiers.core.attributeclassobservers.BinaryTreeNumericAttributeClassObserverRegression.Node;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.options.FlagOption;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.options.MultiChoiceOption;
import weka.core.Instance;
import weka.core.Utils;
//import samoa.instances.Instance;
//import moa.core.Utils;
import java.math.BigDecimal;
import java.util.*; 

/**
 * Adaptive Model Rules (AMRules), the streaming rule learning algorithm for regression problems.
 * This algorithm learn ordered and unordered rule set from data stream. Each rule in AMRules use a 
 * Page-Hinkley test to detect changes in the processing generating data and react to changes by pruning the rule set.
 * This algorithm also does the detection of anomalies.
 * 
 * <p>Learning Adaptive Model Rules from High-Speed Data Streams, ECML 2013, E. Almeida, C. Ferreira, and J. Gama; </p>
 * Project Knowledge Discovery from Data Streams, FCT LIAAD-INESC TEC, 
 *
 * Contact: jgama@fep.up.pt
 *  
 * <p>Parameters:</p>
 * <ul>
 * <li> -h: The threshold value to be used in the Page Hinckley change detection tests.<li>
 * <li> -a: The alpha value to use in the Page Hinckley change detection tests.</li>
 * <li> -q: The number of instances a leaf should observe before permitting Naive Bayes.</li>
 * <li> -p: Minimum value of p </li>
 * <li> -t: Tie Threshold </li>
 * <li> -c: Split Confidence </li>
 * <li> -g: GracePeriod, the number of instances a leaf should observe between split attempts </li>
 * <li> -z: Prediction function to use. Ex: TargetMean </li>
 * <li> -r: learningRatio </li>
 * <li> -w: Learn ordered or unordered rule </li>
 * <li> -x: randomSeed<li>
 * </ul>
 * 
 * @author E. Almeida, J. Gama
 * @version $Revision: 1 $
 */


public class AMRules extends AbstractClassifier{
	
	private static final long serialVersionUID = 1L;
	
	protected AutoExpandVector<AttributeClassObserver> attributeObservers;
	
	protected ArrayList<ArrayList<Double>> saveBestValGlobalSDR = new ArrayList<ArrayList<Double>>(); // For each attribute contains the best value of SDR and its cutPoint.
	
	protected ArrayList<Double> saveTheBest = new ArrayList<Double>(); // Contains the best attribute.
	
	protected ArrayList<Rule> ruleSet = new ArrayList<Rule>();
	
	protected ArrayList<Rule> ruleSetAnomalies = new ArrayList<Rule>();
	
	protected ArrayList<Integer> ruleAnomaliesIndex = new ArrayList<Integer>();
	
	protected ArrayList<ArrayList<Integer>> caseAnomaly = new ArrayList<ArrayList<Integer>>();
	
	protected ArrayList<ArrayList<ArrayList<Double>>> ruleAttribAnomalyStatistics = new ArrayList<ArrayList<ArrayList<Double>>>();
	
	protected ArrayList<Double> targetValue = new ArrayList<Double>(); // Target value for each rule.
	
	protected ArrayList<Double> numTargetValue = new ArrayList<Double>(); // Number of target value seen by each rule.
	
	protected ArrayList<Double> ruleTargetMean = new ArrayList<Double>(); // Target mean of each rule.
	
	protected ArrayList<Double> ruleTargetMeanAnomalies = new ArrayList<Double>(); 
	
	protected double[] weightAttributeDefault; //The Perceptron weights. 
	
	protected DoubleVector saveBestGlobalSDR = new DoubleVector();   // For each attribute contains the best value of SDR.
	
	protected DoubleVector attributeStatisticsDefault = new DoubleVector(); // Statistic used for error calculations.
	
	protected DoubleVector squaredAttributeStatisticsDefault = new DoubleVector(); // Statistics used for  anomaly detection in the test data.
	
	protected DoubleVector attributesProbabilityDefault = new DoubleVector(); // Probalility of each attribute of default rule.
	
	protected Instance instance;
	
	protected boolean resetDefault=true; // If the model should be reset or not.
	
	// Statistics used for normalize actualClass and predictedClass.
	protected double actualClassStatisticsDefault = 0.0; 
	
	protected double squaredActualClassStatisticsDefault = 0.0;
	
	protected double sumTotalLeft; // The sum total of all target attibute values for instances falling to the left or on the split point.
	
	protected double sumTotalRight; // The sum total of all target attibute values for instances falling to the right of the split point.
	
	protected double sumSqTotalLeft; //The corresponding sums of squared target attribute values.
	
	protected double sumSqTotalRight;
	
	protected double rightTotal;
	
	protected double total;
	
	protected double maxSDR=0.0; //Standard deviation reduction of the best split.
	
	protected double splitpoint=0.0; //Value of the best split.
	
	protected double symbolTemp = 0.0;
	
	protected double symbol = 0.0;
	
	protected double sumTotalTemp = 0.0;
	
	protected double sumTotal = 0.0;
	
	protected double numSomaTotalTemp = 0.0;
	
	protected double numSomaTotal = 0.0;
	
	protected double learnRateDecay = 0.001;
	
	protected double initLearnRate = 0.1;
	
    protected int instancesSeenDefault = 0; // The number of instances seen by the DefaultRule.
	
	protected int instancesSeenDefaultTest = 0; // The number of test instances seen by the DefaultRule.
	
	protected int numInstTest = 0;
	
	protected int numInstance = 0;
	
	protected int maxInt=Integer.MAX_VALUE;

	Node root;
	
	Predicates pred;
	

	public static final double NORMAL_CONSTANT = Math.sqrt(2 * Math.PI);
	
	public FloatOption splitConfidenceOption = new FloatOption(
			"splitConfidence",
			'c',
			"The allowable error in split decision, values closer to 0 will take longer to decide.",
			0.0000001, 0.0, 1.0);

	public FloatOption tieThresholdOption = new FloatOption("tieThreshold",
			't', "Threshold below which a split will be forced to break ties.",
			0.05, 0.0, 1.0);
	
	public FloatOption pageHinckleyAlphaOption = new FloatOption(
			"PageHinckleyAlpha",
			'a',
			"The alpha value to use in the Page Hinckley change detection tests.",
			0.005, 0.0, 1.0); 
	
	public FloatOption anomalyProbabilityThresholdOption = new FloatOption(
			"anomalyprobabilityThreshold",
			'o',
			"The threshold value.",
			0.99, 0.0, 1.0);
	
	public FloatOption probabilityThresholdOption = new FloatOption(
			"probabilityThreshold",
			'k',
			"The threshold value.",
			0.10, 0.0, 1.0);

	public IntOption pageHinckleyThresholdOption = new IntOption(
			"PageHinckleyThreshold",
			'h',
			"The threshold value to be used in the Page Hinckley change detection tests.",
			50, 0, Integer.MAX_VALUE);
	
	public IntOption anomalyNumInstThresholdOption = new IntOption(
			"anomalyThreshold",
			'i',
			"The threshold value to be used in the anomaly detection.",
			15, 0, Integer.MAX_VALUE);

	public IntOption gracePeriodOption = new IntOption(
			"gracePeriod",'g', "The number of instances a leaf should observe between split attempts.",
			200, 0, Integer.MAX_VALUE);
	
	public FloatOption learningRatioOption = new FloatOption("learningRatio", 'w', "Learning ratio to use for training the Perceptrons in the leaves.", 0.01);
	
	public FloatOption seedOption = new FloatOption(
			"randomSeed",
			'x',
			"The alpha value to use in the Page Hinckley change detection tests.",
			100);  

	public MultiChoiceOption predictionFunctionOption = new MultiChoiceOption(
			"predictionFunctionOption", 'z', "The prediction function to use.", new String[]{
					"Adaptative","Perceptron", "Target Mean"}, new String[]{
					"Adaptative","Perceptron", "Target Mean"}, 0);
	
	public FlagOption orderedRulesOption = new FlagOption("orderedRules", 'r',
			"orderedRules.");
	
	public FlagOption anomalyDetectionOption = new FlagOption("anomalyDetection", 'u',
			"anomaly Detection.");
	
	public FlagOption learningRatio_Decay_or_Const_Option = new FlagOption("learningRatio_Decay_or_Const", '�',
			"learning Ratio Decay or const parameter.");
			
	@Override
	public boolean isRandomizable() {
		// TODO Auto-generated method stub
		return false; 
	}
	
	@Override
	public double[] getVotesForInstance(Instance inst) {
		double[] votes = new double[1];
		this.numInstTest = this.numInstTest + 1;
		switch (this.predictionFunctionOption.getChosenIndex()) { // Adaptative strategy.
		case 0:
		{
			if (this.orderedRulesOption.isSet()) { //Ordered rule.
				if (this.numInstTest > 100) {
					double perceptronPrediction = getVotesOrderedRulesPerceptron(inst);
					double targetMeanPrediction = getVotesOrderedRulesTargetMean(inst);
					double perceptronError = Math.abs(inst.classValue() - perceptronPrediction);
					double targetMeanError = Math.abs(inst.classValue() - targetMeanPrediction);
					if (perceptronError < targetMeanError) {
						votes[0] = perceptronPrediction;
					} else {
						votes[0] = targetMeanPrediction;
					}
					}else{
					votes[0] = getVotesOrderedRulesTargetMean(inst);
					}
				} else { //Unordered rule.
				if (this.numInstTest > 100){
					double perceptronPrediction = getVotesUnorderedRulesPerceptron(inst);
					double targetMeanPrediction = getVotesUnorderedRulesTargetMean(inst);
					double perceptronError = Math.abs(inst.classValue() - perceptronPrediction);
					double targetMeanError = Math.abs(inst.classValue() - targetMeanPrediction);
					if (perceptronError < targetMeanError) {
						votes[0] = perceptronPrediction;
					} else {
						votes[0] = targetMeanPrediction;
					}
					} else {
					votes[0] = getVotesUnorderedRulesTargetMean(inst);
					}
				}
			break;
			}
		case 1: // Perceptron strategy.
		{
			if (this.orderedRulesOption.isSet()) { //Ordered rule.
				if(this.numInstTest > 100) {
					votes[0] = getVotesOrderedRulesPerceptron(inst);
					} else {
						votes[0] = getVotesOrderedRulesTargetMean(inst);
						}
				} else {  //Unordered rule.
					if (this.numInstTest > 100) {
						votes[0] = getVotesUnorderedRulesPerceptron(inst);
						}else{
							votes[0] = getVotesUnorderedRulesTargetMean(inst);
							}
					}
			break;
			}
		case 2: // Target mean strategy.
		{
			if (this.orderedRulesOption.isSet()) {
				votes[0] = getVotesOrderedRulesTargetMean(inst);
				}else{
					votes[0] = getVotesUnorderedRulesTargetMean(inst);
					}
			break;
			}
		}
		return votes;
		}
	
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetLearningImpl() {
		// TODO Auto-generated method stub
		this.attributeObservers = new AutoExpandVector<AttributeClassObserver>();
	}
	
	@Override
	public void trainOnInstanceImpl(Instance inst) {
		this.numInstance = this.numInstance + 1;
		int countRuleFiredTrue = 0;
		boolean ruleFired = false;
		this.instance = inst;
		for (int j = 0; j < this.ruleSet.size(); j++) {
			if (this.ruleSet.get(j).ruleEvaluate(inst) == true) {
				countRuleFiredTrue = countRuleFiredTrue + 1;
				this.saveBestValGlobalSDR = new ArrayList<ArrayList<Double>>();
				this.saveBestGlobalSDR = new DoubleVector(); 
				this.saveTheBest = new ArrayList<Double>();
				double anomaly = computeAnomaly(this.ruleSet.get(j), j, inst); // compute anomaly
				if((this.ruleSet.get(j).instancesSeen <= this.anomalyNumInstThresholdOption.getValue()) || (anomaly < this.anomalyProbabilityThresholdOption.getValue() && this.anomalyDetectionOption.isSet()) ||!this.anomalyDetectionOption.isSet()){
				for (int i = 0; i < inst.numAttributes() - 1; i++) {
					int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
					AttributeClassObserver obs = this.ruleSet.get(j).observers.get(i);
					if (obs == null) {
						obs = inst.attribute(instAttIndex).isNominal() ? newNominalClassObserver()
								: newNumericClassObserverRegression();
						this.ruleSet.get(j).observers.set(i, obs);     
					}
					obs.observeAttributeTarget(inst.value(instAttIndex), inst.classValue());
					}
				
				double RuleError = computeRuleError(inst, this.ruleSet.get(j), j); // compute rule error
				boolean ph = PageHinckleyTest(RuleError, this.pageHinckleyThresholdOption.getValue(), this.ruleSet.get(j));
				if (ph == true) { //Page Hinckley test.
					//Pruning rule set.
				//	System.out.print("Pruning rule set \n");
					this.ruleSet.remove(j);
					this.targetValue.remove(j);
					this.numTargetValue.remove(j);
					this.ruleTargetMean.remove(j);
				} else {
					this.expandeRule(this.ruleSet.get(j), j, inst); //Expand the rule.
					}
			}
				if (this.orderedRulesOption.isSet()) { // Ordered rules
					break;
				}
			}
		}
		if (countRuleFiredTrue > 0) {
			ruleFired = true;
		}else{
			ruleFired = false;
		}
		if (ruleFired == false) { //Default rule
			this.saveBestValGlobalSDR = new ArrayList<ArrayList<Double>>();
			this.saveBestGlobalSDR = new DoubleVector();
			this.saveTheBest = new ArrayList<Double>();
			double anomalies = computeAnomalyDefaultRules(inst);
			if((instancesSeenDefault <= this.anomalyNumInstThresholdOption.getValue()) || (anomalies < this.anomalyProbabilityThresholdOption.getValue() && this.anomalyDetectionOption.isSet()) ||!this.anomalyDetectionOption.isSet()) {
			for (int i = 0; i < inst.numAttributes() - 1; i++) {
				int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
				AttributeClassObserver obs = this.attributeObservers.get(i);
				if (obs == null) {
					obs = inst.attribute(instAttIndex).isNominal() ? newNominalClassObserver()
							: newNumericClassObserverRegression();
					this.attributeObservers.set(i, obs); 
				}
				obs.observeAttributeTarget(inst.value(instAttIndex), inst.classValue());
			} 
			initialyPerceptron(inst); // Initialize Perceptron if necessary.  
			this.updateAttWeight(inst, this.weightAttributeDefault, this.squaredActualClassStatisticsDefault, 
					this.actualClassStatisticsDefault, this.squaredAttributeStatisticsDefault, 
					this.attributeStatisticsDefault, this.instancesSeenDefault, resetDefault); // Update weights. Ensure actual class and the predicted class are normalised first.
			this.updatedefaultRuleStatistics(inst); //Update the default rule statistics.
			this.createRule(inst);//This function creates a rule
			}
			}
		}


	@Override	
	public void getModelDescription(StringBuilder out, int indent) {
		if(this.anomalyDetectionOption.isSet()){
			this.getModelDescriptionAnomalyDetection(out, indent); //Anomaly detection
		}else{
			this.getModelDescriptionNoAnomalyDetection(out, indent); //No anomaly detection
			}
		}
	
	public void getModelDescriptionAnomalyDetection(StringBuilder out, int indent) { //ModelDescription anomaly detection
		StringUtils.appendNewline(out);
		for (int k = 0; k < this.ruleSetAnomalies.size(); k++) {
			StringUtils.appendIndented(out, indent, "Case: "+this.caseAnomaly.get(k).get(0)+"   Anomaly Score: "+this.caseAnomaly.get(k).get(1)+"%");
			StringUtils.appendNewline(out);
			if(this.ruleSetAnomalies.get(k).predicateSet.isEmpty()){
				StringUtils.appendNewline(out);
				StringUtils.appendIndented(out, indent, "Default Rule { } -> ");
				StringUtils.appendIndented(out, indent,"TargetAverage: "+round(this.ruleTargetMeanAnomalies.get(k))+"  ");
				StringUtils.appendNewline(out);
			}else{
			StringUtils.appendIndented(out, indent, "Rule "+this.ruleAnomaliesIndex.get(k)+": ");
			for (int i = 0; i < this.ruleSetAnomalies.get(k).predicateSet.size(); i++) {
				if (this.ruleSetAnomalies.get(k).predicateSet.size() == 1) {
					if (this.ruleSetAnomalies.get(k).predicateSet.get(i).getSymbol() == -1.0){
						String nam = this.instance.attribute((int)this.ruleSetAnomalies.get(k).predicateSet.get(i).getAttributeValue()).name();
						StringUtils.appendIndented(out, indent, nam+" <= "+round(this.ruleSetAnomalies.get(k).predicateSet.get(i).getValue())+" --> ");
						StringUtils.appendIndented(out, indent,"TargetAverage: "+round(this.ruleTargetMeanAnomalies.get(k))+"  ");
						StringUtils.appendNewline(out);
					} else {
						String nam = this.instance.attribute((int)this.ruleSetAnomalies.get(k).predicateSet.get(i).getAttributeValue()).name();
						StringUtils.appendIndented(out, indent, nam+" > "+round(this.ruleSetAnomalies.get(k).predicateSet.get(i).getValue())+" --> ");
						StringUtils.appendIndented(out, indent,"TargetAverage: "+round(this.ruleTargetMeanAnomalies.get(k))+"  ");
						StringUtils.appendNewline(out);
					}
				} else {
					if (this.ruleSetAnomalies.get(k).predicateSet.get(i).getSymbol()==-1.0){
						String nam = this.instance.attribute((int)this.ruleSetAnomalies.get(k).predicateSet.get(i).getAttributeValue()).name();
						StringUtils.appendIndented(out, indent, nam+" <= "+round(ruleSetAnomalies.get(k).predicateSet.get(i).getValue())+" ");
					} else {
						String nam = this.instance.attribute((int)this.ruleSetAnomalies.get(k).predicateSet.get(i).getAttributeValue()).name();
						StringUtils.appendIndented(out, indent, nam+" > "+round(this.ruleSetAnomalies.get(k).predicateSet.get(i).getValue())+" ");
					}
					if (i < this.ruleSetAnomalies.get(k).predicateSet.size() - 1) {
						StringUtils.appendIndented(out, indent, "and ");
					} else {
						StringUtils.appendIndented(out, indent, " --> ");
						StringUtils.appendIndented(out, indent,"TargetAverage: "+round(this.ruleTargetMeanAnomalies.get(k))+"  ");
						StringUtils.appendNewline(out);
					}
				}
			}
			}
			for(int z=0; z < this.ruleAttribAnomalyStatistics.get(k).size(); z++) {
				String s = String.format ("%.3e", this.ruleAttribAnomalyStatistics.get(k).get(z).get(4)); 
				StringUtils.appendIndented(out, indent, instance.attribute(this.ruleAttribAnomalyStatistics.get(k).get(z).get(0).intValue()).name()+"="+round(this.ruleAttribAnomalyStatistics.get(k).get(z).get(1))+"   ("+round(this.ruleAttribAnomalyStatistics.get(k).get(z).get(2))+" +- "+round(this.ruleAttribAnomalyStatistics.get(k).get(z).get(3))+")   P="+s);
				StringUtils.appendNewline(out);
			}
			StringUtils.appendNewline(out);
			StringUtils.appendNewline(out);
		}
			
	}
	
	public void getModelDescriptionNoAnomalyDetection(StringBuilder out, int indent) { //ModelDescription no anomaly detection
		StringUtils.appendNewline(out);
		StringUtils.appendIndented(out, indent, "Default Rule { } -> ");
		StringUtils.appendIndented(out, indent,"TargetAverage: "+round(observersDistrib(this.instance, this.attributeObservers))+"  ");
		StringUtils.appendNewline(out);
		StringUtils.appendIndented(out, indent, "Number of Rule: " + this.ruleSet.size());
		StringUtils.appendNewline(out);
		StringUtils.appendNewline(out);
		for (int k = 0; k < this.ruleSet.size(); k++) {
			StringUtils.appendIndented(out, indent, "Rule "+(k+1)+": ");
			for (int i = 0; i < this.ruleSet.get(k).predicateSet.size(); i++) {
				if (this.ruleSet.get(k).predicateSet.size() == 1) {
					if (this.ruleSet.get(k).predicateSet.get(i).getSymbol() == -1.0){
						String nam = this.instance.attribute((int)ruleSet.get(k).predicateSet.get(i).getAttributeValue()).name();
						StringUtils.appendIndented(out, indent, nam+" <= "+ruleSet.get(k).predicateSet.get(i).getValue()+" --> ");
						StringUtils.appendIndented(out, indent,"TargetAverage: "+round(this.ruleTargetMean.get(k))+"  ");
						StringUtils.appendNewline(out);
						StringUtils.appendNewline(out);
					} else {
						String nam = this.instance.attribute((int)this.ruleSet.get(k).predicateSet.get(i).getAttributeValue()).name();
						StringUtils.appendIndented(out, indent, nam+" > "+this.ruleSet.get(k).predicateSet.get(i).getValue()+" --> ");
						StringUtils.appendIndented(out, indent,"TargetAverage: "+round(this.ruleTargetMean.get(k))+"  ");
						StringUtils.appendNewline(out);
						StringUtils.appendNewline(out);
					}
				} else {
					if (this.ruleSet.get(k).predicateSet.get(i).getSymbol()==-1.0){
						String nam = this.instance.attribute((int)this.ruleSet.get(k).predicateSet.get(i).getAttributeValue()).name();
						StringUtils.appendIndented(out, indent, nam+" <= "+this.ruleSet.get(k).predicateSet.get(i).getValue()+" ");
					} else {
						String nam = this.instance.attribute((int)this.ruleSet.get(k).predicateSet.get(i).getAttributeValue()).name();
						StringUtils.appendIndented(out, indent, nam+" > "+this.ruleSet.get(k).predicateSet.get(i).getValue()+" ");
					}
					if (i < this.ruleSet.get(k).predicateSet.size() - 1) {
						StringUtils.appendIndented(out, indent, "and ");
					} else {
						StringUtils.appendIndented(out, indent, " --> ");
						StringUtils.appendIndented(out, indent,"TargetAverage: "+round(this.ruleTargetMean.get(k))+"  ");
						StringUtils.appendNewline(out);
						StringUtils.appendNewline(out);
					}
				}
			}
			StringUtils.appendNewline(out);
		}
	}
	
	//Compute the Standard deviation reduction.
	protected double computeSDR(){
		double standardDR = 0.0; // Standard deviation reduction.
		double sdS = 0.0;
		double sdSL = 0.0;
		double sdSR = 0.0;
		double NL = this.total - this.rightTotal;
		double NR = this.rightTotal;
		double N = NL + NR;
		double sumTotal = this.sumTotalLeft + this.sumTotalRight;
		double sumSqTotal = this.sumSqTotalLeft + this.sumSqTotalRight;
		sdS = Math.sqrt((1 / N) * (sumSqTotal - (1 / N) * Math.pow(sumTotal,2)));
		sdSL = Math.sqrt((1 / NL) * (this.sumSqTotalLeft - (1 / NL) * Math.pow(this.sumTotalLeft,2)));
		sdSR = Math.sqrt((1 / NR) * (this.sumSqTotalRight - (1 / NR) * Math.pow(this.sumTotalRight,2)));
		if (sdSL >= sdSR) {
			this.symbolTemp = -1.0;
			this.sumTotalTemp = this.sumTotalLeft;
			this.numSomaTotalTemp = NL;
		} else {
			this.symbolTemp = 1.0;
			this.sumTotalTemp = sumTotalRight;
			this.numSomaTotalTemp = NR;
		}
		standardDR = sdS - (NL / N) * sdSL - (NR / N) * sdSR;
		return standardDR;
	}
	
	//Get best and second best attributes
	protected double [] getBestSecondBestSDR(DoubleVector SDR){
		double[] SDRValues = new double[2];
		double best = 0.0;
		double secondBest = 0.0;
		for (int i = 0; i < SDR.numValues(); i++) {
			if (SDR.getValue(i) > best) {
				secondBest = best;
				best = SDR.getValue(i);
			} else{
				if (SDR.getValue(i) > secondBest) {
					secondBest = SDR.getValue(i);
				}
			}
			}
		SDRValues[0] = best;
		SDRValues[1] = secondBest;
		
		return SDRValues;
	}
	
	//Find the spit point with lower value SDR.
	protected void findBestSplit(Node root1) {
		if(root1.left != null) {
			findBestSplit(root1.left);
		}
		this.sumTotalLeft = this.sumTotalLeft + root1.lessThan[0];
		this.sumTotalRight = this.sumTotalRight - root1.lessThan[0];
		this.sumSqTotalLeft = this.sumSqTotalLeft + root1.lessThan[1];
		this.sumSqTotalRight = this.sumSqTotalRight - root1.lessThan[1];
		this.rightTotal = this.rightTotal - root1.lessThan[2];
		double standardDR = computeSDR();
		if(this.maxSDR < standardDR){
			this.maxSDR = standardDR;
			this.splitpoint = root1.cut_point;
			this.symbol = this.symbolTemp;
			this.sumTotal = this.sumTotalTemp;
			this.numSomaTotal = this.numSomaTotalTemp;
		}
		if(root1.right != null){
			findBestSplit(root1.right);
		}
		this.sumTotalLeft = this.sumTotalLeft - root1.lessThan[0];
		this.sumTotalRight = this.sumTotalRight + root1.lessThan[0];
		this.sumSqTotalLeft = this.sumSqTotalLeft - root1.lessThan[1];
		this.sumSqTotalRight = this.sumSqTotalRight + root1.lessThan[1];
		this.rightTotal = this.rightTotal + root1.lessThan[2];
	}
	
	// This function save all the informations about the best attribute.
	public void theBestAttributes(Instance instance, 
			AutoExpandVector<AttributeClassObserver> observersParameter) {
		for(int z = 0; z < instance.numAttributes() - 1; z++){
			int instAttIndex = modelAttIndexToInstanceAttIndex(z, instance);
			if(instance.attribute(instAttIndex).isNumeric()){
				this.root=((BinaryTreeNumericAttributeClassObserverRegression)observersParameter.get(z)).root1;
				this.sumTotalLeft = 0.0;
				this.sumTotalRight = this.root.lessThan[0] + this.root.greaterThan[0];
				this.sumSqTotalLeft = 0.0;
				this.sumSqTotalRight = this.root.lessThan[1] + this.root.greaterThan[1];
				this.rightTotal = this.total = this.root.lessThan[2] + this.root.greaterThan[2];
				this.maxSDR=0.0; 
				this.symbol=0.0;
				this.sumTotal=0.0;
				this.numSomaTotal=0.0;
				findBestSplit(this.root); // The best value (SDR) of a numeric attribute.
				ArrayList<Double> saveTheBestAtt = new ArrayList<Double>(); // Contains the best attribute.
				saveTheBestAtt.add(this.splitpoint);
				saveTheBestAtt.add(this.maxSDR);
				saveTheBestAtt.add(this.symbol);
				saveTheBestAtt.add(this.sumTotal);
				saveTheBestAtt.add(this.numSomaTotal);
				this.saveBestValGlobalSDR.add(saveTheBestAtt);
				this.saveBestGlobalSDR.setValue(z, this.maxSDR);
			}
		}
	}
	
	//Check if the best attribute is really the best.
	public boolean checkBestAttrib(double n) {
		boolean  isTheBest = false;
		double[] SDRValues = getBestSecondBestSDR(this.saveBestGlobalSDR);
		double bestSDR = SDRValues[0];
		double secondBestSDR = SDRValues[1];
		double range = Utils.log2(1);
		double hoeffdingBound = computeHoeffdingBound (range, this.splitConfidenceOption.getValue(), n);
		double r = secondBestSDR / bestSDR;
		double upperBound = r + hoeffdingBound;
		if ((upperBound < 1) || (hoeffdingBound < this.tieThresholdOption.getValue())) {
			for (int i = 0; i < this.saveBestValGlobalSDR.size(); i++) {
				if (bestSDR ==(this.saveBestValGlobalSDR.get(i).get(1))) {
					this.saveTheBest.add(this.saveBestValGlobalSDR.get(i).get(0)); 
					this.saveTheBest.add(this.saveBestValGlobalSDR.get(i).get(1));
					this.saveTheBest.add(this.saveBestValGlobalSDR.get(i).get(2));
					this.saveTheBest.add((double)i);
					this.saveTheBest.add(this.saveBestValGlobalSDR.get(i).get(3));
					this.saveTheBest.add(this.saveBestValGlobalSDR.get(i).get(4));
					break;
				}
			}
			isTheBest = true;
		} else {
			isTheBest = false;
		}
		return isTheBest;
	}

	//Hoeffding Bound 
	public  double computeHoeffdingBound(double range, double confidence,
			double n) {
		return Math.sqrt(((range * range) * Math.log(1.0 / confidence))
				/ (2.0 * n));
	}
	
	//Mean
	public  double computeMean(double sum, int size) {
		return sum / size;
	}
		
	//Standard Deviation
	public  double computeSD(double squaredVal, double val, int size) {
		return  Math.sqrt((squaredVal - ((val * val) / size)) / size);
	}
	
	//Attribute probability
	public double computeProbability(double mean, double sd, double value) {
		sd = sd + 0.00001;
		double probability = 0.0;
		double diff = value - mean;
		if (sd > 0.0) {
			 	double k = (Math.abs(value - mean)/sd);
			 	if (k > 1.0) {
			 		probability = 1.0/(k*k);
			 	}
			 	else {
			 		probability=  Math.exp(-(diff * diff / (2.0 * sd * sd)));
			 	}
		 }
		return probability;
	}
	
	//Compute anomalies
	public double computeAnomaly(Rule rl, int ruleIndex, Instance inst) {
		ArrayList<Integer> caseAnomalyTemp = new ArrayList<Integer>();
	    ArrayList<ArrayList<Double>> AttribAnomalyStatisticTemp2 = new ArrayList<ArrayList<Double>>();
	    double D = 0.0;
		double N = 0.0;
		if (rl.instancesSeen > this.anomalyNumInstThresholdOption.getValue() && this.anomalyDetectionOption.isSet()) { 
			for (int x = 0; x < inst.numAttributes() - 1; x++) {
				ArrayList<Double> AttribAnomalyStatisticTemp = new ArrayList<Double>();
				if (inst.attribute(x).isNumeric()) {
					double mean = computeMean(rl.attributeStatistics.getValue(x), rl.instancesSeen);
					double sd = computeSD(rl.squaredAttributeStatistics.getValue(x),rl.attributeStatistics.getValue(x), rl.instancesSeen);
					double probability = computeProbability(mean, sd, inst.value(x));
					if(probability!=0.0) {
						D = D + Math.log(probability);
						if(probability < this.probabilityThresholdOption.getValue()){  //0.10
							N = N + Math.log(probability);
		            	 	AttribAnomalyStatisticTemp.add((double)x);
		            	    AttribAnomalyStatisticTemp.add(inst.value(x));
			                AttribAnomalyStatisticTemp.add(mean);
			            	AttribAnomalyStatisticTemp.add(sd);
			            	AttribAnomalyStatisticTemp.add(probability);
			            	AttribAnomalyStatisticTemp2.add(AttribAnomalyStatisticTemp);
			            	}
						}
					}
				}
			}
		double anomaly = Math.abs(N/D);
		if(anomaly >= this.anomalyProbabilityThresholdOption.getValue()){
			caseAnomalyTemp.add(this.numInstance);
			double val = anomaly * 100;
			caseAnomalyTemp.add((int)val);
			this.caseAnomaly.add(caseAnomalyTemp);
			this.ruleSetAnomalies.add(this.ruleSet.get(ruleIndex));
			this.ruleTargetMeanAnomalies.add(this.ruleTargetMean.get(ruleIndex));
			this.ruleAnomaliesIndex.add(ruleIndex + 1);
			this.ruleAttribAnomalyStatistics.add(AttribAnomalyStatisticTemp2);
		}
		return anomaly;
	}
	
	//Compute anomalies DefaultRule
	public double computeAnomalyDefaultRules(Instance inst){
		double D = 0.0;
		double N = 0.0;
		ArrayList<Integer> caseAnomalyTemp = new ArrayList<Integer>();
	    ArrayList<ArrayList<Double>> AttribAnomalyStatisticTemp2 = new ArrayList<ArrayList<Double>>();
		if (this.instancesSeenDefault > this.anomalyNumInstThresholdOption.getValue() && this.anomalyDetectionOption.isSet()) {
			for (int x = 0; x < inst.numAttributes() - 1; x++) {
				ArrayList<Double> AttribAnomalyStatisticTemp = new ArrayList<Double>();
				if (inst.attribute(x).isNumeric()) {
					double mean = computeMean(this.attributeStatisticsDefault.getValue(x), this.instancesSeenDefault);
					double sd = computeSD(this.squaredAttributeStatisticsDefault.getValue(x), this.attributeStatisticsDefault.getValue(x), this.instancesSeenDefault);
					double probability = computeProbability(mean, sd, inst.value(x));
					if(probability!=0.0) {      
			         D = D + Math.log(probability);
			         if(probability < this.probabilityThresholdOption.getValue()){ //0.10
			             N = N + Math.log(probability);
			           	 AttribAnomalyStatisticTemp.add((double)x);
		            	 AttribAnomalyStatisticTemp.add(inst.value(x));
		            	 AttribAnomalyStatisticTemp.add(mean);
		            	 AttribAnomalyStatisticTemp.add(sd);
		            	 AttribAnomalyStatisticTemp.add(probability);
		            	 AttribAnomalyStatisticTemp2.add(AttribAnomalyStatisticTemp);
		            	 }
			         }
					}
				}
			}
		double anomalies = Math.abs(N/D);
		if(anomalies >= this.anomalyProbabilityThresholdOption.getValue()){
			caseAnomalyTemp.add(this.numInstance);
			double val = anomalies * 100;
			caseAnomalyTemp.add((int)val);
			this.caseAnomaly.add(caseAnomalyTemp);
			Rule rule = new Rule();
			this.ruleSetAnomalies.add(rule);
			this.ruleTargetMeanAnomalies.add(observersDistrib(this.instance, this.attributeObservers));
			this.ruleAnomaliesIndex.add(-1);
			this.ruleAttribAnomalyStatistics.add(AttribAnomalyStatisticTemp2);
		}
		return anomalies;
	}
	


		
	//Get the number of instances of an attribute.
	protected int observersNumberInstance(Instance inst, 
			AutoExpandVector<AttributeClassObserver> observerss) {
		int numberInstance = 0;
		for (int z = 0; z < inst.numAttributes() - 1; z++) {
			numberInstance = 0;
			int instAttIndex = modelAttIndexToInstanceAttIndex(z, inst);
			if (inst.attribute(instAttIndex).isNumeric()) {
				Node rootNode = ((BinaryTreeNumericAttributeClassObserverRegression) observerss.get(z)).root1;
				if (rootNode != null) {
					numberInstance = (int) (rootNode.lessThan[2] + rootNode.greaterThan[2]);
					break;
				}
			} 
		}
		return numberInstance;
	}
	
	//Reanicialized rule statistics
	protected void reanicializeRuleStatistic(Rule rl){
		rl.reset = false;
		rl.instancesSeen = 0;
		rl.actualClassStatistics = 0.0;
		rl.squaredActualClassStatistics = 0.0;
		rl.attributeStatistics = new DoubleVector();
		rl.squaredAttributeStatistics = new DoubleVector();
		rl.attributesProbability = new DoubleVector(); 
		rl.PHmT = 0;
		rl.PHMT = Double.MAX_VALUE;
		rl.XiSum = 0;  //Absolute error
	}

    // Output the prediction made by perceptron on the given instance and an set of rules
	public double prediction(Instance inst, double[] weightAtt, double squaredActualClassStatistics, 
			double actualClassStatistics, int instancesSeen, boolean reset) {
		double prediction = 0;
		if (reset == false) {
			for (int j = 0; j < inst.numAttributes() - 1; j++) {
				if (inst.attribute(j).isNumeric()) {
					prediction += weightAtt[j] * inst.value(j);
				}
			} 
			prediction += weightAtt[inst.numAttributes() - 1];
		}
		double sdPredictedClass = computeSD(squaredActualClassStatistics, actualClassStatistics, instancesSeen);
		double outputDesnorm=0;
		if(sdPredictedClass > 0.0000001){ 	
			outputDesnorm = 3 * prediction * sdPredictedClass + (actualClassStatistics / instancesSeen);
		}
		return outputDesnorm;
	}
	
	// Update weights. Ensure actual class and the predicted class are normalised first.
	public double  updateAttWeight(Instance inst, double[] weightAtt, double squaredActualClassStatistics, 
			double actualClassStatistics, DoubleVector squaredAttributeStatistics, 
			DoubleVector attributeStatistics, int instancesSeen, boolean reset){
		double learningRatio = 0.0;
		if(this.learningRatio_Decay_or_Const_Option.isSet()){ //Decaying learning rate option
			learningRatio = this.learningRatioOption.getValue();
		}else{
			learningRatio = initLearnRate / (1+ instancesSeen*this.learnRateDecay);
		}
		
		  double predict = 0.0;
		  if (instancesSeen > 30) {
          	predict = this.prediction(inst, weightAtt, squaredActualClassStatistics, 
          			actualClassStatistics, instancesSeen, reset);
          	double sdClass = computeSD(squaredActualClassStatistics, actualClassStatistics, instancesSeen);
				double actualClass=0.0;
				double predictedClass=0.0;
				if (sdClass > 0.0000001) {
					actualClass = (inst.classValue() - (actualClassStatistics / instancesSeen)) / (3 * sdClass);
					predictedClass = (predict - (actualClassStatistics / instancesSeen)) / (3 * sdClass);
				}
				double delta = actualClass - predictedClass;
				for (int x = 0; x < inst.numAttributes() - 1; x++) {
					if (inst.attribute(x).isNumeric()) {
						// Update weights. Ensure attribute values are normalised first.
						double sd = Math.sqrt((squaredAttributeStatistics.getValue(x)
								- ((attributeStatistics.getValue(x) * attributeStatistics.getValue(x)) / instancesSeen)) / instancesSeen);
						double instanceValue = 0;
						instanceValue = (inst.value(x) - (attributeStatistics.getValue(x) / instancesSeen));
						if (sd > 0.0000001) {
							instanceValue = instanceValue / (3*sd);
						}
						if (sd == 0.0) {
							weightAtt[x] = 0.0;
						} else {
							weightAtt[x] += learningRatio * delta * instanceValue;
						}
					}
				}
				weightAtt[inst.numAttributes() - 1] += learningRatio * delta;
			}
		  return predict;
	}
	
	// Get the predict value and the actual class value normalised
	public double [] getPredictionActualValueNormalized(Instance inst, Rule rl, int ruleIndex, double predict){
		double[] values = new double[2];
		double predictVal = 0.0;
		double classActual = 0.0;
		double sd = computeSD(rl.squaredActualClassStatistics, rl.actualClassStatistics, rl.instancesSeen);
		if (this.predictionFunctionOption.getChosenIndex() == 2) { //Target mean strategy
			
			predictVal = (this.ruleTargetMean.get(ruleIndex) - (rl.actualClassStatistics / rl.instancesSeen)) / (3 * sd);
			classActual = (inst.classValue() - (rl.actualClassStatistics / rl.instancesSeen)) / (3 * sd);	
			} else if (this.predictionFunctionOption.getChosenIndex() == 1) { //Perceptron strategy
			if (rl.instancesSeen <= 30) { 
				if (sd > 0.0000001) {
					predictVal = (ruleTargetMean.get(ruleIndex) - (rl.actualClassStatistics / rl.instancesSeen)) / (3 * sd); //Predicted value normalized.
					classActual = (inst.classValue() - (rl.actualClassStatistics / rl.instancesSeen)) / (3 * sd);	//Class value normalized.
				}
			} else {
				if (sd > 0.0000001) {
					predictVal = (predict - (rl.actualClassStatistics / rl.instancesSeen)) / (3 * sd); //Predicted value normalized.
				    classActual = (inst.classValue() - (rl.actualClassStatistics / rl.instancesSeen)) / (3 * sd); //Class value normalized.
				}
			}
		} else { //Adaptative strategy.
			double predictValTargetMean = 0;
			double predictValPerceptron = 0;
				if (sd > 0.0000001) {
					predictValTargetMean = (this.ruleTargetMean.get(ruleIndex) - (rl.actualClassStatistics / rl.instancesSeen)) / (3 * sd);
				    predictValPerceptron = (predict - (rl.actualClassStatistics / rl.instancesSeen)) / (3 * sd);
				    classActual = (inst.classValue() - (rl.actualClassStatistics / rl.instancesSeen)) / (3 * sd);
				}
				double absolutErrorTargetMean = Math.abs(classActual - predictValTargetMean); //Target mean strategy absolute error.
				double absolutErrorPerceptron = Math.abs(classActual - predictValPerceptron); //Perceptron strategy absolute error.
				if (absolutErrorTargetMean < absolutErrorPerceptron) {
					predictVal = predictValTargetMean;
				} else {
					predictVal = predictValPerceptron;
				}
			}
		values[0] = predictVal;
		values[1] = classActual;
		
		return values;
	}
	
	// Update rule statistics
	public void updateRuleStatistics(Instance inst, Rule rl, int ruleIndex){
		rl.instancesSeen++; 
		double targetValueSize = this.numTargetValue.get(ruleIndex) + 1.0;
		double targetVal = this.targetValue.get(ruleIndex) + inst.classValue();
		this.targetValue.set(ruleIndex, targetVal);  
		this.numTargetValue.set(ruleIndex, targetValueSize);
		setRuleTarget(this.targetValue.get(ruleIndex), this.numTargetValue.get(ruleIndex), ruleIndex);
		rl.ValorTargetRule = this.ruleTargetMean.get(ruleIndex);
		for (int s = 0; s < inst.numAttributes() -1; s++) {
			rl.attributeStatistics.addToValue(s, inst.value(s));	
			rl.squaredAttributeStatistics.addToValue(s, inst.value(s) * inst.value(s));
		}
		rl.actualClassStatistics += inst.classValue();
		rl.squaredActualClassStatistics += inst.classValue() * inst.classValue();
	}
	
	//Get rule error
	public double computeRuleError(Instance inst, Rule rl, int ruleIndex){//
		double predict = this.updateAttWeight(inst, rl.weightAttribute, rl.squaredActualClassStatistics,
				rl.actualClassStatistics, rl.squaredAttributeStatistics, 
				rl.attributeStatistics, rl.instancesSeen, rl.reset);
		double[] values = getPredictionActualValueNormalized(inst, rl, ruleIndex, predict);
		double predictVal = values[0];
		double classActual = values[1];
		
		this.updateRuleStatistics(inst, rl, ruleIndex);  // Update rule statistics
		double xi = Math.abs(classActual - predictVal); // Absolute error.
		rl.XiSum += xi;
		double RuleError = xi - (rl.XiSum / rl.instancesSeen) - this.pageHinckleyAlphaOption.getValue();
		
		return RuleError;
	}
	
	//This function adds a predicate to a rule and updates the statistics of this rule
	public void AddPredUpdateRuleStatistics(Rule rl, int RuleIndex, double targetValorTotal, double contaTargetValorTotal){
		rl.predicateSet.add(this.pred);
		this.targetValue.set(RuleIndex, targetValorTotal);  
		this.numTargetValue.set(RuleIndex, contaTargetValorTotal);
		setRuleTarget(this.targetValue.get(RuleIndex), this.numTargetValue.get(RuleIndex), RuleIndex);
		rl.ValorTargetRule = this.ruleTargetMean.get(RuleIndex);
		reanicializeRuleStatistic(rl);
		rl.observers=new AutoExpandVector<AttributeClassObserver>();
		}
	
	public void expandeRule(Rule rl, int ruleIndex, Instance inst){
		 ruleTargetData(inst, ruleIndex); // For each rule gets the respective target sum.
		 int remainder = (int)Double.MAX_VALUE;
		 int numInstanciaObservers = observersNumberInstance( inst, rl.observers); // Number of instances for this rule observers.
		    if (numInstanciaObservers != 0 && this.gracePeriodOption.getValue() != 0) {
		    	remainder = (numInstanciaObservers) % (this.gracePeriodOption.getValue());
		    }
	    if (remainder == 0) {
		    theBestAttributes(inst, rl.observers); // The best value of SDR for each attribute.
		    boolean bestAttribute = checkBestAttrib(numInstanciaObservers);	// Check if the best attribute value is really the best.
		    if (bestAttribute == true) {
		    	double attributeValue = this.saveTheBest.get(3);
		    	double symbol = this.saveTheBest.get(2);	 // <=, > (-1.0, 1.0).
		    	double value = this.saveTheBest.get(0);  // Value of the attribute.
		    	double targetValorTotal = this.saveTheBest.get(4);
		    	double contaTargetValorTotal = this.saveTheBest.get(5);
		    	this.pred = new Predicates(attributeValue, symbol, value);
		    	int countPred = 0;
		    	for (int i = 0; i < rl.predicateSet.size(); i++) { // Checks if the new predicate is not yet in the predicateSet. 
		    			if (rl.predicateSet.get(i).getAttributeValue() != this.pred.getAttributeValue() 
		    					|| rl.predicateSet.get(i).getSymbol() != this.pred.getSymbol() 
		    					|| rl.predicateSet.get(i).getValue() != this.pred.getValue()) {
		    				countPred = countPred+1;
		    			}
		    		}
		    	if (countPred == rl.predicateSet.size()) {
		    		int countDifPred = 0;
		    		ArrayList<Predicates> predicSetTemp = new ArrayList<Predicates>();
		    		for (int x = 0; x < rl.predicateSet.size(); x++) {
		    			predicSetTemp.add(rl.predicateSet.get(x));
		    		}
		    		predicSetTemp.add(this.pred);
		    		for (int f = 0; f < this.ruleSet.size(); f++) {
					int countDifPredTemp = 0;
					if (this.ruleSet.get(f).predicateSet.size() == predicSetTemp.size()) {
						for(int x = 0; x < this.ruleSet.get(f).predicateSet.size(); x++) {
							if (this.ruleSet.get(f).predicateSet.get(x).getAttributeValue() == predicSetTemp.get(x).getAttributeValue() 
									&& this.ruleSet.get(f).predicateSet.get(x).getSymbol() == predicSetTemp.get(x).getSymbol() 
									&& this.ruleSet.get(f).predicateSet.get(x).getValue() == predicSetTemp.get(x).getValue()) {
								countDifPredTemp = countDifPredTemp + 1;
							}
						}
						if (countDifPredTemp == predicSetTemp.size()) {
							break;
						}else{
							countDifPred = countDifPred + 1;
							}
					}else{
						countDifPred = countDifPred + 1;
					}
				}
				if (countDifPred == this.ruleSet.size()) {
					 if (this.pred.getSymbol() == 1.0) {
						int countIqualPred = 0;
						for (int f = 0; f < rl.predicateSet.size(); f++) {
							if (this.pred.getAttributeValue() == rl.predicateSet.get(f).getAttributeValue()
									&& this.pred.getSymbol() == rl.predicateSet.get(f).getSymbol()) {
								countIqualPred = countIqualPred + 1;
								if (this.pred.getValue() > rl.predicateSet.get(f).getValue()) {
									rl.predicateSet.remove(f);
									AddPredUpdateRuleStatistics(rl, ruleIndex, targetValorTotal, contaTargetValorTotal);
								}
							}
						}
						if (countIqualPred == 0) {
							AddPredUpdateRuleStatistics(rl, ruleIndex, targetValorTotal, contaTargetValorTotal);
						}
					} else {
						int countIqualPred = 0;
						for (int f = 0; f < rl.predicateSet.size(); f++) {
							if (this.pred.getAttributeValue() == rl.predicateSet.get(f).getAttributeValue()
									&& pred.getSymbol() == rl.predicateSet.get(f).getSymbol()) {
								countIqualPred = countIqualPred + 1;
								if (this.pred.getValue() < rl.predicateSet.get(f).getValue()) {
									rl.predicateSet.remove(f);
									AddPredUpdateRuleStatistics(rl, ruleIndex, targetValorTotal, contaTargetValorTotal);
								}
							}
						}
						if (countIqualPred == 0) {
							AddPredUpdateRuleStatistics(rl, ruleIndex, targetValorTotal, contaTargetValorTotal);
						}
					}
				}
			}
		    }
		   }
	}
	
	
	//Check to see if the set of rules needs updating.
	public boolean PageHinckleyTest(double error, double threshold, Rule rl) {	
		rl.PHmT += error; // Update the cumulative mT sum
		if (rl.PHmT < rl.PHMT) { // Update the minimum mT value if the new mT is smaller than the current minimum
			rl.PHMT = rl.PHmT;
		}
		return rl.PHmT - rl.PHMT > threshold; // Return true if the cumulative value - the current minimum is greater than the current threshold
	}
	
	//This function This expands the rule
		public void expandeRule(Rule rl, Instance inst) {
			
		}

	//Update the rule target value
	protected void ruleTargetData(Instance inst, int ruleIndex) {
		double target = this.targetValue.get(ruleIndex) + inst.classValue();
		double targetCount = this.numTargetValue.get(ruleIndex) + 1;
		this.targetValue.set(ruleIndex, target);
		this.numTargetValue.set(ruleIndex, targetCount);
	}
	
	protected void getRuleTarget(double sum, double count, int index) {
		double value = sum / count;
		this.ruleTargetMean.add(value);
	}
	
	protected void setRuleTarget(double sum, double count, int index) {
			double value = sum /count;
			this.ruleTargetMean.set(index, value);
			}
	
	//Initialize perceptron if is necessary
	public void initialyPerceptron(Instance inst){
		if (this.resetDefault == true) {
			this.resetDefault = false;
			this.weightAttributeDefault = new double[inst.numAttributes()];
			this.instancesSeenDefault = 0;
			this.actualClassStatisticsDefault = 0.0;
			this.squaredActualClassStatisticsDefault = 0.0;
			this.attributeStatisticsDefault = new DoubleVector();
			this.squaredAttributeStatisticsDefault = new DoubleVector();
			this.attributesProbabilityDefault = new DoubleVector();
			Random r = new Random();
			long value = (long)seedOption.getValue();
			r.setSeed(value);
				for (int j = 0; j < inst.numAttributes(); j++) {
					this.weightAttributeDefault[j] = 2 * r.nextDouble() - 1;
				}	
		}
	}
	
	//Update the default rule statistics.
	public void updatedefaultRuleStatistics(Instance inst){
		this.instancesSeenDefault++;
		for (int j = 0; j < inst.numAttributes() -1; j++) {
			this.attributeStatisticsDefault.addToValue(j, inst.value(j));	
			this.squaredAttributeStatisticsDefault.addToValue(j, inst.value(j) * inst.value(j));
		}
		this.actualClassStatisticsDefault += inst.classValue();
		this.squaredActualClassStatisticsDefault += inst.classValue() * inst.classValue();
	}
	
	//This function creates a rule
	public void createRule(Instance inst){
		int remainder = (int)Double.MAX_VALUE;
		int numInstanciaObservers = observersNumberInstance( inst, this.attributeObservers);
		if (numInstanciaObservers != 0 && this.gracePeriodOption.getValue() != 0) {
			remainder = (numInstanciaObservers) % (this.gracePeriodOption.getValue());
		}
		if (remainder == 0) {
			theBestAttributes(inst, this.attributeObservers);
			boolean bestAttribute = checkBestAttrib(numInstanciaObservers);	// Check if the best attribute value is really the best.
			if (bestAttribute == true) {
				double attributeValue = this.saveTheBest.get(3);
				double symbol = this.saveTheBest.get(2);		// <=, > : (0.0, -1.0, 1.0).
				double value = this.saveTheBest.get(0);		// Value of the attribute
				double targetValorTotal = this.saveTheBest.get(4);
				double contaTargetValorTotal = this.saveTheBest.get(5);
				this.pred = new Predicates(attributeValue, symbol, value);
				Rule Rl = new Rule();		// Create new rule.
				Rl.predicateSet.add(pred);
				Rl.weightAttribute = new double[inst.numAttributes()];
				System.arraycopy(this.weightAttributeDefault, 0, Rl.weightAttribute, 0, this.weightAttributeDefault.length); //Initialize the rule array of weights.
				reanicializeRuleStatistic(Rl); //Initialize the others statistics of the rule.
				this.ruleSet.add(Rl);
				this.targetValue.add(targetValorTotal);
				this.numTargetValue.add(contaTargetValorTotal);
				getRuleTarget(this.targetValue.get(ruleSet.size()-1), this.numTargetValue.get(ruleSet.size()-1), this.ruleSet.size()-1);
				Rl.ValorTargetRule = this.ruleTargetMean.get(this.ruleSet.size()-1);
				this.attributeObservers = new AutoExpandVector<AttributeClassObserver>();
				}
			}	
		}
	
	// Get the mean of observer distributions
	protected double observersDistrib(Instance inst, AutoExpandVector<AttributeClassObserver> observerss) {
		double votes = 0.0;
		for (int z = 0; z < inst.numAttributes() - 1; z++) {
			int instAttIndex = modelAttIndexToInstanceAttIndex(z, inst);
			if (inst.attribute(instAttIndex).isNumeric()) {
				if(observerss.get(z) != null){
					Node rootNode = ((BinaryTreeNumericAttributeClassObserverRegression) observerss.get(z)).root1;
					if (rootNode != null) {
						double sum = rootNode.greaterThan[0] + rootNode.lessThan[0];
						double numTarget = rootNode.greaterThan[2] + rootNode.lessThan[2];
						votes = sum / numTarget;
						break;
					}
				}
			}
		}
		return votes;
	}
	
	// The following functions are used for the prediction 
	protected double getVotesUnorderedRulesTargetMean(Instance inst) { //Unordered Rules Target Mean prediction
		double votes = 0.0;
		double sum = 0.0;
		boolean fired = false;
		int countFired = 0;
		int count = 0;
		for (int j = 0; j < this.ruleSet.size(); j++) {
			if (this.ruleSet.get(j).ruleEvaluate(inst) == true) {
				countFired = countFired + 1;
				double value = this.ruleSet.get(j).ValorTargetRule;
				sum = sum + value;
				count = count + 1;
			}
		}
		if (countFired > 0) {
			fired = true;
			votes = sum / count;
		} else {
			fired = false;
		}
		if (fired == false) {
			votes = observersDistrib(inst, this.attributeObservers);
		}
		return votes;
	}
	
	protected double getVotesOrderedRulesTargetMean(Instance inst) { //Ordered Rules Target Mean prediction
		double votes = 0.0;
		boolean fired = false;
		int countFired = 0;
		for (int j = 0; j < this.ruleSet.size(); j++) {
			if (this.ruleSet.get(j).ruleEvaluate(inst) == true) {
				countFired = countFired + 1;
				double value = this.ruleSet.get(j).ValorTargetRule;
				votes = value;
				break;
			}
		}
		if (countFired > 0) {
			fired = true;
		} else {
			fired = false;
		}
		if (fired == false) {
			votes = observersDistrib(inst, this.attributeObservers);
		}
		return votes;
	}
	
	protected double getVotesUnorderedRulesPerceptron(Instance inst) { //Unordered Rules Perceptron prediction
		double votes = 0.0;
		double sum = 0.0;
		boolean fired = false;
		int countFired = 0;
		int count = 0;
		for (int j = 0; j < this.ruleSet.size(); j++) {
			if (this.ruleSet.get(j).ruleEvaluate(inst) == true) {
				countFired = countFired + 1;
				double value = this.prediction(inst, this.ruleSet.get(j).weightAttribute, this.ruleSet.get(j).squaredActualClassStatistics, 
						this.ruleSet.get(j).actualClassStatistics, this.ruleSet.get(j).instancesSeen, this.ruleSet.get(j).reset);
				sum = sum + value;
				count = count + 1;
			}
		}
		if (countFired > 0) {
			fired = true;
			votes= sum / count;
		} else {
			fired = false;
		}
		if (fired == false) {
			votes = this.prediction(inst, this.weightAttributeDefault, this.squaredActualClassStatisticsDefault, 
					this.actualClassStatisticsDefault, this.instancesSeenDefault, this.resetDefault);
		}
		return votes;
	}
	
	protected double getVotesOrderedRulesPerceptron(Instance inst) { //Ordered Rules Perceptron prediction
		double votes = 0.0;
		boolean fired = false;
		int countFired = 0;
		for (int j = 0; j < this.ruleSet.size(); j++) {
			if (this.ruleSet.get(j).ruleEvaluate(inst) == true) {
				countFired = countFired + 1;
				double value = this.prediction(inst, this.ruleSet.get(j).weightAttribute, this.ruleSet.get(j).squaredActualClassStatistics, 
						this.ruleSet.get(j).actualClassStatistics, this.ruleSet.get(j).instancesSeen, this.ruleSet.get(j).reset);
				votes = value;
				break;
			}
		}
		if (countFired > 0) {
			fired = true;
		} else {
			fired = false;
		}
		if (fired == false) {
			votes= this.prediction(inst, this.weightAttributeDefault, this.squaredActualClassStatisticsDefault, 
					this.actualClassStatisticsDefault, this.instancesSeenDefault, this.resetDefault);
		}
		return votes;
	}
	
	protected AttributeClassObserver newNominalClassObserver() {
		return new NominalAttributeClassObserver();
	}

	protected AttributeClassObserver newNumericClassObserver() {
		return new BinaryTreeNumericAttributeClassObserver();
	}

	protected AttributeClassObserver newNumericClassObserverRegression() {
		return new BinaryTreeNumericAttributeClassObserverRegression();
	}
	
	//Round an number
	protected BigDecimal round(double val){
		BigDecimal value = new BigDecimal(val);
		if(val!=0.0){
			value = value.setScale(3, BigDecimal.ROUND_DOWN);
		}
		return value;
	}
	
	//Round an number
		protected BigDecimal roundValue(double val){
			BigDecimal value = new BigDecimal(val);
			if(val!=0.0){
				value = value.setScale(0, BigDecimal.ROUND_DOWN);
			}
			return value;
		}
		
}
		
		


