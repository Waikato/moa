package moa.classifiers.core.splitcriteria;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.FloatOption;
import moa.tasks.TaskMonitor;
//import weka.core.Utils;
//import samoa.instances.Instance;
import weka.core.Instance;

public class SDRSplitCriterion extends AbstractOptionHandler implements SplitCriterion {

    private static final long serialVersionUID = 1L;

/*    @Override
    public double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists) {
   
    	double N = preSplitDist[0];
    	double SDR = computeSD(preSplitDist);

    //	System.out.print("postSplitDists.length"+postSplitDists.length+"\n");
    	for(int i = 0; i < postSplitDists.length; i++)
    	{
    		double Ni = postSplitDists[i][0];
    		SDR -= (Ni/N)*computeSD(postSplitDists[i]);
    	}

        return SDR;
    }*/
    
    @Override
    public double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists) {
        double SDR=0.0;
    	double N = preSplitDist[0];
    	int count = 0; 
    	
    	for(int i = 0; i < postSplitDists.length; i++)
    	{
    		double Ni = postSplitDists[i][0];
    		if(Ni >=5.0){
    			count = count +1;
    		}
    	}
    	
    	if(count == postSplitDists.length){
    		SDR = computeSD(preSplitDist);
    		for(int i = 0; i < postSplitDists.length; i++)
        	{
        		double Ni = postSplitDists[i][0];
        		SDR -= (Ni/N)*computeSD(postSplitDists[i]);
        	}
    	}
    	return SDR;
    }
    	


    @Override
    public double getRangeOfMerit(double[] preSplitDist) {
        return 1;
    }

    public static double computeSD(double[] dist) {
       
    	int N = (int)dist[0];
        double sum = dist[1];
        double sumSq = dist[2];
     //   return Math.sqrt((sumSq - ((sum * sum)/N))/N);
        return (sumSq - ((sum * sum)/N))/N;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // TODO Auto-generated method stub
    }
}
