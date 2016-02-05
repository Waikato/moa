/*
 *    ClusteringFeature.java
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

import moa.cluster.Cluster;
import moa.cluster.SphereCluster;

/**
 * Provides a ClusteringFeature.
 *
 * Citation: Hendrik Fichtenberger, Marc Gillé, Melanie Schmidt,
 * Chris Schwiegelshohn, Christian Sohler:
 * BICO: BIRCH Meets Coresets for k-Means Clustering.
 * ESA 2013: 481-492 (2013)
 * http://ls2-www.cs.tu-dortmund.de/bico/
 *
 */
public class ClusteringFeature extends SphereCluster {

	private static final long serialVersionUID = 1L;

	private int numPoints;
	private double[] sumPoints;
	private double sumSquaredLength;

	/**
	 * Creates a ClusteringFeature.
	 *
	 * @param center
	 *            the center point
	 * @param numPoints
	 *            the number of points
	 * @param sumPoints
	 *            the sum of all points
	 * @param sumSquaredPoints
	 *            the sum of the squared lengths
	 * @param radius
	 *            the radius
	 */
	public ClusteringFeature(double[] center, int numPoints,
			double[] sumPoints, double sumSquaredPoints, double radius) {
		super(center, radius, numPoints);
		assert (center.length == sumPoints.length);
		this.numPoints = numPoints;
		this.sumPoints = sumPoints;
		this.sumSquaredLength = sumSquaredPoints;
	}

	/**
	 * Creates a ClusteringFeature.
	 *
	 * @param center
	 *            the center point
	 * @param radius
	 *            the radius
	 */
	public ClusteringFeature(double[] center, double radius) {
		super(center, radius, 1);
		this.numPoints = 1;
		this.sumPoints = new double[center.length];
		System.arraycopy(center, 0, this.sumPoints, 0, center.length);
		this.sumSquaredLength = Metric.distanceSquared(center);
	}

	/**
	 * Adds a point to the ClusteringFeature.
	 *
	 * @param numPoints
	 *            the number of points to add
	 * @param sumPoints
	 *            the sum of points to add
	 * @param sumSquaredPoints
	 *            the sum of the squared lengths to add
	 */
	public void add(int numPoints, double[] sumPoints, double sumSquaredPoints) {
		assert (this.sumPoints.length == sumPoints.length);
		this.numPoints += numPoints;
		super.setWeight(this.numPoints);
		for (int i = 0; i < this.sumPoints.length; i++) {
			this.sumPoints[i] += sumPoints[i];
		}
		this.sumSquaredLength += sumSquaredPoints;
	}

	/**
	 * Merges the ClusteringFeature with an other ClusteringFeature.
	 *
	 * @param x
	 *            the ClusteringFeature to merge with
	 */
	public void merge(ClusteringFeature x) {
		assert (this.sumPoints.length == x.sumPoints.length);
		this.numPoints += x.numPoints;
		super.setWeight(this.numPoints);
		for (int i = 0; i < this.sumPoints.length; i++) {
			this.sumPoints[i] += x.sumPoints[i];
		}
		this.sumSquaredLength += x.sumSquaredLength;
	}

	/**
	 * Creates a Cluster of the ClusteringFeature.
	 *
	 * @return a Cluster
	 */
	public Cluster toCluster() {
		double[] output = new double[this.sumPoints.length];
		System.arraycopy(this.sumPoints, 0, output, 0, this.sumPoints.length);
		for (int i = 0; i < output.length; i++) {
			output[i] /= this.numPoints;
		}
		return new SphereCluster(output, getThreshold(), this.numPoints);
	}

	/**
	 * Creates the cluster center of the ClusteringFeature.
	 *
	 * @return the cluster center
	 */
	public double[] toClusterCenter() {
		double[] output = new double[this.sumPoints.length + 1];
		System.arraycopy(this.sumPoints, 0, output, 1, this.sumPoints.length);
		output[0] = this.numPoints;
		for (int i = 1; i < output.length; i++) {
			output[i] /= this.numPoints;
		}
		return output;
	}

	/**
	 * Writes the cluster center to a given stream.
	 *
	 * @param stream
	 *            the stream
	 * @throws IOException
	 *            If an I/O error occurs
	 */
	public void printClusterCenter(Writer stream) throws IOException {
		stream.write(String.valueOf(this.numPoints));
		for (int j = 0; j < this.sumPoints.length; j++) {
			stream.write(' ');
			stream.write(String.valueOf(this.sumPoints[j] / this.numPoints));
		}
		stream.write(System.getProperty("line.separator"));
	}

	/**
	 * Returns the number of points of the ClusteringFeature.
	 *
	 * @return the number of points
	 */
	public int getNumPoints() {
		return this.numPoints;
	}

	/**
	 * Sets the number of points of the ClusteringFeature.
	 *
	 * @param numPoints
	 *            the number of points of the ClusteringFeature to set
	 */
	public void setNumPoints(int numPoints) {
		this.numPoints = numPoints;
		super.setWeight(numPoints);
	}

	/**
	 * Returns the sum of points of the ClusteringFeature.
	 *
	 * @return the sum of points
	 */
	public double[] getSumPoints() {
		return sumPoints;
	}

	/**
	 * Sets the sum of points of the ClusteringFeature.
	 *
	 * @param sumPoints
	 *            the sum of points of the ClusteringFeature to set
	 */
	public void setSumPoints(double[] sumPoints) {
		assert (this.sumPoints.length == sumPoints.length);
		this.sumPoints = sumPoints;
	}

	/**
	 * Returns the sum of the squared lengths of the ClusteringFeature.
	 *
	 * @return the sum of the squared lengths
	 */
	public double getSumSquaredLength() {
		return sumSquaredLength;
	}

	/**
	 * Sets the sum of the squared lengths of the ClusteringFeature.
	 *
	 * @param sumSquaredLength
	 *            the sum of the squared lengths of the ClusteringFeature to set
	 */
	public void setSumSquaredLength(double sumSquaredLength) {
		this.sumSquaredLength = sumSquaredLength;
	}

	/**
	 * Returns the threshold of the ClusteringFeature.
	 *
	 * @return the threshold
	 */
	public double getThreshold() {
		return super.getRadius();
	}

	/**
	 * Sets the threshold of the ClusteringFeature.
	 *
	 * @param threshold
	 *            the threshold of the ClusteringFeature to set
	 */
	public void setThreshold(double threshold) {
		super.setRadius(threshold);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.cluster.Cluster#getDescription(java.lang.StringBuilder, int)
	 */
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		sb.append("ClusteringFeature Object");
	}

	/**
	 * Calculates the k-means costs of the ClusteringFeature too a center.
	 *
	 * @param center
	 *            the center too calculate the costs
	 * @return the costs
	 */
	public double calcKMeansCosts(double[] center) {
		assert (this.sumPoints.length == center.length);
		return this.sumSquaredLength - 2
				* Metric.dotProduct(this.sumPoints, center) + this.numPoints
				* Metric.dotProduct(center);
	}

	/**
	 * Calculates the k-means costs of the ClusteringFeature and a point too a
	 * center.
	 *
	 * @param center
	 *            the center too calculate the costs
	 * @param point
	 *            the point too calculate the costs
	 * @return the costs
	 */
	public double calcKMeansCosts(double[] center, double[] point) {
		assert (this.sumPoints.length == center.length &&
				this.sumPoints.length == point.length);
		return (this.sumSquaredLength + Metric.distanceSquared(point)) - 2
				* Metric.dotProductWithAddition(this.sumPoints, point, center)
				+ (this.numPoints + 1) * Metric.dotProduct(center);
	}

	/**
	 * Calculates the k-means costs of the ClusteringFeature and another
	 * ClusteringFeature too a center.
	 *
	 * @param center
	 *            the center too calculate the costs
	 * @param points
	 *            the points too calculate the costs
	 * @return the costs
	 */
	public double calcKMeansCosts(double[] center, ClusteringFeature points) {
		assert (this.sumPoints.length == center.length &&
				this.sumPoints.length == points.sumPoints.length);
		return (this.sumSquaredLength + points.sumSquaredLength)
				- 2 * Metric.dotProductWithAddition(this.sumPoints,
						points.sumPoints, center)
				+ (this.numPoints + points.numPoints)
				* Metric.dotProduct(center);
	}

}
