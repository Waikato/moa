/*
 *    ClassificationPerformanceEvaluator.java
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
package moa.evaluation;

import moa.MOAObject;
import moa.core.Measurement;
import weka.core.Instance;

/**
 * Interface implemented by learner evaluators to monitor
 * the results of the learning process.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public interface ClassificationPerformanceEvaluator extends MOAObject {

    /**
     * Resets this evaluator. It must be similar to
     * starting a new evaluator from scratch.
     *
     */
    public void reset();

    //public void addClassificationAttempt(int trueClass, double[] classVotes,
    //		double weight);
    /**
     * Adds a learning result to this evaluator.
     *
     * @param inst the instance to be classified
     * @param classVotes an array containing the estimated membership
     * probabilities of the test instance in each class
     * @return an array of measurements monitored in this evaluator
     */
    public void addResult(Instance inst, double[] classVotes);

    /**
     * Gets the current measurements monitored by this evaluator.
     *
     * @return an array of measurements monitored by this evaluator
     */
    public Measurement[] getPerformanceMeasurements();
}
