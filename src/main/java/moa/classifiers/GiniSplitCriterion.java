/*
 *    GiniSplitCriterion.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.classifiers;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;
import weka.core.Utils;

public class GiniSplitCriterion extends AbstractOptionHandler implements
		SplitCriterion {

	private static final long serialVersionUID = 1L;

	public double getMeritOfSplit(double[] preSplitDist,
			double[][] postSplitDists) {
		double totalWeight = 0.0;
		double[] distWeights = new double[postSplitDists.length];
		for (int i = 0; i < postSplitDists.length; i++) {
			distWeights[i] = Utils.sum(postSplitDists[i]);
			totalWeight += distWeights[i];
		}
		double gini = 0.0;
		for (int i = 0; i < postSplitDists.length; i++) {
			gini += (distWeights[i] / totalWeight)
					* computeGini(postSplitDists[i], distWeights[i]);
		}
		return 1.0 - gini;
	}

	public double getRangeOfMerit(double[] preSplitDist) {
		return 1.0;
	}

	public static double computeGini(double[] dist, double distSumOfWeights) {
		double gini = 1.0;
		for (int i = 0; i < dist.length; i++) {
			double relFreq = dist[i] / distSumOfWeights;
			gini -= relFreq * relFreq;
		}
		return gini;
	}

	public static double computeGini(double[] dist) {
		return computeGini(dist, Utils.sum(dist));
	}

	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		// TODO Auto-generated method stub

	}

}
