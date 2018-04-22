/**
 *    CharacteristicVector.java
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

/**
 * The Characteristic Vector of a density grid is defined in 
 * Definition 3.2 of Chen and Tu 2007 as:
 * The characteristic vector of a grid g is a tuple (tg,tm,D, label,status), 
 * where tg is the last time when g is updated, tm is the last time when g 
 * is removed from grid list as a sporadic grid (if ever), D is the grid 
 * density at the last update, label is the class label of the grid, and 
 * status = {SPORADIC, NORMAL} is a label used for removing sporadic grids.
 * 
 * Citation: Y. Chen and L. Tu, “Density-Based Clustering for Real-Time Stream Data,” in
 * Proceedings of the 13th ACM SIGKDD international conference on Knowledge discovery and
 * data mining, 2007, pp. 133–142.
 */
public class CharacteristicVector {
	
	private static final int SPARSE = 0;
	private static final int TRANSITIONAL = 1;
	private static final int DENSE = 2;
	
	/**
	 * t_g in the characteristic vector tuple; 
	 * The last time when g is updated
	 */
	private int updateTime;
	
	/**
	 * t_m in the characteristic vector tuple;
	 * the last time when g is removed from grid_list as a sporadic
	 * grid (if ever).
	 */
	private int removeTime;
	
	/**
	 * D in the characteristic vector tuple; 
	 * the grid density at the last update
	 */
	private double gridDensity;
	
	/**
	 * label in the characteristic vector tuple; 
	 * the cluster label of the grid
	 */
	private int label;
	
	/**
	 * status in the characteristic vector tuple; 
	 * status = {SPORADIC, NORMAL}
	 */
	private boolean isSporadic;
	
	/**
	 * attribute mentioned in figure 4, line 3 of Chen and Tu 2007;
	 * attribute = {SPARSE, TRANSITIONAL, DENSE}
	 */
	private int attribute;
	
	/**
	 * time stamp at which the grid's density was last updated (including initial and adjust clusterings)
	 */
	private int densityTimeStamp;
	
	/**
	 * Flag marking whether there was a change in the attribute field
	 * the last time the grid density was updated.
	 */
	private boolean attChange;
	
	/**
	 * @category Constructor method for the Characteristic Vector of grid g
	 * @param tg - the last time when g is updated
	 * @param tm - the last time when g is removed from grid_list
	 * @param D - the grid density at the last update
	 * @param label - the class label of the grid
	 * @param status - SPORADIC (true) or NORMAL (false)
	 */
	public CharacteristicVector(int tg, int tm, double D, int label, boolean status, double dl, double dm)
	{
		this.setUpdateTime(tg);
		this.setRemoveTime(tm);
		this.setGridDensity(D, tg);
		this.setLabel(label);
		this.setSporadic(status);
		
		if (this.isSparse(dl))
			this.attribute = SPARSE;
		else if (this.isDense(dm))
			this.attribute = DENSE;
		else
			this.attribute = TRANSITIONAL;
		
		this.attChange = false;
	}

	/**
	 * @return the time at which the grid was last updated
	 */
	public int getUpdateTime() {
		return this.updateTime;
	}

	/**
	 * @param updateTime the time at which the grid was updated
	 */
	public void setUpdateTime(int updateTime) {
		this.updateTime = updateTime;
	}

	/**
	 * @return the last time at which the grid was removed from grid_list
	 */
	public int getRemoveTime() {
		return this.removeTime;
	}

	/**
	 * @param removeTime the time at which the grid was removed from grid_list
	 */
	public void setRemoveTime(int removeTime) {
		this.removeTime = removeTime;
	}

	/**
	 * @return the density of the grid
	 */
	public double getGridDensity() {
		return this.gridDensity;
	}
	
	/**
	 * @param currTime - the current time to calculate the density for
	 * @param decayFactor - the decay factor, lambda, of the algorithm
	 * 
	 * @return the density of the grid at the current time
	 */
	public double getCurrGridDensity(int currTime, double decayFactor)
	{
		return Math.pow(decayFactor, (currTime-this.getUpdateTime())) * this.getGridDensity();
	}

	/**
	 * @param gridDensity the density of the grid
	 * @param timeStamp the time at which the gridDensity is being updated
	 */
	public void setGridDensity(double gridDensity, int timeStamp) {
		this.gridDensity = gridDensity;
		this.densityTimeStamp = timeStamp;
	}

	/**
	 * @return the label of the cluster to which the grid is assigned
	 */
	public int getLabel() {
		return this.label;
	}

	/**
	 * @param newLabel the label of the cluster to assign the grid to
	 */
	public void setLabel(int newLabel) {
		this.label = newLabel;
	}

	/**
	 * @return TRUE if the characteristic vector is sporadic, FALSE otherwise
	 */
	public boolean isSporadic() {
		return this.isSporadic;
	}

	/**
	 * @param isSporadic TRUE if the characteristic vector is to be labelled as sporadic,
	 * FALSE otherwise
	 */
	public void setSporadic(boolean isSporadic) {
		this.isSporadic = isSporadic;
	}

	public int getDensityTimeStamp() {
		return densityTimeStamp;
	}

	public void setDensityTimeStamp(int densityTimeStamp) {
		this.densityTimeStamp = densityTimeStamp;
	}

	/**
	 * Implements the density update function given in 
	 * eq 5 (Proposition 3.1) of Chen and Tu 2007.
	 * 
	 * @param currTime the data stream's current internal time
	 * @param decayFactor the value of lambda
	 */
	public void densityWithNew(int currTime, double decayFactor)
	{
		// Update the density grid's density
		double densityOfG = this.getGridDensity();
		
		//System.out.print("["+decayFactor+"^("+currTime+" - "+this.getDensityTimeStamp()+") * "+densityOfG+"] + 1.0 = ");
		densityOfG = (Math.pow(decayFactor, (currTime-this.getUpdateTime())) * densityOfG)+1.0;
		//System.out.println(densityOfG);
		
		this.setGridDensity(densityOfG, currTime);
	}
	
	/**
	 * Implements the update the density of all grids step given at line 2 of 
	 * both Fig 3 and Fig 4 of Chen and Tu 2007.
	 * 
	 * @param currTime the data stream's current internal time
	 * @param decayFactor the value of lambda
	 * @param dl the threshold for sparse grids
	 * @param dm the threshold for dense grids
	 * @param addRecord TRUE if a record has been added to the density grid, FALSE otherwise
	 */
	public void updateGridDensity(int currTime, double decayFactor, double dl, double dm)
	{
		// record the last attribute
		int lastAtt = this.getAttribute();

		// Update the density grid's density
		double densityOfG = (Math.pow(decayFactor, (currTime-this.getDensityTimeStamp())) * this.getGridDensity());

		this.setGridDensity(densityOfG, currTime);

		// Evaluate whether or not the density grid is now SPARSE, DENSE or TRANSITIONAL
		if (this.isSparse(dl))
			this.attribute = SPARSE;
		else if (this.isDense(dm))
			this.attribute = DENSE;
		else
			this.attribute = TRANSITIONAL;

		// Evaluate whether or not the density grid attribute has changed and set the attChange flag accordingly
		if (this.getAttribute() == lastAtt)
			this.attChange = false;
		else
			this.attChange = true;
	}
	
	/**
	 * Implements the test for whether a density grid is dense given
	 * in eq 8 of Chen and Tu 2007.
	 * 
	 * @param dm the threshold for dense grids
	 */
	public boolean isDense(double dm)
	{
		if (this.gridDensity >= dm)
			return true;
		else
			return false;
	}
	
	/**
	 * Implements the test for whether a density grid is sparse given
	 * in eq 9 of Chen and Tu 2007.
	 * 
	 * @param dl the threshold for sparse grids
	 */
	public boolean isSparse(double dl)
	{
		if (this.gridDensity <= dl)
			return true;
		else
			return false;
	}
	
	/**
	 * Implements the test for whether a density grid is transitional
	 * given in eq 10 of Chen and Tu 2007.
	 * 
	 * @param dm the threshold for dense grids
	 * @param dl the threshold for sparse grids
	 */
	public boolean isTransitional(double dm, double dl)
	{
		if(this.isDense(dm) || this.isSparse(dl))
			return false;
		else
			return true;
	}

	/**
	 * @return the characteristic vector's attribute {SPARSE, TRANSITIONAL, DENSE}
	 */
	public int getAttribute() {
		return this.attribute;
	}

	/**
	 * @return true if the characteristic vector's attribute changed during the last
	 * density update, false otherwise.
	 */
	public boolean isAttChanged() {
		return this.attChange;
	}
	
	/**
	 * Overrides Object's toString method.
	 * 
	 * @return a String listing each value in the characteristic vector tuple
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(80);
		
		sb.append("CV / A (tg tm D class status) chgflag: ");
		
		if (this.getAttribute() == DENSE)
			sb.append("D ");
		else if (this.getAttribute() == SPARSE)
			sb.append("S ");
		else
			sb.append("T ");
		
		sb.append(this.getUpdateTime()+" ");
		sb.append(this.getRemoveTime()+" ");
		sb.append(this.getGridDensity()+" ");
		sb.append(this.getLabel()+" ");
		
		if (this.isSporadic())
			sb.append("Sporadic ");
		else
			sb.append("Normal ");
		
		if (this.isAttChanged())
			sb.append("CHANGED");
		
		return sb.toString();
	}
}
