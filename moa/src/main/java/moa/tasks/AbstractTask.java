/*
 *    AbstractTask.java
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

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;

/**
 * Abstract Task. All runnable tasks in MOA extend this class.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public abstract class AbstractTask extends AbstractOptionHandler implements
        Task {

    /**
     * Gets the name of this task.
     *
     * @return the name of this task
     */
    public String getTaskName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Object doTask() {
        return doTask(new NullMonitor(), null);
    }

    @Override
    public Object doTask(TaskMonitor monitor, ObjectRepository repository) {
        monitor.setCurrentActivity("Preparing options to " + getTaskName()
                + "...", -1.0);
        prepareClassOptions(monitor, repository);
        if (monitor.taskShouldAbort()) {
            return null;
        }
        monitor.setCurrentActivity("Doing task " + getTaskName() + "...", -1.0);
        Object result = doTaskImpl(monitor, repository);
        monitor.setCurrentActivity("Task " + getTaskName() + " complete.", 1.0);
        //this.classOptionNamesToPreparedObjects = null; // clean up refs
        return result;
    }

    /**
     * This method performs this task.
     * <code>AbstractTask</code> implements <code>doTask</code> so all
     * its extensions only need to implement <code>doTaskImpl</code>.
     *
     * @param monitor the TaskMonitor to use
     * @param repository  the ObjectRepository to use
     * @return an object with the result of this task
     */
    protected abstract Object doTaskImpl(TaskMonitor monitor,
            ObjectRepository repository);

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // tasks prepare themselves upon running
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
