package moa.classifiers.rules.multilabel.core;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.InstanceInformation;
import com.yahoo.labs.samoa.instances.InstancesHeader;
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
import moa.classifiers.rules.core.NumericRulePredicate;
import moa.classifiers.rules.core.Utils;
import moa.classifiers.rules.multilabel.attributeclassobservers.AttributeStatisticsObserver;
import moa.classifiers.rules.multilabel.attributeclassobservers.NominalStatisticsObserver;
import moa.classifiers.rules.multilabel.attributeclassobservers.NumericStatisticsObserver;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.rules.multilabel.functions.AMRulesFunction;
import moa.classifiers.rules.multilabel.instancetransformers.InstanceAttributesSelector;
import moa.classifiers.rules.multilabel.instancetransformers.InstanceOutputAttributesSelector;
import moa.classifiers.rules.multilabel.instancetransformers.NoInstanceTransformation;
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
	
	double [] varianceShift; //for proper computation of variance

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

		//disable attributes that are not relevant
		int []oldInputs=inputsToLearn.clone();
		inputsToLearn=inputSelector.getNextInputIndices(bestSplitSuggestions); //
		Arrays.sort(this.inputsToLearn);
		for (int i=0; i<oldInputs.length; i++){
			if(attributesMask[oldInputs[i]]){
				if(Arrays.binarySearch(inputsToLearn, oldInputs[i])<0)
				{
					this.attributeObservers.set(oldInputs[i], null);
				}
			}
		}


		// If only one split was returned, use it
		if (bestSplitSuggestions.length < 2) {
			//shouldSplit = ((bestSplitSuggestions.length > 0) && (bestSplitSuggestions[0].merit > 0)); 
			bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
			shouldSplit = true;
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
			

			//set other branch (only used if default rule expands)
			otherBranchLearningLiteral=new LearningLiteralRegression();
			otherBranchLearningLiteral.setLearner((MultiLabelLearner)learner.copy());
			otherBranchLearningLiteral.setInstanceTransformer(this.instanceTransformer); //TODO: check this 
			//Set expanding branch
			//if is AMRulesFunction and the  number of output attributes changes, start learning a new predictor
			//should we do the same for input attributes (attributesMask)?. It would have impact in RandomAMRules
			if(learner instanceof AMRulesFunction && newOutputs.length == outputsToLearn.length){ //Reset learning
				((AMRulesFunction) learner).resetWithMemory();
			}
			//just reset learning
			else{
				learner.resetLearning();
			}
				
			expandedLearningLiteral=new LearningLiteralRegression(newOutputs);
			expandedLearningLiteral.setLearner((MultiLabelLearner)this.learner.copy());
			

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
		for (int i = 0; i < this.inputsToLearn.length; i++) {
			if(attributesMask[inputsToLearn[i]]){ //Should always be true (check trainOnInstance(). Remove?
				AttributeStatisticsObserver obs = this.attributeObservers.get(inputsToLearn[i]);
				if (obs != null) {
					AttributeExpansionSuggestion bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion, literalStatistics, inputsToLearn[i]);

					if (bestSuggestion == null) {
						//ALL attributes must have a best suggestion. Adding dummy suggestion with minimal merit.
						bestSuggestion=new  AttributeExpansionSuggestion(new NumericRulePredicate(inputsToLearn[i],0,true),null,Double.MIN_VALUE);
					}
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
		int numInputs=instance.numInputAttributes();
		if(!hasStarted)
		{
			if(outputsToLearn==null)
			{
				outputsToLearn=new int[numOutputs];
				for (int i=0; i<numOutputs;i++){
					outputsToLearn[i]=i;
				}
			}
			if(inputsToLearn==null)
			{
				inputsToLearn=new int[numInputs];
				for (int i=0; i<numInputs;i++){//TODO: check with mask?
					if(attributesMask[i])
						inputsToLearn[i]=i;
				}
			}

			literalStatistics= new DoubleVector[outputsToLearn.length];
			varianceShift=new double[outputsToLearn.length];
			for(int i=0; i<outputsToLearn.length; i++){
				literalStatistics[i]=new DoubleVector(new double[5]);
				varianceShift[i]=instance.valueOutputAttribute(outputsToLearn[i]);
			}
			if (outputsToLearn.length==instance.numOutputAttributes())//attributes are the original
				instanceTransformer=new NoInstanceTransformation();
			else
				instanceTransformer=new InstanceOutputAttributesSelector((InstancesHeader)instance.dataset(), outputsToLearn);

			hasStarted=true;
		}
		double weight=instance.weight();
		DoubleVector []exampleStatistics=new DoubleVector[outputsToLearn.length];
		for (int i=0; i<outputsToLearn.length; i++){
			double target=instance.valueOutputAttribute(outputsToLearn[i]);
			double sum=weight*target;
			double squaredSum=weight*target*target;
			double sumShifted=weight*target-varianceShift[i];
			double squaredSumShifted=weight*(target-varianceShift[i])*(target-varianceShift[i]);
			exampleStatistics[i]= new DoubleVector(new double[]{weight,sum, squaredSum,sumShifted,squaredSumShifted});
			literalStatistics[i].addValues(exampleStatistics[i].getArrayRef());
		}

		if(this.attributeObservers==null)
			this.attributeObservers=new AutoExpandVector<AttributeStatisticsObserver>();
		for(int i=0; i<inputsToLearn.length; i++){
			if(attributesMask[inputsToLearn[i]]){ //this is checked above. Remove?
				AttributeStatisticsObserver obs=this.attributeObservers.get(inputsToLearn[i]);
				if(obs==null){
					if(instance.attribute(inputsToLearn[i]).isNumeric()){
						obs=((NumericStatisticsObserver)numericStatisticsObserver.copy());
					}else if(instance.attribute(inputsToLearn[i]).isNominal()){  //just to make sure its nominal (in the future there may be ordinal?
						obs=((NominalStatisticsObserver)nominalStatisticsObserver.copy());
					}
					this.attributeObservers.set(inputsToLearn[i], obs);
				}
				obs.observeAttribute(instance.valueInputAttribute(inputsToLearn[i]), exampleStatistics);
			}
		}
		
		//Transform instance for learning
		Instance transformedInstance=instanceTransformer.sourceInstanceToTarget(instance);
		Prediction prediction=null;
		Prediction targetPrediction=learner.getPredictionForInstance(transformedInstance);
		if(targetPrediction!=null)
			prediction=instanceTransformer.targetPredictionToSource(targetPrediction);
		
		if(prediction!=null)
			errorMeasurer.addPrediction(prediction, instance);
		
		learner.trainOnInstance(transformedInstance);
		
		weightSeen+=instance.weight();
	}

	@Override
	public String getStaticOutput(InstanceInformation instanceInformation) {
		StringBuffer sb = new StringBuffer();
		if(this.literalStatistics!=null){
			for(int i=0; i<this.literalStatistics.length; i++){
				sb.append(instanceInformation.outputAttribute(outputsToLearn[i]).name() +  ": " + literalStatistics[i].getValue(1)/literalStatistics[i].getValue(0) + " ");
			}
		}
		return sb.toString();
	}


	/*@Override
	public void resetLearning() {
		weightSeen=0;
		//hasStarted=false;
	}*/
}
