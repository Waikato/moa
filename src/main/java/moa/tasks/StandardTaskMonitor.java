/*
 *    StandardTaskMonitor.java
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

public class StandardTaskMonitor implements TaskMonitor {

	protected String currentActivityDescription = "";

	protected double currentActivityFractionComplete = -1.0;

	protected volatile boolean cancelFlag = false;

	protected volatile boolean pauseFlag = false;

	protected volatile boolean isComplete = false;

	protected volatile boolean resultPreviewRequested = false;

	protected volatile Object latestResultPreview = null;

	protected volatile ResultPreviewListener resultPreviewer = null;

	public void setCurrentActivity(String activityDescription,
			double fracComplete) {
		setCurrentActivityDescription(activityDescription);
		setCurrentActivityFractionComplete(fracComplete);
	}

	public void setCurrentActivityDescription(String activity) {
		this.currentActivityDescription = activity;
	}

	public void setCurrentActivityFractionComplete(double fracComplete) {
		this.currentActivityFractionComplete = fracComplete;
	}

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

	public String getCurrentActivityDescription() {
		return this.currentActivityDescription;
	}

	public double getCurrentActivityFractionComplete() {
		return this.currentActivityFractionComplete;
	}

	public boolean isCancelled() {
		return this.cancelFlag;
	}

	public void requestCancel() {
		this.cancelFlag = true;
		requestResume();
	}

	public void requestPause() {
		this.pauseFlag = true;
	}

	public synchronized void requestResume() {
		this.pauseFlag = false;
		notify();
	}

	public boolean isPaused() {
		return this.pauseFlag;
	}

	public Object getLatestResultPreview() {
		return this.latestResultPreview;
	}

	public void requestResultPreview() {
		this.resultPreviewRequested = true;
	}

	public void requestResultPreview(ResultPreviewListener toInform) {
		this.resultPreviewer = toInform;
		this.resultPreviewRequested = true;
	}

	public boolean resultPreviewRequested() {
		return this.resultPreviewRequested;
	}

	public synchronized void setLatestResultPreview(Object latestPreview) {
		this.resultPreviewRequested = false;
		this.latestResultPreview = latestPreview;
		if (this.resultPreviewer != null) {
			this.resultPreviewer.latestPreviewChanged();
		}
		this.resultPreviewer = null;
	}

}
