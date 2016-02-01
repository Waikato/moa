/*
 *    KMeansCosts.java
 *    Copyright (C) 2015 TU Dortmund University, Germany
 *    @author Jan Stallmann (jan.stallmann@tu-dortmund.de)
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
package moa.evaluation;

import java.util.ArrayList;

import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;

/**
 * Calculates the k-means costs.
 * 
 */
public class KMeansCosts extends MeasureCollection {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a MeasureCollection to calculate the k-means costs.
	 * 
	 */
	public KMeansCosts() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.evaluation.MeasureCollection#getNames()
	 */
	@Override
	protected String[] getNames() {
		return new String[] { "k-means costs" };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.evaluation.MeasureCollection#getDefaultEnabled()
	 */
	@Override
	protected boolean[] getDefaultEnabled() {
		return new boolean[] { true };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.evaluation.MeasureCollection#evaluateClustering(moa.cluster.
	 * Clustering, moa.cluster.Clustering, java.util.ArrayList)
	 */
	@Override
	protected void evaluateClustering(Clustering clustering,
			Clustering trueClustering, ArrayList<DataPoint> points) {
		double[][] center = new double[clustering.size()][];
		for (int i = 0; i < clustering.size(); i++) {
			center[i] = clustering.get(i).getCenter();
		}

		double costs = 0.0;
		for (int l = 0; l < points.size(); l++) {
			double minDistance = Double.MAX_VALUE;
			for (int i = 0; i < clustering.size(); i++) {
				double distance = 0.0;
				for (int j = 0; j < center[i].length; j++) {
					double d = points.get(l).value(j) - center[i][j];
					distance += d * d;
				}
				minDistance = Math.min(minDistance, distance);
			}
			costs += minDistance;
		}

		addValue(0, costs);
	}

}
