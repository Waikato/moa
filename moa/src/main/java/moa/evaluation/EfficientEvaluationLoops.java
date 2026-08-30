package moa.evaluation;

import com.yahoo.labs.samoa.instances.Instance;

import moa.core.Example;
import moa.core.Measurement;
import moa.learners.Learner;
import moa.streams.ExampleStream;

import java.util.ArrayList;
import java.util.HashMap;

public class EfficientEvaluationLoops {

    public static class PrequentialResult {
        public ArrayList<double[]> windowedResults;
        public double[] cumulativeResults;
        public ArrayList<Number> targets;
        public ArrayList<Number> predictions;
        public HashMap<String, Double> otherMeasurements;

        public PrequentialResult(
                ArrayList<double[]> windowedResults,
                double[] cumulativeResults,
                ArrayList<Number> targets,
                ArrayList<Number> predictions,
                HashMap<String, Double> otherMeasurements) {
            this.windowedResults = windowedResults;
            this.cumulativeResults = cumulativeResults;
            this.targets = targets;
            this.predictions = predictions;
            this.otherMeasurements = otherMeasurements;
        }

        public PrequentialResult(
                ArrayList<double[]> windowedResults,
                double[] cumulativeResults,
                ArrayList<Number> targets,
                ArrayList<Number> predictions) {
            this(windowedResults, cumulativeResults, targets, predictions, null);
        }

        public PrequentialResult(
                ArrayList<double[]> windowedResults,
                double[] cumulativeResults,
                HashMap<String, Double> otherMeasurements) {
            this(windowedResults, cumulativeResults, null, null, otherMeasurements);
        }
    }

    /**
     * Calculates the test-then-train metrics and prequential windowed metrics at once.
     *
     * @param stream the data stream to evaluate on
     * @param learner the learning algorithm to evaluate
     * @param basicEvaluator the evaluator for cumulative test-then-train metrics; set to null to
     *     disable
     * @param windowedEvaluator the evaluator for prequential windowed metrics; set to null to
     *     disable
     * @param maxInstances stop evaluation after this many instances
     * @param windowSize the size of the prequential evaluation window
     * @param storeY whether to store the true class labels
     * @param storePredictions whether to store the predicted class labels
     * @param loop the hookable evaluation loop to use for running the evaluation
     * @return a list of evaluation results produced across the windowed evaluation
     */
    public static PrequentialResult prequentialEvaluationClassification(
            ExampleStream stream,
            Learner learner,
            LearningPerformanceEvaluator<Example<Instance>> basicEvaluator,
            LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator,
            long maxInstances,
            long windowSize,
            boolean storeY,
            boolean storePredictions,
            HookableEvaluationLoop loop) {
        if (!stream.getHeader().outputAttribute(1).isNominal()) {
            throw new IllegalArgumentException(
                    "Stream class attribute is not nominal. Use the regression variant instead.");
        }

        ArrayList<Number> targetValues = new ArrayList<>();
        ArrayList<Number> predictions = new ArrayList<>();
        ArrayList<double[]> windowedResults = new ArrayList<>();

        loop.onPrediction(
                (i, instance, votes) -> {
                    if (storeY) targetValues.add((int) Math.round(instance.getData().classValue()));
                    if (storePredictions) predictions.add(moa.core.Utils.maxIndex(votes));
                    if (basicEvaluator != null) basicEvaluator.addResult(instance, votes);
                    if (windowedEvaluator != null) windowedEvaluator.addResult(instance, votes);
                });

        if (windowedEvaluator != null) {
            loop.onClassifierWindow(
                    (u, votes, labels) -> windowedResults.add(flatten(windowedEvaluator)));
        }

        loop.runClassifier(stream, learner, maxInstances, (int) windowSize);

        double[] cumulativeResults = basicEvaluator != null ? flatten(basicEvaluator) : null;
        return new PrequentialResult(windowedResults, cumulativeResults, targetValues, predictions);
    }

    /**
     * Variant of {@link prequentialEvaluationClassification} that uses a default {@link
     * HookableEvaluationLoop}.
     */
    public static PrequentialResult prequentialEvaluationClassification(
            ExampleStream stream,
            Learner learner,
            LearningPerformanceEvaluator<Example<Instance>> basicEvaluator,
            LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator,
            long maxInstances,
            long windowSize,
            boolean storeY,
            boolean storePredictions) {
        return prequentialEvaluationClassification(
                stream,
                learner,
                basicEvaluator,
                windowedEvaluator,
                maxInstances,
                windowSize,
                storeY,
                storePredictions,
                new HookableEvaluationLoop());
    }

    /**
     * Calculates the test-then-train metrics and prequential windowed metrics at once for
     * regression tasks.
     *
     * @param stream the data stream to evaluate on
     * @param learner the learning algorithm to evaluate
     * @param basicEvaluator the evaluator for cumulative test-then-train metrics; set to null to
     *     disable
     * @param windowedEvaluator the evaluator for prequential windowed metrics; set to null to
     *     disable
     * @param maxInstances stop evaluation after this many instances
     * @param windowSize the size of the prequential evaluation window
     * @param storeY whether to store the true target values
     * @param storePredictions whether to store the predicted values; instances the learner had no
     *     prediction for yet are stored as {@link Double#NaN}
     * @param loop the hookable evaluation loop to use for running the evaluation
     * @return a list of evaluation results produced across the windowed evaluation
     */
    public static PrequentialResult prequentialEvaluationRegression(
            ExampleStream<Example<Instance>> stream,
            Learner learner,
            LearningPerformanceEvaluator<Example<Instance>> basicEvaluator,
            LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator,
            long maxInstances,
            long windowSize,
            boolean storeY,
            boolean storePredictions,
            HookableEvaluationLoop loop) {
        if (!stream.getHeader().outputAttribute(1).isNumeric()) {
            throw new IllegalArgumentException(
                    "Stream class attribute is not numeric. Use the classification variant"
                            + " instead.");
        }

        ArrayList<Number> targetValues = new ArrayList<>();
        ArrayList<Number> predictions = new ArrayList<>();
        ArrayList<double[]> windowedResults = new ArrayList<>();

        loop.onPrediction(
                (i, instance, votes) -> {
                    if (storeY) targetValues.add(instance.getData().classValue());
                    if (storePredictions)
                        predictions.add(votes.length > 0 ? votes[0] : Double.NaN);
                    if (basicEvaluator != null) basicEvaluator.addResult(instance, votes);
                    if (windowedEvaluator != null) windowedEvaluator.addResult(instance, votes);
                });

        if (windowedEvaluator != null) {
            loop.onRegressorWindow(
                    (u, votes, labels) -> windowedResults.add(flatten(windowedEvaluator)));
        }

        loop.runRegressor(stream, learner, maxInstances, (int) windowSize);

        double[] cumulativeResults = basicEvaluator != null ? flatten(basicEvaluator) : null;
        return new PrequentialResult(windowedResults, cumulativeResults, targetValues, predictions);
    }

    /**
     * Variant of {@link prequentialEvaluationRegression} that uses a default {@link
     * HookableEvaluationLoop}.
     */
    public static PrequentialResult prequentialEvaluationRegression(
            ExampleStream<Example<Instance>> stream,
            Learner learner,
            LearningPerformanceEvaluator<Example<Instance>> basicEvaluator,
            LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator,
            long maxInstances,
            long windowSize,
            boolean storeY,
            boolean storePredictions) {
        return prequentialEvaluationRegression(
                stream,
                learner,
                basicEvaluator,
                windowedEvaluator,
                maxInstances,
                windowSize,
                storeY,
                storePredictions,
                new HookableEvaluationLoop());
    }

    /**
     * Prequential evaluation for semi-supervised learning (SSL) tasks.
     *
     * @param stream the data stream to evaluate on.
     * @param learner the learning algorithm to evaluate.
     * @param basicEvaluator the basic performance evaluator.
     * @param windowedEvaluator the windowed performance evaluator.
     * @param maxInstances the maximum number of instances to evaluate.
     * @param windowSize the size of the evaluation window.
     * @param initialWindowSize the warmup period before evaluation begins.
     * @param delayLength the delay length for labeling.
     * @param labelProbability the probability of an instance being labeled.
     * @param randomSeed the random seed for reproducibility.
     * @param debugPseudoLabels whether instances should secretly still contain labels.
     * @param storeY whether to store the true labels.
     * @param storePredictions whether to store the predicted labels.
     * @param loop the evaluation loop to use.
     * @return the result of the prequential SSL evaluation.
     */
    public static PrequentialResult prequentialSSLEvaluation(
            ExampleStream<Example<Instance>> stream,
            Learner learner,
            LearningPerformanceEvaluator<Example<Instance>> basicEvaluator,
            LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator,
            long maxInstances,
            long windowSize,
            long initialWindowSize,
            long delayLength,
            double labelProbability,
            int randomSeed,
            boolean debugPseudoLabels,
            boolean storeY,
            boolean storePredictions,
            HookableEvaluationLoop loop) {
        if (!stream.getHeader().outputAttribute(1).isNominal()) {
            throw new IllegalArgumentException(
                    "Stream class attribute is not nominal. Use the regression variant instead.");
        }
        // Hacky way to allow modification within lambda expressions
        // TODO: Should probably pop these out into a separate object that we register.
        int[] numUnlabeledData = {0};
        int[] numCorrectPseudoLabeled = {0};
        int[] numInstancesTested = {0};
        HashMap<String, Double> otherMeasures = new HashMap<>();

        ArrayList<Number> targetValues = new ArrayList<>();
        ArrayList<Number> predictions = new ArrayList<>();
        ArrayList<double[]> windowedResults = new ArrayList<>();

        loop.onPrediction(
                (i, instance, votes) -> {
                    numInstancesTested[0]++;
                    if (storeY) targetValues.add((int) Math.round(instance.getData().classValue()));
                    if (storePredictions) predictions.add(moa.core.Utils.maxIndex(votes));
                    if (basicEvaluator != null) basicEvaluator.addResult(instance, votes);
                    if (windowedEvaluator != null) windowedEvaluator.addResult(instance, votes);
                });
        loop.onClassifierPseudoLabel(
                (long i, Instance instance, int pseudoLabel, int trueLabel, boolean isLabeled) -> {
                    if (!isLabeled) {
                        numUnlabeledData[0]++;
                        if (pseudoLabel == trueLabel) {
                            numCorrectPseudoLabeled[0]++;
                        }
                    }
                });

        if (windowedEvaluator != null) {
            loop.onClassifierWindow(
                    (u, votes, labels) -> windowedResults.add(flatten(windowedEvaluator)));
        }

        loop.runSSLClassifier(
                stream,
                learner,
                maxInstances,
                (int) windowSize,
                initialWindowSize,
                delayLength,
                labelProbability,
                randomSeed);

        double[] cumulativeResults = basicEvaluator != null ? flatten(basicEvaluator) : null;

        // TODO: Add this measures in a windowed way.
        otherMeasures.put("num_unlabeled_instances", (double) numUnlabeledData[0]);
        otherMeasures.put("num_correct_pseudo_labeled", (double) numCorrectPseudoLabeled[0]);
        otherMeasures.put("num_instances_tested", (double) numInstancesTested[0]);
        otherMeasures.put(
                "pseudo_label_accuracy",
                (double) numCorrectPseudoLabeled[0] / numInstancesTested[0]);

        return new PrequentialResult(
                windowedResults, cumulativeResults, targetValues, predictions, otherMeasures);
    }

    /**
     * Overload of {@link #prequentialSSLEvaluation} that uses a default {@link
     * HookableEvaluationLoop}.
     */
    public static PrequentialResult prequentialSSLEvaluation(
            ExampleStream<Example<Instance>> stream,
            Learner learner,
            LearningPerformanceEvaluator<Example<Instance>> basicEvaluator,
            LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator,
            long maxInstances,
            long windowSize,
            long initialWindowSize,
            long delayLength,
            double labelProbability,
            int randomSeed,
            boolean debugPseudoLabels,
            boolean storeY,
            boolean storePredictions) {
        return prequentialSSLEvaluation(
                stream,
                learner,
                basicEvaluator,
                windowedEvaluator,
                maxInstances,
                windowSize,
                initialWindowSize,
                delayLength,
                labelProbability,
                randomSeed,
                debugPseudoLabels,
                storeY,
                storePredictions,
                new HookableEvaluationLoop());
    }

    /** The current value of every measurement the evaluator reports, in its own order. */
    private static double[] flatten(LearningPerformanceEvaluator<Example<Instance>> evaluator) {
        Measurement[] measurements = evaluator.getPerformanceMeasurements();
        double[] values = new double[measurements.length];
        for (int i = 0; i < values.length; ++i) values[i] = measurements[i].getValue();
        return values;
    }

    /**
     * @deprecated Use {@link #prequentialEvaluationClassification} or {@link
     *     #prequentialSSLEvaluation} instead. Kept for backwards compatibility with callers using
     *     the old naming convention.
     */
    @Deprecated
    public static PrequentialResult PrequentialEvaluation(
            ExampleStream stream,
            Learner learner,
            LearningPerformanceEvaluator<Example<Instance>> basicEvaluator,
            LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator,
            long maxInstances,
            long windowSize,
            boolean storeY,
            boolean storePredictions) {

        boolean classification = stream.getHeader().outputAttribute(1).isNominal();
        if (classification)
            return prequentialEvaluationClassification(
                    stream,
                    learner,
                    basicEvaluator,
                    windowedEvaluator,
                    maxInstances,
                    windowSize,
                    storeY,
                    storePredictions);
        else
            return prequentialEvaluationRegression(
                    stream,
                    learner,
                    basicEvaluator,
                    windowedEvaluator,
                    maxInstances,
                    windowSize,
                    storeY,
                    storePredictions);
    }

    /**
     * @deprecated Use {@link #prequentialSSLEvaluation} instead. Kept for backwards compatibility
     *     with callers using the old naming convention.
     */
    @Deprecated
    public static PrequentialResult PrequentialSSLEvaluation(
            ExampleStream<Example<Instance>> stream,
            Learner learner,
            LearningPerformanceEvaluator<Example<Instance>> basicEvaluator,
            LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator,
            long maxInstances,
            long windowSize,
            long initialWindowSize,
            long delayLength,
            double labelProbability,
            int randomSeed,
            boolean debugPseudoLabels,
            boolean storeY,
            boolean storePredictions,
            HookableEvaluationLoop loop) {
        return prequentialSSLEvaluation(
                stream,
                learner,
                basicEvaluator,
                windowedEvaluator,
                maxInstances,
                windowSize,
                initialWindowSize,
                delayLength,
                labelProbability,
                randomSeed,
                debugPseudoLabels,
                storeY,
                storePredictions,
                loop);
    }

    /**
     * @deprecated Use {@link #prequentialSSLEvaluation} instead. Kept for backwards compatibility
     *     with callers using the old naming convention.
     */
    @Deprecated
    public static PrequentialResult PrequentialSSLEvaluation(
            ExampleStream<Example<Instance>> stream,
            Learner learner,
            LearningPerformanceEvaluator<Example<Instance>> basicEvaluator,
            LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator,
            long maxInstances,
            long windowSize,
            long initialWindowSize,
            long delayLength,
            double labelProbability,
            int randomSeed,
            boolean debugPseudoLabels,
            boolean storeY,
            boolean storePredictions) {
        return prequentialSSLEvaluation(
                stream,
                learner,
                basicEvaluator,
                windowedEvaluator,
                maxInstances,
                windowSize,
                initialWindowSize,
                delayLength,
                labelProbability,
                randomSeed,
                debugPseudoLabels,
                storeY,
                storePredictions);
    }
}
