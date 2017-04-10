/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.classifiers.rules.multilabel.functions;

import moa.classifiers.MultiLabelClassifier;
import moa.options.ClassOption;

/**
 *
 * @author RSousa
 */
public class MajorityLabelClassification extends AbstractAMRulesFunctionBasicMlLearner
                    implements MultiLabelClassifier, AMRulesFunction{
    
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void init(){
        baseLearnerOption=new ClassOption("baseLearner", 'l',
            "moa.classifiers.bayes.MajorityLabel", moa.classifiers.bayes.MajorityLabel.class, "moa.classifiers.bayes.MajorityLabel");
    }

    @Override
    public String getPurposeString() {
        return "Uses an ensemble of Majority Label to preform Multi Label classification.\n"
	       + "Extends BasicMultiLabelLearner by allowing only Majority Label";
    }
    
    @Override
    public void resetWithMemory() {
		//for (int i = 0; i < this.ensemble.length; i++) {
		//TODO: JD - reset all statistics? how can we keep some memory?
		//	
    }
    
}
