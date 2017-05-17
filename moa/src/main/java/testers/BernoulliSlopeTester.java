package testers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class BernoulliSlopeTester implements Tester
{
    private int iterations = 100;
    private int numDrifts = 1000;
    private int driftInterval = 10000;
    private double noisePercent = 0.0;
    private int noise = (int)(noisePercent * 100);
    private double leftBound = 0.2;
    private double rightBound = 0.8;
    private double perturbationVariable = 0;

    public void doTest2()
    {
	try
	{

	    for(int i = 0; i<iterations; i++)
	    {
		BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Categorization\\Streams\\Guaranteed\\Per 0 Noise "+ noise + "\\" + numDrifts + "_" + noisePercent + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
		
		BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\BernoulliSlope\\Guaranteed\\Per 0 Noise "+ noise + "\\" + numDrifts + "_" + noisePercent + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
		
		int count = 0;
		double left = 0;
		double right = 0;
		
		int sCount = 0;
		double[] slopes = new double[numDrifts];
		
		String line = "";
		
		while((line = br.readLine()) != null)
		{
		    count++;
		    if(count % driftInterval == 0)
		    {
			slopes[sCount++] = (right - left) / 1000;
			count = 0;
			
			left = 0;
			right = 0;
		    }
		    else if(count % driftInterval >= 9000)
		    {
			right += Double.parseDouble(line);
		    }
		    else if(count % driftInterval <= 1000)
		    {
			left += Double.parseDouble(line);
		    }
		}
		
		for(double d : slopes)
		{
		    bw.write(d + "\n");
		}
		
		br.close();
		bw.close();
	    }
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }
    
    public void doTest()
    {
	try
	{
	    for(int i = 0; i< iterations; i++)
	    {
		BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Categorization\\BernoulliSlope\\Guaranteed\\Per 0 Noise "+ noise + "\\" + numDrifts + "_" + noisePercent + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
		BufferedReader brSlope = new BufferedReader(new FileReader("src\\testers\\Categorization\\SlopeData\\Guaranteed\\Per 0 Noise " + noise + "\\SlopeData_" + numDrifts + "_" + noisePercent + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
		    
		BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\BernoulliSlopeComparison\\Guaranteed\\Per 0 Noise " + noise + "\\" + numDrifts + "_" + noisePercent + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
		
		String line1 = "";
		String line2 = "";
		
		double avg = 0;
		
		while((line1 = br.readLine()) != null && (line2 = brSlope.readLine()) != null)
		{
		    double d1 = Double.parseDouble(line1);
		    double d2 = Double.parseDouble(line2.split(",")[2]);
		    
		    bw.write(d1/900 + "," + d2 + "," + (Math.abs(d1/900) - d2) + "\n");
		    
		    avg += Math.abs(Math.abs(d1/900) - d2);
		}
	    
		System.out.println(avg);
		
		bw.close();
	    }
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }

}
