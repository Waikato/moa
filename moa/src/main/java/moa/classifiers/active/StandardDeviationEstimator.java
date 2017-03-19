/*
 *    StandardDeviationEstimator.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tuan Pham Minh (tuan.pham@ovgu.de)
 *    
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.classifiers.active;

/**
 * This class is used to incrementally calculate the standard derivation
 * for each feature separately.
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class StandardDeviationEstimator {
	// mean, standard deviation and variance for all features
	double[] mean;
	double[] std;
	double[] var;
	// number of features
	int numFeatures;
	// number of observed instances
	int numInstances;

	/**
	 * constructor
	 * @param numFeatures the number of features for the instances which will be used
	 */
	public StandardDeviationEstimator(int numFeatures) {
		this.numFeatures = numFeatures;
		mean = new double[numFeatures];
		std = new double[numFeatures];
		var = new double[numFeatures];
		numInstances = 0;
	}

	/**
	 * adding a point and optionally remove a point from the observation
	 * @param removedElement the instance which will be removed (set to null if none is removed)
	 * @param newElement the instance which will be added to the observation
	 */
	public void addPoint(double[] removedElement, double[] newElement) {
		// iterate over all features to calculate the values for each feature separately
		for (int i = 0; i < numFeatures; ++i) {
			// old mean and variance
			double oldMean = mean[i];
			double oldVar = var[i];
			// the value of the i-th feature of the new instance
			double xn = newElement[i];
			// check if an instance is removed or not
			if (removedElement == null) {
				// increase the number of instances if none is removed
				++numInstances;
				// calculate the difference between xn and the old mean
				double t = xn-oldMean;
				// incremetal calculation of variance and mean for the case
				// that no element is removed
				var[i] = ((numInstances-2.0)/(numInstances-1.0))*oldVar+t*(t/numInstances);
				mean[i] += t/numInstances;
			} else {
				// the value of the i-th feature of the instance which will be removed
				double x0 = removedElement[i];
				// incremetal calculation of variance and mean for the case
				// that one element is removed
				mean[i] += (-x0 + xn)/numInstances;
				var[i] += ((xn - x0) * ((xn+x0) - (oldMean + mean[i])))/(numInstances-1);
			}
			
			// calculate the standard derivation from the variance
			std[i] = Math.sqrt(var[i]);
			
			// set the variance and standard derivation to zero if the number of instances is zero
			if(numInstances == 1)
			{
				var[i] = 0;
				std[i] = 0;
			}
		}
	}

	/**
	 * get the standard derivations for all features
	 * @return an array with the standard derivation for all features
	 */
	public double[] getStd() {
		return std;
	}
}
