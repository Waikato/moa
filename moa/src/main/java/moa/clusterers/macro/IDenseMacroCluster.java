/**
 * [IDenseMacroCluster.java] for Subspace MOA
 * 
 * @author Stephen Wels
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
package moa.clusterers.macro;

import java.util.List;

import moa.cluster.CFCluster;
import moa.cluster.Clustering;

public interface IDenseMacroCluster {
	public Clustering getClustering();

	public List<CFCluster> getMicroClusters();
}
