package moa.classifiers.rules.errormeasurers;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;

/*
 * Computes the Mean Absolute Deviation for single target regression problems
 */
public class MeanAbsoluteDeviation extends ErrorMeasurement {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private double weightSeen=0;
	private double sumError=0;

	@Override
	public double getCurrentError() {
		if(weightSeen==0)
			return Double.MAX_VALUE;
		else
			return sumError/weightSeen;
	}

	@Override
	public void addPrediction(double[] prediction, Instance inst) {
		sumError=Math.abs(prediction[0]-inst.classValue())*inst.weight()+fadingErrorFactor*sumError;
		weightSeen=inst.weight()+fadingErrorFactor*weightSeen;
	}

}
