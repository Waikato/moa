package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import cutpointdetection.ADWIN;
import cutpointdetection.SingDetector;

public class VDriftDetectionFalsePositiveTester implements Tester
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
	    int length = 100000;
	    double[] betas = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5};
	    double[] deltas = {0.05, 0.10, 0.30};    

	    for(int j = 0; j < betas.length; j++)
	    {
		System.out.println(betas[j]);
		BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\SEED\\FP Test\\FP_"+length+"_sine"+betas[j]+".csv"));
		bWriter.write("Beta,Number of Drifts,FP Rate");
		bWriter.newLine();

		for(int i = 0; i<deltas.length; i++)
		{
		    System.out.println(deltas[i]);
		    int totalDrift = 0;
       
		    
		    for (int k = 0; k < iterations; k++)
		    {
			//System.out.println(k);
			ADWIN adwin = new ADWIN(deltas[i]);
			SingDetector sing = new SingDetector(deltas[i],32,1,1,0.01,0.8,75);
			BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Abrupt_0.2_0.4\\v100000_Abrupt_0.2_0.4_"+k+".csv"));

			sing.setTension(betas[j]);
			sing.setMode(SINE_FUNC);

			String line = "";

			int c = 0;
			double relPos = 0.0;

			while ((line = br.readLine()) != null)
			{
			    relPos = c / 100000.0;

			    if (sing.setInput(Double.parseDouble(line), deltas[i], relPos))
			    {
				totalDrift++;
			    }
			    c++;
			    
			    if(c > length){break;}
			}
			br.close();
		    }

		    bWriter.write(deltas[i] + ",");
		    bWriter.write(totalDrift + ",");
		    bWriter.write(totalDrift / (double) (iterations*length) + ",");
		    bWriter.newLine();
		}
		bWriter.close();
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
