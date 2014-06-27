package moa.classifiers.multitarget.functions;


import com.yahoo.labs.samoa.instances.DenseInstanceData;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiTargetLearner;
import moa.core.Measurement;

/**
 * MultiTargetNoChange class regressor. It always predicts the last target values seen.
 *
 * @author Albert Bifet (abifet@cs.waikato.ac.nz)
 * @version $Revision: 1 $
 */
public class MultiTargetNoChange extends AbstractClassifier implements MultiTargetLearner {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Weather Forecast class classifier: always predicts the last class seen.";
    }

    protected InstanceData lastSeenClasses;

    @Override
    public void resetLearningImpl() {
        this.lastSeenClasses = null;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
    	MultiLabelInstance instance = (MultiLabelInstance) inst;
        this.lastSeenClasses = instance.classValues();
    }

    public InstanceData getPredictionForInstance(Instance i) {
        return (lastSeenClasses!=null) ? this.lastSeenClasses : new DenseInstanceData();
    }
    
    public double[] getVotesForInstance(Instance i) {
        return this.getPredictionForInstance(i).toDoubleArray();
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

}
