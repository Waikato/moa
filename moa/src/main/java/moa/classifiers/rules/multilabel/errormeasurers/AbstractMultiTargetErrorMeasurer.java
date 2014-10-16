package moa.classifiers.rules.multilabel.errormeasurers;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

public abstract class AbstractMultiTargetErrorMeasurer extends AbstractMultiLabelErrorMeasurer implements MultiLabelErrorMeasurer{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void addPrediction(Prediction prediction, MultiLabelInstance inst){
		int numOutputs=inst.numberOutputTargets();
		Prediction trueClass= new MultiLabelPrediction(numOutputs);
		for (int i=0; i<numOutputs; i++){
			trueClass.setVotes(i, new double[]{inst.valueOutputAttribute(i)});
		}
		addPrediction(prediction, trueClass, inst.weight());
	}

}
