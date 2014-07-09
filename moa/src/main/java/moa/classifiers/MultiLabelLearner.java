package moa.classifiers;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;

public interface MultiLabelLearner extends Classifier{
	public void trainOnInstanceImpl(MultiLabelInstance instance);

}
