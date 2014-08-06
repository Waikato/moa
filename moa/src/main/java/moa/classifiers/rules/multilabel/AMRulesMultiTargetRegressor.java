package moa.classifiers.rules.multilabel;

import moa.classifiers.MultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.rules.multilabel.core.MultiLabelRule;
import moa.classifiers.rules.multilabel.core.MultiLabelRuleRegression;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.rules.multilabel.core.voting.ErrorWeightedVoteMultiLabel;
import moa.classifiers.rules.multilabel.errormeasurers.MultiLabelErrorMeasurer;
import moa.options.ClassOption;

public class AMRulesMultiTargetRegressor extends AMRulesMultiLabelLearner implements MultiTargetRegressor{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public  AMRulesMultiTargetRegressor(){
		splitCriterionOption = new ClassOption("splitCriterionOption", 's',
				"Split criterion used to assess the merit of a split", MultiLabelSplitCriterion.class, "MultiTargetVarianceRatio") ;

		 weightedVoteOption = new ClassOption("weightedVoteOption",
					'w', "Weighted vote type", 
					ErrorWeightedVoteMultiLabel.class,
					"UniformWeightedVoteMultiLabel");
		 
		 learnerOption = new ClassOption("learnerOption",
					'L', "Learner", 
					MultiLabelLearner.class,
					"moa.classifiers.rules.multilabel.functions.AdaptiveMultiTargetRegressor");
		 
		 errorMeasurerOption = new ClassOption("errorMeasurer", 'e',
					"Measure of error for deciding which learner should predict.", MultiLabelErrorMeasurer.class, "MeanAbsoluteDeviationMT") ;

	}
	
	@Override
	public ErrorWeightedVoteMultiLabel newErrorWeightedVote(){
		return (ErrorWeightedVoteMultiLabel)((ErrorWeightedVoteMultiLabel) getPreparedClassOption(weightedVoteOption)).copy();
		
	}

	@Override
	protected MultiLabelRule newDefaultRule() {
		return new MultiLabelRuleRegression(1);
	}


	
}
