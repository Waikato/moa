package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import cutpointdetection.ADWIN;
import cutpointdetection.CutPointDetector;
import cutpointdetection.SEEDChangeDetector;
import cutpointdetection.SingDetector;
import cutpointdetection.OnePassDetector.EDD;

public class FalsePositiveTester implements Tester
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
  
	    int blockSize = 32;
	    int numInstances = 100000;

	    //double[] epsilonPrimes = { 0.0025, 0.005, 0.0075, 0.01 };
	    double[] epsilonPrimes = { 0.01 };
	    //double[] linearAlphas = { 0.2, 0.4, 0.6, 0.8 };
	    double[] linearAlphas = { 0.8 };
	    double[] expAlphas = { 0.01, 0.025, 0.05, 0.075, 0.1 };
	    
	    double[] alphas = null;
	    int compTerms = 75;

	    if (DECAY_MODE == LINEAR_DECAY)
	    {
		alphas = linearAlphas;
	    } 
	    else if (DECAY_MODE == EXPONENTIAL_DECAY)
	    {
		alphas = expAlphas;
	    }

	    for (int w = 0; w < alphas.length; w++)
	    {
		for (int q = 0; q < epsilonPrimes.length; q++)
		{
		    double[] probs = { 0.01, 0.1, 0.3, 0.5 };
		    double[] deltas = { 0.05, 0.1, 0.3 };

//		    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Volatility&ChangeDetection\\" + "Test" + "\\" + blockSize + "_" + DECAY_MODE + "_" + COMPRESSION_MODE + "_" + compTerms + "_" + epsilonPrimes[q] + "_" + alphas[w] + "_" + numInstances + "_" + iterations + ".csv"));
//		    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Volatility&ChangeDetection\\" + "Test" + "\\EDD_" + numInstances + "_" + iterations + ".csv"));
		    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Volatility&ChangeDetection\\" + "Test" + "\\SEED_TEST.csv"));
		    
		    bWriter.write("mean\\delta,0.05,0.1,0.3");
		    bWriter.newLine();

		    for (int i = 0; i < probs.length; i++)
		    {
			bWriter.write(probs[i] + "");

			for (int j = 0; j < deltas.length; j++)
			{
			    int totalDrift = 0;

			    for (int k = 0; k < iterations; k++)
			    {
				double[] prob = { probs[i] };
				generateInput(prob, numInstances, k);
				
				EDD edd = new EDD(deltas[j], 100, 1, 0);
				ADWIN adwin = new ADWIN(deltas[j]);
				SingDetector sing = new SingDetector(deltas[j], blockSize, DECAY_MODE,COMPRESSION_MODE, epsilonPrimes[q], alphas[w], compTerms);
				SEEDChangeDetector seed = new SEEDChangeDetector();
				CutPointDetector detector = sing;
				
				
				
				BufferedReader br = new BufferedReader(new FileReader("src\\testers\\FPData.txt"));
				String line = "";

				while ((line = br.readLine()) != null)
				{
				    seed.input(Double.parseDouble(line));
				    if (seed.getChange())
				    {
					totalDrift++;
				    }
				}
				//totalChecks += detector.getChecks();
				br.close();
			    }
			    bWriter.write("," + totalDrift);
			}
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

    public void generateInput(double[] prob, int numInstances, int randomSeed)
    {
	try
	{
	    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
		    "src\\testers\\FPData.txt"));

	    BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(
		    prob, numInstances, randomSeed);
	    while (gen.hasNextTransaction())
	    {
		bWriter.write(gen.getNextTransaction() + "\n");
	    }

	    bWriter.close();

	} catch (Exception e)
	{
	    System.err.println("error");
	}
    }
}
