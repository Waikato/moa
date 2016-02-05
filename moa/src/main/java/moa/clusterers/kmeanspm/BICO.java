/*
 *    BICO.java
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
package moa.clusterers.kmeanspm;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.clusterers.AbstractClusterer;
import moa.core.AutoExpandVector;
import moa.core.Measurement;

/**
 * A instance of this class provides the BICO clustering algorithm.
 *
 * Citation: Hendrik Fichtenberger, Marc Gillé, Melanie Schmidt,
 * Chris Schwiegelshohn, Christian Sohler:
 * BICO: BIRCH Meets Coresets for k-Means Clustering.
 * ESA 2013: 481-492 (2013)
 * http://ls2-www.cs.tu-dortmund.de/bico/
 *
 */
public class BICO extends AbstractClusterer {

	private static final long serialVersionUID = 1L;

	public IntOption numClustersOption = new IntOption("Cluster", 'k',
			"Number of desired centers.", 5, 1, Integer.MAX_VALUE);

	public IntOption numDimensionsOption = new IntOption("Dimensions", 'd',
			"Number of the dimensions of the input points.", 10, 1,
			Integer.MAX_VALUE);

	public IntOption maxNumClusterFeaturesOption = new IntOption(
			"MaxClusterFeatures", 'n', "Maximum size of the coreset.", 5 * 200, 1,
			Integer.MAX_VALUE);

	public IntOption numProjectionsOption = new IntOption("Projections", 'p',
			"Number of random projections used for the nearest neighbour search.",
			10, 1, Integer.MAX_VALUE);

	protected int numClusters;
	protected int numDimensions;
	protected int maxNumClusterFeatures;
	protected int numProjections;

	private boolean bufferPhase;
	private List<double[]> buffer;
	private double minDistance;
	private int pairwiseDifferent;

	private ClusteringTreeNode root;
	private int rootCount;
	private double T;

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.clusterers.Clusterer#isRandomizable()
	 */
	@Override
	public boolean isRandomizable() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.clusterers.AbstractClusterer#implementsMicroClusterer()
	 */
	@Override
	public boolean implementsMicroClusterer() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.clusterers.AbstractClusterer#getMicroClusteringResult()
	 */
	@Override
	public Clustering getMicroClusteringResult() {
		return this.root.addToClustering(new Clustering(
				new AutoExpandVector<Cluster>(this.rootCount)));
	}

	/**
	 * Writes all micro cluster to a given stream.
	 *
	 * @param stream
	 *            the stream
	 * @throws IOException
	 *            If an I/O error occurs
	 */
	public void printMicroClusteringResult(Writer stream) throws IOException {
		this.root.printClusteringCenters(stream);
	}

	/**
	 * Returns the current size of the micro clustering.
	 *
	 * @return The size of the micro clustering
	 */
	public int getMicroClusteringSize() {
		return this.rootCount;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.clusterers.Clusterer#getVotesForInstance(weka.core.Instance)
	 */
	@Override
	public double[] getVotesForInstance(Instance inst) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.clusterers.Clusterer#getClusteringResult()
	 */
	@Override
	public Clustering getClusteringResult() {
		List<double[]> microClustering;
		// If the buffer phase is active then the buffer (without duplicates) is
		// equal to the micro clustering
		if (this.bufferPhase) {
			microClustering = new ArrayList<double[]>(this.buffer.size());
			for (double[] pointA : this.buffer) {
				// Searches for a duplicate of the current point
				boolean duplicate = false;
				for (double[] pointB : microClustering) {
					int i;
					for (i = 0; i < pointA.length; i++) {
						if (pointA[i] != pointB[i + 1]) {
							break;
						}
					}
					if (i == pointA.length) {
						duplicate = true;
						pointB[0] += 1;
						break;
					}
				}

				if (!duplicate) {
					// Copies the point to an array with the weight in the first
					// element
					double[] pointWeighted = new double[pointA.length + 1];
					pointWeighted[0] = 1;
					System.arraycopy(pointA, 0, pointWeighted, 1, pointA.length);
					microClustering.add(pointWeighted);
				}
			}
		} else {
			// Collects all nodes as arrays with the weight in the first element
			microClustering = this.root
					.addToClusteringCenters(new ArrayList<double[]>(this.rootCount));
		}
		// Runs the k-means algorithm five times on the micro clustering
		List<double[]> result = null;
		double minValue = Double.POSITIVE_INFINITY;
		for (int i = 0; i < 5; i++) {
			// Creates the starting centroids
			List<double[]> newResult = CoresetKMeans
					.generatekMeansPlusPlusCentroids(this.numClusters,
							microClustering, this.clustererRandom);
			// Runs the k-means algorithm with changing the starting centroids
			double newValue = CoresetKMeans.kMeans(newResult, microClustering);
			// Selects the result with minimum costs
			if (newValue < minValue) {
				result = newResult;
				minValue = newValue;
			}
		}

		// Prepares the points for the clustering result
		AutoExpandVector<Cluster> resultClustering = new AutoExpandVector<Cluster>(
				result.size());
		for (double[] point : result) {
			resultClustering.add(new SphereCluster(point, 0.0));
		}
		return new Clustering(resultClustering);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.clusterers.AbstractClusterer#resetLearningImpl()
	 */
	@Override
	public void resetLearningImpl() {
		this.numClusters = this.numClustersOption.getValue();
		this.numDimensions = this.numDimensionsOption.getValue();
		this.maxNumClusterFeatures = this.maxNumClusterFeaturesOption.getValue();
		this.numProjections = this.numProjectionsOption.getValue();

		this.bufferPhase = true;
		this.buffer = new ArrayList<double[]>(
				this.maxNumClusterFeaturesOption.getValue() + 1);
		this.minDistance = Double.POSITIVE_INFINITY;
		this.pairwiseDifferent = 0;

		int hashSize = (int) Math.ceil(Math.log(
				5 * this.maxNumClusterFeaturesOption.getValue()) / Math.log(2));
		this.root = new ClusteringTreeHeadNode(null, new ClusteringFeature(
				new double[0], 1), this.numDimensionsOption.getValue(),
				this.numProjectionsOption.getValue(), Math.min(hashSize, 30),
				this.clustererRandom);
		this.rootCount = 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * moa.clusterers.AbstractClusterer#trainOnInstanceImpl(weka.core.Instance)
	 */
	@Override
	public void trainOnInstanceImpl(Instance inst) {
		double[] x = inst.toDoubleArray();
		if (this.numDimensions != x.length) {
			System.out.println("Line skipped because line dimension is "
					+ x.length + " instead of " + this.numDimensions);
			return;
		}
		// Starts with the buffer phase to calculate the starting threshold
		if (this.bufferPhase) {
			// Calculates the pairwise distance to all unequal point in the
			// buffer
			for (double[] point : this.buffer) {
				double d = Metric.distanceSquared(point, x);
				if (d > 0) {
					this.pairwiseDifferent++;
					if (d < minDistance) {
						minDistance = d;
					}
				}
			}
			this.buffer.add(x);

			// Checks if the buffer is large enough
			if (this.pairwiseDifferent >= this.maxNumClusterFeatures + 1) {
				// Calculates the starting threshold
				this.T = 16 * minDistance;
				this.root.setThreshold(calcRSquared(1));
				this.bufferPhase = false;
				// Adds all points to the ClusteringFeature tree
				for (double[] point : this.buffer) {
					bicoUpdate(point);
				}
				this.buffer.clear();
				this.buffer = null;
			}
		} else {
			// Adds the point directly to the ClusteringFeature tree
			bicoUpdate(x);
		}
	}

	/**
	 * Inserts a new point into the ClusteringFeature tree.
	 *
	 * @param x
	 *            the point
	 */
	protected void bicoUpdate(double[] x) {
		assert (!this.bufferPhase && this.numDimensions == x.length);
		// Starts with the global root node as the current root node
		ClusteringTreeNode r = this.root;
		int i = 1;
		while (true) {
			ClusteringTreeNode y = r.nearestChild(x);
			// Checks if the point can not be added to the current level
			if (r.hasNoChildren()
					|| y == null
					|| Metric.distanceSquared(x, y.getCenter()) > calcRSquared(i)) {
				// Creates a new node for the point and adds it to the current
				// root node
				r.addChild(new ClusteringTreeNode(x, new ClusteringFeature(x, calcR(i))));
				this.rootCount++;
				break;
			} else {
				// Checks if the point can be added to the nearest node without
				// exceeding the global threshold
				if (y.getClusteringFeature().calcKMeansCosts(y.getCenter(), x) <= this.T) {
					// Adds the point to the ClusteringFeature
					y.getClusteringFeature().add(1, x, Metric.distanceSquared(x));
					break;
				} else {
					// Navigates one level down in the tree
					r = y;
					i++;
				}
			}
		}
		// Checks if the number of nodes in the tree exceeds the maximum number
		if (this.rootCount > this.maxNumClusterFeatures) {
			rebuild();
		}
	}

	/**
	 * If the number of ClusteringTreeNodes exceeds the maximum bound, the
	 * global threshold T will be doubled and the tree will be rebuild with the
	 * new threshold.
	 *
	 */
	protected void rebuild() {
		// Checks if the number of nodes in the tree exceeds the maximum number
		while (this.rootCount > this.maxNumClusterFeatures) {
			// Doubles the global threshold
			this.T *= 2.0;
			this.root.setThreshold(calcRSquared(1));
			// Adds all nodes to the ClusteringFeature tree again
			Queue<ClusteringTreeNode> Q = new LinkedList<ClusteringTreeNode>();
			Q.addAll(this.root.getChildren());
			this.root.clearChildren();
			this.rootCount = 0;
			while (!Q.isEmpty()) {
				ClusteringTreeNode x = Q.element();
				Q.addAll(x.getChildren());
				x.clearChildren();
				bicoCFUpdate(x);
				Q.remove();
			}
		}
	}

	/**
	 * Inserts a ClusteringTreeNode into the ClusteringFeature tree.
	 *
	 * @param x
	 *            the ClusteringTreeNode
	 */
	protected void bicoCFUpdate(ClusteringTreeNode x) {
		// Starts with the global root node as the current root node
		ClusteringTreeNode r = this.root;
		int i = 1;
		while (true) {
			ClusteringTreeNode y = r.nearestChild(x.getCenter());
			// Checks if the node can not be merged to the current level
			if (r.hasNoChildren()
					|| y == null
					|| Metric.distanceSquared(x.getCenter(), y.getCenter()) > calcRSquared(i)) {
				// Adds the node to the current root node
				x.setThreshold(calcR(i));
				r.addChild(x);
				this.rootCount++;
				break;
			} else {
				// Checks if the node can be merged to the nearest node without
				// exceeding the global threshold
				if (y.getClusteringFeature().calcKMeansCosts(y.getCenter(),
						x.getClusteringFeature()) <= this.T) {
					// Merges the ClusteringFeature of the node to the
					// ClusteringFeature of the nearest node
					y.getClusteringFeature().merge(x.getClusteringFeature());
					break;
				} else {
					// Navigates one level down in the tree
					r = y;
					i++;
				}
			}
		}
	}

	/**
	 * Calculates the squared threshold at a specific level in the
	 * ClusteringFeature tree.
	 *
	 * @param level
	 *            level in the tree
	 * @return the squared threshold
	 */
	protected double calcRSquared(int level) {
		return this.T / (double) (1 << (3 + level));
	}

	/**
	 * Calculates the threshold at a specific level in the ClusteringFeature
	 * tree.
	 *
	 * @param level
	 *            level in the tree
	 * @return the threshold
	 */
	protected double calcR(int level) {
		return Math.sqrt(calcRSquared(level));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.clusterers.AbstractClusterer#getModelMeasurementsImpl()
	 */
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * moa.clusterers.AbstractClusterer#getModelDescription(java.lang.StringBuilder, int)
	 */
	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
