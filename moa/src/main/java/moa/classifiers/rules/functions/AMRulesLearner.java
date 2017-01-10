package moa.classifiers.rules.functions;

import com.yahoo.labs.samoa.instances.Instance;

import moa.core.Example;
import moa.learners.Learner;

public interface AMRulesLearner extends Learner<Example<Instance>>{
	
	public double getCurrentError();

}
