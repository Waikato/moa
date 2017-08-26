package moa.classifiers.rules.multilabel.functions;

import moa.classifiers.MultiLabelClassifier;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.rules.functions.Perceptron;
import moa.options.ClassOption;

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
