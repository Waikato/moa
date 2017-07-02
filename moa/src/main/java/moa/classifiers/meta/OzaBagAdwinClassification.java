package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.MultiTargetRegressionPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.DoubleVector;
import moa.learners.Classifier;

public class OzaBagAdwinClassification extends AbstractOzaBagAdwin<Classifier> implements Classifier{

	private static final long serialVersionUID = 1L;

	public OzaBagAdwinClassification() {
		super(Classifier.class, "moa.classifiers.trees.HoeffdingTree");
	}

	public double getAdwinError(Instance inst, int i) {
		return this.ensemble.get(i).correctlyClassifies(inst) ? 0 : 1;
	}

	public Prediction combinePredictions(Prediction[] predictions) {
		DoubleVector sums = new DoubleVector();
		for (Prediction p : predictions) {
			sums.addValues(p.asDoubleVector());
		}
		sums.scaleValues(1 / sums.numValues());
		return new MultiTargetRegressionPrediction(sums);
	}

}
