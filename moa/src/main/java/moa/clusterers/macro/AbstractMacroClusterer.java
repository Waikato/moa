/**
 * [AbstractMacroClusterer.java] for Subspace MOA
 * 
 * @author Stephen Wels
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
package moa.clusterers.macro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import moa.cluster.Cluster;
import moa.cluster.Clustering;

public abstract class AbstractMacroClusterer {

	public abstract Clustering getClustering(Clustering microClusters);

	protected void setClusterIDs(Clustering clustering) {
		// int numOfClusters = clustering.size();
		// Set<Double> oldClusterIDs = new TreeSet<Double>();
		//
		// // Collect all the old IDs of the microclusters
		// for (Cluster c : clustering.getClustering()) {
		// NonConvexCluster ncc = (NonConvexCluster) c;
		// for (Cluster mc : ncc.mMicroClusters) {
		// if (!oldClusterIDs.contains(mc.getId()))
		// oldClusterIDs.add(mc.getId());
		// }
		// }

		HashMap<Double, Integer> countIDs = new HashMap<Double, Integer>();
		for (Cluster c : clustering.getClustering()) {
			HashMap<Double, Integer> ids = new HashMap<Double, Integer>();
			NonConvexCluster ncc = (NonConvexCluster) c;
			for (Cluster mc : ncc.getMicroClusters()) {
				if (!ids.containsKey(mc.getId()))
					ids.put(mc.getId(), new Integer(1));
				else {
					int i = ids.get(mc.getId());
					i++;
					ids.put(mc.getId(), i);
				}
			}
			// find max
			double maxID = -1d;
			int max = -1;
			for (Map.Entry<Double, Integer> entry : ids.entrySet()) {
				if (entry.getValue() >= max) {
					max = entry.getValue();
					maxID = entry.getKey();
				}
			}
			c.setId(maxID);

			if (!countIDs.containsKey(maxID))
				countIDs.put(maxID, new Integer(1));
			else {
				int i = countIDs.get(maxID);
				i++;
				countIDs.put(maxID, i);
			}

		}

		// check if there are 2 clusters with the same color (same id, could
		// appear after a split);
		double freeID = 0;
		List<Double> reservedIDs = new Vector<Double>();
		reservedIDs.addAll(countIDs.keySet());
		for (Map.Entry<Double, Integer> entry : countIDs.entrySet()) {
			if (entry.getValue() > 1 || entry.getKey() == -1) {
				// find first free id, search all the clusters which has the
				// same id and replace the ids with free ids. One cluster can
				// keep its id
				int to = entry.getValue();
				if (entry.getKey() != -1)
					to--;

				for (int i = 0; i < to; i++) {
					while (reservedIDs.contains(freeID)
							&& freeID < ColorArray.getNumColors())
						freeID += 1.0;
					for (int c = clustering.size() - 1; c >= 0; c--)
						if (clustering.get(c).getId() == entry.getKey()) {
							clustering.get(c).setId(freeID);
							reservedIDs.add(freeID);
							break;
						}
				}
			}
		}

		for (Cluster c : clustering.getClustering()) {
			NonConvexCluster ncc = (NonConvexCluster) c;
			for (Cluster mc : ncc.getMicroClusters()) {
				mc.setId(c.getId());
			}
		}
	}
}
