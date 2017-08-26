/*
 *    RuleActiveRegressionNode.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.rules.AMRulesRegressorOld;
import moa.classifiers.rules.core.splitcriteria.AMRulesSplitCriterion;
import moa.classifiers.rules.functions.Perceptron;
import moa.classifiers.rules.functions.TargetMean;
import moa.core.DoubleVector;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * A modified ActiveLearningNode that uses a Perceptron as the leaf node model,
 * and ensures that the class values sent to the attribute observers are not
 * truncated to ints if regression is being performed
 */
public class RuleActiveRegressionNode extends RuleActiveLearningNode{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1607453624545272049L;

	protected Perceptron perceptron;
	
	public Perceptron getPerceptron() {
		return perceptron;
	}

	public void setPerceptron(Perceptron perceptron) {
		this.perceptron = perceptron;
	}

	public TargetMean getTargetMean() {
		return targetMean;
	}

	public void setTargetMean(TargetMean targetMean) {
		this.targetMean = targetMean;
	}

	protected TargetMean targetMean;
	public RuleActiveRegressionNode(double[] initialClassObservations) {
		super(initialClassObservations);
	}

	public RuleActiveRegressionNode() {
		this(new double[0]);
	}

	public RuleActiveRegressionNode(Rule.Builder builder) {
		super(builder);
		this.perceptron = new Perceptron();
		this.perceptron.prepareForUse();
		this.perceptron.learningRatioOption = ((AMRulesRegressorOld)this.amRules).learningRatioOption;
		this.perceptron.constantLearningRatioDecayOption = ((AMRulesRegressorOld)this.amRules).constantLearningRatioDecayOption;	


		if(this.predictionFunction!=1)
		{
			this.targetMean = new TargetMean(); 
			if (builder.statistics[0]>0)
				this.targetMean.reset(builder.statistics[1]/builder.statistics[0],(long)builder.statistics[0]);
		}
		this.predictionFunction = builder.predictionFunction;
		if (builder.statistics!=null)
			this.nodeStatistics=new DoubleVector(builder.statistics);
	}

	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#updateStatistics(weka.core.Instance)
	 */
	public void updateStatistics(Instance instance) {
		super.updateStatistics(instance);
		this.perceptron.trainOnInstance(instance);
		if (this.predictionFunction != 1) { //Train target mean if prediction function is not Perceptron
			this.targetMean.trainOnInstance(instance);
		}
	}

	public double[] getPrediction(Instance instance, int predictionMode) {
		double[] ret = new double[1];  
		if (predictionMode == 1)
			ret=this.perceptron.getVotesForInstance(instance);
		else
			ret=this.targetMean.getVotesForInstance(instance);
		return ret;
	}

	public double getNormalizedPrediction(Instance instance) {
		double res;
		double [] aux;
		switch (this.predictionFunction) {
		//perceptron - 1
		case 1:
			res=this.perceptron.normalizedPrediction(instance); ;
			break;
			//target mean - 2
		case 2:
			aux=this.targetMean.getVotesForInstance((Instance)null);
			res=normalize(aux[0]);
			break;
			//adaptive	- 0
		case 0:  
			int predictionMode = this.getLearnerToUse(instance, 0);
			if(predictionMode == 1)
			{
				res=this.perceptron.normalizedPrediction(instance);
			}
			else{
				aux=this.targetMean.getVotesForInstance(instance);
				res = normalize(aux[0]); 
			}
			break;
		default: 
			throw new UnsupportedOperationException("Prediction mode not in range.");
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getLearnerToUse(weka.core.Instance, int)
	 */
	public int getLearnerToUse(Instance instance, int predMode) {
		int predictionMode = predMode;
		if (predictionMode == 0) {
			double 	perceptronError= this.perceptron.getCurrentError();
			double meanTargetError =this.targetMean.getCurrentError();
			debug("\n Check P:" + perceptronError + " M:" + meanTargetError,5);
			debug("Rule" + this.owner.ruleNumberID + " P:" + this.perceptron.getVotesForInstance(instance)[0] + " (" + perceptronError + ")" + " M:" + this.targetMean.getVotesForInstance(instance)[0]+ " (" + meanTargetError + ")",3) ; //Commented by JD
			debug("Observed Value: " + instance.classValue(),5);
			if (perceptronError < meanTargetError) {
				predictionMode = 1; //PERCEPTRON
			} else {
				predictionMode = 2; //TARGET MEAN
			}
		}
		return predictionMode;
	}


	private double normalize(double value) {
		double meanY = this.nodeStatistics.getValue(1)/this.nodeStatistics.getValue(0);
		double sdY = computeSD(this.nodeStatistics.getValue(2), this.nodeStatistics.getValue(1), (long)this.nodeStatistics.getValue(0));
		double normalizedY = 0.0;
		if (sdY > 0.0000001) {
			normalizedY = (value - meanY) / (sdY);
		}
		return normalizedY;
	}


	public double computeSD(double squaredVal, double val, double size) {
		if (size > 1) {
			return Math.sqrt((squaredVal - ((val * val) / size)) / (size - 1.0));
		}
		return 0.0;
	}
	public double computeSD(double squaredVal, double val, long size) {

		return computeSD(squaredVal, val, (double)size);
	}


	public double computeError(Instance instance) {
		double normalizedPrediction = getNormalizedPrediction(instance); 
		double normalizedClassValue = normalize(instance.classValue());
		return Math.abs(normalizedClassValue - normalizedPrediction);
	}
	public boolean isAnomaly(Instance instance,
			double uniVariateAnomalyProbabilityThreshold,
			double multiVariateAnomalyProbabilityThreshold,
			int numberOfInstanceesForAnomaly) {
		//AMRUles is equipped with anomaly detection. If on, compute the anomaly value.
		double perceptronIntancesSeen=this.perceptron.getInstancesSeen();
		if ( perceptronIntancesSeen>= numberOfInstanceesForAnomaly) {
			double atribSum = 0.0;
			double atribSquredSum = 0.0;
			double D = 0.0;
			double N = 0.0;
			double anomaly = 0.0;

			for (int x = 0; x < instance.numAttributes() - 1; x++) {
				// Perceptron is initialized each rule.
				// this is a local anomaly.
				int instAttIndex = AMRulesRegressorOld.modelAttIndexToInstanceAttIndex(x, instance);
				atribSum = this.perceptron.perceptronattributeStatistics.getValue(x);
				atribSquredSum = this.perceptron.squaredperceptronattributeStatistics.getValue(x);
				double mean = atribSum / perceptronIntancesSeen;
				double sd = computeSD(atribSquredSum, atribSum, perceptronIntancesSeen);
				double probability = computeProbability(mean, sd, instance.value(instAttIndex));
				
				/* old implementation
				if (probability > 0.0) {
					D = D + Math.abs(Math.log(probability));
					if (probability < uniVariateAnomalyProbabilityThreshold) {//0.10
						N = N + Math.abs(Math.log(probability));
					}
				} else {
					debug("Anomaly with probability 0 in atribute : " + x, 4);
				}*/
				
				//odds ratio
				if(probability>0)
					anomaly+=Math.log(probability/(1-probability));
				if(probability==1) //TODO: JD comment: only for testing
					anomaly+=Math.log(probability/(1-probability));
			}

			/*anomaly = 0.0; //Old implementation
			if (D != 0.0) { 
				anomaly = N / D;
			}
			if (anomaly >= multiVariateAnomalyProbabilityThreshold) {
				debuganomaly(instance,
						uniVariateAnomalyProbabilityThreshold,
						multiVariateAnomalyProbabilityThreshold,
						anomaly);
				return true;
			}*/
			System.out.println("Anomaly = " + anomaly); //TODO: JD remove commented code
			/*try {
			    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/home/jduarte/fried_anomalies.txt", true)));
			    out.println(anomaly);
			    out.close();
			} catch (IOException e) {
			    //exception handling left as an exercise for the reader
			}*/
			return anomaly<0;
		}
		return false;
	}
	protected void debuganomaly(Instance instance, double uni, double multi, double probability) {
		double atribSum = 0.0;
		double atribSquredSum = 0.0;

		for (int x = 0; x < instance.numAttributes() - 1; x++) {
			int instAttIndex = AMRulesRegressorOld.modelAttIndexToInstanceAttIndex(x, instance);
			atribSum = perceptron.perceptronattributeStatistics.getValue(x);
			atribSquredSum = perceptron.squaredperceptronattributeStatistics.getValue(x);
			double mean = atribSum / perceptron.getInstancesSeen();
			double sd = computeSD(
					atribSquredSum,
					atribSum,
					perceptron.getInstancesSeen()
					);
			debug("Attribute : " + x, 5);
			debug("Value : " + instance.value(instAttIndex), 5);
			debug("Mean : " + mean, 5);
			debug("SD : " + sd, 5);
			debug("Probability : " + probability, 5);
			debug("Univariate : " + uni, 5);
			debug("Multivariate : " + multi, 5);
			debug("Anomaly in rule :" + this.owner.ruleNumberID, 5);
		}
	}
	public void initialize(RuleActiveLearningNode oldLearningNode) {

		if(((RuleActiveRegressionNode) oldLearningNode).perceptron!=null)
		{
			this.perceptron=new Perceptron(((RuleActiveRegressionNode) oldLearningNode).perceptron);
			this.perceptron.resetError();
			this.perceptron.setLearningRatio(((AMRulesRegressorOld)this.amRules).learningRatioOption.getValue());
		}

		if(((RuleActiveRegressionNode) oldLearningNode).targetMean!=null)
		{
			this.targetMean= new TargetMean(((RuleActiveRegressionNode) oldLearningNode).targetMean);
			this.targetMean.resetError();
		}
		//reset statistics
		this.nodeStatistics.setValue(0, 0);
		this.nodeStatistics.setValue(1, 0);
		this.nodeStatistics.setValue(2, 0);

	}

	public double[] getSimplePrediction() {
		if( this.targetMean!=null)
			return this.targetMean.getVotesForInstance((Instance) null);
		else
			return new double[]{0};

	}

	public boolean tryToExpand(double splitConfidence, double tieThreshold) {

		// splitConfidence. Hoeffding Bound test parameter.
		// tieThreshold. Hoeffding Bound test parameter.
		//SplitCriterion splitCriterion = new SDRSplitCriterionAMRules(); 
			//SplitCriterion splitCriterion = new SDRSplitCriterionAMRulesNode();//JD for assessing only best branch
		AMRulesSplitCriterion splitCriterion=(AMRulesSplitCriterion)((AMRulesSplitCriterion) ((AMRulesRegressorOld)this.amRules).splitCriterionOption.getPreMaterializedObject()).copy();

		// Using this criterion, find the best split per attribute and rank the results
		AttributeSplitSuggestion[] bestSplitSuggestions
		= this.getBestSplitSuggestions(splitCriterion);
		Arrays.sort(bestSplitSuggestions);
		// Declare a variable to determine if any of the splits should be performed
		boolean shouldSplit = false;


		// If only one split was returned, use it
		if (bestSplitSuggestions.length < 2) {
			shouldSplit = ((bestSplitSuggestions.length > 0) && (bestSplitSuggestions[0].merit > 0)); 
			bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
		} // Otherwise, consider which of the splits proposed may be worth trying
		else {
			// Determine the hoeffding bound value, used to select how many instances should be used to make a test decision
			// to feel reasonably confident that the test chosen by this sample is the same as what would be chosen using infinite examples
			double hoeffdingBound = computeHoeffdingBound(1, splitConfidence, getWeightSeen());
			debug("Hoeffding bound " + hoeffdingBound, 4);
			// Determine the top two ranked splitting suggestions
			bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
			AttributeSplitSuggestion secondBestSuggestion
			= bestSplitSuggestions[bestSplitSuggestions.length - 2];

			debug("Merits: " + secondBestSuggestion.merit + " " + bestSuggestion.merit, 4);

			// If the upper bound of the sample mean for the ratio of SDR(best suggestion) to SDR(second best suggestion),
			// as determined using the hoeffding bound, is less than 1, then the true mean is also less than 1, and thus at this
			// particular moment of observation the bestSuggestion is indeed the best split option with confidence 1-delta, and
			// splitting should occur.
			// Alternatively, if two or more splits are very similar or identical in terms of their splits, then a threshold limit
			// (default 0.05) is applied to the hoeffding bound; if the hoeffding bound is smaller than this limit then the two
			// competing attributes are equally good, and the split will be made on the one with the higher SDR value.

			if (bestSuggestion.merit > 0) {
				//if ((((secondBestSuggestion.merit / bestSuggestion.merit) + hoeffdingBound) < 1) //ratio
				if ((((bestSuggestion.merit-secondBestSuggestion.merit) ) > hoeffdingBound) // if normalized
						|| (hoeffdingBound < tieThreshold)) {
					debug("Expanded ", 5);
					shouldSplit = true;
				}
			}
		}

		if (shouldSplit == true) {
			AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];
			double minValue = Double.MAX_VALUE;
			 double[] branchMerits = splitCriterion.computeBranchSplitMerits(bestSuggestion.resultingClassDistributions);

			for (int i = 0; i < bestSuggestion.numSplits(); i++) {
				double value = branchMerits[i];
				if (value < minValue) {
					minValue = value;
					splitIndex = i;
					statisticsNewRuleActiveLearningNode = bestSuggestion.resultingClassDistributionFromSplit(i);
				}
			}
			statisticsBranchSplit = splitDecision.resultingClassDistributionFromSplit(splitIndex);
			statisticsOtherBranchSplit = bestSuggestion.resultingClassDistributionFromSplit(splitIndex == 0 ? 1 : 0);

		}
		return shouldSplit;
	}

	public void learnFromInstance(Instance inst) {

		// Update the statistics for this node
		// number of instances passing through the node
		nodeStatistics.addToValue(0, inst.weight());
		// sum of y values
		nodeStatistics.addToValue(1, inst.classValue()*inst.weight());
		// sum of squared y values
		nodeStatistics.addToValue(2, inst.classValue()*inst.classValue()*inst.weight());
		/*
		for (int i = 0; i < inst.numAttributes() - 1; i++) {
			int instAttIndex = AbstractAMRules.modelAttIndexToInstanceAttIndex(i, inst);

			AttributeClassObserver obs = this.attributeObservers.get(i);
			if (obs == null) {
				// At this stage all nominal attributes are ignored
				if (inst.attribute(instAttIndex).isNumeric()) //instAttIndex
				{
					obs = newNumericClassObserver();
					this.attributeObservers.set(i, obs);
				}
			}
			if (obs != null) {
				((FIMTDDNumericAttributeClassObserver) obs).observeAttributeClass(inst.value(instAttIndex), inst.classValue(), inst.weight());
			}
		}*/
		//if was of attributes was not created so far, generate one and create perceptron
		if (attributesMask==null)
		{
			numAttributesSelected=(int)Math.round((inst.numAttributes()-1)*this.amRules.getAttributesPercentage())/100;
			
			attributesMask=new boolean[inst.numAttributes()];
			ArrayList<Integer> indices = new ArrayList<Integer>();
			for(int i=0; i<inst.numAttributes(); i++)
				if(i!=inst.classIndex())
					indices.add(i);
			Collections.shuffle(indices, this.amRules.classifierRandom);
			indices.add(inst.classIndex()); // add class index only after shuffle
			
			for (int i=0; i<numAttributesSelected;++i)
				attributesMask[indices.get(i)]=true;
		}
		
		for (int i = 0, ct=0; i < attributesMask.length; i++) {
			if(attributesMask[i])
			{
			AttributeClassObserver obs = this.attributeObservers.get(ct);
			if (obs == null) {
				// At this stage all nominal attributes are ignored
				if (inst.attribute(ct).isNumeric()) //instAttIndex
				{
					obs = newNumericClassObserver();
					this.attributeObservers.set(ct, obs);
				}
			}
			if (obs != null) {
				((FIMTDDNumericAttributeClassObserver) obs).observeAttributeClass(inst.value(i), inst.classValue(), inst.weight());
			}
			++ct;
			}
		}
		
	}

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

	@Override
	public double getWeightSeen() {
		if (nodeStatistics != null) {
			return this.nodeStatistics.getValue(0);
		} else {
			return 0;
		}
	}

	@Override
	public double getCurrentError() {
		double error;
		if (this.perceptron!=null){
			if (targetMean==null)
				error=perceptron.getCurrentError();
			else{
				double errorP=perceptron.getCurrentError();
				double errorTM=targetMean.getCurrentError();
				error = (errorP<errorTM) ? errorP : errorTM;	
			}		
		}
		else
			error=Double.MAX_VALUE;
		return error;
	}
}