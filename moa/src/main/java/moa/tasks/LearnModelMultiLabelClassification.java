package moa.tasks;

import moa.learners.MultiLabelClassifier;
import moa.options.ClassOption;

public class LearnModelMultiLabelClassification extends AbstractLearnModel<MultiLabelClassifier> {

    @Override
    public String getPurposeString() {
        return "Learns a multi-label classification model from a stream.";
    }

    private static final long serialVersionUID = 1L;

    public LearnModelMultiLabelClassification() {
		this.learnerOption = new ClassOption("learner", 'l', "Learner to train.", MultiLabelClassifier.class, "moa.classifiers.mlc.trees.MultiLabelHoeffdingTree");
	}
    
	
}
