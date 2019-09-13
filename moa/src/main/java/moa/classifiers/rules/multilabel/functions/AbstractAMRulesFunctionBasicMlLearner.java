/*
 *    AbstractAMRulesFunctionBasicMlLearner.java
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
package moa.classifiers.rules.multilabel.functions;

import moa.classifiers.mlc.BasicMultiLabelLearner;
import moa.classifiers.mtr.BasicMultiTargetRegressor;
import moa.learners.Classifier;
import moa.learners.Regressor;

abstract public class AbstractAMRulesFunctionBasicMlLearner extends
BasicMultiTargetRegressor implements AMRulesFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	abstract public void resetWithMemory();

	@Override
	public void selectOutputsToLearn(int[] outputsToLearn) {
		Regressor[] newEnsemble= new Regressor[outputsToLearn.length];
		for(int i=0; i<outputsToLearn.length; i++){
			newEnsemble[i] = (Regressor) this.ensemble[outputsToLearn[i]].copy();
		}
		this.ensemble=newEnsemble;
	}


}
