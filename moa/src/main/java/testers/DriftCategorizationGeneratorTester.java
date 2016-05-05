package testers;

import inputstream.DriftCategorizationStreamGenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DriftCategorizationGeneratorTester implements Tester
{
    private DriftCategorizationStreamGenerator gen;
    private BufferedWriter bWriter, bWriterSlope;;
    
    public DriftCategorizationGeneratorTester()
    {
	
	try
	{	    
	    int normalizer = 1000;
	    bWriter = new BufferedWriter(new FileWriter("src\\testers\\TypeData.txt"));
	    bWriterSlope = new BufferedWriter(new FileWriter("src\\testers\\SlopeDataTruth.txt"));
	    gen = new DriftCategorizationStreamGenerator(bWriter, bWriterSlope, 0.20, 1000, 1, 0.00, 5, 0.2/normalizer, 0.4/normalizer, 0.4/normalizer, 0.8/normalizer, 0.8/normalizer, 1.0/normalizer);
	   
	} 
	catch (IOException e)
	{
	    System.err.println("error");
	}
	
    }

    @Override
    public void doTest()
    { 
	gen.generateInput();
	try
	{
	    bWriter.close();
	    bWriterSlope.close();
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
	System.out.println("ended");

    }

}
