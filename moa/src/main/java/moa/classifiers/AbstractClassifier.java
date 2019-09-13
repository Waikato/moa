package moa.classifiers;

import moa.learners.Classifier;

public abstract class AbstractClassifier extends AbstractInstanceLearner<Classifier> implements Classifier {

	public AbstractClassifier() {
		super(Classifier.class);
	}
	
}
