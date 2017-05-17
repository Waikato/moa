package cutpointdetection;
/**
 * 
 * @deprecated
 *
 */
public class VSWindow
{
    private VSBlock head;
    private VSBlock tail;

    private int blockSize;
    private int width;
    private double total;
    private double variance;
    private int blockCount;

    public VSWindow(int blockSize)
    {
	clear();
	this.blockSize = blockSize;
	addBlockToHead(new VSBlock(blockSize));
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
	    addBlockToTail(new VSBlock(this.blockSize));
	}
	tail.add(value);
	total += value;

	width++;
	
	if (width >= 2)
	{
	    double incVariance = (width - 1) * (value - total / (width - 1)) * (value - total / (width - 1)) / width;
	    variance += incVariance;
	    tail.setVariance(tail.getVariance() + incVariance);
	}
    }

    public void addBlockToHead(VSBlock block)
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

    public void removeBlock(VSBlock block)
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
	} 
	else if (block.getPrevious() == null && block.getNext() != null)
	{
	    block.getNext().setPrevious(null);
	    head = block.getNext();
	    block.setNext(null);
	} 
	else if (block.getPrevious() != null && block.getNext() == null)
	{
	    block.getPrevious().setNext(null);
	    tail = block.getPrevious();
	    block.setPrevious(null);
	} 
	else if (block.getPrevious() == null && block.getNext() == null)
	{
	    head = null;
	    tail = null;
	}
    }

    public void addBlockToTail(VSBlock block)
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
    
    public boolean getIsBlockFull()
    {
	return this.tail.isFull();
    }

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

    public void setHead(VSBlock head)
    {
	this.head = head;
    }

    public void setTail(VSBlock tail)
    {
	this.tail = tail;
    }

    public VSBlock getHead()
    {
	return this.head;
    }

    public VSBlock getTail()
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

}
