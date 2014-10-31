package moa.classifiers.rules.multilabel.instancetransformers;

import moa.AbstractMOAObject;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Performs no transformation. Returns
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 */

public class NoInstanceTransformation extends AbstractMOAObject implements InstanceTransformer{
	private static final long serialVersionUID = 1L;

	@Override
	public Instance sourceInstanceToTarget(Instance sourceInstance) {
		return sourceInstance; //.copy?
	}

	@Override
	public Prediction targetPredictionToSource(Prediction targetPrediction) {
		return targetPrediction; //.copy?
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}

}
