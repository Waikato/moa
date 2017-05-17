package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class VolatilityPredictingDriftSummary_Random implements Tester
{
    int numDriftInstances = 1000;
    //int numDriftInstances = 500000;
    int iterations = 100;

    @Override
    public void doTest()
    {
	try
	{
	    int[] numInst = { 10000000 };
	    //String[] slopes = { "", "100000", "50000", "10000", "5000", "1000" };
	    //String[] epsilonPrimes = { "0.4", "0.6", "0.8"};
	    String[] epsilonPrimes = { "0.0001", "0.0002", "0.0003", "0.0004", "0.0006"};
	    double[] linearAlphas = {0.0};

	    double delta = 0.05;

	    double[] alphas = linearAlphas;   

	    double[] prob = { 0.2 };

		for (int w = 0; w < alphas.length; w++)
		{
		    for (int q = 0; q < epsilonPrimes.length; q++)
		    {
			for (int i = 0; i < numInst.length; i++)
			{
			    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\Summary\\Rand\\Summary_" + "VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+".csv"));

			    bWriter.write("Slope,Total TP,Delay,Delay Stdev");
			    bWriter.newLine();
				int[] delays = new int[iterations*100];
				int totalDrift = 0;
				int totalFP = 0;
				int totalRealisticFP = 0;

				for (int k = 0; k < iterations; k++)
				{
				    System.out.println(k);
				    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Results\\RandDrift Point\\VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"\\sigmoid"+linearAlphas[w]+"_driftpoints_" + numInst[i] + "_" + k + ".csv"));
				    BufferedReader br2 = new BufferedReader(new FileReader("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\Gradual_"+epsilonPrimes[q]+"\\rand_"+numInst[i]+"_" + epsilonPrimes[q] + "_" + k + "_driftlocations.csv"));
				    
				    ArrayList<Integer> list = new ArrayList<Integer>();
				    String line2 = "";
				    while ((line2 = br2.readLine()) != null)
				    {
					list.add(Integer.parseInt(line2));
				    }
				    list.add(numInst[i]);
				    String line = "";
				    
				    int[] TPs = new int[100];
				    int d = 0;
				    
				    while (d < list.size())
				    {
					if(list.get(d) == numInst[i]){break;}
					while ((line = br.readLine()) != null)
					{
					    int t = Integer.parseInt(line);
					    t -= list.get(d);
					    
					    if(t >= 0 && TPs[d] == 0)
					    {
						TPs[d] = 1;
						totalDrift++;
						delays[k*d] = t;
						d++;
					    }
					}
				    }
				    br.close();
				    br2.close();
				    
				}

				bWriter.write("Rand,");
				bWriter.write(totalDrift + ",");
//				bWriter.write(totalFP + ",");
//				bWriter.write(totalRealisticFP + ",");
//				bWriter.write(totalDrift / (double) iterations + ",");
				bWriter.write(calculateSum(delays) / totalDrift + ",");
				bWriter.write(calculateStdev(delays, calculateSum(delays) / totalDrift) + "");
//				bWriter.write((double) totalTime / iterations + ",");
//				bWriter.write(calculateStdevLong(times, (double) totalTime / iterations) + ",");
//				bWriter.write((double) totalSize / (double) iterations + ",");
				bWriter.newLine();
			    
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
