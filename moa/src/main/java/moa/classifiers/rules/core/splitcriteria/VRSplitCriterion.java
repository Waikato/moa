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

}
