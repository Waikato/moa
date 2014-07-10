package moa.classifiers;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

public interface MultiLabelLearner extends Classifier{
	
	public void trainOnInstanceImpl(MultiLabelInstance instance);
	
	public Prediction getPredictionForInstance(MultiLabelInstance instance);

}
