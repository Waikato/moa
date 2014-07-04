package moa.classifiers.rules.core.multilabel;

import java.util.List;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.AbstractMOAObject;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.rules.core.anomalydetection.AnomalyDetector;
import moa.classifiers.rules.error.MultiLabelErrorMeasurement;
import moa.classifiers.rules.functions.multilabel.AMRulesMultiLabelLearner;
import moa.core.DoubleVector;

public abstract class LearningLiteral extends AbstractMOAObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected List<DoubleVector> nodeStatistics;
	
	protected AMRulesMultiLabelLearner learner;
	
	protected MultiLabelErrorMeasurement errorMeasurement;
	
	protected ChangeDetector changeDetector;
	
	protected AnomalyDetector anomalyDetector;
	
	protected double weightSeen=0;
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

	public boolean updateChangeDetection(MultiLabelInstance instance) {
		changeDetector.input(getNormalizedError(instance));
		return changeDetector.getChange();
		
	}

	/**
	 * @param instance 
	 * @return normalized error of the current instance
	 */
	protected abstract double getNormalizedError(MultiLabelInstance instance);

	public boolean updateAnomalyDetection(MultiLabelInstance instance) {
		return anomalyDetector.updateAndCheck(instance);
	}

	public double getWeightSeenSinceExpansion() {
		return weightSeen;
	}

	public void trainOnInstance(MultiLabelInstance instance) {
		learner.trainOnInstance(instance);
	}

	public Prediction getPredictionForInstance(MultiLabelInstance instance) {
		// TODO Auto-generated method stub
		return learner.getPredictionForInstance(instance);
	}

}
