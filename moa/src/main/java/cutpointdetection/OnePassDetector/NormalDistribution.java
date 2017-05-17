package cutpointdetection.OnePassDetector;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
 

/**
 *
 * @author ssakthit
 */
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;
class NormalDistribution
{
double[] d_Mean;
double d_Variance;
String s_FileName;
int i_driftID;

private Random random;
NormalDistribution(double[] _dMean, double _dVariance)
{
	d_Mean = _dMean;
	d_Variance = _dVariance;
	random = new Random();
	i_driftID = 0;
	s_FileName = "data.txt";	
}
public void generate(int _iInstances,int _iNumDrifts)
{
	try
	{
	BufferedWriter b1 = new BufferedWriter(new FileWriter(s_FileName));
	for(int iIndex=1; iIndex <= _iInstances;iIndex++)
	{
		if(iIndex == 1)
		{
			System.out.println("Next Mean is 	:"+d_Mean[i_driftID]);	
		}
		if((iIndex % (_iInstances/(_iNumDrifts))) == 0)
		{
			i_driftID++;
			System.out.println("Next Mean is 	:"+d_Mean[i_driftID]);		
		}
		
		
		b1.write(getGaussian()+"\n");	
		//System.out.println(getGaussian());
	}
	b1.close();
}
catch(Exception e)
{
	
}
}
private void write()
{
	
}
private double getGaussian()
{
	return d_Mean[i_driftID] + random.nextGaussian()*Math.sqrt(d_Variance);
}
}
