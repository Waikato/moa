/*
 *    FixedBM.java
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

public class FixedBM extends AbstractOptionHandler implements BudgetManager {

    
	@Override
    public String getPurposeString() {
        return "Budget manager with a fixed budget.";
    }
	
	public FloatOption budgetOption = new FloatOption("budget",
    		'b', "The budget that should be used by the BudgetManager.",
    		0.1, 0.00, 1.00);
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	int acquisitionReport = 0;
	
	@Override
	public boolean isAbove(double value) {
		boolean acquire = false;
		if (value >= (1-budgetOption.getValue()))
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
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		
	}

}
