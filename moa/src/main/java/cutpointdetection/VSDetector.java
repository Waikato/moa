package cutpointdetection;
/**
 * 
 * @deprecated
 *
 */
public class VSDetector implements CutPointDetector
{
    public VSWindow window;
    private double DELTA;
    private int defaultBlockSize;
    private int blockSize;
    private int elementCount;

    private int prevDriftPoint;
    private int volatilityPrediction;
    private double[] adjustmentMatrix;
    
    // Testing purpose public variable
    public int checks;
    public int warningCount = 0;

    public VSDetector(double delta, int blockSize)
    {
	this.DELTA = delta;
	this.defaultBlockSize = blockSize;
	this.blockSize = blockSize;
	this.window = new VSWindow(blockSize);
	
	this.prevDriftPoint = 0;
	this.volatilityPrediction = 0;
	this.adjustmentMatrix = new double[]{1,30,30,30,30,30,30,30,30,1};
    }

    @Override
    public boolean setInput(double inputValue)
    {
	blockAdjustment();
	
	VSBlock cursor;

	addElement(inputValue);

	if (window.getIsBlockFull() && window.getBlockCount() >= 2) // Drift Point Check
	{
	    boolean blnReduceWidth = true;

	    while (blnReduceWidth)
	    {
		// int warningStage = 0;
		// boolean warning = false;

		blnReduceWidth = false;
		int n1 = 0;
		int n0 = window.getWidth();
		double u1 = 0;
		double u0 = window.getTotal();

		cursor = window.getTail();
		while (cursor.getPrevious() != null)
		{
		    n0 -= cursor.getItemCount();
		    n1 += cursor.getItemCount();
		    u0 -= cursor.getTotal();
		    u1 += cursor.getTotal();
		    double diff = Math.abs(u1 / n1 - (u0 / n0));

		    checks++;
		    // if(diff > getHoeffdingBound(n0, n1)) // drift exists
		    if (diff > getADWINBound(n0, n1))
		    // if(diff > getBernsteinBound(n0, n1))
		    // if(diff > getRusselBound(n0, n1))
		    // if(diff > getADWINRusselCorrectionBound(n0, n1))
		    {
			blnReduceWidth = true;
			window.setHead(cursor);

			while (cursor.getPrevious() != null)
			{
			    cursor = cursor.getPrevious();
			    window.setWidth(window.getWidth() - cursor.getItemCount());
			    window.setTotal(window.getTotal() - cursor.getTotal());
			    window.setVariance(window.getVariance() - cursor.getVariance());
			    window.setBlockCount(window.getBlockCount() - 1);
			}

			window.getHead().setPrevious(null);
			this.prevDriftPoint = this.elementCount;
			
			return true;
		    }

		    cursor = cursor.getPrevious();
		}
	    }
	}

	return false;
    }
    
    //can be optimized
    public void blockAdjustment()
    {
	if(elementCount >= this.volatilityPrediction){this.volatilityPrediction += 10000;}
	
	int range = this.volatilityPrediction - this.prevDriftPoint;
	int pos = ((this.elementCount - this.prevDriftPoint) / range) * 10;
	this.window.setBlockSize((int)(this.adjustmentMatrix[pos] * this.defaultBlockSize));
    }

    private double getRusselBound(double n0, double n1)
    {
	double n = n0 + n1;
	double dPrime = 2 * DELTA / (1.0 - Math.pow(0.5, n));
	double p = Math.log(4 / dPrime);
	double b = defaultBlockSize;
	double v = window.getVariance() / window.getWidth();

	double epsilon = (2 / (3 * b))
		* (p + Math.sqrt((p * p) + (18 * v * v * b * p)));

	return epsilon;
    }

    private double getBernsteinBound(double n0, double n1)
    {
	double n = n0 + n1;
	double dPrime = 2 * DELTA / (1.0 - Math.pow(0.5, n));
	double p = Math.log(4 / dPrime);
	double m = (n1 * n0) / (n0 + n1);
	double k = n1 / (n1 + n0);
	double v = window.getVariance() / window.getWidth();

	double epsilon = (1 / (3 * k * m))
		* (k * p + Math.sqrt(Math.pow(k * p, 2)
			+ (18 * v * v * m * p * k)));

	return epsilon;
    }

    private double getADWINBound(double n0, double n1)
    {
	double n = n0 + n1;
	// System.out.println(n0 + " " + n1);
	double dd = Math.log(2 * Math.log(n) / DELTA);
	double v = window.getVariance() / window.getWidth();
	double m = (1 / (n0)) + (1 / (n1));
	double epsilon = Math.sqrt(2 * m * v * dd) + (double) 2 / 3 * dd * m;

	return epsilon;
    }

    private double getADWINRusselCorrectionBound(double n0, double n1)
    {
	double n = n0 + n1;
	double dPrime = 2 * DELTA / (1.0 - Math.pow(0.5, n));
	// System.out.println(n0 + " " + n1);
	double dd = Math.log(2 / dPrime);
	double v = window.getVariance() / window.getWidth();
	double m = (1 / (n0)) + (1 / (n1));
	double epsilon = Math.sqrt(2 * m * v * dd) + (double) 2 / 3 * dd * m;

	return epsilon;
    }

    private double getHoeffdingBound(double n0, double n1)
    {
	double m = 1 / ((1 / n0) + (1 / n1));
	double deltaPrime = DELTA / (n0 + n1);
	double epsilon = Math.sqrt((1 / (2 * m)) * Math.log(4 / deltaPrime));

	return epsilon;
    }

    public void addElement(double value)
    {
	window.addTransaction(value);
	elementCount++;
    }

    public int getChecks()
    {
	// TODO Auto-generated method stub
	return 0;
    }
}
