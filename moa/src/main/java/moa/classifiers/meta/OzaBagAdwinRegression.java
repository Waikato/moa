package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;
import com.yahoo.labs.samoa.instances.predictions.RegressionPrediction;

import moa.learners.Regressor;

public class OzaBagAdwinRegression extends AbstractOzaBagAdwin<Regressor> {

	private static final long serialVersionUID = 1L;

	public OzaBagAdwinRegression() {
		super(Regressor.class, "classifiers.trees.FIMTD");
	}

	public double getAdwinError(Instance inst, int i) {
		return Math.abs(this.ensemble.get(i).getPredictionForInstance(inst).asDouble() - inst.classValue());
	}

	public Prediction combinePredictions(Prediction[] predictions) {
		double sum = 0;
		for (Prediction p : predictions) {
			sum += p.asDouble();
		}
		return new RegressionPrediction(sum / predictions.length);
	}
}
