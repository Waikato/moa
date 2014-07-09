package moa.classifiers.rules.core.splitcriteria.multilabel;


import moa.options.OptionHandler;

public interface MultiLabelSplitCriterion extends OptionHandler{
		
	public double getMeritOfSplit(double[][] preSplitDist,double[][][] postSplitDists);
	
	public double getRangeOfMerit(double[] preSplitDist);

}
