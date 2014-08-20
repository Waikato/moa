package moa.classifiers.multitarget.functions;


import com.yahoo.labs.samoa.instances.DenseInstanceData;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.core.Measurement;

/**
 * MultiTargetNoChange class regressor. It always predicts the last target values seen.
 *
 * @author Albert Bifet (abifet@cs.waikato.ac.nz)
 * @version $Revision: 1 $
 */
public class MultiTargetNoChange extends AbstractMultiLabelLearner implements MultiTargetRegressor {

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
    public void trainOnInstanceImpl(MultiLabelInstance inst) {
    	int numOutputs = inst.numberOutputTargets();
    	Prediction prediction = new MultiLabelPrediction(numOutputs);
    	
    	for(int i=0; i<numOutputs;i++)
    		prediction.setVotes(i,new double[]{inst.classValue(i)});
    	
        this.lastSeenClasses = prediction;
    	
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
	public Prediction getPredictionForInstance(MultiLabelInstance inst) {
		//return (lastSeenClasses!=null) ? this.lastSeenClasses : new MultiLabelPrediction();
		return (lastSeenClasses!=null) ? this.lastSeenClasses : null;
	}

}
