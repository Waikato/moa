/*
 *    ALEvaluator.java
 *    Copyright (C) 2016 Otto von Guericke University, Magdeburg, Germany
 *    @author Daniel Kottke (daniel.kottke@ovgu.de)
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

import com.yahoo.labs.samoa.instances.Instance;

import moa.core.Example;

/**
 * Active Learning Evaluator Interface to make AL Evaluators selectable in AL tasks.
 *
 * @author Daniel Kottke (daniel.kottke@ovgu.de)
 * @version $Revision: 1 $
 */
public interface ALClassificationPerformanceEvaluator extends ClassificationPerformanceEvaluator {
	
	   /**
	     * Reports if a label of an instance was acquired.
	     *
	     * @param trainInst the instance that was previously considered
	     * @param labelAcquired bool type which indicates if trainInst 
	     *        was acquired by the active learner
	     */
		public void doLabelAcqReport(Example<Instance> trainInst, int labelAcquired);

}
