package moa.classifiers.mtr.functions;


import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.MultiTargetRegressionPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.classifiers.AbstractMultiTargetRegressor;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.learners.MultiTargetRegressor;

/**
 * MultiTargetNoChange class regressor. It always predicts the last target values seen.
 *
 * @author Albert Bifet (abifet@cs.waikato.ac.nz)
 * @version $Revision: 1 $
 */
public class MultiTargetNoChange extends AbstractMultiTargetRegressor implements MultiTargetRegressor {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Weather Forecast class classifier: always predicts the last class seen.";
    }

    //protected InstanceData lastSeenClasses;
    Prediction lastSeenClasses;
    
    @Override
    public void resetLearningImpl() {
        this.lastSeenClasses = null;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
    	int numOutputs = inst.numOutputAttributes();
    	DoubleVector prediction = new DoubleVector();
    	
    	for(int i=0; i<numOutputs;i++)
    		prediction.setValue(i, inst.classValue(i));
    	
        this.lastSeenClasses = new MultiTargetRegressionPrediction(prediction);
    	
    }
   
    

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
       
    }

    public boolean isRandomizable() {
        return false;
    }

	@Override
	public Prediction getPredictionForInstance(Instance inst) {
		//return (lastSeenClasses!=null) ? this.lastSeenClasses : new MultiLabelPrediction();
		return (lastSeenClasses!=null) ? this.lastSeenClasses : null;
	}

}
