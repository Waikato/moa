package moa.classifiers.rules.multilabel;

import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.rules.core.Rule.Builder;
import moa.classifiers.rules.core.anomalydetection.AnomalyDetector;
import moa.classifiers.rules.core.RuleActiveLearningNode;
import moa.classifiers.rules.multilabel.core.voting.ErrorWeightedVoteMultiLabel;
import moa.options.ClassOption;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

public class AMRulesMultiTargetRegressor extends AMRulesMultiLabelLearner implements MultiTargetRegressor{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	


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
	public boolean isRandomizable() {
		// TODO Auto-generated method stub
		return false;
	}


}
