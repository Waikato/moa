package moa.tasks.classification;

import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.evaluation.evaluators.LearningPerformanceEvaluator;
import moa.learners.predictors.Classifier;
import moa.options.ClassOption;
import moa.tasks.AbstractEvaluateInterleavedTestThenTrain;

public class EvaluateInterleavedTestThenTrain extends AbstractEvaluateInterleavedTestThenTrain<Classifier>
		implements ClassificationMainTask {

	public EvaluateInterleavedTestThenTrain() {
		this.learnerOption = new ClassOption("learner", 'l', "Learner to train.", Classifier.class,
				"bayes.NaiveBayes");
		this.evaluatorOption = new ClassOption("evaluator", 'e', "Classification performance evaluation method.",
				LearningPerformanceEvaluator.class, "BasicClassificationPerformanceEvaluator");
	}

	@Override
	public String getPurposeString() {
		return "Evaluates a classifier on a stream by testing then training with each example in sequence.";
	}

	@Override
	public ImmutableCapabilities defineImmutableCapabilities() {
		if (this.getClass() == EvaluateInterleavedTestThenTrain.class)
			return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
		else
			return new ImmutableCapabilities(Capability.VIEW_STANDARD);
	}
}
