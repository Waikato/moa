/*
 *    RandomAMRules.java
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
package moa.classifiers.rules.meta;

import moa.classifiers.Regressor;
import moa.classifiers.rules.multilabel.meta.MultiLabelRandomAMRules;

/**
 * Random AMRules algoritgm that performs analogous procedure as the Random Forest Trees but with Rules
 */


public class RandomAMRules extends MultiLabelRandomAMRules implements Regressor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RandomAMRules() {
		super();
	//	baseLearnerOption = new ClassOption("baseLearner", 'l', "Classifier to train.", AMRulesMultiLabelLearner.class, "AMRulesMultiTargetRegressor"); 
	}

}
