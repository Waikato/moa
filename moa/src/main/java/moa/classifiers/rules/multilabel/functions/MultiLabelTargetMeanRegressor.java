package moa.classifiers.rules.multilabel.functions;

import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.multitarget.BasicMultiTargetRegressor;
import moa.classifiers.rules.functions.TargetMean;
import moa.options.ClassOption;

public class MultiLabelTargetMeanRegressor extends BasicMultiTargetRegressor
		implements MultiTargetRegressor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MultiLabelTargetMeanRegressor() {
		baseLearnerOption=new ClassOption("baseLearner", 'l',
				"TargetMean", TargetMean.class, "TargetMean") ;
	}

	@Override
	public String getPurposeString() {
		return "Uses an ensemble of rules.TargetMean to perform multitarget regression.\n"
				+ "Extends BasicMultiTargetRegressor by allowing only rules.TargetMean";
	}
	
	
	
	
}
