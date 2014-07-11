package moa.classifiers.rules.multilabel.core;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.MultiLabelLearner;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.rules.multilabel.attributeclassobservers.AttributeStatisticsObserver;
import moa.classifiers.rules.multilabel.attributeclassobservers.NumericStatisticsObserver;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.rules.multilabel.functions.AMRulesFunction;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.learners.Learner;
import moa.tasks.TaskMonitor;

public class LearningLiteralRegression extends LearningLiteral {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LearningLiteralRegression() {
	}

	public LearningLiteralRegression(int [] outputsToLearn) {
		super(outputsToLearn);
	}



	public boolean updateAndCheckChange(MultiLabelInstance instance) {
		boolean hasChanged=false;
		if (hasStarted){
			if (changeDetectors==null){
				changeDetectors=new ChangeDetector[outputsToLearn.length]; 
				for (int i=0; i<outputsToLearn.length; i++){
					changeDetectors[i]=changeDetector.copy();
				}
			}
			Prediction prediction=getPredictionForInstance(instance);
			double []normalizedErrors=getNormalizedErrors(prediction, instance.classValues());
			for (int i=0; i<outputsToLearn.length;i++){
				changeDetectors[i].input(normalizedErrors[i]);
				if(changeDetectors[i].getChange()){
					hasChanged=true;
					break;
				}
			}
		}
		return hasChanged;
	}

	private double [] getNormalizedErrors(Prediction prediction, InstanceData classValues) {
		double [] errors= new double[outputsToLearn.length];

		for (int i=0; i<outputsToLearn.length;i++){
			double predY=normalizeOutputValue(outputsToLearn[i],prediction.getVote(outputsToLearn[i], 0));
			double trueY=normalizeOutputValue(outputsToLearn[i],classValues.value(outputsToLearn[i]));
			errors[i]=Math.abs(predY-trueY);
		}
		return errors;
	}

	private double normalizeOutputValue(int outputAttributeIndex, double value) {
		double meanY = this.nodeStatistics[outputAttributeIndex].getValue(1)/this.nodeStatistics[outputAttributeIndex].getValue(0);
		double sdY = computeSD(this.nodeStatistics[outputAttributeIndex].getValue(2), this.nodeStatistics[outputAttributeIndex].getValue(1), this.nodeStatistics[outputAttributeIndex].getValue(0));
		double normalizedY = 0.0;
		if (sdY > 0.0000001) {
			normalizedY = (value - meanY) / (sdY);
		}
		return normalizedY;
	}


	public static double computeSD(double squaredSum, double sum, double weightSeen) {
		if (weightSeen > 1) {
			return Math.sqrt((squaredSum - ((sum * sum) / weightSeen)) / (weightSeen - 1.0));
		}
		return 0.0;
	}


	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

	}

	@Override
	public boolean tryToExpand(double splitConfidence, double tieThreshold) {
		boolean shouldSplit=false;
		//find the best split per attribute and rank the results
		AttributeExpansionSuggestion[] bestSplitSuggestions	= this.getBestSplitSuggestions(splitCriterion);
		Arrays.sort(bestSplitSuggestions);

		// If only one split was returned, use it
		if (bestSplitSuggestions.length < 2) {
			//shouldSplit = ((bestSplitSuggestions.length > 0) && (bestSplitSuggestions[0].merit > 0)); 
			bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
		} // Otherwise, consider which of the splits proposed may be worth trying
		else {
			double hoeffdingBound = computeHoeffdingBound(1, splitConfidence, weightSeen);
			//debug("Hoeffding bound " + hoeffdingBound, 4);
			// Determine the top two ranked splitting suggestions
			bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
			AttributeExpansionSuggestion secondBestSuggestion
			= bestSplitSuggestions[bestSplitSuggestions.length - 2];
			//if ((((bestSuggestion.merit-secondBestSuggestion.merit)) > hoeffdingBound) || (hoeffdingBound < tieThreshold)) {
			if ((((secondBestSuggestion.merit/bestSuggestion.merit) + hoeffdingBound) < 1) || (hoeffdingBound < tieThreshold)) {
				//debug("Expanded ", 5);
				shouldSplit = true;
			}
		}

		if(shouldSplit)
		{
			//check which branch is better and update bestSuggestion (in amrules the splits are binary )
			DoubleVector[][] resultingStatistics=bestSuggestion.getResultingNodeStatistics();
			//if not or higher is better, change predicate (negate condition)
			double [] branchMerits=splitCriterion.getBranchesSplitMerits(resultingStatistics);
			if(branchMerits[1]>branchMerits[0])
				bestSuggestion.getPredicate().negateCondition();


			//set expanding branch
			if(learner instanceof AMRulesFunction){
				((AMRulesFunction) learner).resetWithMemory();;
			}
			expandedLearningLiteral=new LearningLiteralRegression();
			expandedLearningLiteral.setLearner((MultiLabelLearner)this.learner.copy());

			//set other branch (used if default rule expands)
			otherBranchLearningLiteral=new LearningLiteralRegression();
			otherBranchLearningLiteral.setLearner((MultiLabelLearner)learner.copy());

		}
		return shouldSplit;
	}



	private AttributeExpansionSuggestion[] getBestSplitSuggestions(MultiLabelSplitCriterion criterion) {
		List<AttributeExpansionSuggestion> bestSuggestions = new LinkedList<AttributeExpansionSuggestion>();
		for (int i = 0; i < this.attributeObservers.size(); i++) {
			AttributeStatisticsObserver obs = this.attributeObservers.get(i);
			if (obs != null) {
				AttributeExpansionSuggestion bestSuggestion = null;
				bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion, nodeStatistics, i);

				if (bestSuggestion != null) {
					bestSuggestions.add(bestSuggestion);
				}
			}
		}
		return bestSuggestions.toArray(new AttributeExpansionSuggestion[bestSuggestions.size()]);
	}

	@Override
	public void trainOnInstance(MultiLabelInstance instance)  {
		//learn for all output attributes if not specified at construction time
		int numOutputs=instance.numberOutputTargets();
		if(!hasStarted)
		{
			if(outputsToLearn==null)
			{
				outputsToLearn=new int[instance.numberOutputTargets()];
				for (int i=0; i<numOutputs;i++){
					outputsToLearn[i]=i;
				}
			}
			nodeStatistics= new DoubleVector[outputsToLearn.length];
			for(int i:outputsToLearn)
				nodeStatistics[i]=new DoubleVector(new double[3]);

			hasStarted=true;
		}
		double weight=instance.weight();
		DoubleVector []exampleStatistics=new DoubleVector[outputsToLearn.length];
		for (int i:outputsToLearn){
			double target=instance.valueOutputAttribute(i);
			double sum=weight*target;
			double squaredSum=weight*target*target;
			exampleStatistics[i]= new DoubleVector(new double[]{weight,sum, squaredSum});
			nodeStatistics[i].addValues(exampleStatistics[i].getArrayRef());
		}

		if(this.attributeObservers==null)
			this.attributeObservers=new AutoExpandVector<AttributeStatisticsObserver>();
		for(int i =0; i<instance.numInputAttributes(); i++){
			AttributeStatisticsObserver obs=this.attributeObservers.get(i);
			if(obs==null){
				if(instance.attribute(i).isNumeric()){
					obs=((NumericStatisticsObserver)numericStatisticsObserver.copy());
				}
				this.attributeObservers.set(i, obs);
				//TODO: JD - support categorical attributes
			}
			obs.observeAttribute(instance.valueInputAttribute(i), exampleStatistics);
		}
		Prediction prediction=learner.getPredictionForInstance(instance);
		if(prediction!=null)
			errorMeasurer.addPrediction(prediction, instance);
		learner.trainOnInstance(instance);
		weightSeen+=instance.weight();
	}

	/*@Override
	public void resetLearning() {
		weightSeen=0;
		//hasStarted=false;
	}*/
}
