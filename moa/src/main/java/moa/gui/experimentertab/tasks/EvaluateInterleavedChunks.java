/*
 *    EvaluateInterleavedChunks.java
 *    Copyright (C) 2010 Poznan University of Technology, Poznan, Poland
 *    @author Dariusz Brzezinski (dariusz.brzezinski@cs.put.poznan.pl)
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
package moa.gui.experimentertab.tasks;

import moa.core.ObjectRepository;
import moa.tasks.*;

import moa.evaluation.preview.LearningCurve;
import moa.options.ClassOption;
import com.github.javacliparser.IntOption;
import moa.evaluation.LearningPerformanceEvaluator;


public class EvaluateInterleavedChunks extends ExperimenterTask {

	@Override
	public String getPurposeString() {
		return "Evaluates a classifier on a stream by testing then training with chunks of data in sequence.";
	}
	
	private static final long serialVersionUID = 1L;

	/**
	 * Allows to select the classifier performance evaluation method.
	 */
	public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Learning performance evaluation method.",
            LearningPerformanceEvaluator.class,
			"BasicClassificationPerformanceEvaluator");

	/**
	 * Allows to define the maximum number of instances to test/train on  (-1 = no limit).
	 */
	public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
			"Maximum number of instances to test/train on  (-1 = no limit).",
			100000000, -1, Integer.MAX_VALUE);

	/**
	 * Allow to define the training/testing chunk size.
	 */
	public IntOption chunkSizeOption = new IntOption("chunkSize", 'c',
			"Number of instances in a data chunk.",
			1000, 1, Integer.MAX_VALUE);
	
	/**
	 * Allows to define the maximum number of seconds to test/train for (-1 = no limit).
	 */
	public IntOption timeLimitOption = new IntOption("timeLimit", 't',
			"Maximum number of seconds to test/train for (-1 = no limit).", -1,
			-1, Integer.MAX_VALUE);

	/**
	 * Defines how often classifier parameters will be calculated.
	 */
	public IntOption sampleFrequencyOption = new IntOption("sampleFrequency",
			'f',
			"How many instances between samples of the learning performance.",
			100000, 0, Integer.MAX_VALUE);

	/**
	 * Allows to define the memory limit for the created model.
	 */
	public IntOption maxMemoryOption = new IntOption("maxMemory", 'b',
			"Maximum size of model (in bytes). -1 = no limit.", -1, -1,
			Integer.MAX_VALUE);

	/**
	 * Allows to define the frequency of memory checks.
	 */
	public IntOption memCheckFrequencyOption = new IntOption(
			"memCheckFrequency", 'q',
			"How many instances between memory bound checks.", 100000, 0,
			Integer.MAX_VALUE);

	/**
	 * Allows to define the output file name and location.
	 */
//	public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
//			"File to append intermediate csv reslts to.", null, "csv", true);

	/**
	 * Defines the task's result type.
	 */
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}

    @Override
    protected Object doTaskImpl(TaskMonitor monitor, ObjectRepository repository) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
	
}
