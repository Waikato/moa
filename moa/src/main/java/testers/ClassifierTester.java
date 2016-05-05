package testers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import classifiers.thirdparty.VFDT;
import cutpointdetection.SingDetector;
import inputstream.HyperplaneGenerator;
import volatilityevaluation.VolatilityPredictionFeatureExtractor;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

public class ClassifierTester implements Tester
{
    private final int NOCORRECTION = 0;
    private final int SINE_FUNC = 1;
    private final int SIGMOID_FUNC = 2;

    int numInst = 2219803;
    int initInst = 3000; // (ECML Paper - is 10000)
    int iterations = 30;

    public void doTest()
    {
//	doTest2();
	long[] times = new long[iterations];
	double[] preds = new double[iterations];
	double[] dc = new double[iterations];
	for(int k = 0; k < iterations; k++)
	{
	    System.out.println(k);
	    long totalBuildTime = 0;
	    double predTotal = 0.0;
	    try
	    {
		//BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\SEED\\driftresults\\SEA"+(k+1)+"_drifts_online.csv"));
		//BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\covertype_driftpoints.csv"));
//		BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\SEED\\poker_seed_only.csv"));
		//BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\Hyperplane_1M.csv"));
///*				
		VolatilityPredictionFeatureExtractor extractor = new VolatilityPredictionFeatureExtractor();
		int length = 80000;
		for (int j = 8; j < 18; j++)
		{
		    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Abrupt_0.2_0.4\\v100000_Abrupt_0.2_0.4_"+ j +".csv"));
		    //BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\hyperplane_1M_pred"));

		    int c = 0;
		    String line = "";
		    while ((line = br.readLine()) != null)
		    {		
//			if(c % 100000 < 40000 || c % 100000 > 60000){continue;}
//			String[] p = line.split(",");
//			if(p[0].equals(p[1]))
//			{
//			    line = "1";
//			}
//			else
//			{
//			    line = "0";
//			}
			extractor.extract(Double.parseDouble(line));
			c++;
			if(c > length){break;}
			
		    }
		    br.close();
		}

//*/		
//		ADWIN adwin = new ADWIN(0.05);
		SingDetector sing = new SingDetector(0.05,32,1,1,0.01,0.8,75);
		sing.setTension(0.5);  
		sing.setMode(SINE_FUNC);
		int in = initInst;
		double volatility = 0.0;
		double vCount = 0.0;
		double drift = 0;
		double prevdrift = 0.0;
		
		int c = 0; 
//		int dc = 0;

		ArffLoader loader = new ArffLoader();
//		loader.setFile(new File("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\SEA\\SEA"+(k+1)+".arff"));
//		loader.setFile(new File("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\Hyperplane\\Hyperplane1M_" + k + ".arff"));
		loader.setFile(new File("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\sensor.arff"));
		Instances struc = loader.getStructure();
		struc.setClassIndex(struc.numAttributes() - 1);

		VFDT ht = new VFDT();
		ht.buildClassifier(struc);
		Instance current;

//		double relPos = 0.0;
		
		while((current = loader.getNextInstance(struc)) != null)
		{
//		    relPos = (c - prevdrift) / (drift - prevdrift);
		    
		    if(in > 0)
		    {
			long startTime = System.currentTimeMillis();
			// ht.updateClassifier(current);
			ht.addInstance(current);
			
			long endTime = System.currentTimeMillis();
			totalBuildTime = totalBuildTime + (endTime - startTime);

			in--;
		    }
		    double out = ht.classifyInstance(current);	
		    double pred = out == current.classValue() ? 1.0 : 0.0;
		    predTotal += pred;
		    
		    extractor.setInput(pred);
		    double conf = extractor.getConfidencePrediction();
		    
//		    if(sing.setInput(pred, 0.05, relPos))
		    if(sing.setInput(pred, 0.05, conf))
//		    if(sing.setInput(pred))
		    {
//			if(c - prevdrift > 200)
//			{
//			    volatility = volatility + (c - prevdrift); 
//			    vCount++;	

//			    prevdrift = c;
//			    drift = c + volatility;
//			    bw.write(c + "\n");
//			}
			in = initInst;
			ht = new VFDT();
			ht.buildClassifier(struc);
			sing = new SingDetector(0.05,32,1,1,0.01,0.8,75);
//			adwin = new ADWIN(0.05);
			sing.setTension(0.5); 
			sing.setMode(SINE_FUNC);
			dc[k]++;
		    }
		    c++;

		}
		System.out.println(dc[k]);
//		System.out.println(volatility);
//		bw.close();
	    } 
	    catch (Exception e)
	    {
		e.printStackTrace();
	    }
	    times[k] = totalBuildTime;
	    preds[k] = predTotal / numInst;
	}
	
	System.out.println("mean time: " + calculateSum(times)/iterations);
	System.out.println("variance time: " + calculateStdev(times, calculateSum(times)/iterations));
	System.out.println("mean accuracy: " + calculateSum(preds)/iterations);
	System.out.println("variance accuracy: " + calculateStdev(preds, calculateSum(preds)/iterations));
	System.out.println("mean drifts: " + calculateSum(dc)/iterations);
	System.out.println("variance drifts: " + calculateStdev(dc, calculateSum(dc)/iterations));
	
    }
/*
    public void doTest()
    {
//	doTest2();
	long[] times = new long[iterations];
	double[] preds = new double[iterations];
	for(int k = 0; k < iterations; k++)
	{
	    System.out.println(k);
	    long totalBuildTime = 0;
	    double predTotal = 0.0;
	    try
	    {
		BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\covtypeNorm_drifts.csv"));
		//BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\covertype_driftpoints.csv"));
//		BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\Hyperplane_Onlinez1.65.csv"));
		//BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\Hyperplane_1M.csv"));
		
/*		ArrayList<Double> list = new ArrayList<Double>();
		String line2 = "";
		while ((line2 = br.readLine()) != null)
		{
		    list.add(Double.parseDouble(line2));
		}
		list.add((double)numInst);
		br.close();
		double prevdrift = 0;
		double drift = list.remove(0);
*/		
//		VolatilityPredictionFeatureExtractor extractor = new VolatilityPredictionFeatureExtractor();
//		int length = 80000;
/*		for (int j = 8; j < 18; j++)
		{
		    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Abrupt_0.2_0.4\\v100000_Abrupt_0.2_0.4_"+ j +".csv"));
		    //BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\hyperplane_1M_pred"));

		    int c = 0;
		    String line = "";
		    while ((line = br.readLine()) != null)
		    {		
/*			if(c % 100000 < 40000 || c % 100000 > 60000){continue;}
			String[] p = line.split(",");
			if(p[0].equals(p[1]))
			{
			    line = "1";
			}
			else
			{
			    line = "0";
			}
			extractor.extract(Double.parseDouble(line));
			c++;
			if(c > length){break;}
			
		    }
		    br.close();
		}
*

		
		ADWIN adwin = new ADWIN(0.05);
		adwin.setTension(0.5);  
		adwin.setMode(SINE_FUNC);
		int in = initInst;
		double volatility = 0.0;
		double vCount = 0.0;
		double drift = 895;
		double prevdrift = 0.0;
		
		int c = 0;

		ArffLoader loader = new ArffLoader();
		loader.setFile(new File("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\powersupply.arff"));
		//loader.setFile(new File("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\covtypeNorm.arff"));
		//loader.setFile(new File("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\Hyperplane_1M.arff"));
		Instances struc = loader.getStructure();
		struc.setClassIndex(struc.numAttributes() - 1);

//		NaiveBayesUpdateable NB = new NaiveBayesUpdateable();
		HoeffdingTree ht = new HoeffdingTree();
//		NB.buildClassifier(struc);
		ht.buildClassifier(struc);
		Instance current;
		//	    while((current = loader.getNextInstance(struc)) != null && initInst > 0)
		//	    {
		//		NB.updateClassifier(current);
		//		initInst--;
		//		c++;
		//	    }

		double relPos = 0.0;
		
		while((current = loader.getNextInstance(struc)) != null)
		{
		    relPos = (c - prevdrift) / (drift - prevdrift);
		    
/*		    if(c == drift)
		    {

			prevdrift = drift;
			drift = list.remove(0);			
//			if(!list.isEmpty()){list.remove(0);}
		    }
*		    if(in > 0)
		    {
			long startTime = System.currentTimeMillis();
//			NB.updateClassifier(current);
			ht.updateClassifier(current);

			long endTime = System.currentTimeMillis();
			totalBuildTime = totalBuildTime + (endTime - startTime);

			in--;
		    }
//		    double out = NB.classifyInstance(current);
		    double out = ht.classifyInstance(current);	
		    double pred = out == current.classValue() ? 1.0 : 0.0;
		    predTotal += pred;
		    //		System.out.println(ht.getVotesForInstance(current));
//		    bw.write(out == current.classValue() ? "1\n" : "0\n");
		    //		if(c % 50000 == 0)
		    
//		    extractor.setInput(pred);
//		    double conf = extractor.getConfidencePrediction();
		    
		    if(adwin.setInput(pred, 0.05, relPos))
//		    if(adwin.setInput(pred, 0.05, conf))
//		    if(adwin.setInput(pred))
		    {
//			if(c - prevdrift > 200)
//			{
			    volatility = volatility + (c - prevdrift); 
			    vCount++;	
//			    volatility /= vCount;

			    prevdrift = c;
			    drift = c + volatility;
//			}
//			System.out.println(c);
			bw.write(c + "\n");
			in = initInst;
//			NB = new NaiveBayesUpdateable();
//			NB.buildClassifier(struc);
			ht = new HoeffdingTree();
			ht.buildClassifier(struc);
			adwin = new ADWIN(0.05);
			adwin.setTension(0.5); 
			adwin.setMode(SINE_FUNC);
			
		    }
		    c++;

		}
		System.out.println(volatility);
//		System.out.println(totalBuildTime);
//		System.out.println(predTotal / 1000000);
		bw.close();
	    } 
	    catch (Exception e)
	    {
		e.printStackTrace();
	    }
	    times[k] = totalBuildTime;
	    preds[k] = predTotal / numInst;
	}
	
	System.out.println("mean time: " + calculateSum(times)/iterations);
	System.out.println("variance time: " + calculateStdev(times, calculateSum(times)/iterations));
	System.out.println(preds[0]);
    }
*/    
    public void doTest2()
    {
	try
	{
	    BufferedWriter bw = new BufferedWriter (new FileWriter("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\Hyperplane_1M.arff"));
	    bw.write("@RELATION Hyperplane\n\n" +
		    "@ATTRIBUTE att1 NUMERIC\n" +
		    "@ATTRIBUTE att2 NUMERIC\n" +
		    "@ATTRIBUTE att3 NUMERIC\n" +
		    "@ATTRIBUTE att4 NUMERIC\n" +
		    "@ATTRIBUTE att5 NUMERIC\n" +
		    "@ATTRIBUTE att6 NUMERIC\n" +
		    "@ATTRIBUTE att7 NUMERIC\n" +
		    "@ATTRIBUTE att8 NUMERIC\n" +
		    "@ATTRIBUTE class {0,1}\n\n" +
		    "@DATA\n");


	    for(int j = 8; j < 18; j++)
	    {
		HyperplaneGenerator hpgen = new HyperplaneGenerator(j, 8, 4, 0.25, 0.1);
		for(int i = 0; i < 10000; i++)
		{
		    hpgen.induceDrift();
		}
		for(int i = 0; i < numInst; i++)
		{
		    bw.write(hpgen.getNextTransaction() + "\n");
		}

	    }

	    bw.close();


	} 
	catch (IOException e)
	{
	    e.printStackTrace();
	}

    }
    
    public double calculateStdev(long[] times, double mean)
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
    
    public double calculateStdev(double[] times, double mean)
    {
	double sum = 0;
	int count = 0;
	for (double i : times)
	{
	    if (i > 0)
	    {
		count++;
		sum += Math.pow(i - mean, 2);
	    }
	}
	return Math.sqrt(sum / count);
    }

    public double calculateSum(long[] delays)
    {
	double sum = 0.0;
	for (double d : delays)
	{
	    sum += d;
	}

	return sum;
    }
    
    public double calculateSum(double[] delays)
    {
	double sum = 0.0;
	for (double d : delays)
	{
	    sum += d;
	}

	return sum;
    }

}
