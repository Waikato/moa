package moa.evaluation;

import static org.junit.Assert.assertEquals;

import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.Measurement;
import org.junit.Test;

public class WindowClassificationPerformanceEvaluatorTest {

    private static final double[] ABSTAIN = {0, 0};
    private static final double[] PREDICT = {0.2, 0.8};

    private static double measurement(WindowClassificationPerformanceEvaluator evaluator, String name) {
        for (Measurement m : evaluator.getPerformanceMeasurements()) {
            if (m.getName().equals(name)) {
                return m.getValue();
            }
        }
        throw new AssertionError("missing measurement " + name);
    }

    private static WindowClassificationPerformanceEvaluator evaluator(int width) {
        WindowClassificationPerformanceEvaluator evaluator = new WindowClassificationPerformanceEvaluator();
        evaluator.abstentionsOption.set();
        evaluator.widthOption.setValue(width);
        evaluator.reset(2);
        return evaluator;
    }

    @Test
    public void countsTotalAndWindowAbstentions() {
        InstancesHeader header = BasicClassificationPerformanceEvaluatorTest.header();
        WindowClassificationPerformanceEvaluator evaluator = evaluator(2);
        evaluator.addResult(BasicClassificationPerformanceEvaluatorTest.example(header, 0), ABSTAIN);
        evaluator.addResult(BasicClassificationPerformanceEvaluatorTest.example(header, 1), ABSTAIN);
        assertEquals(2.0, measurement(evaluator, "abstentions"), 0.0);
        assertEquals(2.0, measurement(evaluator, "abstentions (window)"), 0.0);
    }

    @Test
    public void windowCountDropsOldAbstentions() {
        InstancesHeader header = BasicClassificationPerformanceEvaluatorTest.header();
        WindowClassificationPerformanceEvaluator evaluator = evaluator(2);
        evaluator.addResult(BasicClassificationPerformanceEvaluatorTest.example(header, 0), ABSTAIN);
        evaluator.addResult(BasicClassificationPerformanceEvaluatorTest.example(header, 1), PREDICT);
        evaluator.addResult(BasicClassificationPerformanceEvaluatorTest.example(header, 1), PREDICT);
        assertEquals(1.0, measurement(evaluator, "abstentions"), 0.0);
        assertEquals(0.0, measurement(evaluator, "abstentions (window)"), 0.0);
    }
}
