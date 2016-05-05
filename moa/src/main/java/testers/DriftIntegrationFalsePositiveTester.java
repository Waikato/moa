package testers;

import java.io.BufferedWriter;
import java.io.FileWriter;

import driftmodelintegration.ModelAggregatorPI;

public class DriftIntegrationFalsePositiveTester implements Tester
{
    int numInst = 500000;
    int numDriftInstances = 5000;
    int iterations = 100;
    int trainInstances = 10000;
    int initInstances = 1000;
    

    @Override
    public void doTest()
    {
	try
	{
	    double[] deltas = {0.05,0.1,0.3};
	    double[] probs = {0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5};
	    double probability = 0.0;
	    double threshold = 1.2;
	    
	    BufferedWriter bWriter = new BufferedWriter(new FileWriter("F:\\DriftStreams\\Results\\EDDTEST_FP_LED.csv"));

	    bWriter.write("mean\\delta,0.05,0.1,0.3");
	    bWriter.newLine();

	    for(int i = 0; i < probs.length; i++)
	    {
		bWriter.write(probs[i] + ",");
		threshold = probs[i];
		
		for (int j = 0; j < deltas.length; j++)
		{	
		    int totalDrift = 0;
		    System.out.println(probs[i] + "," + deltas[j]);
		    
		    for (int k = 1; k <= iterations; k++)
		    {
			System.out.println(k);
			String[] args = {"4", ""+threshold, 
				"F:\\DriftStreams\\LED\\Split\\LED_InterlacedDrift_" + probability + "_" + k + "_" + "p1.arff",
				"F:\\DriftStreams\\LED\\Split\\LED_InterlacedDrift_" + probability + "_" + k + "_" + "p2.arff",
				"F:\\DriftStreams\\LED\\Split\\LED_InterlacedDrift_" + probability + "_" + k + "_" + "p3.arff",
				"F:\\DriftStreams\\LED\\Split\\LED_InterlacedDrift_" + probability + "_" + k + "_" + "p4.arff"};

			ModelAggregatorPI GPI = new ModelAggregatorPI(args, deltas[j]);

			int c = initInstances;

			while(c < trainInstances + initInstances)
			{
			    GPI.Learn();
			    c++;
			}

			while(c < numInst)
			{	
//			    System.out.println(c);
			    if (GPI.checkDrift())
			    {
				totalDrift++;
			    }
			    c++;
			}
			//		    br.close();
			System.out.println(totalDrift);
		    }

		    bWriter.write(totalDrift + ",");

		}
		bWriter.newLine();
	    }
	    bWriter.close();
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
}
