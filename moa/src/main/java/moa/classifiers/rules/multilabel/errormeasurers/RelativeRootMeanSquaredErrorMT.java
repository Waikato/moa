package moa.classifiers.rules.multilabel.errormeasurers;

import com.yahoo.labs.samoa.instances.Prediction;

public class RelativeRootMeanSquaredErrorMT extends AbstractMultiTargetErrorMeasurer {

	/**
	 * 
	 */
	protected double weightSeen;
	protected double [] sumY;
	protected double [] sumSquaredError;
	protected double [] sumSquaredErrorToTargetMean;
	protected static final long serialVersionUID = 1L;
	protected boolean hasStarted;
	protected int numLearnedOutputs;

	@Override
	public void addPrediction(Prediction prediction, Prediction trueClass, double weight) {
		int numOutputs=prediction.numOutputAttributes();
		if (!hasStarted){
			sumSquaredError=new double[numOutputs];
			sumY=new double[numOutputs];
			sumSquaredErrorToTargetMean=new double[numOutputs];
			hasStarted=true;
			for(int i=0; i<numOutputs;i++)
				if(prediction.hasVotesForAttribute(i))
					++numLearnedOutputs;
			hasStarted=true;
		}
		weightSeen=weight+fadingErrorFactor*weightSeen;
		for(int i=0; i<numOutputs;i++){
			if(prediction.hasVotesForAttribute(i)){
				sumY[i]=trueClass.getVote(i, 0)*weight+fadingErrorFactor*sumY[i]; //sum target
				double errorOutput=prediction.getVote(i, 0)-trueClass.getVote(i, 0);
				double errorOutputTM=prediction.getVote(i, 0)-sumY[i]/weightSeen; //error to target mean
				sumSquaredError[i]=errorOutput*errorOutput*weight+fadingErrorFactor*sumSquaredError[i];
				sumSquaredErrorToTargetMean[i]=errorOutputTM*errorOutputTM*weight+fadingErrorFactor*sumSquaredErrorToTargetMean[i];
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
			int numOutputs=sumSquaredError.length;
			for (int i=0; i<numOutputs; i++)
				sum+=Math.sqrt(sumSquaredError[i]/sumSquaredErrorToTargetMean[i]);
			return sum/numLearnedOutputs;
		}
	}

	@Override
	public double getCurrentError(int index) {
		return Math.sqrt(sumSquaredError[index]/sumSquaredErrorToTargetMean[index]);
	}

	@Override
	public double[] getCurrentErrors() {
		double [] errors=null;
		if(sumSquaredError!=null){
			errors= new double[sumSquaredError.length];
			for (int i=0;i<sumSquaredError.length; i++)
				errors[i]=getCurrentError(i) ;
		}
		return errors;
	}


}
