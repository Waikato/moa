/**
 * [IMacroClusterer.java] for Subspace MOA
 * 
 * @author Stephen Wels
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
package moa.clusterers.macro;

import moa.cluster.Clustering;

public interface IMacroClusterer {

	public Clustering getClustering(Clustering microClusters);
}
