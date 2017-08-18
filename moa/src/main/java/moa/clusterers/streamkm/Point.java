package moa.clusterers.streamkm;

import moa.cluster.Cluster;
import moa.cluster.SphereCluster;

import com.yahoo.labs.samoa.instances.Instance;

/**
 *
 * @author Marcel R. Ackermann, Christiane Lammersen, Marcus Maertens, Christoph Raupach, 
Christian Sohler, Kamil Swierkot
 */
public class Point {

	//dimension
	int dimension;

	//Clustering Features (weight, squareSum, linearSum)
	double weight;
	double squareSum;
	double[] coordinates;

	//cost and index of the centre, the point is currently assigned to
	double curCost;
	int centreIndex;
	
	//id and class (if there is class information in the file)
	int id;
	int cl;

	public Point(int dimension){
		this.weight = 1.0;
		this.squareSum = 0.0;
		this.dimension = dimension;
		this.coordinates = new double[dimension];
		this.id = -1;
		this.cl = -1;
		this.curCost = 0;
		this.centreIndex = -1;
		for(int l=0;l<dimension;l++){
			this.coordinates[l] = 0.0;
		}
	}
	
	public Point(Instance inst, int id){
		this.weight = inst.weight();
		this.squareSum = 0.0;
		this.dimension = inst.numAttributes();
		this.coordinates = new double[this.dimension];
		this.id = id;
		this.cl = 0;// NOT USED (int) inst.classValue();
		this.curCost = 0;
		this.centreIndex = -1;
		for(int l=0;l<this.dimension;l++){
			double nextNumber = inst.value(l) * inst.value(l);
			this.coordinates[l] = inst.value(l);
			this.squareSum += nextNumber*nextNumber;
		}
	}
		
	public Point clone(){
		Point res = new Point(this.dimension);
		res.weight = this.weight;
		res.squareSum = this.squareSum;
		res.dimension = this.dimension;
		res.coordinates = this.coordinates.clone();
		res.id = this.id;
		res.cl = this.cl;
		res.curCost = this.curCost;
		res.centreIndex = this.centreIndex;
		return res;
	}
	
	public Cluster toCluster(double radius){
		//Convert point to Cluster
		double[] centre = new double[this.dimension];
		
		for (int i = 0 ; i < this.dimension ; i++)
		{
			centre[i] = this.coordinates[i]/this.weight;
		}
		
		return (new SphereCluster(centre, radius, this.weight));
		
	}

	/**
	Computes the cost of this point with the given array of centres centres[] (of size k)
	**/
	public double costOfPoint(int k, Point[] centres){
		double nearestCost = -1.0;
		for(int j=0; j<k; j++){
			double distance = 0.0;
			int l = 0;
			for(l=0;l<this.dimension;l++){
				//Centroid coordinate of the point
				double centroidCoordinatePoint;
				if(this.weight != 0.0){
					centroidCoordinatePoint = this.coordinates[l] / this.weight;
				} else {
					centroidCoordinatePoint = this.coordinates[l];
				}
				//Centroid coordinate of the centre
				double centroidCoordinateCentre;
				if(centres[j].weight != 0.0){
					centroidCoordinateCentre = centres[j].coordinates[l] / centres[j].weight;
				} else {
					centroidCoordinateCentre = centres[j].coordinates[l];
				}
				distance += (centroidCoordinatePoint-centroidCoordinateCentre) * 
						(centroidCoordinatePoint-centroidCoordinateCentre) ;
			}
			
			if(nearestCost <0 || distance < nearestCost) {
				nearestCost = distance;
			} 
		}
		return this.weight * nearestCost;
	}


	/**
	Computes the index of the centre nearest to this point with the given array of centres centres[] (of size k)
	**/
	public int determineClusterCentreKMeans(int k, Point[] centres){
		int centre = 0;
		double nearestCost = -1.0;
		for(int j=0; j < k; j++){
			double distance = 0.0;
			for(int l=0; l<this.dimension;l++){
				//Centroid coordinate of the point
				double centroidCoordinatePoint;
				if(this.weight != 0.0){
					centroidCoordinatePoint = this.coordinates[l] / this.weight;
				} else {
					centroidCoordinatePoint = this.coordinates[l];
				}
				//Centroid coordinate of the centre
				double centroidCoordinateCentre;
				if(centres[j].weight != 0.0){
					centroidCoordinateCentre = centres[j].coordinates[l] / centres[j].weight;
				} else {
					centroidCoordinateCentre = centres[j].coordinates[l];
				}
				distance += (centroidCoordinatePoint-centroidCoordinateCentre) * 
						(centroidCoordinatePoint-centroidCoordinateCentre) ;
			}
			
			if(nearestCost <0 || distance < nearestCost) {
				nearestCost = distance;
				centre = j;
			} 
		}
		return centre;	
	}


	/**
	Computes the cost of this point with centre centre
	**/
	public double costOfPointToCenter(Point centre){
		if(this.weight == 0.0){
			return 0.0;
		}

		//stores the distance between p and centre
		double distance = 0.0;

		//loop counter
		for(int l=0; l<this.dimension; l++){
			//Centroid coordinate of the point
			double centroidCoordinatePoint;
			if(this.weight != 0.0){
				centroidCoordinatePoint = this.coordinates[l] / this.weight;
			} else {
				centroidCoordinatePoint = this.coordinates[l];
			}
			//Centroid coordinate of the centre
			double centroidCoordinateCentre;
			if(centre.weight != 0.0){
				centroidCoordinateCentre = centre.coordinates[l] / centre.weight;
			} else {
				centroidCoordinateCentre = centre.coordinates[l];
			}
			distance += (centroidCoordinatePoint-centroidCoordinateCentre) * 
					(centroidCoordinatePoint-centroidCoordinateCentre) ;
					
		}
		return distance * this.weight;
	}


}
