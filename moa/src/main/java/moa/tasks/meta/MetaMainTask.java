/*
 *    MetaMainTask.java
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
package moa.tasks.meta;

import moa.tasks.MainTask;
import moa.tasks.TaskThread;

import java.awt.Color;
import java.util.List;

/**
 * This class provides features for handling tasks in a tree-like 
 * structure of parents and subtasks.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public abstract class MetaMainTask extends MainTask {
	
	private static final long serialVersionUID = 1L;
	
	protected boolean[] isLastSubtaskOnLevel = {};
	
	protected String nameSuffix = "";
	
	protected Color colorCoding = Color.BLACK;
	
	/**
	 * Get the list of threads for all subtasks and recursively the children's
	 * subtasks.
	 * 
	 * @return list of subtask threads, recursively generated
	 */
	public abstract List<? extends TaskThread> getSubtaskThreads();
	
	/**
	 * Get the task's display name consisting of the general task name, 
	 * indentation showing the tree structure depending on the subtask level
	 * and optionally a name suffix given from a supertask.
	 * 
	 * @return display name
	 */
	public String getDisplayName() {
		StringBuilder name = new StringBuilder();
		
		// add indentation representing tree structure of tasks
		for (int i = 0; i < this.getSubtaskLevel() -1; i++) {
			if (this.isLastSubtaskOnLevel[i]) {
				name.append("         ");
			}
			else {
				name.append("│      ");
			}
		}
		if (this.getSubtaskLevel() > 0) {
			if (this.isLastSubtaskOnLevel[this.getSubtaskLevel() - 1]) 
			{
				name.append("└──");
			}
			else {
				name.append("├──");
			}
		}
		
		// append class name
		name.append(this.getClass().getSimpleName());
		
		// append optional suffix
		name.append(" ").append(this.nameSuffix);
		
		return name.toString();
	}
	
	/**
	 * Set a suffix for the tasks display name.
	 * 
	 * @param suffix
	 */
	public void setNameSuffix(String suffix) {
		this.nameSuffix = suffix;
	}
	
	/**
	 * Set the list of booleans indicating if the current branch in the 
	 * subtask tree is the last one on its respective level.
	 * 
	 * @param parentIsLastSubtaskList the internal list of the parent
	 * @param isLastSubtask if the current subtask is the parents last one
	 */
	public void setIsLastSubtaskOnLevel(
			boolean[] parentIsLastSubtaskList, boolean isLastSubtask)
	{
		this.isLastSubtaskOnLevel = 
				new boolean[parentIsLastSubtaskList.length + 1];
		
		for (int i = 0; i < parentIsLastSubtaskList.length; i++) {
			this.isLastSubtaskOnLevel[i] = parentIsLastSubtaskList[i];
		}
		this.isLastSubtaskOnLevel[parentIsLastSubtaskList.length] = 
				isLastSubtask;
	}
	
	/**
	 * Get the tasks subtask level (how deep it is in the tree).
	 * 0 is the root task level.
	 * 
	 * @return
	 */
	public int getSubtaskLevel() {
		return this.isLastSubtaskOnLevel.length;
	}
	
	/**
	 * Check if the task is a subtask of another parent.
	 * 
	 * @return true if the task is a subtask
	 */
	public boolean isSubtask() {
		return this.getSubtaskLevel() > 0;
	}
	
	/**
	 * Set the color coding for this task (the color which is used for multi-curve plots).
	 * 
	 * @param newColorCoding the new color coding for this task
	 */
	public void setColorCoding(Color newColorCoding) {
		this.colorCoding = newColorCoding;
	}
	
	/**
	 * Get the color coding for this task (the color which is used for multi-curve plots).
	 * 
	 * @return the color coding for this task
	 */
	public Color getColorCoding() {
		return this.colorCoding;
	}
}
