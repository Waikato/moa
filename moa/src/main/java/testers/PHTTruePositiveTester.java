package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import sizeof.agent.SizeOfAgent;
import cutpointdetection.ADWIN;
import cutpointdetection.CutPointDetector;
import cutpointdetection.PHT;
import cutpointdetection.SingDetector;

public class PHTTruePositiveTester implements Tester
{
    int numDriftInstances = 1000;
    //int numDriftInstances = 500000;
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
	    //int[] numInst = {10000, 50000, 100000, 1000000};
	    //int[] numInst = {100000000, 250000000, 500000000};
	    int[] numInst = { 1000000 };
	    double[] slopes = { 0.0000, 0.0001, 0.0002, 0.0003, 0.0004 };
	    //double[] slopes = { 0.0000 };
	    //double[] epsilonPrimes = { 0.0025, 0.005, 0.0075, 0.01 };
	    double[] epsilonPrimes = { 10, 15, 20, 25, 30, 35, 40, 45, 50 }; //PHT Detection Threshold
	    //double[] linearAlphas = { 0.2, 0.4, 0.6, 0.8 };
	    double[] linearAlphas = { 0.8 };
	    double[] expAlphas = { 0.01, 0.025, 0.05, 0.075, 0.1 };
	    int[] fixedCompressionTerms = { 75 };
	    int[] paretoCompressionTerms = { 200, 400, 600, 800 };

	    int[] blocksizes = { 32, 64, 128, 256 };
	    int blockSize = 32;

	    double delta = 0.05;
	    
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

	    double[] prob = { 0.2 };

	    for (int e = 0; e < compTerms.length; e++)
	    {
		for (int w = 0; w < alphas.length; w++)
		{
		    for (int q = 0; q < epsilonPrimes.length; q++)
		    {
			for (int i = 0; i < numInst.length; i++)
			{
//			    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Volatility&ChangeDetection\\Test\\"+ blockSize + "_"+ DECAY_MODE + "_" + COMPRESSION_MODE + "_" + "L-" + numDriftInstances + "_" + compTerms[e] + "_" + epsilonPrimes[q] + "_" + alphas[w] + "_" + numInst[i] + ".csv"));
//			    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Volatility&ChangeDetection\\Test\\ADWIN_"+ numInst[i] + ".csv"));
			    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Volatility&ChangeDetection\\Test\\PHT_" + epsilonPrimes[q] + "_0.02.csv"));

			    bWriter.write("Slope,Number of Drifts,TP Rate,Avg Delay,Delay Stdev,Avg Time,Time Stdev,Memory Size");
//			    bWriter.write("Slope,Number of Drifts,TP Rate,Avg Delay,Delay Stdev,Avg Time,Time Stdev,Memory Size");
			    bWriter.newLine();
			    for (int j = 0; j < slopes.length; j++)
			    {
				int[] delays = new int[iterations];
				int totalDrift = 0;
				long[] times = new long[iterations];
				long totalTime = 0;

				int totalCompCount = 0;
				int totalCompChecks = 0;
				int totalWarningCount = 0;

				int[] checks = new int[iterations];

				int totalSize = 0;
				for (int k = 0; k < iterations; k++)
				{
				    generateSlopedInput(prob[0], slopes[j], numInst[i], numDriftInstances, k);
				    PHT pht = new PHT(epsilonPrimes[q]);
				    ADWIN adwin = new ADWIN(delta);
				    SingDetector sing = new SingDetector(delta, blockSize, DECAY_MODE, COMPRESSION_MODE, epsilonPrimes[q], alphas[w], compTerms[e]);
				    CutPointDetector detector = sing;
				    
				    int y = k + 1;
				    //BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\DriftStreams\\Hyperplane\\Prequential\\Hyperplane_Prequential_"+ y + ".csv"));
				    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\TPData.txt"));
				    //BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Volatility&ChangeDetection\\VolatilityStreamDriftPoint\\3\\VolatilityStream_3_"+k+".csv"));
				    String line = "";

				    int c = 0;

				    int delay = -1;
				    while ((line = br.readLine()) != null)
				    {
/*					    String[] pred = line.split(",");

						if(pred[0].equals(pred[1]))
						{
						    line = "1";
						}
						else
						{
						    line = "0";
						}
*/					
					long startTime = System.currentTimeMillis();
					if (pht.setInput(Double.parseDouble(line)) && c > (numInst[i] - numDriftInstances))
					{
					    long endTime = System.currentTimeMillis();
					    totalTime = totalTime + (endTime - startTime);
					    times[k] = times[k] + endTime - startTime;
					    delay = c - (numInst[i] - numDriftInstances);
					    delays[k] = delay;
					    totalDrift++;
//					    totalCompCount += sing.window.compCount;
//					    totalCompChecks += sing.window.compChecks;
//					    totalWarningCount += sing.warningCount;
//					    checks[k] = detector.getChecks();
					    break;
					}
					long endTime = System.currentTimeMillis();
					totalTime = totalTime + (endTime - startTime);
					times[k] = times[k] + endTime - startTime;
//					checks[k] = detector.getChecks();
					c++;
				    }
				    totalSize += SizeOfAgent.fullSizeOf(pht);
				    br.close();
				}

				bWriter.write(slopes[j] + ",");
				bWriter.write(totalDrift + ",");
				bWriter.write(totalDrift / (double) iterations + ",");
				bWriter.write(calculateSum(delays) / totalDrift + ",");
				bWriter.write(calculateStdev(delays, calculateSum(delays) / totalDrift) + ",");
				bWriter.write((double) totalTime / iterations + ",");
				bWriter.write(calculateStdevLong(times, (double) totalTime / iterations) + ",");
//				bWriter.write((double) totalCompCount / totalCompChecks + ",");
//				bWriter.write((double) totalCompCount / iterations + ",");
//				bWriter.write(calculateSum(checks) / iterations + ",");
//				bWriter.write(calculateStdev(checks, calculateSum(checks) / iterations) + ",");
				bWriter.write((double) totalSize / (double) iterations + ",");
				// bWriter.write((double)totalWarningCount / (double)iterations + "");
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

    public void generateSlopedInput(double driftProb, double slope, int numInstances, int numDriftInstances, int randomSeed)
    {
	try
	{

	    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\TPData.txt"));

	    double[] driftMean = new double[1];
	    driftMean[0] = driftProb;
	    // System.out.println(driftMean[0]);
	    BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(driftMean, numInstances - numDriftInstances, randomSeed);
	    while (gen.hasNextTransaction())
	    {
		bWriter.write(gen.getNextTransaction() + "\n");
	    }

	    BernoulliDistributionGenerator genDrift = new BernoulliDistributionGenerator(driftMean, numDriftInstances, randomSeed);
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
