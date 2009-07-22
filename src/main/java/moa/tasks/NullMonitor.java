/*
 *    NullMonitor.java
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

public class NullMonitor implements TaskMonitor {

	public void setCurrentActivity(String activityDescription,
			double fracComplete) {

	}

	public void setCurrentActivityDescription(String activity) {

	}

	public void setCurrentActivityFractionComplete(double fracComplete) {

	}

	public boolean taskShouldAbort() {
		return false;
	}

	public String getCurrentActivityDescription() {
		return null;
	}

	public double getCurrentActivityFractionComplete() {
		return -1.0;
	}

	public boolean isPaused() {
		return false;
	}

	public boolean isCancelled() {
		return false;
	}

	public void requestCancel() {

	}

	public void requestPause() {

	}

	public void requestResume() {

	}

	public Object getLatestResultPreview() {
		return null;
	}

	public void requestResultPreview() {

	}

	public boolean resultPreviewRequested() {
		return false;
	}

	public void setLatestResultPreview(Object latestPreview) {

	}

	public void requestResultPreview(ResultPreviewListener toInform) {

	}

}
