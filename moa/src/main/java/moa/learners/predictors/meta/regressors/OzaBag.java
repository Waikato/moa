package moa.learners.predictors.meta.regressors;

import com.yahoo.labs.samoa.instances.predictions.Prediction;
import com.yahoo.labs.samoa.instances.predictions.RegressionPrediction;

import moa.learners.predictors.Regressor;
import moa.learners.predictors.meta.AbstractOzaBag;

public class OzaBag extends AbstractOzaBag<Regressor> implements Regressor {

	private static final long serialVersionUID = 1L;

	public OzaBag() {
		super(Regressor.class, "trees.FIMTDD");
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
