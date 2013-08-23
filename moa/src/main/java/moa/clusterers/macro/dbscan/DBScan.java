/**
 * [DBScan.java] for Subspace MOA
 * 
 * An implementation of DBSCAN.
 * 
 * @editor Yunsu Kim
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

package moa.clusterers.macro.dbscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.clusterers.macro.AbstractMacroClusterer;
import moa.clusterers.macro.NonConvexCluster;

public class DBScan extends AbstractMacroClusterer {

	Clustering datasource;
	private double mEps;
	private int mMinPts;

	public DBScan(Clustering microClusters, double eps, int MinPts) {
		datasource = microClusters;
		mEps = eps;
		mMinPts = MinPts;
	}

	private ArrayList<DenseMicroCluster> expandCluster(DenseMicroCluster dmc,
			List<DenseMicroCluster> neighbours,
			ArrayList<DenseMicroCluster> arrayList,
			Vector<DenseMicroCluster> dbmc) {

		if (!dmc.isClustered()) {
			dmc.setClustered();
			arrayList.add(dmc);
		}
		while (!neighbours.isEmpty()) {
			DenseMicroCluster mc = neighbours.get(0);
			neighbours.remove(0);
			if (!mc.isVisited()) {
				mc.setVisited();
				List<DenseMicroCluster> neighbours2 = getNeighbourhood(mc, dbmc);
				if (neighbours2.size() >= mMinPts) {
					while (!neighbours2.isEmpty()) {
						DenseMicroCluster temp = neighbours2.get(0);
						neighbours2.remove(0);
						if (!temp.isVisited()) {
							neighbours.add(temp);
						}
					}
					neighbours.addAll(neighbours2);
					if (!mc.isClustered()) {
						mc.setClustered();
						arrayList.add(mc);
					}
				}
			}
		}
		return arrayList;
	}

	private List<DenseMicroCluster> getNeighbourhood(DenseMicroCluster mc,
			Vector<DenseMicroCluster> dbmc) {
		List<DenseMicroCluster> res = new Vector<DenseMicroCluster>();
		for (DenseMicroCluster dmc : dbmc) {
			if (distance(dmc.getCFCluster().getCenter(), mc.getCFCluster().getCenter()) < mEps) {
				res.add(dmc);
			}
		}
		return res;
	}

	/**
	 * eclidean distance
	 * 
	 * @param center
	 * @param center2
	 * @return
	 */
	private double distance(double[] center, double[] center2) {
		double d = 0D;
		for (int i = 0; i < center.length; i++) {
			d += Math.pow((center[i] - center2[i]), 2);
		}
		return Math.sqrt(d);
	}

	@Override
	public Clustering getClustering(Clustering microClusters) {
		if (microClusters != null && microClusters.size() != 0) {
			Vector<DenseMicroCluster> dbmc = new Vector<DenseMicroCluster>();
			for (Cluster c : microClusters.getClustering()) {
				CFCluster cf = null;
				if (c instanceof CFCluster) {
					cf = (CFCluster) c;
					dbmc.add(new DenseMicroCluster(cf));
				} else
					throw new RuntimeException();
			}

			ArrayList<ArrayList<DenseMicroCluster>> clusters = new ArrayList<ArrayList<DenseMicroCluster>>();

			for (DenseMicroCluster dmc : dbmc) {
				if (!dmc.isVisited()) {
					dmc.setVisited();
					List<DenseMicroCluster> neighbours = getNeighbourhood(dmc,
							dbmc);
					if (neighbours.size() >= mMinPts) {
						ArrayList<DenseMicroCluster> cluster = expandCluster(
								dmc, neighbours,
								new ArrayList<DenseMicroCluster>(), dbmc);
						clusters.add(cluster);
					}
				}
			}
			// ** create big microclusters,
			// CFCluster[] res = new CFCluster[clusters.size()];
			// int clusterPos = 0;
			// for (ArrayList<DenseMicroCluster> cluster : clusters) {
			// if (cluster.size() != 0) {
			// CFCluster temp = (CFCluster) (cluster.get(0).mCluster.copy());
			// res[clusterPos] = temp;
			// for (int i = 1; i < cluster.size(); i++) {
			// res[clusterPos].add(cluster.get(i).mCluster);
			// }
			// clusterPos++;
			// }
			// }
			// Clustering result = new Clustering(res);

			// **
			CFCluster[] res = new CFCluster[clusters.size()];
			int clusterPos = 0;
			for (ArrayList<DenseMicroCluster> cluster : clusters) {
				if (cluster.size() != 0) {
					CFCluster temp = new NonConvexCluster(
							cluster.get(0).getCFCluster(),
							Convert2microclusterList(cluster));
					res[clusterPos] = temp;
					for (int i = 1; i < cluster.size(); i++) {
						res[clusterPos].add(cluster.get(i).getCFCluster());
					}
					clusterPos++;
				}
			}

			// //// count Noise
			int noise = 0;
			for (DenseMicroCluster c : dbmc) {
				if (!c.isClustered()) {
					noise++;
				}
			}
			System.out.println("microclusters which are not clustered:: "
					+ noise);
			Clustering result = new Clustering(res);
			setClusterIDs(result);
			// int i = 0;
			// for (Cluster c : result.getClustering()) {
			// c.setId(i++);
			// }
			return result;
		}
		return new Clustering();
	}

	private List<CFCluster> Convert2microclusterList(
			ArrayList<DenseMicroCluster> cluster) {
		List<CFCluster> cfCluster = new Vector<CFCluster>();
		for (DenseMicroCluster d : cluster) {
			cfCluster.add(d.getCFCluster());
		}
		return cfCluster;
	}
}
