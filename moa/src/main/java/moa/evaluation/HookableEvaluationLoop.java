package moa.evaluation;

import com.yahoo.labs.samoa.instances.Instance;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.Utils;
import moa.evaluation.EfficientEvaluationLoops.PrequentialResult;
import moa.learners.Learner;
import moa.streams.ExampleStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A prequential evaluation loop that callers can observe by registering hooks.
 *
 * <p>{@link EfficientEvaluationLoops#PrequentialEvaluation} runs entirely inside Java and only
 * returns aggregated measurements, so a caller on the other side of a language boundary cannot see
 * anything the loop does while it runs. This class runs the same loop, but lets a caller register
 * callbacks that are invoked per instance ({@link OnLabel}) or per window ({@link OnWindowClose}).
 *
 * <p>Hooks receive normalized predicted probabilities rather than MOA's raw votes, which is what
 * score-based metrics such as AUC need. The registered evaluators still see the raw votes, so the
 * returned {@link PrequentialResult} is unaffected by whether hooks are present.
 *
 * <pre>
 * PrequentialResult result = new HookableEvaluationLoop()
 *         .registerBasic(basicEvaluator)
 *         .registerWindowed(windowedEvaluator)
 *         .register((OnWindowClose) (predProbs, labels) -&gt; ...)
 *         .run(stream, learner, 1000, 100, true, true);
 * </pre>
 */
public class HookableEvaluationLoop {

    /**
     * Marker for anything that can be passed to {@link #register(Hook)}. Implement one or both of
     * {@link OnLabel} and {@link OnWindowClose}; a hook that implements neither is rejected.
     */
    public interface Hook {
    }

    /** Called once per instance, after the learner has predicted but before it has trained. */
    @FunctionalInterface
    public interface OnLabel extends Hook {
        /**
         * @param instance  the instance just tested on. It belongs to the stream and may be reused
         *                  or mutated once this call returns, so a hook must not retain it.
         * @param predProbs predicted probabilities for {@code instance}, summing to one. All zeros
         *                  if the learner produced no votes at all, which happens while it is
         *                  still untrained.
         */
        void onLabel(Example<Instance> instance, double[] predProbs);
    }

    /**
     * Called once per window, including the trailing partial window when the stream does not divide
     * evenly into windows.
     */
    @FunctionalInterface
    public interface OnWindowClose extends Hook {
        /**
         * @param predProbs one row per instance in the window, each summing to one. Rows may differ
         *                  in length: MOA grows the vote array as it encounters new classes, so
         *                  early rows can be shorter than later ones.
         * @param labels    the true class index of each instance in the window, parallel to
         *                  {@code predProbs}.
         */
        void onWindowClose(double[][] predProbs, int[] labels);
    }

    protected final List<OnLabel> onLabelHooks = new ArrayList<>();
    protected final List<OnWindowClose> onWindowCloseHooks = new ArrayList<>();
    protected LearningPerformanceEvaluator<Example<Instance>> basicEvaluator;
    protected LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator;

    /**
     * Registers a callback. A hook implementing both sub-interfaces is registered for both.
     *
     * @throws IllegalArgumentException if the hook implements neither sub-interface, which would
     *                                  otherwise silently do nothing.
     */
    public HookableEvaluationLoop register(Hook hook) {
        boolean registered = false;
        if (hook instanceof OnLabel) {
            this.onLabelHooks.add((OnLabel) hook);
            registered = true;
        }
        if (hook instanceof OnWindowClose) {
            this.onWindowCloseHooks.add((OnWindowClose) hook);
            registered = true;
        }
        if (!registered)
            throw new IllegalArgumentException(
                    "Hook must implement OnLabel and/or OnWindowClose, got: " + hook.getClass().getName());
        return this;
    }

    /**
     * Sets the evaluator producing {@link PrequentialResult#cumulativeResults}. May be null, in
     * which case no cumulative results are produced.
     */
    public HookableEvaluationLoop registerBasic(LearningPerformanceEvaluator<Example<Instance>> evaluator) {
        this.basicEvaluator = evaluator;
        return this;
    }

    /**
     * Sets the evaluator sampled at each window boundary to produce
     * {@link PrequentialResult#windowedResults}. May be null, in which case no windowed results are
     * produced. {@link OnWindowClose} hooks fire regardless of whether this is set.
     */
    public HookableEvaluationLoop registerWindowed(LearningPerformanceEvaluator<Example<Instance>> evaluator) {
        this.windowedEvaluator = evaluator;
        return this;
    }

    /**
     * Runs test-then-train over the stream, notifying any registered hooks as it goes.
     *
     * @param maxInstances     stop after this many instances, or -1 for the whole stream.
     * @param windowSize       how many instances make up a window.
     * @param storeY           collect the true class of every instance into the result.
     * @param storePredictions collect the predicted class of every instance into the result.
     */
    public PrequentialResult run(ExampleStream stream, Learner learner,
                                 long maxInstances, long windowSize,
                                 boolean storeY, boolean storePredictions) {
        int instancesProcessed = 0;

        if (!stream.hasMoreInstances())
            stream.restart();

        ArrayList<double[]> windowed_results = new ArrayList<>();
        ArrayList<Integer> targetValues = new ArrayList<>();
        ArrayList<Integer> predictions = new ArrayList<>();

        // Only buffer a window's observations when somebody is going to be handed them.
        boolean buffering = !this.onWindowCloseHooks.isEmpty();
        ArrayList<double[]> windowPredProbs = buffering ? new ArrayList<>() : null;
        ArrayList<Integer> windowLabels = buffering ? new ArrayList<>() : null;

        while (stream.hasMoreInstances() &&
                (maxInstances == -1 || instancesProcessed < maxInstances)) {

            Example<Instance> instance = stream.nextInstance();

            double[] prediction = learner.getVotesForInstance(instance);

            // Update evaluators and store predictions if requested
            if (this.basicEvaluator != null)
                this.basicEvaluator.addResult(instance, prediction);
            if (this.windowedEvaluator != null)
                this.windowedEvaluator.addResult(instance, prediction);
            if (storePredictions)
                predictions.add(Utils.maxIndex(prediction));
            if (storeY)
                targetValues.add((int) Math.round(instance.getData().classValue()));

            // Notify hooks. The evaluators above deliberately saw the raw votes.
            if (!this.onLabelHooks.isEmpty()) {
                double[] predProbs = toProbs(prediction);
                for (OnLabel hook : this.onLabelHooks)
                    hook.onLabel(instance, predProbs);
            }
            if (buffering) {
                windowPredProbs.add(toProbs(prediction));
                windowLabels.add((int) Math.round(instance.getData().classValue()));
            }

            learner.trainOnInstance(instance);
            instancesProcessed++;

            // Store windowed results if requested
            if (instancesProcessed % windowSize == 0) {
                if (this.windowedEvaluator != null)
                    windowed_results.add(flatten(this.windowedEvaluator));
                if (buffering)
                    closeWindow(windowPredProbs, windowLabels);
            }
        }
        // The stream ran out mid-window; close it out anyway.
        if (instancesProcessed % windowSize != 0) {
            if (this.windowedEvaluator != null)
                windowed_results.add(flatten(this.windowedEvaluator));
            if (buffering)
                closeWindow(windowPredProbs, windowLabels);
        }

        double[] cumulative_results = null;

        if (this.basicEvaluator != null)
            cumulative_results = flatten(this.basicEvaluator);

        return new PrequentialResult(
            windowed_results,
            cumulative_results,
            targetValues,
            predictions
        );
    }

    /** Hands the buffered window to every {@link OnWindowClose} hook, then empties the buffer. */
    protected void closeWindow(ArrayList<double[]> windowPredProbs, ArrayList<Integer> windowLabels) {
        double[][] predProbs = windowPredProbs.toArray(new double[0][]);
        int[] labels = new int[windowLabels.size()];
        for (int i = 0; i < labels.length; ++i)
            labels[i] = windowLabels.get(i);

        for (OnWindowClose hook : this.onWindowCloseHooks)
            hook.onWindowClose(predProbs, labels);

        windowPredProbs.clear();
        windowLabels.clear();
    }

    /** The current value of every measurement the evaluator reports, in its own order. */
    protected static double[] flatten(LearningPerformanceEvaluator<Example<Instance>> evaluator) {
        Measurement[] measurements = evaluator.getPerformanceMeasurements();
        double[] values = new double[measurements.length];
        for (int i = 0; i < values.length; ++i)
            values[i] = measurements[i].getValue();
        return values;
    }

    /**
     * Copies votes and scales them to sum to one. Unlike {@link Utils#normalize(double[])} this
     * tolerates a zero sum, which an untrained learner produces routinely, by returning the zeros
     * unchanged rather than throwing.
     */
    protected static double[] toProbs(double[] votes) {
        double[] probs = Arrays.copyOf(votes, votes.length);
        double sum = 0;
        for (double vote : probs)
            sum += vote;
        if (sum > 0 && !Double.isNaN(sum))
            for (int i = 0; i < probs.length; ++i)
                probs[i] /= sum;
        return probs;
    }
}
