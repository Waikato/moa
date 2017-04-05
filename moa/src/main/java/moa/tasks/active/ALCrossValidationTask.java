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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.github.javacliparser.IntOption;
import com.github.javacliparser.Option;

import moa.core.ObjectRepository;
import moa.evaluation.PreviewCollection;
import moa.evaluation.PreviewCollectionLearningCurveWrapper;
import moa.options.ClassOption;
import moa.streams.PartitioningStream;
import moa.tasks.TaskMonitor;

/**
 * This task extensively evaluates an active learning classifier on a stream.
 * First, the given data set is partitioned into separate folds for performing
 * cross validation. On each fold, the ALMultiParamTask is performed which
 * individually evaluates the active learning classifier for each element of a
 * set of parameter values. The individual evaluation is done by prequential 
 * evaluation (testing, then training with each example in sequence).
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class ALCrossValidationTask extends ALMainTask {

	private static final long serialVersionUID = 1L;

	@Override
	 public String getPurposeString() {
	  return "Evaluates an active learning classifier on a stream by"
	    + " partitioning the data stream and evaluating"
	    + " the classifier on each subset for each element of a"
	    + " set of parameter values using prequential evaluation"
	    + " (testing, then training with each example in sequence).";
	 }
	public ClassOption multiParamTaskOption = new ClassOption(
			"multiParamTask", 't', 
			"Multi param task to be performed for each fold", 
			ALMultiParamTask.class, "moa.tasks.active.ALMultiParamTask");

	public IntOption numFoldsOption = new IntOption("numFolds", 'k', 
			"Number of cross validation folds.", 10);

	public IntOption randomSeedOption = new IntOption("randomSeed", 'r', 
			"random seed which is used for partitioning of the stream.", 0);
	
	
	private ArrayList<ALTaskThread> subtaskThreads = new ArrayList<>();
	private ArrayList<ALTaskThread> flattenedSubtaskThreads = new ArrayList<>();
	
	
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		super.prepareForUseImpl(monitor, repository);
		
		colorCoding = Color.WHITE;
		
		// get subtask objects
		ALMultiParamTask multiParamTask = (ALMultiParamTask) 
				this.multiParamTaskOption.getPreMaterializedObject();
		ALPrequentialEvaluationTask evalTask = (ALPrequentialEvaluationTask)
				multiParamTask.prequentialEvaluationTaskOption
				.getPreMaterializedObject();
		String baseStream = evalTask.streamOption.getValueAsCLIString();
		
		Random random = new Random(randomSeedOption.getValue());
		
		// setup subtask for each cross validation fold
		for (int i = 0; i < this.numFoldsOption.getValue(); i++) {
			// wrap base stream into a KFoldStream to split up data
			PartitioningStream stream = new PartitioningStream();
			stream.streamOption.setValueViaCLIString(baseStream);
			stream.partitionIndexOption.setValue(i);
			stream.numPartitionsOption.setValue(this.numFoldsOption.getValue());
			stream.randomSeedOption.setValue(random.nextInt());
			// create subtask
			ALMultiParamTask foldTask = (ALMultiParamTask) multiParamTask.copy();
			foldTask.setIsLastSubtaskOnLevel(
					this.isLastSubtaskOnLevel, i == this.numFoldsOption.getValue() - 1);
			foldTask.setFoldIdx(i);
			
			ALPrequentialEvaluationTask foldEvalTask = (ALPrequentialEvaluationTask) 
					foldTask.prequentialEvaluationTaskOption.getPreMaterializedObject();
			foldEvalTask.streamOption.setCurrentObject(stream);
			
			foldTask.prepareForUse();

			List<ALTaskThread> childSubtasks = foldTask.getSubtaskThreads();

			ALTaskThread subtaskThread = new ALTaskThread(foldTask);
			this.subtaskThreads.add(subtaskThread);

			this.flattenedSubtaskThreads.add(subtaskThread);
			this.flattenedSubtaskThreads.addAll(childSubtasks);
		}

	}

	@Override
	public Class<?> getTaskResultType() {
		return PreviewCollection.class;
	}

	@Override
	protected Object doMainTask(
			TaskMonitor monitor, ObjectRepository repository) 
	{
		// get varied parameter values
		ALMultiParamTask multiParamTask = (ALMultiParamTask) 
				this.multiParamTaskOption.getPreMaterializedObject();
		Option[] variedParamValueOptions = 
				multiParamTask.variedParamValuesOption.getList();
		int numVariedParams = variedParamValueOptions.length;
		double[] variedParamValues = new double[numVariedParams];
		for (int i = 0; i < numVariedParams; i++) {
			variedParamValues[i] = 
					Double.valueOf(variedParamValueOptions[i].getValueAsCLIString());
		}
		
		// initialize the learning curve collection
		PreviewCollection<PreviewCollection<PreviewCollectionLearningCurveWrapper>> 
			previewCollection = new PreviewCollection<>(
					"cross validation entry id", "fold id", this.getClass(),
					multiParamTask.variedParamNameOption.getValueAsCLIString(),
					variedParamValues);
		

		monitor.setCurrentActivity("Performing cross validation...", 50.0);
		
		// start subtasks
		monitor.setCurrentActivity("Performing cross validation...", -1.0);
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
				if(currentTaskThread.failed() || currentTaskThread.cancelled())
				{
					monitor.requestCancel();
				}
				// get the completion fraction
				completionSum += currentTaskThread.getCurrentActivityFracComplete();
				// get the latest preview
				@SuppressWarnings("unchecked")
				PreviewCollection<PreviewCollectionLearningCurveWrapper> 
					latestPreview = 
						(PreviewCollection<PreviewCollectionLearningCurveWrapper>)
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
	            }
			}
		}
		
		return previewCollection;
	}

	@Override
	public List<ALTaskThread> getSubtaskThreads() {
		return this.flattenedSubtaskThreads;
	}
}
