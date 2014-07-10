package moa.classifiers.rules.core.anomalydetection;

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;

public class NoAnomalyDetection extends AbstractAnomalyDetector{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}

	@Override
	public boolean updateAndCheckAnomalyDetection(MultiLabelInstance instance) {
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
