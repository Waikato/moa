package moa.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import java.util.ArrayList;
import java.util.Arrays;
import moa.core.Example;
import moa.core.InstanceExample;
import org.junit.Test;

public class BasicClassificationPerformanceEvaluatorTest {

    /** Two-class stream header: one numeric attribute and the class. */
    static InstancesHeader header() {
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new Attribute("x"));
        attributes.add(new Attribute("class", Arrays.asList("a", "b")));
        Instances instances = new Instances("test", attributes, 0);
        instances.setClassIndex(1);
        return new InstancesHeader(instances);
    }

    static Example<Instance> example(InstancesHeader header, double trueClass) {
        Instance inst = new DenseInstance(1.0, new double[]{0.0, trueClass});
        inst.setDataset(header);
        return new InstanceExample(inst);
    }

    @Test
    public void equalVotesAreAbstentions() {
        assertTrue(BasicClassificationPerformanceEvaluator.isAbstention(new double[0], 2));
        assertTrue(BasicClassificationPerformanceEvaluator.isAbstention(new double[]{0, 0, 0}, 3));
        assertTrue(BasicClassificationPerformanceEvaluator.isAbstention(new double[]{0.5, 0.5}, 2));
        assertFalse(BasicClassificationPerformanceEvaluator.isAbstention(new double[]{0.4, 0.6}, 2));
    }

    @Test
    public void shortVotesArePaddedBeforeComparing() {
        // only class 0 seen: [n] is [n, 0], a confident prediction
        assertFalse(BasicClassificationPerformanceEvaluator.isAbstention(new double[]{5}, 2));
        assertFalse(BasicClassificationPerformanceEvaluator.isAbstention(new double[]{0, 3}, 3));
        assertTrue(BasicClassificationPerformanceEvaluator.isAbstention(new double[]{0}, 2));
        assertTrue(BasicClassificationPerformanceEvaluator.isAbstention(new double[]{1}, 1));
    }

    @Test
    public void countsAbstentionsInTotal() {
        InstancesHeader header = header();
        BasicClassificationPerformanceEvaluator evaluator = new BasicClassificationPerformanceEvaluator();
        evaluator.abstentionsOption.set();
        evaluator.reset(2);
        evaluator.addResult(example(header, 0), new double[]{0, 0});
        evaluator.addResult(example(header, 1), new double[]{0.2, 0.8});
        evaluator.addResult(example(header, 1), new double[]{1, 1});
        assertEquals(3.0, evaluator.getTotalWeightObserved(), 0.0);
        assertEquals(2.0, evaluator.getAbstentions(), 0.0);
    }

    @Test
    public void abstentionIsReportedAsMeasurement() {
        InstancesHeader header = header();
        BasicClassificationPerformanceEvaluator evaluator = new BasicClassificationPerformanceEvaluator();
        evaluator.abstentionsOption.set();
        evaluator.reset(2);
        evaluator.addResult(example(header, 0), new double[0]);
        double reported = Double.NaN;
        for (moa.core.Measurement m : evaluator.getPerformanceMeasurements()) {
            if (m.getName().equals("abstentions")) {
                reported = m.getValue();
            }
        }
        assertEquals(1.0, reported, 0.0);
    }

    @Test
    public void abstentionsAreNotCountedByDefault() {
        InstancesHeader header = header();
        BasicClassificationPerformanceEvaluator evaluator = new BasicClassificationPerformanceEvaluator();
        evaluator.reset(2);
        evaluator.addResult(example(header, 0), new double[]{0, 0});
        assertEquals(0.0, evaluator.getAbstentions(), 0.0);
        for (moa.core.Measurement m : evaluator.getPerformanceMeasurements()) {
            assertFalse(m.getName().startsWith("abstentions"));
        }
    }
}
