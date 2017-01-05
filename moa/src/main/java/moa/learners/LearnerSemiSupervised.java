/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;
import moa.core.Example;

/**
 *
 * @author RSousa
 */
public interface LearnerSemiSupervised<E extends Example> extends Learner<Example<Instance>>{
    
   public Prediction getTrainingPrediction();
 
}
