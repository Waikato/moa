/*
 *    MultiLabelNaiveBayes.java
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

import moa.classifiers.MultiLabelClassifier;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.rules.functions.Perceptron;
import moa.options.ClassOption;

/**
 *  Binary relevance with Naive Bayes
 */

public class MultiLabelNaiveBayes extends AbstractAMRulesFunctionBasicMlLearner
		implements MultiLabelClassifier, AMRulesFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	protected void init(){
		baseLearnerOption=new ClassOption("baseLearner", 'l',
				"NaiveBayes", NaiveBayes.class, "moa.classifiers.bayes.NaiveBayes");
	}

	@Override
	public String getPurposeString() {
		return "Uses an ensemble of rules.Perceptron to preform multitarget regression.\n"
				+ "Extends BasicMultiLabelLearner by allowing only rules.Perceptron";
	}

	@Override
	public void resetWithMemory() {
		//for (int i = 0; i < this.ensemble.length; i++) {
		//TODO: JD - reset all statistics? how can we keep some memory?
		//}
		
	}

}
