package moa.classifiers.rules;

import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.rules.core.Rule.Builder;
import moa.classifiers.rules.core.RuleActiveLearningNode;
import moa.classifiers.rules.core.voting.multilabel.ErrorWeightedVoteMultiLabel;

import com.yahoo.labs.samoa.instances.Instance;

public class AMRulesMultiTargetRegressor extends AMRulesMultiLabelLearner implements MultiTargetRegressor{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean isRandomizable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RuleActiveLearningNode newRuleActiveLearningNode(Builder builder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RuleActiveLearningNode newRuleActiveLearningNode(
			double[] initialClassObservations) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ErrorWeightedVoteMultiLabel newErrorWeightedVote() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		// TODO Auto-generated method stub

	}

}
