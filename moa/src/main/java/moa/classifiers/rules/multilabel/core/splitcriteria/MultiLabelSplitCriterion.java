package moa.classifiers.rules.multilabel.core.splitcriteria;


import moa.core.DoubleVector;
import moa.options.OptionHandler;

public interface MultiLabelSplitCriterion extends OptionHandler{
		
	double getMeritOfSplit(DoubleVector[] preSplitDist,DoubleVector[][] postSplitDists);
	
	double getRangeOfMerit(DoubleVector [] preSplitDist);

	double [] getBranchesSplitMerits(DoubleVector[][] postSplitDists);
	

}
