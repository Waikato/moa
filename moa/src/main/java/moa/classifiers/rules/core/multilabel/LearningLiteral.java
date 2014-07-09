package moa.classifiers.rules.core.multilabel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.AbstractMOAObject;
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.rules.core.anomalydetection.AnomalyDetector;
import moa.classifiers.rules.error.MultiLabelErrorMeasurement;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;

public abstract class LearningLiteral extends AbstractMOAObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected AutoExpandVector<AttributeClassObserver> attributeObservers = new AutoExpandVector<AttributeClassObserver>();
	
	protected List<DoubleVector> nodeStatistics;
	
	protected DoubleVector outputsToLearn;
	
	protected MultiLabelLearner learner;
	
	protected MultiLabelErrorMeasurement errorMeasurement;
	
	protected List<ChangeDetector> changeDetectors;
	
	protected AnomalyDetector anomalyDetector;
	
	protected double weightSeen;
	
	protected DoubleVector newOutputsToLearn;
	
	// Maintain statistics for input and output attributes for standard deviation computation?
	
	public LearningLiteral(){
	}
	
	public LearningLiteral(DoubleVector outputsToLearn){
		this();
		outputsToLearn=(DoubleVector)outputsToLearn.copy();
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

	public boolean updateChangeDetection(MultiLabelInstance instance) {
		//changeDetector.input(getNormalizedError(instance));
		//return changeDetector.getChange();
		
		
		//TODO: JD - one change detection for all outputs or one for each output? (think second is better)
		return false;
		
	}

	/**
	 * @param instance 
	 * @return normalized error of the current instance
	 */
	// abstract double getNormalizedError(MultiLabelInstance instance);

	public boolean updateAnomalyDetection(MultiLabelInstance instance) {
		return anomalyDetector.updateAndCheck(instance);
	}

	public double getWeightSeenSinceExpansion() {
		return weightSeen;
	}

	public void trainOnInstance(MultiLabelInstance instance) {
		//learn for all output attributes if not specified at construction time
		if(outputsToLearn==null)
		{
			outputsToLearn=new DoubleVector(new double[instance.numberOutputTargets()]);
			for (int i=0; i<instance.numberOutputTargets();i++)
				outputsToLearn.setValue(i, i);
		}
		learner.trainOnInstance(instance);
	}

	public Prediction getPredictionForInstance(MultiLabelInstance instance) {
		return learner.getPredictionForInstance(instance);
	}

}
