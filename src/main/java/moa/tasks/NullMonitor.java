/*
 *    NullMonitor.java
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
 * Class that represents a null monitor.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class NullMonitor implements TaskMonitor {

    @Override
    public void setCurrentActivity(String activityDescription,
            double fracComplete) {
    }

    @Override
    public void setCurrentActivityDescription(String activity) {
    }

    @Override
    public void setCurrentActivityFractionComplete(double fracComplete) {
    }

    @Override
    public boolean taskShouldAbort() {
        return false;
    }

    @Override
    public String getCurrentActivityDescription() {
        return null;
    }

    @Override
    public double getCurrentActivityFractionComplete() {
        return -1.0;
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void requestCancel() {
    }

    @Override
    public void requestPause() {
    }

    @Override
    public void requestResume() {
    }

    @Override
    public Object getLatestResultPreview() {
        return null;
    }

    @Override
    public void requestResultPreview() {
    }

    @Override
    public boolean resultPreviewRequested() {
        return false;
    }

    @Override
    public void setLatestResultPreview(Object latestPreview) {
    }

    @Override
    public void requestResultPreview(ResultPreviewListener toInform) {
    }
}
