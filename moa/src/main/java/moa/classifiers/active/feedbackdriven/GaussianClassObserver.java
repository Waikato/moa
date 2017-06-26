package moa.classifiers.active.feedbackdriven;

import moa.classifiers.core.attributeclassobservers.GaussianNumericAttributeClassObserver;
import moa.core.GaussianEstimator;

/*
 * Add a getter for the gaussianEstimators
 */
public class GaussianClassObserver extends GaussianNumericAttributeClassObserver {
	private static final long serialVersionUID = 1L;

	public GaussianEstimator GetDistributionForClass(int classID) {
		return this.attValDistPerClass.get(classID);
	}
}