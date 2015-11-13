package moa.classifiers.rules.multilabel.core;

//import org.hamcrest.core.IsInstanceOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.MultiLabelLearner;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NumericAttributeClassObserver;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.multilabel.core.attributeclassobservers.AttributeStatisticsObserver;
import moa.classifiers.multilabel.core.attributeclassobservers.NominalStatisticsObserver;
import moa.classifiers.multilabel.core.attributeclassobservers.NumericStatisticsObserver;
import moa.classifiers.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.rules.core.AttributeExpansionSuggestion;
import moa.classifiers.rules.core.anomalydetection.AnomalyDetector;
import moa.classifiers.rules.multilabel.errormeasurers.AbstractMultiTargetErrorMeasurer;
import moa.classifiers.rules.multilabel.errormeasurers.MultiLabelErrorMeasurer;
import moa.classifiers.rules.multilabel.outputselectors.OutputAttributesSelector;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;


public abstract class LearningLiteral extends AbstractOptionHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected AutoExpandVector<AttributeStatisticsObserver> attributeObservers ;

	protected DoubleVector[] literalStatistics;

	protected int[] outputsToLearn;

	protected MultiLabelLearner learner;

	protected MultiLabelErrorMeasurer errorMeasurer;

	//change detector for each outputsToLearn
	protected ChangeDetector[] changeDetectors;

	protected ChangeDetector changeDetector;

	protected AnomalyDetector anomalyDetector;

	protected MultiLabelSplitCriterion splitCriterion;

	protected double weightSeen;

	protected boolean hasStarted;

	protected LearningLiteral expandedLearningLiteral;

	protected LearningLiteral otherBranchLearningLiteral;

	protected AttributeExpansionSuggestion bestSuggestion;


	//public ClassOption changeDetectorOption;

	//public ClassOption anomalyDetectorOption;

	protected NumericStatisticsObserver numericStatisticsObserver;
	
	protected NominalStatisticsObserver nominalStatisticsObserver;

	protected OutputAttributesSelector outputSelector;

	protected Random randomGenerator;
	
    protected boolean [] attributesMask; //TODO: JD Use sparse representation?
    protected double attributesPercentage;


	// Maintain statistics for input and output attributes for standard deviation computation?

	public LearningLiteral(){
	}

	public LearningLiteral(int [] outputsToLearn){
		this();
		this.outputsToLearn=outputsToLearn.clone();
	}

	abstract public void trainOnInstance(MultiLabelInstance instance);

	public Prediction getPredictionForInstance(MultiLabelInstance instance) {
		if (learner!=null)
			return learner.getPredictionForInstance(instance);
		else
			return null;
	}


	public abstract boolean tryToExpand(double splitConfidence, double tieThresholdOption);

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
			double []normalizedErrors=getNormalizedErrors(prediction, instance);
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


	protected abstract double[] getNormalizedErrors(Prediction prediction,
			Instance inst);

	public boolean updateAndCheckAnomalyDetection(MultiLabelInstance instance) {
		if(hasStarted)
			return anomalyDetector.updateAndCheckAnomalyDetection(instance);
		else
			return false;
	}


	public double getWeightSeenSinceExpansion() {
		return weightSeen;
	}

	public int[] getOutputsToLearn() {
		return outputsToLearn;
	}

	public void setOutputsToLearn(int[] outputsToLearn) {
		this.outputsToLearn = outputsToLearn;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

	public LearningLiteral getExpandedLearningLiteral() {
		return expandedLearningLiteral;
	}

	public LearningLiteral getOtherBranchLearningLiteral() {
		return otherBranchLearningLiteral;
	}

	public double[] getErrors() {
		if(errorMeasurer!=null)
			return errorMeasurer.getCurrentErrors();
		else 
			return null;
	}

	public void setSplitCriterion(MultiLabelSplitCriterion splitCriterion) {
		this.splitCriterion=splitCriterion;

	}

	public void setChangeDetector(ChangeDetector changeDetector) {
		this.changeDetectors=null;
		this.changeDetector=changeDetector; //copy()?

	}

	public void setAnomalyDetector(AnomalyDetector anomalyDetector) {
		this.anomalyDetector=anomalyDetector; //copy()?

	}

	public void setNumericObserverOption(NumericStatisticsObserver numericStatisticsObserver) {
		if(this.attributeObservers!=null){
			for (AttributeStatisticsObserver obs : this.attributeObservers)
				if( obs instanceof NumericStatisticsObserver)
					obs=null;
		}
		this.numericStatisticsObserver=numericStatisticsObserver;
	}

	public void setLearner(MultiLabelLearner learner) {
		this.learner=learner;

	}

	public void setErrorMeasurer(MultiLabelErrorMeasurer errorMeasurer) {
		this.errorMeasurer=errorMeasurer;

	}

	public 	AttributeExpansionSuggestion getBestSuggestion(){
		return bestSuggestion;
	}

	public static double computeHoeffdingBound(double range, double confidence,
			double n) {
		return Math.sqrt(((range * range) * Math.log(1.0 / confidence))
				/ (2.0 * n));
	}

	public void setOutputAttributesSelector(
			OutputAttributesSelector outputSelector) {
		this.outputSelector=outputSelector;
		
	}

	public void setNominalObserverOption(NominalStatisticsObserver nominalStatisticsObserver) {
		this.nominalStatisticsObserver=nominalStatisticsObserver;
	}
	

	

	public void setRandomGenerator(Random random) {
		this.randomGenerator=random;
		
	}

	public void setAttributesPercentage(double attributesPercentage) {
		this.attributesPercentage=attributesPercentage;
	}
	
	protected void initializeAttibutesMask(MultiLabelInstance inst) {
		int numInputAttributes=inst.numInputAttributes();
		int numAttributesSelected=(int)Math.round(numInputAttributes*attributesPercentage/100);
		
		attributesMask=new boolean[numInputAttributes]; 
		ArrayList<Integer> indices = new ArrayList<Integer>(numInputAttributes);
		for(int i=0; i<numInputAttributes; i++)
				indices.add(i);
		Collections.shuffle(indices, this.randomGenerator);
		
		for (int i=0; i<numAttributesSelected;++i)
			attributesMask[indices.get(i)]=true;
		
	}
	
	
	

	//	abstract public void resetLearning();
}
