/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package moa.classifiers.rules.multilabel;

/**
 *
 * @author RSousa
 */
 
    
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.core.driftdetection.ChangeDetector;
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

public class AMRulesMultiTargetRegressorSemiSuper extends AMRulesMultiLabelLearnerSemiSuper implements MultiTargetRegressor{

	private static final long serialVersionUID = 1L;
	
	public  AMRulesMultiTargetRegressorSemiSuper(){
            splitCriterionOption = new ClassOption("splitCriterionOption", 'S',"Split criterion used to assess the merit of a split", MultiLabelSplitCriterion.class, MultiTargetVarianceRatio.class.getName()) ;
            weightedVoteOption = new ClassOption("weightedVoteOption",'w', "Weighted vote type", ErrorWeightedVoteMultiLabel.class,InverseErrorWeightedVoteMultiLabel.class.getName());
            learnerOption = new ClassOption("learnerOption",'L', "Learner", MultiTargetRegressor.class,AdaptiveMultiTargetRegressor.class.getName());
            errorMeasurerOption = new ClassOption("errorMeasurer", 'e',"Measure of error for deciding which learner should predict.", MultiLabelErrorMeasurer.class, RelativeMeanAbsoluteDeviationMT.class.getName()) ;
            changeDetector = new ClassOption("changeDetector",'H', "Change Detector.", ChangeDetector.class,"PageHinkleyDM -d 0.05 -l 35.0");
	}
	

	@Override
	protected MultiLabelRule newDefaultRule() {
		return new MultiLabelRuleRegression(1);
	}

	public AMRulesMultiTargetRegressorSemiSuper(double attributesPercentage) {
		super(attributesPercentage);
	}

}