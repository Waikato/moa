package moa.classifiers;

import moa.learners.MultiTargetRegressor;

public abstract class AbstractMultiTargetRegressor extends AbstractInstanceLearner<MultiTargetRegressor> implements MultiTargetRegressor {

	private static final long serialVersionUID = 1L;

	public AbstractMultiTargetRegressor() {
		super(MultiTargetRegressor.class);
	}
}
