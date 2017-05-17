package testers;

import inputstream.DriftCategorizationStreamGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import driftcategorization.DriftCategorizer;

public class DriftCategorizationAccuracyTester implements Tester
{
    private DriftCategorizationStreamGenerator streamGen;
    private DriftCategorizer categorizer;

    private final int LINEAR_DECAY = 1;
    private final int EXPONENTIAL_DECAY = 2;
    private final int FIXED_TERM = 1;
    private final int PARETO = 2;
    private final int UNIFORM = 0;
    private final int GAUSSIAN = 1;
    private final int POISSON = 2;
    private final int BINOMIAL = 3;

    private int DECAY_MODE = EXPONENTIAL_DECAY;
    private int COMPRESSION_MODE = FIXED_TERM;

    private int iterations = 100;
    private int numDrifts = 1000;
    private int driftInterval = 1000;
    private double noisePercent = 0.20;
    private int noise = (int)(noisePercent * 100);
    private double leftBound = 0.25;
    private double rightBound = 0.55;
    private double perturbationVariable = 0;
    private double delta = 0.20;
    
    private double mean = 0.5;
    private double stdev = 0.35;
    
    private int n = 20;
    private double p = 0.4;

    @Override
    public void doTest()
    {

	try
	{
/*	    
	    //Generation
	   double[] noises = {0.0,0.01,0.05,0.10,0.20};
	//   double[] noises = {0.0};
	    for(int j=0; j<noises.length;j++)
	    {
		noisePercent = noises[j];
		noise = (int)(noisePercent * 100);

		for(int i = 0; i < iterations; i++)
		{	    	
		    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\Streams\\BINOMIAL\\" + n + "_" + p + "\\Per 0 Noise " + noise + "\\" + numDrifts + "_" + noisePercent + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
		    BufferedWriter bWriterSlope = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\SlopeData\\BINOMIAL\\" + n + "_" + p + "\\Per 0 Noise " + noise + "\\" + "SlopeData_" + numDrifts + "_" + noisePercent + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));

		    //UNIFORM
//		    streamGen = new DriftCategorizationStreamGenerator(bWriter, bWriterSlope, 0.00, numDrifts, i, noisePercent, perturbationVariable, 0.01, driftInterval, 0.0/driftInterval, leftBound/driftInterval, leftBound/driftInterval, rightBound/driftInterval, rightBound/driftInterval, 1.0/driftInterval);
		    //GAUSSIAN
//		    streamGen = new DriftCategorizationStreamGenerator(GAUSSIAN, bWriter, bWriterSlope, 0.00, numDrifts, i, noisePercent, perturbationVariable, 0.01, driftInterval, mean, stdev);
		    //BINOMIAL
		    streamGen = new DriftCategorizationStreamGenerator(BINOMIAL, bWriter, bWriterSlope, 0.00, numDrifts, i, noisePercent, perturbationVariable, 0.01, driftInterval, n, p);
		    
		    streamGen.generateInput(); 

		    bWriter.close();
		    bWriterSlope.close();
		}
	    }
	    //Generation
	  

	    //Categorization FOR TRUE DRIFT POINT EXPERIMENT
//	       double[] noises = {0.0,0.01,0.05,0.10,0.20};
	   // double[] noises = {0.0};
	       int delay = 0;
	    
	    for(int k=0; k<noises.length;k++)
	    { 
		noisePercent = noises[k];
		noise = (int)(noisePercent * 100);

		double[] deltas = {0.05,0.1,0.15,0.2,0.25};

		long[] times = new long[iterations];


		for(int j = 0; j < deltas.length; j++)
		{
		    long totalTime = 0;
		    double totalMemory = 0;
		    for(int i = 0; i < iterations; i++)
		    {	    
			BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Categorization\\Streams\\BINOMIAL\\" + n + "_" + p + "\\Per 0 Noise "+ noise + "\\" + numDrifts + "_" + noisePercent + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
			BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\TrueDriftResults\\BINOMIAL\\" + n + "_" + p + "\\Per 0 Noise " + noise + "\\delta " + deltas[j] + "\\" + deltas[j] + "_"  + numDrifts + "_" + noisePercent + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));	    

			DriftCategorizer categorizer = new DriftCategorizer(deltas[j], driftInterval/10, 200, 100);

			String line = "";
			int count = 0;
			//int count = -1 * delay;

			while ((line = br.readLine()) != null)
			{
			    long startTime = System.currentTimeMillis();
			    count++;
			    categorizer.setInput(Double.parseDouble(line));
			    //		categorizer.setInputDouble(Double.parseDouble(line));
			    if(count % driftInterval == 0)
			    //if(count > 0 && count % driftInterval == 0)
			    {
				String result = categorizer.evaluateDrift();
				//			System.out.println(result);
				long endTime = System.currentTimeMillis();
				totalTime = totalTime + (endTime - startTime);
				times[i] = times[i] + endTime - startTime;

				bw.write(result + "\n");
				count = 0;
			    }
			}		    

			totalMemory += sizeof.agent.SizeOfAgent.fullSizeOf(categorizer);

			br.close();
			bw.close();
			//Categorization
		    }
		    System.out.println(totalTime);
		    System.out.println(totalMemory);
		}
	    }
*/
	    
	    double[] deltas = {0.25};
	    double[] noises = {0.0};
	    for(int k = 0; k<noises.length;k++)
	    {
		noise = (int)(noises[k] * 100);
		for(int j = 0; j < deltas.length; j++)
		{
		    for(int i = 0; i < 1; i++)
		    {	    
//			BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Categorization\\Streams\\BINOMIAL\\" + n + "_" + p + "\\Per 0 Noise "+ noise + "\\" + numDrifts + "_" + noises[k] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
//			BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\ADWIN_Results\\BINOMIAL\\" + n + "_" + p + "\\Per 0 Noise " + noise + "\\delta " + deltas[j] + "\\" + deltas[j] + "_"  + numDrifts + "_" + noises[k] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));	    
//			BufferedReader brDrift = new BufferedReader(new FileReader("src\\testers\\Categorization\\ADWIN_Drifts\\BINOMIAL\\" + n + "_" + p + "\\Per 0 Noise "+ noise + "\\" + numDrifts + "_" + noises[k] + "_" + perturbationVariable + "_" + leftBound + "_" + rightBound + "_" + i + ".csv"));
			BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Categorization\\Streams\\sensor.csv"));
			BufferedWriter bw = new BufferedWriter(new FileWriter("src\\testers\\Categorization\\ADWIN_Results\\real world\\delta " + deltas[j] + "\\sensor200.csv"));	    

			BufferedReader brDrift = new BufferedReader(new FileReader("src\\testers\\Categorization\\ADWIN_Drifts\\sensor.csv"));
			
			DriftCategorizer categorizer = new DriftCategorizer(deltas[j], 100, 200, 100);

			String line = "";
			
			
			
			int driftPoint = Integer.parseInt(brDrift.readLine());
			int count = 0;
			int hundred = 0;

			while ((line = br.readLine()) != null)
			{
			    count++;			
			    String[] pred = line.split(",");

				if(pred[0].equals(pred[1]))
				{
				    line = "1";
				}
				else
				{
				    line = "0";
				}
				
			    categorizer.setInput(Double.parseDouble(line));
			    //		categorizer.setInputDouble(Double.parseDouble(line));
			    if(count == driftPoint)
				//			if(count % 1000 == 0)
			    {
				hundred++;
				String l = "";
				if((l = brDrift.readLine()) != null)
				{
				    driftPoint = Integer.parseInt(l);			
				}

				String result = categorizer.evaluateDrift();
				//			System.out.println(result);
				bw.write(result + "\n");
				//			    count = 0;
				if(hundred == 100)
				{
				    hundred = 0;
				    System.out.println(categorizer.getReservoir().getReservoirMean() + "," + categorizer.getReservoir().getReservoirStdev()); 
				}
			    }
			}

			br.close();
			bw.close();
			brDrift.close();
			//Categorization
		    }
		}
	    }
	    
    
	} 
	catch (Exception e)
	{
	    System.err.println("Error");
	    e.printStackTrace();
	}

    }

}
