package inputstream;

import java.util.Random;

import core.Centroid;

public class RBFGenerator implements InputStreamInterface
{

	private int instanceRandomSeed;
	private int modelRandomSeed;
	private int numAttributes;
	private int numCentroids;
	private int numDriftCentroids;
	private double changeMagnitude;
	private Random instanceRNG;
	private Random modelRNG;

	private Centroid[] centroids;

	public RBFGenerator(int instanceRandomSeed, int modelRandomSeed, int numAttributes, int numCentroids, int numDriftCentroids)
	{
		this.instanceRandomSeed = instanceRandomSeed;
		this.modelRandomSeed = modelRandomSeed;
		this.numAttributes = numAttributes;
		this.numCentroids = numCentroids;
		this.numDriftCentroids = numDriftCentroids;
		this.changeMagnitude = 0.1;

		this.centroids = new Centroid[numCentroids];

		this.instanceRNG = new Random(instanceRandomSeed);
		this.modelRNG = new Random(modelRandomSeed);

		generateCentroids();
	}

	public RBFGenerator(int instanceRandomSeed, int modelRandomSeed, int numAttributes, int numCentroids, int numDriftCentroids, double changeMagnitude)
	{
		this.instanceRandomSeed = instanceRandomSeed;
		this.modelRandomSeed = modelRandomSeed;
		this.numAttributes = numAttributes;
		this.numCentroids = numCentroids;
		this.numDriftCentroids = numDriftCentroids;
		this.changeMagnitude = changeMagnitude;

		this.centroids = new Centroid[numCentroids];

		this.instanceRNG = new Random(instanceRandomSeed);
		this.modelRNG = new Random(modelRandomSeed);

		generateCentroids();
	}

	@Override
	public boolean hasNextTransaction()
	{
		return true;
	}

	@Override
	public String getNextTransaction()
	{
		String transaction = "";

		Centroid centroid = selectCentroidBasedOnWeight(centroids);

		for (int i = 0; i < numAttributes; i++)
		{
			double sigma = instanceRNG.nextDouble() >= 0.5 ? 1.0 : -1.0;
			double change = changeMagnitude * sigma * instanceRNG.nextGaussian() * centroid.getStdev();

			transaction += centroid.getCenterAtPos(i) + change + ",";
		}

		transaction += centroid.getClassLabel();

		return transaction;
	}

	public void induceDrift()
	{
		for (int i = 0; i < numDriftCentroids; i++)
		{
			for (int j = 0; j < numAttributes; j++)
			{
				double sigma = instanceRNG.nextDouble() >= 0.5 ? 1.0 : -1.0;
				double change = Math.sqrt(changeMagnitude * sigma * modelRNG.nextDouble());
				centroids[i].setCenterAtPos(j, centroids[i].getCenterAtPos(j) + change);
			}
		}
	}

	public void generateCentroids()
	{

		for (int i = 0; i < numCentroids; i++)
		{
			double[] randomCenters = new double[numAttributes];

			for (int j = 0; j < numAttributes; j++)
			{
				randomCenters[j] = modelRNG.nextDouble();
			}

			Centroid centroid = new Centroid();
			centroid.setCenters(randomCenters);
			centroid.setWeight(modelRNG.nextDouble());
			centroid.setStdev(modelRNG.nextDouble());
			centroid.setClassLabel(i);

			centroids[i] = centroid;
		}
	}

	public Centroid selectCentroidBasedOnWeight(Centroid[] Centroids)
	{
		double sumWeight = 0.0;
		for (Centroid cent : centroids)
		{
			sumWeight += cent.getWeight();
		}

		double prob = instanceRNG.nextDouble() * sumWeight;

		Centroid centroid = null;
		for (int i = 0; i < Centroids.length; i++)
		{
			prob -= Centroids[i].getWeight();
			if (prob <= 0.0)
			{
				centroid = Centroids[i];
				break;
			}
		}

		return centroid;
	}

}
