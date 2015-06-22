package moa.classifiers.rules.multilabel;

import moa.classifiers.MultiLabelClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.rules.multilabel.core.MultiLabelRule;
import moa.classifiers.rules.multilabel.core.MultiLabelRuleClassification;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiTargetVarianceRatio;
import moa.classifiers.rules.multilabel.core.voting.ErrorWeightedVoteMultiLabel;
import moa.classifiers.rules.multilabel.core.voting.UniformWeightedVoteMultiLabel;
import moa.classifiers.rules.multilabel.errormeasurers.MultiLabelErrorMeasurer;
import moa.classifiers.rules.multilabel.errormeasurers.RelativeMeanAbsoluteDeviationMT;
import moa.classifiers.rules.multilabel.functions.MultiLabelNaiveBayes;
import moa.options.ClassOption;

public class AMRulesMultiLabelClassifier extends AMRulesMultiLabelLearner
		implements MultiLabelClassifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public  AMRulesMultiLabelClassifier(){
		splitCriterionOption = new ClassOption("splitCriterionOption", 'S',
				"Split criterion used to assess the merit of a split", MultiLabelSplitCriterion.class, MultiTargetVarianceRatio.class.getName()) ;

		 weightedVoteOption = new ClassOption("weightedVoteOption",
					'w', "Weighted vote type", 
					ErrorWeightedVoteMultiLabel.class,
					UniformWeightedVoteMultiLabel.class.getName());
		 
		 learnerOption = new ClassOption("learnerOption",
					'L', "Learner", 
					MultiLabelClassifier.class,
					MultiLabelNaiveBayes.class.getName());
		 
		 errorMeasurerOption = new ClassOption("errorMeasurer", 'e',
					"Measure of error for deciding which learner should predict.", MultiLabelErrorMeasurer.class, RelativeMeanAbsoluteDeviationMT.class.getName()) ;

		 changeDetector = new ClassOption("changeDetector",
					'H', "Change Detector.", 
					ChangeDetector.class,
					"DDM");
	}
	
	
	@Override
	protected MultiLabelRule newDefaultRule() {
		return new MultiLabelRuleClassification(1);
	}


}
