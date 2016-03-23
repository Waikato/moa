package moa.classifiers.rules.core.anomalydetection;

import moa.options.AbstractOptionHandler;
import com.yahoo.labs.samoa.instances.StructuredInstance;


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
