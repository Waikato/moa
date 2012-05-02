/*
 *    Classifier.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.classifiers;

import moa.MOAObject;
import moa.core.InstancesHeader;
import moa.core.Measurement;
import moa.gui.AWTRenderable;
import moa.options.OptionHandler;
import weka.core.Instance;

/**
 * Classifier interface for incremental classification models. 
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public interface Classifier extends MOAObject, OptionHandler, AWTRenderable {

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

    /**
     * Gets whether this classifier needs a random seed.
     * Examples of methods that needs a random seed are bagging and boosting.
     *
     * @return true if the classifier needs a random seed.
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
     * by this classifier during the training in <code>trainOnInstance</code>
     *
     * @return the weight of the instances that have been used training
     */
    public double trainingWeightSeenByModel();

    /**
     * Resets this classifier. It must be similar to
     * starting a new classifier from scratch.
     *
     */
    public void resetLearning();

    /**
     * Trains this classifier incrementally using the given instance.
     *
     * @param inst the instance to be used for training
     */
    public void trainOnInstance(Instance inst);

    /**
     * Predicts the class memberships for a given instance. If
     * an instance is unclassified, the returned array elements
     * must be all zero.
     *
     * @param inst the instance to be classified
     * @return an array containing the estimated membership
     * probabilities of the test instance in each class
     */
    public double[] getVotesForInstance(Instance inst);

    /**
     * Gets whether this classifier correctly classifies an instance.
     * Uses getVotesForInstance to obtain the prediction and
     * the instance to obtain its true class.
     *
     *
     * @param inst the instance to be classified
     * @return true if the instance is correctly classified
     */
    public boolean correctlyClassifies(Instance inst);

    /**
     * Gets the current measurements of this classifier.
     *
     * @return an array of measurements to be used in evaluation tasks
     */
    public Measurement[] getModelMeasurements();

    /**
     * Gets the classifiers of this ensemble.
     * Returns null if this classifier is a single classifier.
     *
     * @return an array of the classifiers of the ensemble
     */
    public Classifier[] getSubClassifiers();

    /**
     * Produces a copy of this classifier.
     *
     * @return the copy of this classifier
     */
    public Classifier copy();
}
