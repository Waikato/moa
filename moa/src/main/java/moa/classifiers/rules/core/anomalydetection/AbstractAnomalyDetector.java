package moa.classifiers.rules.core.anomalydetection;

import com.yahoo.labs.samoa.instances.StructuredInstance;

import moa.options.AbstractOptionHandler;


public abstract class AbstractAnomalyDetector extends AbstractOptionHandler implements AnomalyDetector {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public abstract boolean updateAndCheckAnomalyDetection(StructuredInstance instance);

	@Override
	public AnomalyDetector copy() {
		return (AnomalyDetector) super.copy();
	}

}
