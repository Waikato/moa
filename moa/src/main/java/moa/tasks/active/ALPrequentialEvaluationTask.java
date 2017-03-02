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
import moa.evaluation.ALClassificationPerformanceEvaluator;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningEvaluation;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.tasks.TaskMonitor;

/**
 * This task performs prequential evaluation for an active learning classifier 
 * (testing, then training with each example in sequence).
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
	
	public FloatOption budgetOption = new FloatOption("budget", 't', 
			"Active learner budget.", 0.9);
	
	public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
            "Maximum number of instances to test/train on  (-1 = no limit).",
            100000000, -1, Integer.MAX_VALUE);
	
	
	@Override
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
		/*
		 * TODO Implement prequential evaluation main task
		 * 
		 * Process each sample from the given stream:
		 * 1. Test on sample
		 * 2. Train on sample
		 */
		
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
        int sampleFrequency = 100000;
        
        monitor.setCurrentActivity("Evaluating learner...", -1.0);
        while (stream.hasMoreInstances()
        	   && ((maxInstances < 0) 
        		   || (instancesProcessed < maxInstances))) 
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
        		learningCurve.insertEntry(new LearningEvaluation(
        				new Measurement[]{
        						new Measurement(
        								"learning evaluation instances",
        								instancesProcessed)
        				},
        				evaluator, learner));
        	}
        	
        	// update monitor
        	if (instancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
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
        		
        		if (monitor.resultPreviewRequested()) {
                    monitor.setLatestResultPreview(learningCurve.copy());
                }
        	}
        }
		
		return learningCurve;
	}
	
	@Override
	public List<ALTaskThread> getSubtaskThreads() {
		return new ArrayList<ALTaskThread>();
	}
}
