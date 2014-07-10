package moa.classifiers.rules.multilabel.core.splitcriteria;


import moa.options.OptionHandler;

public interface MultiLabelSplitCriterion extends OptionHandler{
		
	public double getMeritOfSplit(double[][] preSplitDist,double[][][] postSplitDists);
	
	public double getRangeOfMerit(double[] preSplitDist);

}
