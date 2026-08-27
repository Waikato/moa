package moa.evaluation;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.bayes.NaiveBayes;
import moa.core.Example;
import moa.core.Utils;
import moa.evaluation.EfficientEvaluationLoops.PrequentialResult;
import moa.evaluation.HookableEvaluationLoop.Hook;
import moa.evaluation.HookableEvaluationLoop.OnLabel;
import moa.evaluation.HookableEvaluationLoop.OnWindowClose;
import moa.streams.generators.RandomTreeGenerator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HookableEvaluationLoopTest {

    private static final int N = 1000;
    private static final int WINDOW = 100;

    /**
     * Accuracy of the configuration below, captured by running the loop before it was made
     * hookable. Pinning it here is what makes the parity assertions meaningful, since
     * EfficientEvaluationLoops now delegates to the class under test.
     */
    private static final double BASELINE_ACCURACY = 70.8;

    private static final int ACCURACY_INDEX = 1; // "classifications correct (percent)"

    private RandomTreeGenerator stream() {
        RandomTreeGenerator stream = new RandomTreeGenerator();
        stream.treeRandomSeedOption.setValue(1);
        stream.instanceRandomSeedOption.setValue(1);
        stream.prepareForUse();
        return stream;
    }

    private NaiveBayes learner(RandomTreeGenerator stream) {
        NaiveBayes learner = new NaiveBayes();
        learner.setModelContext(stream.getHeader());
        learner.prepareForUse();
        return learner;
    }

    private BasicClassificationPerformanceEvaluator basicEvaluator() {
        BasicClassificationPerformanceEvaluator evaluator = new BasicClassificationPerformanceEvaluator();
        evaluator.prepareForUse();
        return evaluator;
    }

    private WindowClassificationPerformanceEvaluator windowedEvaluator() {
        WindowClassificationPerformanceEvaluator evaluator = new WindowClassificationPerformanceEvaluator();
        evaluator.widthOption.setValue(WINDOW);
        evaluator.prepareForUse();
        return evaluator;
    }

    /** Runs the loop with the standard configuration and whatever hooks are given. */
    private PrequentialResult run(Hook... hooks) {
        RandomTreeGenerator stream = stream();
        HookableEvaluationLoop loop = new HookableEvaluationLoop()
                .registerBasic(basicEvaluator())
                .registerWindowed(windowedEvaluator());
        for (Hook hook : hooks)
            loop.register(hook);
        return loop.run(stream, learner(stream), N, WINDOW, true, true);
    }

    @Test
    public void unhookedRunMatchesBaseline() {
        PrequentialResult result = run();

        assertEquals(BASELINE_ACCURACY, result.cumulativeResults[ACCURACY_INDEX], 1e-9);
        assertEquals(N / WINDOW, result.windowedResults.size());
        assertEquals(N, result.targets.size());
        assertEquals(N, result.predictions.size());
    }

    @Test
    public void staticEntryPointStillMatchesBaseline() {
        RandomTreeGenerator stream = stream();
        PrequentialResult result = EfficientEvaluationLoops.PrequentialEvaluation(
                stream, learner(stream), basicEvaluator(), windowedEvaluator(), N, WINDOW, true, true);

        assertEquals(BASELINE_ACCURACY, result.cumulativeResults[ACCURACY_INDEX], 1e-9);
        assertEquals(N / WINDOW, result.windowedResults.size());
    }

    @Test
    public void hooksDoNotChangeResults() {
        PrequentialResult unhooked = run();
        PrequentialResult hooked = run((OnLabel) (instance, predProbs) -> {
        }, (OnWindowClose) (predProbs, labels) -> {
        });

        assertEquals(unhooked.cumulativeResults[ACCURACY_INDEX], hooked.cumulativeResults[ACCURACY_INDEX], 1e-9);
        assertEquals(unhooked.targets, hooked.targets);
        assertEquals(unhooked.predictions, hooked.predictions);
        assertEquals(unhooked.windowedResults.size(), hooked.windowedResults.size());
        for (int i = 0; i < unhooked.windowedResults.size(); ++i)
            assertArrayEquals(unhooked.windowedResults.get(i), hooked.windowedResults.get(i), 1e-9);
    }

    @Test
    public void onLabelFiresOncePerInstanceWithProbabilities() {
        List<Integer> seenPredictions = new ArrayList<>();
        List<Integer> seenLabels = new ArrayList<>();

        PrequentialResult result = run((OnLabel) (instance, predProbs) -> {
            assertProbabilities(predProbs);
            seenPredictions.add(Utils.maxIndex(predProbs));
            seenLabels.add((int) Math.round(instance.getData().classValue()));
        });

        assertEquals(N, seenPredictions.size());
        assertEquals(result.predictions, seenPredictions);
        assertEquals(result.targets, seenLabels);
    }

    @Test
    public void onWindowCloseFiresPerWindowWithTheWindowsInstances() {
        List<double[][]> windows = new ArrayList<>();
        List<int[]> windowLabels = new ArrayList<>();

        PrequentialResult result = run((OnWindowClose) (predProbs, labels) -> {
            assertEquals(predProbs.length, labels.length);
            windows.add(predProbs);
            windowLabels.add(labels);
        });

        assertEquals(N / WINDOW, windows.size());

        List<Integer> flatPredictions = new ArrayList<>();
        List<Integer> flatLabels = new ArrayList<>();
        for (int w = 0; w < windows.size(); ++w) {
            assertEquals(WINDOW, windows.get(w).length);
            for (double[] predProbs : windows.get(w)) {
                assertProbabilities(predProbs);
                flatPredictions.add(Utils.maxIndex(predProbs));
            }
            for (int label : windowLabels.get(w))
                flatLabels.add(label);
        }

        assertEquals(result.predictions, flatPredictions);
        assertEquals(result.targets, flatLabels);
    }

    @Test
    public void trailingPartialWindowIsClosed() {
        RandomTreeGenerator stream = stream();
        List<Integer> windowSizes = new ArrayList<>();

        PrequentialResult result = new HookableEvaluationLoop()
                .registerWindowed(windowedEvaluator())
                .register((OnWindowClose) (predProbs, labels) -> windowSizes.add(labels.length))
                .run(stream, learner(stream), 250, WINDOW, false, false);

        assertEquals(3, windowSizes.size());
        assertEquals(Integer.valueOf(WINDOW), windowSizes.get(0));
        assertEquals(Integer.valueOf(WINDOW), windowSizes.get(1));
        assertEquals(Integer.valueOf(50), windowSizes.get(2));
        assertEquals(3, result.windowedResults.size());
    }

    /** A hook implementing both interfaces should be wired up by a single register() call. */
    @Test
    public void combinedHookReceivesBothCallbacks() {
        class Both implements OnLabel, OnWindowClose {
            int labels = 0;
            int windows = 0;

            @Override
            public void onLabel(Example<Instance> instance, double[] predProbs) {
                this.labels++;
            }

            @Override
            public void onWindowClose(double[][] predProbs, int[] labels) {
                this.windows++;
            }
        }

        Both hook = new Both();
        run(hook);

        assertEquals(N, hook.labels);
        assertEquals(N / WINDOW, hook.windows);
    }

    @Test
    public void hookImplementingNeitherInterfaceIsRejected() {
        try {
            new HookableEvaluationLoop().register(new Hook() {
            });
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("OnLabel"));
        }
    }

    /**
     * An untrained learner votes all zeros, which cannot be normalized. Those rows should come
     * through as zeros rather than throwing.
     */
    @Test
    public void zeroVotesSurviveAsZeros() {
        RandomTreeGenerator stream = stream();
        List<double[]> firstRows = new ArrayList<>();

        new HookableEvaluationLoop()
                .register((OnLabel) (instance, predProbs) -> {
                    if (firstRows.size() < 5)
                        firstRows.add(predProbs);
                })
                .run(stream, learner(stream), 5, WINDOW, false, false);

        assertEquals(5, firstRows.size());
        double sum = 0;
        for (double p : firstRows.get(0))
            sum += p;
        assertEquals(0.0, sum, 1e-9);
    }

    @Test
    public void nullEvaluatorsAreAccepted() {
        RandomTreeGenerator stream = stream();
        PrequentialResult result = new HookableEvaluationLoop()
                .registerBasic(null)
                .registerWindowed(null)
                .run(stream, learner(stream), N, WINDOW, false, false);

        assertEquals(null, result.cumulativeResults);
        assertTrue(result.windowedResults.isEmpty());
    }

    /** Rows sum to one, or to zero while the learner has nothing to say. */
    private static void assertProbabilities(double[] predProbs) {
        double sum = 0;
        for (double p : predProbs) {
            assertTrue("probability out of range: " + p, p >= 0 && p <= 1);
            sum += p;
        }
        assertTrue("row sums to " + sum, Math.abs(sum - 1) < 1e-9 || sum == 0);
    }
}
