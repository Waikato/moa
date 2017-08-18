/**
 *    DensityGrid.java
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

import java.util.ArrayList;
import com.yahoo.labs.samoa.instances.Instance;
import moa.cluster.CFCluster;

/**
 * Density Grids are defined in equation 3 (section 3.1) of Chen and Tu 2007 as:
 * In D-Stream, we partition the d−dimensional space S into density grids. Suppose 
 * for each dimension, its space Si, i =1, ··· ,d is divided into pi partitions as
 * Si = Si,1 U Si,2 U ··· U Si,pi (2)
 * then the data space S is partitioned into N = PRODUCT (i=1..d) pi density grids. 
 * For a density grid g that is composed of S1,j1 ×S2,j2 ···×Sd,jd , ji =1, ...,pi,
 * we denote it as 
 * g =(j1,j2, ··· ,jd). (3)
 * 
 * Citation: Y. Chen and L. Tu, “Density-Based Clustering for Real-Time Stream Data,” in
 * Proceedings of the 13th ACM SIGKDD international conference on Knowledge discovery and
 * data mining, 2007, pp. 133–142.
 */
public class DensityGrid extends CFCluster
{
	private static final long serialVersionUID = 7527683701564950206L;
	
	/**
	 * For each dimension, its space Si, i =1, ··· ,d is divided into pi partitions as
	 * Si = Si,1 U Si,2 U ··· U Si,pi
	 * A density grid g that is composed of S1,j1 ×S2,j2 ···×Sd,jd , ji =1, ...,pi,
	 * has coordinates (j1,j2, ··· ,jd).
	 */
	private int[] coordinates;
	
	/**
	 * The value of 'd' for the d-dimensional space S considered by D-Stream.
	 */
	private int dimensions;
	
	/**
	 * Flag denoting whether this density grid has been inspected during the adjustClustering()
	 * step of D-Stream.
	 */
	private boolean isVisited;
	
	/**
	 * A constructor method for a density grid
	 * 
	 * @param c the coordinates of the density grid
	 */
	public DensityGrid(int[]c)
	{
		super(c.length);
		this.dimensions = c.length;
		this.coordinates = new int[this.dimensions];
		N = 1;
		LS = new double[this.dimensions];
		SS = new double[this.dimensions];
		
		for (int i = 0 ; i < this.dimensions ; i++)
		{
			int cI = c[i];
			this.coordinates[i] = cI;
			LS[i] += (double)cI;
			SS[i] += Math.pow((double)cI, 2);
		}
		
		this.isVisited = false;
	}

	/**
	 * A constructor method for a density grid
	 * 
	 * @param dg the density grid to copy
	 */
	public DensityGrid(DensityGrid dg)
	{
		super(dg.getDimensions());
		int[] dgCoord = dg.getCoordinates();
		this.dimensions = dg.getDimensions();
		this.coordinates = new int[this.dimensions];
		N = 1;
		LS = new double[this.dimensions];
		SS = new double[this.dimensions];
		
		for (int i = 0 ; i < this.dimensions ; i++)
		{
			int cI = dgCoord[i];
			this.coordinates[i] = cI;
			LS[i] += (double)cI;
			SS[i] += Math.pow((double)cI, 2);
		}
		
		this.isVisited = false;
	}
	
	/**
	 * Overrides Object's method equals to declare that two DensityGrids are equal iff their
	 * dimensions are the same and each of their corresponding coordinates are the same.
	 * 
	 * @param o the object being compared for equality
	 * @return  TRUE if the objects being compared are equal; FALSE otherwise
	 */
	@Override
	public boolean equals(Object o)
	{
		if(o == null)
			return false;
		if(o.getClass() != DensityGrid.class)
			return false;
		
		DensityGrid dg = (DensityGrid) o;
		
		if(dg.getDimensions() != dimensions)
			return false;
		
		int[] dgCoord = dg.getCoordinates();
		for(int i = 0 ; i < dimensions ; i++)
		{
			if(dgCoord[i] != coordinates[i])
				return false;
		}
		
		return true;		
	}
	
	/**
	 * Overrides Object's method hashCode to generate a hashCode for DensityGrids based on
	 * their coordinates.
	 *
	 * @return hc the Object's hash code
	 */
	@Override
	public int hashCode()
	{
		//int[] primes = {31, 37, 41, 43, 47, 53, 59};
		int hc = 1;
		
		for (int i = 0 ; i < this.dimensions ; i++)
		{
			hc = (hc * 31) + this.coordinates[i];
		}
		
		return hc;
	}
	
	/**
	 * Generates an Array List of neighbours for this density grid by varying each coordinate
	 * by one in either direction. Does not test whether the generated neighbours are valid as 
	 * DensityGrid is not aware of the number of partitions in each dimension.
	 * 
	 * @return an Array List of neighbours for this density grid
	 */
	public ArrayList<DensityGrid> getNeighbours()
	{
		ArrayList<DensityGrid> neighbours = new ArrayList<DensityGrid>();
		DensityGrid h;
		int[] hCoord = this.getCoordinates();
		
		for (int i = 0 ; i < this.dimensions ; i++)
		{
			hCoord[i] = hCoord[i]-1;
			h = new DensityGrid(hCoord);
			neighbours.add(h);
			
			hCoord[i] = hCoord[i]+2;
			h = new DensityGrid(hCoord);
			neighbours.add(h);
			
			hCoord[i] = hCoord[i]-1;
		}
		
		return neighbours;
	}
	
	/**
	 * @return coordinates the coordinates of the density grid
	 */
	public int[] getCoordinates() {
		return this.coordinates;
	}

	/**
	 * @return dimensions the number of dimensions for the density grid
	 */
	public int getDimensions() {
		return this.dimensions;
	}

	/**
	 * @return a String listing each coordinate of the density grid
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(15 + (2*this.dimensions));
		sb.append("DG:");
		for (int i = 0 ; i < this.dimensions ; i++)
		{
			sb.append(" "+this.coordinates[i]);
		}
		
		sb.append(" HC:"+this.hashCode());
		
		return sb.toString();
	}

	/**
	 * Returns a reference to the DensityGrid.
	 * 
	 * @return this density grid.
	 */
	@Override
	public CFCluster getCF() {
		return this;
	}

	/**
	 * Provides the probability of the argument instance belonging to the density grid in question.
	 * 
	 * @return 1.0 if the instance equals the density grid's coordinates; 0.0 otherwise.
	 */
	@Override
	public double getInclusionProbability(Instance instance) {
		for (int i = 0 ; i < this.dimensions ; i++)
		{
			if ((int) instance.value(i) != this.coordinates[i])
				return 0.0;
		}
		
		return 1.0;
	}

	/**
	 * Provides the radius of a density grid.
	 * 
	 * @return	1.0, approximating a unit hypercube of dimension d.
	 */
	@Override
	public double getRadius() {
		return 1.0;
	}

	/**
	 * @return the isVisited
	 */
	public boolean isVisited() {
		return isVisited;
	}

	/**
	 * @param isVisited the isVisited to set
	 */
	public void setVisited(boolean isVisited) {
		this.isVisited = isVisited;
	}
}
