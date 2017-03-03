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

import java.util.ArrayList;
import java.util.List;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.ListOption;
import com.github.javacliparser.Option;

import moa.classifiers.active.ALClassifier;
import moa.core.ObjectRepository;
import moa.evaluation.ALClassificationPerformanceEvaluator;
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
			"prequentialEvaluator", 'e',
            "Prequential classification performance evaluation method.",
            ALClassificationPerformanceEvaluator.class,
            "ALBasicClassificationPerformanceEvaluator");
	
	public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
            "Maximum number of instances to test/train on  (-1 = no limit).",
            100000000, -1, Integer.MAX_VALUE);
	
	/* options used in in this class */
	public ListOption budgetsOption = new ListOption("budgets", 'b',
			"List of budgets to train classifiers for.",
			new FloatOption("budget", 't', "Active learner budget.", 0.9), 
			new Option[0], ',');
	
	public ClassOption multiBudgetEvaluatorOption = new ClassOption(
			"multiBudgetEvaluator", 'm',
            "Multi-budget classification performance evaluation method.",
            ALClassificationPerformanceEvaluator.class,
            "ALBasicClassificationPerformanceEvaluator");
	
	
	private ArrayList<ALPrequentialEvaluationTask> subtasks = new ArrayList<>();
	private ArrayList<ALTaskThread> subtaskThreads = new ArrayList<>();
	private ArrayList<ALTaskThread> flattenedSubtaskThreads = new ArrayList<>();
	
	@Override
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}
	
	
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		super.prepareForUseImpl(monitor, repository);
		
		// setup task for each budget
		Option[] budgets = this.budgetsOption.getList();
		for (int i = 0; i < budgets.length; i++) {
			
			// create subtask
			ALPrequentialEvaluationTask budgetTask = 
					new ALPrequentialEvaluationTask();
			budgetTask.setIsLastSubtaskOnLevel(
					this.isLastSubtaskOnLevel, i == budgets.length - 1);
			
			for (Option opt : budgetTask.getOptions().getOptionArray()) {
				switch (opt.getName()) {
				case "learner":
					opt.setValueViaCLIString(
							this.learnerOption.getValueAsCLIString());
					break;
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
					opt.setValueViaCLIString(budgets[i].getValueAsCLIString());
					break;
				case "instanceLimit":
					opt.setValueViaCLIString(
							this.instanceLimitOption.getValueAsCLIString());
					break;
				}
			}
			
			budgetTask.prepareForUse();
			
			List<ALTaskThread> childSubtasks = budgetTask.getSubtaskThreads();
			
			// add new subtask and its thread to lists
			this.subtasks.add(budgetTask);
			
			ALTaskThread subtaskThread = new ALTaskThread(budgetTask);
			this.subtaskThreads.add(subtaskThread);

			this.flattenedSubtaskThreads.add(subtaskThread);
			this.flattenedSubtaskThreads.addAll(childSubtasks);
		}
	}
	
	@Override
	protected Object doMainTask(
			TaskMonitor monitor, ObjectRepository repository) 
	{
		// setup learning curve
		LearningCurve learningCurve = new LearningCurve(
				"learning evaluation instances");
		
		// start subtasks
		monitor.setCurrentActivity("Evaluating learners for budgets...", -1.0);
		for(int i = 0; i < this.subtaskThreads.size(); ++i)
		{
			subtaskThreads.get(i).start();
		}

		try {
			for(int i = 0; i < this.subtaskThreads.size(); ++i)
			{
					subtaskThreads.get(i).join();
			}		
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return learningCurve;
	}
	
	@Override
	public List<ALTaskThread> getSubtaskThreads() {
		return this.flattenedSubtaskThreads;
	}
}
