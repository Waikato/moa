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

import moa.core.Example;
import moa.learners.Learner;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Classifier interface for incremental classification models.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public interface Classifier extends Learner<Example<Instance>> {

    /**
     * Gets the classifiers of this ensemble. Returns null if this learner is a
     * single learner.
     *
     * @return an array of the learners of the ensemble
     */
    public Classifier[] getSubClassifiers();

    /**
     * Produces a copy of this learner.
     *
     * @return the copy of this learner
     */
    public Classifier copy();

    /**
     * Gets whether this classifier correctly classifies an instance. Uses
     * getVotesForInstance to obtain the prediction and the instance to obtain
     * its true class.
     *
     *
     * @param inst the instance to be classified
     * @return true if the instance is correctly classified
     */
    public boolean correctlyClassifies(Instance inst);

    /**
     * Trains this learner incrementally using the given example.
     *
     * @param inst the instance to be used for training
     */
    public void trainOnInstance(Instance inst);

    /**
     * Predicts the class memberships for a given instance. If an instance is
     * unclassified, the returned array elements must be all zero.
     *
     * @param inst the instance to be classified
     * @return an array containing the estimated membership probabilities of the
     * test instance in each class
     */
    public double[] getVotesForInstance(Instance inst);
    
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
    
    public Prediction getPredictionForInstance(Instance inst);
}
