/*
 *    MainTask.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
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
package moa.tasks;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import moa.core.ObjectRepository;
import moa.core.SerializeUtils;
import moa.options.FileOption;

public abstract class MainTask extends AbstractTask {

	private static final long serialVersionUID = 1L;

	protected static final int INSTANCES_BETWEEN_MONITOR_UPDATES = 10;

	public FileOption outputFileOption = new FileOption("taskResultFile", 'O',
			"File to save the final result of the task to.", null, "moa", true);

	@Override
	protected Object doTaskImpl(TaskMonitor monitor, ObjectRepository repository) {
		Object result = doMainTask(monitor, repository);
		if (monitor.taskShouldAbort()) {
			return null;
		}
		File outputFile = this.outputFileOption.getFile();
		if (outputFile != null) {
			if (result instanceof Serializable) {
				monitor.setCurrentActivity("Saving result of task "
						+ getTaskName() + " to file " + outputFile + "...",
						-1.0);
				try {
					SerializeUtils.writeToFile(outputFile,
							(Serializable) result);
				} catch (IOException ioe) {
					throw new RuntimeException("Failed writing result of task "
							+ getTaskName() + " to file " + outputFile, ioe);
				}
			} else {
				throw new RuntimeException("Result of task " + getTaskName()
						+ " is not serializable, so cannot be written to file "
						+ outputFile);
			}
		}
		return result;
	}

	protected abstract Object doMainTask(TaskMonitor monitor,
			ObjectRepository repository);

}
