package moa.classifiers.rules.multilabel.errormeasurers;

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

import com.yahoo.labs.samoa.instances.Prediction;

public class RootMeanSquaredErrorMT extends MultiTargetErrorMeasurer {

	/**
	 * 
	 */
	private double weightSeen;
	private double [] sumSquaredError;
	private static final long serialVersionUID = 1L;
	protected boolean hasStarted;
	
	@Override
	public void addPrediction(Prediction prediction, Prediction trueClass, double weight) {
		int numOutputs=prediction.numOutputAttributes();
		if (!hasStarted){
			sumSquaredError=new double[numOutputs];
			hasStarted=true;
		}
		for(int i=0; i<numOutputs;i++){
			double errorOutput=prediction.getVote(i, 0)-trueClass.getVote(i, 0);
			sumSquaredError[i]=errorOutput*errorOutput*weight+fadingErrorFactor*sumSquaredError[i];
		}
		weightSeen=weight+fadingErrorFactor*weightSeen;
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
