/*
 *    PALStreamEstimatorMultivariate.java
 *    
 *    OpalEstimatorMultivariate Code:
 *    Copyright (C) 2016 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Christian Beyer (christian.beyer@ovgu.de)
 *    Implementation of MCPALEstimatorMultivariate based on OpalEstimatorMultivariate:
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
 * This class implements a multivariate Gaussian kernel density estimation and is based
 * on Christian Beyer's implementation of OpalEstimatorMultivariate and was modified to fit the
 * the needs for PALStream
 * 
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class PALStreamEstimatorMultivariate{
	
	private double bandwidth;
	private RingBuffer<double[]> points;
	
	public PALStreamEstimatorMultivariate(double bandwidth, int bufferSize){
		this.bandwidth=bandwidth;
		points = new RingBuffer<>(bufferSize);
	}
	
	public double[] addValue(double[] x){
		return points.add(x);
	}
	
	/**
	 * Returns the frequency of instances at position x NOT the density
	 * @param x
	 * @return
	 */
	public double getFrequencyEstimate(double[] x, double[] std){
		if(points.size()==0)
			return 0;

		double prob=0;
		for(int i=0;i<points.size();i++){
			prob +=getNormal(x, points.get(i), std);
		}
		return prob;
	}
	public double getNormal(double[] x, double[] mu, double[] std){
		int numFeatures = x.length;
		double v = 0;
		for(int i = 0; i < numFeatures; ++i)
		{
			// normalize the difference with the standard deviation
			double d = (x[i] - mu[i])/std[i];
			// check if std is zero
			// if yes replace the normalized difference with zero
			if(Double.isInfinite(d) || Double.isNaN(d))
			{
				d = 0;
			}
			v += d*d/bandwidth;
		}
		
		return Math.exp(-0.5 * v);
	}
	
	/**
	 * get the number of instances used to estimate the frequency
	 * @return number of points
	 */
	public int getNumPoints(){
		return points.size();
	}
	
}