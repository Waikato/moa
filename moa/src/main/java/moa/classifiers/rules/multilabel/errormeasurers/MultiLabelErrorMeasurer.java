package moa.classifiers.rules.multilabel.errormeasurers;

import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.StructuredInstance;

import moa.options.OptionHandler;

public interface MultiLabelErrorMeasurer extends OptionHandler {

	public void addPrediction(Prediction prediction, Prediction trueClass, double weight);

	public void addPrediction(Prediction prediction, Prediction trueClass);

	public void addPrediction(Prediction prediction, StructuredInstance inst);

	public double getCurrentError();

	public double getCurrentError(int index);

	public double [] getCurrentErrors();

}
