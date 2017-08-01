package moa.clusterers.streamkm;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import moa.cluster.Clustering;
import moa.clusterers.AbstractClusterer;
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;

/**
 *
 * @author Marcel R. Ackermann, Christiane Lammersen, Marcus Maertens, Christoph Raupach, 
Christian Sohler, Kamil Swierkot

Modified by Richard Hugh Moulton (24 Jul 2017)

Citation: Marcel R. Ackermann, Christiane Lammersen, Marcus Märtens,
Christoph Raupach, Christian Sohler, Kamil Swierkot: StreamKM++: A
Clustering Algorithms for Data Streams. ALENEX 2010: 173-187
 */

public class StreamKM extends AbstractClusterer {

	public IntOption sizeCoresetOption = new IntOption("sizeCoreset",
			's', "Size of the coreset (m).", 10000);

	public IntOption numClustersOption = new IntOption(
			"numClusters", 'k',
			"Number of clusters to compute.", 5);

	public IntOption lengthOption = new IntOption("length",
			'l', "Length of the data stream (n).", 100000, 0, Integer.MAX_VALUE);

	public FlagOption evaluateOption = new FlagOption("evaluateFinalOnly",
			'e', "If true, only the final clustering is evaluated.");

	public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
			"Seed for random behaviour of the classifier.", 1);	

	protected MTRandom clustererRandom;
	protected Point[] centresStreamingCoreset;

	protected int numberInstances;

	protected int dimension;	
	protected int length;
	protected int numberOfCentres;
	protected int coresetsize;	

	protected BucketManager manager;

	protected boolean initialized = false;	

	private final static double THRESHOLD = 1.000;

	@Override
	public void resetLearningImpl() {
		this.initialized = false;
		this.coresetsize = sizeCoresetOption.getValue();
		this.numberOfCentres = numClustersOption.getValue();
		this.length = lengthOption.getValue();
		this.centresStreamingCoreset = new Point[this.numberOfCentres];

		//initalize random generator with seed
		this.clustererRandom = new MTRandom(this.randomSeedOption.getValue());
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {

		if (this.initialized == false) {
			this.dimension =  inst.numAttributes();
			manager = new BucketManager(this.length, this.dimension, this.coresetsize, this.clustererRandom);
			this.initialized = true;
		}

		manager.insertPoint(new Point(inst, this.numberInstances));     

		this.numberInstances++;
		if (this.numberInstances % lengthOption.getValue() == 0) {

			Point[] streamingCoreset = manager.getCoresetFromManager(dimension);

			//compute 5 clusterings of the coreset with kMeans++ and take the best
			CoresetCostTriple triple;
			double minCost = 0.0;
			double curCost = 0.0;

			triple = lloydPlusPlus(numberOfCentres, coresetsize, dimension, streamingCoreset);
			minCost = triple.getCoresetCost();
			for (int j = 0 ; j < this.numberOfCentres ; j++)
			{
				centresStreamingCoreset[j] = triple.getCoresetCentres()[j].clone();
			}
			curCost = minCost;

			for(int i = 1; i < 5; i++){
				triple = lloydPlusPlus(numberOfCentres, coresetsize, dimension, streamingCoreset);
				curCost = triple.getCoresetCost();

				if(curCost < minCost) {
					minCost = curCost;
					for (int j = 0 ; j < this.numberOfCentres ; j++)
					{
						centresStreamingCoreset[j] = triple.getCoresetCentres()[j].clone();
					}
				}
			}
		}
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isRandomizable() {
		return true;
	}

	public double[] getVotesForInstance(Instance inst) {
		throw new UnsupportedOperationException("Not supported yet.");
	}


	@Override
	public Clustering getClusteringResult() {
		if ( !this.initialized ) {
			return new Clustering();
		}

		Clustering clustering = new Clustering();

		if (!evaluateOption.isSet())
		{
			Point[] streamingCoreset = manager.getCoresetFromManager(dimension);

			//compute 5 clusterings of the coreset with kMeans++ and take the best
			CoresetCostTriple triple;
			double minCost = 0.0;
			double curCost = 0.0;

			triple = lloydPlusPlus(numberOfCentres, coresetsize, dimension, streamingCoreset);
			minCost = triple.getCoresetCost();
			for (int j = 0 ; j < this.numberOfCentres ; j++)
			{
				centresStreamingCoreset[j] = triple.getCoresetCentres()[j].clone();
			}
			curCost = minCost;

			for(int i = 1; i < 5; i++){
				triple = lloydPlusPlus(numberOfCentres, coresetsize, dimension, streamingCoreset);
				curCost = triple.getCoresetCost();

				if(curCost < minCost) {
					minCost = curCost;
					for (int j = 0 ; j < this.numberOfCentres ; j++)
					{
						centresStreamingCoreset[j] = triple.getCoresetCentres()[j].clone();
					}
				}
			}

			for ( int i = 0; i < centresStreamingCoreset.length; i++ ) {
				if(centresStreamingCoreset[i] != null){
					clustering.add(centresStreamingCoreset[i].toCluster(triple.getRadii()[i]));
				}
			}
		}

		return clustering;
	}


	public CoresetCostTriple lloydPlusPlus(int k, int n, int d, Point points[]){
		//printf("starting kMeans++\n");
		CoresetCostTriple triple;
		double[] radii = new double[k];

		//choose random centres
		Point[] centres = chooseRandomCentres(k, n, d, points);
		double cost = targetFunctionValue(k, n, centres, points);
		double newCost = cost;


		Point[] massCentres = new Point[k];
		double[] numberOfPoints = new double[k];

		do{
			cost = newCost;
			//reset centres of mass
			int i = 0;
			for(i = 0; i < k; i++){	
				massCentres[i] = new Point(d);
				numberOfPoints[i] = 0.0;
			}
			//compute centres of mass
			for(i = 0; i < n; i++){
				int centre = points[i].determineClusterCentreKMeans(k,centres);
				for(int l = 0; l < massCentres[centre].dimension; l++){
					if(points[i].weight != 0.0)
						massCentres[centre].coordinates[l] += points[i].coordinates[l];
				}
				numberOfPoints[centre] += points[i].weight;

			}

			//move centres
			for(i=0; i<k; i++){
				for(int l=0; l<centres[i].dimension; l++){
					centres[i].coordinates[l] = massCentres[i].coordinates[l];
				}
				centres[i].weight = numberOfPoints[i];
			}

			//calculate costs
			newCost = targetFunctionValue(k, n, centres, points);
			//printf("old cost:%f, new cost:%f \n",cost,newCost);
		} while (newCost < THRESHOLD * cost);

		//compute radii
		for (int i = 0 ; i < n ; i++)
		{
			int centre = points[i].determineClusterCentreKMeans(k,centres);
			double radius = 0.0;
			double distance;

			for(int j = 0 ; j < points[i].dimension ; j++)
			{
				distance = Math.abs((centres[centre].coordinates[j]/centres[centre].weight) - (points[i].coordinates[j]/points[i].weight));
				radius += Math.pow(distance, 2.0);
			}

			radii[centre] += radius*points[i].weight;	
		}
		for (int i = 0 ; i < k ; i++)
		{
			radii[i] = 2.0 * Math.sqrt(radii[i]/centres[i].weight);
		}


		/*printf("Centres: \n");
		int i=0;
		for(i=0;i<k;i++){
			printf("(");
			int l = 0;
			for(l=0;l<centres[i].dimension;l++){
				printf("%f,",centres[i].coordinates[l] / centres[i].weight);
			}
			printf(")\n");
		}
		printf("kMeans++ finished\n");
		 */ 
		triple = new CoresetCostTriple(centres, radii, newCost);

		return triple; 
	}

	private Point[] chooseRandomCentres(int k, int n, int d, Point points[]){

		//array to store the choosen centres
		Point[] centres = new Point[k]; 

		//choose the first centre (each point has the same probability of being choosen)
		int i = 0;

		int next = 0;
		int j = 0;
		do{ //only choose from the n-i points not already choosen
			next = this.clustererRandom.nextInt(n-1); 

			//check if the choosen point is not a dummy
		} while( points[next].weight < 1);

		//set j to next unchoosen point
		j = next;
		//copy the choosen point to the array
		centres[i] = points[j].clone();

		//set the current centre for all points to the choosen centre
		for(i = 0; i < n; i++){
			points[i].centreIndex = 0;
			points[i].curCost = points[i].costOfPointToCenter(centres[0]);

		}
		//choose centre 1 to k-1 with the kMeans++ distribution
		for(i = 1; i < k; i++){

			double cost = 0.0;
			for(j = 0; j < n; j++){
				cost += points[j].curCost;
			}

			double random = 0;
			double sum = 0.0;
			int pos = -1;

			do{
				random = this.clustererRandom.nextDouble();//genrand_real3();
				sum = 0.0;
				pos = -1;

				for(j = 0; j < n; j++){
					sum = sum + points[j].curCost;
					if(random <= sum/cost){
						pos = j;
						break;
					}	
				}	
			} while (points[pos].weight < 1);

			//copy the choosen centre
			centres[i] = points[pos].clone();
			//check which points are closest to the new centre
			for(j = 0; j < n; j++){
				double newCost = points[j].costOfPointToCenter(centres[i]);
				if(points[j].curCost > newCost){
					points[j].curCost = newCost;
					points[j].centreIndex = i;
				}
			}

		}

		/*printf("random centres: \n");
		for(i = 0; i < k; i++){
			//printf("%d: (",i);
			int l = 0;
			for(l = 0; l < centres[i].dimension; l++){
				printf("%f,",centres[i].coordinates[l] / centres[i].weight);
			}
			printf(")\n");
		}*/

		return centres;
	}

	/**
	computes the target function for the given pointarray points[] (of size n) with the given array of
	centres centres[] (of size k)
	 **/
	public double targetFunctionValue(int k, int n, Point[] centres, Point[] points){
		int i=0;
		double sum = 0.0;
		for(i=0;i<n;i++){
			double nearestCost = -1.0;
			int j=0;
			for(j=0;j<k;j++){
				double distance = 0.0;
				int l = 0;
				for(l=0;l<points[i].dimension;l++){
					//Centroid coordinate of the point
					double centroidCoordinatePoint;
					if(points[i].weight != 0.0){
						centroidCoordinatePoint = points[i].coordinates[l] / points[i].weight;
					} else {
						centroidCoordinatePoint = points[i].coordinates[l];
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
			sum += nearestCost * points[i].weight;
		}
		return sum;
	}

}
