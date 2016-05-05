package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class BernoulliGeneratorTester implements Tester
{

    public BernoulliGeneratorTester()
    {

    }
    @Override
    public void doTest()
    {
	int iterations = 100; // int vLevel = 1;
	//int[] vLevels = {1000, 5000, 10000, 50000, 100000};
	int length = 100000;
	int driftlength = 1000;

	//String[] slopes = {"0.0001","0.0002","0.0003","0.0004","0.0006"};
	String[] slopes = {"0.4","0.6","0.8"};
	
	double[] means = { 0.2 };
	String[] positions = {"0.25","0.50","0.75"};
	System.err.println("test");
	try
	{
	    for(int j = 0; j<positions.length; j++)
	    {
		double pos = Double.parseDouble(positions[j]);
		for(int p = 0; p<slopes.length; p++)
		{
		    System.out.println(slopes[p]);
		    double slope = Double.parseDouble(slopes[p]);
		    for(int i = 0; i<iterations; i++)
		    {
			BufferedWriter bWriter = new BufferedWriter(new FileWriter("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\FN Test\\"+positions[j]+"\\Abrupt_0.2_"+slopes[p]+"\\"+positions[j]+"_" +length+"_"+slopes[p]+"_" + i + ".csv"));
			//BufferedWriter bWriter2 = new BufferedWriter(new FileWriter("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\FN Test\\"+positions[j]+"\\Abrupt_0.2_"+slopes[p]+"\\"+positions[j]+"_" +length+"_"+slopes[p]+"_" + i + "_test.csv"));
			
			means[0] = 0.2;
			BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(means, (int)(length * pos), i);
			while (gen.hasNextTransaction())
			{
			    //bWriter2.write(means[0] + "\n");
			    bWriter.write(gen.getNextTransaction() + "\n");
			}
/*
			BernoulliDistributionGenerator gen2 = new BernoulliDistributionGenerator(means, driftlength, i);
			while (gen2.hasNextTransaction())
			{
			    means[0] += slope;
			    gen2.setMeans(means);
			    //bWriter2.write(means[0] + "\n");
			    bWriter.write(gen2.getNextTransaction() + "\n");
			}
*/
//			BernoulliDistributionGenerator gen3 = new BernoulliDistributionGenerator(means, (int)(length * (1-pos)) - driftlength, i);
			means[0] = slope;
			BernoulliDistributionGenerator gen3 = new BernoulliDistributionGenerator(means, (int)(length * (1-pos)), i);
			while (gen3.hasNextTransaction())
			{
			    //bWriter2.write(means[0] + "\n");
			    bWriter.write(gen3.getNextTransaction() + "\n");
			}
			bWriter.close();
			//bWriter2.close();
		    }
		}
	    }	    

	} catch (Exception e)
	{
	    e.printStackTrace();
	}

	System.out.println("ended");

    }
    
    // RANDOM DRIFT LOCATION WITH GRADUAL DRIFTS
    /*
    @Override public void doTest() 
    { 
	int iterations = 100; // int vLevel = 1;
	//int[] vLevels = {1000, 5000, 10000, 50000, 100000};
	int length = 10000000;
	int driftlength = 1000;

	String[] slopes = {"0.0001","0.0002","0.0003","0.0004","0.0006"};
	 
	try 
	{ 
	    for(int p = 0; p<slopes.length; p++)
	    {
		System.out.println(slopes[p]);
		for(int i = 0; i<iterations; i++) 
		{
		    BufferedWriter bWriterloc = new BufferedWriter(new FileWriter("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\Gradual_"+slopes[p]+"\\rand_" +length+"_"+slopes[p]+"_" + i + "_driftlocations.csv"));
		    int[] driftlocations = new int[102];
		    Random rand = new Random(i);
		    //		driftlocations[0] = 0;
		    for(int k = 1; k<101; k++)
		    {
			driftlocations[k] = rand.nextInt(length-(driftlength*100));// + (k*driftlength);
		    }
		    driftlocations[101] = length-(driftlength*100);
		    Arrays.sort(driftlocations);		


		    BufferedWriter bWriter = new BufferedWriter(new FileWriter("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\Gradual_"+slopes[p]+"\\rand_" +length+"_"+slopes[p]+"_" + i + ".csv"));
//		    BufferedWriter bWriter2 = new BufferedWriter(new FileWriter("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\Gradual_"+slopes[p]+"\\rand_" +length+"_"+slopes[p]+"_" + i + "_test.csv"));
		    
		    double[] mean = new double[]{0.2};
		    double direction = 1.0;
		    double slope = Double.parseDouble(slopes[p]);
		    int c = 0;
		    //		COLD START - FIRST BLOCK WITH NO DRIFT
		    //-----------------------------------------------------------------------------------------------------------------------------------------------------
//		    System.out.println(driftlocations[1] - driftlocations[0]);
		    BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(mean, driftlocations[1] - driftlocations[0], i);
		    while(gen.hasNextTransaction()) 
		    { 
//			bWriter2.write(c++ + "," + mean[0] + "\n");
			bWriter.write(gen.getNextTransaction() + "\n"); 			
		    }
		    //-----------------------------------------------------------------------------------------------------------------------------------------------------		    

		    for(int j = 1; j<101; j++)
		    {
			BernoulliDistributionGenerator genDrift = new BernoulliDistributionGenerator(mean, driftlength, i);
			while (genDrift.hasNextTransaction())
			{
			    mean[0] += slope * direction;
//			    bWriter2.write(c++ + "," + mean[0] + "\n");
			    genDrift.setMeans(mean);
			    bWriter.write(genDrift.getNextTransaction() + "\n");
			}

			direction *= -1.0;

			int numInstances = driftlocations[j+1] - driftlocations[j];

			BernoulliDistributionGenerator gen_ndrift = new BernoulliDistributionGenerator(mean, numInstances, i);
			while(gen_ndrift.hasNextTransaction()) 
			{ 
//			    bWriter2.write(c++ + "," + mean[0] + "\n");
			    bWriter.write(gen_ndrift.getNextTransaction() + "\n"); 			
			}
		    }
		    bWriter.close();

		    for(int k = 0; k < driftlocations.length-2; k++)
		    {
			driftlocations[k+1] += k*driftlength;
			bWriterloc.write(driftlocations[k+1] + "\n");
		    }
		    bWriterloc.close();
//		    bWriter2.close();
		} 
	    }

	} 
	catch(Exception e) 
	{ 
	    e.printStackTrace();
	} 
    }
    */
    // RANDOM DRIFT LOCATION ABRUPT
    /*
    @Override public void doTest() 
    { 
	int iterations = 100; // int vLevel = 1;
	//int[] vLevels = {1000, 5000, 10000, 50000, 100000};
	int length = 10000000;

	double[] driftMean = {0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4};
	 
	try 
	{ 
	    for(int i = 0; i<iterations; i++) 
	    {
		BufferedWriter bWriterloc = new BufferedWriter(new FileWriter("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\Abrupt_0.2_0.4\\rand_" +length+"_Abrupt_0.2_0.4_" + i + "_driftlocations.csv"));
		int[] driftlocations = new int[102];
		Random rand = new Random(i);
//		driftlocations[0] = 0;
		for(int k = 1; k<101; k++)
		{
		    driftlocations[k] = rand.nextInt(length);
		}
		driftlocations[101] = length;
		Arrays.sort(driftlocations);		
		for(int l=1; l<driftlocations.length-1; l++)
		{
		    bWriterloc.write(driftlocations[l] + "\n");
		}
		bWriterloc.close();
		
		BufferedWriter bWriter = new BufferedWriter(
			new FileWriter("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\Abrupt_0.2_0.4\\rand_" +length+"_Abrupt_0.2_0.4_" + i + ".csv"));

		for(int j = 0; j<101; j++)
		{
		    int numInstances = driftlocations[j+1] - driftlocations[j];
		    double[] mean = new double[1];
		    mean[0] = driftMean[j];
		    
		    BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(mean, numInstances, i);
		    while(gen.hasNextTransaction()) 
		    { 
			bWriter.write(gen.getNextTransaction() + "\n"); 			
		    }
		}
		bWriter.close(); 
	    } 


	} 
	catch(Exception e) 
	{ 
	    e.printStackTrace();
	} 
    }
 */   
//    @Override public void doTest() 
//    { 
//	int iterations = 100; // int vLevel = 1;
//	int[] vLevels = {1000, 5000, 10000, 50000, 100000};
//	String[] slopes = {"0.0001"};
//
//	double[] driftMean = {0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
//		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
//		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
//		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
//		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
//		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
//		0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2, 0.4, 0.2,
//		0.4, 0.2, 0.4, 0.2, 0.4, 0.2};
//	 
//	try 
//	{ 
//	    for(int vLevel = 0; vLevel < vLevels.length; vLevel++) 
//	    { 
//		int numInstances = vLevels[vLevel]; 
//		for(int k = 0; k < slopes.length; k++)
//		{
//		    double slope = Double.parseDouble(slopes[k]);
//		    int numDriftInstances = 1000;
////		    double[] driftMean = {0.2};
//		    
//		    for(int i = 0; i<iterations; i++) 
//		    {
//			double direction = 1.0;
//			BufferedWriter bWriter = new BufferedWriter(
//				new FileWriter("src\\testers\\VolatilityPredictingDrift\\Datasets\\Abrupt_0.2_0.4\\v" +vLevels[vLevel]+"_Abrupt_0.2_0.4_" + i + ".csv"));
//
////			for(int j = 0; j < 100; j++)
////			{
//			    BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(driftMean, numInstances, i);
//			    while(gen.hasNextTransaction()) 
//			    { 
//				bWriter.write(gen.getNextTransaction() + "\n"); 
////				System.out.println(driftMean[0]);
//			    }
///*
//			    BernoulliDistributionGenerator genDrift = new BernoulliDistributionGenerator(driftMean, numDriftInstances, i);
//			    while (genDrift.hasNextTransaction())
//			    {
//				driftMean[0] += slope * direction;
////				System.out.println(driftMean[0]);
//				genDrift.setMeans(driftMean);
//				bWriter.write(genDrift.getNextTransaction() + "\n");
//			    }
//			    
//			    direction *= -1.0;
//*/
////			}
//			bWriter.close(); 
//		    } 
//		}
//	    }
//
//	} 
//	catch(Exception e) 
//	{ 
//	    e.printStackTrace();
//	} 
//    }
    //    @Override
    //    public void doTest()
    //    {
    //	int i = 0;
    //
    //	double[] means = { 0.5 };
    //	BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(
    //		means, 1000000, 1);
    //
    //	try
    //	{
    //	    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
    //		    "src\\testers\\Data.txt"));
    //	    while (gen.hasNextTransaction())
    //	    {
    //		if (gen.getCurrentTID() != i)
    //		{
    //		    System.out.println("no match");
    //		}
    //		bWriter.write(gen.getNextTransaction() + "\n");
    //		i++;
    //	    }
    //
    //	    bWriter.close();
    //
    //	} catch (Exception e)
    //	{
    //	    System.err.println("error");
    //	}
    //
    //	System.out.println("ended");
    //
    //    }

}
