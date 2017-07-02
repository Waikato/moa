package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.predictions.Prediction;
import com.yahoo.labs.samoa.instances.predictions.RegressionPrediction;

import moa.learners.Regressor;

public class OzaBagRegression extends AbstractOzaBag<Regressor> implements Regressor {
	
	private static final long serialVersionUID = 1L;

	public OzaBagRegression() {
		super(Regressor.class, "moa.classifiers.trees.FIMTDD");
	}
	
	public Prediction combinePredictions(Prediction[] predictions) {
		double sum = 0;
		for (Prediction p : predictions) {
			sum += p.asDouble();
		}
		return new RegressionPrediction(sum / predictions.length);
	}

}
