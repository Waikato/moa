/*
 *    ALPrequentialEvaluationTask.java
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

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.Option;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.active.ALClassifier;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.ALClassificationPerformanceEvaluator;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.PreviewCollectionLearningCurveWrapper;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.tasks.TaskMonitor;

/**
 * This task performs prequential evaluation for an active learning classifier 
 * (testing, then training with each example in sequence). It is mainly based
 * on the class EvaluateALPrequentialCV.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class ALPrequentialEvaluationTask extends ALMainTask {
	
	private static final long serialVersionUID = 1L;
	
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
	
	public FloatOption budgetOption = new FloatOption("budget", 'b', 
			"Active learner budget.", 0.9);
	
	public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
            "Maximum number of instances to test/train on  (-1 = no limit).",
            100000000, -1, Integer.MAX_VALUE);
	
	public IntOption timeLimitOption = new IntOption("timeLimit", 't',
            "Maximum number of seconds to test/train for (-1 = no limit).", -1,
            -1, Integer.MAX_VALUE);
	
	/**
	 * Constructor which sets the color coding to black.
	 */
	public ALPrequentialEvaluationTask() {
		this(Color.BLACK);
	}
	
	/**
	 * Constructor with which a color coding can be set.
	 * @param colorCoding the color used by the task
	 */
	public ALPrequentialEvaluationTask(Color colorCoding) {
		this.colorCoding = colorCoding;
	}
	
	@Override
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
		// get stream
		ExampleStream<Example<Instance>> stream = 
				(ExampleStream<Example<Instance>>) 
				getPreparedClassOption(this.streamOption);
		
		// initialize learner with given budget
		ALClassifier learner = 
				(ALClassifier) getPreparedClassOption(this.learnerOption);
		learner.setModelContext(stream.getHeader());
		for (Option opt : learner.getOptions().getOptionArray()) {
			// TODO: Use arbitrarily definable budget option
			if (opt.getName().equals("budget")) {
				opt.setValueViaCLIString(
						this.budgetOption.getValueAsCLIString());
			}
		}
		
		// get evaluator
        ALClassificationPerformanceEvaluator evaluator = (ALClassificationPerformanceEvaluator) 
        		getPreparedClassOption(this.prequentialEvaluatorOption);
        
        // initialize learning curve
        LearningCurve learningCurve = new LearningCurve(
        		"learning evaluation instances");
        
		// perform training and testing
        int maxInstances = this.instanceLimitOption.getValue();
        int instancesProcessed = 0;
        int maxSeconds = this.timeLimitOption.getValue();
        int secondsElapsed = 0;
        boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
        long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        long lastEvaluateStartTime = evaluateStartTime;
        double RAMHours = 0.0;
        int sampleFrequency = 100000;
        
        double budgetThreshold = budgetOption.getValue();
        
        monitor.setCurrentActivity("Evaluating learner...", -1.0);
        while (stream.hasMoreInstances()
        	   && ((maxInstances < 0) 
        		   || (instancesProcessed < maxInstances))
        	   && ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) 
        {
        	Example<Instance> trainInst = stream.nextInstance();
        	Example<Instance> testInst = trainInst;
        	
        	// predict class for instance
        	double[] prediction = learner.getVotesForInstance(testInst);
        	evaluator.addResult(testInst, prediction);
        	
        	// train on instance
        	learner.trainOnInstance(trainInst);
        	
        	// check if label was acquired
        	int labelAcquired = learner.getLastLabelAcqReport();
        	evaluator.doLabelAcqReport(trainInst, labelAcquired);
        	
        	instancesProcessed++;
        	
        	// update learning curve
        	if (instancesProcessed % sampleFrequency == 0 ||
        		!stream.hasMoreInstances())
        	{
        		long evaluateTime = 
        				TimingUtils.getNanoCPUTimeOfCurrentThread();
                double time = TimingUtils.nanoTimeToSeconds(
                		evaluateTime - evaluateStartTime);
                double timeIncrement = TimingUtils.nanoTimeToSeconds(
                		evaluateTime - lastEvaluateStartTime);
                double RAMHoursIncrement = 
                		learner.measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs
                RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours
                RAMHours += RAMHoursIncrement;
                lastEvaluateStartTime = evaluateTime;
        		
        		learningCurve.insertEntry(new LearningEvaluation(
        				new Measurement[]{
        						new Measurement(
        								"learning evaluation instances",
        								instancesProcessed),
        						new Measurement(
        	                            "evaluation time ("
        	                            + (preciseCPUTiming ? "cpu "
        	                            : "") + "seconds)",
        	                            time),
	                            new Measurement(
        	                            "model cost (RAM-Hours)",
        	                            RAMHours),
        						new Measurement(
        	                            "budget threshold",
        	                            budgetThreshold
        	                            )
        				},
        				evaluator, learner));
        	}
        	
        	// update monitor
        	if (instancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0 && learningCurve.numEntries() > 0) {
        		if (monitor.taskShouldAbort()) {
                    return null;
                }
        		
        		long estimatedRemainingInstances = 
        				stream.estimatedRemainingInstances();
        		
        		if (maxInstances > 0) {
        			long maxRemaining = maxInstances - instancesProcessed;
        			if ((estimatedRemainingInstances < 0)
        				|| (maxRemaining < estimatedRemainingInstances))
        			{
        				estimatedRemainingInstances = maxRemaining;
        			}
        		}
        		
        		// calculate completion fraction
        		double fractionComplete = (double) instancesProcessed / 
        				(instancesProcessed + estimatedRemainingInstances);
        		monitor.setCurrentActivityFractionComplete(
        				estimatedRemainingInstances < 0 ? 
        						-1.0 : fractionComplete);
        		
        		
        		// TODO currently the preview is sent after each instance
        		// 		should be changed later on
        		if (monitor.resultPreviewRequested() || isSubtask()) {
        			monitor.setLatestResultPreview(new PreviewCollectionLearningCurveWrapper((LearningCurve)learningCurve.copy(), this.getClass()));
                }
        		
        		// update time measurement
        		secondsElapsed = (int) TimingUtils.nanoTimeToSeconds(
        				TimingUtils.getNanoCPUTimeOfCurrentThread()
                        - evaluateStartTime);
        	}
        }
		
		return new PreviewCollectionLearningCurveWrapper(learningCurve, this.getClass());
	}
	
	@Override
	public List<ALTaskThread> getSubtaskThreads() {
		return new ArrayList<ALTaskThread>();
	}
}
