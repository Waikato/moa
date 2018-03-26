/**
 *    GridCluster.java
 *    
 *    @author Richard Hugh Moulton  (rmoul026 -[at]- uottawa dot ca)
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
 */

package moa.clusterers.dstream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.yahoo.labs.samoa.instances.Instance;

import moa.cluster.CFCluster;
import moa.clusterers.macro.NonConvexCluster;

/**
 * Grid Clusters are defined in Definition 3.6 of Chen and Tu 2007 as:
 * Let G =(g1, ·· · ,gm) be a grid group, if every inside grid of G is 
 * a dense grid and every outside grid is either a dense grid or a 
 * transitional grid, then G is a grid cluster.
 * 
 * Citation: Y. Chen and L. Tu, “Density-Based Clustering for Real-Time Stream Data,” in
 * Proceedings of the 13th ACM SIGKDD international conference on Knowledge discovery and
 * data mining, 2007, pp. 133–142.
 */
public class GridCluster extends NonConvexCluster
{
	private static final long serialVersionUID = -6498733665209706370L;
	private HashMap<DensityGrid, Boolean> grids;
	private HashMap<DensityGrid, Boolean> visited;
	private int clusterLabel;
	
	public GridCluster(CFCluster cluster, List<CFCluster> microclusters, int label)
	{
		super(cluster, microclusters);
		this.grids = new HashMap<DensityGrid,Boolean>();
		this.clusterLabel = label;
	}
	
	public GridCluster(CFCluster cluster, List<CFCluster> microclusters, HashMap<DensityGrid,Boolean> hashMap, int label)
	{
		super(cluster, microclusters);
		this.grids = new HashMap<DensityGrid,Boolean>();
		
		for (Map.Entry<DensityGrid, Boolean> grid : hashMap.entrySet())
		{
			DensityGrid dg = grid.getKey();
			Boolean inside = grid.getValue();
			
			this.grids.put(dg,  inside);
		}
		
		this.clusterLabel = label;
		
	}
	
	/**
	 * @param dg the density grid to add to the cluster
	 */
	public void addGrid(DensityGrid dg)
	{
		Boolean inside = isInside(dg);
		this.grids.put(dg, inside);
		
		for(Map.Entry<DensityGrid, Boolean> gridToUpdate : this.grids.entrySet())
		{
			Boolean inside2U = gridToUpdate.getValue();
			
			if(!inside2U)
			{
				DensityGrid dg2U = gridToUpdate.getKey();
				this.grids.put(dg2U, this.isInside(dg2U));
			}
		}
	}
	
	/**
	 * @param dg the density grid to remove from the cluster
	 */
	public void removeGrid(DensityGrid dg)
	{
		this.grids.remove(dg);
	}
	
	/**
	 * @param gridClus the GridCluster to be absorbed into this cluster
	 */
	public void absorbCluster(GridCluster gridClus)
	{
		DensityGrid dg;
		Boolean inside;
		Iterator<Map.Entry<DensityGrid, Boolean>> grid;
		HashMap<DensityGrid, Boolean> newCluster = new HashMap<DensityGrid, Boolean>();
		
		//System.out.println("Absorb cluster "+gridClus.getClusterLabel()+" into cluster "+this.getClusterLabel()+".");
		
		// Add each density grid from gridClus into this.grids
		grid = gridClus.getGrids().entrySet().iterator();
		while (grid.hasNext())
		{
			Map.Entry<DensityGrid, Boolean> entry = grid.next();
			dg = entry.getKey();
			this.grids.put(dg, false);
		}
		//System.out.println("...density grids added");
		
		// Determine which density grids in this.grids are 'inside' and which are 'outside'
		grid = this.getGrids().entrySet().iterator();
		while(grid.hasNext())
		{
			Map.Entry<DensityGrid, Boolean> entry = grid.next();
			dg = entry.getKey();
			inside = isInside(dg);
			newCluster.put(dg, inside);
		}
		this.grids = newCluster;
		//System.out.println("...inside/outside determined");

	}
	
	/**
	 * Inside Grids are defined in Definition 3.5 of Chen and Tu 2007 as:
	 * Consider a grid group G and a grid g ∈ G, suppose g =(j1, ··· ,jd), if g has 
	 * neighboring grids in every dimension i =1, ·· · ,d, then g is an inside grid 
	 * in G.Otherwise g is an outside grid in G.
	 * 
	 * @param dg the density grid to label as being inside or out
	 * @return TRUE if g is an inside grid, FALSE otherwise
	 */
	public Boolean isInside(DensityGrid dg)
	{
		Iterator<DensityGrid> dgNeighbourhood = dg.getNeighbours().iterator();
		
		while(dgNeighbourhood.hasNext())
		{
			DensityGrid dgprime = dgNeighbourhood.next();
			if(!this.grids.containsKey(dgprime))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Inside Grids are defined in Definition 3.5 of Chen and Tu 2007 as:
	 * Consider a grid group G and a grid g ∈ G, suppose g =(j1, ··· ,jd), if g has 
	 * neighboring grids in every dimension i =1, ·· · ,d, then g is an inside grid 
	 * in G. Otherwise g is an outside grid in G.
	 * 
	 * @param dg the density grid being labelled as inside or outside
	 * @param dgH the density grid being proposed for addition
	 * @return TRUE if g would be an inside grid, FALSE otherwise
	 */
	public Boolean isInside(DensityGrid dg, DensityGrid dgH)
	{
		Iterator<DensityGrid> dgNeighbourhood = dg.getNeighbours().iterator();
		
		while(dgNeighbourhood.hasNext())
		{
			DensityGrid dgprime = dgNeighbourhood.next();
			if(!this.grids.containsKey(dgprime) && !dgprime.equals(dgH))
			{
				return false;
			}
		}
		
		return true;
	}

	/**
	 * @return the class label assigned to the cluster
	 */
	public int getClusterLabel() {
		return clusterLabel;
	}

	public HashMap<DensityGrid,Boolean> getGrids()
	{
		return this.grids;
	}
	
	/**
	 * @param clusterLabel the class label to assign to the cluster
	 */
	public void setClusterLabel(int clusterLabel) {
		this.clusterLabel = clusterLabel;
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		sb.append("Cluster of grids.");
	}

	/**
	 * @return the number of density grids in the cluster
	 */
	@Override
	public double getWeight() {
		return this.grids.size();
	}
	
	/**
	 * Tests a grid cluster for connectedness according to Definition 3.4, Grid Group, from
	 * Chen and Tu 2007.
	 * 
	 * Selects one density grid in the grid cluster as a starting point and iterates repeatedly
	 * through its neighbours until no more density grids in the grid cluster can be visited.
	 *  
	 * @return TRUE if the cluster represent one single grid group; FALSE otherwise.
	 */
	public boolean isConnected()
	{
		this.visited = new HashMap<DensityGrid, Boolean>();
		Iterator<DensityGrid> initIter = this.grids.keySet().iterator();
		DensityGrid dg;
		
		if (initIter.hasNext())
		{
			dg = initIter.next();
			visited.put(dg, this.grids.get(dg));
			boolean changesMade;
			
			do{
				changesMade = false;
				
				Iterator<Map.Entry<DensityGrid, Boolean>> visIter = this.visited.entrySet().iterator();
				HashMap<DensityGrid, Boolean> toAdd = new HashMap<DensityGrid, Boolean>();
				
				while(visIter.hasNext() && toAdd.isEmpty())
				{
					Map.Entry<DensityGrid, Boolean> toVisit = visIter.next();
					DensityGrid dg2V = toVisit.getKey();
					
					Iterator<DensityGrid> dg2VNeighbourhood = dg2V.getNeighbours().iterator();
					
					while(dg2VNeighbourhood.hasNext())
					{
						DensityGrid dg2VN = dg2VNeighbourhood.next();
						
						if(this.grids.containsKey(dg2VN) && !this.visited.containsKey(dg2VN))
							toAdd.put(dg2VN, this.grids.get(dg2VN));
					}
					
				}
				
				if(!toAdd.isEmpty())
				{
					this.visited.putAll(toAdd);
					changesMade = true;
				}
			}while(changesMade);
			
		}		
		
		if (this.visited.size() == this.grids.size())
		{
			//System.out.println("The cluster is still connected. "+this.visited.size()+" of "+this.grids.size()+" reached.");
			return true;
		}
		else
		{
			//System.out.println("The cluster is no longer connected. "+this.visited.size()+" of "+this.grids.size()+" reached.");
			return false;
		}
	}

	/**
	 * Iterates through the DensityGrids in the cluster and calculates the inclusion probability for each.
	 * 
	 * @return 1.0 if instance matches any of the density grids; 0.0 otherwise.
	 */
	@Override
	public double getInclusionProbability(Instance instance) {
		Iterator<Map.Entry<DensityGrid, Boolean>> gridIter = grids.entrySet().iterator();
		
		while(gridIter.hasNext())
		{
			Map.Entry<DensityGrid, Boolean> grid = gridIter.next();
			DensityGrid dg = grid.getKey();
			if(dg.getInclusionProbability(instance) == 1.0)
				return 1.0;
		}
		
		return 0.0;
	}
	
	/**
	 * @return a String listing each coordinate of the density grid
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(10*this.grids.size());
		for(Map.Entry<DensityGrid, Boolean> grids : this.grids.entrySet())
		{
			DensityGrid dg = grids.getKey();
			Boolean inside = grids.getValue();
			
			sb.append("("+dg.toString());
			if (inside)
				sb.append(" In)");
			else
				sb.append(" Out)");
		}
		
		return sb.toString();
	}
	
}
