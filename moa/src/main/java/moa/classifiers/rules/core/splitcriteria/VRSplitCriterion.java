package moa.classifiers.rules.core.splitcriteria;

import moa.classifiers.core.splitcriteria.VarianceReductionSplitCriterion;

public class VRSplitCriterion extends VarianceReductionSplitCriterion implements AMRulesSplitCriterion{

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
			SDR[i] = (Ni/N)*computeSD(postSplitDists[i]);
		}
		return SDR;

	}
	
    @Override
    public double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists) {
        double SDR=0.0;
    	double N = preSplitDist[0];
    	int count = 0; 
    	
    	for(int i = 0; i < postSplitDists.length; i++)
    	{
    		double Ni = postSplitDists[i][0];
    		if(Ni >=0.05*N){
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

}
