package driftcategorization;

public class DriftCategorizer
{
    private final double DELTA;
    private int checkInterval;

    private Buckets buckets;
    
    private Reservoir slopeReservoir;

    private double runningTotal;
    private int elementCount;
    private int timestep;

    private double storedAverage;

    public DriftCategorizer(double delta, int checkInterval, int bucketSize, int reservoirSize)
    {
	this.DELTA = delta;
	this.checkInterval = checkInterval;
	this.runningTotal = 0.0;
	this.elementCount = 0;
	this.timestep = 0;
	this.storedAverage = 0.0;
	this.buckets = new Buckets(bucketSize);
	this.slopeReservoir = new Reservoir(reservoirSize);
    }

    public void setInput(double inputValue)
    {
	elementCount++;
	timestep++;
	runningTotal += inputValue;

	if (elementCount % checkInterval == 0)
	{
	    if(storedAverage == -1)
	    {
		storedAverage = runningTotal / elementCount;
	    }
	    else
	    {
		double slope = ((runningTotal / elementCount) - storedAverage);

//		System.out.println("here: " + slope);
		buckets.addElement(slope);

		storedAverage = runningTotal / elementCount;
		runningTotal = 0;
		elementCount = 0;
	    }
	}
    }
    
    public void setInputDouble(double inputValue)
    {
	elementCount++;
	
	if(storedAverage == -1) 
	{
	    storedAverage = inputValue;
	}
	else
	{
	    double slope = inputValue - storedAverage;
	    buckets.addElement(slope);

//	    System.out.println("here: " + slope);
	    storedAverage = inputValue;
	}

/*	
	runningTotal = inputValue;

	if (elementCount % checkInterval == 0)
	{
	    if(storedAverage == -1)
	    {
		storedAverage = inputValue;
	    }
	    else
	    {
		double slope = (runningTotal - storedAverage) / checkInterval;

//		System.out.println("here: " + slope);
		buckets.addElement(slope);

		storedAverage = inputValue;
	    }
	}
*/
    }

    public String evaluateDrift()
    {
	String result = "";
//	System.out.println("avg: " + buckets.calculateWeightedAverage());
	slopeReservoir.addElement(Math.abs(buckets.calculateWeightedAverage()));
	
	if(slopeReservoir.isFull())
	{
	    double ZScore = (Math.abs(buckets.calculateWeightedAverage()) - slopeReservoir.getReservoirMean()) / slopeReservoir.getReservoirStdev();
	    double CDF = normCDF(ZScore);
	    
//	    System.out.println(ZScore);
//	    System.out.println(slopeReservoir.getReservoirMean());
//	    System.out.println(CDF);
	    
	    if(CDF <= DELTA)
	    {
		result = timestep + "," + "GRADUAL" + "," + Math.abs(buckets.calculateWeightedAverage());
	    }
	    else if(CDF > DELTA && CDF < 1-DELTA)
	    {
		result = timestep + "," + "MODERATE" + "," + Math.abs(buckets.calculateWeightedAverage());
	    }
	    else if(CDF >= 1-DELTA)
	    {
		result = timestep + "," + "ABRUPT" + "," + Math.abs(buckets.calculateWeightedAverage());
	    }	   
	}
	else
	{
	    result = timestep + ",Need more slope data to analyze drift";
	}
	
	 reset();
	 return result;
    }
    
    public Reservoir getReservoir()
    {
	return this.slopeReservoir;
    }

    public void reset()
    {
	buckets.resetBuckets();
	this.storedAverage = -1;
	this.runningTotal = 0.0;
	this.elementCount = 0;
    }
    
//Source: http://www1.fpl.fs.fed.us/distributions.html
    public static double normCDF(double z) {

	double zabs;
	double p;
	double expntl,pdf;

	final double p0 = 220.2068679123761;
	final double p1 = 221.2135961699311;
	final double p2 = 112.0792914978709;
	final double p3 = 33.91286607838300;
	final double p4 = 6.373962203531650;
	final double p5 = .7003830644436881;
	final double p6 = .3526249659989109E-01;

	final double q0 = 440.4137358247522;
	final double q1 = 793.8265125199484;
	final double q2 = 637.3336333788311;
	final double q3 = 296.5642487796737;
	final double q4 = 86.78073220294608;
	final double q5 = 16.06417757920695;
	final double q6 = 1.755667163182642;
	final double q7 = .8838834764831844E-1;

	final double cutoff = 7.071;
	final double root2pi = 2.506628274631001;

	zabs = Math.abs(z);

	//  |z| > 37

	if (z > 37.0) {

	    p = 1.0;

	    return p;

	}

	if (z < -37.0) {

	    p = 0.0;

	    return p;

	}

	//  |z| <= 37.

	expntl = Math.exp(-.5*zabs*zabs);

	pdf = expntl/root2pi;

	//  |z| < cutoff = 10/sqrt(2).

	if (zabs < cutoff) {

	    p = expntl*((((((p6*zabs + p5)*zabs + p4)*zabs + p3)*zabs +
		    p2)*zabs + p1)*zabs + p0)/(((((((q7*zabs + q6)*zabs +
			    q5)*zabs + q4)*zabs + q3)*zabs + q2)*zabs + q1)*zabs +
			    q0);

	} else {

	    p = pdf/(zabs + 1.0/(zabs + 2.0/(zabs + 3.0/(zabs + 4.0/
		    (zabs + 0.65)))));

	}

	if (z < 0.0) {

	    return p;

	} else {

	    p = 1.0 - p;

	    return p;

	}

    }


}
