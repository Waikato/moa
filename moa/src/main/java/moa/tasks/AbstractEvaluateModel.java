/*
 *    EvaluateModel.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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

import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.preview.LearningCurve;
import moa.learners.InstanceLearner;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

/**
 * Task for evaluating a static model on a stream.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public abstract class AbstractEvaluateModel<MLTask extends InstanceLearner> extends MainTask implements CapabilitiesHandler {

    @Override
    public String getPurposeString() {
        return "Evaluates a static model on a stream.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption modelOption;

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Classification performance evaluation method.",
            LearningPerformanceEvaluator.class,
            "BasicClassificationPerformanceEvaluator");

    public IntOption maxInstancesOption = new IntOption("maxInstances", 'i',
            "Maximum number of instances to test.", 100000000, 0,
            Integer.MAX_VALUE);
    
    public IntOption sampleFrequencyOption = new IntOption("sampleFrequency",
            'f',
            "How many instances between samples of the learning performance.",
            100000, 0, Integer.MAX_VALUE);

    public FileOption outputPredictionFileOption = new FileOption("outputPredictionFile", 'o',
            "File to append output predictions to.", null, "pred", true);

    @Override
    public Class<?> getTaskResultType() {
        return LearningEvaluation.class;
    }

    @Override
    public Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        MLTask model = (MLTask) getPreparedClassOption(this.modelOption);
        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
        LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
        LearningCurve learningCurve = new LearningCurve("learning evaluation instances");
        int maxInstances = this.maxInstancesOption.getValue();
        long instancesProcessed = 0;
        monitor.setCurrentActivity("Evaluating model...", -1.0);

        //File for output predictions
        File outputPredictionFile = this.outputPredictionFileOption.getFile();
        PrintStream outputPredictionResultStream = null;
        if (outputPredictionFile != null) {
            try {
                if (outputPredictionFile.exists()) {
                    outputPredictionResultStream = new PrintStream(
                            new FileOutputStream(outputPredictionFile, true), true);
                } else {
                    outputPredictionResultStream = new PrintStream(
                            new FileOutputStream(outputPredictionFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open prediction result file: " + outputPredictionFile, ex);
            }
        }
        while (stream.hasMoreInstances()
                && ((maxInstances < 0) || (instancesProcessed < maxInstances))) {
            Example testInst = (Example) stream.nextInstance();//.copy();
            int trueClass = (int) ((Instance) testInst.getData()).classValue();
            //testInst.setClassMissing();
            Prediction prediction = model.getPredictionForInstance(testInst);
            //evaluator.addClassificationAttempt(trueClass, prediction, testInst
            //		.weight());
            if (outputPredictionFile != null) {
                outputPredictionResultStream.println(Utils.maxIndex(prediction) + "," +(
                        ((Instance) testInst.getData()).classIsMissing() == true ? " ? " : trueClass));
            }
            evaluator.addResult(testInst, prediction);
            instancesProcessed++;

            if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0
                    || stream.hasMoreInstances() == false) {
	            learningCurve.insertEntry(new LearningEvaluation(
	                    new Measurement[]{
	                        new Measurement(
	                        "learning evaluation instances",
	                        instancesProcessed)
	                    },
	                    evaluator, model));
            }
            if (instancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
                if (monitor.taskShouldAbort()) {
                    return null;
                }
                long estimatedRemainingInstances = stream.estimatedRemainingInstances();
                if (maxInstances > 0) {
                    long maxRemaining = maxInstances - instancesProcessed;
                    if ((estimatedRemainingInstances < 0)
                            || (maxRemaining < estimatedRemainingInstances)) {
                        estimatedRemainingInstances = maxRemaining;
                    }
                }
                monitor.setCurrentActivityFractionComplete(estimatedRemainingInstances < 0 ? -1.0
                        : (double) instancesProcessed
                        / (double) (instancesProcessed + estimatedRemainingInstances));
                if (monitor.resultPreviewRequested()) {
                    monitor.setLatestResultPreview(learningCurve.copy());
                }
            }
        }
        if (outputPredictionResultStream != null) {
            outputPredictionResultStream.close();
        }
        return learningCurve;
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == EvaluateModel.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
