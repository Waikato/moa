/*
 *    EvaluatePrequential.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import moa.classifiers.Classifier;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.ClassificationPerformanceEvaluator;
import moa.evaluation.WindowClassificationPerformanceEvaluator;
import moa.evaluation.EWMAClassificationPerformanceEvaluator;
import moa.evaluation.FadingFactorClassificationPerformanceEvaluator;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningEvaluation;
import moa.options.ClassOption;
import moa.options.FileOption;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.streams.InstanceStream;
import weka.core.Instance;

public class EvaluatePrequential extends MainTask {

	@Override
	public String getPurposeString() {
		return "Evaluates a classifier on a stream by testing then training with each example in sequence.";
	}
	
	private static final long serialVersionUID = 1L;

	public ClassOption learnerOption = new ClassOption("learner", 'l',
			"Classifier to train.", Classifier.class, "NaiveBayes");

	public ClassOption streamOption = new ClassOption("stream", 's',
			"Stream to learn from.", InstanceStream.class,
			"generators.RandomTreeGenerator");

	public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
			"Classification performance evaluation method.",
			ClassificationPerformanceEvaluator.class,
			"WindowClassificationPerformanceEvaluator");

	public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
			"Maximum number of instances to test/train on  (-1 = no limit).",
			100000000, -1, Integer.MAX_VALUE);

	public IntOption timeLimitOption = new IntOption("timeLimit", 't',
			"Maximum number of seconds to test/train for (-1 = no limit).", -1,
			-1, Integer.MAX_VALUE);

	public IntOption sampleFrequencyOption = new IntOption("sampleFrequency",
			'f',
			"How many instances between samples of the learning performance.",
			100000, 0, Integer.MAX_VALUE);

	public IntOption maxMemoryOption = new IntOption("maxMemory", 'b',
			"Maximum size of model (in bytes). -1 = no limit.", -1, -1,
			Integer.MAX_VALUE);

	public IntOption memCheckFrequencyOption = new IntOption(
			"memCheckFrequency", 'q',
			"How many instances between memory bound checks.", 100000, 0,
			Integer.MAX_VALUE);

	public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
			"File to append intermediate csv results to.", null, "csv", true);


        //New for prequential method
	public IntOption widthOption = new IntOption("width",
			'w', "Size of Window", 1000);
			
	public FloatOption alphaOption = new FloatOption("alpha",
			'a', "Fading factor or exponential smoothing factor", .01);
	//End New for prequential methods

	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}

	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
		Classifier learner = (Classifier) getPreparedClassOption(this.learnerOption);
		InstanceStream stream = (InstanceStream) getPreparedClassOption(this.streamOption);
		ClassificationPerformanceEvaluator evaluator = (ClassificationPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
		//New for prequential methods
		if (evaluator instanceof WindowClassificationPerformanceEvaluator) {
			((WindowClassificationPerformanceEvaluator) evaluator).setWindowWidth(widthOption.getValue());
		}
		if (evaluator instanceof EWMAClassificationPerformanceEvaluator) {
			((EWMAClassificationPerformanceEvaluator) evaluator).setalpha(alphaOption.getValue());
		}
		if (evaluator instanceof FadingFactorClassificationPerformanceEvaluator) {
			((FadingFactorClassificationPerformanceEvaluator) evaluator).setalpha(alphaOption.getValue());
		}
		//End New for prequential methods
		
		learner.setModelContext(stream.getHeader());
		int maxInstances = this.instanceLimitOption.getValue();
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
		boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
		long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
		while (stream.hasMoreInstances()
				&& ((maxInstances < 0) || (instancesProcessed < maxInstances))
				&& ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {
			Instance trainInst = stream.nextInstance();
			Instance testInst = (Instance) trainInst.copy();
			int trueClass = (int) trainInst.classValue();
			testInst.setClassMissing();
			double[] prediction = learner.getVotesForInstance(testInst);
			evaluator.addClassificationAttempt(trueClass, prediction, testInst
					.weight());
			learner.trainOnInstance(trainInst);
			instancesProcessed++;
			if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0) {
				learningCurve
						.insertEntry(new LearningEvaluation(
								new Measurement[] {
										new Measurement(
												"learning evaluation instances",
												instancesProcessed),
										new Measurement(
												("evaluation time ("
														+ (preciseCPUTiming ? "cpu "
																: "") + "seconds)"),
												TimingUtils
														.nanoTimeToSeconds(TimingUtils
																.getNanoCPUTimeOfCurrentThread()
																- evaluateStartTime)) },
								evaluator, learner));
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
