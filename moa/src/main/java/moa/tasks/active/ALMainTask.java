/*
 *    ALMainTask.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
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
package moa.tasks.active;

import moa.tasks.MainTask;
import moa.tasks.TaskThread;

import java.util.List;

/**
 * This class provides a superclass for Active Learning tasks, which 
 * enables convenient searching for those tasks for example when showing 
 * a list of available Active Learning tasks.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public abstract class ALMainTask extends MainTask {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Get the list of threads for all subtasks and recursively the children's
	 * subtasks.
	 * 
	 * @return list of subtask threads, recursively generated
	 */
	public abstract List<ALTaskThread> getSubtaskThreads();
	
	/**
	 * Get the task's display name consisting of the general task name and 
	 * potentially extensions for the parent's name, fold or budget indices.
	 * 
	 * @return display name
	 */
	public abstract String getDisplayName();
	
	/**
	 * Check if the task is a subtask of another parent.
	 * 
	 * @return if the task is a subtask
	 */
	public abstract boolean isSubtask();
}
