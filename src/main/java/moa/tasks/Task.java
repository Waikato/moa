/*
 *    Task.java
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
package moa.tasks;

import moa.MOAObject;
import moa.core.ObjectRepository;

/**
 * Interface representing a task. 
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $ 
 */
public interface Task extends MOAObject {

    /**
     * Gets the result type of this task.
     * Tasks can return LearningCurve, LearningEvaluation,
     * Classifier, String, Instances..
     *
     * @return a class object of the result of this task
     */
    public Class<?> getTaskResultType();

    /**
     * This method performs this task,
     * when TaskMonitor and ObjectRepository are no needed.
     *
     * @return an object with the result of this task
     */
    public Object doTask();

    /**
     * This method performs this task.
     * <code>AbstractTask</code> implements this method so all
     * its extensions only need to implement <code>doTaskImpl</code>
     *
     * @param monitor the TaskMonitor to use
     * @param repository  the ObjectRepository to use
     * @return an object with the result of this task
     */
    public Object doTask(TaskMonitor monitor, ObjectRepository repository);
}
