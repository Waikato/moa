package moa.classifiers.rules.functions.multilabel;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.MultiLabelLearner;

public interface AMRulesMultiLabelLearner extends MultiLabelLearner {
	public void trainOnInstance(MultiLabelInstance instance);
	
	public Prediction getPredictionForInstance(MultiLabelInstance instance);
	public double getCurrentError();

}
