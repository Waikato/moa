import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.Classifier;
import moa.classifiers.trees.BRIMRegressor;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.evaluation.BasicRegressionPerformanceEvaluator;
import moa.options.ClassOption;
import moa.streams.ArffFileStream;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Prequential (test-then-train) evaluation of a MOA regressor on an ARFF file, measuring accuracy, time and memory.
 *
 * <p>Usage: {@code java -javaagent:sizeofag-1.1.0.jar --add-opens ... PrequentialBenchmark <arff> <dataset name>
 * <config name> <learner CLI> <seed> <memory samples>}</p>
 *
 * <ul>
 *   <li>Accuracy: the final measurements of a {@link BasicRegressionPerformanceEvaluator} (MAE, RMSE, R², adjusted R²).</li>
 *   <li>Time: the wall-clock time spent in the learner only, i.e., in {@code getVotesForInstance} and
 *   {@code trainOnInstance}. Reading the data, evaluating and measuring memory are excluded.</li>
 *   <li>Memory: the size of the learner measured with the SizeOf agent ({@link SizeOf#fullSizeOf}) at
 *   {@code memory samples} evenly spaced points and at the end (peak, mean and final). For BRIMRegressor, its own
 *   structural estimate is recorded as well. With 0 memory samples, memory is not measured (-1), which saves the
 *   time of measuring large models when only the accuracy is needed (e.g., tuning). When the agent runs out of
 *   memory on a very large model (its set of visited objects does not fit in the heap next to the model, e.g., SGBR
 *   with FIMT-DD on the SUP3 streams), the model is measured in parts instead (see {@link #modelSize}), and a note
 *   is printed to stderr.</li>
 * </ul>
 *
 * <p>The seed is set with {@link Classifier#setRandomSeed(int)}, which works for all randomizable learners
 * (the {@code -r} option cannot be set from the command line in this version of MOA).</p>
 *
 * <p>Prints one CSV line (see {@link #HEADER}).</p>
 */
public class PrequentialBenchmark {

    public static final String HEADER = "dataset,config,seed,instances,mae,rmse,r2,adj_r2,learner_wall_s,"
            + "mem_peak_bytes,mem_mean_bytes,mem_final_bytes,est_peak_bytes,est_final_bytes";

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--header")) {
            System.out.println(HEADER);
            return;
        }
        if (args.length != 6) {
            System.err.println("Usage: PrequentialBenchmark <arff> <dataset> <config> <learner CLI> <seed> <memory samples>");
            System.exit(1);
        }
        String arff = args[0], dataset = args[1], config = args[2], learnerCli = args[3];
        int seed = Integer.parseInt(args[4]);
        int memorySamples = Integer.parseInt(args[5]);

        long numInstances = countInstances(arff);
        long sampleEvery = Math.max(1, numInstances / Math.max(1, memorySamples));

        ArffFileStream stream = new ArffFileStream(arff, -1);
        stream.prepareForUse();
        Classifier learner = (Classifier) ClassOption.cliStringToObject(learnerCli, Classifier.class, null);
        learner.setModelContext(stream.getHeader());
        learner.prepareForUse();
        // Some learners (e.g., AMRules) can only take a seed once prepared; resetLearning then applies it
        learner.setRandomSeed(seed);
        learner.resetLearning();
        BasicRegressionPerformanceEvaluator evaluator = new BasicRegressionPerformanceEvaluator();
        evaluator.reset();

        long learnerNanos = 0;
        long memPeak = -1, memSum = 0, memCount = 0, memFinal = -1, estPeak = -1, estFinal = -1;

        long i = 0;
        while (stream.hasMoreInstances()) {
            Example<Instance> example = stream.nextInstance();
            Instance instance = example.getData();
            long t0 = System.nanoTime();
            double[] prediction = learner.getVotesForInstance(instance);
            long t1 = System.nanoTime();
            evaluator.addResult(example, prediction);
            long t2 = System.nanoTime();
            learner.trainOnInstance(instance);
            long t3 = System.nanoTime();
            learnerNanos += (t1 - t0) + (t3 - t2);
            i++;
            if (memorySamples > 0 && (i % sampleEvery == 0 || !stream.hasMoreInstances())) {
                long size = modelSize(learner);
                if (size >= 0) {
                    memPeak = Math.max(memPeak, size);
                    memSum += size;
                    memCount++;
                    memFinal = size;
                }
                if (learner instanceof BRIMRegressor) {
                    long estimate = ((BRIMRegressor) learner).estimateByteSize();
                    estPeak = Math.max(estPeak, estimate);
                    estFinal = estimate;
                }
            }
        }
        double mae = measurement(evaluator, "mean absolute error");
        double rmse = measurement(evaluator, "root mean squared error");
        double r2 = measurement(evaluator, "coefficient of determination");
        double adjR2 = measurement(evaluator, "adjusted coefficient of determination");
        System.out.println(String.format(Locale.ROOT, "%s,%s,%d,%d,%.10g,%.10g,%.10g,%.10g,%.6f,%d,%d,%d,%d,%d",
                dataset, config, seed, i, mae, rmse, r2, adjR2, learnerNanos / 1e9,
                memPeak, memCount > 0 ? memSum / memCount : -1, memFinal, estPeak, estFinal));
    }

    /** Set once the agent has failed on this model, so that later samples go straight to the parts. */
    private static boolean measureInParts = false;

    /**
     * The size of a learner: {@link SizeOf#fullSizeOf}, or, if the agent fails (it runs out of memory, or its identity
     * map of visited objects reaches its maximum capacity of about 2^29 objects), the sum of the full sizes of its
     * sub-learners (the {@link Classifier}s it references, each measured the same way) and the shallow
     * sizes of its other objects. Objects shared by several sub-learners (e.g., the stream header) are then counted
     * once per sub-learner, which adds a few kilobytes to a model of gigabytes.
     */
    private static long modelSize(Object learner) {
        if (!measureInParts) {
            try {
                return SizeOf.fullSizeOf(learner);
            } catch (OutOfMemoryError | IllegalStateException e) {
                measureInParts = true;
                System.err.println("SizeOf failed (" + e + "); measuring " + learner.getClass().getName() + " in parts");
            }
        }
        List<Object> parts = new ArrayList<>();
        long size = shallowSizesUpTo(learner, parts);
        for (Object part : parts) {
            try {
                size += SizeOf.fullSizeOf(part);
            } catch (OutOfMemoryError | IllegalStateException e) {
                size += modelSize(part);
            }
        }
        return size;
    }

    /**
     * Sums the shallow sizes of the objects reachable from {@code root} without passing through another
     * {@link Classifier}, and collects those classifiers (each once) in {@code parts}.
     */
    private static long shallowSizesUpTo(Object root, List<Object> parts) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> seenParts = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(root);
        visited.add(root);
        long size = 0;
        while (!stack.isEmpty()) {
            Object o = stack.pop();
            size += SizeOf.sizeOf(o);
            List<Object> children = new ArrayList<>();
            Class<?> c = o.getClass();
            if (c.isArray()) {
                if (!c.getComponentType().isPrimitive())
                    for (int k = 0; k < Array.getLength(o); k++)
                        children.add(Array.get(o, k));
            } else {
                for (; c != null; c = c.getSuperclass())
                    for (Field f : c.getDeclaredFields()) {
                        if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive())
                            continue;
                        try {
                            f.setAccessible(true);
                            children.add(f.get(o));
                        } catch (RuntimeException | IllegalAccessException e) {
                            // a field of a module that is not opened: counted as a reference only
                        }
                    }
            }
            for (Object child : children) {
                if (child == null || child instanceof Class || child instanceof Enum || visited.contains(child))
                    continue;
                if (child instanceof Classifier) {
                    if (seenParts.add(child))
                        parts.add(child);
                    continue;
                }
                visited.add(child);
                stack.push(child);
            }
        }
        return size;
    }

    private static double measurement(BasicRegressionPerformanceEvaluator evaluator, String name) {
        for (Measurement m : evaluator.getPerformanceMeasurements())
            if (m.getName().equals(name))
                return m.getValue();
        return Double.NaN;
    }

    /**
     * Counts the data lines of an ARFF file (excluding comments and empty lines), to space the memory samples.
     */
    private static long countInstances(String arff) throws IOException {
        long n = 0;
        boolean data = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(arff))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("%"))
                    continue;
                if (data)
                    n++;
                else if (trimmed.toLowerCase(Locale.ROOT).startsWith("@data"))
                    data = true;
            }
        }
        return n;
    }
}
