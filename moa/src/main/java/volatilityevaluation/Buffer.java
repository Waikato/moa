package volatilityevaluation;

public class Buffer
{
	private double[] buffer;
	private int size;
	private int slidingIndex;
	private boolean isFull;

	private double total;

	public Buffer(int size)
	{
		this.buffer = new double[size];
		this.size = size;
		this.slidingIndex = 0;
		this.isFull = false;

		this.total = 0;
	}
	
	/**
	 * Calculate intervals between elements in the buffer. And returns the mean of all intervals.
	 * @return mean of all intervals
	 */
	public double getIntervalsMean()
	{
		
		if (isFull)
		{
			double sum = 0;
			int intervalsNum = size - 1;
			
			for(int i=0;i<intervalsNum;i++)
			{
				sum += buffer[i+1] - buffer[i];
			}
			return sum / intervalsNum;
			
		} else
		{
			double sum = 0;
			int intervalsNum = slidingIndex-1;
			
			for(int i=0;i<intervalsNum;i++)
			{
				sum += buffer[i+1] - buffer[i];
			}
			return sum / intervalsNum;
		}
		
	}

	public double add(double value)
	{
		if (slidingIndex == size)
		{
			isFull = true;
			slidingIndex = 0;
		}

		double removed = buffer[slidingIndex];
		total -= removed;

		buffer[slidingIndex++] = value;
		total += value;

		if (isFull)
		{
			return removed;
		} else
		{
			return -1;
		}
	}

	public double getMean()
	{
		if (isFull)
		{
			return total / size;
		} else
		{
			return total / slidingIndex;
		}
	}

	public Boolean isFull()
	{
		return isFull;
	}

	public void clear()
	{
		this.buffer = new double[size];
		this.slidingIndex = 0;
		this.isFull = false;

		this.total = 0;
	}

	public double[] getBuffer()
	{
		return buffer;
	}

	public double getStdev()
	{
		return calculateStdev(buffer, getMean());
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
}
