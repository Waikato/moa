package cutpointdetection;

import cutpointdetection.SEEDChangeDetector.SEED;

public class SingDetector implements CutPointDetector
{
    public SingWindow window;
    private double DELTA;
    private int defaultBlockSize;
    private int blockSize;
    private int elementCount;

    // Testing purpose public variable
    public int checks;
    public int warningCount = 0;
    
    public SingDetector(double delta, int blockSize)
    {
	this.DELTA = delta;
	this.defaultBlockSize = blockSize;
	this.blockSize = blockSize;
	this.window = new SingWindow(blockSize);
    }

    public SingDetector(double delta, int blockSize, int decayMode,
	    int compressionMode, double epsilonPrime, double alpha, int term)
    {
	this.DELTA = delta;
	this.defaultBlockSize = blockSize;
	this.blockSize = blockSize;
	this.window = new SingWindow(blockSize, decayMode, compressionMode,epsilonPrime, alpha, term);
    }

    @Override
    public boolean setInput(double inputValue)
    {
	SingBlock cursor;

	addElement(inputValue);

	if (elementCount % blockSize == 0 && window.getBlockCount() >= 2) // Drift Point Check
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

		    // System.out.println(window.getBlockCount() + " " + diff +
		    // " " + getADWINBound(n0, n1) + " " + getBernsteinBound(n0,
		    // n1) + " " + getRusselBound(n0, n1));

		    checks++;
		    // if(diff > getHoeffdingBound(n0, n1)) // drift exists
		    if (diff > getADWINBound(n0, n1))
		    // if(diff > getBernsteinBound(n0, n1))
		    // if(diff > getRusselBound(n0, n1))
		    // if(diff > getADWINRusselCorrectionBound(n0, n1))
		    {
			blnReduceWidth = true;
			window.resetDecayIteration();
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

			// BLOCK REDUCTION TECHNIQUE
			// window.setBlockSize(defaultBlockSize);
			// blockSize = defaultBlockSize;
			// warningStage = 0;

			return true;
		    }

		    // BLOCK REDUCTION TECHNIQUE
		    /*
		     * if(warning == false && diff > getADWINBound(n0, n1)*0.95)
		     * { warning = true; warningCount++; }
		     * 
		     * if(warning == true) { if(warningStage < 3) {
		     * window.setBlockSize((int)(window.getBlockSize() / 2.0));
		     * blockSize = (int)(window.getBlockSize() / 2.0);
		     * warningStage++; } else { warningStage = 0; warning =
		     * false; window.setBlockSize(defaultBlockSize); blockSize =
		     * defaultBlockSize; } }
		     */
		    cursor = cursor.getPrevious();
		}
	    }
	}

	return false;
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
/*
	double n = n0 + n1;
	// System.out.println(n0 + " " + n1);
	double dd = Math.log(2 * Math.log(n) / DELTA);
	double v = window.getVariance() / window.getWidth();
	double m = (1 / (n0)) + (1 / (n1));
	double epsilon = Math.sqrt(2 * m * v * dd) + (double) 2 / 3 * dd * m;

	return epsilon;
*/	
	double n = n0 + n1;
	// System.out.println(n0 + " " + n1);
	double dd = Math.log(2 * Math.log(n) / DELTA);
	double v = window.getVariance() / window.getWidth();
	double m = (1 / (n0)) + (1 / (n1));
	double epsilon = Math.sqrt(2 * m * v * dd) + (double) 2 / 3 * dd * m;	
//
// 	SINE FUNCTION	
//	System.out.println(volatility);
//	double x = clockcount / (double)volatility;
	double x = relPos;
	double ebar = epsilon * (1 + (tension * Math.sin(Math.PI * x)));	
//
//	SIGMOID FUNCTION
	double y = 1-x; 
	double esigbar = (1 + (tension * (y / (.01 + Math.abs(y))))) * epsilon;
	esigbar = x > 0.1 ? esigbar : epsilon;
//	System.out.println(epsilon + "," + ebar + "," + esigbar);
//
	//epsilon = ebar;
	
	if(mode == 1) { epsilon = ebar; }
	else if(mode == 2) { epsilon = esigbar; }	
	//
	
//	pWeight = (Math.abs(absvalue) - epsilon + 1) > pWeight ? Math.abs(absvalue) - epsilon + 1 : pWeight;
	
	//
	return epsilon;
    }
    
    private int mode = 0;
    public void setMode(int value)
    {
	mode = value;
    }
    private double tension = 0.0;
    public void setTension(double value)
    {
	this.tension = value;
    }
    
    private double relPos;
    public boolean setInput(double intEntrada, double delta, double x)
    {
	relPos = x;
	relPos = x > 1.0 ? 1.0 : x;
	return setInput(intEntrada);
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
	return checks;
    }
}
