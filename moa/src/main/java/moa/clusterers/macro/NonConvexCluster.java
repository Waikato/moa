/**
 * [NonConvexCluster.java] for Subspace MOA
 * 
 * A set of [CFCluster]s, grouped as a non-convex cluster.
 * 
 * @author Stephen Wels
 * @editor Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.macro;

import java.util.List;
import java.util.Vector;

import moa.cluster.CFCluster;
import moa.cluster.Clustering;
import weka.core.Instance;

public class NonConvexCluster extends CFCluster implements IDenseMacroCluster {

	List<CFCluster> mMicroClusters;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NonConvexCluster(CFCluster cluster, List<CFCluster> microclusters) {
		super(cluster); // required
		mMicroClusters = new Vector<CFCluster>();
		mMicroClusters.addAll(microclusters);
		// assuming we have a circular shaped cluster, compute it's center: only
		// for visualization
		for (CFCluster cf : microclusters) {
			if (!cf.equals(cluster))
				this.add(cf);
		}
	}

	@Override
	public CFCluster getCF() {
		// TODO Auto-generated method stub
		return this;
	}

	public void insert(CFCluster cf) {
		mMicroClusters.add(cf);
	}

	public void remove(CFCluster cf) {
		mMicroClusters.remove(cf);
	}

	@Override
	public double getInclusionProbability(Instance instance) {
		double probability = 0;
		for (CFCluster cf : mMicroClusters) {
			probability = cf.getInclusionProbability(instance);
			if (probability > 0d)
				return probability;
		}
		return probability;
	}

	@Override
	public double getRadius() {
		// zero since this is an arbitrarily shaped cluster
		return 0;
	}

	public Clustering getClustering() {
		Clustering c = new Clustering();
		for (CFCluster mc : mMicroClusters)
			c.add(mc);
		return c;
	}

	public List<CFCluster> getMicroClusters() {
		return mMicroClusters;
	}

}
