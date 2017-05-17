package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import sizeof.agent.SizeOfAgent;
import volatilityevaluation.VolatilityPredictionFeatureExtractor;
import cutpointdetection.ADWIN;
import cutpointdetection.SingDetector;

public class VDriftDetectionTruePositiveTester implements Tester
{
    int numDriftInstances = 1000;
    //int numDriftInstances = 500000;
    int iterations = 100;
    int totalIt = 900;

    public final int NOCORRECTION = 0;
    public final int SINE_FUNC = 1;
    public final int SIGMOID_FUNC = 2;

    @Override
    public void doTest()
    {
	try
	{
	    int[] numInst = { 1000000 };
	    int length = 100000;
	    double[] deltas = { 0.05 };
	    String[] slopes = {"0.0001","0.0002", "0.0003", "0.0004"};
	    //String[] slopes = {"0.0004"};
	    double[] betas = {0.0,0.1,0.2,0.3,0.4,0.5};

	    double delta = 0.05;    

	    double[] prob = { 0.2 };
	    for(int j = 0; j < betas.length; j++)
	    {
		System.out.println(betas[j]);
		BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\SEED\\TP_Test_Online\\Online_z1.65_BS30_TP_"+length+"_sine"+betas[j]+".csv"));
		bWriter.write("Slopes,Number of Drifts,TP Rate,Delay,DelayStdev,Time,TimeStdev");
		bWriter.newLine();

		VolatilityPredictionFeatureExtractor extractor = new VolatilityPredictionFeatureExtractor();

		for(int i = 0; i < slopes.length; i++)
		{
		    int fold = 10;
		    System.out.println(slopes[i]);
		    int count = 0;
		    totalIt = 90*fold;
		    int[] delays = new int[totalIt];
		    int totalDrift = 0;
		    long[] times = new long[totalIt];
		    long totalTime = 0;
		    
		    for (int nthfold = 0; nthfold < fold; nthfold++)
		    {

			for (int k = nthfold*10; k < 10; k++)
			{
			    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_"+slopes[i]+"\\"+length+"_"+slopes[i]+ "_" + k + ".csv"));

			    int c = 0;
			    String line = "";
			    while ((line = br.readLine()) != null)
			    {			   
				extractor.extract(Double.parseDouble(line));
				c++;
				if(c > length){break;}
			    }
			    br.close();
			}

			for (int k = 0; k < iterations; k++)
			{
			    if(k / 10 == nthfold){continue;}

			    count++;
			    //System.out.println(k);
			    //ADWIN adwin = new ADWIN(deltas[0]);	
			    SingDetector sing = new SingDetector(deltas[0],32,1,1,0.01,0.8,75);
			    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_"+slopes[i]+"\\"+length+"_"+slopes[i]+ "_" + k + ".csv"));

			    sing.setTension(betas[j]);
			    sing.setMode(SINE_FUNC);
			    //adwin.setTension(betas[j]);
			    //adwin.setMode(SINE_FUNC);

			    String line = "";

			    int c = 0;
			    double relPos = 0.5;
			    int delay = -1;

			    while ((line = br.readLine()) != null)
			    {
				//System.out.println(c);
				long startTime = System.currentTimeMillis();
				extractor.setInput(Double.parseDouble(line));
				double pred = extractor.getConfidencePrediction();
				
//				if(extractor.getConfidencePrediction() > 0.1)
//				{
				    if(sing.setInput(Double.parseDouble(line), deltas[0], pred) && c >= length)
				    {
					totalDrift++;
					long endTime = System.currentTimeMillis();
					totalTime = totalTime + (endTime - startTime);
					times[count-1] = times[count-1] + endTime - startTime;
					delay = c - length;
					delays[count-1] = delay;
//					System.out.println(c);
					break;
				    }
//				}
//				else
//				{
//				    if(adwin.setInput(Double.parseDouble(line), deltas[0], 1.0) && c >= length)
//				    {
//					totalDrift++;
//					long endTime = System.currentTimeMillis();
//					totalTime = totalTime + (endTime - startTime);
//					times[count-1] = times[count-1] + endTime - startTime;
//					delay = c - length;
//					delays[count-1] = delay;
//					System.out.println(c);
//					break;
//				    }
//				}
				c++;
			    }
			    br.close();
			}
		    }

		    bWriter.write(slopes[i] + ",");
		    bWriter.write(totalDrift + ",");
		    bWriter.write(totalDrift / (double) totalIt + ",");
		    bWriter.write(calculateSum(delays) / totalDrift + ",");
		    System.out.println(calculateSum(delays) +","+ totalDrift + ",");
		    bWriter.write(calculateStdev(delays, calculateSum(delays) / totalDrift) + ",");
		    bWriter.write((double) totalTime / totalIt + ",");
		    bWriter.write(calculateStdevLong(times, (double) totalTime / totalIt) + ",");
		    bWriter.newLine();

//		    System.out.println(count);
		}
		bWriter.close();
		
	    }  
	}
	catch (Exception e)
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


