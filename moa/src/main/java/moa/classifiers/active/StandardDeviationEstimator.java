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
 * @author Daniel Kottke (daniel.kottke@uni-kassel.de)
 * @version $Revision: 2 $
 */
public class StandardDeviationEstimator {
	// mean, standard deviation and variance for all features
	double[] sum;
	double[] sumsq;
	double[] std;
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
		sum = new double[numFeatures];
		sumsq = new double[numFeatures];
		std = new double[numFeatures];
		numInstances = 0;
		for (int i = 0; i < numFeatures; ++i) {
			std[i] = 1;
		}
	}

	/**
	 * adding a point and optionally remove a point from the observation
	 * @param removedElement the instance which will be removed (set to null if none is removed)
	 * @param newElement the instance which will be added to the observation
	 */
	public void addPoint(double[] removedElement, double[] newElement) {
		// increase the number of instances 
		if (newElement != null) {
			++numInstances;			
		}
		if (removedElement != null) {
			--numInstances;			
		}
		
		
		// iterate over all features to calculate the values for each feature separately
		for (int i = 0; i < numFeatures; ++i) {

			sum[i] += newElement[i];
			sumsq[i] += newElement[i]*newElement[i];
			
			// check if an instance is removed or not
			if (removedElement != null) {
				// increase the number of instances 
				sum[i] -= removedElement[i];
				sumsq[i] -= removedElement[i]*removedElement[i];
			}
			
			// set the variance and standard derivation to zero if the number of instances is zero
			if(numInstances < 2){
				std[i] = 1;
			}
			else{
				std[i] = Math.sqrt((sumsq[i] - (sum[i]*sum[i]/numInstances))/numInstances);
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
