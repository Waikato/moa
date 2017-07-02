package moa.tasks;

import java.io.PrintStream;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.learners.MultiLabelClassifier;

public class EvaluatePrequentialMultiLabelClassification extends AbstractEvaluatePrequential<MultiLabelClassifier> {

	private static final long serialVersionUID = 1L;

	public EvaluatePrequentialMultiLabelClassification() {
		super(MultiLabelClassifier.class, "moa.classifiers.mlc.trees.MultilabelHoeffdingTree");
	}
	
	@Override
    public String getPurposeString() {
        return "Evaluates a multi-label classifier on a stream by testing then training with each example in sequence.";
    }
	
	@Override
	public void printPrediction(PrintStream print, Instance inst, Prediction prediction) {
		print.println(prediction.asPredictionString() + "," + inst.outputAttributesToString());	
	}
}
