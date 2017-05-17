package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import cutpointdetection.ChanceDetector;

public class KappaTester implements Tester
{
    int numInstances = 10000;
    int numDriftInstances = 2300;

    @Override
    public void doTest()
    {
	try
	{
	    double[] prob = { 0.2 };
	    double increment = 0.05;

	    int randomSeed = 10;
	    double delta = 0.05;

	    int totalDrift = 0;

	    for (int k = 0; k < 100; k++)
	    {
		ChanceDetector chanceDetector = new ChanceDetector(0.0001, k);

		generateInput(prob, increment, numInstances, randomSeed);

		BufferedReader br = new BufferedReader(new FileReader(
			"src\\testers\\TPData.txt"));
		String line = "";

		int i = 0;

		while ((line = br.readLine()) != null)
		{
		    if (chanceDetector.setInput(Double.parseDouble(line))
			    && i > (numInstances - numDriftInstances))
		    {
			totalDrift++;
			break;
		    }
		    i++;
		}
		br.close();
	    }

	    System.out.println("Accuracy: " + totalDrift / 100.0);

	} catch (Exception e)
	{
	    e.printStackTrace();
	}

    }

    public void generateInput(double[] driftProb, double driftIncrement,
	    int numInstances, int randomSeed)
    {
	try
	{

	    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
		    "src\\testers\\TPData.txt"));

	    BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(
		    driftProb, numInstances - numDriftInstances, randomSeed);
	    while (gen.hasNextTransaction())
	    {
		bWriter.write(gen.getNextTransaction() + "\n");
	    }

	    driftProb[0] += driftIncrement;

	    BernoulliDistributionGenerator genDrift = new BernoulliDistributionGenerator(
		    driftProb, numDriftInstances, randomSeed);
	    while (genDrift.hasNextTransaction())
	    {
		bWriter.write(genDrift.getNextTransaction() + "\n");
	    }

	    driftProb[0] -= driftIncrement;

	    bWriter.close();

	} catch (Exception e)
	{
	    System.err.println("error");
	}
    }

}
