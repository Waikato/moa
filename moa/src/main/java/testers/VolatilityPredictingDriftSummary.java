package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class VolatilityPredictingDriftSummary implements Tester
{
    int numDriftInstances = 1000;
    //int numDriftInstances = 500000;
    int iterations = 100;

    @Override
    public void doTest()
    {
	try
	{
	    int[] numInst = { 1000000 };
	    String[] slopes = { "", "100000", "50000", "10000", "5000", "1000" };
	    //String[] epsilonPrimes = { "0.4", "0.6", "0.8"};
	    String[] epsilonPrimes = { "0.0001", "0.0002", "0.0003", "0.0004"};
	    double[] linearAlphas = {0.1,0.2,0.3,0.4,0.5};

	    double delta = 0.05;

	    double[] alphas = linearAlphas;   

	    double[] prob = { 0.2 };

		for (int w = 0; w < alphas.length; w++)
		{
		    for (int q = 0; q < epsilonPrimes.length; q++)
		    {
			for (int i = 0; i < numInst.length; i++)
			{
			    //BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\Summary\\Summary_5%_" + "VaryingMagnitude_abrupt_0.2_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+".csv"));
			    //BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\Summary\\Summary_5%_" + "VaryingMagnitude_abrupt_0.2_"+epsilonPrimes[q]+"_nocorrection.csv"));
			    //BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\Summary\\Summary_5%_" + "VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_sine"+linearAlphas[w]+".csv"));
			    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\Summary\\Summary_5%_" + "VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_nocorrection.csv"));

			    bWriter.write("Slope,Total TP,Total FP,Total Realistic FP,Delay,Delay Stdev");
			    bWriter.newLine();
			    for (int j = 1; j < slopes.length; j++)
			    {
				int[] delays = new int[iterations*100];
				int totalDrift = 0;
				int totalFP = 0;
				int totalRealisticFP = 0;

				for (int k = 0; k < iterations; k++)
				{
				    //BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_abrupt_0.2_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"\\driftpoints_" + slopes[j] + "_" + k + ".csv"));
				    //BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_abrupt_0.2_"+epsilonPrimes[q]+"_nocorrection\\driftpoints_" + slopes[j] + "_" + k + ".csv"));
				    //BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_sine"+linearAlphas[w]+"\\driftpoints_" + slopes[j] + "_" + k + ".csv"));
				    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_nocorrection\\driftpoints_" + slopes[j] + "_" + k + ".csv"));

				    String line = "";
				    int[] TPs = new int[100];
				    int interval = Integer.parseInt(slopes[j]);
				    while ((line = br.readLine()) != null)
				    {
					int t = Integer.parseInt(line);
					//gradual
					t -= interval;
					if(t < 0) { continue; }
					//
					int index = t / (interval+1000);
										
					if(TPs[index] == 0)
					{
					    TPs[index] = 1;
					    totalDrift++;
					    delays[k*index] = t - ((interval+1000) * index); // gradual
					    //delays[k*index] = t - (interval * index); // abrupt
					}
					else
					{
					    totalFP++;
					    if(t - ((interval+1000) * index) > 1000) // gradual
					    //if(t - (interval * index) > (interval * 0.10)) // abrupt
					    //{
						totalRealisticFP++;
					    //}
					}
				    }

				    br.close();
				    
				}

				bWriter.write(slopes[j] + ",");
				bWriter.write(totalDrift + ",");
				bWriter.write(totalFP + ",");
				bWriter.write(totalRealisticFP + ",");
//				bWriter.write(totalDrift / (double) iterations + ",");
				bWriter.write(calculateSum(delays) / totalDrift + ",");
				bWriter.write(calculateStdev(delays, calculateSum(delays) / totalDrift) + "");
//				bWriter.write((double) totalTime / iterations + ",");
//				bWriter.write(calculateStdevLong(times, (double) totalTime / iterations) + ",");
//				bWriter.write((double) totalSize / (double) iterations + ",");
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
