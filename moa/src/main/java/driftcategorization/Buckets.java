package driftcategorization;

import java.util.ArrayList;

public class Buckets
{
    private ArrayList<Bucket> bucketList;
    private double rangePerBucket;
    private int size;

    public Buckets(int size)
    {
	if (size % 2 != 0)
	{
	    System.err.println("Bucket size must be even");
	}

	this.rangePerBucket = 2.0 / size;
	this.size = size;
	this.bucketList = new ArrayList<Bucket>();

	for (int i = 0; i < size; i++)
	{
	    Bucket bucket = new Bucket();
	    bucketList.add(bucket);
	}
    }

    public void addElement(double element)
    {
	int pos = (int) (element / rangePerBucket);
	if (pos >= 0)
	{
	    pos = ((size / 2) - 1) - pos;

	    if (pos < 0)
	    {
		pos = 0;
	    }
	} else
	{
	    pos = Math.abs(pos) + (size / 2);

	    if (pos > size - 1)
	    {
		pos = size - 1;
	    }
	}

	bucketList.get(pos).addElement(element);
    }

    public double calculateWeightedAverage()
    {
	double sum = 0.0;
	int totalCount = 1;

	for (Bucket b : bucketList)
	{
	    sum += b.getTotal();
	    totalCount += b.getCount();
	}

	return sum / totalCount;
    }

    public void resetBuckets()
    {
	this.bucketList.clear();
	this.bucketList = new ArrayList<Bucket>();

	for (int i = 0; i < size; i++)
	{
	    Bucket bucket = new Bucket();
	    bucketList.add(bucket);
	}
    }

    private class Bucket
    {
	private int count;
	private double total;

	public Bucket()
	{
	    this.count = 0;
	    this.total = 0.0;
	}

	public void addElement(double element)
	{
	    this.count++;
	    this.total += element;
	}

	public void adjustCount(int amount)
	{
	    this.count += amount;
	}

	public int getCount()
	{
	    return this.count;
	}

	public void adjustTotal(int amount)
	{
	    this.total += amount;
	}

	public double getTotal()
	{
	    return this.total;
	}

	public double getAverage()
	{
	    return total / count;
	}

	public void reset()
	{
	    this.count = 0;
	    this.total = 0;
	}
    }

}
