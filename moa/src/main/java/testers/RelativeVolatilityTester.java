package testers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import volatilityevaluation.RelativeVolatilityDetector;
import cutpointdetection.ADWIN;
import cutpointdetection.SingDetector;

public class RelativeVolatilityTester implements Tester
{

    @Override
    public void doTest()
    {
	try
	{
	    ADWIN adwin = new ADWIN(0.05);
	    SingDetector sing = new SingDetector(0.05, 32, 1, 1, 0.0025, 0.2, 75);

	    RelativeVolatilityDetector rvDet = new RelativeVolatilityDetector(sing, 32, 0.858);

	    // BufferedReader br = new BufferedReader(new
	    // FileReader("src\\testers\\VolatilityStream\\" + 3 + "_to_" + 5 +
	    // "\\VolatilityStream_" + 3 + "_to_" + 5 + "_" + k + ".csv"));
	    BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\poker.csv"));
	    BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\ACER\\Desktop\\poker_testvpts.csv"));

	    String line = "";
	    int c = 0;
	    
	    while ((line = br.readLine()) != null)
	    {
		String[] pred = line.split(",");

		if(pred[0].equals(pred[1]))
		{
		    line = "1";
		}
		else
		{
		    line = "0";
		}
		
		if(rvDet.setInputVar(Double.parseDouble(line)))
		{
		    bw.write(c + "\n");
		}
		
		c++;
	    }
	    
	    bw.close();
	} 
	catch (Exception e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

}
