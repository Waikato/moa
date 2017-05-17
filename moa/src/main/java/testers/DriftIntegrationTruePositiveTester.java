package testers;

import java.io.BufferedWriter;
import java.io.FileWriter;

import sizeof.agent.SizeOfAgent;
import driftmodelintegration.ModelAggregatorPI;

public class DriftIntegrationTruePositiveTester implements Tester
{
    int numInst = 500000;
    int numDriftInstances = 5000;
    int iterations = 100;
    int trainInstances = 10000;
    int initInstances = 1000;
    
    double threshold = 1.3;

    @Override
    public void doTest()
    {
	try
	{
	    double delta = 0.05;
	    double[] probs = {0.0, 0.2, 0.4, 0.6, 0.8, 1.0};

	    BufferedWriter bWriter = new BufferedWriter(new FileWriter("F:\\DriftStreams\\Results\\EDD_LED_" + threshold + ".csv"));

	    bWriter.write("Slope,Number of Drifts,TP Rate,Avg Delay,Delay Stdev,Avg InitTime,InitTime Stdev,Avg LearnTime,LearnTime Stdev,Avg Time,Time Stdev,Memory Size");
	    //			    bWriter.write("Slope,Number of Drifts,TP Rate,Avg Delay,Delay Stdev,Avg Time,Time Stdev,Memory Size");
	    bWriter.newLine();
	    for (int j = 0; j < probs.length; j++)
	    {
		int[] delays = new int[iterations];
		int totalDrift = 0;
		long[] times = new long[iterations];
		long totalTime = 0;
		long[] inittimes = new long[iterations];
		long totalinitTime = 0;
		long[] learntimes = new long[iterations];
		long totallearnTime = 0;
		int totalSize = 0;
		
		double probability = probs[j];
		
		for (int k = 1; k <= iterations; k++)
		{
		    System.out.println(k);
		    String[] args = {"4", ""+threshold, 
			    "F:\\DriftStreams\\LED\\Split\\LED_InterlacedDrift_" + probability + "_" + k + "_" + "p1.arff",
			    "F:\\DriftStreams\\LED\\Split\\LED_InterlacedDrift_" + probability + "_" + k + "_" + "p2.arff",
			    "F:\\DriftStreams\\LED\\Split\\LED_InterlacedDrift_" + probability + "_" + k + "_" + "p3.arff",
			    "F:\\DriftStreams\\LED\\Split\\LED_InterlacedDrift_" + probability + "_" + k + "_" + "p4.arff"};
		    
		    long startinitTime = System.currentTimeMillis();
		    ModelAggregatorPI GPI = new ModelAggregatorPI(args, 0.05);
		    long endinitTime = System.currentTimeMillis();
		    totalinitTime = totalinitTime + (endinitTime - startinitTime);
		    inittimes[k-1] = inittimes[k-1] + endinitTime - startinitTime;
		    
		    int c = initInstances;
		    long startlearnTime = System.currentTimeMillis();
		    while(c < trainInstances + initInstances)
		    {
			GPI.Learn();
			c++;
		    }
		    long endlearnTime = System.currentTimeMillis();
		    totallearnTime = totallearnTime + (endlearnTime - startlearnTime);
		    learntimes[k-1] = learntimes[k-1] + endlearnTime - startlearnTime;
		    
		    int delay = -1;
		    while(c < numInst)
		    {	
//			System.out.println(c);
			long startTime = System.currentTimeMillis();
			if (GPI.checkDrift() && c > numInst - numDriftInstances)
			{
			    long endTime = System.currentTimeMillis();
			    totalTime = totalTime + (endTime - startTime);
			    times[k-1] = times[k-1] + endTime - startTime;
			    delay = c - (numInst - numDriftInstances);
			    delays[k-1] = delay;
			    totalDrift++;
			    //					    totalCompCount += sing.window.compCount;
			    //					    totalCompChecks += sing.window.compChecks;
			    //					    totalWarningCount += sing.warningCount;
			    //					    checks[k] = detector.getChecks();
			    break;
			}
			long endTime = System.currentTimeMillis();
			totalTime = totalTime + (endTime - startTime);
			times[k-1] = times[k-1] + endTime - startTime;
			//					checks[k] = detector.getChecks();
			c++;
		    }
		    totalSize += SizeOfAgent.fullSizeOf(GPI);
//		    br.close();
		}

		bWriter.write(probs[j] + ",");
		bWriter.write(totalDrift + ",");
		bWriter.write(totalDrift / (double) iterations + ",");
		bWriter.write(calculateSum(delays) / totalDrift + ",");
		bWriter.write(calculateStdev(delays, calculateSum(delays) / totalDrift) + ",");

		bWriter.write((double) totalinitTime / iterations + ",");
		bWriter.write(calculateStdevLong(inittimes, (double) totalinitTime / iterations) + ",");
		bWriter.write((double) totallearnTime / iterations + ",");
		bWriter.write(calculateStdevLong(learntimes, (double) totallearnTime / iterations) + ",");
		
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