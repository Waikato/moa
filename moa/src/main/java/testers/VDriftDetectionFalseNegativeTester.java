package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import sizeof.agent.SizeOfAgent;
import cutpointdetection.ADWIN;
import cutpointdetection.SingDetector;

public class VDriftDetectionFalseNegativeTester implements Tester
{
    int numDriftInstances = 1000;
    //int numDriftInstances = 500000;
    int iterations = 100;

    private final int NOCORRECTION = 0;
    private final int SINE_FUNC = 1;
    private final int SIGMOID_FUNC = 2;

    @Override
    public void doTest()
    {
	try
	{
	    String[] driftPosition = {"0.25", "0.50", "0.75" };
	    //String[] slopes = { "0.4", "0.6", "0.8" };
	    String[] slopes = {"0.0001","0.0002", "0.0003", "0.0004", "0.0006"};

	    int length = 100000;
	    double[] betas = {0.0,0.1,0.2,0.3,0.4,0.5};

	    double delta = 0.05;    

	    double[] prob = { 0.2 };

	    for (int w = 0; w < driftPosition.length; w++)
	    {
		System.out.println(driftPosition[w]);
		for (int q = 0; q < slopes.length; q++)
		{
		    System.out.println(slopes[q]);
		    
		    //BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\SEED\\FN Test\\"+driftPosition[w]+"\\Abrupt_0.2_"+slopes[q]+"_"+driftPosition[w]+"_" +length+"_sine.csv"));
		    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\SEED\\FN Test\\"+driftPosition[w]+"\\Gradual_"+slopes[q]+"_"+driftPosition[w]+"_" +length+"_sigmoid.csv"));
		    
		    bWriter.write("Beta,Number of Drifts,FN Rate,Avg Delay,Delay Stdev,Avg Time,Time Stdev,Memory Size");
		    bWriter.newLine();
		    for (int j = 0; j < betas.length; j++)
		    {
			int[] delays = new int[iterations];
			int totalDrift = 0;
			long[] times = new long[iterations];
			long totalTime = 0;

			//int totalCompCount = 0;
			//int totalCompChecks = 0;
			//int totalWarningCount = 0;

			//int[] checks = new int[iterations];

			int totalSize = 0;
			for (int k = 0; k < iterations; k++)
			{
			    ADWIN adwin = new ADWIN(delta);
			    SingDetector sing = new SingDetector(0.05,32,1,1,0.01,0.8,75);
			    //BufferedReader br = new BufferedReader(new FileReader("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\FN Test\\"+driftPosition[w]+"\\Abrupt_0.2_"+slopes[q]+"\\"+driftPosition[w]+"_" +length+"_"+slopes[q]+"_" + k + ".csv"));
			    BufferedReader br = new BufferedReader(new FileReader("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\FN Test\\"+driftPosition[w]+"\\Gradual_"+slopes[q]+"\\"+driftPosition[w]+"_" +length+"_"+slopes[q]+"_" + k + ".csv"));
			    
			    double relPos = 0.0;
			    //adwin.setVolatility(Integer.parseInt(driftPosition[j]));
			    sing.setTension(betas[j]);
			    sing.setMode(SIGMOID_FUNC);

			    String line = "";

			    int c = 0;

			    int delay = -1;
			    double prevdrift = 0;
			    double drift = length;

			    while ((line = br.readLine()) != null)
			    {
				relPos = (c - prevdrift) / (drift - prevdrift);

				long startTime = System.currentTimeMillis();
				if (sing.setInput(Double.parseDouble(line), delta, relPos) && c > (int)(length * (Double.parseDouble(driftPosition[w]))))
				{
				    long endTime = System.currentTimeMillis();
				    totalTime = totalTime + (endTime - startTime);
				    times[k] = times[k] + endTime - startTime;
				    delay = c - (int)(length * (Double.parseDouble(driftPosition[w])));
				    delays[k] = delay;
				    totalDrift++;
				    c++;
				    break;
				}
				long endTime = System.currentTimeMillis();
				totalTime = totalTime + (endTime - startTime);
				times[k] = times[k] + endTime - startTime;
				c++;
			    }
			    totalSize += SizeOfAgent.fullSizeOf(sing);
			    br.close();
			}

			bWriter.write(betas[j] + ",");
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
