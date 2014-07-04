package moa.classifiers.rules.error;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

public class RootMeanSquaredErrorMT extends MultiLabelErrorMeasurement {

	/**
	 * 
	 */
	private double weightSeen=0;
	private double sumSquaredError=0;
	private static final long serialVersionUID = 1L;

	@Override
	public void addPrediction(Prediction prediction, MultiLabelInstance inst) {
		double error=0;
		int numOutputs=inst.numOutputAttributes();

		for(int i=0; i<numOutputs;i++){
			double errorOutput=prediction.getVote(i, 0)-inst.valueOutputAttribute(i);
			error=errorOutput*errorOutput;
		}
		
		sumSquaredError=error*inst.weight()/numOutputs+fadingErrorFactor*sumSquaredError;
		weightSeen=inst.weight()+fadingErrorFactor*weightSeen;
	}

	@Override
	public double getCurrentError() {
		if(weightSeen==0)
			return Double.MAX_VALUE;
		else
			return sumSquaredError/weightSeen;
	}

}
