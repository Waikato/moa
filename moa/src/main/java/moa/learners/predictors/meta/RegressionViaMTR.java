package moa.learners.predictors.meta;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;
import com.yahoo.labs.samoa.instances.predictions.RegressionPrediction;

import moa.core.Measurement;
import moa.learners.predictors.AbstractRegressor;
import moa.learners.predictors.MultiTargetRegressor;
import moa.learners.predictors.Regressor;
import moa.options.ClassOption;

public class RegressionViaMTR extends AbstractRegressor implements Regressor {

	public MultiTargetRegressor mtr;

	public ClassOption regressorOption = new ClassOption("MTRegessor", 'c',
			"Multi-target regressor to be used as a single target regressor.", MultiTargetRegressor.class,
			"trees.ISOUPTree");

	@Override
	public boolean isRandomizable() {
		return true;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		out.append("Meta single-target regressor which uses a multi-target regressor with a single target.");
	}

	@Override
	public Prediction getPredictionForInstance(Instance instance) {
		Prediction p = mtr.getPredictionForInstance(instance);
		return new RegressionPrediction(p.getPrediction(0));
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		mtr.trainOnInstance(inst);
	}

	@Override
	public void resetLearningImpl() {
		if (this.mtr == null)
			this.mtr = (MultiTargetRegressor) getPreparedClassOption(this.regressorOption);
		this.mtr.resetLearning();
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return this.mtr.getModelMeasurements();
	}

	@Override
	public void modelContextSet() {
		this.mtr.setModelContext(getModelContext());
	}
	
}
