/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.classifiers.rules.multilabel.functions;

import moa.classifiers.MultiLabelClassifier;
import moa.options.ClassOption;
import moa.classifiers.rules.functions.Perceptron;

/**
 *
 * @author RSousa
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
