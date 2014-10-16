package moa.classifiers.rules.multilabel.errormeasurers;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.options.OptionHandler;

public interface MultiLabelErrorMeasurer extends OptionHandler {

	public void addPrediction(Prediction prediction, Prediction trueClass, double weight);

	public void addPrediction(Prediction prediction, Prediction trueClass);

	public void addPrediction(Prediction prediction, MultiLabelInstance inst);

	public double getCurrentError();

	public double getCurrentError(int index);

	public double [] getCurrentErrors();

}
