package cutpointdetection.OnePassDetector;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;
class PoissonDistribution
{
double d_Lambda[];
String s_FileName;
int i_driftID;

private Random random;
PoissonDistribution(double[] _dLambda)
{
	d_Lambda = _dLambda;
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
			System.out.println("Next Lambda is 	:"+d_Lambda[i_driftID]);	
		}
		if((iIndex % (_iInstances/(_iNumDrifts))) == 0)
		{
			i_driftID++;
			System.out.println("Next lambda is 	:"+d_Lambda[i_driftID]);		
		}
		
		
		b1.write(getPoisson(d_Lambda[i_driftID])+"\n");	
	}
	b1.close();
}
catch(Exception e)
{
	System.out.println(e);
}
}
private void write()
{
	
}

private static int getPoisson(double lambda) {
  double L = Math.exp(-lambda);
  double p = 1.0;
  int k = 0;

  do {
    k++;
    p *= Math.random();
  } while (p > L);

  return k - 1;
}

public static void main(String args[])
{
double dLambda[] = new double[2];
dLambda[0] = 2.0;
dLambda[1] = 3.0;
PoissonDistribution pDist = new PoissonDistribution(dLambda);
pDist.generate(1000,1);
}
}