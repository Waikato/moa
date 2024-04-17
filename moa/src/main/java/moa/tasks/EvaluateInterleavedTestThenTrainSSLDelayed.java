package moa.tasks;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * An evaluation task that relies on the mechanism of Interleaved Test Then
 * Train,
 * applied on semi-supervised data streams
 */
public class EvaluateInterleavedTestThenTrainSSLDelayed extends SemiSupervisedMainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a classifier on a semi-supervised stream by testing only the labeled data, " +
                "then training with each example in sequence.";
    }

    private static final long serialVersionUID = 1L;

    public IntOption randomSeedOption = new IntOption(
            "instanceRandomSeed", 'r',
            "Seed for random generation of instances.", 1);

    public FlagOption onlyLabeledDataOption = new FlagOption("labeledDataOnly", 'a',
            "Learner only trained on labeled data");

    public ClassOption standardLearnerOption = new ClassOption("standardLearner", 'b',
            "A standard learner to train. This will be ignored if labeledDataOnly flag is not set.",
            MultiClassClassifier.class, "moa.classifiers.trees.HoeffdingTree");

    public ClassOption sslLearnerOption = new ClassOption("sslLearner", 'l',
            "A semi-supervised learner to train.", SemiSupervisedLearner.class,
            "moa.classifiers.semisupervised.ClusterAndLabelClassifier");

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", ExampleStream.class,
            "moa.streams.ArffFileStream");

    public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Classification performance evaluation method.",
            LearningPerformanceEvaluator.class,
            "BasicClassificationPerformanceEvaluator");

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** Option: Probability of instance being unlabeled */
    public FloatOption labelProbabilityOption = new FloatOption("labelProbability", 'j',
            "The ratio of labeled data",
            0.01);

    public IntOption delayLengthOption = new IntOption("delay", 'k',
            "Number of instances before test instance is used for training. -1 = no delayed labeling.",
            -1, -1, Integer.MAX_VALUE);

    public IntOption initialWindowSizeOption = new IntOption("initialTrainingWindow", 'p',
            "Number of instances used for training in the beginning of the stream (-1 = no initialWindow).",
            -1, -1, Integer.MAX_VALUE);

    public FlagOption debugPseudoLabelsOption = new FlagOption("debugPseudoLabels", 'w',
            "Learner also receives the labeled data, but it is not used for training (just for statistics)");

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    public FileOption outputPredictionFileOption = new FileOption("outputPredictionFile", 'o',
            "File to append output predictions to.", null, "pred", true);

    public FileOption debugOutputUnlabeledClassInformation = new FileOption("debugOutputUnlabeledClassInformation", 'h',
            "Single column containing the class label or -999 indicating missing labels.", null, "csv", true);

    private int numUnlabeledData = 0;

    private Learner getLearner(ExampleStream stream) {
        Learner learner;
        if (this.onlyLabeledDataOption.isSet()) {
            learner = (Learner) getPreparedClassOption(this.standardLearnerOption);
        } else {
            learner = (SemiSupervisedLearner) getPreparedClassOption(this.sslLearnerOption);
        }

        learner.setModelContext(stream.getHeader());
        if (learner.isRandomizable()) {
            learner.setRandomSeed(this.randomSeedOption.getValue());
            learner.resetLearning();
        }
        return learner;
    }

    private String getLearnerString() {
        if (this.onlyLabeledDataOption.isSet()) {
            return this.standardLearnerOption.getValueAsCLIString();
        } else {
            return this.sslLearnerOption.getValueAsCLIString();
        }
    }

    private PrintStream newPrintStream(File f, String err_msg) {
        if (f == null)
            return null;
        try {
            return new PrintStream(new FileOutputStream(f, f.exists()), true);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(err_msg, e);
        }
    }

    private Object internalDoMainTask(TaskMonitor monitor, ObjectRepository repository, LearningPerformanceEvaluator evaluator)
    {
        int maxInstances = this.instanceLimitOption.getValue();
        int maxSeconds = this.timeLimitOption.getValue();
        int delayLength = this.delayLengthOption.getValue();
        double labelProbability = this.labelProbabilityOption.getValue();
        String streamString = this.streamOption.getValueAsCLIString();
        RandomGenerator taskRandom = new MersenneTwister(this.randomSeedOption.getValue());
        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
        Learner learner = getLearner(stream);
        String learnerString = getLearnerString();

        // A number of output files used for debugging and manual evaluation
        PrintStream dumpStream = newPrintStream(this.dumpFileOption.getFile(), "Failed to create dump file");
        PrintStream predStream = newPrintStream(this.outputPredictionFileOption.getFile(),
                "Failed to create prediction file");
        PrintStream labelStream = newPrintStream(this.debugOutputUnlabeledClassInformation.getFile(),
                "Failed to create unlabeled class information file");
        if (labelStream != null)
            labelStream.println("class");

        // Setup evaluation
        monitor.setCurrentActivity("Evaluating learner...", -1.0);
        LearningCurve learningCurve = new LearningCurve("learning evaluation instances");

        boolean firstDump = true;
        boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
        long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        long lastEvaluateStartTime = evaluateStartTime;
        long instancesProcessed = 0;
        int secondsElapsed = 0;
        double RAMHours = 0.0;

        // The buffer is a list of tuples. The first element is the index when
        // it should be emitted. The second element is the instance itself.
        List<Pair<Long, Example>> delayBuffer = new ArrayList<Pair<Long, Example>>();

        while (stream.hasMoreInstances()
                && ((maxInstances < 0) || (instancesProcessed < maxInstances))
                && ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {
            instancesProcessed++;

            // TRAIN on delayed instances
            while (delayBuffer.size() > 0
                    && delayBuffer.get(0).getKey() == instancesProcessed) {
                Example delayedExample = delayBuffer.remove(0).getValue();
                learner.trainOnInstance(delayedExample);
            }

            // Obtain the next Example from the stream.
            // The instance is expected to be labeled.
            Example originalExample = stream.nextInstance();
            Example unlabeledExample = originalExample.copy();
            int trueClass = (int) ((Instance) originalExample.getData()).classValue();

            // In case it is set, then the label is not removed. We want to pass the
            // labelled data to the learner even in trainOnUnlabeled data to generate statistics such as number
            // of correctly pseudo-labeled instances.
            if (!debugPseudoLabelsOption.isSet()) {
                // Remove the label of the unlabeledExample indirectly through
                // unlabeledInstanceData.
                Instance instance = (Instance) unlabeledExample.getData();
                instance.setMissing(instance.classIndex());
            }

            // WARMUP
            // Train on the initial instances. These are not used for testing!
            if (instancesProcessed <= this.initialWindowSizeOption.getValue()) {
                if (learner instanceof SemiSupervisedLearner)
                    ((SemiSupervisedLearner) learner).addInitialWarmupTrainingInstances();
                learner.trainOnInstance(originalExample);
                continue;
            }

            Boolean is_labeled = labelProbability > taskRandom.nextDouble();
            if (!is_labeled) {
                this.numUnlabeledData++;
                if (labelStream != null)
                    labelStream.println(-999);
            } else {
                if (labelStream != null)
                    labelStream.println((int) trueClass);
            }

            // TEST
            // Obtain the prediction for the testInst (i.e. no label)
            double[] prediction = learner.getVotesForInstance(unlabeledExample);

            // Output prediction
            if (predStream != null) {
                // Assuming that the class label is not missing for the originalInstanceData
                predStream.println(Utils.maxIndex(prediction) + "," + trueClass);
            }
            evaluator.addResult(originalExample, prediction);

            // TRAIN
            if (is_labeled && delayLength >= 0) {
                // The instance will be labeled but has been delayed
                if (learner instanceof SemiSupervisedLearner)
                {
                    ((SemiSupervisedLearner) learner).trainOnUnlabeledInstance((Instance) unlabeledExample.getData());
                }
                delayBuffer.add(
                        new MutablePair<Long, Example>(1 + instancesProcessed + delayLength, originalExample));
            } else if (is_labeled) {
                // The instance will be labeled and is not delayed e.g delayLength = -1
                learner.trainOnInstance(originalExample);
            } else {
                // The instance will never be labeled
                if (learner instanceof SemiSupervisedLearner)
                    ((SemiSupervisedLearner) learner).trainOnUnlabeledInstance((Instance) unlabeledExample.getData());
            }

            if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0 || !stream.hasMoreInstances()) {
                long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
                double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);
                double timeIncrement = TimingUtils.nanoTimeToSeconds(evaluateTime - lastEvaluateStartTime);
                double RAMHoursIncrement = learner.measureByteSize() / (1024.0 * 1024.0 * 1024.0); // GBs
                RAMHoursIncrement *= (timeIncrement / 3600.0); // Hours
                RAMHours += RAMHoursIncrement;
                lastEvaluateStartTime = evaluateTime;
                learningCurve.insertEntry(new LearningEvaluation(
                        new Measurement[] {
                                new Measurement(
                                        "learning evaluation instances",
                                        instancesProcessed),
                                new Measurement(
                                        "evaluation time ("
                                                + (preciseCPUTiming ? "cpu "
                                                : "")
                                                + "seconds)",
                                        time),
                                new Measurement(
                                        "model cost (RAM-Hours)",
                                        RAMHours),
                                new Measurement(
                                        "Unlabeled instances",
                                        this.numUnlabeledData)
                        },
                        evaluator, learner));
                if (dumpStream != null) {
                    if (firstDump) {
                        dumpStream.print("Learner,stream,randomSeed,");
                        dumpStream.println(learningCurve.headerToString());
                        firstDump = false;
                    }
                    dumpStream.print(learnerString + "," + streamString + ","
                            + this.randomSeedOption.getValueAsCLIString() + ",");
                    dumpStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));
                    dumpStream.flush();
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
        if (dumpStream != null) {
            dumpStream.close();
        }
        if (predStream != null) {
            predStream.close();
        }
        return learningCurve;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        // Some resource must be closed at the end of the task
        try (
                LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption)
        ) {
            return internalDoMainTask(monitor, repository, evaluator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }
}
