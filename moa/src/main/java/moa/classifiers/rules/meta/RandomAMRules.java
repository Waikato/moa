package moa.classifiers.rules.meta;

import moa.classifiers.Regressor;
import moa.classifiers.rules.multilabel.AMRulesMultiLabelLearner;
import moa.classifiers.rules.multilabel.meta.MultiLabelRandomAMRules;
import moa.options.ClassOption;

public class RandomAMRules extends MultiLabelRandomAMRules implements Regressor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RandomAMRules() {
		super();
		baseLearnerOption = new ClassOption("baseLearner", 'l', "Classifier to train.", AMRulesMultiLabelLearner.class, "AMRulesMultiTargetRegressor"); 
	}

}
