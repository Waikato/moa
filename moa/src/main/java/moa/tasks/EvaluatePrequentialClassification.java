package moa.tasks;

import java.io.PrintStream;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.Utils;
import moa.learners.Classifier;

public class EvaluatePrequentialClassification extends AbstractEvaluatePrequential<Classifier> implements ClassificationMainTask {

	private static final long serialVersionUID = 1L;
	
	public EvaluatePrequentialClassification() {
		super(Classifier.class, "moa.classifiers.trees.HoeffdingTree");
	}

	@Override
    public String getPurposeString() {
        return "Evaluates a classifier on a stream by testing then training with each example in sequence.";
    }
	@Override
	public void printPrediction(PrintStream print, Instance inst, Prediction prediction) {
		int trueClass = (int) inst.classValue();
		print.println(Utils.maxIndex(prediction.asDoubleArray()) + "," + (inst.classIsMissing() ? " ? " : trueClass));

	}

}
