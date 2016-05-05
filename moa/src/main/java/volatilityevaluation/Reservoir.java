package volatilityevaluation;

import java.util.Random;

public class Reservoir
{
    private int size;
    public double[] elements;
    private double elementTotal;
    private int eIndex;
    private Random rand;

    public Reservoir(int size)
    {
	this.size = size;
	this.elements = new double[size];
	elementTotal = 0;
	this.eIndex = 0;
	rand = new Random(50);
    }

    public void addElement(double inputValue)
    {
	if (eIndex < size)
	{
	    elements[eIndex] = inputValue;
	    elementTotal += inputValue;
	    eIndex++;
	} 
	else
	{
	    int removeIndex = (int) (rand.nextDouble() * eIndex);
	    elementTotal -= elements[removeIndex];
	    elements[removeIndex] = inputValue;
	    elementTotal += inputValue;
	}
    }

    public double getReservoirMean()
    {
	return elementTotal / eIndex;
    }

    public double getReservoirStdev()
    {
	double stdev = calculateStdev(elements, getReservoirMean());
	return stdev == 0 ? 0.00000000001 : stdev;
    }

    public int getCount()
    {
	return eIndex;
    }

    public boolean isFull()
    {
	if (eIndex == size)
	{
	    return true;
	} else
	{
	    return false;
	}
    }

    public double calculateStdev(double[] times, double mean)
    {
	double sum = 0;
	int count = 0;
	for (double d : times)
	{
	    if (d > 0)
	    {
		count++;
		sum += Math.pow(d - mean, 2);
	    }
	}
	return Math.sqrt(sum / count);
    }

    public double[] getReservoir()
    {
	return elements;
    }
    
    public void clear()
    {
	this.elements = new double[size];
	elementTotal = 0;
	this.eIndex = 0;
    }
}
