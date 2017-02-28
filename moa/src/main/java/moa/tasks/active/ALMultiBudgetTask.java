/*
 *    ALMultiBudgetTask.java
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

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.ListOption;
import com.github.javacliparser.Option;

import moa.classifiers.active.ALClassifier;
import moa.core.ObjectRepository;
import moa.evaluation.ALEvaluator;
import moa.evaluation.LearningCurve;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.tasks.TaskMonitor;

/**
 * This task individually evaluates an active learning classifier for each 
 * element of a set of budgets. The individual evaluation is done by 
 * prequential evaluation (testing, then training with each example in 
 * sequence).
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class ALMultiBudgetTask extends ALMainTask {
	
	private static final long serialVersionUID = 1L;
	
	/* options actually used in ALPrequentialEvaluationTask */
	public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Learner to train.", ALClassifier.class, 
            "moa.classifiers.active.ALZliobaite2011");
	
	public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", ExampleStream.class,
            "generators.RandomTreeGenerator");
	
	public ClassOption prequentialEvaluatorOption = new ClassOption(
			"prequential evaluator", 'e',
            "Prequential classification performance evaluation method.",
            ALEvaluator.class,
            "ALBasicClassificationPerformanceEvaluator");
	
	/* options used in in this class */
	public ListOption budgetsOption = new ListOption("budgets", 'b',
			"List of budgets to train classifiers for.",
			new FloatOption("budget", 't', "Active learner budget.", 0.9), 
			new Option[0], ',');
	
	public ClassOption multiBudgetEvaluatorOption = new ClassOption(
			"multi-budget evaluator", 'm',
            "Multi-budget classification performance evaluation method.",
            ALEvaluator.class,
            "ALBasicClassificationPerformanceEvaluator");
	
	
	private List<ALPrequentialEvaluationTask> subtasks;
	private List<ALTaskThread> subtaskThreads;
	
	
	@Override
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}
	
	@Override
	protected Object doMainTask(
			TaskMonitor monitor, ObjectRepository repository) 
	{
		// setup learning curve
		LearningCurve learningCurve = new LearningCurve(
                "multi-budget evaluation");
		
		// setup task for each budget
		FloatOption[] budgets = (FloatOption[]) this.budgetsOption.getList();
		for (FloatOption budget : budgets) {
			
			// create subtask
			ALPrequentialEvaluationTask budgetTask = 
					new ALPrequentialEvaluationTask();
			budgetTask.setIsSubtask(true);
			
			for (Option opt : budgetTask.getOptions().getOptionArray()) {
				switch (opt.getName()) {
				case "learner":
					opt.setValueViaCLIString(
							this.learnerOption.getValueAsCLIString());
				case "stream": 
					opt.setValueViaCLIString(
							this.streamOption.getValueAsCLIString());
					break;
				case "prequential evaluator":
					opt.setValueViaCLIString(
							this.prequentialEvaluatorOption
							.getValueAsCLIString());
					break;
				case "budget":
					opt.setValueViaCLIString(budget.getValueAsCLIString());
					break;
				}
			}
			
			// add new subtask to list
			this.subtasks.add(budgetTask);
			
			// TODO: run task and perform evaluation
		}
		
		return learningCurve;
	}
	
	@Override
	public List<ALTaskThread> getSubtaskThreads() {
		return this.subtaskThreads;
	}
	
	@Override
	public String getDisplayName() {
		if (this.isSubtask()) {
			return "|-- ALMultiBudgetTask";
		}
		else {
			return "ALMultiBudgetTask";
		}
	}
}
