/**
 * [DenseMicroCluster.java] for Subspace MOA
 * 
 * A microcluster class for DBSCAN.
 * 
 * @editor Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
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
