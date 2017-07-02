package moa.classifiers;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.StructuredInstance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.learners.MultiTargetRegressor;

public abstract class AbstractMultiTargetRegressor extends AbstractInstanceLearner<MultiTargetRegressor> implements MultiTargetRegressor {

	private static final long serialVersionUID = 1L;

	public Prediction getPredictionForInstance(StructuredInstance instance) {
		return this.getPredictionForInstance((Instance) instance);
	}

	@Override
	public void trainOnInstanceImpl(StructuredInstance inst) {
		this.trainOnInstanceImpl((Instance) inst); 
	}

}
