package moa.classifiers.rules.multilabel.core;

import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class LearningLiteralRegression extends LearningLiteral {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LearningLiteralRegression() {
	}

	public LearningLiteralRegression(DoubleVector outputsToLearn) {
		super(outputsToLearn);
	}

	
	public boolean updateAndCheckChange(MultiLabelInstance instance) {
		int numOutputs=instance.numberOutputTargets();
		if (!hasStarted){
			for (int i=0; i<numOutputs; i++){
				changeDetectors[i]=((ChangeDetector)this.getPreparedClassOption(changeDetectorOption)).copy();
			}
		}
		boolean hasChanged=false;
		Prediction prediction=getPredictionForInstance(instance);
		double []normalizedErrors=getNormalizedErrors(prediction, instance.classValues());
		for (int i=0; i<outputsToLearn.length;i++){
			changeDetectors[i].input(normalizedErrors[i]);
			if(changeDetectors[i].getChange())
				hasChanged=true;
		}
		return hasChanged;
		
	}
	
	private double [] getNormalizedErrors(Prediction prediction, InstanceData classValues) {
		double [] errors= new double[outputsToLearn.length];
		
		for (int i=0; i<outputsToLearn.length;i++){
			double predY=normalizeOutputValue(outputsToLearn[i],prediction.getVote(outputsToLearn[i], 0));
			double trueY=normalizeOutputValue(outputsToLearn[i],classValues.value(outputsToLearn[i]));
			errors[i]=Math.abs(predY-trueY);
		}
		return errors;
	}
	
	private double normalizeOutputValue(int outputAttributeIndex, double value) {
		double meanY = this.nodeStatistics[outputAttributeIndex].getValue(1)/this.nodeStatistics[outputAttributeIndex].getValue(0);
		double sdY = computeSD(this.nodeStatistics[outputAttributeIndex].getValue(2), this.nodeStatistics[outputAttributeIndex].getValue(1), this.nodeStatistics[outputAttributeIndex].getValue(0));
		double normalizedY = 0.0;
		if (sdY > 0.0000001) {
			normalizedY = (value - meanY) / (sdY);
		}
		return normalizedY;
	}
	

	public double computeSD(double squaredSum, double sum, double weightSeen) {
		if (weightSeen > 1) {
			return Math.sqrt((squaredSum - ((sum * sum) / weightSeen)) / (weightSeen - 1.0));
		}
		return 0.0;
	}
	
	
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

	}

	
}
