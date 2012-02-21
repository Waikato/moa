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
     * @param activity the description of the current activity
     * @param fracComplete the percentage done of the current activity
     */
    public void setCurrentActivity(String activityDescription,
            double fracComplete);

    /**
     * Sets the description of the current activity.
     *
     * @param activity the description of the current activity
     */
    public void setCurrentActivityDescription(String activity);

    /**
     * Sets the percentage done of the current activity
     *
     * @param fracComplete the percentage done of the current activity
     */
    public void setCurrentActivityFractionComplete(double fracComplete);

    /**
     * Gets whether the task should abort.
     *
     * @return true if the task should abort
     */
    public boolean taskShouldAbort();

    /**
     * Gets whether there is a request for preview the task result.
     *
     * @return true if there is a request for preview the task result
     */
    public boolean resultPreviewRequested();

    /**
     * Sets the current result to preview
     *
     * @param latestPreview the result to preview
     */
    public void setLatestResultPreview(Object latestPreview);

    /**
     * Gets the description of the current activity.
     *
     * @return the description of the current activity
     */
    public String getCurrentActivityDescription();

    /**
     * Gets the percentage done of the current activity
     *
     * @return the percentage done of the current activity
     */
    public double getCurrentActivityFractionComplete();

    /**
     * Requests the task monitored to pause.
     *
     */
    public void requestPause();

    /**
     * Requests the task monitored to resume.
     *
     */
    public void requestResume();

    /**
     * Requests the task monitored to cancel.
     *
     */
    public void requestCancel();

    /**
     * Gets whether the task monitored is paused.
     *
     * @return true if the task is paused
     */
    public boolean isPaused();

    /**
     * Gets whether the task monitored is cancelled.
     *
     * @return true if the task is cancelled
     */
    public boolean isCancelled();

    /**
     * Requests to preview the task result.
     *
     */
    public void requestResultPreview();

    /**
     * Requests to preview the task result.
     *
     * @param toInform the listener of the changes in the preview of the result
     */
    public void requestResultPreview(ResultPreviewListener toInform);

    /**
     * Gets the current result to preview
     *
     * @return the result to preview
     */
    public Object getLatestResultPreview();
}
