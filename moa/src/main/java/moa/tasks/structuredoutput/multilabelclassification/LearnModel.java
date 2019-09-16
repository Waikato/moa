package moa.tasks.structuredoutput.multilabelclassification;

import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.learners.predictors.MultiLabelClassifier;
import moa.options.ClassOption;
import moa.tasks.AbstractLearnModel;

public class LearnModel extends AbstractLearnModel<MultiLabelClassifier> implements MultiLabelClassificatioMainTask {

	@Override
	public String getPurposeString() {
		return "Learns a multi-label classification model from a stream.";
	}

	private static final long serialVersionUID = 1L;

	public LearnModel() {
		this.learnerOption = new ClassOption("learner", 'l', "Learner to train.", MultiLabelClassifier.class,
				"mlc.trees.MultiLabelHoeffdingTree");
	}

	@Override
	public ImmutableCapabilities defineImmutableCapabilities() {
		if (this.getClass() == LearnModel.class)
			return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
		else
			return new ImmutableCapabilities(Capability.VIEW_STANDARD);
	}
	
}
