/*
 *    TaskThread.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    Modified Work: Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tuan Pham Minh (tuan.pham@ovgu.de)
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

import java.util.List;

import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.tasks.FailedTaskReport;
import moa.tasks.Task;
import moa.tasks.TaskThread;

/**
 * Task Thread for ALMainTask which supports pausing/resuming and cancelling of child threads
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class ALTaskThread extends TaskThread {

	public ALTaskThread(Task toRun) {
		super(toRun, null);
	}

	public ALTaskThread(Task toRun, ObjectRepository repository) {
		super(toRun, repository);
	}
	
	@Override
    public synchronized void pauseTask() {
		ALMainTask task = (ALMainTask)getTask();
		List<ALTaskThread> threads = task.getSubtaskThreads();
		
        super.pauseTask();
        
        // pause all subtask threads
        for(int i = 0; i < threads.size(); ++i)
        {
        	threads.get(i).pauseTask();
        }
    }

	@Override
    public synchronized void resumeTask() {
		ALMainTask task = (ALMainTask)getTask();
		List<ALTaskThread> threads = task.getSubtaskThreads();
		
        super.resumeTask();

        // resume all subtask threads
        for(int i = 0; i < threads.size(); ++i)
        {
        	threads.get(i).resumeTask();
        }
    }

	@Override
    public synchronized void cancelTask() {
		ALMainTask task = (ALMainTask)getTask();
		List<ALTaskThread> threads = task.getSubtaskThreads();
		
        super.cancelTask();
        
        if(!isFailed())
        	this.finalResult = getLatestResultPreview();
        

        // cancel all subtask threads
        for(int i = 0; i < threads.size(); ++i)
        {
        	if(!threads.get(i).isComplete())
        	{
            	threads.get(i).cancelTask();
        	}
        }
    }

    @Override
    public void run() {
        TimingUtils.enablePreciseTiming();
        this.taskStartTime = TimingUtils.getNanoCPUTimeOfThread(getId());
        try {
            this.currentStatus = Status.RUNNING;
            this.finalResult = this.runningTask.doTask(this.taskMonitor,
                    this.repository);
            this.currentStatus = this.taskMonitor.isCancelled() ? Status.CANCELLED
                    : Status.COMPLETED;
        } catch (Throwable ex) {
            this.finalResult = new FailedTaskReport(ex);
            this.currentStatus = Status.FAILED;
        }
        
        if(currentStatus == Status.FAILED || currentStatus == Status.CANCELLED)
        {
            cancelTask();
        }
        
        this.taskEndTime = TimingUtils.getNanoCPUTimeOfThread(getId());
        fireTaskCompleted();
        this.taskMonitor.setLatestResultPreview(null); // free preview memory
    }
}
