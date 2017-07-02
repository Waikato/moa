package moa.tasks;

import moa.learners.Classifier;
import moa.options.ClassOption;

public class LearnModelClassification extends AbstractLearnModel<Classifier> implements ClassificationMainTask {

    @Override
    public String getPurposeString() {
        return "Learns a Classification model from a stream.";
    }

    private static final long serialVersionUID = 1L;

    public LearnModelClassification() {
		this.learnerOption = new ClassOption("learner", 'l', "Learner to train.", Classifier.class, "moa.classifiers.trees.HoeffdingTree");
	}
	
}
