package cutpointdetection.OnePassDetector;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
 

/**
 *
 * @author ssakthit
 */
import java.io.BufferedReader;
import java.io.InputStreamReader;
public class GenerateData
{
public static void main(String args[])
{
    
}
public void run()
{
    	int iNoOfExperiments = 1;
	double dVariance = 0.0;

	try
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String sInput= "";
		
		System.out.print("Enter the distribution 1)Normal [1], 2)Bernoulli [2] :");
		int iDistribution = Integer.parseInt(br.readLine());

		System.out.print("Enter the number of instances for each concept :");
		sInput = br.readLine();
		int iInstances  = Integer.parseInt(sInput);
		if( iDistribution == 1)
		{
			System.out.print("Enter variance :");
			sInput = br.readLine();
			dVariance = Double.parseDouble(sInput);
		}
		else if(iDistribution == 2)
		{
			iNoOfExperiments  = 1;
	
		}
		System.out.print("ENter the number of drift points :");
 		sInput = br.readLine();
		int iDriftPoints = Integer.parseInt(sInput) + 1;
		System.out.println("\n");

		iInstances = iInstances * iDriftPoints;

		double[] dMeans = new double[iDriftPoints];
		
		for(int i=iDriftPoints;i> 0 ; i--)
		{
			
			System.out.print("Enter mean :");
			sInput= br.readLine();
			dMeans[iDriftPoints -i]= Double.parseDouble(sInput);
		}

		if(iDistribution == 1)
		{		
			NormalDistribution normal = new NormalDistribution(dMeans,dVariance);
			normal.generate(iInstances,iDriftPoints);
		}
		else if(iDistribution == 2)
		{
			BinomialDistribution binomial = new BinomialDistribution(dMeans, iNoOfExperiments,"data.txt");
			binomial.generate(iInstances, iDriftPoints);
		}
	}
	catch(Exception e)
	{
		System.out.println(e);
	}
}
}