package moa.classifiers.rules.multilabel.core;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.rules.core.anomalydetection.AnomalyDetector;
import moa.classifiers.rules.multilabel.errormeasurers.MultiTargetErrorMeasurer;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;

public abstract class LearningLiteral extends AbstractOptionHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected AutoExpandVector<AttributeClassObserver> attributeObservers = new AutoExpandVector<AttributeClassObserver>();
	
	protected DoubleVector[] nodeStatistics;
	
	protected int[] outputsToLearn;
	
	protected MultiLabelLearner learner;
	
	protected MultiTargetErrorMeasurer errorMeasurers;
	
	//change detector for each outputsToLearn
	protected ChangeDetector[] changeDetectors;
	
	protected AnomalyDetector anomalyDetector;
	
	protected double weightSeen;
	
	protected int[] newOutputsToLearn;
	
	protected boolean hasStarted;
	

	public ClassOption changeDetectorOption;
	
	public ClassOption anomalyDetectorOption;

	
	// Maintain statistics for input and output attributes for standard deviation computation?
	
	public LearningLiteral(){
	}
	
	public LearningLiteral(DoubleVector outputsToLearn){
		this();
		outputsToLearn=(DoubleVector)outputsToLearn.copy();
	}
	
	public void trainOnInstance(MultiLabelInstance instance) {
		//learn for all output attributes if not specified at construction time
		if(!hasStarted)
		{
			outputsToLearn=new int[instance.numberOutputTargets()];
			for (int i=0; i<instance.numberOutputTargets();i++)
				outputsToLearn[i]=i;
			hasStarted=true;
		}
		learner.trainOnInstance(instance);
		weightSeen+=instance.weight();
	}

	public Prediction getPredictionForInstance(MultiLabelInstance instance) {
		if (hasStarted)
			return learner.getPredictionForInstance(instance);
		else
			return new MultiLabelPrediction(instance.numberOutputTargets());
	}
	
	
	public abstract boolean updateAndCheckChange(MultiLabelInstance instance);


	public boolean updateAndCheckAnomalyDetection(MultiLabelInstance instance) {
		return anomalyDetector.updateAndCheckAnomalyDetection(instance);
	}
	
	public ClassOption getChangeDetectorOption() {
		return changeDetectorOption;
	}

	public void setChangeDetectorOption(ClassOption changeDetectorOption) {
		this.changeDetectorOption = changeDetectorOption;
	}

	public ClassOption getAnomalyDetectorOption() {
		return anomalyDetectorOption;
	}

	public void setAnomalyDetectorOption(ClassOption anomalyDetectorOption) {
		this.anomalyDetectorOption = anomalyDetectorOption;
	}

	
	public double getWeightSeenSinceExpansion() {
		return weightSeen;
	}

	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}



}
