package moa.learners.predictors.meta.regressors;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;
import com.yahoo.labs.samoa.instances.predictions.RegressionPrediction;

import moa.learners.predictors.Regressor;
import moa.learners.predictors.meta.AbstractOzaBagAdwin;

public class OzaBagAdwin extends AbstractOzaBagAdwin<Regressor> implements Regressor {

	private static final long serialVersionUID = 1L;

	public OzaBagAdwin() {
		super(Regressor.class, "trees.FIMTDD");
	}

	@Override
	public double getAdwinError(Instance inst, int i) {
		return Math.abs(this.ensemble.get(i).getPredictionForInstance(inst).asDouble() - inst.classValue());
	}

	@Override
	public Prediction combinePredictions(Prediction[] predictions) {
		double sum = 0;
		for (Prediction p : predictions) {
			sum += p.asDouble();
		}
		return new RegressionPrediction(sum / predictions.length);
	}
}
