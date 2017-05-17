package cutpointdetection;

public class SingWindow
{
    private SingBlock head;
    private SingBlock tail;

    private int blockSize;
    private int width;
    private double total;
    private double variance;
    private int blockCount;

    private int DECAY_MODE = 1;
    private final int LINEAR_DECAY = 1;
    private final int EXPONENTIAL_DECAY = 2;

    private int COMPRESSION_MODE = 1;
    private final int FIXED_TERM = 1;
    private final int PARETO = 2;

    private int decayCompressionCount = 0;
    private int decayIteration = 1;
    private int paretoDecayTermSize = 200;
    private int paretoDecayDefaultTermSize = 200;
    private int linearFixedTermSize = 50;

    private double epsilonPrime = 0.0;
    private double alpha = 0.0;

    public SingWindow(int blockSize)
    {
	clear();
	this.blockSize = blockSize;
	addBlockToHead(new SingBlock(blockSize));
    }

    public SingWindow(int blockSize, int decayMode, int compressionMode,
	    double epsilonPrime, double alpha, int compressionTerm)
    {
	clear();
	this.blockSize = blockSize;
	this.DECAY_MODE = decayMode;
	this.COMPRESSION_MODE = compressionMode;
	this.epsilonPrime = epsilonPrime;
	this.alpha = alpha;
	setCompressionTerm(compressionTerm);
	addBlockToHead(new SingBlock(blockSize));
    }

    public void clear()
    {
	head = null;
	tail = null;
	width = 0;
	blockCount = 0;
	total = 0;
	variance = 0;
    }

    public void addTransaction(double value)
    {
	if (tail.isFull())
	{
	    /*
	     * // FIXED EPSILON APPROACH if(tail.getPrevious() != null &&
	     * checkHomogeneity(tail)) // Check if homogeneous & compress blocks
	     * { compCount++; tail.getPrevious().setTotal(tail.getTotal() +
	     * tail.getPrevious().getTotal());
	     * tail.getPrevious().setItemCount(tail.getItemCount() +
	     * tail.getPrevious().getItemCount());
	     * tail.getPrevious().setVariance(tail.getVariance() +
	     * tail.getPrevious().getVariance());
	     * 
	     * blockCount--; tail = tail.getPrevious(); tail.setNext(null); }
	     */

	    // INCREMENTING EPSILON APPROACH
	    if (COMPRESSION_MODE == FIXED_TERM)
	    {
		if (tail.getPrevious() != null
			&& decayCompressionCount > linearFixedTermSize)
		{
		    decayCompressionCount = 0;
		    SingBlock cursor = tail;

		    double epsilon = 0.0;

		    int i = 0; // Exponential

		    while (cursor != null && cursor.getPrevious() != null)
		    {
			double n0 = cursor.getItemCount();
			double n1 = cursor.getPrevious().getItemCount();
			double u0 = cursor.getTotal();
			double u1 = cursor.getPrevious().getTotal();

			double diff = Math.abs(u1 / n1 - (u0 / n0));

			if (DECAY_MODE == LINEAR_DECAY)
			{
			    epsilon += epsilonPrime * alpha;
			} 
			else if (DECAY_MODE == EXPONENTIAL_DECAY)
			{
			    epsilon = epsilonPrime * Math.pow(1 + alpha, i);
			}

			if (diff < epsilon)
			{
			    compressBlock(cursor);
			}
			cursor = cursor.getPrevious();
			i++; // Exponential
		    }
		}
	    } else if (COMPRESSION_MODE == PARETO)
	    {
		// INCREMENTING EPSILON APPROACH
		if (tail.getPrevious() != null
			&& decayCompressionCount == paretoDecayTermSize)
		{
		    paretoDecayTermSize = getParetoEpsilon(decayIteration++);
		    decayCompressionCount = 0;
		    SingBlock cursor = tail;

		    double epsilon = 0.0;

		    // int i = blockCount; // Sublinear
		    int i = 0; // Exponential

		    while (cursor != null && cursor.getPrevious() != null)
		    {
			double n0 = cursor.getItemCount();
			double n1 = cursor.getPrevious().getItemCount();
			double u0 = cursor.getTotal();
			double u1 = cursor.getPrevious().getTotal();

			double diff = Math.abs(u1 / n1 - (u0 / n0));

			// SUBLINEAR DECAY FUNCTION
			// epsilon += epsilonPrime * Math.pow(alpha, blockCount
			// - i);

			// EXPONENTIAL DECAY FUNCTION
			// epsilon = epsilonPrime * Math.pow(1+alpha, i);

			// LINEAR DECAY FUNCTION
			// epsilon += epsilonPrime * alpha;

			if (DECAY_MODE == LINEAR_DECAY)
			{
			    epsilon += epsilonPrime * alpha;
			} 
			else if (DECAY_MODE == EXPONENTIAL_DECAY)
			{
			    epsilon = epsilonPrime * Math.pow(1 + alpha, i);
			}

			if (diff < epsilon)
			{
			    compressBlock(cursor);
			}
			cursor = cursor.getPrevious();
			// i--; // Sublinear
			i++; // Exponential
		    }
		}
	    }

	    addBlockToTail(new SingBlock(this.blockSize));
	    decayCompressionCount++;
	}
	tail.add(value);
	total += value;

	width++;
	if (width >= 2)
	{
	    double incVariance = (width - 1) * (value - total / (width - 1))
		    * (value - total / (width - 1)) / width;
	    variance += incVariance;
	    tail.setVariance(tail.getVariance() + incVariance);
	}

    }

    public void resetDecayIteration()
    {
	this.decayIteration = 0;
    }

    public int getParetoEpsilon(int x)
    {
	double paretoAlpha = 1;
	int size = (int) Math.pow(x, -1 * paretoAlpha)
		* paretoDecayDefaultTermSize;

	if (size < 32)
	{
	    return 32;
	}

	return size;

    }

    public void compressBlock(SingBlock cursor)
    {
	// System.out.println("COMPRESS");
	cursor.getPrevious().setTotal(
		cursor.getTotal() + cursor.getPrevious().getTotal());
	cursor.getPrevious().setItemCount(
		cursor.getItemCount() + cursor.getPrevious().getItemCount());
	cursor.getPrevious().setVariance(
		cursor.getVariance() + cursor.getPrevious().getVariance());
	cursor.getPrevious().setBlockSize(
		cursor.getBlockSize() + cursor.getPrevious().getBlockSize());

	if (cursor.getNext() != null)
	{
	    cursor.getPrevious().setNext(cursor.getNext());
	    cursor.getNext().setPrevious(cursor.getPrevious());
	} else
	{
	    cursor.getPrevious().setNext(null);
	    tail = cursor.getPrevious();
	}
	// cursor.setPrevious(null);
	// cursor.setNext(null);

	blockCount--;
    }

    public boolean checkHomogeneity(SingBlock block)
    {
	double diff = Math.abs(block.getMean() - block.getPrevious().getMean());
	double epsilonPrime = getADWINBound(block.getItemCount(), block
		.getPrevious().getItemCount());
	// double epsilonPrime = 0.01;
	if (diff < epsilonPrime)
	{
	    return true;
	} else
	{
	    return false;
	}
    }

    private double getADWINBound(double n0, double n1)
    {
	double n = n0 + n1;
	double dd = Math.log(2 * Math.log(n) / 0.99);
	double v = variance / width;
	double m = (1 / (n0)) + (1 / (n1));
	double epsilon = Math.sqrt(2 * m * v * dd) + (double) 2 / 3 * dd * m;

	return epsilon;
    }

    public void addBlockToHead(SingBlock block)
    {
	if (head == null)
	{
	    head = block;
	    tail = block;
	} else
	{
	    block.setNext(head);
	    head.setPrevious(block);
	    head = block;
	}
	blockCount++;
    }

    public void removeBlock(SingBlock block)
    {
	width -= block.getItemCount();
	total -= block.getTotal();
	variance -= block.getVariance();
	blockCount--;

	if (block.getPrevious() != null && block.getNext() != null)
	{
	    block.getPrevious().setNext(block.getNext());
	    block.getNext().setPrevious(block.getPrevious());
	    block.setNext(null);
	    block.setPrevious(null);
	} else if (block.getPrevious() == null && block.getNext() != null)
	{
	    block.getNext().setPrevious(null);
	    head = block.getNext();
	    block.setNext(null);
	} else if (block.getPrevious() != null && block.getNext() == null)
	{
	    block.getPrevious().setNext(null);
	    tail = block.getPrevious();
	    block.setPrevious(null);
	} else if (block.getPrevious() == null && block.getNext() == null)
	{
	    head = null;
	    tail = null;
	}
    }

    // public void removeBlockFromHead()
    // {
    // width -= head.getItemCount();
    // total -= head.getTotal();
    //
    // int n1 = head.getItemCount();
    // double u1 = head.getTotal() / n1;
    // double incVariance = head.getVariance() + n1 * width * (u1 - total /
    // width) * (u1 - total / width) / (n1 + width);
    // System.out.println(incVariance);
    // variance -= incVariance;
    //
    // head = head.getNext();
    // if (head != null) {
    // head.setPrevious(null);
    // } else {
    // tail = null;
    // }
    // blockCount--;
    // }

    public void addBlockToTail(SingBlock block)
    {
	if (tail == null)
	{
	    tail = block;
	    head = block;
	} else
	{
	    block.setPrevious(tail);
	    tail.setNext(block);
	    tail = block;
	}
	blockCount++;
    }

    // public void removeBlockFromTail()
    // {
    // width -= tail.getItemCount();
    // total -= tail.getTotal();
    // tail = tail.getPrevious();
    // if (tail != null) {
    // tail.setNext(null);
    // } else {
    // head = null;
    // }
    // blockCount--;
    // }

    public int getBlockCount()
    {
	return this.blockCount;
    }

    public void setBlockCount(int value)
    {
	this.blockCount = value;
    }

    public int getWidth()
    {
	return this.width;
    }

    public void setWidth(int value)
    {
	this.width = value;
    }

    public void setHead(SingBlock head)
    {
	this.head = head;
    }

    public void setTail(SingBlock tail)
    {
	this.tail = tail;
    }

    public SingBlock getHead()
    {
	return this.head;
    }

    public SingBlock getTail()
    {
	return this.tail;
    }

    public double getTotal()
    {
	return this.total;
    }

    public void setTotal(double value)
    {
	this.total = value;
    }

    public double getVariance()
    {
	return this.variance;
    }

    public void setVariance(double value)
    {
	this.variance = value;
    }

    public void setBlockSize(int value)
    {
	if (value > 32)
	{
	    this.blockSize = value;
	} else
	{
	    this.blockSize = 32;
	}
    }

    public int getBlockSize()
    {
	return this.blockSize;
    }

    public double getEpsilonPrime()
    {
	return epsilonPrime;
    }

    public void setEpsilonPrime(double epsilonPrime)
    {
	this.epsilonPrime = epsilonPrime;
    }

    public void setAlpha(double alpha)
    {
	this.alpha = alpha;
    }

    public void setCompressionTerm(int value)
    {
	this.paretoDecayTermSize = value;
	this.paretoDecayDefaultTermSize = value;
	this.linearFixedTermSize = value;
    }
}
