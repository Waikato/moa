package moa.classifiers.rules.error;

import java.util.List;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.AbstractMOAObject;

public abstract class MultiLabelErrorMeasurement extends AbstractMOAObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public FloatOption fadingErrorFactorOption = new FloatOption(
			"fadingErrorFactor", 'e', 
			"Fading factor for the error", 0.99, 0, 1);
	
	protected double fadingErrorFactor;
	
	abstract public void addPrediction(Prediction prediction, MultiLabelInstance inst);
	
	abstract public double getCurrentError();
	
	abstract public double getCurrentError(int index);
	
	public MultiLabelErrorMeasurement(){
		fadingErrorFactor=fadingErrorFactorOption.getValue();
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}

}
