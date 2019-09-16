/*
 *    MultiTargetPerceptronRegressor.java
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
package moa.learners.predictors.core.functions;

import moa.learners.predictors.MultiTargetRegressor;
import moa.learners.predictors.rules.functions.Perceptron;
import moa.options.ClassOption;

/**
 * Binary relevance with a regression perceptron
 */

public class MultiTargetPerceptronRegressor extends AbstractAMRulesFunctionBasicMlLearner
		implements MultiTargetRegressor, AMRulesFunction {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public MultiTargetPerceptronRegressor() {
		super();
		regressorOption = new ClassOption("baseLearner", 'l', "Perceptron", Perceptron.class, "Perceptron");
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getPurposeString() {
		return "Uses an ensemble of rules.Perceptron to preform multitarget regression.\n"
				+ "Extends BasicMultiTargetRegressor by allowing only rules.Perceptron";
	}

	@Override
	public void resetWithMemory() {
		for (int i = 0; i < this.regressors.size(); i++) {
			Perceptron p = new Perceptron((Perceptron) this.regressors.get(i));
			p.setLearningRatio(((Perceptron) getPreparedClassOption(regressorOption)).learningRatioOption.getValue());
			this.regressors.set(i, p);
		}

	}

}
