package moa.classifiers.rules.core.anomalydetection;

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;

public class AnomalinessScore extends AbstractAnomalyDetector {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	@Override
	public boolean updateAndCheckAnomalyDetection(MultiLabelInstance instance) {
		
		
		// TODO JD
		return false;
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

}
