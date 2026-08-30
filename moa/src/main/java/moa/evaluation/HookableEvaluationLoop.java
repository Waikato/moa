package moa.evaluation;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.SemiSupervisedLearner;
import moa.core.Example;
import moa.learners.Learner;
import moa.streams.ExampleStream;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/** Prequential evaluation loop that notifies registered hooks as it processes a stream. */
public class HookableEvaluationLoop {

    /** Marker interface for callbacks registrable via {@link #register(Hook)}. */
    public interface Hook {}

    /**
     * Fires once per prediction (classifier or regressor), before training on the instance.
     *
     * <p>{@code votes} is the raw output of {@link Learner#getVotesForInstance}: a class-vote
     * distribution for classifiers, or a single-element (or empty, if the learner has no
     * prediction yet) array for regressors.
     *
     * <p>Be mindful of the performance implications of registering per-instance hooks, as they can
     * introduce significant overhead (e.g. JPype). Consider using {@link OnClassifierWindow} or
     * {@link OnRegressorWindow} for window based aggregates instead.
     */
    @FunctionalInterface
    public interface OnPrediction extends Hook {
        void onPrediction(long i, Example<Instance> instance, double[] votes);
    }

    /** Fires when a classification window closes, with that window's votes and true labels. */
    @FunctionalInterface
    public interface OnClassifierWindow extends Hook {
        void onClassifierWindow(long i, double[][] votes, int[] labels);
    }

    /** Fires for each unlabeled instance a semi-supervised learner pseudo-labels. */
    @FunctionalInterface
    public interface OnClassifierPseudoLabel extends Hook {
        void onClassifierPseudoLabel(
                long i, Instance instance, int pseudoLabel, int trueLabel, boolean isLabeled);
    }

    /** Fires when a regression window closes, with that window's predictions and targets. */
    @FunctionalInterface
    public interface OnRegressorWindow extends Hook {
        void onRegressorWindow(long i, double[] predictions, double[] targets);
    }

    private final List<OnPrediction> onPredictionHooks = new ArrayList<>();
    private final List<OnClassifierWindow> onClassifierWindowHooks = new ArrayList<>();
    private final List<OnClassifierPseudoLabel> onClassifierPseudoLabelHooks = new ArrayList<>();
    private final List<OnRegressorWindow> onRegressorWindowHooks = new ArrayList<>();

    // Builders

    /** Registers a prediction hook (fires for both classifier and regressor runs). */
    public HookableEvaluationLoop onPrediction(OnPrediction hook) {
        this.onPredictionHooks.add(Objects.requireNonNull(hook));
        return this;
    }

    /** Registers a classifier window-close hook. Returns this for chaining. */
    public HookableEvaluationLoop onClassifierWindow(OnClassifierWindow hook) {
        this.onClassifierWindowHooks.add(Objects.requireNonNull(hook));
        return this;
    }

    /** Registers a classifier pseudo-label hook. Returns this for chaining. */
    public HookableEvaluationLoop onClassifierPseudoLabel(OnClassifierPseudoLabel hook) {
        this.onClassifierPseudoLabelHooks.add(Objects.requireNonNull(hook));
        return this;
    }

    /** Registers a regressor window-close hook. Returns this for chaining. */
    public HookableEvaluationLoop onRegressorWindow(OnRegressorWindow hook) {
        this.onRegressorWindowHooks.add(Objects.requireNonNull(hook));
        return this;
    }

    /** Registers {@code hook} against every hook list whose type it implements. */
    public HookableEvaluationLoop register(Hook hook) {
        if (hook instanceof OnPrediction) {
            this.onPredictionHooks.add((OnPrediction) hook);
        }
        if (hook instanceof OnClassifierWindow) {
            this.onClassifierWindowHooks.add((OnClassifierWindow) hook);
        }
        if (hook instanceof OnClassifierPseudoLabel) {
            this.onClassifierPseudoLabelHooks.add((OnClassifierPseudoLabel) hook);
        }
        if (hook instanceof OnRegressorWindow) {
            this.onRegressorWindowHooks.add((OnRegressorWindow) hook);
        }
        return this;
    }

    // Notifier helpers

    protected void notifyPrediction(long i, Example<Instance> instance, double[] votes) {
        for (OnPrediction hook : this.onPredictionHooks) hook.onPrediction(i, instance, votes);
    }

    protected void notifyClassifierWindow(long i, double[][] votes, int[] labels) {
        for (OnClassifierWindow hook : this.onClassifierWindowHooks)
            hook.onClassifierWindow(i, votes, labels);
    }

    protected void notifyClassifierPseudoLabel(
            long i, Instance instance, int pseudoLabel, int trueLabel, boolean isLabeled) {
        for (OnClassifierPseudoLabel hook : this.onClassifierPseudoLabelHooks)
            hook.onClassifierPseudoLabel(i, instance, pseudoLabel, trueLabel, isLabeled);
    }

    protected void notifyRegressorWindow(long i, double[] predictions, double[] targets) {
        for (OnRegressorWindow hook : this.onRegressorWindowHooks)
            hook.onRegressorWindow(i, predictions, targets);
    }

    // Runners

    /** Runs prequential classification: predict, notify hooks, then train, per instance. */
    public void runClassifier(
            ExampleStream<?> stream,
            Learner<Example<Instance>> learner,
            long maxInstances,
            int windowSize) {
        if (!stream.hasMoreInstances()) stream.restart();

        double[][] windowVotes = new double[windowSize][];
        int[] windowLabels = new int[windowSize];

        long i = 0; // Use long to prevent integer overflow on large streams
        for (; stream.hasMoreInstances() && (maxInstances == -1 || i < maxInstances); i++) {
            // Prediction
            Example<Instance> instance = stream.nextInstance();
            double[] votes = learner.getVotesForInstance(instance);

            // Instance Hook
            notifyPrediction(i, instance, votes);

            // Window Hook
            int windowIndex = (int) (i % windowSize);
            windowVotes[windowIndex] = votes;
            windowLabels[windowIndex] = (int) Math.round(instance.getData().classValue());

            if (windowIndex == windowSize - 1) notifyClassifierWindow(i, windowVotes, windowLabels);

            // Train
            learner.trainOnInstance(instance);
        }

        // Handle last window
        int remainder = (int) (i % windowSize);
        if (remainder > 0) {
            notifyClassifierWindow(
                    i - 1,
                    java.util.Arrays.copyOf(windowVotes, remainder),
                    java.util.Arrays.copyOf(windowLabels, remainder));
        }
    }

    /** Runs prequential regression: predict, notify hooks, then train, per instance. */
    public void runRegressor(
            ExampleStream<?> stream,
            Learner<Example<Instance>> learner,
            long maxInstances,
            int windowSize) {
        if (!stream.hasMoreInstances()) stream.restart();

        double[] windowPredictions = new double[windowSize];
        double[] windowTargets = new double[windowSize];

        long i = 0;
        for (; stream.hasMoreInstances() && (maxInstances == -1 || i < maxInstances); i++) {
            // Prediction
            Example<Instance> instance = stream.nextInstance();
            double[] votes = learner.getVotesForInstance(instance);

            // Instance Hook
            notifyPrediction(i, instance, votes);

            // Window Hook — some regressors return an empty array when they have no
            // prediction yet; represent that as NaN rather than a made-up 0.0.
            int windowIndex = (int) (i % windowSize);
            windowPredictions[windowIndex] = votes.length > 0 ? votes[0] : Double.NaN;
            windowTargets[windowIndex] = instance.getData().classValue();

            if (windowIndex == windowSize - 1)
                notifyRegressorWindow(i, windowPredictions, windowTargets);

            // Train
            learner.trainOnInstance(instance);
        }

        // Handle last window
        int remainder = (int) (i % windowSize);
        if (remainder > 0) {
            notifyRegressorWindow(
                    i - 1,
                    java.util.Arrays.copyOf(windowPredictions, remainder),
                    java.util.Arrays.copyOf(windowTargets, remainder));
        }
    }

    /**
     * Runs prequential semi-supervised classification: warms up on the first {@code
     * initialWindowSize} instances, then for each remaining instance predicts, notifies hooks, and
     * trains on either the true label (with probability {@code labelProbability}, optionally after
     * {@code delayLength} instances) or a pseudo-label from the learner.
     */

    /**
     * Runs prequential semi-supervised classification with a warmup period, delayed training, and
     * probabilistic labeling.
     *
     * @param stream the data stream to evaluate on.
     * @param learner the learning algorithm to evaluate maybe either purely-supervised or
     *     semi-supervised.
     * @param maxInstances the maximum number of instances to evaluate on.
     * @param windowSize the size of the evaluation window.
     * @param warmupInstances the number of initial instances to use for warmup.
     * @param delay the number of instances to delay training for.
     * @param labelProbability the probability of using the true label for training.
     * @param randomSeed the seed for the random number generator.
     */
    public void runSSLClassifier(
            ExampleStream<Example<Instance>> stream,
            Learner<Example<Instance>> learner,
            long maxInstances,
            int windowSize,
            long warmupInstances,
            long delay,
            double labelProbability,
            int randomSeed) {

        RandomGenerator rng = new MersenneTwister(randomSeed);

        if (!stream.hasMoreInstances()) stream.restart();

        double[][] windowVotes = new double[windowSize][];
        int[] windowLabels = new int[windowSize];

        // Use a Queue to ensure O(1) polling performance for high delay buffers
        Queue<Pair<Long, Example<Instance>>> delayBuffer = new ArrayDeque<>();

        long i = 0;
        long evaluatedCount = 0; // Decoupled from 'i' to handle warmup jumps safely

        for (; stream.hasMoreInstances() && (maxInstances == -1 || i < maxInstances); i++) {

            // Train on delayed instances
            while (!delayBuffer.isEmpty() && delayBuffer.peek().getKey() == i) {
                Example<Instance> delayedExample = delayBuffer.poll().getValue();
                learner.trainOnInstance(delayedExample);
            }

            Example<Instance> instance = stream.nextInstance();
            int trueLabel = (int) Math.round(instance.getData().classValue());

            // Warmup
            if (i < warmupInstances) {
                learner.trainOnInstance(instance);
                continue; // Skip evaluation hooks during warmup
            }

            // Prediction
            double[] votes = learner.getVotesForInstance(instance);

            // Instance Hook
            notifyPrediction(i, instance, votes);

            // Window Hook
            int windowIndex = (int) (evaluatedCount % windowSize);
            windowVotes[windowIndex] = votes;
            windowLabels[windowIndex] = trueLabel;

            if (windowIndex == windowSize - 1) notifyClassifierWindow(i, windowVotes, windowLabels);

            evaluatedCount++;

            // Train (SSL & Delay Logic)
            boolean isLabeled = labelProbability > rng.nextDouble();
            int pseudoLabel = -1;

            if (isLabeled && delay < 0) {
                learner.trainOnInstance(instance);
            } else {
                // Prepare Unlabeled Example
                Example<Instance> unlabeledInstance = instance.copy();
                // Mask the class value to simulate an unlabeled instance
                Instance __instance = unlabeledInstance.getData();
                __instance.setMissing(__instance.classIndex());

                if (learner instanceof SemiSupervisedLearner) {
                    SemiSupervisedLearner semiLearner = (SemiSupervisedLearner) learner;
                    pseudoLabel = semiLearner.trainOnUnlabeledInstance(unlabeledInstance.getData());
                }
                if (isLabeled) {
                    delayBuffer.add(new MutablePair<>(1 + i + delay, instance));
                }
                notifyClassifierPseudoLabel(
                        i, instance.getData(), pseudoLabel, trueLabel, isLabeled);
            }
        }

        // Handle last window
        int remainder = (int) (evaluatedCount % windowSize);
        if (remainder > 0) {
            notifyClassifierWindow(
                    i - 1, // Pass the global stream index where the window closed
                    java.util.Arrays.copyOf(windowVotes, remainder),
                    java.util.Arrays.copyOf(windowLabels, remainder));
        }
    }
}
