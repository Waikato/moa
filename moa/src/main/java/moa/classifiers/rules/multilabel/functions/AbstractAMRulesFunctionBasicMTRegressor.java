package moa.classifiers.rules.multilabel.functions;

import moa.classifiers.Classifier;
import moa.classifiers.multitarget.BasicMultiTargetRegressor;

abstract public class AbstractAMRulesFunctionBasicMTRegressor extends
		BasicMultiTargetRegressor implements AMRulesFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	abstract public void resetWithMemory();

	@Override
	public void selectOutputsToLearn(int[] outputsToLearn) {
		Classifier[] newEnsemble= new Classifier[outputsToLearn.length];
		for(int i=0; i<outputsToLearn.length; i++){
			newEnsemble[i]=this.ensemble[outputsToLearn[i]].copy();
		}
		this.ensemble=newEnsemble;
	}


}
