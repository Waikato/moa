/*
 *    CentroidsOutput.java
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

import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;

/**
 * Prints the calculated clustering.
 * 
 */
public class CentroidsOutput extends MeasureCollection {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a MeasureCollection to print the calculated clustering.
	 * 
	 */
	public CentroidsOutput() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.evaluation.MeasureCollection#getNames()
	 */
	@Override
	public String[] getNames() {
		String[] names = { "Centroids Output" };
		return names;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.evaluation.MeasureCollection#getDefaultEnabled()
	 */
	@Override
	protected boolean[] getDefaultEnabled() {
		boolean[] defaults = { false };
		return defaults;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * moa.evaluation.MeasureCollection#evaluateClustering(moa.cluster.Clustering
	 * , moa.cluster.Clustering, java.util.ArrayList)
	 */
	@Override
	public void evaluateClustering(Clustering clustering,
			Clustering trueClustering, ArrayList<DataPoint> points) {
		System.out.println("Centroids: ");
		for (Cluster cluster : clustering.getClustering()) {
			double[] point = cluster.getCenter();
			System.out.print('1');
			for (int i = 0; i < point.length; i++) {
				System.out.print(' ');
				System.out.print(point[i]);
			}
			System.out.println();
		}
	}

}
