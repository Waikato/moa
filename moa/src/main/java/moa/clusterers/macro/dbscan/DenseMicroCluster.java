/**
 * [DenseMicroCluster.java] for Subspace MOA
 * 
 * A microcluster class for DBSCAN.
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

import moa.cluster.CFCluster;

public class DenseMicroCluster {

	private CFCluster mCluster;
	private boolean mVisited;
	private boolean mIsClustered;
	
	public DenseMicroCluster(CFCluster mc){
		mCluster = mc;
		mVisited = false;
	}
	
	public void setVisited(){
		mVisited = true;
	}
	
	public boolean isVisited(){
		return mVisited;
	}
	
	public void setClustered(){
		mIsClustered = true;
	}
	
	public boolean isClustered(){
		return mIsClustered;
	}
	
	public CFCluster getCFCluster(){
		return mCluster;
	}
	
}
