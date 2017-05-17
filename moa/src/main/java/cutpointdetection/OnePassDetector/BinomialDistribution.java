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
class BinomialDistribution
{
double[] d_Mean;
int i_noOfExperiments;
String s_FileName;
int i_driftID;

private Random random;
BinomialDistribution(double[] _dMean,int _iNoOfExperiments, String _sFileName)
{
	d_Mean = _dMean;
        i_noOfExperiments = _iNoOfExperiments;
	random = new Random();
	i_driftID = 0;
	s_FileName = _sFileName;
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
			System.out.println("Next Mean (probability) is 	:"+d_Mean[i_driftID]);	
		}
		if((iIndex % (_iInstances/(_iNumDrifts))) == 0)
		{
			i_driftID++;
			System.out.println("Next Mean is 	:"+d_Mean[i_driftID]);		
		}
		
		
		b1.write(getBinomial(i_noOfExperiments,d_Mean[i_driftID])+"\n");	
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
public int getBinomial(int n, double p) {
  int x = 0;
  for(int i = 0; i < n; i++) {
    if(Math.random() < p)
      x++;
  }
  return x;
}

public static void main(String args[])
{
double dmeans[] = new double[3];
dmeans[0] = 0.3;
//BinomialDistribution bd = new BinomialDistribution(dmeans,1);
//bd.generate(100000,1);
}


}