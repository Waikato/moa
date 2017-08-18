package moa.clusterers.streamkm;

/**
 * 
 * CoresetCostTriple is a wrapper that allows the lloydPlusPlus method in StreamKM to return the coresetCentres,
 * radii of the associated clusters and the cost associated with the coreset.
 * 
 * @author Richard Hugh Moulton
 *
 */

public class CoresetCostTriple
{
	private Point[] coresetCentres;
	private double[] radii;
	private double coresetCost;
	
	public CoresetCostTriple(Point[] centres, double[] rad, double cost)
	{
		this.coresetCentres = new Point[centres.length];
				
		for (int i = 0 ; i < centres.length ; i++)
		{
			this.coresetCentres[i] = centres[i].clone();
		}
		
		this.radii = rad.clone();
		this.coresetCost = cost;
	}
	
	/**
	 * @return the coresetCentres
	 */
	public Point[] getCoresetCentres() {
		return coresetCentres;
	}
	/**
	 * @param coresetCentres the coresetCentres to set
	 */
	public void setCoresetCentres(Point[] coresetCentres) {
		this.coresetCentres = coresetCentres;
	}
	/**
	 * @return the coresetCost
	 */
	public double getCoresetCost() {
		return coresetCost;
	}
	/**
	 * @param coresetCost the coresetCost to set
	 */
	public void setCoresetCost(double coresetCost) {
		this.coresetCost = coresetCost;
	}

	/**
	 * @return the radii
	 */
	public double[] getRadii() {
		return radii;
	}

	/**
	 * @param radii the radii to set
	 */
	public void setRadii(double[] radii) {
		this.radii = radii;
	}
}
