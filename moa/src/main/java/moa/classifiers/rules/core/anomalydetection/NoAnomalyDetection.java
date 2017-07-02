package moa.classifiers.rules.core.anomalydetection;

import com.yahoo.labs.samoa.instances.Instance;

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class NoAnomalyDetection extends AbstractAnomalyDetector{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}

	@Override
	public boolean updateAndCheckAnomalyDetection(Instance instance) {
		return false;
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		
	}
	
    @Override
    public String getPurposeString() {
        return "Use this class to NOT detect anomalies.";
    }

}
