package moa.learners.predictors.functions;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;
import com.yahoo.labs.samoa.instances.predictions.RegressionPrediction;

import moa.core.Measurement;
import moa.core.StringUtils;
import moa.learners.predictors.AbstractRegressor;
import moa.learners.predictors.Regressor;

public class Mean extends AbstractRegressor implements Regressor {

	double n = 0;
	double sum = 0;

	@Override
	public boolean isRandomizable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		StringUtils.appendIndented(out, indent, Double.toString(sum / n));
	}

	@Override
	public Prediction getPredictionForInstance(Instance instance) {
		// TODO Auto-generated method stub
		if (n > 0)
			return new RegressionPrediction(sum / n);
		else
			return null;
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		n += inst.weight();
		sum += inst.classValue() * inst.weight();
	}

	@Override
	public void resetLearningImpl() {
		n = 0;
		sum = 0;
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		// TODO Auto-generated method stub
		return null;
	}

}
