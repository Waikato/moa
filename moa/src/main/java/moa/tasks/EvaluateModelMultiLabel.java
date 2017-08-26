/*
 *    EvaluateModelMultiTarget.java
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
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.Classifier;
import moa.classifiers.MultiLabelClassifier;
import moa.classifiers.rules.multilabel.functions.MultiLabelNaiveBayes;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.MultiTargetPerformanceEvaluator;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.streams.InstanceStream;
import moa.streams.MultiTargetInstanceStream;

/**
 * Task for evaluating a static model on a stream.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class EvaluateModelMultiLabel extends MultiLabelMainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a static model on a stream.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption modelOption = new ClassOption("model", 'm',
            "Learner to evaluate.", MultiLabelClassifier.class, MultiLabelNaiveBayes.class.getName());
    
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", MultiTargetInstanceStream.class,
            "MultiTargetArffFileStream");

    public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Classification performance evaluation method.",
            MultiTargetPerformanceEvaluator.class,
            "BasicMultiLabelPerformanceEvaluator");


    public IntOption maxInstancesOption = new IntOption("maxInstances", 'i',
            "Maximum number of instances to test.", 1000000, 0,
            Integer.MAX_VALUE);

    public FileOption outputPredictionFileOption = new FileOption("outputPredictionFile", 'o',
            "File to append output predictions to.", null, "pred", true);

    public EvaluateModelMultiLabel() {
    }

    public EvaluateModelMultiLabel(Classifier model, InstanceStream stream,
                                   LearningPerformanceEvaluator evaluator, int maxInstances) {
        this.modelOption.setCurrentObject(model);
        this.streamOption.setCurrentObject(stream);
        this.evaluatorOption.setCurrentObject(evaluator);
        this.maxInstancesOption.setValue(maxInstances);
    }

    @Override
    public Class<?> getTaskResultType() {
        return LearningEvaluation.class;
    }

    @Override
    public Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        Learner model = (Learner) getPreparedClassOption(this.modelOption);
        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
        LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
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
            double trueClass = ((Instance) testInst.getData()).classValue();
            //testInst.setClassMissing();
            double[] prediction = model.getVotesForInstance(testInst);
            //evaluator.addClassificationAttempt(trueClass, prediction, testInst
            //		.weight());
            if (outputPredictionFile != null) {
                outputPredictionResultStream.println(prediction[0] + "," + trueClass);
            }
            evaluator.addResult(testInst, prediction);
            instancesProcessed++;
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
                    monitor.setLatestResultPreview(new LearningEvaluation(
                            evaluator, model));
                }
            }
        }
        if (outputPredictionResultStream != null) {
            outputPredictionResultStream.close();
        }
        return new LearningEvaluation(evaluator, model);
    }
}
