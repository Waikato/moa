package inputstream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class DriftCategorizationStreamGenerator
{ 
    public class DriftPools
    {
	private int seed;
	private Random RNG;
	private double[] gradual, moderate, abrupt;
	private double[] poolWeights;
	private String label;
	
	// variables for gaussian distribution
	private double mean;
	private double stdev;
	
	// variables for binomial distribution
	private int n;
	private double p;
	private BinomialRNG binomialRNG;
	
	private int[] test;
	
	private ArrayList<Slope> updatedList;
	
	public DriftPools(int seed, double gLeftBound, double gRightBound, double mLeftBound, double mRightBound, double aLeftBound, double aRightBound)
	{
	    this.seed = seed;
	    RNG = new Random(seed);
	    
	    double[] gradual = {gLeftBound, gRightBound};
	    this.gradual = gradual;
	    
	    double[] moderate = {mLeftBound, mRightBound};
	    this.moderate = moderate;
	    
	    double[] abrupt = {aLeftBound, aRightBound};
	    this.abrupt = abrupt;
	    
	    double[] weights = {0.2, 0.6, 0.2};
	    this.poolWeights = weights;
	    
	    this.updatedList = new ArrayList<Slope>();
	    
	    this.test = new int[3];
	}
	
	public DriftPools(int seed, double mean, double stdev)
	{
	    this.seed = seed;
	    RNG = new Random(seed);
	    
	    this.mean = mean;
	    this.stdev = stdev;
	    
	    double[] weights = {0.2, 0.6, 0.2};
	    this.poolWeights = weights;
	    
	    this.updatedList = new ArrayList<Slope>();
	    
	    this.test = new int[3];
	}
	
	public DriftPools(int seed, int n, double p)
	{
	    this.seed = seed;
	    RNG = new Random(seed);
	    
	    this.n = n;
	    this.p = p;
	    this.binomialRNG = new BinomialRNG(n, p);
	    
	    double[] weights = {0.45, 0.3, 0.25};
	    this.poolWeights = weights;
	    
	    this.updatedList = new ArrayList<Slope>();
	    
	    this.test = new int[3];
	}
	
	public double selectBinomialSlope()
	{
	    double slope = 0.0;
	    double binomial = (double)binomialRNG.nextBinomial();
		    
	    binomial = binomial == 0 ? 1 : binomial;
	    binomial = binomial == n ? n-1 : binomial;
		    
	    slope = binomial / n;
//	    System.out.println(binomialRNG.nextBinomial());
	    label = getSlopeLabel(slope);
	    
	    if(label.equals("GRADUAL"))
	    {
		this.test[0]++;
	    }
	    else if(label.equals("MODERATE"))
	    {
		this.test[1]++;
	    }
	    else if(label.equals("ABRUPT"))
	    {
		this.test[2]++;
	    }
	    
	    return slope;
	}
	
	public double selectGaussianSlope()
	{
	    double slope = 0.0;
	    slope = RNG.nextGaussian() * stdev + mean;
	    
	    slope = slope < 0 ? 0 : slope;
	    slope = slope > 1 ? 1 : slope;
	    
	    label = getSlopeLabel(slope);
	    
	    if(label.equals("GRADUAL"))
	    {
		this.test[0]++;
	    }
	    else if(label.equals("MODERATE"))
	    {
		this.test[1]++;
	    }
	    else if(label.equals("ABRUPT"))
	    {
		this.test[2]++;
	    }
	    
	    return slope;
	}
	
	public double selectSlope()
	{
	    double randPool = Math.random();//RNG.nextDouble();
	    double[] selectedPool;
	    double slope = 0.0;
	    
//	    System.out.println(randPool + " " + poolWeights[0] + " " + poolWeights[1] + " " + poolWeights[2]);
	    
	    if(randPool <= poolWeights[0])
	    {
		selectedPool = gradual;
		label = "GRADUAL";
		this.test[0]++;
	    }
	    else if(randPool <= poolWeights[1]+poolWeights[0])
	    {
		selectedPool = moderate;
		label = "MODERATE";
		this.test[1]++;
	    }
	    else
	    {
		selectedPool = abrupt;
		label = "ABRUPT";
		this.test[2]++;
	    }
	    
	    slope = (RNG.nextDouble() * (selectedPool[1] - selectedPool[0])) + selectedPool[0];
	    
//	    System.out.println(slope);
	    
	    return slope;
	}
	
	public String getSlopeLabel(double slope)
	{
	    slope = Math.abs(slope);
	    
	    if(slope <= poolWeights[0])
	    {
		return "GRADUAL";
	    }
	    else if(slope < poolWeights[0] + poolWeights[1])
	    {
		return "MODERATE";
	    }
	    else if(slope <= poolWeights[0] + poolWeights[1] + poolWeights[2])
	    {
		return "ABRUPT";
	    }
	    
	    return "UNKNOWN";
	}
	
	public String getLabel()
	{
	    return label;
	}
	
	public int[] getTest()
	{
	    return test;
	}
	
	public void addToList(Slope value)
	{
	    updatedList.add(value);
	}
	
	public void removeFromList(int pos)
	{
	    updatedList.remove(pos);
	}
	
	public ArrayList<Slope> getList()
	{
	    return this.updatedList;
	}
	
	public int listSize()
	{
	    return this.updatedList.size();
	}
    }
    
    public class Slope
    {
	private double slope;
	private String label;
	
	public Slope(double slope, String label)
	{
	    this.slope = slope;
	    this.label = label;
	}
	
	public double getSlope()
	{
	    return slope;
	}
	
	public String getLabel()
	{
	    return label;
	}
    }
    
    private DriftPools pool;
    private BufferedWriter bWriter, w2, bWriterSlope;
    private double startingMean;
    private int numDrifts;
    private int seed;
    private double noisePercent;
    private double perturbationVariable;
    private double marginOfError;
    private int timeInterval;
    
    private final int UNIFORM = 0;
    private final int GAUSSIAN = 1;
    private final int POISSON = 2;
    private final int BINOMIAL = 3;
    private int MODE = 0;
    
    private int timestep = 0;
    
    public DriftCategorizationStreamGenerator(BufferedWriter bWriter, BufferedWriter bWriterSlope, double startingMean, int numDrifts, int seed, double noisePercent, double perturbationVariable, double gLeftBound, double gRightBound, double mLeftBound, double mRightBound, double aLeftBound, double aRightBound) throws IOException
    {
	this.pool = new DriftPools(seed, gLeftBound, gRightBound, mLeftBound, mRightBound, aLeftBound, aRightBound);
	this.bWriter = bWriter;
	this.bWriterSlope = bWriterSlope;
	this.startingMean = startingMean;
	this.numDrifts = numDrifts;
	this.seed = seed;
	this.noisePercent = noisePercent;
	this.perturbationVariable = perturbationVariable;
    }
    
    public DriftCategorizationStreamGenerator(BufferedWriter bWriter, BufferedWriter bWriterSlope, double startingMean, int numDrifts, int seed, double noisePercent, double perturbationVariable, double marginOfError, int timeInterval, double gLeftBound, double gRightBound, double mLeftBound, double mRightBound, double aLeftBound, double aRightBound) throws IOException
    {
	this.pool = new DriftPools(seed, gLeftBound, gRightBound, mLeftBound, mRightBound, aLeftBound, aRightBound);
	this.bWriter = bWriter;
	this.bWriterSlope = bWriterSlope;
	this.startingMean = startingMean;
	this.numDrifts = numDrifts;
	this.seed = seed;
	this.noisePercent = noisePercent;
	this.perturbationVariable = perturbationVariable;
	this.marginOfError = marginOfError;
	this.timeInterval = timeInterval;
    }
    
    public DriftCategorizationStreamGenerator(int mode, BufferedWriter bWriter, BufferedWriter bWriterSlope, double startingMean, int numDrifts, int seed, double noisePercent, double perturbationVariable, double marginOfError, int timeInterval, double gaussianMean, double gaussianStdev) throws IOException
    {
	this.MODE = mode;
	this.pool = new DriftPools(seed, gaussianMean, gaussianStdev);
	this.bWriter = bWriter;
	this.bWriterSlope = bWriterSlope;
	this.startingMean = startingMean;
	this.numDrifts = numDrifts;
	this.seed = seed;
	this.noisePercent = noisePercent;
	this.perturbationVariable = perturbationVariable;
	this.marginOfError = marginOfError;
	this.timeInterval = timeInterval;
    }
    
    public DriftCategorizationStreamGenerator(int mode, BufferedWriter bWriter, BufferedWriter bWriterSlope, double startingMean, int numDrifts, int seed, double noisePercent, double perturbationVariable, double marginOfError, int timeInterval, int binomialN, double binomialP) throws IOException
    {
	this.MODE = mode;
	this.pool = new DriftPools(seed, binomialN, binomialP);
	this.bWriter = bWriter;
	this.bWriterSlope = bWriterSlope;
	this.startingMean = startingMean;
	this.numDrifts = numDrifts;
	this.seed = seed;
	this.noisePercent = noisePercent;
	this.perturbationVariable = perturbationVariable;
	this.marginOfError = marginOfError;
	this.timeInterval = timeInterval;
    }
    
    public void generateInput()
    {
	try
	{
	    Random RNG = new Random(1);
	    double sign = RNG.nextDouble() > 0.5 ? 1.0 : -1.0;
	    double currentMean = startingMean;
	    
	    for (int i = 0; i < numDrifts; i++)
	    {
		double slope = 0.0;
		String label = "";
//		int timeFrame = (int) (RNG.nextDouble() * 1000) + 100;	
		int timeFrame = timeInterval;	
		double magnitude = slope * timeFrame;

		boolean boundTest = false;
		
		int j = pool.listSize();

		while (!boundTest)
		{
		    if(j > 0)
		    {
			slope = pool.getList().get(j-1).getSlope();
			label = pool.getList().get(j-1).getLabel();
		    }
		    else
		    {
			if(MODE == UNIFORM)
			{
			    slope = pool.selectSlope();
			    label = pool.getLabel();
			}
			else if(MODE == GAUSSIAN)
			{
			    slope = pool.selectGaussianSlope()/this.timeInterval;
			    label = pool.getLabel();
			}
			else if(MODE == BINOMIAL)
			{
			    slope = pool.selectBinomialSlope()/this.timeInterval;
			    label = pool.getLabel();
			}
		    }
		    
//		    timeFrame = (int) (RNG.nextDouble() * 1000) + 100;
		    timeFrame = timeInterval;
		    magnitude = slope * timeFrame;

//		    System.out.println(currentMean + " " + slope + " " + label);
		    
		    double condition = currentMean + (sign * magnitude);

		    if (condition <= 1.0 && condition >= 0.0)
		    {
			if(j > 0)
			{
			    pool.removeFromList(j-1);
			}
			boundTest = true;
			break;
		    } 
		    else
		    {
			sign *= -1.0;
		    }

		    condition = currentMean + (sign * magnitude);

		    if (condition <= 1.0 && condition >= 0.0)
		    {
			if(j > 0)
			{
			    pool.removeFromList(j-1);
			}
			boundTest = true;
			break;
		    }
		    
		    if(j <= 0)
		    {
			pool.addToList(new Slope(slope, label));
		    }
		    j--;
		    
		}
		
//		System.out.println("Drift Created");
//		System.out.println(timestep + "," + label + ","+slope);
//		System.out.println(addNoise(label));
		bWriterSlope.write(timestep + "," + addNoise(label) + ","+slope +"\n");
//		currentMean = generateSingleDrift(currentMean, sign * slope, timeFrame, seed);
		currentMean = generateGuaranteedSingleDrift(currentMean, sign * slope, timeFrame, seed, marginOfError);

	    }

	    for(int i: pool.getTest())
	    {
		System.out.println(i);;
	    }

	    bWriter.close();

	} catch (Exception e)
	{
	    System.err.println("error");
	    e.printStackTrace();
	}
    }
    
    public String addNoise(String label)
    {
	if(Math.random() < noisePercent)
	{
	    double randPool = Math.random();
	    if(label.equals("GRADUAL"))
	    {
		label = randPool >= 0.5 ? "MODERATE" : "ABRUPT";
	    }
	    else if(label.equals("MODERATE"))
	    {
		label = randPool >= 0.5 ? "GRADUAL" : "ABRUPT";
	    }
	    else if(label.equals("ABRUPT"))
	    {
		label = randPool >= 0.5 ? "GRADUAL" : "MODERATE";
	    }	    
	}	
	return label;
    }
    
    private double generateSingleDrift(double mean, double slope, int numInstances, int randomSeed)
    {
	double[] driftMean = new double[1];
	try
	{
	    driftMean[0] = mean;

	    Random RNG = new Random(1);

	    BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(
		    driftMean, numInstances, randomSeed);
	    
	    while (gen.hasNextTransaction())
	    {
		double rand = RNG.nextGaussian();

		driftMean[0] += (slope + (perturbationVariable * slope * rand)); //Gaussian Perturbation
		// driftMean[0] += slope;
//		w2.write(driftMean[0] + "\n");

		gen.setMeans(driftMean);
		bWriter.write(gen.getNextTransaction() + "\n");
		
		timestep++;
	    }

	    driftMean = gen.getMean();
	    
	} catch (Exception e)
	{
	    System.err.println("error");
	    e.printStackTrace();
	}

	return driftMean[0];

    }
    
    private double generateGuaranteedSingleDrift(double mean, double slope, int numInstances, int randomSeed, double marginOfError)
    {
	double[] driftMean = new double[1];
	double[] temp = new double[numInstances];
	int count = 0;
	boolean guaranteed = false;
	
	BernoulliDistributionGenerator gen;
	
	try
	{
	    while(guaranteed == false)
	    {
		driftMean[0] = mean;

		Random RNG = new Random(randomSeed++);

		gen = new BernoulliDistributionGenerator(driftMean, numInstances, randomSeed);

		while (gen.hasNextTransaction())
		{
		    double rand = RNG.nextGaussian();

		    driftMean[0] += (slope + (perturbationVariable * slope * rand)); //Gaussian Perturbation
		    gen.setMeans(driftMean);
		    
		    temp[count++] = Double.parseDouble(gen.getNextTransaction());
		}
		
		double left = 0;
		double right = 0;
		
		for(int i=0; i<(timeInterval/10); i++)
		{
		    left += temp[i];
		}
		for(int j=numInstances-(timeInterval/10); j<numInstances; j++)
		{
		    right += temp[j];
		}
		
		double estimatedSlope = (right - left) / (timeInterval/10);
//		System.out.println(estimatedSlope + "," + (slope*numInstances + marginOfError) + "," + (slope*numInstances - marginOfError));
		if((estimatedSlope <= (slope * numInstances + marginOfError)) && (estimatedSlope >= (slope * numInstances - marginOfError)))
		{
		    guaranteed = true;
		}
//		System.out.println(guaranteed);
		driftMean = gen.getMean();
		count = 0;
	    }
	    
	    for(double d: temp)
	    {
		bWriter.write("" + d + "\n");
		timestep++;
	    }
	    
	} catch (Exception e)
	{
	    System.err.println("error");
	}

	return driftMean[0];
    }
}
