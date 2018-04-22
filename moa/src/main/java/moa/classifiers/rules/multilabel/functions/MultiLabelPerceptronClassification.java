/*
 *    MultiLabelPerceptronClassification.java
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




package moa.classifiers.rules.multilabel.functions;

import moa.classifiers.MultiLabelClassifier;
import moa.options.ClassOption;
import moa.classifiers.rules.functions.Perceptron;

/**
 * Multi-Label perceptron classifier (by Binary Relevance).
 *This method computes a perceptron classifier per output
 * @author RSousa
 * @version $Revision: 1 $
 */
public class MultiLabelPerceptronClassification extends AbstractAMRulesFunctionBasicMlLearner
                    implements MultiLabelClassifier, AMRulesFunction{
    
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void init(){
        baseLearnerOption=new ClassOption("baseLearner", 'l',
            "moa.classifiers.functions.Perceptron", moa.classifiers.functions.Perceptron.class, "moa.classifiers.functions.Perceptron");
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
		//	
    }
}
