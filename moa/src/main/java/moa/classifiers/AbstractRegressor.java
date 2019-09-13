package moa.classifiers;

import moa.learners.Regressor;

public abstract class AbstractRegressor extends AbstractInstanceLearner<Regressor> implements Regressor {

	private static final long serialVersionUID = 1L;

	public AbstractRegressor() {
		super(Regressor.class);
	}
}
