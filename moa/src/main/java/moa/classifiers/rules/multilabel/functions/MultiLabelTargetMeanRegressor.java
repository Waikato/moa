package moa.classifiers.rules.multilabel.functions;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.multitarget.BasicMultiTargetRegressor;
import moa.classifiers.rules.functions.Perceptron;
import moa.classifiers.rules.functions.TargetMean;
import moa.options.ClassOption;

public class MultiLabelTargetMeanRegressor extends BasicMultiTargetRegressor
		implements MultiTargetRegressor, AMRulesFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void init(){
		baseLearnerOption=new ClassOption("baseLearner", 'l',
				"TargetMean", TargetMean.class, "TargetMean") ;
	}

	@Override
	public String getPurposeString() {
		return "Uses an ensemble of rules.TargetMean to perform multitarget regression.\n"
				+ "Extends BasicMultiTargetRegressor by allowing only rules.TargetMean";
	}

	@Override
	public void resetWithMemory() {
		for (int i = 0; i < this.ensemble.length; i++) {
			TargetMean tm=new TargetMean((TargetMean)this.ensemble[i]);
			double mean=tm.getVotesForInstance((Instance)null)[0];
			tm.reset(mean, 1);
			this.ensemble[i] = tm;
		}
	}
	
	
	
	
}
