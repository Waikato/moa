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
	
	private Matrix invCovariance;
	private double bandwidth;
	private ArrayList<Matrix> points;
	private boolean dimSet=false;
	
	public MCPALEstimatorMultivariate(double bandwidth){
		this.bandwidth=bandwidth;
		this.points = new ArrayList<Matrix>();
	}
	
	public void addValue(double[] x){
		Matrix point=doubleArrayToMatrix(x);
		points.add(point);
		
		if(points.size() > 100)
		{
			points.remove(0);
		}
		
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

		Matrix point=doubleArrayToMatrix(x);
		double prob=0;
		for(int i=0;i<points.size();i++){
			prob+=getNormal(point, points.get(i));
		}
		return prob;
	}
	
	public double getNormal(Matrix x, Matrix mu){
		double prob=0;
		Matrix diff = x.minus(mu);
		Matrix exponent=diff.transpose().times(invCovariance).times(diff); 
		double pow=-0.5 * exponent.get(0,0);

		prob=Math.exp(pow);

		return prob;
	}
	
	private Matrix doubleArrayToMatrix(double[] x)
	{
		double[][] vec=new double[x.length][1];
		for(int i=0;i<x.length;i++)
			vec[i][0]=x[i];
		return new Matrix(vec);
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
		this.invCovariance=(new Matrix(cov)).inverse();
	}
	
	/**
	 * get the number of instances used to estimate the frequency
	 * @return number of points
	 */
	public int getNumPoints(){
		return points.size();
	}
	
}