/*
 *    MCPALEstimatorMultivariate.java
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

import java.util.ArrayList;

import Jama.*;

/**
 * This class implements a multivariate Gaussian kernel density estimation and is based
 * on Christian's implementation of OpalEstimatorMultivariate and was modified to fit the
 * the needs for MCPAL
 * 
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class MCPALEstimatorMultivariate{
	
	private Matrix covariance;
	private double bandwidth;
	private ArrayList<Matrix> points;
	private boolean dimSet=false;
	
	public MCPALEstimatorMultivariate(double bandwidth){
		this.bandwidth=bandwidth;
		this.points = new ArrayList<Matrix>();
	}
	
	public void addValue(double x){
		double[] val={x};
		addValue(val);
	}
	
	public void addValue(double[] x){
		double[][] vec=new double[x.length][1];
		for(int i=0;i<x.length;i++)
			vec[i][0]=x[i];
		Matrix point=new Matrix(vec);
		points.add(point);
		if(dimSet==false)
			initCovariance(x.length);
	}
	
	/**
	 * Returns the frequency of instances at position x NOT the density
	 * @param x
	 * @return
	 */
	public double getFrequencyEstimate(double[] x){
		if(points.size()==0)
			return 0;
		
		double[][] vec=new double[x.length][1];
		for(int i=0;i<x.length;i++)
			vec[i][0]=x[i];
		Matrix point=new Matrix(vec);
		double prob=0;
		for(int i=0;i<points.size();i++){
			prob+=getNormal(point, points.get(i));
		}
		return prob;
	}

	/**
	 * Returns the density of the point. If includePoint is set to true the 
	 * point will be included to the set of points over which the density is 
	 * calculated
	 * @param point
	 * @param includePoint
	 * @return
	 */
	public double getDensity(double[] point, boolean includePoint)
	{
		// get the frequency estimation for the point
		double frequencyEstimate = getFrequencyEstimate(point);
		int numPoints = points.size();
		
		// check if the point should be included
		if(includePoint)
		{
			// if yes increase the frequency estimate by 1
			// because K(x,x) = 1
			frequencyEstimate += 1;
			// increase the number of points by 1
			numPoints += 1;
		}
		
		// normalize the result
		return frequencyEstimate / numPoints;
	}
	
	public double getNormal(Matrix x, Matrix mu){
		double prob=0;
		Matrix exponent=x.minus(mu).transpose().times(covariance.inverse()).times(x.minus(mu)); 
		double pow=exponent.get(0,0);
		pow*=-0.5;
		//double normalizer=1.0/Math.sqrt((Math.pow(2*Math.PI, dim)*covariance.determinant()));
		//prob=normalizer*Math.exp(pow);
		prob=Math.exp(pow);
		//System.out.println("powMulti = "+pow+" \t normMulti = "+normalizer);
		return prob;
	}
	
	public void initCovariance(int dim){
		dimSet=true;
		double[][] cov=new double[dim][dim];
		for(int i=0;i<dim;i++)
			for(int j=0;j<dim;j++)
				if(i==j)
					cov[i][j]=bandwidth;
				else
					cov[i][j]=0;
		this.covariance=new Matrix(cov);
	}
	
}