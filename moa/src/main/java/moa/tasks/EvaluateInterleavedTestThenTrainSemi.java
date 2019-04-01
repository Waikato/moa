package moa.tasks;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.core.*;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.preview.LearningCurve;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.streams.InstanceStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * An evaluation task that relies on the mechanism of Interleaved Test Then Train,
 * applied on semi-supervised data streams
 */
public class EvaluateInterleavedTestThenTrainSemi extends SemiSupervisedMainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a classifier on a semi-supervised stream by testing only the labeled data, " +
                "then training with each example in sequence.";
    }

    private static final long serialVersionUID = 1L;

    public FlagOption onlyLabeledDataOption = new FlagOption("labeledDataOnly", 'a',
            "Learner only trained on labeled data");

    public ClassOption standarLearnerOption = new ClassOption("standardLearner", 'b',
            "A standard learner to train. This will be ignored if labeledDataOnly flag is not set.",
            MultiClassClassifier.class, "moa.classifiers.trees.HoeffdingTree");

    public ClassOption sslLearnerOption = new ClassOption("semiLeaner", 'l',
            "A semi-supervised learner to train.", SemiSupervisedLearner.class,
            "moa.classifiers.semisupervised.BaseSemiSupervisedClassifier");

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", ExampleStream.class,
            "SemiSupervisedStream");

    public IntOption randomSeedOption = new IntOption(
            "instanceRandomSeed", 'r',
            "Seed for random generation of instances.", 1);

    public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Classification performance evaluation method.",
            LearningPerformanceEvaluator.class,
            "BasicClassificationPerformanceEvaluator");

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
            "File to append intermediate csv reslts to.", null, "csv", true);

    private int numUnlabeledData = 0;

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        String streamString = this.streamOption.getValueAsCLIString();
        boolean isFullySupervised = this.onlyLabeledDataOption.isSet();

        // if we only want the standard classification on labeled data on
        Learner learner = (Learner) (isFullySupervised ?
                getPreparedClassOption(this.standarLearnerOption) : getPreparedClassOption(this.sslLearnerOption));
        String learnerString = isFullySupervised ?
                this.standarLearnerOption.getValueAsCLIString() : this.sslLearnerOption.getValueAsCLIString();

        if (learner.isRandomizable()) {
            learner.setRandomSeed(this.randomSeedOption.getValue());
            learner.resetLearning();
        }
        ExampleStream stream = (InstanceStream) getPreparedClassOption(this.streamOption);

        LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
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
        long lastEvaluateStartTime = evaluateStartTime;
        double RAMHours = 0.0;

        while (stream.hasMoreInstances()
                && ((maxInstances < 0) || (instancesProcessed < maxInstances))
                && ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {
            Example trainInst = stream.nextInstance();
            Example testInst = trainInst; //.copy();

            Instance inst = (Instance) testInst.getData();
            if (inst.classIsMissing()) this.numUnlabeledData++;

            double[] prediction = learner.getVotesForInstance(testInst);
            evaluator.addResult(testInst, prediction);
            learner.trainOnInstance(trainInst);
            instancesProcessed++;
            if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0 || !stream.hasMoreInstances()) {
                long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
                double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);
                double timeIncrement = TimingUtils.nanoTimeToSeconds(evaluateTime - lastEvaluateStartTime);
                double RAMHoursIncrement = learner.measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs
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
                                        "Unlabeled instances",
                                        this.numUnlabeledData
                                )
                        },
                        evaluator, learner));
                if (immediateResultStream != null) {
                    if (firstDump) {
                        immediateResultStream.print("Learner,stream,randomSeed,");
                        immediateResultStream.println(learningCurve.headerToString());
                        firstDump = false;
                    }
                    immediateResultStream.print(learnerString + "," + streamString + ","
                            + this.randomSeedOption.getValueAsCLIString() + ",");
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
                        : (double) instancesProcessed / (double) (instancesProcessed + estimatedRemainingInstances));
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

    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }
}
