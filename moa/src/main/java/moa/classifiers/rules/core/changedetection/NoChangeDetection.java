package moa.classifiers.rules.core.changedetection;

import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class NoChangeDetection extends AbstractChangeDetector implements
		ChangeDetector {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NoChangeDetection()
	{
		resetLearning();
	}
	@Override
	public void input(double inputValue) {
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

	}
	@Override
	public void resetLearning()
	{
		this.isInitialized=true;
		this.isChangeDetected=false;
		this.isWarningZone=false;
        this.estimation = 0.0;
        this.delay = 0.0;
	}
	
    @Override
    public String getPurposeString() {
        return "Use this class to NOT detect changes.";
    }

}
