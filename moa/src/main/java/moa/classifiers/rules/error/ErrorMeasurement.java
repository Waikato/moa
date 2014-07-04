package moa.classifiers.rules.error;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.SingleLabelInstance;

import moa.AbstractMOAObject;

abstract public class ErrorMeasurement extends AbstractMOAObject {
	
	public FloatOption fadingErrorFactorOption = new FloatOption(
			"fadingErrorFactor", 'e', 
			"Fading factor for the error", 0.99, 0, 1);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected double fadingErrorFactor;
	abstract public void addPrediction(double [] prediction, SingleLabelInstance inst);
	
	abstract public double getCurrentError();

	public ErrorMeasurement(){
		fadingErrorFactor=fadingErrorFactorOption.getValue();
	}
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
