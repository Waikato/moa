package moa.classifiers.rules.multilabel.errormeasurers;

import com.yahoo.labs.samoa.instances.Prediction;

public class RelativeMeanAbsoluteDeviationMT extends AbstractMultiTargetErrorMeasurer {

	/**
	 * 
	 */
	protected double weightSeen;
	protected double [] sumError;
	
	protected double [] sumY;
	protected double [] sumErrorToTargetMean;
	
	private static final long serialVersionUID = 1L;
	protected boolean hasStarted;
	protected int numLearnedOutputs;

	@Override
	public void addPrediction(Prediction prediction, Prediction trueClass, double weight) {
		int numOutputs=prediction.numOutputAttributes();
		if (!hasStarted){
			sumError=new double[numOutputs];
			sumY=new double[numOutputs];
			sumErrorToTargetMean=new double[numOutputs];
			hasStarted=true;
			for(int i=0; i<numOutputs;i++)
				if(prediction.hasVotesForAttribute(i))
					++numLearnedOutputs;
			hasStarted=true;
		}
		weightSeen=weight+fadingErrorFactor*weightSeen;
		for(int i=0; i<numOutputs;i++){
			if(prediction.hasVotesForAttribute(i)){
				sumError[i]=Math.abs(prediction.getVote(i, 0)-trueClass.getVote(i, 0))*weight+fadingErrorFactor*sumError[i];
				sumY[i]=trueClass.getVote(i, 0)*weight+fadingErrorFactor*sumY[i]; 
				double errorOutputTM=Math.abs(prediction.getVote(i, 0)-sumY[i]/weightSeen); //error to target mean
				sumErrorToTargetMean[i]=errorOutputTM*weight+fadingErrorFactor*sumErrorToTargetMean[i];
			}
		}

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
				sum+=getCurrentError(i);
			return sum/numLearnedOutputs;
		}
	}

	@Override
	public double getCurrentError(int index) {
		return sumError[index]/sumErrorToTargetMean[index];
	}

	@Override
	public double[] getCurrentErrors() {
		double [] errors=null;
		if(sumError!=null){
			errors=new double[sumError.length];
			for (int i=0;i<sumError.length; i++)
				errors[i]=getCurrentError(i);
		}
		return errors;
	}

}
