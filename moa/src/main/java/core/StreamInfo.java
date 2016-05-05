package core;

public class StreamInfo
{

    private int blockSize;
    private double delta;
    private double variance;
    private double diff;
    private double mean;
    private int K;
    private double currentValue;
    private double prevValue;

    public void setBlockSize(int size)
    {
	this.blockSize = size;
    }

    public int getBlockSize()
    {
	return this.blockSize;
    }

    public void setDelta(double delta)
    {
	this.delta = delta;
    }

    public double getDelta()
    {
	return this.delta;
    }

    public void setMean(double mean)
    {
	this.mean = mean;
    }

    public double getMean()
    {
	return this.mean;
    }

    public void setK(int K)
    {
	this.K = K;
    }

    public int getK()
    {
	return this.K;
    }

    public void setVariance(double variance)
    {
	this.variance = variance;
    }

    public double getVariance()
    {
	return this.variance;
    }

    public void setDiff(double diff)
    {
	this.diff = diff;
    }

    public double getDiff()
    {
	return this.diff;
    }

    public void setCurrent(double currentValue)
    {
	this.currentValue = currentValue;
    }

    public double getCurrent()
    {
	return this.currentValue;
    }

    public void setPrev(double prevValue)
    {
	this.prevValue = prevValue;
    }

    public double getPrev()
    {
	return this.prevValue;
    }
}
