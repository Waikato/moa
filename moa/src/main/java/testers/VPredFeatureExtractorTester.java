package testers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

import cutpointdetection.ADWIN;

import volatilityevaluation.VolatilityPredictionFeatureExtractor;

public class VPredFeatureExtractorTester implements Tester
{

    @Override
    public void doTest()
    {
	try
	{
	    BufferedWriter bw = new BufferedWriter(new FileWriter("D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\FeatureExtractor\\test7.csv"));
	    VolatilityPredictionFeatureExtractor extractor = new VolatilityPredictionFeatureExtractor();
	   
	    for(int k = 15; k < 16; k++)
	    {
		int c = 0;
		ADWIN adwin = new ADWIN(0.05);
		adwin.setTension(0.5);
		adwin.setMode(1);
		double relPos = 0.5;
		extractor.reset();
		int numInst = 110000;
		//BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Abrupt_0.2_0.4\\v100000_Abrupt_0.2_0.4_"+k+".csv"));
		//BufferedReader brTrain = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Abrupt_0.2_0.4\\v100000_Abrupt_0.2_0.4_"+(k+1)+".csv"));
		BufferedReader br = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_0.0004\\100000_0.0004_"+k+".csv"));
		BufferedReader brTrain = new BufferedReader(new FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_0.0004\\100000_0.0004_"+(k+1)+".csv"));

		String lineT = "";
		int t = 0;
		while ((lineT = brTrain.readLine()) != null)
		{
		    if(t > 10000 && t < 90000)
		    {
			extractor.extract(Double.parseDouble(lineT));		    
			//bw.write(c + "," + extractor.getStats());
		    }
		    t++;
		}
		
		String line = "";
		while ((line = br.readLine()) != null)
		{
		    extractor.setInput(Double.parseDouble(line));
		    //if(numInst > 20000 && numInst < 100000)
		    //{
			//extractor.extract(Double.parseDouble(line));		    
			//bw.write(c + "," + extractor.getStats());
		    //}
		    //if(numInst < 0){break;}
		    
		    if(extractor.getConfidencePrediction() > 0.0)
		    {
			bw.write(c + "," + "1" + "\n");
			if(adwin.setInput(Double.parseDouble(line), 0.05, relPos))
			{
			    //bw.write(c + "," + extractor.getStats());			    
			    break;
			}
		    }
		    else
		    {
			bw.write(c + "," + "0" + "\n");
			if(adwin.setInput(Double.parseDouble(line), 0.05, 1.0))
			{
			    //bw.write(c + "," + extractor.getStats());
			    
			    break;
			}
		    }
		    
		    c++;
		    numInst--;
		}		
		br.close();
		brTrain.close();
	    }
	    bw.close();
	    

	} 
	catch (Exception e)
	{
	    e.printStackTrace();
	}
	

    }

}
