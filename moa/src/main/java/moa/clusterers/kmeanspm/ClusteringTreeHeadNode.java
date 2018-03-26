/*
 *    ClusteringTreeHeadNode.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Provides a ClusteringTreeNode with an extended nearest neighbor search in the
 * root.
 *
 * Citation: Hendrik Fichtenberger, Marc Gillé, Melanie Schmidt,
 * Chris Schwiegelshohn, Christian Sohler:
 * BICO: BIRCH Meets Coresets for k-Means Clustering.
 * ESA 2013: 481-492 (2013)
 * http://ls2-www.cs.tu-dortmund.de/bico/
 *
 */
public class ClusteringTreeHeadNode extends ClusteringTreeNode {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final int numProjections;

	private List<double[]> projections;
	private List<CuckooHashing<List<ClusteringTreeNode>>> buckets;

	/**
	 * Creates a ClusteringTreeNode with an extended nearest neighbor search in
	 * the root.
	 *
	 * @param dimension
	 *            the number of dimensions of the points
	 * @param numProjections
	 *            the number of projections to use for the nearest neighbor
	 *            search
	 * @param hashSize
	 *            size of the hash function (must be smaller than 31)
	 * @param random
	 *            instance to generate a stream of pseudorandom numbers
	 */
	public ClusteringTreeHeadNode(double[] center, ClusteringFeature cf,
			int dimension, int numProjections, int hashSize, Random random) {
		super(center, cf);
		assert(hashSize < 31);
		this.numProjections = numProjections;

		this.projections = new ArrayList<double[]>(numProjections);
		for (int i = 0; i < numProjections; i++) {
			double[] current = new double[dimension];
			double norm = 0.0;
			for (int j = 0; j < current.length; j++) {
				double d = random.nextGaussian();
				current[j] = d;
				norm += d * d;
			}
			norm = Math.sqrt(norm);
			for (int j = 0; j < current.length; j++) {
				current[j] /= norm;
			}
			this.projections.add(current);
		}

		this.buckets = new ArrayList<CuckooHashing<List<ClusteringTreeNode>>>(
				numProjections);
		for (int i = 0; i < numProjections; i++) {
			this.buckets.add(new CuckooHashing<List<ClusteringTreeNode>>(
					hashSize, random));
		}
	}

	/**
	 * Projects a point to a random projection.
	 *
	 * @param pointA
	 *            the point to project
	 * @param i
	 *            the index of the projection
	 * @return the position of the point
	 */
	private double project(double[] pointA, int i) {
		assert (this.projections.size() < i &&
				this.projections.get(i).length == pointA.length);
		return Metric.dotProduct(pointA, this.projections.get(i));
	}

	/**
	 * Projects a point to a random projection.
	 *
	 * @param pointA
	 *            the point to project
	 * @param i
	 *            the projection
	 * @return the position of the point
	 */
	private double project(double[] pointA, double[] i) {
		assert (i.length == pointA.length);
		return Metric.dotProduct(pointA, i);
	}

	/**
	 * Calculates the bucket number.
	 *
	 * @param val
	 *            position of the point
	 * @return the bucket number
	 */
	private long calcBucketNumber(double val) {
		return (long) Math.floor(val / this.getThreshold());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.evaluation.bico.ClusteringTreeNode#nearestChild(double[])
	 */
	@Override
	public ClusteringTreeNode nearestChild(double[] pointA) {
		assert (this.projections.size() > 0 &&
				this.projections.get(0).length == pointA.length);
		int minBucketProjection = -1;
		int minSize = Integer.MAX_VALUE;
		Iterator<double[]> iIterProjections = this.projections.iterator();
		Iterator<CuckooHashing<List<ClusteringTreeNode>>> iIterBuckets = this.buckets
				.iterator();
		int size = this.projections.size();
		for (int i = 0; i < size; i++) {
			long bucketNumber = calcBucketNumber(project(pointA,
					iIterProjections.next()));
			List<ClusteringTreeNode> currentBucket = iIterBuckets.next().get(
					bucketNumber);
			int bucketSize;
			if (currentBucket != null
					&& (bucketSize = currentBucket.size()) <= minSize) {
				minBucketProjection = i;
				minSize = bucketSize;
			}
		}
		if (minBucketProjection == -1) {
			return null;
		}

		long bucketNumber = calcBucketNumber(project(pointA,
				minBucketProjection));
		double minDistance = Double.POSITIVE_INFINITY;
		ClusteringTreeNode min = null;
		// for (int i = -1; i <= 1; i++) {
		List<ClusteringTreeNode> currentBucket = this.buckets.get(
				minBucketProjection).get(bucketNumber);// + i);
		if (currentBucket != null) {
			for (ClusteringTreeNode node : currentBucket) {
				double d = Metric.distance(pointA, node.getCenter());
				if (d < minDistance) {
					minDistance = d;
					min = node;
				}
			}
		}
		// }
		return min;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.evaluation.bico.ClusteringTreeNode#addChild(moa.evaluation.bico.
	 * ClusteringTreeNode)
	 */
	@Override
	public boolean addChild(ClusteringTreeNode e) {
		assert (this.projections.get(0).length == e.getClusteringFeature()
				.getCenter().length);
		super.addChild(e);
		Iterator<double[]> iIterProjections = this.projections.iterator();
		Iterator<CuckooHashing<List<ClusteringTreeNode>>> iIterBuckets = this.buckets
				.iterator();
		int size = this.projections.size();
		for (int i = 0; i < size; i++) {
			long bucketNumber = calcBucketNumber(project(e.getCenter(),
					iIterProjections.next()));
			CuckooHashing<List<ClusteringTreeNode>> currentBuckets = iIterBuckets
					.next();
			List<ClusteringTreeNode> bucket = currentBuckets.get(bucketNumber);
			if (bucket == null) {
				bucket = new ArrayList<ClusteringTreeNode>(1);
				currentBuckets.put(bucketNumber, bucket);
			}
			bucket.add(e);
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.evaluation.bico.ClusteringTreeNode#clearChildren()
	 */
	@Override
	public void clearChildren() {
		super.clearChildren();
		for (CuckooHashing<List<ClusteringTreeNode>> bucket : this.buckets) {
			bucket.clear();
		}
	}

}
