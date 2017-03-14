/*
 *    ALMultiParamTask.java
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

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.ListOption;
import com.github.javacliparser.Option;
import com.github.javacliparser.Options;

import moa.classifiers.active.ALClassifier;
import moa.core.ObjectRepository;
import moa.evaluation.ALClassificationPerformanceEvaluator;
import moa.evaluation.LearningCurve;
import moa.evaluation.PreviewCollection;
import moa.evaluation.PreviewCollectionLearningCurveWrapper;
import moa.gui.colorGenerator.HSVColorGenerator;
import moa.options.ClassOption;
import moa.options.ClassOptionWithListenerOption;
import moa.options.EditableMultiChoiceOption;
import moa.streams.ExampleStream;
import moa.tasks.TaskMonitor;

/**
 * This task individually evaluates an active learning classifier for each 
 * element of a set of parameter values. The individual evaluation is done by 
 * prequential evaluation (testing, then training with each example in 
 * sequence).
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class ALMultiParamTask extends ALMainTask {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public String getPurposeString() {
		return "Individually evaluates an active learning classifier for each"
				+ " element of a set of parameter values using prequential"
				+ " evaluation (testing, then training with each example in"
				+ " sequence).";
	}
	
	/* options actually used in ALPrequentialEvaluationTask */
	public ClassOptionWithListenerOption learnerOption = 
			new ClassOptionWithListenerOption(
				"learner", 'l', "Learner to train.", ALClassifier.class, 
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
	
	public IntOption timeLimitOption = new IntOption("timeLimit", 't',
            "Maximum number of seconds to test/train for (-1 = no limit).", -1,
            -1, Integer.MAX_VALUE);
	
	/* options used in in this class */
	public EditableMultiChoiceOption variedParamNameOption = 
			new EditableMultiChoiceOption(
					"variedParamName", 'p', 
					"Name of the parameter to be varied.",
					new String[]{"budget"}, 
					new String[]{"default varied parameter name"}, 
					0);
	
	public ListOption variedParamValuesOption = new ListOption(
			"variedParamValues", 'v',
			"List of parameter values to train classifiers for.",
			new FloatOption("value", ' ', "Parameter value.", 0.0), 
			new FloatOption[]{
					new FloatOption("", ' ', "", 0.5),
					new FloatOption("", ' ', "", 0.9)
			}, ',');
	
	public ClassOption multiParamEvaluatorOption = new ClassOption(
			"multiParamEvaluator", 'm',
            "Multi-param classification performance evaluation method.",
            ALClassificationPerformanceEvaluator.class,
            "ALBasicClassificationPerformanceEvaluator");
	
	
	private ArrayList<ALPrequentialEvaluationTask> subtasks = new ArrayList<>();
	private ArrayList<ALTaskThread> subtaskThreads = new ArrayList<>();
	private ArrayList<ALTaskThread> flattenedSubtaskThreads = new ArrayList<>();
	
	private Color[] subTaskColorCoding;
	
	public ALMultiParamTask() {
		super();
		
		// initialize subtasks
		subtasks = new ArrayList<>();
		subtaskThreads = new ArrayList<>();
		flattenedSubtaskThreads = new ArrayList<>();
		subTaskColorCoding = null;
		
		// reset last learner option
		ALMultiParamTask.lastLearnerOption = null;
		
		// Enable refreshing the variedParamNameOption depending on the
		// learnerOption
		this.learnerOption.setListener(new RefreshParamsChangeListener(
				this.learnerOption, this.variedParamNameOption));
	}
	
	public ALMultiParamTask(Color[] subTaskColorCoding) {
		this();
		this.subTaskColorCoding = subTaskColorCoding;
	}
	
	@Override
	public Options getOptions() {
		Options options = super.getOptions();
		
		// Get the initial values for the variedParamNameOption
		ALMultiParamTask.refreshVariedParamNameOption(
				this.learnerOption, this.variedParamNameOption);
		
		return options;
	}
	
	@Override
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}
	
	
	@Override
	protected void prepareForUseImpl(
			TaskMonitor monitor, ObjectRepository repository) 
	{
		super.prepareForUseImpl(monitor, repository);
		
		// get varied parameter name
		final String variedParamName = 
				this.variedParamNameOption.getValueAsCLIString();
		
		// get learner
		ALClassifier learner = 
				(ALClassifier) getPreparedClassOption(this.learnerOption);
		
		// get the learner's varied parameter option
		Option learnerVariedParamOption = null;
		for (Option opt : learner.getOptions().getOptionArray()) {
			if (opt.getName().equals(variedParamName)) {
				learnerVariedParamOption = opt;
				break;
			}
		}
		
		// get values for the varied parameter
		Option[] paramValues;
		if (learnerVariedParamOption != null) {
			paramValues = this.variedParamValuesOption.getList();
		}
		else {
			paramValues = new Option[]{null};
		}
		
		// create color coding
		colorCoding = Color.WHITE;
		
		if(subTaskColorCoding == null)
		{
			subTaskColorCoding = 
					new HSVColorGenerator().generateColors(paramValues.length);
		}
		
		// setup task for each parameter value
		for (int i = 0; i < paramValues.length; i++) {
			
			// create subtask
			ALPrequentialEvaluationTask paramValueTask = 
					new ALPrequentialEvaluationTask(subTaskColorCoding[i]);
			paramValueTask.setIsLastSubtaskOnLevel(
					this.isLastSubtaskOnLevel, i == paramValues.length - 1);
			
			// set the learner's varied parameter option
			if (learnerVariedParamOption != null) {
				learnerVariedParamOption.setValueViaCLIString(
						paramValues[i].getValueAsCLIString());
			}
			
			for (Option opt : paramValueTask.getOptions().getOptionArray()) {
				switch (opt.getName()) {
				case "learner":
					opt.setValueViaCLIString(ClassOption.objectToCLIString(
							learner, ALClassifier.class));
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
				case "instanceLimit":
					opt.setValueViaCLIString(
							this.instanceLimitOption.getValueAsCLIString());
					break;
				case "timeLimit":
					opt.setValueViaCLIString(
							this.timeLimitOption.getValueAsCLIString());
					break;
				}
			}
			
			paramValueTask.prepareForUse();
			
			List<ALTaskThread> childSubtasks = paramValueTask.getSubtaskThreads();
			
			// add new subtask and its thread to lists
			this.subtasks.add(paramValueTask);
			
			ALTaskThread subtaskThread = new ALTaskThread(paramValueTask);
			this.subtaskThreads.add(subtaskThread);

			this.flattenedSubtaskThreads.add(subtaskThread);
			this.flattenedSubtaskThreads.addAll(childSubtasks);
		}
		
		// reset learner varied parameter option
		if (learnerVariedParamOption != null) {
			learnerVariedParamOption.resetToDefault();
		}
	}
	
	@Override
	protected Object doMainTask(
			TaskMonitor monitor, ObjectRepository repository) 
	{
		// setup learning curve
		PreviewCollection<PreviewCollectionLearningCurveWrapper> 
			previewCollection = new PreviewCollection<>(
					"multi param entry id", "learner id", this.getClass());		
		// start subtasks
		monitor.setCurrentActivity(
				"Evaluating learners for parameter values...", -1.0);
		for(int i = 0; i < this.subtaskThreads.size(); ++i)
		{
			subtaskThreads.get(i).start();
		}

		// get the number of subtask threads
		int numSubtaskThreads = subtaskThreads.size();
		// check the previews of subtaskthreads
		boolean allThreadsCompleted = false;
		// iterate while there are threads active
		while(!allThreadsCompleted)
		{
			allThreadsCompleted = true;
			int oldNumEntries = previewCollection.numEntries();
			double completionSum = 0;
			// iterate over all threads
			for(int i = 0; i < numSubtaskThreads; ++i)
			{
				ALTaskThread currentTaskThread = subtaskThreads.get(i);
				// check if the thread is completed
				allThreadsCompleted &= currentTaskThread.isComplete();
				// get the completion fraction
				completionSum += currentTaskThread.getCurrentActivityFracComplete();
				// get the latest preview
				PreviewCollectionLearningCurveWrapper latestPreview = 
						(PreviewCollectionLearningCurveWrapper) 
						currentTaskThread.getLatestResultPreview();
				// ignore the preview if it is null
				if(latestPreview != null && latestPreview.numEntries() > 0)
				{	
					// update/add the learning curve to the learning curve collection
					previewCollection.setPreview(i, latestPreview);
				}
				else
				{
					// skip for loop until all threads before were at least added once
					break;
				}
			}
			double completionFraction = completionSum / numSubtaskThreads;
			
			monitor.setCurrentActivityFractionComplete(completionFraction);
			
			// check if the task should abort or paused
    		if (monitor.taskShouldAbort()) {
                return null;
            }
			
			// check if the preview has actually changed
			if(oldNumEntries < previewCollection.numEntries())
			{
				// check if a preview is requested
	    		if (monitor.resultPreviewRequested() || isSubtask()) {
	    			// send the latest preview to the monitor
	                monitor.setLatestResultPreview(previewCollection.copy());

	        		monitor.setCurrentActivityFractionComplete(-1.0);
	            }
			}
		}
		
		return previewCollection;
	}
	
	@Override
	public List<ALTaskThread> getSubtaskThreads() {
		return this.flattenedSubtaskThreads;
	}
	
	
	
	/* Static classes and methods */
	
	protected static String lastLearnerOption;
	
	protected static class RefreshParamsChangeListener 
		implements ChangeListener, Serializable 
	{
		
		private static final long serialVersionUID = 1L;
		
		private ClassOption learnerOption;
		private EditableMultiChoiceOption variedParamNameOption;
		
		public RefreshParamsChangeListener(
				ClassOption learnerOption, 
				EditableMultiChoiceOption variedParamNameOption) 
		{
			this.learnerOption = learnerOption;
			this.variedParamNameOption = variedParamNameOption;
		}
		
		@Override
		public void stateChanged(ChangeEvent e) {
			ALMultiParamTask.refreshVariedParamNameOption(
					this.learnerOption, this.variedParamNameOption);
		}
	}
	
	protected static void refreshVariedParamNameOption(
			ClassOption learnerOption, 
			EditableMultiChoiceOption variedParamNameOption)
	{
		ALClassifier learner = 
				(ALClassifier) learnerOption.getPreMaterializedObject();
		String currentLearner = learner.getClass().getSimpleName();
		
		// check if an update is actually needed
		if (lastLearnerOption == null || 
			!lastLearnerOption.equals(currentLearner)) 
		{
			lastLearnerOption = currentLearner;
			
			Option[] options = learner.getOptions().getOptionArray();
			
			// filter for Int and Float Options
			options = Arrays.stream(options)
					.filter(x -> x instanceof IntOption || 
							x instanceof FloatOption)
					.toArray(Option[]::new);
			
			String[] optionNames = new String[options.length];
			String[] optionDescriptions = new String[options.length];
			int defaultIndex = -1;
			
			for (int i = 0; i < options.length; i++) {
				optionNames[i] = options[i].getName();
				optionDescriptions[i] = options[i].getPurpose();
				
				if (optionNames[i].equals("budget") || 
					(optionNames[i].contains("budget") && defaultIndex < 0)) 
				{
					defaultIndex = i;
				}
			}
			
			variedParamNameOption.setOptions(optionNames, optionDescriptions, 
					defaultIndex >= 0 ? defaultIndex : 0);
		}
	}
}
