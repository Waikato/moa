package moa.tasks;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import moa.classifiers.SemiSupervisedLearner;
import moa.core.ObjectRepository;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.preview.LearningCurve;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

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

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Learner to train.", SemiSupervisedLearner.class,
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

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        return null;
    }

    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }
}
