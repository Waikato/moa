package moa.classifiers.rules.error;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

public class RootMeanSquaredErrorMT extends MultiLabelErrorMeasurement {

	/**
	 * 
	 */
	private double weightSeen;
	private double [] sumSquaredError;
	private static final long serialVersionUID = 1L;

	@Override
	public void addPrediction(Prediction prediction, MultiLabelInstance inst) {
		int numOutputs=inst.numOutputAttributes();

		for(int i=0; i<numOutputs;i++){
			double errorOutput=prediction.getVote(i, 0)-inst.valueOutputAttribute(i);
			sumSquaredError[i]=errorOutput*errorOutput+fadingErrorFactor*sumSquaredError[i];
		}
		weightSeen=inst.weight()+fadingErrorFactor*weightSeen;
	}

	@Override
	public double getCurrentError() {
		if(weightSeen==0)
			return Double.MAX_VALUE;
		else
		{
			double sum=0;
			int numOutputs=sumSquaredError.length;
			for (int i=0; i<numOutputs; i++)
				sum+=sumSquaredError[i];
			return Math.sqrt(sum/(weightSeen*numOutputs));
		}
	}

	@Override
	public double getCurrentError(int index) {
		return Math.sqrt(sumSquaredError[index]/weightSeen);
	}

}
