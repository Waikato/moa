package moa.tasks.classification;

import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.learners.predictors.Classifier;
import moa.options.ClassOption;
import moa.tasks.AbstractLearnModel;

public class LearnModel extends AbstractLearnModel<Classifier> implements ClassificationMainTask, CapabilitiesHandler {

	@Override
	public String getPurposeString() {
		return "Learns a Classification model from a stream.";
	}

	private static final long serialVersionUID = 1L;

	public LearnModel() {
		this.learnerOption = new ClassOption("learner", 'l', "Learner to train.", Classifier.class,
				"trees.HoeffdingTree");
	}

	@Override
	public ImmutableCapabilities defineImmutableCapabilities() {
		if (this.getClass() == LearnModel.class)
			return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
		else
			return new ImmutableCapabilities(Capability.VIEW_STANDARD);
	}
}
