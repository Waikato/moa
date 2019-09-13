package moa.classifiers;

import moa.learners.MultiLabelClassifier;

public abstract class AbstractMultiLabelClassifier extends AbstractInstanceLearner<MultiLabelClassifier> implements MultiLabelClassifier {
	
	private static final long serialVersionUID = 1L;

	public AbstractMultiLabelClassifier() {
		super(MultiLabelClassifier.class);
	}
}
