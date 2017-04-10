package moa.classifiers.rules.multilabel.functions;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiLabelClassifier;
import moa.classifiers.MultiLabelLearner;
import moa.core.Measurement;

public class DominantLabelsClassifier extends AbstractMultiLabelLearner
		implements MultiLabelClassifier, AMRulesFunction {


	/**
	 * Predicts for each label by majority voting
	 */
	private static final long serialVersionUID = 1L;
	
	private double countVector[];
	private double numInstances;

	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public void resetWithMemory() {
		//0-1 vector with majority class by label? Use fading strategy instead?

	}

	@Override
	public void selectOutputsToLearn(int[] outputAttributes) {
		int n=outputAttributes.length;
		double [] newCountVector=new double[n];
		for(int i=0; i<n; i++)
			newCountVector[i]=countVector[outputAttributes[i]];
		newCountVector=countVector;
	}

	@Override
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		int numOutputs=instance.numberOutputTargets();
		if(countVector==null){
			countVector=new double[numOutputs];
		}
		double weight=instance.weight();
		for(int i=0; i<numOutputs;i++){
			countVector[i]+=weight*instance.valueOutputAttribute(i);
		}
		numInstances+=weight;
	}

	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance inst) {
		int numOutputs=inst.numOutputAttributes();
		Prediction p=new MultiLabelPrediction(numOutputs);
		if(countVector!=null){
			for (int i=0; i<numOutputs; i++){
				double frac=countVector[i]/numInstances;
				p.setVote(i, 1, frac);
				p.setVote(i, 0, 1-frac);
			}
		}
		return p;
	}

	@Override
	public void resetLearningImpl() {
		countVector=null;
		numInstances=0;
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
	}

}
