/*
 *    StandardTaskMonitor.java
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
 * Class that represents a standard task monitor.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class StandardTaskMonitor implements TaskMonitor {

    protected String currentActivityDescription = "";

    protected double currentActivityFractionComplete = -1.0;

    protected volatile boolean cancelFlag = false;

    protected volatile boolean pauseFlag = false;

    protected volatile boolean isComplete = false;

    protected volatile boolean resultPreviewRequested = false;

    protected volatile Object latestResultPreview = null;

    protected volatile ResultPreviewListener resultPreviewer = null;

    @Override
    public void setCurrentActivity(String activityDescription,
            double fracComplete) {
        setCurrentActivityDescription(activityDescription);
        setCurrentActivityFractionComplete(fracComplete);
    }

    @Override
    public void setCurrentActivityDescription(String activity) {
        this.currentActivityDescription = activity;
    }

    @Override
    public void setCurrentActivityFractionComplete(double fracComplete) {
        this.currentActivityFractionComplete = fracComplete;
    }

    @Override
    public boolean taskShouldAbort() {
        if (this.pauseFlag) {
            try {
                synchronized (this) {
                    while (this.pauseFlag && !this.cancelFlag) {
                        wait();
                    }
                }
            } catch (InterruptedException e) {
            }
        }
        return this.cancelFlag;
    }

    @Override
    public String getCurrentActivityDescription() {
        return this.currentActivityDescription;
    }

    @Override
    public double getCurrentActivityFractionComplete() {
        return this.currentActivityFractionComplete;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelFlag;
    }

    @Override
    public void requestCancel() {
        this.cancelFlag = true;
        requestResume();
    }

    @Override
    public void requestPause() {
        this.pauseFlag = true;
    }

    @Override
    public synchronized void requestResume() {
        this.pauseFlag = false;
        notify();
    }

    @Override
    public boolean isPaused() {
        return this.pauseFlag;
    }

    @Override
    public Object getLatestResultPreview() {
        return this.latestResultPreview;
    }

    @Override
    public void requestResultPreview() {
        this.resultPreviewRequested = true;
    }

    @Override
    public void requestResultPreview(ResultPreviewListener toInform) {
        this.resultPreviewer = toInform;
        this.resultPreviewRequested = true;
    }

    @Override
    public boolean resultPreviewRequested() {
        return this.resultPreviewRequested;
    }

    @Override
    public synchronized void setLatestResultPreview(Object latestPreview) {
        this.resultPreviewRequested = false;
        this.latestResultPreview = latestPreview;
        if (this.resultPreviewer != null) {
            this.resultPreviewer.latestPreviewChanged();
        }
        this.resultPreviewer = null;
    }
}
