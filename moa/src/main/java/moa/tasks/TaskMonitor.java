/*
 *    TaskMonitor.java
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

/**
 * Interface representing a task monitor.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public interface TaskMonitor {

	/**
	 * Sets the description and the percentage done of the current activity.
	 *
	 * @param activity     the description of the current activity
	 * @param fracComplete the percentage done of the current activity
	 */
	void setCurrentActivity(String activityDescription, double fracComplete);

	/**
	 * Sets the description of the current activity.
	 *
	 * @param activity the description of the current activity
	 */
	void setCurrentActivityDescription(String activity);

	/**
	 * Sets the percentage done of the current activity
	 *
	 * @param fracComplete the percentage done of the current activity
	 */
	void setCurrentActivityFractionComplete(double fracComplete);

	/**
	 * Gets whether the task should abort.
	 *
	 * @return true if the task should abort
	 */
	boolean taskShouldAbort();

	/**
	 * Gets whether there is a request for preview the task result.
	 *
	 * @return true if there is a request for preview the task result
	 */
	boolean resultPreviewRequested();

	/**
	 * Sets the current result to preview
	 *
	 * @param latestPreview the result to preview
	 */
	void setLatestResultPreview(Object latestPreview);

	/**
	 * Gets the description of the current activity.
	 *
	 * @return the description of the current activity
	 */
	String getCurrentActivityDescription();

	/**
	 * Gets the percentage done of the current activity
	 *
	 * @return the percentage done of the current activity
	 */
	double getCurrentActivityFractionComplete();

	/**
	 * Requests the task monitored to pause.
	 *
	 */
	void requestPause();

	/**
	 * Requests the task monitored to resume.
	 *
	 */
	void requestResume();

	/**
	 * Requests the task monitored to cancel.
	 *
	 */
	void requestCancel();

	/**
	 * Gets whether the task monitored is paused.
	 *
	 * @return true if the task is paused
	 */
	boolean isPaused();

	/**
	 * Gets whether the task monitored is cancelled.
	 *
	 * @return true if the task is cancelled
	 */
	boolean isCancelled();

	/**
	 * Requests to preview the task result.
	 *
	 */
	void requestResultPreview();

	/**
	 * Requests to preview the task result.
	 *
	 * @param toInform the listener of the changes in the preview of the result
	 */
	void requestResultPreview(ResultPreviewListener toInform);

	/**
	 * Gets the current result to preview
	 *
	 * @return the result to preview
	 */
	Object getLatestResultPreview();
}
