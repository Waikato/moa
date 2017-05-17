package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import volatilityevaluation.RelativeVolatilityDetector;
import cutpointdetection.SingDetector;

public class VolatilityStreamGenerator implements Tester
{


    public void doTest2()
    {
	int[] driftpoint = { 0, 100000, 50000, 10000, 5000, 1000, 500, 100 };

	for (int vLevel = 1; vLevel <= 7; vLevel++)
	{
	    for (int j = 0; j < 100; j++)
	    {
		try
		{
		    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityStreamDriftPoint\\" + vLevel + "\\VolatilityStream_" + vLevel + "_" + j + ".csv"));
		    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\FPRemoved\\" + vLevel + "\\VolatilityStream_" + vLevel + "_" + j + ".csv"));

		    SingDetector sing = new SingDetector(0.05, 32, 1, 1, 0.0025, 0.2, 50);
		    RelativeVolatilityDetector rv = new RelativeVolatilityDetector(sing, 32);

		    int c = 0;
		    int y = 0;
		    boolean drift = false;
		    String line = "";
		    while ((line = br.readLine()) != null)
		    {
			c++;
			y++;

			if (c % driftpoint[vLevel] == 0)
			{
			    drift = true;
			}

			if (drift == true && sing.setInput(Double.parseDouble(line)))
			{
			    bWriter.write(y + "\n");
			    y = 0;
			    drift = false;
			}

		    }

		    br.close();
		    bWriter.close();
		} 
		catch (Exception e)
		{

		}

	    }
	}

    }


    @Override 
    public void doTest() 
    { 
	int iterations = 100; // int vLevel = 1;
	int[] vLevels = {0, 100000, 50000, 10000, 5000, 1000, 500, 100};


	double[] driftMean = {0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2,
		0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2,
		0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2,
		0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2,
		0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2,
		0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2,
		0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2, 0.8, 0.2,
		0.8, 0.2, 0.8, 0.2, 0.8, 0.2};

	try 
	{ 
	    for(int vLevel = 1; vLevel < vLevels.length; vLevel++) 
	    { 
		int numInstances = vLevels[vLevel]; 

		for(int i = 0; i<iterations; i++) 
		{
		    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\VolatilityStreamDriftPoint\\" + vLevel + "\\VolatilityStream_" + vLevel + "_" + i + ".csv"));

		    BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(driftMean, numInstances, i);
		    while(gen.hasNextTransaction()) 
		    { 
			bWriter.write(gen.getNextTransaction() + "\n"); 
		    }

		    bWriter.close(); 
		} 
	    }

	} 
	catch(Exception e) 
	{ 
	    System.err.println("error"); 
	} 
    }

}
