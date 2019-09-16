package moa.learners.predictors.meta.classifiers;

import com.yahoo.labs.samoa.instances.predictions.MultiTargetRegressionPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.DoubleVector;
import moa.learners.predictors.Classifier;
import moa.learners.predictors.meta.AbstractOzaBag;

public class OzaBag extends AbstractOzaBag<Classifier> implements Classifier {

	private static final long serialVersionUID = 1L;

	public OzaBag() {
		super(Classifier.class, "trees.HoeffdingTree");
	}

	@Override
	public Prediction combinePredictions(Prediction[] predictions) {
		DoubleVector sums = new DoubleVector();
		for (Prediction p : predictions) {
			sums.addValues(p.asDoubleVector());
		}
		sums.scaleValues(1 / sums.numValues());
		return new MultiTargetRegressionPrediction(sums);
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub

	}

}
