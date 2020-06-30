/*
 *    Dstream.java
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.cluster.CFCluster;
import moa.cluster.Clustering;
import moa.clusterers.AbstractClusterer;
import moa.core.Measurement;

/** Citation: Y. Chen and L. Tu, “Density-Based Clustering for Real-Time Stream Data,” in
 *  Proceedings of the 13th ACM SIGKDD international conference on Knowledge discovery and
 *  data mining, 2007, pp. 133–142.
 */
public class Dstream extends AbstractClusterer {

	private static final long serialVersionUID = 8759754409276716725L;
	private static final int NO_CLASS = -1;
	private static final int SPARSE = 0;
	private static final int TRANSITIONAL = 1;
	private static final int DENSE = 2;

	public FloatOption decayFactorOption = new FloatOption("decayFactor",
			'd', "The decay factor, lambda, in (0,1)", 0.998, 0.001, 0.999);

	public FloatOption cmOption = new FloatOption("Cm", 'm', "Controls the "
			+ "threshold for dense grids, > 1", 3.0, 1.001, Double.MAX_VALUE);

	public FloatOption clOption = new FloatOption("Cl", 'l', "Controls the "
			+ "threshold for sparse grids, in (0,1)", 0.8, 0.001, 0.999);

	public FloatOption betaOption = new FloatOption("Beta", 'b', "Adjusts the "
			+ "window of protection for renaming previously deleted grids as "
			+ "sporadic, > 0", 0.3, 0.001, Double.MAX_VALUE);

	/**
	 * The data stream's current internal time. Starts at 0.
	 */
	private int currTime; 
	
	/**
	 * User defined parameter: Time gap between calls to the offline component
	 */
	private int gap;
	
	/**
	 * User defined parameter, represented as lambda in Chen and Tu 2007
	 */
	private double decayFactor;
	
	/**
	 * User defined parameter: Controls the threshold for dense grids
	 */
	private double cm;
	
	/**
	 * User defined parameter: Controls the threshold for sparse grids
	 */
	private double cl;
	
	/**
	 * User defined parameter: Adjusts the window of protection for renaming 
	 * previously deleted grids as being sporadic
	 */
	private double beta;
	
	/**
	 * Density threshold for dense grids; controlled by cm; given in eq 8 of Chen and Tu 2007
	 * 
	 * @see #cm
	 */
	private double dm;
	
	/**
	 * Density threshold for sparse grids; controlled by cl; given in eq 9 of Chen and Tu 2007
	 * 
	 * @see #cl
	 */
	private double dl;
	
	/**
	 * The number of dimensions in the data stream; defined in section 3.1 of Chen and Tu 2007
	 */
	private int d;
	
	/**
	 * The number of density grids; defined after eq 2 in Chen and Tu 2007
	 */
	private int N;
	
	/**
	 * True if initialization of D-Stream is complete, false otherwise
	 */
	private boolean initialized;
	
	/**
	 * A list of all density grids which are being monitored;
	 * given in figure 1 of Chen and Tu 2007
	 */
	private HashMap<DensityGrid,CharacteristicVector> grid_list;
	
	/**
	 * A list of all density grids which have been deleted;
	 * allows the recording of tm - the last time when the 
	 * grid is removed from grid list as a sporadic grid (if ever).
	 */
	private HashMap<DensityGrid,Integer> deleted_grids;
	
	
	/**
	 * A list of all Grid Clusters, which are defined in 
	 * Definition 3.6 of Chen and Tu 2007
	 */
	private ArrayList<GridCluster> cluster_list;
	
	/**
	 * A list of grid clusters used when reclustering an existing cluster.
	 */
	private ArrayList<GridCluster> newClusterList;
	
	/**
	 * The minimum value seen for a numerical dimension; used to calculate N
	 * 
	 * @see #N
	 */
	private int[]minVals;
	
	/**
	 * The maximum value seen for a numerical dimension; used to calculate N
	 * 
	 * @see #N
	 */
	private int[]maxVals;

	/**
	 *  @see moa.clusterers.Clusterer#isRandomizable()
	 * D-Stream is not randomizable.
	 */
	@Override
	public boolean isRandomizable() {
		return false;
	}

	/**
	 * @see moa.clusterers.Clusterer#getVotesForInstance(com.yahoo.labs.samoa.instances.Instance)
	 * D-Stream does not vote on instances.
	 */
	@Override
	public double[] getVotesForInstance(Instance inst) {
		return null;
	}

	/**
	 *  @see moa.clusterers.Clusterer#getClusteringResult()
	 */
	@Override
	public Clustering getClusteringResult() {
		Clustering c = new Clustering();
		for(GridCluster gc : cluster_list)
		{
			c.add(gc);
		}
		return c;
	}

	/**
	 * @see moa.clusterers.AbstractClusterer#resetLearningImpl()
	 */
	@Override
	public void resetLearningImpl() {
		//System.out.println("Dstream . resetLearningImpl");
		this.setCurrTime(0);
		//System.out.println("Current time set...");
		
		this.decayFactor = decayFactorOption.getValue();
		this.cm = cmOption.getValue();
		this.cl = clOption.getValue();
		this.beta = betaOption.getValue();
		//System.out.println("Option values set...");

		this.initialized = false;
		this.grid_list = new HashMap<DensityGrid, CharacteristicVector>();
		this.deleted_grids = new HashMap<DensityGrid, Integer>();
		this.cluster_list = new ArrayList<GridCluster>();
		//System.out.println("Data structures initialized...");

		this.gap = 1;
		this.dm = -1.0;
		this.dl = -1.0;
		this.d = -1;
		this.N = -1;
		this.minVals = null;
		this.maxVals = null;
		//System.out.println("Dependent values initialized...\n");
		//printDStreamState();
	}

	/**
	 * @see moa.clusterers.AbstractClusterer#trainOnInstanceImpl(com.yahoo.labs.samoa.instances.Instance)
	 * 
	 * trainOnInstanceImpl implements the procedure given in Figure 1 of Chen and Tu 2007
	 */
	@Override
	public void trainOnInstanceImpl(Instance inst) {
		
		//System.out.print("Dstream.trainOnInstanceImpl (");
		int[]g;
		DensityGrid dg;
		CharacteristicVector cv;
		boolean recalculateN = false;	// flag indicating whether N needs to be recalculated after this instance

		// 1. Read record x = (x1,x2,...,xd)
		//printInst(inst);
		//System.out.println(") time "+this.getCurrTime());
		// Passed Instance inst
		if (!this.initialized)
		{
			//System.out.println("Not yet initialized");
			this.d = inst.numAttributes();
			//System.out.println("d = "+this.d);
			this.minVals = new int[this.d];
			this.maxVals = new int[this.d];
			//System.out.println("...data initialized");
			
			for(int i = 0 ; i < this.d ; i++)
			{
				//System.out.print(i+" ");
				if (inst.attribute(i).isNumeric())
				{
					maxVals[i] = (int) inst.value(i);
					minVals[i] = (int) inst.value(i);
				}
			}
			//System.out.println("...arrays initialized");
			recalculateN = true;
			this.initialized = true;
			//System.out.println("...boolean values initialized");
			//System.out.print("Grid list is: ");
			//printGridList();
		}		

		// 2. Determine the density grid g that contains x
		//System.out.print(" & Step 2 ");
		g = new int[this.d];

		for (int i = 0 ; i < this.d ; i++)
		{
			if (inst.attribute(i).isNumeric())
			{
				g[i] = (int) inst.value(i);
				if (g[i] > maxVals[i])
				{
					maxVals[i] = g[i];
					recalculateN = true;
				}
				else if (g[i] < minVals[i])
				{
					minVals[i] = g[i];
					recalculateN = true;
				}
			}
			else
			{
				g[i] = (int) inst.value(i);
			}
		}

		if (recalculateN)
		{
			//System.out.print(" recalculateN:");
			int n = 1;
			for (int i = 0 ; i < this.d ; i++)
			{
				//System.out.print(" "+n);
				if (inst.attribute(i).isNominal())
					n = n * inst.attribute(i).numValues();
				else
					n = n * (3+maxVals[i]-minVals[i]);
			}
			//System.out.print(" "+n);
			this.N = n;
			this.dl = this.cl/(this.N * (1.0 - this.decayFactor));
			this.dm = this.cm/(this.N * (1.0 - this.decayFactor));
			//System.out.print(" dl = " + this.dl + ", dm = " + this.dm);
			
			//Calculate the value for gap using the method defined in eq 26 of Chen and Tu 2007 
			double optionA = this.cl/this.cm;
			double optionB = ((double)this.N-this.cm)/((double)this.N-this.cl);
			gap = (int)Math.floor(Math.log(Math.max(optionA, optionB))/Math.log(this.getDecayFactor()));
			// Ensure that gap is not zero (i.e. if the procedure to calculate gap rounds down to zero, then set gap to 1 and adjust clustering every instance)
			if(gap == 0)
			{
				gap = 1;
			}
			//System.out.println(" A is "+optionA+", B is "+optionB+" and gap = "+gap);
		}

		dg = new DensityGrid(g);
		//System.out.println(dg.toString());
		
		// 3. If (g not in grid_list) insert dg to grid_list
		//System.out.println(" & Step 3 or 4");
		
		if(!this.grid_list.containsKey(dg))
		{
			//System.out.print("3 - dg wasn't in grid_list!");
			if(this.deleted_grids.containsKey(dg))
			{
				//System.out.print(" but it was in deleted_grids!");
				cv = new CharacteristicVector(this.getCurrTime(), this.deleted_grids.get(dg).intValue(), 1.0, -1, false, this.getDL(), this.getDM());
				this.deleted_grids.remove(dg);
			}
			else
				cv = new CharacteristicVector(this.getCurrTime(), -1, 1.0, -1, false, this.getDL(), this.getDM());
			
			this.grid_list.put(dg, cv);
			//System.out.print(" "+dg.toString()+" "+cv.toString());
			//System.out.println(" The size of grid_list is now "+grid_list.size());
		}
		// 4. Update the characteristic vector of dg
		else
		{
			//System.out.print("4 - dg was in grid_list!");
			cv = this.grid_list.get(dg);
				
			cv.densityWithNew(this.getCurrTime(), this.getDecayFactor());
				
			cv.setUpdateTime(this.getCurrTime());
		
			//System.out.println(" "+dg.toString()+" "+cv.toString());
		
			grid_list.put(dg, cv);
		}

		// 5. If tc == gap, then initial clustering
		// and
		// 6. If tc mod gap == 0, then:
		//    a. Detect and remove sporadic grids from grid_list
		//    b. Adjust clustering
		//System.out.println("\nCurrent Time is " + this.getCurrTime() + " and gap is " + this.gap);
		if (this.getCurrTime() != 0 && this.getCurrTime() % gap == 0)
		{
			if (this.getCurrTime() == gap)
			{
				//System.out.print(" & Step 5 x6x");
				this.initialClustering();
			}
			else
			{
				//System.out.print(" & Step x5x 6");
				this.removeSporadic();
				this.adjustClustering();
			}
		}

		// 7. Increment tc
		//System.out.println(" & Step 7");
		this.incCurrTime();

	}

	/**
	 * @see moa.clusterers.AbstractClusterer#getModelMeasurementsImpl()
	 */
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * @see moa.clusterers.AbstractClusterer#getModelDescription(java.lang.StringBuilder, int)
	 */
	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Implements the procedure given in Figure 3 of Chen and Tu 2007
	 */
	private void initialClustering() {
		//System.out.println("INITIAL CLUSTERING CALLED");
		//printDStreamState();
		// 1. Update the density of all grids in grid_list

		updateGridListDensity();
		//printGridList();
				
		// 2. Assign each dense grid to a distinct cluster
		// and
		// 3. Label all other grids as NO_CLASS	
		Iterator<Map.Entry<DensityGrid, CharacteristicVector>> glIter = this.grid_list.entrySet().iterator();
		HashMap<DensityGrid, CharacteristicVector> newGL = new HashMap<DensityGrid, CharacteristicVector>();
		
		while(glIter.hasNext())
		{
			Map.Entry<DensityGrid, CharacteristicVector> grid = glIter.next();
			DensityGrid dg = grid.getKey();
			CharacteristicVector cvOfG = grid.getValue();

			//System.out.print(dg.toString());
			if(cvOfG.getAttribute() == DENSE)
			{
				int gridClass = this.cluster_list.size();
				cvOfG.setLabel(gridClass);
				GridCluster gc = new GridCluster ((CFCluster)dg, new ArrayList<CFCluster>(), gridClass);
				gc.addGrid(dg);
				this.cluster_list.add(gc);
				//System.out.print(" was dense (class "+gridClass+")");
			}
			else
				cvOfG.setLabel(NO_CLASS);

			//System.out.println();
			newGL.put(dg, cvOfG);
		}

		this.grid_list = newGL;
		//printGridClusters();
		
		// 4. Make changes to grid labels by doing:
		//    a. For each cluster c
		//    b. For each outside grid g of c
		//    c. For each neighbouring grid h of g
		//    d. If h belongs to c', label c and c' with 
		//       the label of the largest cluster
		//    e. Else if h is transitional, assign it to c
		//    f. While changes can be made

		boolean changesMade;

		do{	
			changesMade = adjustLabels();
		}while(changesMade);	// while changes are being made
		
		//printGridList();
		//printGridClusters();
	}

	/**
	 * Makes first change available to it by following the steps:
	 * <ol type=a>
	 * <li>For each cluster c</li>
	 * <li>For each outside grid g of c</li>
	 * <li>For each neighbouring grid h of g</li>
	 * <li>If h belongs to c', label c and c' with the label of the largest cluster</li>
	 * <li>Else if h is transitional, assign it to c</li>
	 * </ol>
	 * 
	 * @return TRUE if a change was made to any cluster's labels, FALSE otherwise
	 */
	private boolean adjustLabels()
	{
		Iterator<GridCluster> gridClusIter = this.cluster_list.iterator();

		// a. For each cluster c
		while (gridClusIter.hasNext())
		{
			GridCluster c = gridClusIter.next();
			//System.out.print("Adjusting from cluster "+c.getClusterLabel()+", standby...");

			// b. for each grid, dg, of c
			for (Map.Entry<DensityGrid, Boolean> grid : c.getGrids().entrySet())
			{
				DensityGrid dg = grid.getKey();
				Boolean inside = grid.getValue();
				//System.out.print(" Inspecting density grid, dg:"+dg.toString()+", standby...");
				
				// b. for each OUTSIDE grid, dg, of c
				if (!inside)
				{
					//System.out.println(" Density grid dg is outside!");
					// c. for each neighbouring grid, dgprime, of dg
					Iterator<DensityGrid> dgNeighbourhood = dg.getNeighbours().iterator();
					
					while(dgNeighbourhood.hasNext())
					{
						DensityGrid dgprime = dgNeighbourhood.next();
						//System.out.print("Inspecting neighbour, dgprime:"+dgprime.toString()+", standby...");
						
						if(this.grid_list.containsKey(dgprime))
						{
							CharacteristicVector cv1 = this.grid_list.get(dg);
							CharacteristicVector cv2 = this.grid_list.get(dgprime);
							//System.out.print(" 1: "+cv1.toString()+", 2: "+cv2.toString());
							int class1 = cv1.getLabel();
							int class2 = cv2.getLabel();
							//System.out.println(" // classes "+class1+" and "+class2+".");

							// ...and if dgprime isn't already in the same cluster as dg...
							if (class1 != class2)
							{
								// If dgprime is in cluster c', merge c and c' into the larger of the two
								if (class2 != NO_CLASS)
								{
									//System.out.println("C is "+class1+" and C' is "+class2+".");
									if (this.cluster_list.get(class1).getWeight() < this.cluster_list.get(class2).getWeight())
										mergeClusters(class1, class2);
									else
										mergeClusters(class2, class1);

									return true;

								}
								// If dgprime is transitional and outside of c, assign it to c
								else if (cv2.isTransitional(dm, dl))
								{
									//System.out.println("h is transitional and is assigned to cluster "+class1);
									cv2.setLabel(class1);
									c.addGrid(dgprime);
									this.cluster_list.set(class1, c);
									this.grid_list.put(dg, cv2);
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Performs the periodic adjustment of clusters every 'gap' timesteps.
	 * Implements the procedure given in Figure 4 of Chen and Tu 2007
	 * 
	 * @see moa.clusterers.dstream.Dstream#gap
	 */
	private void adjustClustering() {
		//System.out.println("ADJUST CLUSTERING CALLED (time"+this.getCurrTime()+")");
		//printDStreamState();
		//printDStreamState();
		//printGridClusters();
		// 1. Update the density of all grids in grid_list

		updateGridListDensity();
		//printGridList();
		
		// 2. For each grid dg whose attribute is changed since last call
		//    a. If dg is sparse
		//    b. If dg is dense
		//    c. If dg is transitional
		boolean changesMade = false;
		
		do{
			changesMade=inspectChangedGrids();
		}while(changesMade);

		//printGridList();
		//System.out.print("Time: "+this.getCurrTime()+" and ");
		//printGridClusters();
		//System.out.println("Wait...");
		//try {
		//System.in.read();
		//} catch (IOException e) {
		//e.printStackTrace();
		//}
		 
	}

	/**
	 * Inspects each density grid in grid_list whose attribute has changed since the last 
	 * call to adjustClustering. Implements lines 3/4/7/19 of the procedure given in Figure 
	 * 4 of Chen and Tu 2007.
	 * 
	 * @return TRUE if any grids are updated; FALSE otherwise.
	 */
	private boolean inspectChangedGrids()
	{
		HashMap<DensityGrid, CharacteristicVector> glNew = new HashMap<DensityGrid, CharacteristicVector>();
		Iterator<Map.Entry<DensityGrid, CharacteristicVector>> gridIter = this.grid_list.entrySet().iterator();
		
		while (gridIter.hasNext() && glNew.isEmpty())
		{
			Map.Entry<DensityGrid, CharacteristicVector> grid = gridIter.next();
			DensityGrid dg = grid.getKey();
			CharacteristicVector cv = grid.getValue();
			int dgClass = cv.getLabel();
			
			if(cv.isAttChanged() && !dg.isVisited())
			{
				dg.setVisited(true);
				glNew.put(dg, cv);
				//System.out.print(dg.toString()+" is changed and now ");
				if (cv.getAttribute() == SPARSE)
					glNew.putAll(adjustForSparseGrid(dg, cv, dgClass));
				else if (cv.getAttribute() == DENSE)
					glNew.putAll(adjustForDenseGrid(dg, cv, dgClass));
				else	// TRANSITIONAL
					glNew.putAll(adjustForTransitionalGrid(dg, cv, dgClass));
			}
		}
		
		// If there are grids in glNew, update the corresponding grids in grid_list and clean up the cluster list
		if (!glNew.isEmpty())
		{
			//System.out.println("There are "+glNew.size()+" entries to update from glNew to grid_list.");
			this.grid_list.putAll(glNew);
			cleanClusters();
			return true;
		}
		else
			return false;
	}
	
	
	/**
	 * Adjusts the clustering of a sparse density grid. Implements lines 5 and 6 from Figure 4 of Chen and Tu 2007.
	 * 
	 * @param dg the sparse density grid being adjusted
	 * @param cv the characteristic vector of dg
	 * @param dgClass the cluster to which dg belonged
	 * 
	 * @return a HashMap<DensityGrid, CharacteristicVector> containing density grids for update after this iteration
	 */
	private HashMap<DensityGrid, CharacteristicVector> adjustForSparseGrid(DensityGrid dg, CharacteristicVector cv, int dgClass)
	{
		HashMap<DensityGrid, CharacteristicVector> glNew = new HashMap<DensityGrid, CharacteristicVector>();
		//System.out.print("Density grid "+dg.toString()+" is adjusted as a sparse grid at time "+this.getCurrTime()+". ");
		if (dgClass != NO_CLASS)
		{
			//System.out.println("It is removed from cluster "+dgClass+".");
			GridCluster gc = this.cluster_list.get(dgClass);
			gc.removeGrid(dg);
			cv.setLabel(NO_CLASS);
			glNew.put(dg, cv);
			this.cluster_list.set(dgClass, gc);
			
			if(gc.getWeight() > 0.0 && !gc.isConnected())
				glNew.putAll(recluster(gc));
		}
		//else
			//System.out.println("It was not clustered ("+dgClass+").");

		return glNew;
	}
	
	/**
	 * Reclusters a gridcluster into two (or more) constituent clusters when it has been identified that the original cluster
	 * is no longer a grid group. It does so by echoing the initial clustering procedure over only those grids in gc.
	 * 
	 * @param gc the gridcluster to be reclustered
	 * 
	 * @return a HashMap<DensityGrid, CharacteristicVector> containing density grids for update after this iteration
	 */
	private HashMap<DensityGrid, CharacteristicVector> recluster (GridCluster gc)
	{
		HashMap<DensityGrid, CharacteristicVector> glNew = new HashMap<DensityGrid, CharacteristicVector>();
		Iterator<Map.Entry<DensityGrid,Boolean>> gcIter = gc.getGrids().entrySet().iterator();
		newClusterList = new ArrayList<GridCluster>();
		//System.out.println("Recluster called for cluster "+gc.getClusterLabel());
		
		// Assign every dense grid in gc to its own cluster, assign all other grids to NO_CLASS
		while (gcIter.hasNext())
		{
			Map.Entry<DensityGrid,Boolean> grid = gcIter.next();
			DensityGrid dg = grid.getKey();
			CharacteristicVector cvOfG = this.grid_list.get(dg);

			if(cvOfG.getAttribute() == DENSE)
			{
				int gridClass = newClusterList.size();
				cvOfG.setLabel(gridClass);
				GridCluster newClus = new GridCluster ((CFCluster)dg, new ArrayList<CFCluster>(), gridClass);
				newClus.addGrid(dg);
				newClusterList.add(newClus);
			}
			else
				cvOfG.setLabel(NO_CLASS);

			glNew.put(dg, cvOfG);	
		}
		
		boolean changesMade;
		
		// While changes can be made...
		do
		{
			changesMade = false;
			HashMap<DensityGrid, CharacteristicVector> glAdjusted = adjustNewLabels(glNew);
			
			if(!glAdjusted.isEmpty())
			{
				glNew.putAll(glAdjusted);
				changesMade = true;
			}
		}while(changesMade);
		
		// Update the cluster list with the newly formed clusters
		gc.getGrids().clear();
		this.cluster_list.set(gc.getClusterLabel(), gc);
		this.cluster_list.addAll(newClusterList);
		
		return glNew;
	}
	
	
	private HashMap<DensityGrid, CharacteristicVector> adjustNewLabels(HashMap<DensityGrid, CharacteristicVector> glNew)
	{
		Iterator<GridCluster> newClusIter = newClusterList.iterator();
		HashMap<DensityGrid, CharacteristicVector> glAdjusted = new HashMap<DensityGrid, CharacteristicVector>();

		// a. For each cluster c
		while (newClusIter.hasNext())
		{
			GridCluster c = newClusIter.next();

			// b. for each grid, dg, of c
			for (Map.Entry<DensityGrid, Boolean> grid : c.getGrids().entrySet())
			{
				DensityGrid dg = grid.getKey();
				Boolean inside = grid.getValue();
				
				// b. for each OUTSIDE grid, dg, of c
				if (!inside)
				{
					// c. for each neighbouring grid, dgprime, of dg
					Iterator<DensityGrid> dgNeighbourhood = dg.getNeighbours().iterator();
					
					while(dgNeighbourhood.hasNext())
					{
						DensityGrid dgprime = dgNeighbourhood.next();
						
						if(glNew.containsKey(dgprime))
						{
							CharacteristicVector cv1 = glNew.get(dg);
							CharacteristicVector cv2 = glNew.get(dgprime);
							int class1 = cv1.getLabel();
							int class2 = cv2.getLabel();

							// ...and if dgprime isn't already in the same cluster as dg...
							if (class1 != class2)
							{
								GridCluster c1 = newClusterList.get(class1);
								// If dgprime is in cluster c', merge c and c' into the larger of the two
								if (class2 != NO_CLASS)
								{
									GridCluster c2 = newClusterList.get(class2);
									//System.out.println("C is "+class1+" and C' is "+class2+".");
									if (c1.getWeight() < c2.getWeight())
										glAdjusted.putAll(mergeNewClusters(glNew, class1, class2));
									else
										glAdjusted.putAll(mergeNewClusters(glNew, class2, class1));

									return glAdjusted;
								}
								// If dgprime is transitional and outside of c, assign it to c
								else if (cv2.isTransitional(dm, dl))
								{
									cv2.setLabel(class1);
									c1.addGrid(dgprime);
									this.newClusterList.set(class1, c1);
									glAdjusted.put(dgprime, cv2);
									return glAdjusted;
								}
							}
						}
					}
				}
			}
		}
		
		return glAdjusted;
	}
	
	private HashMap<DensityGrid, CharacteristicVector> mergeNewClusters(HashMap<DensityGrid, CharacteristicVector> glNew, int smallClus, int bigClus)
	{
		//System.out.println("Merge new clusters "+smallClus+" and "+bigClus+".");
		// Iterate through the density grids in grid_list to find those which are in highClass
		for (Map.Entry<DensityGrid, CharacteristicVector> grid : glNew.entrySet())
		{
			DensityGrid dg = grid.getKey();
			CharacteristicVector cv = grid.getValue();

			// Assign density grids in smallClus to bigClus
			if(cv.getLabel() == smallClus)
			{
				cv.setLabel(bigClus);
				glNew.put(dg, cv);
			}
		}
		//System.out.println("Density grids assigned to cluster "+bigClus+".");
		
		// Merge the GridCluster objects representing each cluster
		GridCluster bGC = this.newClusterList.get(bigClus);
		bGC.absorbCluster(this.newClusterList.get(smallClus));
		this.newClusterList.set(bigClus, bGC);
		this.newClusterList.remove(smallClus);
		//System.out.println("Cluster "+smallClus+" removed from list.");
		glNew = cleanNewClusters(glNew);
		
		return glNew;
	}
	
	/**
	 * Adjusts the clustering of a dense density grid. Implements lines 8 through 18 from Figure 4 of Chen and Tu 2007.
	 * 
	 * @param dg the dense density grid being adjusted
	 * @param cv the characteristic vector of dg
	 * @param dgClass the cluster to which dg belonged
	 * 
	 * @return a HashMap<DensityGrid, CharacteristicVector> containing density grids for update after this iteration
	 */
	private HashMap<DensityGrid, CharacteristicVector> adjustForDenseGrid(DensityGrid dg, CharacteristicVector cv, int dgClass)
	{
		//System.out.print("Density grid "+dg.toString()+" is adjusted as a dense grid at time "+this.getCurrTime()+". ");

		// Among all neighbours of dg, find the grid h whose cluster ch has the largest size
		GridCluster ch;								// The cluster, ch, of h
		DensityGrid hChosen = new DensityGrid(dg);	// The chosen grid h, whose cluster ch has the largest size
		double hChosenSize = -1.0;					// The size of ch, the largest cluster
		DensityGrid dgH;							// The neighbour of g being considered
		int hClass = NO_CLASS;						// The class label of h
		int hChosenClass = NO_CLASS;				// The class label of ch
		Iterator<DensityGrid> dgNeighbourhood = dg.getNeighbours().iterator();
		HashMap<DensityGrid, CharacteristicVector> glNew = new HashMap<DensityGrid, CharacteristicVector>();
		
		while (dgNeighbourhood.hasNext())
		{
			dgH = dgNeighbourhood.next();
		
			if (this.grid_list.containsKey(dgH))
			{
				hClass = this.grid_list.get(dgH).getLabel();
				if (hClass != NO_CLASS)
				{
					ch = this.cluster_list.get(hClass);
			
					if (ch.getWeight() > hChosenSize)
					{
						hChosenSize = ch.getWeight();
						hChosenClass = hClass;
						hChosen = new DensityGrid(dgH);
					}
				}
			}
		}
		
		//System.out.println(" Chosen neighbour is "+hChosen.toString()+" from cluster "+hChosenClass+".");
		
		if (hChosenClass != NO_CLASS  && hChosenClass != dgClass)
		{
			ch = this.cluster_list.get(hChosenClass);
			
			// If h is a dense grid
			if (this.grid_list.get(hChosen).getAttribute() == DENSE)
			{
				//System.out.println("h is dense.");
				// If dg is labelled as NO_CLASS
				if(dgClass == NO_CLASS)
				{
					//System.out.println("g was labelled NO_CLASS");
					cv.setLabel(hChosenClass);
					glNew.put(dg, cv);
					ch.addGrid(dg);
					this.cluster_list.set(hChosenClass, ch);
					
				}
				// Else if dg belongs to cluster c and h belongs to c'
				else
				{
					//System.out.println("g was labelled "+dgClass);
					double gSize = this.cluster_list.get(dgClass).getWeight();
					
					if (gSize <= hChosenSize)
						mergeClusters(dgClass, hChosenClass);
					else
						mergeClusters(hChosenClass, dgClass);
				}
			}
		
			// Else if h is a transitional grid
			else if (this.grid_list.get(hChosen).getAttribute() == TRANSITIONAL)
			{
				//System.out.print("h is transitional.");
				// If dg is labelled as no class and if h is an outside grid if dg is added to ch
				if (dgClass == NO_CLASS && !ch.isInside(hChosen, dg))
				{
					cv.setLabel(hChosenClass);
					glNew.put(dg, cv);
					ch.addGrid(dg);
					this.cluster_list.set(hChosenClass, ch);
					//System.out.println(" dg is added to cluster "+hChosenClass+".");
				}
				// Else if dg is in cluster c and |c| >= |ch|
				else if (dgClass != NO_CLASS)
				{
					GridCluster c = this.cluster_list.get(dgClass);
					double gSize = c.getWeight();
					
					if (gSize >= hChosenSize)
					{
						// Move h from cluster ch to cluster c
						ch.removeGrid(hChosen);
						c.addGrid(hChosen);
						CharacteristicVector cvhChosen = this.grid_list.get(hChosen);
						cvhChosen.setLabel(dgClass);
						glNew.put(hChosen, cvhChosen);
						//System.out.println("dgClass is "+dgClass+", hChosenClass is "+hChosenClass+", gSize is "+gSize+" and hChosenSize is "+hChosenSize+" h is added to cluster "+dgClass+".");
						this.cluster_list.set(hChosenClass, ch);
						this.cluster_list.set(dgClass, c);
					}
				}
			}
		}
		// If dgClass is dense and not in a cluster, and none if its neighbours are in a cluster,
		// put it in its own new cluster and search the neighbourhood for transitional or dense
		// grids to add
		else if (dgClass == NO_CLASS)
		{
			int newClass = this.cluster_list.size();
			GridCluster c = new GridCluster((CFCluster)dg, new ArrayList<CFCluster>(), newClass);
			c.addGrid(dg);
			//System.out.println("Added "+dg.toString()+" to cluster "+newClass+".");
			this.cluster_list.add(c);
			cv.setLabel(newClass);
			glNew.put(dg, cv);
			
			// Iterate through the neighbourhood until no more transitional neighbours can be added
			// (dense neighbours will add themselves as part of their adjust process)
			dgNeighbourhood = dg.getNeighbours().iterator();
					
			while(dgNeighbourhood.hasNext())
			{
				DensityGrid dghprime = dgNeighbourhood.next();
						
				if (this.grid_list.containsKey(dghprime) && !c.getGrids().containsKey(dghprime))
				{
					CharacteristicVector cvhprime = this.grid_list.get(dghprime);
					if(cvhprime.getAttribute() == TRANSITIONAL)
					{
						//System.out.println("Added "+dghprime.toString()+" to cluster "+newClass+".");
						c.addGrid(dghprime);
						cvhprime.setLabel(newClass);
						glNew.put(dghprime, cvhprime);
					}
				}
			}
	
			this.cluster_list.set(newClass, c);
			//System.out.println("Cluster "+newClass+": "+this.cluster_list.get(newClass).toString());		
		}
		
		return glNew;
	}
	
	/**
	 * Adjusts the clustering of a transitional density grid. Implements lines 20 and 21 from Figure 4 of Chen and Tu 2007.
	 * 
	 * @param dg the dense density grid being adjusted
	 * @param cv the characteristic vector of dg
	 * @param dgClass the cluster to which dg belonged
	 * 
	 * @return a HashMap<DensityGrid, CharacteristicVector> containing density grids for update after this iteration
	 */
	private HashMap<DensityGrid, CharacteristicVector> adjustForTransitionalGrid(DensityGrid dg, CharacteristicVector cv, int dgClass)
	{
		//System.out.print("Density grid "+dg.toString()+" is adjusted as a transitional grid at time "+this.getCurrTime()+". ");
		
		// Among all neighbours of dg, find the grid h whose cluster ch has the largest size
		// and satisfies that dg would be an outside grid if added to it
		GridCluster ch;								// The cluster, ch, of h
		double hChosenSize = 0.0;					// The size of ch, the largest cluster
		DensityGrid dgH;							// The neighbour of dg being considered
		int hClass = NO_CLASS;						// The class label of h
		int hChosenClass = NO_CLASS;				// The class label of ch
		Iterator<DensityGrid> dgNeighbourhood = dg.getNeighbours().iterator();
		HashMap<DensityGrid, CharacteristicVector> glNew = new HashMap<DensityGrid, CharacteristicVector>();
		
		while (dgNeighbourhood.hasNext())
		{
			dgH = dgNeighbourhood.next();
			
			if (this.grid_list.containsKey(dgH))
			{
				hClass = this.grid_list.get(dgH).getLabel();
				if (hClass != NO_CLASS)
				{
					ch = this.cluster_list.get(hClass);
			
					if ((ch.getWeight() > hChosenSize) && !ch.isInside(dg, dg))
					{
						hChosenSize = ch.getWeight();
						hChosenClass = hClass;
					}
				}
			}
		}
		
		//System.out.println(" Chosen neighbour is from cluster "+hChosenClass+", dgClass is "+dgClass+".");
		
		if (hChosenClass != NO_CLASS && hChosenClass != dgClass)
		{
			ch = this.cluster_list.get(hChosenClass);
			ch.addGrid(dg);
			this.cluster_list.set(hChosenClass, ch);
			
			if(dgClass != NO_CLASS)
			{
				GridCluster c = this.cluster_list.get(dgClass);
				c.removeGrid(dg);
				this.cluster_list.set(dgClass, c);
			}
			
			cv.setLabel(hChosenClass);
			glNew.put(dg, cv);
		}
		
		return glNew;
	}
	
	/**
	 * Iterates through cluster_list to ensure that all empty clusters have been removed and
	 * that all cluster IDs match the cluster's index in cluster_list.
	 */
	private void cleanClusters()
	{
		//System.out.println("Clean Clusters");
		Iterator<GridCluster> clusIter = this.cluster_list.iterator();
		ArrayList<GridCluster> toRem = new ArrayList<GridCluster>();

		// Check to see if there are any empty clusters
		while(clusIter.hasNext())
		{
			GridCluster c = clusIter.next();

			if(c.getWeight() == 0)
				toRem.add(c);
		}

		// Remove empty clusters
		if (!toRem.isEmpty())
		{
			clusIter = toRem.iterator();

			while(clusIter.hasNext())
			{
				this.cluster_list.remove(clusIter.next());
			}
		}

		// Adjust remaining clusters as necessary
		clusIter = this.cluster_list.iterator();

		while(clusIter.hasNext())
		{
			GridCluster c = clusIter.next();
			int index = this.cluster_list.indexOf(c);

			c.setClusterLabel(index);
			this.cluster_list.set(index, c);

			Iterator<Map.Entry<DensityGrid, Boolean>> gridsOfClus = c.getGrids().entrySet().iterator();

			while(gridsOfClus.hasNext())
			{
				DensityGrid dg = gridsOfClus.next().getKey();
				CharacteristicVector cv = this.grid_list.get(dg);
				if(cv == null)
				{
					System.out.println("Warning, cv is null for "+dg.toString()+" from cluster "+index+".");
					printGridList();
					printGridClusters();
				}
				//System.out.println("Cluster "+index+": "+dg.toString()+" is here.");
				cv.setLabel(index);
				this.grid_list.put(dg, cv);
			}
		}
	}
	
	private HashMap<DensityGrid, CharacteristicVector> cleanNewClusters(HashMap<DensityGrid, CharacteristicVector> glNew)
	{
		Iterator<GridCluster> clusIter = this.newClusterList.iterator();
		ArrayList<GridCluster> toRem = new ArrayList<GridCluster>();

		// Check to see if there are any empty clusters
		while(clusIter.hasNext())
		{
			GridCluster c = clusIter.next();

			if(c.getWeight() == 0)
				toRem.add(c);
		}

		// Remove empty clusters
		if (!toRem.isEmpty())
		{
			clusIter = toRem.iterator();

			while(clusIter.hasNext())
			{
				this.newClusterList.remove(clusIter.next());
			}
		}

		// Adjust remaining clusters as necessary
		clusIter = this.newClusterList.iterator();

		while(clusIter.hasNext())
		{
			GridCluster c = clusIter.next();
			int index = this.newClusterList.indexOf(c);

			c.setClusterLabel(index);

			Iterator<Map.Entry<DensityGrid, Boolean>> gridsOfClus = c.getGrids().entrySet().iterator();

			while(gridsOfClus.hasNext())
			{
				DensityGrid dg = gridsOfClus.next().getKey();
				CharacteristicVector cv = glNew.get(dg);
				cv.setLabel(index);
				glNew.put(dg, cv);
			}
		}
		
		return glNew;
	}
	
	/**
	 * Implements the procedure described in section 4.2 of Chen and Tu 2007
	 */
	private void removeSporadic() {
		//System.out.println("REMOVE SPORADIC CALLED");
		// 1. For each grid g in grid_list
		//    a. If g is sporadic
		//       i. If currTime - tg > gap, delete g from grid_list
		//       ii. Else if (S1 && S2), mark as sporadic
		//       iii. Else, mark as normal
		//    b. Else
		//       i. If (S1 && S2), mark as sporadic
		
		// For each grid g in grid_list
		Iterator<Map.Entry<DensityGrid, CharacteristicVector>> glIter = this.grid_list.entrySet().iterator();
		HashMap<DensityGrid, CharacteristicVector> newGL = new HashMap<DensityGrid, CharacteristicVector>();
		ArrayList<DensityGrid> remGL = new ArrayList<DensityGrid>();
				
		while(glIter.hasNext())
		{
			Map.Entry<DensityGrid, CharacteristicVector> grid = glIter.next();
			DensityGrid dg = grid.getKey();
			CharacteristicVector cv = grid.getValue();
			
			// If g is sporadic
			if (cv.isSporadic())
			{
				// If currTime - tg > gap, delete g from grid_list
				if ((this.getCurrTime() - cv.getUpdateTime()) >= gap)
				{
					int dgClass = cv.getLabel();
					
					if (dgClass != -1)
						this.cluster_list.get(dgClass).removeGrid(dg);
					
					remGL.add(dg);
					//System.out.println("Removed "+dg.toString()+" from cluster "+dgClass);
				}
				// Else if (S1 && S2), mark as sporadic - Else mark as normal
				else
				{
					cv.setSporadic(checkIfSporadic(cv));
					//System.out.println("within gap" + dg.toString() + " sporadicity assessed "+cv.isSporadic());
					newGL.put(dg, cv);
				}
				
			}
			// Else if (S1 && S2), mark as sporadic
			else
			{
				cv.setSporadic(checkIfSporadic(cv));
				//System.out.println(dg.toString() + " sporadicity assessed "+cv.isSporadic());
				newGL.put(dg, cv);
			}
		}
		
		this.grid_list.putAll(newGL);
		
		//System.out.println(" - Removed "+remGL.size()+" grids from grid_list.");
		Iterator<DensityGrid> remIter = remGL.iterator();
		
		while(remIter.hasNext())
		{
			DensityGrid sporadicDG = remIter.next();
			//System.out.println("Removing sporadic grid "+sporadicDG.toString()+" at time "+this.getCurrTime()+".");
			this.deleted_grids.put(sporadicDG, new Integer(this.getCurrTime()));
			this.grid_list.remove(sporadicDG);
		}
		
	}

	/**
	 * Determines whether a sparse density grid is sporadic using rules S1 and S2 of Chen and Tu 2007
	 * 
	 * @param cv - the CharacteristicVector of the density grid being assessed for sporadicity
	 */
	private boolean checkIfSporadic(CharacteristicVector cv)
	{
		// Check S1
		if(cv.getCurrGridDensity(this.getCurrTime(), this.getDecayFactor()) < densityThresholdFunction(cv.getDensityTimeStamp(), this.cl, this.getDecayFactor(), this.N))
		{
			// Check S2
			if(cv.getRemoveTime() == -1 || this.getCurrTime() >= ((1 + this.beta)*cv.getRemoveTime()))
				return true;
		}
		
		return false; 
	}
	
	/**
	 * Implements the function pi given in Definition 4.1 of Chen and Tu 2007
	 * 
	 * @param tg - the update time in the density grid's characteristic vector
	 * @param cl - user defined parameter which controls the threshold for sparse grids
	 * @param decayFactor - user defined parameter which is represented as lambda in Chen and Tu 2007
	 * @param N - the number of density grids, defined after eq 2 in Chen and Tu 2007
	 */
	private double densityThresholdFunction(int tg, double cl, double decayFactor, int N)
	{
		return (cl * (1.0 - Math.pow(decayFactor, (this.getCurrTime()-tg+1.0))))/(N * (1.0 - decayFactor));
	}
	
	/**
	 * Reassign all grids belonging in the small cluster to the big cluster
	 * Merge the GridCluster objects representing each cluster
	 * 
	 * @param smallClus - the index of the smaller cluster
	 * @param bigClus - the index of the bigger cluster
	 */
	private void mergeClusters (int smallClus, int bigClus)
	{		
		//System.out.println("Merge clusters "+smallClus+" and "+bigClus+".");
		// Iterate through the density grids in grid_list to find those which are in highClass
		for (Map.Entry<DensityGrid, CharacteristicVector> grid : grid_list.entrySet())
		{
			DensityGrid dg = grid.getKey();
			CharacteristicVector cv = grid.getValue();

			// Assign density grids in smallClus to bigClus
			if(cv.getLabel() == smallClus)
			{
				cv.setLabel(bigClus);
				this.grid_list.put(dg, cv);
			}
		}
		//System.out.println("Density grids assigned to cluster "+bigClus+".");
		
		// Merge the GridCluster objects representing each cluster
		GridCluster bGC = this.cluster_list.get(bigClus);
		bGC.absorbCluster(this.cluster_list.get(smallClus));
		this.cluster_list.set(bigClus, bGC);
		this.cluster_list.remove(smallClus);
		//System.out.println("Cluster "+smallClus+" removed from list.");
		cleanClusters();
	}

	/**
	 * Iterates through grid_list and updates the density for each density grid therein.
	 * Also marks each density grid as unvisited for this call to adjustClustering.
	 */
	private void updateGridListDensity()
	{
		for (Map.Entry<DensityGrid, CharacteristicVector> grid : grid_list.entrySet())
		{
			DensityGrid dg = grid.getKey();
			CharacteristicVector cvOfG = grid.getValue();

			dg.setVisited(false);
			cvOfG.updateGridDensity(this.getCurrTime(), this.getDecayFactor(), this.getDL(), this.getDM());

			this.grid_list.put(dg, cvOfG);
		}
	}

	/**
	 * @return currTime - the stream's internal time
	 */
	public int getCurrTime()
	{
		return this.currTime;
	}

	/**
	 * @param t - sets the stream's internal time to 't'
	 */
	private void setCurrTime(int t)
	{
		this.currTime = t;
	}

	/**
	 * Increments the stream's internal time
	 */
	private void incCurrTime()
	{
		this.currTime++;
	}

	/**
	 * @return decay factor - represented as lambda in Chen and Tu 2007
	 */
	public double getDecayFactor()
	{
		return this.decayFactor;
	}

	/**
	 * @return dm - the density threshold for dense grids. It is controlled by cl and given in eq 8 of Chen and Tu 2007
	 */
	public double getDM()
	{
		return this.dm;
	}

	/**
	 * @return dl - the density threshold for sparse grids. It is controlled by cl and given in eq 9 of Chen and Tu 2007
	 */
	public double getDL()
	{
		return this.dl;
	}
	
	public void printInst(Instance inst)
	{
		for (int i = 0 ; i < inst.numAttributes() ; i++)
			System.out.print(inst.value(i)+" ");
	}
	
	/**
	 * Prints out the values of the parameters associated with this instance of the D-Stream algorithm:
	 * <ol>
	 * <li>gap;</li>
	 * <li>decay factor (lambda);</li>
	 * <li>C_m and C_l;</li>
	 * <li>D_m and D_l; and</li>
	 * <li>beta.</li>
	 * </ol>
	 */
	public void printDStreamState()
	{
		System.out.println("State of D-Stream algorithm");
		System.out.println("Time Gap: "+this.gap+", Decay Factor: "+this.decayFactor);
		System.out.println("C_m: "+this.cm+", C_l: "+this.cl);
		System.out.println("D_m: "+this.dm+", D_l: "+this.dl);
		System.out.println("Beta: "+this.beta);
	}
	
	/**
	 * Iterates through grid_list and prints out each density grid therein as a string.
	 * 
	 * @see moa.clusterers.dstream.Dstream#grid_list
	 * @see moa.clusterers.dstream.DensityGrid#toString
	 */
	public void printGridList()
	{
		System.out.println("Grid List. Size "+this.grid_list.size()+".");
		for (Map.Entry<DensityGrid, CharacteristicVector> grid : grid_list.entrySet())
		{
			DensityGrid dg = grid.getKey();
			CharacteristicVector cv = grid.getValue();
			
			if (cv.getAttribute() != SPARSE)
			{
				double dtf = densityThresholdFunction(cv.getUpdateTime(), this.cl, this.getDecayFactor(), this.N);
				System.out.println(dg.toString()+" "+cv.toString()+" // Density Threshold Function = "+dtf);
			}
		}
	}
	
	/**
	 * Iterates through cluster_list and prints out each grid cluster therein as a string.
	 * 
	 * @see moa.clusterers.dstream.Dstream#cluster_list
	 * @see moa.clusterers.dstream.GridCluster#toString
	 */
	public void printGridClusters()
	{
		System.out.println("List of Clusters. Total "+this.cluster_list.size()+".");
		for(GridCluster gc : this.cluster_list)
		{
			System.out.println(gc.getClusterLabel()+": "+gc.getWeight()+" {"+gc.toString()+"}");
		}
	}

	
	public void adjustParameters(){
		this.decayFactor = decayFactorOption.getValue();
		this.cm = cmOption.getValue();
		this.cl = clOption.getValue();
		this.beta = betaOption.getValue();
	}
}
