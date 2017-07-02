package moa.classifiers.rules.multilabel.errormeasurers;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.options.OptionHandler;

public interface MultiLabelErrorMeasurer extends OptionHandler {

	public void addPrediction(Prediction prediction, Prediction trueClass, double weight);

	public void addPrediction(Prediction prediction, Prediction trueClass);

	public void addPrediction(Prediction prediction, Instance inst);

	public double getCurrentError();

	public double getCurrentError(int index);

	public double [] getCurrentErrors();

}
