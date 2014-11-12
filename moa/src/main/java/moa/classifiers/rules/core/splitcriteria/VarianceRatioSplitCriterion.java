package moa.classifiers.rules.core.splitcriteria;

import moa.classifiers.core.splitcriteria.VarianceReductionSplitCriterion;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class VarianceRatioSplitCriterion extends AbstractOptionHandler implements AMRulesSplitCriterion{

	private static final long serialVersionUID = -1066043659710778883L;

	public double[] computeBranchSplitMerits(double[][] postSplitDists) {
		double[] SDR = new double[postSplitDists.length];
		double N = 0;

		for(int i = 0; i < postSplitDists.length; i++)
		{
			double Ni = postSplitDists[i][0];
			N += Ni;
		}
		for(int i = 0; i < postSplitDists.length; i++)
		{
			double Ni = postSplitDists[i][0];
			//SDR[i] = Ni/N*VarianceReductionSplitCriterion.computeSD(postSplitDists[i]);
			SDR[i] = VarianceReductionSplitCriterion.computeSD(postSplitDists[i]);
		}
		return SDR;

	}
	
	@Override
	public double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists) {

	    	double N = preSplitDist[0];
	    	int count = 0; 
	    	
	    	for(int i = 0; i < postSplitDists.length; i++)
	    	{
	    		double Ni = postSplitDists[i][0];
	    		if(Ni >=0.05*preSplitDist[0]){
	    			count = count +1;
	    		}
	    	}
	    	
	    	if(count == postSplitDists.length){
	    		double SDR = VarianceReductionSplitCriterion.computeSD(preSplitDist);
	    		double sumPostSDR=0;
	    		for(int i = 0; i < postSplitDists.length; i++)
	        	{
	        		double Ni = postSplitDists[i][0];
	        		sumPostSDR += (Ni/N)*VarianceReductionSplitCriterion.computeSD(postSplitDists[i]);
	        		//sumPostSDR += VarianceReductionSplitCriterion.computeSD(postSplitDists[i]);
	        	}
	    		//SDR=1-(sumPostSDR)/SDR;
	    		return (1-(sumPostSDR/postSplitDists.length)/SDR);
	    	}
	    	else
	    		return -Double.MAX_VALUE;
	    	
	}

	@Override
	public double getRangeOfMerit(double[] preSplitDist) {
		return 1;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}


	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		
	}

}
