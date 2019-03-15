/**
 * [NonConvexCluster.java] for Subspace MOA
 * 
 * A set of [CFCluster]s, grouped as a non-convex cluster.
 * 
 * @author Stephen Wels
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
package moa.clusterers.macro;

import java.util.List;
import java.util.Vector;

import moa.cluster.CFCluster;
import moa.cluster.Clustering;
import com.yahoo.labs.samoa.instances.Instance;

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
