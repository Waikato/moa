package moa.classifiers.rules.multilabel.functions;

import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.multitarget.BasicMultiTargetRegressor;
import moa.classifiers.rules.functions.Perceptron;
import moa.options.ClassOption;

public class MultiLabelPerceptronRegressor extends BasicMultiTargetRegressor
		implements MultiTargetRegressor, AMRulesFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	protected void init(){
		baseLearnerOption=new ClassOption("baseLearner", 'l',
				"Perceptron", Perceptron.class, "Perceptron");
	}

	@Override
	public String getPurposeString() {
		return "Uses an ensemble of rules.Perceptron to preform multitarget regression.\n"
				+ "Extends BasicMultiTargetRegressor by allowing only rules.Perceptron";
	}

	@Override
	public void resetWithMemory() {
		for (int i = 0; i < this.ensemble.length; i++) {
			Perceptron p=new Perceptron((Perceptron)this.ensemble[i]);
			p.setLearningRatio(((Perceptron)getPreparedClassOption(baseLearnerOption)).learningRatioOption.getValue());
			this.ensemble[i] = p;
		}
		
	}
	
	
	
	
}
