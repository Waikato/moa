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
package moa.tasks.meta;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.ListOption;
import com.github.javacliparser.Option;
import com.github.javacliparser.Options;

import moa.classifiers.active.ALClassifier;
import moa.core.ObjectRepository;
import moa.evaluation.preview.LearningCurve;
import moa.evaluation.preview.PreviewCollection;
import moa.evaluation.preview.PreviewCollectionLearningCurveWrapper;
import moa.gui.colorGenerator.HSVColorGenerator;
import moa.options.ClassOption;
import moa.options.ClassOptionWithListenerOption;
import moa.options.DependentOptionsUpdater;
import moa.options.EditableMultiChoiceOption;
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
	
	public ClassOptionWithListenerOption prequentialEvaluationTaskOption = 
			new ClassOptionWithListenerOption(
				"prequentialEvaluationTask", 'e', 
				"Prequential evaluation task to be performed for each " + 
				"parameter value.", ALPrequentialEvaluationTask.class, 
				"moa.tasks.meta.ALPrequentialEvaluationTask");
	
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
					new FloatOption("", ' ', "", 0.1),
					new FloatOption("", ' ', "", 0.2),
					new FloatOption("", ' ', "", 0.5)
			}, ',');
	
	
	private ArrayList<ALPrequentialEvaluationTask> subtasks = new ArrayList<>();
	private ArrayList<ALTaskThread> subtaskThreads = new ArrayList<>();
	private ArrayList<ALTaskThread> flattenedSubtaskThreads = new ArrayList<>();
	
	private Color[] subTaskColorCoding;
	private int partitionIdx = -1;
	
	/**
	 * Default constructor which sets up the refresh mechanism between the 
	 * learner and the variedParamName option.
	 */
	public ALMultiParamTask() {
		super();
		
		// enable refreshing the variedParamNameOption depending on the
		// learnerOption
		new DependentOptionsUpdater(
				this.prequentialEvaluationTaskOption, 
				this.variedParamNameOption);
	}
	
	/**
	 * Constructor that sets the color coding for the subtasks additionally
	 * to the default constructor.
	 * 
	 * @param subTaskColorCoding
	 */
	public ALMultiParamTask(Color[] subTaskColorCoding) {
		this();
		this.subTaskColorCoding = subTaskColorCoding;
	}
	
	public void setPartitionIdx(int partitionIdx) {
		this.partitionIdx = partitionIdx;
		this.setNameSuffix("partition " + partitionIdx);
	}
	
	@Override
	public Options getOptions() {
		Options options = super.getOptions();
		
		// make sure that all dependent options are up to date
		this.prequentialEvaluationTaskOption.getChangeListener()
			.stateChanged(null);
		
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
		String variedParamName = 
				this.variedParamNameOption.getValueAsCLIString();
		
		// get prequential evaluation task
		ALPrequentialEvaluationTask evalTask = (ALPrequentialEvaluationTask)
				this.prequentialEvaluationTaskOption.getPreMaterializedObject();
		
		// get learner
		ALClassifier learner = (ALClassifier) 
				evalTask.learnerOption.getPreMaterializedObject();
		
		// get the learner's varied parameter option
		Option learnerVariedParamOption = 
				DependentOptionsUpdater.getVariedOption(learner, variedParamName);
		
		// get values for the varied parameter
		Option[] paramValues;
		if (learnerVariedParamOption != null) {
			paramValues = this.variedParamValuesOption.getList();
		}
		else {
			paramValues = new Option[]{null};
		}
		
		// append partition index to task result file if necessary
		if (this.partitionIdx >= 0) {
			File ownTaskResultFile = this.outputFileOption.getFile();
			
			if (ownTaskResultFile != null) {
				this.outputFileOption.setValue(
						this.insertFileNameExtension(
								ownTaskResultFile.getAbsolutePath(), 
								"_p" + this.partitionIdx));
			}
		}
		
		// get base dump file name of subtasks
		File baseDumpFile = evalTask.dumpFileOption.getFile();
		String baseDumpFileName = baseDumpFile != null ? 
				baseDumpFile.getAbsolutePath() : null;
		
		// get task result name of subtask
		File baseTaskResultFile = evalTask.outputFileOption.getFile();
		String baseTaskResultName = baseTaskResultFile != null ?
				baseTaskResultFile.getAbsolutePath() : null;
		
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
					(ALPrequentialEvaluationTask) evalTask.copy();
			paramValueTask.setColorCoding(subTaskColorCoding[i]);
			paramValueTask.setIsLastSubtaskOnLevel(
					this.isLastSubtaskOnLevel, i == paramValues.length - 1);
			
			// set the learner's varied parameter option
			if (learnerVariedParamOption != null) {
				String paramValue = paramValues[i].getValueAsCLIString().trim();
				paramValueTask.setNameSuffix(paramValue);
				
				// parse to integer if necessary
				if (learnerVariedParamOption instanceof IntOption) {
					int intParamValue = (int) Double.parseDouble(paramValue);
					paramValue = Integer.toString(intParamValue);
				}
				
				learnerVariedParamOption.setValueViaCLIString(paramValue);
				
				String outputFileExtension = "";
				if (this.partitionIdx >= 0) {
					// append partition index
					outputFileExtension += "_p" + this.partitionIdx;
				}
				
				if (paramValues.length > 1) {
					// append varied parameter value
					outputFileExtension += "_v" + paramValue;
				}
				
				// adapt the dump file name
				if (baseDumpFileName != null) {
					paramValueTask.dumpFileOption.setValue(
							this.insertFileNameExtension(
									baseDumpFileName, outputFileExtension));
				}
				
				// adapt the task result file name
				if (baseTaskResultName != null) {
					paramValueTask.outputFileOption.setValue(
							this.insertFileNameExtension(
									baseTaskResultName, outputFileExtension));
				}
			}
			
			paramValueTask.learnerOption.setValueViaCLIString(
					ClassOption.objectToCLIString(learner, ALClassifier.class));
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
		// get varied parameter values
		Option[] variedParamValueOptions = this.variedParamValuesOption.getList();
		double[] variedParamValues = new double[variedParamValueOptions.length];
		for (int i = 0; i < variedParamValueOptions.length; i++) {
			variedParamValues[i] = 
					Double.valueOf(variedParamValueOptions[i].getValueAsCLIString());
		}
		
		// initialize the learning curve collection
		PreviewCollection<PreviewCollectionLearningCurveWrapper> 
			previewCollection = new PreviewCollection<>(
					"multi param entry id", "learner id", this.getClass(),
					this.variedParamNameOption.getValueAsCLIString(),
					variedParamValues);
		
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
				
				// request cancel if subtask failed or was cancelled
				if(currentTaskThread.isFailed() || currentTaskThread.isCancelled())
				{
					monitor.requestCancel();
				}
				
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
				else if(!currentTaskThread.isComplete())
				{
					// skip for loop until all threads before were at least added once
					break;
				}
				else {
					// set final result as latest preview
					PreviewCollectionLearningCurveWrapper finalPreview = 
							(PreviewCollectionLearningCurveWrapper) 
							currentTaskThread.getFinalResult();
					previewCollection.setPreview(i, finalPreview);
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
	
	private String insertFileNameExtension(
			String baseName, String fileNameExtension) 
	{
		int fileExtIndex = baseName.lastIndexOf('.');
		
		// remove file extension
		String extendedName = baseName.substring(0, fileExtIndex);
		
		// append partition and parameter extension
		extendedName += fileNameExtension;
		
		// append file extension
		extendedName += baseName.substring(fileExtIndex); 
		
		return extendedName;
	}
}
