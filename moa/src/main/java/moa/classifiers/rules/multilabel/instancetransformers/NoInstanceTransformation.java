package moa.classifiers.rules.multilabel.instancetransformers;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.AbstractMOAObject;

public class NoInstanceTransformation extends AbstractMOAObject implements InstanceTransformer{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Instance sourceInstanceToTarget(Instance sourceInstance) {
		return sourceInstance;
	}

	@Override
	public Prediction targetPredictionToSource(Prediction targetPrediction) {
		return targetPrediction;
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}

}
