/*
 *    ADWIN.java
 *    Copyright (C) 2008 UPC-Barcelona Tech, Catalonia
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

package cutpointdetection;

public class ADWIN_Volatility implements CutPointDetector
{

    private class List
    {

	protected int count;

	protected ListItem head;

	protected ListItem tail;

	public List()
	{
	    // post: initializes the list to be empty.
	    clear();
	    addToHead();
	}

	/* Interface Store Methods */
	public int size()
	{
	    // post: returns the number of elements in the list.
	    return this.count;
	}

	public ListItem head()
	{
	    // post: returns the number of elements in the list.
	    return this.head;
	}

	public ListItem tail()
	{
	    // post: returns the number of elements in the list.
	    return this.tail;
	}

	public boolean isEmpty()
	{
	    // post: returns the true iff store is empty.
	    return (this.size() == 0);
	}

	public void clear()
	{
	    // post: clears the list so that it contains no elements.
	    this.head = null;
	    this.tail = null;
	    this.count = 0;
	}

	/* Interface List Methods */
	public void addToHead()
	{
	    // pre: anObject is non-null
	    // post: the object is added to the beginning of the list
	    this.head = new ListItem(this.head, null);
	    if (this.tail == null)
	    {
		this.tail = this.head;
	    }
	    this.count++;
	}

	public void removeFromHead()
	{
	    // pre: list is not empty
	    // post: removes and returns first object from the list
	    // ListItem temp;
	    // temp = this.head;
	    this.head = this.head.next();
	    if (this.head != null)
	    {
		this.head.setPrevious(null);
	    } else
	    {
		this.tail = null;
	    }
	    this.count--;
	    // temp=null;
	    return;
	}

	public void addToTail()
	{
	    // pre: anObject is non-null
	    // post: the object is added at the end of the list
	    this.tail = new ListItem(null, this.tail);
	    if (this.head == null)
	    {
		this.head = this.tail;
	    }
	    this.count++;
	}

	public void removeFromTail()
	{
	    // pre: list is not empty
	    // post: the last object in the list is removed and returned
	    // ListItem temp;
	    // temp = this.tail;
	    this.tail = this.tail.previous();
	    if (this.tail == null)
	    {
		this.head = null;
	    } else
	    {
		this.tail.setNext(null);
	    }
	    this.count--;
	    // temp=null;
	    return;
	}

    }

    private class ListItem
    {
	// protected Object data;

	protected ListItem next;

	protected ListItem previous;

	protected int bucketSizeRow = 0;

	protected int MAXBUCKETS = ADWIN_Volatility.MAXBUCKETS;

	protected double bucketTotal[] = new double[MAXBUCKETS + 1];

	protected double bucketVariance[] = new double[MAXBUCKETS + 1];

	public ListItem()
	{
	    // post: initializes the node to be a tail node
	    // containing the given value.
	    this(null, null);
	}

	public void clear()
	{
	    bucketSizeRow = 0;
	    for (int k = 0; k <= MAXBUCKETS; k++)
	    {
		clearBucket(k);
	    }
	}

	private void clearBucket(int k)
	{
	    setTotal(0, k);
	    setVariance(0, k);
	}

	public ListItem(ListItem nextNode, ListItem previousNode)
	{
	    // post: initializes the node to contain the given
	    // object and link to the given next node.
	    // this.data = element;
	    this.next = nextNode;
	    this.previous = previousNode;
	    if (nextNode != null)
	    {
		nextNode.previous = this;
	    }
	    if (previousNode != null)
	    {
		previousNode.next = this;
	    }
	    clear();
	}

	public void insertBucket(double Value, double Variance)
	{
	    // insert a Bucket at the end
	    int k = bucketSizeRow;
	    bucketSizeRow++;
	    // Insert new bucket
	    setTotal(Value, k);
	    setVariance(Variance, k);
	}

	public void RemoveBucket()
	{
	    // Removes the first Buvket
	    compressBucketsRow(1);
	}

	public void compressBucketsRow(int NumberItemsDeleted)
	{
	    // Delete first elements
	    for (int k = NumberItemsDeleted; k <= MAXBUCKETS; k++)
	    {
		bucketTotal[k - NumberItemsDeleted] = bucketTotal[k];
		bucketVariance[k - NumberItemsDeleted] = bucketVariance[k];
	    }
	    for (int k = 1; k <= NumberItemsDeleted; k++)
	    {
		clearBucket(MAXBUCKETS - k + 1);
	    }
	    bucketSizeRow -= NumberItemsDeleted;
	    // BucketNumber-=NumberItemsDeleted;
	}

	public ListItem previous()
	{
	    // post: returns the previous node.
	    return this.previous;
	}

	public void setPrevious(ListItem previous)
	{
	    // post: sets the previous node to be the given node
	    this.previous = previous;
	}

	public ListItem next()
	{
	    // post: returns the next node.
	    return this.next;
	}

	public void setNext(ListItem next)
	{
	    // post: sets the next node to be the given node
	    this.next = next;
	}

	public double Total(int k)
	{
	    // post: returns the element in this node
	    return bucketTotal[k];
	}

	public double Variance(int k)
	{
	    // post: returns the element in this node
	    return bucketVariance[k];
	}

	public void setTotal(double value, int k)
	{
	    // post: sets the element in this node to the given
	    // object.
	    bucketTotal[k] = value;
	}

	public void setVariance(double value, int k)
	{
	    // post: sets the element in this node to the given
	    // object.
	    bucketVariance[k] = value;
	}
	/*
	 * public ListItem(Object element, ListItem nextNode){ // post:
	 * initializes the node to contain the given // object and link to the
	 * given next node. this.data = element; this.next = nextNode; } public
	 * ListItem(Object element) { // post: initializes the node to be a tail
	 * node // containing the given value. this(element, null); }
	 * 
	 * 
	 * public Object value() { // post: returns the element in this node
	 * return this.data; } public void setValue(Object anObject) { // post:
	 * sets the element in this node to the given // object. this.data =
	 * anObject; }
	 */

    }

    public static final double DELTA = .002; // .1;

    private static final int mintMinimLongitudWindow = 10; // 10

    private double mdbldelta = .002; // .1;

    private int mintTime = 0;

    private int mintClock = 32;

    private double mdblWidth = 0; // Mean of Width = mdblWidth/Number of items
    // BUCKET

    public static final int MAXBUCKETS = 5;

    private int lastBucketRow = 0;

    private double TOTAL = 0;

    private double VARIANCE = 0;

    private int WIDTH = 0;

    private int BucketNumber = 0;

    private int Detect = 0;

    private int numberDetections = 0;

    private int DetectTwice = 0;

    private boolean blnBucketDeleted = false;

    private int BucketNumberMAX = 0;

    private int mintMinWinLength = 5;

    private List listRowBuckets;

    //
    private int checks = 0;

    public int getChecks()
    {
	return checks;
    }

    private double pWeight = 0.0;
    private int weightCheckCount = 0;
    
    public double getWeight()
    {
	return pWeight;
    }
    
    public void setWeight(double value)
    {
	this.pWeight = value;
    }
    
    private double tension = 0.0;
    
    public void setTension(double value)
    {
	this.tension = value;
    }
    //

    public boolean getChange()
    {
	return blnBucketDeleted;
    }

    public void resetChange()
    {
	blnBucketDeleted = false;
    }

    public int getBucketsUsed()
    {
	return BucketNumberMAX;
    }

    public int getWidth()
    {
	return WIDTH;
    }

    public void setClock(int intClock)
    {
	mintClock = intClock;
    }

    public int getClock()
    {
	return mintClock;
    }

    public boolean getWarning()
    {
	return false;
    }

    public boolean getDetect()
    {
	return (Detect == mintTime);
    }

    public int getNumberDetections()
    {
	return numberDetections;
    }

    public double getTotal()
    {
	return TOTAL;
    }

    public double getEstimation()
    {
	return TOTAL / WIDTH;
    }

    public double getVariance()
    {
	return VARIANCE / WIDTH;
    }

    public double getWidthT()
    {
	return mdblWidth;
    }

    private void initBuckets()
    {
	// Init buckets
	listRowBuckets = new List();
	lastBucketRow = 0;
	TOTAL = 0;
	VARIANCE = 0;
	WIDTH = 0;
	BucketNumber = 0;
    }

    private void insertElement(double Value)
    {
	WIDTH++;
	insertElementBucket(0, Value, listRowBuckets.head());
	double incVariance = 0;
	if (WIDTH > 1)
	{
	    incVariance = (WIDTH - 1) * (Value - TOTAL / (WIDTH - 1))
		    * (Value - TOTAL / (WIDTH - 1)) / WIDTH;
	}
	VARIANCE += incVariance;
	TOTAL += Value;
	compressBuckets();
    }

    private void insertElementBucket(double Variance, double Value,
	    ListItem Node)
    {
	// Insert new bucket
	Node.insertBucket(Value, Variance);
	BucketNumber++;
	if (BucketNumber > BucketNumberMAX)
	{
	    BucketNumberMAX = BucketNumber;
	}
    }

    private int bucketSize(int Row)
    {
	return (int) Math.pow(2, Row);
    }

    public int deleteElement()
    {
	// LIST
	// Update statistics
	ListItem Node;
	Node = listRowBuckets.tail();
	int n1 = bucketSize(lastBucketRow);
	WIDTH -= n1;
	TOTAL -= Node.Total(0);
	double u1 = Node.Total(0) / n1;
	double incVariance = Node.Variance(0) + n1 * WIDTH
		* (u1 - TOTAL / WIDTH) * (u1 - TOTAL / WIDTH) / (n1 + WIDTH);
	VARIANCE -= incVariance;

	// Delete Bucket
	Node.RemoveBucket();
	BucketNumber--;
	if (Node.bucketSizeRow == 0)
	{
	    listRowBuckets.removeFromTail();
	    lastBucketRow--;
	}
	return n1;
    }

    public void compressBuckets()
    {
	// Traverse the list of buckets in increasing order
	int n1, n2;
	double u2, u1, incVariance;
	ListItem cursor;
	ListItem nextNode;
	cursor = listRowBuckets.head();
	int i = 0;
	do
	{
	    // Find the number of buckets in a row
	    int k = cursor.bucketSizeRow;
	    // If the row is full, merge buckets
	    if (k == MAXBUCKETS + 1)
	    {
		nextNode = cursor.next();
		if (nextNode == null)
		{
		    listRowBuckets.addToTail();
		    nextNode = cursor.next();
		    lastBucketRow++;
		}
		n1 = bucketSize(i);
		n2 = bucketSize(i);
		u1 = cursor.Total(0) / n1;
		u2 = cursor.Total(1) / n2;
		incVariance = n1 * n2 * (u1 - u2) * (u1 - u2) / (n1 + n2);

		nextNode.insertBucket(cursor.Total(0) + cursor.Total(1),
			cursor.Variance(0) + cursor.Variance(1) + incVariance);
		BucketNumber++;
		cursor.compressBucketsRow(2);
		if (nextNode.bucketSizeRow <= MAXBUCKETS)
		{
		    break;
		}
	    } else
	    {
		break;
	    }
	    cursor = cursor.next();
	    i++;
	} while (cursor != null);
    }

    @Override
    public boolean setInput(double intEntrada)
    {
	return setInput(intEntrada, mdbldelta);
    }

    private double relPos;
    public boolean setInput(double intEntrada, double delta, double x)
    {
	relPos = x;
	relPos = x > 1.0 ? 1.0 : x;
	return setInput(intEntrada, delta);
    }
    
    public boolean setInput(double intEntrada, double delta)
    {
	//System.out.println("here");
	boolean blnChange = false;
	boolean blnExit = false;
	ListItem cursor;
	mintTime++;

	// 1,2)Increment window in one element
	insertElement(intEntrada);
	blnBucketDeleted = false;
	// 3)Reduce window
	if (mintTime % mintClock == 0 && getWidth() > mintMinimLongitudWindow)
	{

	    boolean blnReduceWidth = true; // Diference

	    while (blnReduceWidth) // Diference
	    {
		blnReduceWidth = false; // Diference
		blnExit = false;
		int n0 = 0;
		int n1 = WIDTH;
		double u0 = 0;
		double u1 = getTotal();
		double v0 = 0;
		double v1 = VARIANCE;
		double n2 = 0;
		double u2 = 0;
		
		//
		weightCheckCount = weightCheckCount < mintClock ? weightCheckCount + 1 : 0 ;
		pWeight = weightCheckCount == 0 ? 0.0 : pWeight;
		//

		cursor = listRowBuckets.tail();
		int i = lastBucketRow;
		do
		{
		    for (int k = 0; k <= (cursor.bucketSizeRow - 1); k++)
		    {
			n2 = bucketSize(i);
			u2 = cursor.Total(k);
			if (n0 > 0)
			{
			    v0 += cursor.Variance(k) + n0 * n2
				    * (u0 / n0 - u2 / n2) * (u0 / n0 - u2 / n2)
				    / (n0 + n2);
			}
			if (n1 > 0)
			{
			    v1 -= cursor.Variance(k) + n1 * n2
				    * (u1 / n1 - u2 / n2) * (u1 / n1 - u2 / n2)
				    / (n1 + n2);
			}

			n0 += bucketSize(i);
			n1 -= bucketSize(i);
			u0 += cursor.Total(k);
			u1 -= cursor.Total(k);

			if (i == 0 && k == cursor.bucketSizeRow - 1)
			{
			    blnExit = true;
			    break;
			}
			checks++;
			double absvalue = u0 / n0 - (u1 / n1); // n1<WIDTH-mintMinWinLength-1
			if ((n1 > mintMinWinLength + 1 && n0 > mintMinWinLength + 1)
				&& // Diference NEGATIVE
				blnCutexpression(n0, n1, u0, u1, v0, v1, absvalue, delta))
			{
			    blnBucketDeleted = true;
			    Detect = mintTime;

			    if (Detect == 0)
			    {
				Detect = mintTime;
				// blnFirst=true;
				// blnWarning=true;
			    } else if (DetectTwice == 0)
			    {
				DetectTwice = mintTime;
				// blnDetect=true;
			    }
			    blnReduceWidth = true; // Diference
			    blnChange = true;			
			    if (getWidth() > 0)
			    { // Reduce width of the window
				// while (n0>0) // Diference NEGATIVE
				n0 -= deleteElement();
				blnExit = true;
				break;
			    }
			} // End if
		    }// Next k
		    cursor = cursor.previous();
		    i--;
		} while (((!blnExit && cursor != null)));
	    }// End While // Diference
	}// End if

	mdblWidth += getWidth();
	if (blnChange)
	{
	    numberDetections++;
	}
	return blnChange;
    }
    
    private int mode = 0;
    
    public void setMode(String value)
    {
	if(value.equals("SINE"))
	{
	    mode = 1;
	}
	else if(value.equals("SIGMOID"))
	{
	    mode = 2;
	}
    }

    private boolean blnCutexpression(int n0, int n1, double u0, double u1, double v0, double v1, double absvalue, double delta)
    {
	int n = getWidth();
	double dd = Math.log(2 * Math.log(n) / delta); // -- ull perque el ln n
						       // va al numerador.
	// Formula Gener 2008
	double v = getVariance();
	// System.out.println(v + " " + WIDTH);
	double m = ((double) 1 / ((n0 - mintMinWinLength + 1)))
		+ ((double) 1 / ((n1 - mintMinWinLength + 1)));
	double epsilon = Math.sqrt(2 * m * v * dd) + (double) 2 / 3 * dd * m;

	double x = relPos;
	double y = 1-x; 

	if(mode == 1) 
	{ 
	    //SINE FUNCTION	
	    double esinebar = epsilon * (1 + (tension * Math.sin(Math.PI * x)));	
	    epsilon = esinebar; 
	}
	else if(mode == 2) 
	{
	    //SIGMOID FUNCTION
	    double esigbar = (1 + (tension * (y / (.01 + Math.abs(y))))) * epsilon;
	    epsilon = esigbar; 
	}	

	return (Math.abs(absvalue) > epsilon);
    }

    public ADWIN_Volatility()
    {
	mdbldelta = DELTA;
	initBuckets();
	Detect = 0;
	numberDetections = 0;
	DetectTwice = 0;

    }

    public ADWIN_Volatility(double d)
    {
	mdbldelta = d;
	initBuckets();
	Detect = 0;
	numberDetections = 0;
	DetectTwice = 0;
    }

    public ADWIN_Volatility(int cl)
    {
	mdbldelta = DELTA;
	initBuckets();
	Detect = 0;
	numberDetections = 0;
	DetectTwice = 0;
	mintClock = cl;
    }

    public String getEstimatorInfo()
    {
	return "ADWIN;;";
    }

    public void setW(int W0)
    {
    }

}