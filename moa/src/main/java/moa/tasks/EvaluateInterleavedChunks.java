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
package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import moa.classifiers.Classifier;
import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.learners.Learner;
import moa.options.ClassOption;
import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import moa.streams.ExampleStream;
import moa.streams.InstanceStream;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

public class EvaluateInterleavedChunks extends ClassificationMainTask {

	@Override
	public String getPurposeString() {
		return "Evaluates a classifier on a stream by testing then training with chunks of data in sequence.";
	}
	
	private static final long serialVersionUID = 1L;

	/**
	 * Allows to select the trained classifier.
	 */
	public ClassOption learnerOption = new ClassOption("learner", 'l',
			"Classifier to train.", Learner.class, "moa.classifiers.bayes.NaiveBayes");

	/**
	 * Allows to select the stream the classifier will learn. 
	 */
	public ClassOption streamOption = new ClassOption("stream", 's',
			"Stream to learn from.", ExampleStream.class,
			"generators.RandomTreeGenerator");

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
	public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
			"File to append intermediate csv reslts to.", null, "csv", true);

	/**
	 * Defines the task's result type.
	 */
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}

	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
		Learner learner = (Learner) getPreparedClassOption(this.learnerOption);
		ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
		LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
		learner.setModelContext(stream.getHeader());
		int maxInstances = this.instanceLimitOption.getValue();
		int chunkSize = this.chunkSizeOption.getValue();
		long instancesProcessed = 0;
		int maxSeconds = this.timeLimitOption.getValue();
		int secondsElapsed = 0;
		
		monitor.setCurrentActivity("Evaluating learner...", -1.0);
		LearningCurve learningCurve = new LearningCurve(
				"learning evaluation instances");
		File dumpFile = this.dumpFileOption.getFile();
		PrintStream immediateResultStream = null;
		if (dumpFile != null) {
			try {
				if (dumpFile.exists()) {
					immediateResultStream = new PrintStream(
							new FileOutputStream(dumpFile, true), true);
				} else {
					immediateResultStream = new PrintStream(
							new FileOutputStream(dumpFile), true);
				}
			} catch (Exception ex) {
				throw new RuntimeException(
						"Unable to open immediate result file: " + dumpFile, ex);
			}
		}
		boolean firstDump = true;
		boolean firstChunk = true;
		boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
		long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
		long sampleTestTime =0, sampleTrainTime = 0;
		double RAMHours = 0.0;
		
		while (stream.hasMoreInstances()
				&& ((maxInstances < 0) || (instancesProcessed < maxInstances))
				&& ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {
			
			Instances chunkInstances = new Instances(stream.getHeader(), chunkSize);
			
			while (stream.hasMoreInstances() && chunkInstances.numInstances() < chunkSize) {
				chunkInstances.add((Instance) stream.nextInstance().getData());
				if (chunkInstances.numInstances()
						% INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
					if (monitor.taskShouldAbort()) {
						return null;
					}
					
					long estimatedRemainingInstances = stream.estimatedRemainingInstances();
			
					if (maxInstances > 0) {
						long maxRemaining = maxInstances - instancesProcessed;
						if ((estimatedRemainingInstances < 0) || (maxRemaining < estimatedRemainingInstances)) {
							estimatedRemainingInstances = maxRemaining;
						}
					}
					
					monitor.setCurrentActivityFractionComplete((double) instancesProcessed/ (double) (instancesProcessed + estimatedRemainingInstances));
				}
			}		
			
			////Testing
			long testStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
			if(!firstChunk)
			{
				for (int i=0; i< chunkInstances.numInstances(); i++) {
					Example testInst = new InstanceExample((Instance) chunkInstances.instance(i));
					//testInst.setClassMissing();
					double[] prediction = learner.getVotesForInstance(testInst);
					evaluator.addResult(testInst, prediction);
			    }
			}
			else
			{
				firstChunk = false;
			}
			
			sampleTestTime += TimingUtils.getNanoCPUTimeOfCurrentThread() - testStartTime;
			
			////Training
			long trainStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
			
			for (int i=0; i< chunkInstances.numInstances(); i++) {
				learner.trainOnInstance(new InstanceExample(chunkInstances.instance(i)));
				instancesProcessed++;
		    }
			
			sampleTrainTime += TimingUtils.getNanoCPUTimeOfCurrentThread() - trainStartTime;
			
			////Result output
			if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0) {
				
				double RAMHoursIncrement = learner.measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs
                RAMHoursIncrement *= (TimingUtils.nanoTimeToSeconds(sampleTrainTime + sampleTestTime) / 3600.0); //Hours
                RAMHours += RAMHoursIncrement;
				
				double avgTrainTime = TimingUtils.nanoTimeToSeconds(sampleTrainTime)/((double)this.sampleFrequencyOption.getValue()/chunkInstances.numInstances());
				double avgTestTime = TimingUtils.nanoTimeToSeconds(sampleTestTime)/((double)this.sampleFrequencyOption.getValue()/chunkInstances.numInstances());
				
				sampleTestTime = 0;
				sampleTrainTime = 0;
				
				learningCurve.insertEntry(new LearningEvaluation(
					new Measurement[] {
						new Measurement("learning evaluation instances", instancesProcessed),
						new Measurement(("evaluation time ("+ (preciseCPUTiming ? "cpu " : "") + "seconds)"),TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - evaluateStartTime)),
						new Measurement("average chunk train time", avgTrainTime),
						new Measurement("average chunk train speed", chunkInstances.numInstances() / avgTrainTime),
						new Measurement("average chunk test time", avgTestTime),
						new Measurement("average chunk test speed", chunkInstances.numInstances()/ avgTestTime),
						new Measurement( "model cost (RAM-Hours)", RAMHours)}, 
					evaluator, 
					learner));
				
				if (immediateResultStream != null) {
					if (firstDump) {
						immediateResultStream.println(learningCurve
								.headerToString());
						firstDump = false;
					}
					immediateResultStream.println(learningCurve
							.entryToString(learningCurve.numEntries() - 1));
					immediateResultStream.flush();
				}
			}
			
			////Memory testing
			if (instancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
				if (monitor.taskShouldAbort()) {
					return null;
				}
				long estimatedRemainingInstances = stream
						.estimatedRemainingInstances();
				if (maxInstances > 0) {
					long maxRemaining = maxInstances - instancesProcessed;
					if ((estimatedRemainingInstances < 0)
							|| (maxRemaining < estimatedRemainingInstances)) {
						estimatedRemainingInstances = maxRemaining;
					}
				}
				monitor
						.setCurrentActivityFractionComplete(estimatedRemainingInstances < 0 ? -1.0
								: (double) instancesProcessed
										/ (double) (instancesProcessed + estimatedRemainingInstances));
				if (monitor.resultPreviewRequested()) {
					monitor.setLatestResultPreview(learningCurve.copy());
				}
				secondsElapsed = (int) TimingUtils
						.nanoTimeToSeconds(TimingUtils
								.getNanoCPUTimeOfCurrentThread()
								- evaluateStartTime);
			}
		}
		if (immediateResultStream != null) {
			immediateResultStream.close();
		}
		return learningCurve;
	}

}
