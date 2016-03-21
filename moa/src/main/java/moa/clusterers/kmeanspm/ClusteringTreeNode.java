/*
 *    ClusteringTreeNode.java
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
import java.util.Collections;
import java.util.List;

import moa.AbstractMOAObject;
import moa.cluster.Clustering;

/**
 * Provides a tree of ClusterFeatures.
 *
 * Citation: Hendrik Fichtenberger, Marc Gillé, Melanie Schmidt,
 * Chris Schwiegelshohn, Christian Sohler:
 * BICO: BIRCH Meets Coresets for k-Means Clustering.
 * ESA 2013: 481-492 (2013)
 * http://ls2-www.cs.tu-dortmund.de/bico/
 *
 */
public class ClusteringTreeNode extends AbstractMOAObject {

	private static final long serialVersionUID = 1L;

	private double[] center;
	private final ClusteringFeature clusteringFeature;
	private List<ClusteringTreeNode> children;

	/**
	 * Creates a tree node for a ClusterFeature.
	 *
	 * @param center
	 *            representation of the ClusterFeature
	 * @param cf
	 *            the ClusterFeature
	 */
	public ClusteringTreeNode(double[] center, ClusteringFeature cf) {
		assert (center == null || center.length == cf.getSumPoints().length);
		this.center = center;
		this.clusteringFeature = cf;
		this.children = new ArrayList<ClusteringTreeNode>();
	}

	/**
	 * Counts the elements in tree with this node as the root.
	 *
	 * @deprecated
	 * @return the number of elements
	 */
	@Deprecated
	public int count() {
		int count = (clusteringFeature != null) ? 1 : 0;
		for (ClusteringTreeNode child : children) {
			count += child.count();
		}
		return count;
	}

	/**
	 * Adds all ClusterFeatures of the tree with this node as the root to a
	 * Clustering.
	 *
	 * @param clustering
	 *            the Clustering to add the ClusterFeatures too.
	 * @return the input Clustering
	 */
	public Clustering addToClustering(Clustering clustering) {
		if (center != null && getClusteringFeature() != null) {
			clustering.add(getClusteringFeature().toCluster());
		}
		for (ClusteringTreeNode child : children) {
			child.addToClustering(clustering);
		}
		return clustering;
	}

	/**
	 * Adds all clustering centers of the ClusterFeatures of the tree with this
	 * node as the root to a List of points.
	 *
	 * @param clustering
	 *            the List to add the clustering centers too.
	 * @return the input List
	 */
	public List<double[]> addToClusteringCenters(List<double[]> clustering) {
		if (center != null && getClusteringFeature() != null) {
			clustering.add(getClusteringFeature().toClusterCenter());
		}
		for (ClusteringTreeNode child : children) {
			child.addToClusteringCenters(clustering);
		}
		return clustering;
	}

	/**
	 * Writes all clustering centers of the ClusterFeatures of the tree with this
	 * node as the root to a given stream.
	 *
	 * @param stream
	 *            the stream
	 * @throws IOException
	 *            If an I/O error occurs
	 */
	public void printClusteringCenters(Writer stream) throws IOException {
		if (center != null && getClusteringFeature() != null) {
			getClusteringFeature().printClusterCenter(stream);
		}
		for (ClusteringTreeNode child : children) {
			child.printClusteringCenters(stream);
		}
	}

	/**
	 * Gets the representation of the ClusteringFeature
	 *
	 * @return the representation of the ClusteringFeature
	 */
	public double[] getCenter() {
		return center;
	}

	/**
	 * Sets the representation of the ClusteringFeature
	 *
	 * @param center
	 *            the representation of the ClusteringFeature to set
	 */
	public void setCenter(double[] center) {
		assert (this.center.length == center.length);
		this.center = center;
	}

	/**
	 * Gets the ClusteringFeature of this node.
	 *
	 * @return the ClusteringFeature
	 */
	public ClusteringFeature getClusteringFeature() {
		return clusteringFeature;
	}

	/**
	 * Gets a <code>List</code> of the children nodes.
	 *
	 * @return a <code>List</code> of the children nodes
	 */
	public List<ClusteringTreeNode> getChildren() {
		return Collections.unmodifiableList(this.children);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see moa.MOAObject#getDescription(java.lang.StringBuilder, int)
	 */
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		sb.append("ClusteringFeature Tree Object");
	}

	/**
	 * Searches for the nearest child node by comparing each representation.
	 *
	 * @param pointA
	 *            to find the nearest child for
	 * @return the child node which is the nearest
	 */
	public ClusteringTreeNode nearestChild(double[] pointA) {
		assert (this.center.length == pointA.length);
		double minDistance = Double.POSITIVE_INFINITY;
		ClusteringTreeNode min = null;
		for (ClusteringTreeNode node : this.getChildren()) {
			double d = Metric.distance(pointA, node.getCenter());
			if (d < minDistance) {
				minDistance = d;
				min = node;
			}
		}
		return min;
	}

	/**
	 * Adds a child node.
	 *
	 * @param e
	 *            the child node to add
	 * @return <code>true</code>
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean addChild(ClusteringTreeNode e) {
		assert (this.center.length == e.center.length);
		return this.children.add(e);
	}

	/**
	 * Removes all children nodes.
	 *
	 * @see java.util.List#clear()
	 */
	public void clearChildren() {
		this.children.clear();
	}

	/**
	 * Returns <code>true</code> if this node contains no children nodes.
	 *
	 * @return <code>true</code> if this node contains no children nodes
	 * @see java.util.List#isEmpty()
	 */
	public boolean hasNoChildren() {
		return this.children.isEmpty();
	}

	/**
	 * Gets the threshold of this node.
	 *
	 * @return the threshold
	 */
	public double getThreshold() {
		return this.clusteringFeature.getThreshold();
	}

	/**
	 * Gets the threshold of this node.
	 *
	 * @param threshold
	 *            the threshold to set
	 */
	public void setThreshold(double threshold) {
		this.clusteringFeature.setThreshold(threshold);
	}

}
