package moa.classifiers.rules.multilabel.core.splitcriteria;

import moa.classifiers.rules.core.Utils;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;



public class MultiTargetVarianceRatio extends AbstractOptionHandler implements MultiLabelSplitCriterion{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public double getMeritOfSplit(DoubleVector[] preSplitDist, DoubleVector[][] postSplitDists) {
		double error=0;
		int numOutputs=preSplitDist.length;
		for (int i=0; i<numOutputs; i++)
			error+=getMeritOfSplitForOutput(preSplitDist,postSplitDists,i);
		return error/numOutputs;
	}
	
	public double[] getBranchSplitVarianceOutput(DoubleVector[] postSplitDists) {
		double[] variances = new double[postSplitDists.length];
		for(int i = 0; i < postSplitDists.length; i++)
			variances[i]=Utils.computeVariance(postSplitDists[i]);
		return variances;

	}
	

	@Override
	public double [] getBranchesSplitMerits(DoubleVector[][] postSplitDists){
		int numOutputs=postSplitDists.length;
		int numBranches=postSplitDists[0].length;
		double [] merits=new double[numBranches];
			for(int j=0; j<numOutputs;j++)
			{
				double [] branchMeritsOutput=getBranchSplitVarianceOutput(postSplitDists[j]);
				/* double weightTotal=0;
				for (int i=0; i<numBranches;i++){
					weightTotal+=postSplitDists[j][i].getValue(0)/weightTotal*postSplitDists[j][i].getValue(0);
				}
				for (int i=0; i<numBranches;i++){
					merits[i]+=postSplitDists[j][i].getValue(0)/weightTotal*branchMeritsOutput[i];
				}*/
				
				for (int i=0; i<numBranches;i++){
					if (postSplitDists[j][i].getValue(0)>0)
						merits[i]-=branchMeritsOutput[i];
					else
						merits[i]=-Double.MAX_VALUE;
				}
			}
		return merits;
	}
	
	protected double getMeritOfSplitForOutput(DoubleVector[] preSplitDist, DoubleVector[][] postSplitDists, int outputAttributeIndex){
		return getMeritOfSplitForOutput(preSplitDist[outputAttributeIndex],postSplitDists[outputAttributeIndex]);
	}

	protected double getMeritOfSplitForOutput(DoubleVector preSplitDist, DoubleVector[] postSplitDists) {
		double merit=0;
		//count number of branches with weightSeen higher than threshold
		int count = 0; 
		for(int i = 0; i < postSplitDists.length; i++)
			if(postSplitDists[i].getValue(0) >=0.05*preSplitDist.getValue(0))
				count = count +1;
		//Consider split if all branches have required weight seen
		if(count == postSplitDists.length){
			double varPreSplit=Utils.computeVariance(preSplitDist);
			double sumVarPostSplit=0;
			double weightTotal=0;
			for (int i=0; i<postSplitDists.length;i++){
				weightTotal+=postSplitDists[i].getValue(0);
			}
			double [] variances=getBranchSplitVarianceOutput(postSplitDists);
			for(int i = 0; i < variances.length; i++)
				if(postSplitDists[i].getValue(0)>0)
					sumVarPostSplit+=(postSplitDists[i].getValue(0)/weightTotal*variances[i]);  //weight variance
			merit=(1-sumVarPostSplit/varPreSplit);
		}
		/*if(merit<0 || merit>1)
			System.out.println("out of range");*/
		return merit;
	}

	public double getRangeOfMerit(DoubleVector [] preSplitDist) {
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
