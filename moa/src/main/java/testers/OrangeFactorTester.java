package testers;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import cutpointdetection.PHT;
import cutpointdetection.SingDetector;


public class OrangeFactorTester implements Tester
{

   
    @Override
    public void doTest()
    {
	String BugID = "888164";
	
	
	try
	{
	    SingDetector seed = new SingDetector(0.05, 32, 1, 1, 0.001, 0.8, 75);
	    PHT pht = new PHT(12);
	    
	    // Read input file
	    BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\888164.csv"));
	    BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\ACER\\Desktop\\888164_minute.txt"));
	    String line = "";
	    
	    int t = 0;
	    int offset = 60;
	    int currentDay = 1372377600 / offset; //start time
	    int tsCount = 0;
	    //boolean drift = false;
	    
	    while((line = br.readLine()) != null)
	    {
		t++;
		String[] lineArr = line.split(",");
		
		
		if (lineArr[14].equals(BugID))
		{
		    int day = Integer.parseInt(lineArr[10]) / offset;
		    if(day == currentDay)
		    {
			tsCount++;
//			System.out.println("1: " + tsCount);
		    }
		    else if(day > currentDay)
		    {
//			System.out.println("2: " + tsCount);
			bw.write("" + tsCount + "\n");
			//bw.write("" + convertUnixtime(currentDay * offset) + "," + tsCount + "\n");
			currentDay++;
			while(day > currentDay)
			{
			    bw.write("" + "0" + "\n");
			    //bw.write("" + convertUnixtime(currentDay * offset) + "," + "0" + "\n");
			    currentDay++;
			}
			tsCount = 1;
		    }
		}
	    }
//	    int p = 0;
//	    while(p < 50)
//	    {
//		bw.write("0" + "\n");
//		p++;
//	    }
	    
	    bw.close();
	    br.close();
	    
	    // Process rows and determine 
//	    System.out.println("test");
	    BufferedReader test = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\888164_minute.txt"));
	    String line2 = "";
	    int c = 0;
	    
	    while((line2 = test.readLine()) != null)
	    {
//		System.out.println("test");
		c++;
		double input = Double.parseDouble(line2);
//		System.out.println(input);
		boolean drift = seed.setInput(input);
		if(drift)
		{
		    System.out.println(c);
		}
	    }
	    
	    test.close();
	    
	    
	} 
	catch (Exception e)
	{
	    e.printStackTrace();
	}

    }

    
    public String convertUnixtime(long unixSeconds)
    {
	//long unixSeconds = 1434974830;
	Date date = new Date(unixSeconds*1000L); // *1000 is to convert seconds to milliseconds
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm"); // the format of your date ("yyyy-MM-dd HH:mm:ss z")
	sdf.setTimeZone(TimeZone.getTimeZone("GMT-0")); // give a timezone reference for formating (see comment at the bottom
	String formattedDate = sdf.format(date);
	return formattedDate;
    }
}
