package testers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import cutpointdetection.ADWIN;
import cutpointdetection.SingDetector;
import driftcategorization.DriftCategorizer;

public class DriftCategorizerTester implements Tester
{
    private int iterations = 100;
    private int numDrifts = 1000;
    private int driftInterval = 1000;
    private double noisePercent = 0.00;
    private int noise = (int)(noisePercent * 100);
    private double leftBound = 0.2;
    private double rightBound = 0.8;
    private double perturbationVariable = 0;
    
    double p = 0.35;

    public void doTest2()
    {	
	try
	{
	    SingDetector sing = new SingDetector(0.05, 32, 1, 1, 0.0010, 0.8, 75);
	    ADWIN adwin = new ADWIN(0.05);

	    double[] noises = {0.0,0.01,0.05,0.10,0.20};

	    for(int j=0; j<noises.length;j++)
	    {
		noise = (int)(noises[j] * 100);
		
		for(int i=0; i<iterations; i++)
		{
		    BufferedReader br = new BufferedReader(new FileReader("D:\\Kauri BackUp Results\\Categorization\\Streams\\Guaranteed\\Per 0 Noise "+ noise + "\\" + numDrifts + "_" + noises[j] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
		    BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\SEED_Drifts\\Guaranteed\\Per 0 Noise "+ noise + "\\" + numDrifts + "_" + noises[j] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
//		    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Categorization\\Streams\\sensor.csv"));
//		    BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\SEED_Drifts\\sensor.csv"));

		    String line = "";
		    int count = driftInterval;
		    int driftPoint = 0;
///*
		    while((line = br.readLine()) != null)
		    {
			driftPoint++;
			if(sing.setInput(Double.parseDouble(line)))
			{
			    if(driftPoint - count >= 0 && driftPoint - count <= driftInterval)
			    {
				bw.write(driftPoint + "\n");
				count += driftInterval;
			    }
			    else if(driftPoint - count > driftInterval)
			    {
				bw.write(driftPoint + "\n");
				count += (driftInterval + driftInterval * (int)((driftPoint - count) / driftInterval));
			    }
			}
		    }
//*/		    
/*		    while((line = br.readLine()) != null)
		    {
			driftPoint++;
			String[] pred = line.split(",");

			if(pred[0].equals(pred[1]))
			{
			    line = "1";
			}
			else
			{
			    line = "0";
			}

			if(sing.setInput(Double.parseDouble(line)))
			{
			    if(driftPoint - count >= 0 && driftPoint - count <= driftInterval)
			    {
				bw.write(driftPoint + "\n");
				count += driftInterval;
			    }
			    else if(driftPoint - count > driftInterval)
			    {
				bw.write(driftPoint + "\n");
				count += (driftInterval + driftInterval * (int)((driftPoint - count) / driftInterval));
			    }
			}
		    }
*/
		    br.close();
		    bw.close();
		}
	    }
	} 
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }

    public void doTest()
    {
	 String[] noises = {"0","1","5","10","20"};
//	 String[] sigmas = {"0.15","0.2","0.25","0.3","0.35"};
	 String[] deltas = {"0.05","0.1","0.15","0.2","0.25"};
	 
	try
	{
	    
	    for(int i = 0; i < iterations; i++)
	    {
		for(int j = 0; j < noises.length; j++)
		{
//		    for(int k = 0; k < sigmas.length; k++)
//		    {
			for(int h = 0; h < deltas.length; h++)
			{
			
			    System.out.printf("iteration:%d noise:%s sigmas:%s delta:%s\n", i, noises[j], i, deltas[h]);
			    
//	    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\sensor.csv"));
	    BufferedReader br = new BufferedReader(new FileReader("D:\\Kauri BackUp Results\\Categorization\\Streams\\Guaranteed\\Per 0 Noise "+noises[j]+"\\1000_"+Double.parseDouble(noises[j])/100+"_0.0_0.2_0.8_"+i+".csv"));
	    //	    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\SlopeData.txt"));
	    BufferedReader br2 = new BufferedReader(new FileReader("src\\testers\\Categorization\\SEED_Drifts\\Guaranteed\\Per 0 Noise "+ noises[j] + "\\" + numDrifts + "_" + Double.parseDouble(noises[j])/100 + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
	    BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\SEED_Results\\Guaranteed\\Per 0 Noise "+noises[j]+"\\delta "+deltas[h]+"\\" + i + ".csv"));
//	    BufferedWriter bw2 = new BufferedWriter(new FileWriter("src\\testers\\DriftPointDataTest.txt"));


	    DriftCategorizer categorizer = new DriftCategorizer(Double.parseDouble(deltas[h]), 100, 200, 100);
	    SingDetector sing = new SingDetector(0.05, 32, 1, 1, 0.0010, 0.8, 75);

	    String line = "";
	    int driftPoint = Integer.parseInt(br2.readLine());
	    int count = 0;
	    while ((line = br.readLine()) != null)
	    {
//		String[] in = line.split(",");
//		if(in[0].equals(in[1]))
//		{
//		    line = "1";
//		}
//		else
//		{
//		    line = "0";
//		}
		
		count++;
		categorizer.setInput(Double.parseDouble(line));
		//categorizer.setInputDouble(Double.parseDouble(line));

//		if(sing.setInput(Double.parseDouble(line)))
//		if(count % 1000 == 0)
		if(count == driftPoint)
		{
		    //		    bw2.write(count + "\n");
		    String l = "";
		    if((l = br2.readLine()) != null)
		    {
			driftPoint = Integer.parseInt(l);
		    }

		    String result = categorizer.evaluateDrift();
//		    System.out.println(result);
		    bw.write(result + "\n");
		    //		    count = 0;
		    
		}
	    }

	    br.close();
	    bw.close();
//	    bw2.close();
			}
		    }
//		}
	    }
	} 
	catch (Exception e)
	{
	    e.printStackTrace();
	}



    }

}
