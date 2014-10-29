package moa.classifiers.rules.multilabel.instancetransformers;

import moa.MOAObject;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Interface for instance transformation
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 */

public interface InstanceTransformer extends MOAObject{
	
	public Instance sourceInstanceToTarget(Instance sourceInstance);
	public Prediction targetPredictionToSource(Prediction targetPrediction);

}
