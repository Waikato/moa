package moa.classifiers.rules.multilabel.core;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import moa.classifiers.MultiLabelLearner;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.rules.core.Utils;
import moa.classifiers.rules.multilabel.attributeclassobservers.AttributeStatisticsObserver;
import moa.classifiers.rules.multilabel.attributeclassobservers.NominalStatisticsObserver;
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
		super();
	}

	public LearningLiteralRegression(int [] outputsToLearn) {
		super(outputsToLearn);
	}




	protected double [] getNormalizedErrors(Prediction prediction, Instance instance) {
		double [] errors= new double[outputsToLearn.length];

		for (int i=0; i<outputsToLearn.length;i++){
			double predY=normalizeOutputValue(i,prediction.getVote(outputsToLearn[i], 0));
			double trueY=normalizeOutputValue(i,instance.valueOutputAttribute(outputsToLearn[i]));
			errors[i]=Math.abs(predY-trueY);
		}
		return errors;
	}

	private double normalizeOutputValue(int outputToLearnIndex, double value) {
		double meanY = this.literalStatistics[outputToLearnIndex].getValue(1)/this.literalStatistics[outputToLearnIndex].getValue(0);
		double sdY = Utils.computeSD(this.literalStatistics[outputToLearnIndex].getValue(2), this.literalStatistics[outputToLearnIndex].getValue(1), this.literalStatistics[outputToLearnIndex].getValue(0));
		double normalizedY = 0.0;
		if (sdY > 0.0000001) {
			normalizedY = (value - meanY) / (sdY);
		}
		return normalizedY;
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
			double hoeffdingBound = computeHoeffdingBound(splitCriterion.getRangeOfMerit(this.literalStatistics), splitConfidence, weightSeen);
			//debug("Hoeffding bound " + hoeffdingBound, 4);
			// Determine the top two ranked splitting suggestions
			bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
			AttributeExpansionSuggestion secondBestSuggestion
			= bestSplitSuggestions[bestSplitSuggestions.length - 2];
			if ((((bestSuggestion.merit-secondBestSuggestion.merit)) > hoeffdingBound) || (hoeffdingBound < tieThreshold)) {
			//if ((((secondBestSuggestion.merit/bestSuggestion.merit) + hoeffdingBound) < 1) || (hoeffdingBound < tieThreshold)) {
				//debug("Expanded ", 5);
				shouldSplit = true;
				//System.out.println(bestSuggestion.merit);
			}
		}

		if(shouldSplit)
		{
			//check which branch is better and update bestSuggestion (in amrules the splits are binary )
			DoubleVector[][] resultingStatistics=bestSuggestion.getResultingNodeStatistics();
			//if not or higher is better, change predicate (negate condition)
			double [] branchMerits=splitCriterion.getBranchesSplitMerits(resultingStatistics);
			DoubleVector[] newLiteralStatistics;
			if(branchMerits[1]>branchMerits[0]){
				bestSuggestion.getPredicate().negateCondition();
				newLiteralStatistics=getBranchStatistics(resultingStatistics,1);
			}else{
				newLiteralStatistics=getBranchStatistics(resultingStatistics,0);
			}
			//
			int [] newOutputs=outputSelector.getNextOutputIndices(newLiteralStatistics,literalStatistics, outputsToLearn);
			//set expanding branch
			if(learner instanceof AMRulesFunction){
				((AMRulesFunction) learner).resetWithMemory();
			}
			expandedLearningLiteral=new LearningLiteralRegression(newOutputs);
			expandedLearningLiteral.setLearner((MultiLabelLearner)this.learner.copy());

			//set other branch (used if default rule expands)
			otherBranchLearningLiteral=new LearningLiteralRegression(newOutputs);
			otherBranchLearningLiteral.setLearner((MultiLabelLearner)learner.copy());

		}
		return shouldSplit;
	}



	private DoubleVector[] getBranchStatistics(DoubleVector[][] resultingStatistics, int indexBranch) {
		DoubleVector[] selBranchStats=new DoubleVector[resultingStatistics.length];
		for(int i=0; i<resultingStatistics.length;i++)
			selBranchStats[i]=resultingStatistics[i][indexBranch];
		return selBranchStats;
	}

	private AttributeExpansionSuggestion[] getBestSplitSuggestions(MultiLabelSplitCriterion criterion) {
		List<AttributeExpansionSuggestion> bestSuggestions = new LinkedList<AttributeExpansionSuggestion>();
		for (int i = 0; i < this.attributeObservers.size(); i++) {
			AttributeStatisticsObserver obs = this.attributeObservers.get(i);
			if (obs != null) {
				AttributeExpansionSuggestion bestSuggestion = null;
				bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion, literalStatistics, i);

				if (bestSuggestion != null) {
					bestSuggestions.add(bestSuggestion);
				}
			}
		}
		return bestSuggestions.toArray(new AttributeExpansionSuggestion[bestSuggestions.size()]);
	}

	@Override
	public void trainOnInstance(MultiLabelInstance instance)  {
		if (attributesMask==null)
			initializeAttibutesMask(instance);
		
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
			literalStatistics= new DoubleVector[outputsToLearn.length];
			for(int i=0; i<outputsToLearn.length; i++)
				literalStatistics[i]=new DoubleVector(new double[3]);

			hasStarted=true;
		}
		double weight=instance.weight();
		DoubleVector []exampleStatistics=new DoubleVector[outputsToLearn.length];
		for (int i=0; i<outputsToLearn.length; i++){
			double target=instance.valueOutputAttribute(outputsToLearn[i]);
			double sum=weight*target;
			double squaredSum=weight*target*target;
			exampleStatistics[i]= new DoubleVector(new double[]{weight,sum, squaredSum});
			literalStatistics[i].addValues(exampleStatistics[i].getArrayRef());
		}

		if(this.attributeObservers==null)
			this.attributeObservers=new AutoExpandVector<AttributeStatisticsObserver>();
		for(int i=0, ct=0; i<instance.numInputAttributes(); i++){
			if(attributesMask[i]){
				AttributeStatisticsObserver obs=this.attributeObservers.get(ct);
				if(obs==null){
					if(instance.attribute(i).isNumeric()){
						obs=((NumericStatisticsObserver)numericStatisticsObserver.copy());
					}else if(instance.attribute(i).isNominal()){ //just to make sure its nominal (in the future there may be ordinal?
						obs=((NominalStatisticsObserver)nominalStatisticsObserver.copy());
					}
					this.attributeObservers.set(ct, obs);
				}
				obs.observeAttribute(instance.valueInputAttribute(i), exampleStatistics);
				ct++;
			}
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
