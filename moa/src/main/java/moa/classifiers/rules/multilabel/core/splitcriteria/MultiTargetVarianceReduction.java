package moa.classifiers.rules.multilabel.core.splitcriteria;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;



public class MultiTargetVarianceReduction extends AbstractOptionHandler implements MultiLabelSplitCriterion{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public double getMeritOfSplit(double[][] preSplitDist, double[][][] postSplitDists) {
		double error=0;
		int numOutputs=preSplitDist.length;
		for (int i=0; i<numOutputs; i++)
			error+=getMeritOfSplitForOutput(preSplitDist,postSplitDists,i);

		return error/numOutputs;
	}
	
	public double[] getBranchSplitMerits(double[] preSplitDist, double[][] postSplitDists) {
		double[] merits = new double[postSplitDists.length];
		for(int i = 0; i < postSplitDists.length; i++)
			merits[i]= computeVariance(postSplitDists[i]);
		return merits;

	}
	
	protected double getMeritOfSplitForOutput(double[][] preSplitDist, double[][][] postSplitDists, int outputAttributeIndex){
		return getMeritOfSplitForOutput(preSplitDist[outputAttributeIndex],postSplitDists[outputAttributeIndex]);
	}

	protected double getMeritOfSplitForOutput(double[] preSplitDist, double[][] postSplitDists) {
		double merit=0;
		//count number of branches with weightSeen higher than threshold
		int count = 0; 
		for(int i = 0; i < postSplitDists.length; i++)
			if(postSplitDists[i][0] >=0.05*preSplitDist[0])
				count = count +1;
		//Consider split if all branches have required weight seen
		if(count == postSplitDists.length){
			double varPreSplit= computeVariance(preSplitDist);
			double sumVarPostSplit=0;
			double [] merits=getBranchSplitMerits(preSplitDist, postSplitDists);
			for(int i = 0; i < merits.length; i++)
				sumVarPostSplit+=merits[i];
			merit=(1-sumVarPostSplit/varPreSplit);
		}
		return merit;
	}

	public double getRangeOfMerit(double[] preSplitDist) {
		return 1;
	}

	protected double computeVariance(double count, double sum, double sumSquares)
	{
		return (sumSquares - ((sum * sum)/count))/count;
	}

	protected double computeVariance(double [] statistics)
	{
		return computeVariance(statistics[0],statistics[1],statistics[2]);
	}


	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {		
	}




}
