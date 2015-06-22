package moa.classifiers.rules.multilabel.core;

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceInformation;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

public class LearningLiteralClassification extends LearningLiteral {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void trainOnInstance(MultiLabelInstance instance) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean tryToExpand(double splitConfidence, double tieThresholdOption) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected double[] getNormalizedErrors(Prediction prediction, Instance inst) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStaticOutput(InstanceInformation instanceInformation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		// TODO Auto-generated method stub

	}

}
