package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.ClassificationPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.DoubleVector;
import moa.learners.Classifier;

public class OzaBoostClassification extends AbstractOzaBoost<Classifier> implements Classifier {

	private static final long serialVersionUID = 1L;

	public OzaBoostClassification() {
		super(Classifier.class, "moa.classifiers.trees.HoeffdingTree");
	}

	@Override
	public void updateWeight(int i, Instance inst) {
		double lambda_d = 1.0;
		if (this.ensemble.get(i).correctlyClassifies(inst)) {
			this.scms[i] += lambda_d;
			lambda_d *= this.trainingWeightSeenByModel / (2 * this.scms[i]);
		} else {
			this.swms[i] += lambda_d;
			lambda_d *= this.trainingWeightSeenByModel / (2 * this.swms[i]);
		}		
	}

	@Override
	public Prediction combinePredictions(Prediction[] predictions, double[] weights) {
		DoubleVector combinedVote = new DoubleVector();
		for (int i = 0; i < predictions.length; i++) {
			if (weights[i] > 0.0) {
				DoubleVector vote = predictions[i].asDoubleVector();
				if (vote.sumOfValues() > 0.0) {
					vote.normalize();
					vote.scaleValues(weights[i]);
					combinedVote.addValues(vote);
				}
			}
		}
		return new ClassificationPrediction(combinedVote.getArrayRef());
	}

}
