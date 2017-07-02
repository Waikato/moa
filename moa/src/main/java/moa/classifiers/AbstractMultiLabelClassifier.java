package moa.classifiers;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.learners.MultiLabelClassifier;

public abstract class AbstractMultiLabelClassifier extends AbstractInstanceLearner<MultiLabelClassifier> {
	
	private static final long serialVersionUID = 1L;

	public Prediction getPredictionForInstance(MultiLabelInstance inst) {
		return this.getPredictionForInstance((Instance) inst);
	}
	
	public void trainOnInstanceImpl(MultiLabelInstance inst) {
		this.trainOnInstanceImpl((Instance) inst);
	}
}
