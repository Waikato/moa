package lin.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.DoublePredicate;

import cutpointdetection.ADWIN;
import inputstream.*;
import volatilityevaluation.RelativeVolatilityDetector;
public class Main {

	public static void main(String[] args) throws IOException 
	{
		testMultipleDriftBernoulliDistributionGenerator();
		testRelativeVolatilityDetector();
		//testADWINMultipleDrift();
	}
	
	public static void testMultipleDriftBernoulliDistributionGenerator()
	{
		MultipleDriftBernoulliDistributionGenerator mgen = new MultipleDriftBernoulliDistributionGenerator();
		mgen.generateInput(0.5, 50, 115, 100000, false, "MultipleDriftBernoulliDistributionGeneratorRes.csv");
		mgen.generateInput(0.5, 100, 115, 100000, true, "MultipleDriftBernoulliDistributionGeneratorRes.csv");
	}
	
	public static void testBerGen() throws IOException
	{
		double[] means = {0.2};
		BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(means, 100, 23121);
		BufferedWriter bWriter = new BufferedWriter(new FileWriter("binary.csv"));
		while (gen.hasNextTransaction())
		{
			
			bWriter.write((gen.getNextTransaction()) + "\n");
		}
		bWriter.close();
	}
	
	public static void testADWIN()
	{
		ADWIN adwin = new ADWIN(0.000000001);
		
		double[] mean1 = {0.2};
		double[] mean2 = {0.9};
		int count = 0;
		BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(mean1, 100, 23121);
		while (gen.hasNextTransaction())
		{
			
			if(adwin.setInput(Integer.parseInt(gen.getNextTransaction())))
			{
				System.out.println("Drift" + count);
			}
			else
			{
				System.out.println(count);
			}
			count++;
			
		}
		System.out.println("****************************************");
		BernoulliDistributionGenerator gen2 = new BernoulliDistributionGenerator(mean2, 100, 321);
		while (gen2.hasNextTransaction())
		{
			if(adwin.setInput(Integer.parseInt(gen2.getNextTransaction())))
			{
				System.out.println("Drift" + count);
			}
			else
			{
				System.out.println(count);
			}
			count++;
		}
		
		System.out.println("Drift Count: " + adwin.getNumberDetections());
	}
	
	public static void testMixedDriftBernoulliGenerator()
	{
		MixedDriftBernoulliGenerator megen = new MixedDriftBernoulliGenerator();
		megen.generateInput(0.6, 10, 23, "MixedDriftBernoulliGeneratorOutPut.csv", 10000000);
		megen.generateInput(0.9, 10, 78673, "MixedDriftBernoulliGeneratorOutPut.csv", 10000);
	}
	
	public static void testADWINMultipleDrift() throws IOException
	{
		ADWIN adwin = new ADWIN(1.0);
		BufferedReader reader = new BufferedReader(new FileReader(new File("MultipleDriftBernoulliDistributionGeneratorRes.csv")));
		
		int count = 0;
		int predriftpos = 0;
		String line = "";
		int bit = 0;
		int driftcount = 0;

		while((line=reader.readLine())!=null)
		{
			bit = Integer.parseInt(line);
			if(adwin.setInput(bit))
			{
				System.out.println("Drift Interval: " + (count-predriftpos));
				
				driftcount++;
				predriftpos = count;
				System.out.println("Drift" + count);
			}
			else
			{
				//System.out.println(count);
			}
			count++;
		}
		System.out.println("Drift Count: " + adwin.getNumberDetections());
		reader.close();
	}
	
	public static void testRelativeVolatilityDetector() throws NumberFormatException, IOException
	{
		ADWIN cutpointdetector = new ADWIN();
		RelativeVolatilityDetector rvd =  new RelativeVolatilityDetector(cutpointdetector, 30, 0.25);
		int count = 0;
		
		BufferedReader reader = new BufferedReader(new FileReader(new File("MultipleDriftBernoulliDistributionGeneratorRes.csv")));
		
		String line = "";
		int bit = 0;
		int driftcount = 0;
		while((line=reader.readLine())!=null)
		{
			bit = Integer.parseInt(line);
			if(rvd.setInputVar(bit))
			{
				driftcount++;
				System.out.println("Volatility Drift" + count);
			}
			else
			{
				//System.out.println(count);
			}
			count++;
		}
		System.out.println("Volatility Drift Count: " + driftcount);
		reader.close();
		
	}

}
