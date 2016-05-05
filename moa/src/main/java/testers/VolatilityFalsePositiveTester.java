package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import sizeof.agent.SizeOfAgent;
import volatilityevaluation.RelativeVolatilityDetector;
import cutpointdetection.ADWIN;
import cutpointdetection.SingDetector;

public class VolatilityFalsePositiveTester implements Tester
{
    int iterations = 100;

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
	    double[] epsilonPrimes = { 0.0075 };
	    double[] linearAlphas = { 0.6 };
	    double[] expAlphas = { 0.01, 0.025, 0.05, 0.075, 0.1 };
	    int[] fixedCompressionTerms = { 75 };
	    int[] paretoCompressionTerms = { 200, 400, 600, 800 };

	    double[] alphas = null;
	    int[] compTerms = null;

	    if (DECAY_MODE == LINEAR_DECAY)
	    {
		alphas = linearAlphas;
	    } else if (DECAY_MODE == EXPONENTIAL_DECAY)
	    {
		alphas = expAlphas;
	    }

	    if (COMPRESSION_MODE == FIXED_TERM)
	    {
		compTerms = fixedCompressionTerms;
	    } else if (COMPRESSION_MODE == PARETO)
	    {
		compTerms = paretoCompressionTerms;
	    }

	    int[] blocksizes = { 32, 64, 128, 256 };
	    String[] levels = { "", "1", "2", "3", "4", "5", "6", "7" };
	    int[] driftpoint = { 0, 100000, 50000, 10000, 5000, 1000, 500, 100 };

	    int level = 3;
	    int trueDriftPoint = driftpoint[level] * 100;

	    double delta = 0.05;

	    int blockSize = 32;

	    for (int e = 0; e < compTerms.length; e++)
	    {
		for (int w = 0; w < alphas.length; w++)
		{
		    for (int q = 0; q < epsilonPrimes.length; q++)
		    {
			BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Volatility&ChangeDetection\\Results\\VolatilityTest\\RVar_FalsePositives_TPOnly_20%test.csv"));
			bWriter.write("Level,Number of Drifts,FP Rate,Avg Time,Time Stdev,Memory Size");
			bWriter.newLine();

			int totalSize = 0;
			for (int p = 1; p < levels.length; p++)
			{
			    int totalDrift = 0;
			    long[] times = new long[iterations];
			    long totalTime = 0;

			    for (int k = 0; k < iterations; k++)
			    {
				ADWIN adwin = new ADWIN(delta);
				SingDetector sing = new SingDetector(delta,blockSize, DECAY_MODE, COMPRESSION_MODE, epsilonPrimes[q], alphas[w], compTerms[e]);
				RelativeVolatilityDetector rv = new RelativeVolatilityDetector(sing, 32, 0.20);

				// BufferedReader br = new BufferedReader(new
				// FileReader("src\\testers\\VolatilityStreamDriftPoint\\"
				// + p +"\\VolatilityStream_" + p + "_" + k +
				// ".csv"));
				BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Volatility&ChangeDetection\\FPRemoved\\" + p + "\\VolatilityStream_" + p + "_" + k + ".csv"));
				String line = "";

				long startTime = System.currentTimeMillis();
				br.readLine();
				br.readLine();
				br.readLine();
				br.readLine();
				while ((line = br.readLine()) != null)
				{

				    if (rv.setInputVarTP(Double.parseDouble(line)))
				    {
					totalDrift++;
					// break;
				    }

				}
				long endTime = System.currentTimeMillis();
				totalTime = totalTime + (endTime - startTime);
				times[k] = times[k] + endTime - startTime;
				totalSize += (SizeOfAgent.fullSizeOf(rv) - SizeOfAgent.fullSizeOf(sing));
				br.close();
			    }
			    bWriter.write(p + ",");
			    bWriter.write(totalDrift + ",");
			    bWriter.write(totalDrift / (double) iterations + ",");
			    bWriter.write((double) totalTime / iterations + ",");
			    bWriter.write(calculateStdevLong(times,(double) totalTime / iterations) + ",");
			    bWriter.write((double) totalSize / (double) iterations + ",");
			    bWriter.newLine();
			}

			bWriter.close();
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
