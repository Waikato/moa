package moa.learners.predictors.rules.core.splitcriteria;

import moa.learners.predictors.core.splitcriteria.SplitCriterion;

public interface AMRulesSplitCriterion extends SplitCriterion {

	double[] computeBranchSplitMerits(double[][] postSplitDists);

	@Override
	double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists);

	@Override
	double getRangeOfMerit(double[] preSplitDist);
}
