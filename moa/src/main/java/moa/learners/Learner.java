/*
 *    Learner.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.learners;

import moa.MOAObject;
import moa.core.Example;

import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.core.Measurement;
import moa.gui.AWTRenderable;
import moa.options.OptionHandler;

/**
 * Learner interface for incremental learning models. 
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public interface Learner<E extends Example> extends MOAObject, OptionHandler, AWTRenderable {


    /**
     * Gets whether this learner needs a random seed.
     * Examples of methods that needs a random seed are bagging and boosting.
     *
     * @return true if the learner needs a random seed.
     */
    public boolean isRandomizable();

    /**
     * Sets the seed for random number generation.
     *
     * @param s the seed
     */
    public void setRandomSeed(int s);

    /**
     * Gets whether training has started.
     *
     * @return true if training has started
     */
    public boolean trainingHasStarted();

    /**
     * Gets the sum of the weights of the instances that have been used
     * by this learner during the training in <code>trainOnInstance</code>
     *
     * @return the weight of the instances that have been used training
     */
    public double trainingWeightSeenByModel();

    /**
     * Resets this learner. It must be similar to
     * starting a new learner from scratch.
     *
     */
    public void resetLearning();

    /**
     * Trains this learner incrementally using the given example.
     *
     * @param inst the instance to be used for training
     */
    public void trainOnInstance(E example);

    /**
     * Predicts the class memberships for a given instance. If
     * an instance is unclassified, the returned array elements
     * must be all zero.
     *
     * @param inst the instance to be classified
     * @return an array containing the estimated membership
     * probabilities of the test instance in each class
     */
    public double[] getVotesForInstance(E example);

    /**
     * Gets the current measurements of this learner.
     *
     * @return an array of measurements to be used in evaluation tasks
     */
    public Measurement[] getModelMeasurements();

    /**
     * Gets the learners of this ensemble.
     * Returns null if this learner is a single learner.
     *
     * @return an array of the learners of the ensemble
     */
    public Learner[] getSublearners();

     /**
     * Gets the model if this learner.
     *
     * @return the copy of this learner
     */
    public MOAObject getModel();
    
     /**
     * Sets the reference to the header of the data stream.
     * The header of the data stream is extended from WEKA <code>Instances</code>.
     * This header is needed to know the number of classes and attributes
     *
     * @param ih the reference to the data stream header
     */
    public void setModelContext(InstancesHeader ih);
    
    /**
     * Gets the reference to the header of the data stream.
     * The header of the data stream is extended from WEKA <code>Instances</code>.
     * This header is needed to know the number of classes and attributes
     *
     * @return the reference to the data stream header
     */
    public InstancesHeader getModelContext();

	public Prediction getPredictionForInstance(E testInst);
    
}



