/*
 *    ThresholdBM.java
 *    Copyright (C) 2016 Otto von Guericke University, Magdeburg, Germany
 *    @author Daniel Kottke (daniel dot kottke at ovgu dot de)
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
package moa.classifiers.active.budget;

import com.github.javacliparser.FloatOption;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class ThresholdBM extends AbstractOptionHandler implements BudgetManager {

    
    public FloatOption thresholdOption = new FloatOption("budget",
    		't', "The threshold which has to be exceeded to acquire the label.",
    		0.1, 0.00, 1.00);
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	int acquisitionReport = 0;
    
	double threshold;
	
	@Override
	public boolean isAbove(double value) {
		boolean acquire = false;
		if (value >= this.threshold)
			acquire = true;
		if (acquire)
			this.acquisitionReport++;
		return acquire;
	}

	@Override
	public int getLastLabelAcqReport() {
		int helper = this.acquisitionReport;
		this.acquisitionReport = 0;
		return helper;
	}

	@Override
	public void resetLearning() {
		this.acquisitionReport = 0;
		this.threshold = thresholdOption.getValue();
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		
	}

}
