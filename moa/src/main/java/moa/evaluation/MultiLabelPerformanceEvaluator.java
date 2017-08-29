/*
 *    MultiLabelPerformanceEvaluator.java
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

import moa.core.Example;
import com.yahoo.labs.samoa.instances.Instance;


/**
 * Interface implemented by learner evaluators to monitor
 * the results of the regression learning process.
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 * @version $Revision: 7 $
 */
public interface MultiLabelPerformanceEvaluator extends LearningPerformanceEvaluator<Example<Instance>> {

    
}
