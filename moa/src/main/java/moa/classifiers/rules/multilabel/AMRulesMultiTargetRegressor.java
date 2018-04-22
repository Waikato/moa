/*
 *    AMRulesMultiTargetRegressor.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */
package moa.classifiers.rules.multilabel;

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

/**
 * AMRules Algorithm  for multitarget
 *
 * splitCriterionOption- Split criterion used to assess the merit of a split
 *
 * weightedVoteOption - Weighted vote type
 *
 * learnerOption - Learner selection
 *
 * errorMeasurerOption -  Measure of error for deciding which learner should predict
 *
 * changeDetector  - Change selection
 *
 * João Duarte, João Gama, Albert Bifet, Adaptive Model Rules From High-Speed Data Streams. TKDD 10(3): 30:1-30:22 (2016)

 */





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
					MultiTargetRegressor.class,
					AdaptiveMultiTargetRegressor.class.getName());
		 
		 errorMeasurerOption = new ClassOption("errorMeasurer", 'e',
					"Measure of error for deciding which learner should predict.", MultiLabelErrorMeasurer.class, RelativeMeanAbsoluteDeviationMT.class.getName()) ;

	
		 changeDetector = new ClassOption("changeDetector",
					'H', "Change Detector.", 
					ChangeDetector.class,
					"PageHinkleyDM -d 0.05 -l 35.0");
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
