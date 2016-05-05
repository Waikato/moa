package cutpointdetection;

import java.util.Random;

public class EnsembleDetector implements CutPointDetector
{
    private int capacity;
//    private int count;
    private CutPointDetector[] ensemble;
    private int[] driftSummary;
    
    private int waittime;
    private double threshold;
    
//    private Random rng = new Random(1);
    
    private int c = 0;
    
//    public EnsembleDetector(int capacity, double threshold)
//    {
//	this.capacity = capacity;
//	this.count = 0;
//	this.ensemble = new CutPointDetector[capacity];
//	this.driftSummary = new int[capacity];
//	this.waittime = 100;
//	this.threshold = threshold;
//    }
    
    public EnsembleDetector(CutPointDetector[] ensemble, double threshold)
    {
	this.capacity = ensemble.length;
//	this.count = 0;
//	for(CutPointDetector det : ensemble)
//	{
//	    if(det != null){ this.count++; }
//	}
	this.ensemble = ensemble;
	this.driftSummary = new int[capacity];
	this.waittime = 100;
	this.threshold = threshold;
    }

    @Override
    public boolean setInput(double d)
    {
	c++;
	// Feeding instances into the individual detectors in the ensemble
	for(int i = 0; i < capacity; i++)
	{
	    if(ensemble[i].setInput(d))
	    {
		driftSummary[i] = waittime;
	    }
	    else
	    {
		driftSummary[i]--;
	    }
	}
	
	// Calculating whether ensemble signals a drift
	double k = 0;
	for(int j = 0; j < capacity; j++)
	{
	    if(driftSummary[j] > 0)
	    {
		k++;
	    }
	}
	
	if((k / capacity) > threshold)
	{
	    reset();
	    return true;
	}
	
	return (k / capacity) > threshold;
    }

    public void reset()
    {
	this.driftSummary = new int[capacity];
    }
    
    public void setWaitTime(int value)
    {
	this.waittime = value;
    }
    
    public int getWaitTime()
    {
	return this.waittime;
    }
    
    public int getCapacity()
    {
	return this.capacity;
    }
    
/*    public int getCount()
    {
	return this.count;
    }
*/
}
