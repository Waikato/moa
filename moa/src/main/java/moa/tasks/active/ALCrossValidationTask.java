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

import java.util.ArrayList;
import java.util.List;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.ListOption;
import com.github.javacliparser.Option;

import moa.classifiers.active.ALClassifier;
import moa.core.ObjectRepository;
import moa.evaluation.ALEvaluator;
import moa.evaluation.LearningCurve;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.streams.KFoldStream;
import moa.tasks.TaskMonitor;

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
                " prequential evaluation (testing, then training with each" +
        		" example in  sequence).";
    }
	
	/* options actually used in ALPrequentialEvaluationTask */
    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Learner to train.", ALClassifier.class, "moa.classifiers.active.ALZliobaite2011");
	
	public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", ExampleStream.class,
            "generators.RandomTreeGenerator");
	
	public ClassOption prequentialEvaluatorOption = new ClassOption(
			"prequentialEvaluator", 'e',
            "Prequential classification performance evaluation method.",
            ALEvaluator.class,
            "ALBasicClassificationPerformanceEvaluator");
	
	/* options actually used in ALMultiBudgetTask */
	public ListOption budgetsOption = new ListOption("budgets", 'b',
			"List of budgets to train classifiers for.",
			new FloatOption("budget", 't', "Active learner budget.", 0.9), 
			new Option[0], ',');
	
	public ClassOption multiBudgetEvaluatorOption = new ClassOption(
			"multiBudgetEvaluator", 'm',
            "Multi-budget classification performance evaluation method.",
            ALEvaluator.class,
            "ALBasicClassificationPerformanceEvaluator");
	
	/* options used in in this class */
	public IntOption numFoldsOption = new IntOption("numFolds", 'k',
            "Number of cross validation folds.", 10);
	
	public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
            "Seed for random behaviour of the task.", 1);
	
	public ClassOption crossValEvaluatorOption = new ClassOption(
			"corssValidationEvaluator", 'c',
            "Cross validation evaluation method.",
            ALEvaluator.class,
            "ALBasicClassificationPerformanceEvaluator");
	
	/*
	 * Possible extensions/further options:
	 * - Ensembles of learners (ensemble size)
	 * - Instance limit
	 * - Time limit
	 * - Sample frequency
	 * - Memory check frequency
	 * - Dump file
	 */
	
	
	private ArrayList<ALMultiBudgetTask> subtasks = new ArrayList<>();
	private ArrayList<ALTaskThread> subtaskThreads = new ArrayList<>();
	private ArrayList<ALTaskThread> flattenedSubtaskThreads = new ArrayList<>();
	
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		// TODO Auto-generated method stub
		super.prepareForUseImpl(monitor, repository);

		// setup subtask for each cross validation fold
		for (int i = 0; i < this.numFoldsOption.getValue(); i++) {
			
			// wrap base stream into a KFoldStream to split up data
			KFoldStream stream = new KFoldStream();
			
			for (Option opt : stream.getOptions().getOptionArray()) {
				switch (opt.getName()) {
				case "stream": 
					opt.setValueViaCLIString(
							this.streamOption.getValueAsCLIString());
					break;
				case "foldIndex":
					opt.setValueViaCLIString(String.valueOf(i));
					break;
				case "numFolds":
					opt.setValueViaCLIString(
							this.numFoldsOption.getValueAsCLIString());
					break;
				}
			}
			
			// create subtask
			ALMultiBudgetTask foldTask = new ALMultiBudgetTask();
			foldTask.setIsLastSubtaskOnLevel(
					this.isLastSubtaskOnLevel, 
					i == this.numFoldsOption.getValue() - 1);
			
			for (Option opt : foldTask.getOptions().getOptionArray()) {
				switch (opt.getName()) {
				case "learner":
					opt.setValueViaCLIString(
							this.learnerOption.getValueAsCLIString());
					break;
				case "stream": 
					opt.setValueViaCLIString(
							ClassOption.objectToCLIString(
									stream, ExampleStream.class));
					break;
				case "prequential evaluator":
					opt.setValueViaCLIString(
							this.prequentialEvaluatorOption
							.getValueAsCLIString());
					break;
				case "budgets":
					opt.setValueViaCLIString(
							this.budgetsOption.getValueAsCLIString());
					break;
				case "multi-budget evaluator":
					opt.setValueViaCLIString(
							this.multiBudgetEvaluatorOption
							.getValueAsCLIString());
					break;
				}
			}
			
			foldTask.prepareForUse();
			
			List<ALTaskThread> childSubtasks = foldTask.getSubtaskThreads();
			
			// add new subtask to list
			this.subtasks.add(foldTask);
			
			
			ALTaskThread subtaskThread = new ALTaskThread(foldTask);
			// TODO: run task and perform evaluation
			this.subtaskThreads.add(subtaskThread);

			this.flattenedSubtaskThreads.add(subtaskThread);
			this.flattenedSubtaskThreads.addAll(childSubtasks);
		}
		
	}
	
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
                "cross validation evaluation");
		
		for(int i = 0; i < this.subtaskThreads.size(); ++i)
		{
			subtaskThreads.get(i).run();
		}
		//monitor.setCurrentActivity("Performing cross validation...", -1.0);
		
		
		return learningCurve;
	}
	
	@Override
	public List<ALTaskThread> getSubtaskThreads() {
		return this.flattenedSubtaskThreads;
	}
}
