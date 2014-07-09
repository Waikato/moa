package moa.classifiers.rules.error;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

public class MeanAbsoluteDeviationMT extends MultiLabelErrorMeasurement {

	/**
	 * 
	 */
	private double weightSeen;
	private double [] sumError;
	private static final long serialVersionUID = 1L;

	@Override
	public void addPrediction(Prediction prediction, MultiLabelInstance inst) {
		int numOutputs=inst.numOutputAttributes();
		for(int i=0; i<numOutputs;i++)
			sumError[i]=Math.abs(prediction.getVote(i, 0)-inst.valueOutputAttribute(i))*inst.weight()+fadingErrorFactor*sumError[i];
		weightSeen=inst.weight()+fadingErrorFactor*weightSeen;
	}

	@Override
	public double getCurrentError() {
		if(weightSeen==0)
			return Double.MAX_VALUE;
		else
		{
			double sum=0;
			int numOutputs=sumError.length;
			for (int i=0; i<numOutputs; i++)
				sum+=sumError[i];
			return sum/(weightSeen*numOutputs);
		}
	}

	@Override
	public double getCurrentError(int index) {
		return sumError[index]/weightSeen;
	}

}
