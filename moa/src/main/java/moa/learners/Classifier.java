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
package moa.learners;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * Classifier interface for incremental classification models.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public interface Classifier extends InstanceLearner {

    /**
     * Gets the classifiers of this ensemble. Returns null if this learner is a
     * single learner.
     *
     * @return an array of the learners of the ensemble
     */
    // public Classifier[] getSubClassifiers();

    /**
     * Predicts the class memberships for a given instance. If an instance is
     * unclassified, the returned array elements must be all zero.
     *
     * @param inst the instance to be classified
     * @return an array containing the estimated membership probabilities of the
     * test instance in each class
     */
    //public Prediction getPredictionForInstance(Instance inst);
    
    public boolean correctlyClassifies(Instance inst);
    
    /**
     * Sets the reference to the header of the data stream. The header of the
     * data stream is extended from WEKA
     * <code>Instances</code>. This header is needed to know the number of
     * classes and attributes
     *
     * @param ih the reference to the data stream header
     */
    //public void setModelContext(InstancesHeader ih);
    
    public InstanceLearner copy();
}
