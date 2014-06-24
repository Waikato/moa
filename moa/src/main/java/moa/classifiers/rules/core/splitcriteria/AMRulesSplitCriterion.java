package moa.classifiers.rules.core.splitcriteria;

import moa.classifiers.core.splitcriteria.SplitCriterion;

public interface AMRulesSplitCriterion extends SplitCriterion{

	public double[] computeBranchSplitMerits(double[][] postSplitDists);
	
	public double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists);
	
	public double getRangeOfMerit(double[] preSplitDist);
}
