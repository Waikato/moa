package moa.classifiers.rules.multilabel.errormeasurers;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.MultiLabelClassificationPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

public abstract class AbstractMultiTargetErrorMeasurer extends AbstractMultiLabelErrorMeasurer implements MultiLabelErrorMeasurer{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void addPrediction(Prediction prediction, Instance inst){
		int numOutputs=inst.numOutputAttributes();
		Prediction trueClass= new MultiLabelClassificationPrediction(numOutputs);
		for (int i=0; i<numOutputs; i++){
			trueClass.setVotes(i, new double[]{inst.valueOutputAttribute(i)});
		}
		addPrediction(prediction, trueClass, inst.weight());
	}

}
