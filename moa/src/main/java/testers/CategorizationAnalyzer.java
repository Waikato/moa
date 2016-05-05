package testers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class CategorizationAnalyzer implements Tester
{

    private int iterations = 100;
    private int numDrifts = 1000;
    private int driftInterval = 1000;
    private double noisePercent = 0.05;
    private int noise = (int)(noisePercent * 100);
    private double leftBound = 0.2;
    private double rightBound = 0.8;
    private double perturbationVariable = 0;
    private double delta = 0.20;
    
    private double n = 0.5;
    private double p = 0.3;
    
    public void doTest()
    {
	double[] deltas = {0.05,0.1,0.15,0.2,0.25};
	double[] noises = {0.0,0.01,0.05,0.10,0.20};
	try
	{
	    for(int k=0; k<noises.length; k++)
	    {
		noise = (int)(noises[k] * 100);
		
		BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\SEED_Results\\Guaranteed\\Per 0 Noise " + noise + "_" + numDrifts + "_" + noises[k] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + ".csv"));
		//BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\SEED_Results\\GAUSSIAN\\"+ n + "_" + p +"\\Per 0 Noise " + noise + "_" + numDrifts + "_" + noises[k] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + ".csv"));
		
		bw.write("Delta,Total Drifts Found,Correctly Categorized Drifts,TP Rate" + "\n");

		for(int j=0; j<deltas.length;j++)
		{
		    long[] times = new long[iterations];
		    long totalTime = 0;
		    int totalDrifts = 0; 
		    int correctDrifts = 0;

		    for(int i=0; i<iterations;i++)
		    {
			System.out.println(i);
			BufferedReader brSlope = new BufferedReader(new FileReader("src\\testers\\Categorization\\SlopeData\\Guaranteed\\Per 0 Noise " + noise + "\\SlopeData_" + numDrifts + "_" + noises[k] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
			BufferedReader brResult = new BufferedReader(new FileReader("src\\testers\\Categorization\\SEED_Results\\Guaranteed\\Per 0 Noise " + noise + "\\delta " + deltas[j] + "\\" + i + ".csv"));

			String line1 = brSlope.readLine();
			String line2 = "";
//			System.out.println("slope: " + line1);
			int v1 = 0, v2;

			v1 = Integer.parseInt(line1.split(",")[0]);

			while((line2 = brResult.readLine()) != null)
			{
			    totalDrifts++;			

			    v2 = Integer.parseInt(line2.split(",")[0]);
			    v2 = v2 == 1000000 ? 999999 : v2;
//			    System.out.println(v1 + "," + v2);
			    while(v1/driftInterval != (v2/driftInterval) && ((line1 = brSlope.readLine()) != null))
			    {
				v1 = Integer.parseInt(line1.split(",")[0]);
			    }
//			    System.out.println(v1);
//			    System.out.println("slope: " + line1);
//	    		    System.out.println("result: " + line2);	

			    if(line1.split(",")[1].equals(line2.split(",")[1]))
			    {
				correctDrifts++;
			    }
			}


			brSlope.close();
			brResult.close();
		    }

		    bw.write(deltas[j] + "," + totalDrifts + "," + correctDrifts + "," + (double)correctDrifts / (totalDrifts - (100*iterations)) + "\n");
		}
		bw.close();
	    }
	} 
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }
    
    public void doTest2()
    {
	
	double[] deltas = {0.05,0.1,0.15,0.2,0.25};
	double[] noises = {0.0,0.01,0.05,0.10,0.20};
	int delay = 0;
	try
	{
	    for(int k=0; k<noises.length; k++)
	    {
		noise = (int)(noises[k] * 100);
		BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\TrueDriftResults\\Guaranteed\\Per 5 Noise " + noise + "_" + numDrifts + "_" + noises[k] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + ".csv"));
		bw.write("Delta,Total Drifts Found,Correctly Categorized Drifts,TP Rate,stdev" + "\n");

		for(int j=0; j<deltas.length;j++)
		{
		    double[] correct = new double[iterations];
		    long totalTime = 0;
		    int totalDrifts = 0;
		    int correctDrifts = 0;
		    

		    for(int i=0; i<iterations;i++)
		    {			
			BufferedReader brSlope = new BufferedReader(new FileReader("src\\testers\\Categorization\\SlopeData\\Guaranteed\\Per 5 Noise " + noise + "\\SlopeData_" + numDrifts + "_" + noises[k] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
			BufferedReader brResult = new BufferedReader(new FileReader("src\\testers\\Categorization\\TrueDriftResults\\Guaranteed\\Per 5 Noise " + noise + "\\delta " + deltas[j] + "\\" + deltas[j] + "_"  + numDrifts + "_" + noises[k] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
			int c = 0;
			String line1 = "";
			String line2 = "";

			while((line1 = brSlope.readLine()) != null && (line2 = brResult.readLine()) != null)
			{	
			    totalDrifts++;
			    
			    if(line1.split(",")[1].equals(line2.split(",")[1]))
			    {
				c++;
				correctDrifts++;
				
			    }
			}
			
			correct[i] = c;

			brSlope.close();
			brResult.close();
		    }

		    bw.write(deltas[j] + "," + totalDrifts + "," + correctDrifts + "," + (double)correctDrifts / (totalDrifts - (100*iterations)) +"," + calculateStdev(correct, (double)correctDrifts / (double)iterations) / (totalDrifts - (100*iterations)) + "\n");
		}
		bw.close();
	    }
	} 
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }
    
    public double calculateStdev(double[] times, double mean)
    {
	double sum = 0;
	int count = 0;
	for (Double i : times)
	{
	    if (i > 0)
	    {
		count++;
		sum += Math.pow(i - mean, 2);
	    }
	}
	return Math.sqrt(sum / count);
    }

}
