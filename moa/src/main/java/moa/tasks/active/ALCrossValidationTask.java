/*
 *    ALCrossValidationTask.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
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
package moa.tasks.active;

import java.util.List;

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;
import moa.tasks.TaskThread;

/**
 * This task extensively evaluates an active learning classifier on a stream.
 * First, the given data set is partitioned into separate folds for performing
 * cross validation. On each fold, the ALMultiBudgetTask is performed which
 * individually evaluates the active learning classifier for each element of 
 * a set of budgets. The individual evaluation is done by prequential 
 * evaluation (testing, then training with each example in sequence).
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class ALCrossValidationTask extends ALMainTask {
	
	private static final long serialVersionUID = 1L;
	
	@Override
    public String getPurposeString() {
        return "Evaluates an active learning classifier on a stream by" +
                " performing cross validation and on each fold evaluating" +
        		" the classifier for each element of a set of budgets using" +
                " prequential evaluation (testing, then training with each)" +
        		" example in  sequence).";
    }
	
	@Override
	public Class<?> getTaskResultType() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<ALTaskThread> getSubtaskThreads() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isSubtask() {
		// TODO Auto-generated method stub
		return false;
	}
}
