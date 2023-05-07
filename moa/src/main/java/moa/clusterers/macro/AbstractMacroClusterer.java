/**
 * [AbstractMacroClusterer.java] for Subspace MOA
 * 
 * @author Stephen Wels
 * Data Management and Data Exploration Group, RWTH Aachen University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */
package moa.clusterers.macro;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

		// Set each cluster's ID to the ID of it's most-prevalent micro-cluster,
		// and create a mapping from the (just-set) cluster ID to the number of
		// occurrences of that ID
		HashMap<Double, Integer> countIDs = new HashMap<>();
		for (Cluster c : clustering.getClustering()) {
			// Create a mapping from micro-cluster ID to the number of occurrences of that ID
			HashMap<Double, Integer> ids = new HashMap<Double, Integer>();
			NonConvexCluster ncc = (NonConvexCluster) c;
			for (Cluster mc : ncc.getMicroClusters()) {
				if (!ids.containsKey(mc.getId()))
					ids.put(mc.getId(), 1);
				else {
					ids.put(mc.getId(), ids.get(mc.getId()) + 1);
				}
			}

			// Find the micro-cluster ID with the largest number of occurrences
			double maxID = -1d;
			int max = -1;
			for (Map.Entry<Double, Integer> entry : ids.entrySet()) {
				if (entry.getValue() >= max) {
					max = entry.getValue();
					maxID = entry.getKey();
				}
			}

			// Set the cluster's ID to the most-prevalent micro-cluster's ID
			// (-1 if the cluster has no micro-clusters)
			c.setId(maxID);

			// Add an occurrence of the new cluster ID
			if (!countIDs.containsKey(maxID))
				countIDs.put(maxID, 1);
			else {
				countIDs.put(maxID, countIDs.get(maxID) + 1);
			}

		}

		// check if there are 2 clusters with the same color (same id, could
		// appear after a split);
		double freeID = 0;
		Set<Double> reservedIDs = new HashSet<>(countIDs.keySet());
		for (Map.Entry<Double, Integer> entry : countIDs.entrySet()) {
			if (entry.getValue() > 1 || entry.getKey() == -1) {
				// find first free id, search all the clusters which has the
				// same id and replace the ids with free ids. One cluster can
				// keep its id
				int numIDsToReassign = entry.getKey() == -1 ?
							entry.getValue() :
							entry.getValue() - 1;

				for (int i = 0; i < numIDsToReassign; i++) {
					while (reservedIDs.contains(freeID) && freeID < ColorArray.getNumColors())
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
