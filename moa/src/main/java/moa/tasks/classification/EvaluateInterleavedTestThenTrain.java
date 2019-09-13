package moa.tasks.classification;

import moa.evaluation.LearningPerformanceEvaluator;
import moa.learners.Classifier;
import moa.options.ClassOption;
import moa.tasks.AbstractEvaluateInterleavedTestThenTrain;

public class EvaluateInterleavedTestThenTrain extends AbstractEvaluateInterleavedTestThenTrain<Classifier> implements ClassificationMainTask {

	public EvaluateInterleavedTestThenTrain(){
		this.learnerOption = new ClassOption("learner", 'l',
	            "Learner to train.", Classifier.class, "moa.classifiers.bayes.NaiveBayes");
		this.evaluatorOption = new ClassOption("evaluator", 'e',
	            "Classification performance evaluation method.",
	            LearningPerformanceEvaluator.class,
	            "BasicClassificationPerformanceEvaluator");
	}
	
    public String getPurposeString() {
        return "Evaluates a classifier on a stream by testing then training with each example in sequence.";
    }

	
}
