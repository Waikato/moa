package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import sizeof.agent.SizeOfAgent;
import volatilityevaluation.RelativeVolatilityDetector;
import cutpointdetection.ADWIN;
import cutpointdetection.CutPointDetector;
import cutpointdetection.SingDetector;

public class VolatilityTruePositiveTester implements Tester
{
    // int numDriftInstances = 1000;
//    int numDriftInstances = 500000;
    int iterations = 30;

    private final int LINEAR_DECAY = 1;
    private final int EXPONENTIAL_DECAY = 2;
    private final int FIXED_TERM = 1;
    private final int PARETO = 2;

    private int DECAY_MODE = LINEAR_DECAY;
    private int COMPRESSION_MODE = FIXED_TERM;

    @Override
    public void doTest()
    {
	try
	{
	    double[] epsilonPrimes = { 0.01 };
	    double[] linearAlphas = { 0.8 };
	    double[] expAlphas = { 0.01 };
	    int[] fixedCompressionTerms = { 75 };
	    int[] paretoCompressionTerms = { 200, 400, 600, 800 };

	    double[] alphas = null;
	    int[] compTerms = null;

	    if (DECAY_MODE == LINEAR_DECAY)
	    {
		alphas = linearAlphas;
	    } 
	    else if (DECAY_MODE == EXPONENTIAL_DECAY)
	    {
		alphas = expAlphas;
	    }

	    if (COMPRESSION_MODE == FIXED_TERM)
	    {
		compTerms = fixedCompressionTerms;
	    } 
	    else if (COMPRESSION_MODE == PARETO)
	    {
		compTerms = paretoCompressionTerms;
	    }

	    int[] blocksizes = { 32, 64, 128, 256 };
	    String[] levels = { "", "1", "2", "3", "4", "5", "6", "7" };
//	    String[] levels = { "", "7" };
	    int[] driftpoint = { 0, 100000, 50000, 10000, 5000, 1000, 500, 100 };

	    for (int j = 1; j < levels.length; j++)
	    {
		int level = j;
		int trueDriftPoint = driftpoint[level] * 100;

		double delta = 0.05;

		int blockSize = 32;

		for (int e = 0; e < compTerms.length; e++)
		{
		    for (int w = 0; w < alphas.length; w++)
		    {
			for (int q = 0; q < epsilonPrimes.length; q++)
			{
			    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Volatility&ChangeDetection\\Results\\VolatilityTest\\ADWIN\\seedtttest_" + levels[level] + "_10%.csv"));
			    bWriter.write("Level,Number of Drifts,TP Rate,Avg Delay,Delay Stdev,Avg Time,Time Stdev,Memory Size");
			    bWriter.newLine();

			    for (int p = 1; p < levels.length; p++)
			    {
				int totalSize = 0;
				if (p == level)
				{
				    continue;
				}
				
				bWriter.write(p + ",");
				
				int[] delays = new int[iterations];
				int totalDrift = 0;
				long[] times = new long[iterations];
				long totalTime = 0;

				for (int k = 0; k < iterations; k++)
				{
				    ADWIN adwin = new ADWIN(delta);
				    SingDetector sing = new SingDetector(delta,
					    blockSize, DECAY_MODE,
					    COMPRESSION_MODE, epsilonPrimes[q],
					    alphas[w], compTerms[e]);

				    CutPointDetector detector = new ADWIN(delta);
				    RelativeVolatilityDetector rv = new RelativeVolatilityDetector(adwin, 32, 0.10);
//				    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Volatility&ChangeDetection\\test.csv"));

				    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Volatility&ChangeDetection\\VolatilityStream\\"
					    						+ levels[level] + "_to_" + levels[p] 
					    						+ "\\VolatilityStream_" + level + "_to_" + p + "_" + k + ".csv"));

				    String line = "";

				    int c = 0;
				    int delay = -1;
				    int timestamp = 0;
				    int dCount = 0;				    
				    
				    while ((line = br.readLine()) != null)
				    {
//					System.out.println(c + " " + line);
					long startTime = System.currentTimeMillis();
//					String[] pred = line.split(",");

//					if(pred[0].equals(pred[1]))
//					{
//					    line = "1";
//					}
//					else
//					{
//					    line = "0";
//					}
					
					if (detector.setInput(Double.parseDouble(line)))
					{
//					    System.out.println("tt");
					    if (c > trueDriftPoint)
					    {
					    	dCount++;
					    	System.out.println(p + "," + k + "," + c);
					    }
					}					    
					if (rv.setInput(Double.parseDouble(line)) && c > trueDriftPoint)
					{
//					    System.out.println("here: " + line);
					    long endTime = System.currentTimeMillis();
					    totalTime = totalTime + (endTime - startTime);
					    times[k] = times[k] + endTime - startTime;
					    delay = dCount;
					    delays[k] = delay;
					    dCount = 0;
					    totalDrift++;

					    break;
					}

//					    timestamp = 0;
//					}
					long endTime = System.currentTimeMillis();
					totalTime = totalTime + (endTime - startTime);
					times[k] = times[k] + endTime - startTime;
					
					c++;
//					timestamp++;
				    }
				    totalSize += (SizeOfAgent.fullSizeOf(rv) - SizeOfAgent.fullSizeOf(adwin));
				    br.close();
				}
//				bWriter.write(p + ",");
				bWriter.write(totalDrift + ",");
				bWriter.write(totalDrift / (double) iterations + ",");
				bWriter.write(calculateSum(delays) / totalDrift + ",");
				bWriter.write(calculateStdev(delays, calculateSum(delays) / totalDrift) + ",");
				bWriter.write((double) totalTime / iterations + ",");
				bWriter.write(calculateStdevLong(times, (double) totalTime / iterations) + ",");
				bWriter.write((double) totalSize / (double) iterations + ",");
				bWriter.newLine();
			    }

			    bWriter.close();
			}
		    }
		}
	    }
	} catch (Exception e)
	{
	    e.printStackTrace();
	}

    }

    public double calculateStdev(int[] times, double mean)
    {
	double sum = 0;
	int count = 0;
	for (int i : times)
	{
	    if (i > 0)
	    {
		count++;
		sum += Math.pow(i - mean, 2);
	    }
	}
	return Math.sqrt(sum / count);
    }

    public double calculateStdevLong(long[] times, double mean)
    {
	double sum = 0;
	int count = 0;
	for (Long i : times)
	{
	    if (i > 0)
	    {
		count++;
		sum += Math.pow(i - mean, 2);
	    }
	}
	return Math.sqrt(sum / count);
    }

    public double calculateSum(int[] delays)
    {
	double sum = 0.0;
	for (double d : delays)
	{
	    sum += d;
	}

	return sum;
    }

    public void generateSlopedInput(double driftProb, double slope,
	    int numInstances, int numDriftInstances, int randomSeed)
    {
	try
	{

	    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
		    "src\\testers\\TPData.txt"));

	    double[] driftMean = new double[1];
	    driftMean[0] = driftProb;
	    // System.out.println(driftMean[0]);
	    BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(
		    driftMean, numInstances - numDriftInstances, randomSeed);
	    while (gen.hasNextTransaction())
	    {
		bWriter.write(gen.getNextTransaction() + "\n");
	    }

	    BernoulliDistributionGenerator genDrift = new BernoulliDistributionGenerator(
		    driftMean, numDriftInstances, randomSeed);
	    while (genDrift.hasNextTransaction())
	    {
		driftMean[0] += slope;
		if (driftMean[0] >= 1.0)
		{
		    driftMean[0] = 1.0;
		}
		// System.out.println(driftMean[0]);
		genDrift.setMeans(driftMean);
		bWriter.write(genDrift.getNextTransaction() + "\n");
	    }

	    bWriter.close();

	} catch (Exception e)
	{
	    System.err.println("error");
	}
    }

    /*
     * public void generateInput(double[] driftProb, double driftIncrement, int
     * numInstances, int numDriftInstances, int randomSeed) { try {
     * 
     * BufferedWriter bWriter = new BufferedWriter(new
     * FileWriter("src\\testers\\TPData.txt"));
     * 
     * BernoulliDistributionGenerator gen = new
     * BernoulliDistributionGenerator(driftProb, numInstances -
     * numDriftInstances, randomSeed); while(gen.hasNextTransaction()) {
     * bWriter.write(gen.getNextTransaction() + "\n"); }
     * 
     * driftProb[0] += driftIncrement;
     * 
     * BernoulliDistributionGenerator genDrift = new
     * BernoulliDistributionGenerator(driftProb, numDriftInstances, randomSeed);
     * while(genDrift.hasNextTransaction()) {
     * bWriter.write(genDrift.getNextTransaction() + "\n"); }
     * 
     * driftProb[0] -= driftIncrement;
     * 
     * bWriter.close();
     * 
     * } catch(Exception e) { System.err.println("error"); } }
     */
}
