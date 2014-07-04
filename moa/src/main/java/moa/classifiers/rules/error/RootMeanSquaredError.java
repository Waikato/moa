package moa.classifiers.rules.error;

import com.yahoo.labs.samoa.instances.SingleLabelInstance;

import moa.AbstractMOAObject;

public class RootMeanSquaredError extends ErrorMeasurement {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private double weightSeen=0;
	private double sumSquaredError=0;
	

	@Override
	public double getCurrentError() {
		if(weightSeen==0)
			return Double.MAX_VALUE;
		else
			return Math.sqrt(sumSquaredError/weightSeen);
	}

	@Override
	public void addPrediction(double[] prediction, SingleLabelInstance inst) {
		double error=(prediction[0]-inst.classValue());
		sumSquaredError=error*error*inst.weight()+fadingErrorFactor*sumSquaredError;
		weightSeen=inst.weight()+fadingErrorFactor*weightSeen;
		
	}



}
