package moa.learners;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

public interface MultiLabelClassifier extends InstanceLearner {
	
    /**
     * Gets whether this classifier correctly classifies an instance. Uses
     * getPredictionForInstance to obtain the prediction and the instance to obtain
     * its true class.
     *
     *
     * @param inst the instance to be classified
     * @return true if the instance is correctly classified
     */
    // TODO Add this to AbstractLearner
    // public boolean correctlyClassifies(MultilabelInstance inst);

    /**
     * Trains this learner incrementally using the given example.
     *
     * @param inst the instance to be used for training
     */
    public void trainOnInstanceImpl(MultiLabelInstance inst);

    /**
     * Sets the reference to the header of the data stream. The header of the
     * data stream is extended from WEKA
     * <code>Instances</code>. This header is needed to know the number of
     * classes and attributes
     *
     * @param ih the reference to the data stream header
     */
    //public void setModelContext(InstancesHeader ih);
    
    /**
     * Gets the reference to the header of the data stream. The header of the
     * data stream is extended from WEKA
     * <code>Instances</code>. This header is needed to know the number of
     * classes and attributes
     *
     * @return the reference to the data stream header
     */
    //public InstancesHeader getModelContext();
    
    public Prediction getPredictionForInstance(MultiLabelInstance inst);
    
    public MultiLabelClassifier copy();

}
