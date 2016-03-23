package moa.classifiers;

import com.yahoo.labs.samoa.instances.StructuredInstance;
import com.yahoo.labs.samoa.instances.Prediction;

public interface MultiLabelLearner extends Classifier{
	
	public void trainOnInstanceImpl(StructuredInstance instance);
	
	public Prediction getPredictionForInstance(StructuredInstance instance);

}
