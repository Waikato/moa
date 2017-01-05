/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.classifiers;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * @author RSousa
 */
public interface MultiLabelLearnerSemiSupervised extends MultiLabelLearner {
    
    public Prediction getTrainingPrediction();
    
    public void trainOnInstanceImpl(MultiLabelInstance instance);
	
    public Prediction getPredictionForInstance(MultiLabelInstance instance);
}
