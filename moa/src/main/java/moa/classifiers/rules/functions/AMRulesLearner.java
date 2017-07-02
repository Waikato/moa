package moa.classifiers.rules.functions;

import moa.learners.InstanceLearner;

public interface AMRulesLearner extends InstanceLearner {
	
	public double getCurrentError();

}
