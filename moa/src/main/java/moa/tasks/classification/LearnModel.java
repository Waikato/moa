package moa.tasks.classification;

import moa.learners.Classifier;
import moa.options.ClassOption;
import moa.tasks.AbstractLearnModel;

public class LearnModel extends AbstractLearnModel<Classifier> implements ClassificationMainTask {

    @Override
    public String getPurposeString() {
        return "Learns a Classification model from a stream.";
    }

    private static final long serialVersionUID = 1L;

    public LearnModel() {
		this.learnerOption = new ClassOption("learner", 'l', "Learner to train.", Classifier.class, "moa.classifiers.trees.HoeffdingTree");
	}
	
}
