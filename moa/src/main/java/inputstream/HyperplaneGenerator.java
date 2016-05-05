package inputstream;

import java.util.Random;

public class HyperplaneGenerator implements InputStreamInterface
{
    private int instanceRandomSeed;
    private int numAttributes;
    private int numDriftAttributes;
    private double changeMagnitude;
    private double sigmaPercentage;

    private Random rng;
    private double[] weights;
    private double[] sigmas;

    public HyperplaneGenerator(int instanceRandomSeed, int numAttributes,
	    int numDriftAttributes, double changeMagnitude,
	    double sigmaPercentage)
    {
	this.instanceRandomSeed = instanceRandomSeed;
	this.numAttributes = numAttributes;
	this.numDriftAttributes = numDriftAttributes;
	this.changeMagnitude = changeMagnitude;
	this.sigmaPercentage = sigmaPercentage;

	this.rng = new Random(instanceRandomSeed);

	this.weights = new double[numAttributes];
	this.weights = new double[]{0.3679,0.3679,0.1839,0.0613,0.0153,0.0031,0.0005,0.0001}; //poisson lambda=1
//	this.weights = new double[]{0.0,0.0,0.0,0.0,0.0,0.0004,0.0089,0.0898,0.2852,0.1216}; //binomial n=20 p=0.9
//	this.weights = new double[]{0.0008,0.0278,0.1304,0.1916,0.1144,0.0308,0.0039,0.0002,0.0,0.0}; //binomial n=20 p=0.3
//	this.weights = new double[]{1.0/1,1.0/2,1.0/3,1.0/4,1.0/5,1.0/6,1.0/7,1.0/8,1.0/9,1.0/10}; //exponential
//	this.weights = new double[]{1.0/10,1.0/10,1.0/10,1.0/10,1.0/10,1.0/10,1.0/10,1.0/10,1.0/10,1.0/10}; //uniform
	this.sigmas = new double[numAttributes];
	for (int i = 0; i < numAttributes; i++)
	{
//	    weights[i] = rng.nextDouble();
	    sigmas[i] = (rng.nextDouble() > 0.5) ? 1.0 : -1.0;
	}
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
	double weightSum = 0.0;
	double sum = 0.0;

	double[] attributes = new double[numAttributes];
	for (int i = 0; i < numAttributes; i++)
	{
	    attributes[i] = rng.nextDouble();
	    transaction += String.format("%.4f", attributes[i]) + ",";

	    weightSum += weights[i];
	    sum += attributes[i] * weights[i];
	}

	int label = (sum > weightSum / 2) ? 1 : 0;

	transaction += label;
//	induceDrift();
	return transaction;
    }

    private Random rngDrift = new Random(3);

    public void induceDrift()
    {
	for (int i = 0; i < numDriftAttributes; i++)
	{
	    // System.out.println(weights[i]);
	    weights[i] += sigmas[i] * changeMagnitude;
	    // System.out.println(weights[i]);

	    if (rngDrift.nextDouble() <= sigmaPercentage)
	    {
		sigmas[i] *= -1.0;
	    }
	}

    }

}
