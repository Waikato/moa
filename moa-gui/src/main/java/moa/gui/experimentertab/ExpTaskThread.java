/*
 *    ExpTaskThread.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @modified Alberto Verdecia (averdeciac@gmail.com)
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
package moa.gui.experimentertab;

import java.util.concurrent.CopyOnWriteArraySet;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.tasks.MainTask;
import moa.tasks.ResultPreviewListener;
import moa.tasks.StandardTaskMonitor;
import moa.tasks.Task;
import moa.tasks.TaskCompletionListener;
import moa.tasks.TaskMonitor;

/**
 * Task Thread.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @modified Alberto Verdecia (averdeciac@gmail.com)  
 */
public class ExpTaskThread extends Thread {

    Buffer tasks;

    public static enum Status {

        NOT_STARTED, RUNNING, PAUSED, CANCELLING, CANCELLED, COMPLETED, FAILED
    }

    protected MainTask runningTask;
    
    protected volatile Status currentStatus;

    protected TaskMonitor taskMonitor;

    protected ObjectRepository repository;

    protected Object finalResult;

    protected long taskStartTime;

    protected long taskEndTime;

    protected double latestPreviewGrabTime = 0.0;
    
    public boolean isCompleted = false;

    CopyOnWriteArraySet<TaskCompletionListener> completionListeners = new CopyOnWriteArraySet<TaskCompletionListener>();

    public ExpTaskThread(Buffer buf) {
        this.tasks = buf;
        this.currentStatus = ExpTaskThread.Status.NOT_STARTED;
        this.taskMonitor = new StandardTaskMonitor();
        this.repository =null;
       
    }

    @Override
    public void run() {
        TimingUtils.enablePreciseTiming();
        this.taskStartTime = TimingUtils.getNanoCPUTimeOfThread(getId());
        while (this.tasks.getCantTask() != this.tasks.getSize()) {
            this.runningTask = this.tasks.getTask();
            this.currentStatus = ExpTaskThread.Status.RUNNING;
            this.taskMonitor.setCurrentActivityDescription("Running task " + this.runningTask);
            this.finalResult = this.runningTask.doTask(this.taskMonitor, this.repository);
            this.currentStatus = this.taskMonitor.isCancelled() ? ExpTaskThread.Status.CANCELLED
                    : ExpTaskThread.Status.COMPLETED;
            //System.out.println(this.taskMonitor.getCurrentActivityFractionComplete()*100); 
        }
        this.isCompleted = true;
    }
   public String getCurrentActivityString() {
        return (isComplete() || (this.currentStatus == ExpTaskThread.Status.NOT_STARTED)) ? ""
                : this.taskMonitor.getCurrentActivityDescription();
    }
       
    public boolean isComplete() {
        return ((this.currentStatus == ExpTaskThread.Status.CANCELLED)
                || (this.currentStatus == ExpTaskThread.Status.COMPLETED) || (this.currentStatus == ExpTaskThread.Status.FAILED));
    }
    public double getCPUSecondsElapsed() {
        double secondsElapsed = 0.0;
        if (this.currentStatus == ExpTaskThread.Status.NOT_STARTED) {
            secondsElapsed = 0.0;
        } else if (isComplete()) {
            secondsElapsed = TimingUtils.nanoTimeToSeconds(this.taskEndTime
                    - this.taskStartTime);
        } else {
            secondsElapsed = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfThread(getId())
                    - this.taskStartTime);
        }
        return secondsElapsed > 0.0 ? secondsElapsed : 0.0;
    }
    public Task getTask() {
        return this.runningTask;
    }
     public String getCurrentStatusString() {
        switch (this.currentStatus) {
            case NOT_STARTED:
                return "not started";
            case RUNNING:
                return "running";
            case PAUSED:
                return "paused";
            case CANCELLING:
                return "cancelling";
            case CANCELLED:
                return "cancelled";
            case COMPLETED:
                return "completed";
            case FAILED:
                return "failed";
        }
        return "unknown";
    }
     public double getCurrentActivityFracComplete() {
        switch (this.currentStatus) {
            case NOT_STARTED:
                return 0.0;
            case RUNNING:
            case PAUSED:
            case CANCELLING:
                return this.taskMonitor.getCurrentActivityFractionComplete();
            case CANCELLED:
            case COMPLETED:
            case FAILED:
                return 1.0;
        }
        return 0.0;
    }
      public Object getFinalResult() {
        return this.finalResult;
    }

    public void addTaskCompletionListener(TaskCompletionListener tcl) {
        this.completionListeners.add(tcl);
    }

    public void removeTaskCompletionListener(TaskCompletionListener tcl) {
        this.completionListeners.remove(tcl);
    }
   
    public void getPreview(ResultPreviewListener previewer) {
        this.taskMonitor.requestResultPreview(previewer);
        this.latestPreviewGrabTime = getCPUSecondsElapsed();
    }

    public Object getLatestResultPreview() {
        return this.taskMonitor.getLatestResultPreview();
    }

    public double getLatestPreviewGrabTimeSeconds() {
        return this.latestPreviewGrabTime;
    }
     public synchronized void pauseTask() {
        if (this.currentStatus == Status.RUNNING) {
            this.taskMonitor.requestPause();
            this.currentStatus = Status.PAUSED;
        }
    }

    public synchronized void resumeTask() {
        if (this.currentStatus == Status.PAUSED) {
            this.taskMonitor.requestResume();
            this.currentStatus = Status.RUNNING;
        }
    }

    public synchronized void cancelTask() {
        if ((this.currentStatus == Status.RUNNING)
                || (this.currentStatus == Status.PAUSED)) {
            this.taskMonitor.requestCancel();
            this.currentStatus = Status.CANCELLING;
        }
    }
   
}
