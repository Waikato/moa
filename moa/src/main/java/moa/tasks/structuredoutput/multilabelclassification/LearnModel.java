package moa.tasks.structuredoutput.multilabelclassification;

import moa.learners.MultiLabelClassifier;
import moa.options.ClassOption;
import moa.tasks.AbstractLearnModel;

public class LearnModel extends AbstractLearnModel<MultiLabelClassifier> implements MultiLabelClassificatioMainTask {

    @Override
    public String getPurposeString() {
        return "Learns a multi-label classification model from a stream.";
    }

    private static final long serialVersionUID = 1L;

    public LearnModel() {
		this.learnerOption = new ClassOption("learner", 'l', "Learner to train.", MultiLabelClassifier.class, "moa.classifiers.mlc.trees.MultiLabelHoeffdingTree");
	}
    
	
}
