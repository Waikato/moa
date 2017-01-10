package moa.classifiers;

import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.StructuredInstance;

public interface MultiLabelLearner extends Classifier{
	
	public void trainOnInstanceImpl(StructuredInstance instance);
	
	public Prediction getPredictionForInstance(StructuredInstance instance);

}
