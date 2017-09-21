/*
 *    EvaluateMultipleClusterings.java
 *    Copyright (C) 2017 Richard Hugh Moulton
 *    @author Richard Hugh Moulton
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
package moa.tasks;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;

import moa.MOAObject;
import moa.clusterers.AbstractClusterer;
import moa.core.ObjectRepository;
import moa.evaluation.LearningCurve;
import moa.gui.BatchCmd;
import moa.options.ClassOption;
import moa.streams.clustering.ClusteringStream;

/**
 * Task for evaluating a clusterer on multiple (related) streams.
 *
 * @author Richard Hugh Moulton
 */
public class EvaluateMultipleClusterings extends AuxiliarMainTask {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4846907962483608940L;

	@Override
	public String getPurposeString()
	{
		return "Evaluates a clusterer on multiple (related) streams.";
	}

	public ClassOption learnerOption = new ClassOption("learner", 'l',
			"Clusterer to train.", AbstractClusterer.class, "clustream.Clustream");

	public ClassOption streamOption = new ClassOption("baseStream", 's',
			"Base stream to learn from.",  ClusteringStream.class,
			"FileStream");

	public IntOption numStreamsOption = new IntOption("numStreams", 'n',
			"The number of streams to iterate through (must be named according to WriteMultipleStreamsToARFF format.",
			100, 2, Integer.MAX_VALUE);
	
	public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
			"Maximum number of instances to test/train on  (-1 = no limit).",
			100000, -1, Integer.MAX_VALUE);

	public IntOption measureCollectionTypeOption = new IntOption(
			"measureCollectionType", 'm',
			"Type of measure collection", 0, 0,
			Integer.MAX_VALUE);

	
	public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
			"File to append intermediate csv reslts to.", "dumpClustering.csv", "csv", true);

	protected Task task;
	
	@Override
	public Class<?> getTaskResultType()
	{
		return Object.class;
	}

	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository)
	{
		Object result = null;
		Task taskBase = (Task) task;
		
		for(int i = 0 ; i < this.numStreamsOption.getValue() ; i++)
		{
			// Get Base task
            this.task = (Task) ((MOAObject) taskBase).copy();
            
            // Build stream
            
            // Build Output File
            
            // Build Task
            
            //Run task
            result = this.task.doTask(monitor, repository);
		}

		return result;
	}

}
