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
			"prequentialEvaluator", 'e',
            "Prequential classification performance evaluation method.",
            ALEvaluator.class,
            "ALBasicClassificationPerformanceEvaluator");
	
	/* options used in in this class */
	public ListOption budgetsOption = new ListOption("budgets", 'b',
			"List of budgets to train classifiers for.",
			new FloatOption("budget", 't', "Active learner budget.", 0.9), 
			new Option[0], ',');
	
	public ClassOption multiBudgetEvaluatorOption = new ClassOption(
			"multiBudgetEvaluator", 'm',
            "Multi-budget classification performance evaluation method.",
            ALEvaluator.class,
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
		// TODO Auto-generated method stub
		super.prepareForUseImpl(monitor, repository);
		
		// setup task for each budget
		Option[] budgets = this.budgetsOption.getList();
		for (Option budget : budgets) {
			
			// create subtask
			ALPrequentialEvaluationTask budgetTask = 
					new ALPrequentialEvaluationTask();
			budgetTask.setSubtaskLevel(this.subtaskLevel + 1);
			
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
					opt.setValueViaCLIString(budget.getValueAsCLIString());
					break;
				}
			}
			
			budgetTask.prepareForUse();
			
			List<ALTaskThread> childSubtasks = budgetTask.getSubtaskThreads();
			
			// add new subtask to list
			this.subtasks.add(budgetTask);
			
			
			ALTaskThread subtaskThread = new ALTaskThread(budgetTask);
			// TODO: run task and perform evaluation
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
                "multi-budget evaluation");
		

		for(int i = 0; i < this.subtaskThreads.size(); ++i)
		{
			subtaskThreads.get(i).run();
		}
		
		return learningCurve;
	}
	
	@Override
	public List<ALTaskThread> getSubtaskThreads() {
		return this.flattenedSubtaskThreads;
	}
}
