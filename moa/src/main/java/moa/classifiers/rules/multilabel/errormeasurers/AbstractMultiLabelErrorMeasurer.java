package moa.classifiers.rules.multilabel.errormeasurers;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

public abstract class AbstractMultiLabelErrorMeasurer  extends AbstractOptionHandler implements MultiTargetErrorMeasurer{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public FloatOption fadingErrorFactorOption = new FloatOption(
			"fadingErrorFactor", 'f', 
			"Fading factor for the error", 0.99, 0, 1);
	
	protected double fadingErrorFactor;
	
	abstract public void addPrediction(Prediction prediction, Prediction trueClass, double weight);
	
	public void addPrediction(Prediction prediction, Prediction trueClass){
		addPrediction(prediction, trueClass);
	}
	
	abstract public void addPrediction(Prediction prediction, MultiLabelInstance inst);
	
	abstract public double getCurrentError();
	
	abstract public double getCurrentError(int index);
	
	abstract public double [] getCurrentErrors();
	
	public AbstractMultiLabelErrorMeasurer() {
		fadingErrorFactor=fadingErrorFactorOption.getValue();
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,ObjectRepository repository) {
		
	}

}
