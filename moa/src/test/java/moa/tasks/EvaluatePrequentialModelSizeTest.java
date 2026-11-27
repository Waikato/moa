package moa.tasks;

import com.yahoo.labs.samoa.instances.Instance;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.Regressor;
import moa.core.Measurement;
import moa.evaluation.BasicClassificationPerformanceEvaluator;
import moa.evaluation.BasicRegressionPerformanceEvaluator;
import moa.evaluation.preview.LearningCurve;
import moa.streams.ArffFileStream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EvaluatePrequentialModelSizeTest {
    private static final long MODEL_BYTES = 1L << 30;

    @Test
    public void reusesSizeAtPeriodicAndFinalCheckpoints() throws Exception {
        CountingRegressor learner = new CountingRegressor();
        LearningCurve curve = evaluate(learner);

        assertEquals(3, curve.numEntries());
        assertEquals(3, learner.sizeCalls);
        assertEquals(5.0, curve.getMeasurement(2, 0), 0.0);
        int bytes = measurementIndex(curve, "model serialized size (bytes)");
        int ramHours = measurementIndex(curve, "model cost (RAM-Hours)");
        assertTrue(curve.getMeasurementName(1).startsWith("evaluation time ("));
        for (int index = 0; index < curve.numEntries(); index++) {
            assertEquals(MODEL_BYTES, curve.getMeasurement(index, bytes), 0.0);
            assertEquals(curve.getMeasurement(index, 1) / 3600.0,
                    curve.getMeasurement(index, ramHours), 1e-12);
        }
    }

    @Test
    public void measuresSizeWhenLearnerOmitsTheSizeColumn() throws Exception {
        CountingRegressor learner = new CountingRegressor() {
            @Override
            public Measurement[] getModelMeasurements() {
                return new Measurement[0];
            }
        };
        LearningCurve curve = evaluate(learner);

        assertEquals(3, learner.sizeCalls);
        assertEquals(-1, measurementIndex(curve, "model serialized size (bytes)"));
        int ramHours = measurementIndex(curve, "model cost (RAM-Hours)");
        assertEquals(curve.getMeasurement(2, 1) / 3600.0,
                curve.getMeasurement(2, ramHours), 1e-12);
    }

    @Test
    public void savesCheckpointsWhenModelSizeIsUnavailable() throws Exception {
        CountingRegressor learner = new CountingRegressor() {
            @Override
            public long measureByteSize() {
                return -1;
            }
        };
        LearningCurve curve = evaluate(learner);

        assertEquals(3, curve.numEntries());
        assertEquals(5.0, curve.getMeasurement(2, 0), 0.0);
        int bytes = measurementIndex(curve, "model serialized size (bytes)");
        int ramHours = measurementIndex(curve, "model cost (RAM-Hours)");
        for (int index = 0; index < curve.numEntries(); index++) {
            assertEquals(-1.0, curve.getMeasurement(index, bytes), 0.0);
            assertTrue(Double.isNaN(curve.getMeasurement(index, ramHours)));
        }
    }

    @Test
    public void marksMixedSubmodelSizesUnavailable() throws Exception {
        CountingRegressor first = new CountingRegressor();
        CountingRegressor second = new CountingRegressor() {
            @Override
            public long measureByteSize() {
                return -1;
            }
        };
        CountingRegressor learner = new CountingRegressor() {
            @Override
            public Classifier[] getSubClassifiers() {
                return new Classifier[] {first, second};
            }
        };
        LearningCurve curve = evaluate(learner);
        int bytes = measurementIndex(curve, "model serialized size (bytes)");
        int avgBytes = measurementIndex(curve, "[avg] model serialized size (bytes)");
        int errBytes = measurementIndex(curve, "[err] model serialized size (bytes)");
        assertEquals(MODEL_BYTES, curve.getMeasurement(2, bytes), 0.0);
        assertEquals(-1.0, curve.getMeasurement(2, avgBytes), 0.0);
        assertEquals(-1.0, curve.getMeasurement(2, errBytes), 0.0);
    }

    @Test
    public void classificationReusesSizeAtPeriodicAndFinalCheckpoints() throws Exception {
        CountingRegressor learner = new CountingClassifier();
        Path file = Files.createTempFile("moa-classification-size", ".arff");
        try {
            Files.write(file, ("@relation sample\n@attribute x numeric\n@attribute y {a,b}\n"
                    + "@data\n1,a\n2,b\n3,a\n4,b\n5,a\n").getBytes(StandardCharsets.UTF_8));
            EvaluatePrequential task = new EvaluatePrequential();
            task.getOptions();
            task.learnerOption.setCurrentObject(learner);
            task.streamOption.setCurrentObject(new ArffFileStream(file.toString(), -1));
            task.evaluatorOption.setCurrentObject(new BasicClassificationPerformanceEvaluator());
            task.sampleFrequencyOption.setValue(2);
            LearningCurve curve = (LearningCurve) task.doTask();
            assertEquals(3, curve.numEntries());
            assertEquals(3, learner.sizeCalls);
            assertEquals(MODEL_BYTES, curve.getMeasurement(2,
                    measurementIndex(curve, "model serialized size (bytes)")), 0.0);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static LearningCurve evaluate(CountingRegressor learner) throws Exception {
        Path file = Files.createTempFile("moa-regression-size", ".arff");
        try {
            Files.write(file, ("@relation sample\n@attribute x numeric\n@attribute y numeric\n"
                    + "@data\n1,1\n2,2\n3,3\n4,4\n5,5\n").getBytes(StandardCharsets.UTF_8));
            EvaluatePrequentialRegression task = new EvaluatePrequentialRegression();
            task.getOptions();
            task.learnerOption.setCurrentObject(learner);
            task.streamOption.setCurrentObject(new ArffFileStream(file.toString(), -1));
            task.evaluatorOption.setCurrentObject(new BasicRegressionPerformanceEvaluator());
            task.sampleFrequencyOption.setValue(2);
            return (LearningCurve) task.doTask();
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static int measurementIndex(LearningCurve curve, String name) {
        for (int i = 0; i < curve.getMeasurementNameCount(); i++) {
            if (name.equals(curve.getMeasurementName(i))) {
                return i;
            }
        }
        return -1;
    }

    public static class CountingRegressor extends AbstractClassifier implements Regressor {
        private static final long serialVersionUID = 1L;
        int sizeCalls;

        @Override public boolean isRandomizable() { return false; }
        @Override public void resetLearningImpl() { sizeCalls = 0; }
        @Override public void trainOnInstanceImpl(Instance instance) { }
        @Override public double[] getVotesForInstance(Instance instance) { return new double[] {0}; }
        @Override protected Measurement[] getModelMeasurementsImpl() { return new Measurement[0]; }
        @Override public void getModelDescription(StringBuilder out, int indent) { }

        @Override
        public long measureByteSize() {
            sizeCalls++;
            return MODEL_BYTES;
        }
    }

    private static final class CountingClassifier extends CountingRegressor implements MultiClassClassifier {
        private static final long serialVersionUID = 1L;

        @Override
        public double[] getVotesForInstance(Instance instance) {
            return new double[] {1, 0};
        }
    }

}
