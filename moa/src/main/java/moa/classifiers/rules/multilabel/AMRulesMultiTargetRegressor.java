package moa.classifiers.rules.multilabel;

import moa.classifiers.MultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.rules.multilabel.core.MultiLabelRule;
import moa.classifiers.rules.multilabel.core.MultiLabelRuleRegression;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiTargetVarianceRatio;
import moa.classifiers.rules.multilabel.core.voting.ErrorWeightedVoteMultiLabel;
import moa.classifiers.rules.multilabel.core.voting.InverseErrorWeightedVoteMultiLabel;
import moa.classifiers.rules.multilabel.errormeasurers.MultiLabelErrorMeasurer;
import moa.classifiers.rules.multilabel.errormeasurers.RelativeMeanAbsoluteDeviationMT;
import moa.classifiers.rules.multilabel.functions.AdaptiveMultiTargetRegressor;
import moa.options.ClassOption;

public class AMRulesMultiTargetRegressor extends AMRulesMultiLabelLearner implements MultiTargetRegressor{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public  AMRulesMultiTargetRegressor(){
		splitCriterionOption = new ClassOption("splitCriterionOption", 'S',
				"Split criterion used to assess the merit of a split", MultiLabelSplitCriterion.class, MultiTargetVarianceRatio.class.getName()) ;

		 weightedVoteOption = new ClassOption("weightedVoteOption",
					'w', "Weighted vote type", 
					ErrorWeightedVoteMultiLabel.class,
					InverseErrorWeightedVoteMultiLabel.class.getName());
		 
		 learnerOption = new ClassOption("learnerOption",
					'L', "Learner", 
					MultiLabelLearner.class,
					AdaptiveMultiTargetRegressor.class.getName());
		 
		 errorMeasurerOption = new ClassOption("errorMeasurer", 'e',
					"Measure of error for deciding which learner should predict.", MultiLabelErrorMeasurer.class, RelativeMeanAbsoluteDeviationMT.class.getName()) ;

	}
	
	@Override
	public ErrorWeightedVoteMultiLabel newErrorWeightedVote(){
		return (ErrorWeightedVoteMultiLabel)((ErrorWeightedVoteMultiLabel) getPreparedClassOption(weightedVoteOption)).copy();
		
	}

	@Override
	protected MultiLabelRule newDefaultRule() {
		return new MultiLabelRuleRegression(1);
	}

	public AMRulesMultiTargetRegressor(double attributesPercentage) {
		super(attributesPercentage);
	}


	
}
