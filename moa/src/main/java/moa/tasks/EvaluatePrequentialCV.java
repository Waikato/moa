/*
 *    EvaluatePrequential.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
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
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.Classifier;
import moa.core.*;
import moa.evaluation.*;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Task for prequential cross-validation evaluation of a classifier on a stream by testing then training with each
 * example in sequence and doing cross-validation at the same time.
 *
 * <p>Albert Bifet, Gianmarco De Francisci Morales, Jesse Read, Geoff Holmes, Bernhard Pfahringer: Efficient Online
 * Evaluation of Big Data Stream Classifiers. KDD 2015: 59-68</p>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class EvaluatePrequentialCV extends MainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a classifier on a stream by doing prequential evaluation (testing then training with each" +
                " example in sequence) and doing cross-validation.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Learner to train.", Classifier.class, "moa.classifiers.bayes.NaiveBayes");

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Classification performance evaluation method.",
            LearningPerformanceEvaluator.class,
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

    public IntOption memCheckFrequencyOption = new IntOption(
            "memCheckFrequency", 'q',
            "How many instances between memory bound checks.", 100000, 0,
            Integer.MAX_VALUE);

    public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
            "File to append intermediate csv results to.", null, "csv", true);

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 'w',
            "The number of distributed models.", 10, 1, Integer.MAX_VALUE);

    public MultiChoiceOption validationMethodologyOption = new MultiChoiceOption(
            "validationMethodology", 'a', "Validation methodology to use.", new String[]{
            "Cross-Validation", "Bootstrap-Validation", "Split-Validation"},
            new String[]{"k-fold distributed Cross Validation",
                    "k-fold distributed Bootstrap Validation",
                    "k-fold distributed Split Validation"
            }, 0);

    public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
            "Seed for random behaviour of the task.", 1);


    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

        Random random = new Random(this.randomSeedOption.getValue());
        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);

        Learner[] learners = new Learner[this.ensembleSizeOption.getValue()];
        Learner baseLearner = (Learner) getPreparedClassOption(this.learnerOption);
        baseLearner.resetLearning();

        LearningPerformanceEvaluator[] evaluators = new LearningPerformanceEvaluator[this.ensembleSizeOption.getValue()];
        LearningPerformanceEvaluator baseEvaluator = (LearningPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
        for (int i = 0; i < learners.length; i++) {
            learners[i] = (Learner) baseLearner.copy();
            learners[i].setModelContext(stream.getHeader());
            evaluators[i] = (LearningPerformanceEvaluator) baseEvaluator.copy();
        }

        LearningCurve learningCurve = new LearningCurve(
                "learning evaluation instances");
        int maxInstances = this.instanceLimitOption.getValue();
        long instancesProcessed = 0;
        int maxSeconds = this.timeLimitOption.getValue();
        int secondsElapsed = 0;
        monitor.setCurrentActivity("Evaluating learner...", -1.0);

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
        long lastEvaluateStartTime = evaluateStartTime;
        double RAMHours = 0.0;
        while (stream.hasMoreInstances()
                && ((maxInstances < 0) || (instancesProcessed < maxInstances))
                && ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {
            Example trainInst = stream.nextInstance();
            Example testInst = (Example) trainInst; //.copy();
            //testInst.setClassMissing();

            for (int i = 0; i < learners.length; i++) {
                evaluators[i].addResult(testInst, learners[i].getVotesForInstance(testInst));
            }

            for (int i = 0; i < learners.length; i++) {
                int k = 1;
                switch (this.validationMethodologyOption.getChosenIndex()) {
                    case 0: //Cross-Validation;
                        k = instancesProcessed % learners.length == i ? 0: 1; //Test all except one
                        break;
                    case 1: //Bootstrap;
                        k = MiscUtils.poisson(1, random);
                        break;
                    case 2: //Split-Validation;
                        k = instancesProcessed % learners.length == i ? 1: 0; //Test only one
                        break;
                }
                if (k > 0) {
                    Example weightedInst = (Example) trainInst.copy();
                    weightedInst.setWeight(trainInst.weight() * k);
                    learners[i].trainOnInstance(weightedInst);
                }
            }

            instancesProcessed++;
            if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0
                    || stream.hasMoreInstances() == false) {
                long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
                double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);
                double timeIncrement = TimingUtils.nanoTimeToSeconds(evaluateTime - lastEvaluateStartTime);

                for (int i = 0; i < learners.length; i++) {
                    double RAMHoursIncrement = learners[i].measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs
                    RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours
                    RAMHours += RAMHoursIncrement;
                }

                lastEvaluateStartTime = evaluateTime;
                learningCurve.insertEntry(new LearningEvaluation(
                        getEvaluationMeasurements(
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
                                        RAMHours)
                        }, evaluators)));

                if (immediateResultStream != null) {
                    if (firstDump) {
                        immediateResultStream.println(learningCurve.headerToString());
                        firstDump = false;
                    }
                    immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));
                    immediateResultStream.flush();
                }
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
                secondsElapsed = (int) TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread()
                        - evaluateStartTime);
            }
        }
        if (immediateResultStream != null) {
            immediateResultStream.close();
        }
        return learningCurve;
    }


    public Measurement[] getEvaluationMeasurements(Measurement[] modelMeasurements, LearningPerformanceEvaluator[] subEvaluators) {
        List<Measurement> measurementList = new LinkedList<Measurement>();
        if (modelMeasurements != null) {
            measurementList.addAll(Arrays.asList(modelMeasurements));
        }
        // add average of sub-model measurements
        if ((subEvaluators != null) && (subEvaluators.length > 0)) {
            List<Measurement[]> subMeasurements = new LinkedList<Measurement[]>();
            for (LearningPerformanceEvaluator subEvaluator : subEvaluators) {
                if (subEvaluator != null) {
                    subMeasurements.add(subEvaluator.getPerformanceMeasurements());
                }
            }
            Measurement[] avgMeasurements = Measurement.averageMeasurements(subMeasurements.toArray(new Measurement[subMeasurements.size()][]));
            measurementList.addAll(Arrays.asList(avgMeasurements));
        }
        return measurementList.toArray(new Measurement[measurementList.size()]);
    }
}
