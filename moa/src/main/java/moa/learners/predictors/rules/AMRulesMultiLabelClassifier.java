/*
 *    AMRulesMultiLabelLearnerSemiSuper.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author R. Sousa, J. Gama
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

package moa.learners.predictors.rules;

import moa.learners.predictors.MultiLabelClassifier;
import moa.learners.predictors.core.MultiLabelRule;
import moa.learners.predictors.core.MultiLabelRuleClassification;
import moa.learners.predictors.core.driftdetection.ChangeDetector;
import moa.learners.predictors.core.errormeasurers.MultiLabelErrorMeasurer;
import moa.learners.predictors.core.errormeasurers.RelativeMeanAbsoluteDeviationMT;
import moa.learners.predictors.core.functions.MultiLabelNaiveBayes;
import moa.learners.predictors.core.splitcriteria.MultiLabelSplitCriterion;
import moa.learners.predictors.core.splitcriteria.MultiTargetVarianceRatio;
import moa.learners.predictors.core.voting.ErrorWeightedVoteMultiLabel;
import moa.learners.predictors.core.voting.UniformWeightedVoteMultiLabel;
import moa.options.ClassOption;

/**
 * Method for online multi-Label classification. This method consists of the
 * adaptation of the Multi-target Regression method to Multi-label
 * Classification by problem transformation. This also implied the adaption of
 * AMRules to the output binary variable through the substitution of some
 * heurístics such as variance measures by entropy measures.
 * 
 * 
 * The algorithm is fully explained in the following publication:
 * 
 * Sousa R., Gama J. (2016) Online Multi-label Classification with Adaptive
 * Model Rules. In: Luaces O. et al. (eds) Advances in Artificial Intelligence.
 * CAEPIA 2016. Lecture Notes in Computer Science, vol 9868. Springer.
 * 
 * @author RSousa
 * @version $Revision: 2 $
 */

public class AMRulesMultiLabelClassifier extends AMRulesMultiLabelLearner implements MultiLabelClassifier {

	private static final long serialVersionUID = 1L;

	@Override
	public String getPurposeString() {
		return "Method for online multi-Label classification.This method consists of the adaptation of the Multi-target Regression method to Multi-label Classification by problem transformation.This also implied the adaption of AMRules to the output binary variable through the substitution ofsome heurístics such as variance measures by entropy measures.";
	}

	public AMRulesMultiLabelClassifier() {
		splitCriterionOption = new ClassOption("splitCriterionOption", 'S',
				"Split criterion used to assess the merit of a split", MultiLabelSplitCriterion.class,
				MultiTargetVarianceRatio.class.getName());

		weightedVoteOption = new ClassOption("weightedVoteOption", 'w', "Weighted vote type",
				ErrorWeightedVoteMultiLabel.class, UniformWeightedVoteMultiLabel.class.getName());

		learnerOption = new ClassOption("learnerOption", 'L', "Learner", MultiLabelClassifier.class,
				MultiLabelNaiveBayes.class.getName());

		errorMeasurerOption = new ClassOption("errorMeasurer", 'e',
				"Measure of error for deciding which learner should predict.", MultiLabelErrorMeasurer.class,
				RelativeMeanAbsoluteDeviationMT.class.getName());

		changeDetector = new ClassOption("changeDetector", 'H', "Change Detector.", ChangeDetector.class, "DDM");
	}

	@Override
	protected MultiLabelRule newDefaultRule() {
		return new MultiLabelRuleClassification(1);
	}

}
