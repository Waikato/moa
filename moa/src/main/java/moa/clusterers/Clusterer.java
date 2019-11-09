/*
 *    Clusterer.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet@cs.waikato.ac.nz)
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
package moa.clusterers;

import moa.MOAObject;
import moa.classifiers.semisupervised.attributeSimilarity.AttributeSimilarityCalculator;
import moa.classifiers.semisupervised.attributeSimilarity.EuclideanDistanceSimilarityCalculator;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.Measurement;
import moa.gui.AWTRenderable;
import moa.options.OptionHandler;
import com.yahoo.labs.samoa.instances.Instance;

import java.util.List;

public interface Clusterer extends MOAObject, OptionHandler, AWTRenderable {

	public void setModelContext(InstancesHeader ih);

	public InstancesHeader getModelContext();

	public boolean isRandomizable();

	public void setRandomSeed(int s);

	public boolean trainingHasStarted();

	public double trainingWeightSeenByModel();

	public void resetLearning();

	public void trainOnInstance(Instance inst);

	public double[] getVotesForInstance(Instance inst);

	public Measurement[] getModelMeasurements();

	public Clusterer[] getSubClusterers();

	public Clusterer copy();

    public Clustering getClusteringResult();

    public boolean implementsMicroClusterer();

    public Clustering getMicroClusteringResult();
    
    public boolean keepClassLabel();

    /////////////////////////////////////////////////////////////////
    // From this point onward it concerns semi-supervised learning //
	/////////////////////////////////////////////////////////////////

	/**
	 * Gets the one cluster that is updated during the update on an instance X.
	 * It may return null, since the clustering mechanism varies by clusterers.
	 * @return the one cluster being updated after the training phase.
	 */
    public Cluster getUpdatedCluster();

	/**
	 * Gets the cluster nearest to an instance X.
	 * Each clusterer makes use of different
	 * distance measures to find the nearest cluster
	 * @return
	 */
	public Cluster getNearestCluster(Instance X, boolean includeClass);

	/**
	 * Returns the confidence that the point X falls into the nearest cluster
	 * @param X an instance
	 * @return the confidence level (0.0 to 1.0)
	 */
	public double getConfidenceLevel(Instance X, Cluster C);

	/**
	 * Computes the euclidean distance between two points.
	 * @param p1 point 1
	 * @param p2 point 2
	 * @param excludes list of attributes that are excluded from the computation
	 * @return the euclidean distance
	 */
	public static double distance(double[] p1, double[] p2, List<Integer> excludes) {
		double distance = 0.0;
		for (int i = 0; i < p1.length; i++) {
			// sometimes, the value of the missing class is NaN & the final distance is NaN (which we don't want)
			if (!Double.isNaN(p1[i]) && !Double.isNaN(p2[i]) && (excludes == null || !excludes.contains(i))) {
				double d = p1[i] - p2[i];
				distance += d * d;
			}
		}
		return Math.sqrt(distance);
	}

	public static double distance(double[] p1, double[] p2) {
		double distance = 0.0;
		for (int i = 0; i < p1.length; i++) {
			// sometimes, the value of the missing class is NaN & the final distance is NaN (which we don't want)
			if (!Double.isNaN(p1[i]) && !Double.isNaN(p2[i])) {
				double d = p1[i] - p2[i];
				distance += d * d;
			}
		}
		return Math.sqrt(distance);
	}

	/**
	 * Computes the distance between two points, such that: numeric attributes are computed with Euclidean didstance,
	 * and nominal attributes are computed with an attribute similarity observer
	 * @param p1 point 1
	 * @param p2 point 2
	 * @param excludes list of attributed excluded from the distance computation
	 * @param obs the attribute similarity observer
	 * @return the distance between 2 points
	 */
	public static double distance(Instance p1, Instance p2, List<Integer> excludes, AttributeSimilarityCalculator obs) {
		double distance = 0.0;

		// compute distances between numeric attributes
		for (int i = 0; i < p1.numAttributes(); i++) {
			if (excludes != null && excludes.contains(i)) continue;
			if ((p1.attribute(i).isNumeric() && p2.attribute(i).isNumeric()) ||
					(obs instanceof EuclideanDistanceSimilarityCalculator)) {
				if (!Double.isNaN(p1.value(i)) && !Double.isNaN(p2.value(i))) {
					double d = p1.value(i) - p2.value(i);
					distance += d * d;
				}
			}
		}
		distance = Math.sqrt(distance);

		// compute similarity-based distances between nominal attributes
		if (obs != null && !(obs instanceof EuclideanDistanceSimilarityCalculator)) {
			double similarity = obs.computeSimilarityOfInstance(p1, p2);
			distance += (1 / similarity - 1);
		}

		return distance;
	}

	public void setExcludeLabel(boolean excludeLabel);

	public List<Integer> getExcludedAttributes(Instance X);
}
