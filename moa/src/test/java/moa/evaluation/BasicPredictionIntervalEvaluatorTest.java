package moa.evaluation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.Measurement;

public class BasicPredictionIntervalEvaluatorTest {

    private static final double EPS = 1e-9;

    @Test
    public void testMetrics() {
        // Header: one numeric attribute and a numeric class.
        List<Attribute> atts = new ArrayList<Attribute>();
        atts.add(new Attribute("x"));
        atts.add(new Attribute("y"));
        Instances data = new Instances("test", atts, 0);
        data.setClassIndex(1);
        InstancesHeader header = new InstancesHeader(data);

        double[] y = {2, 4, 3, 8, 5};
        double[] point = {3, 3, 4, 6, 5};
        // Interval is [point - 1, point + 1], so lower != point.
        int n = y.length;

        BasicPredictionIntervalEvaluator ev = new BasicPredictionIntervalEvaluator();
        ev.reset();
        for (int i = 0; i < n; i++) {
            Instance inst = new DenseInstance(1.0, new double[]{0.0, y[i]});
            inst.setDataset(header);
            Example<Instance> ex = new InstanceExample(inst);
            ev.addResult(ex, new double[]{point[i] - 1, point[i], point[i] + 1});
        }

        double sumAbs = 0, sumSq = 0, sumAbsTarget = 0, sumY = 0;
        int inside = 0;
        for (int i = 0; i < n; i++) {
            sumAbs += Math.abs(y[i] - point[i]);
            sumSq += (y[i] - point[i]) * (y[i] - point[i]);
            // Mean of the earlier targets (0 for the first one).
            double m = i == 0 ? 0.0 : sumY / i;
            sumAbsTarget += Math.abs(y[i] - m);
            sumY += y[i];
            if (point[i] - 1 <= y[i] && y[i] <= point[i] + 1) inside++;
        }

        Measurement[] ms = ev.getPerformanceMeasurements();

        // Each instance counts once, not twice.
        assertEquals(n, ev.getTotalWeightObserved(), EPS);
        assertEquals(n, Measurement.getMeasurementNamed("classified instances", ms).getValue(), EPS);
        // MAE and RMSE score the point, not the lower bound.
        assertEquals(sumAbs / n, ev.getMeanError(), EPS);
        assertEquals(sumAbs / n, Measurement.getMeasurementNamed("mean absolute error", ms).getValue(), EPS);
        assertEquals(Math.sqrt(sumSq / n), Measurement.getMeasurementNamed("root mean squared error", ms).getValue(), EPS);
        // Coverage is in percent: 4 of 5 are inside.
        assertEquals(100.0 * inside / n, ev.getCoverage(), EPS);
        assertEquals(80.0, Measurement.getMeasurementNamed("coverage", ms).getValue(), EPS);
        // Relative MAE scores the point against the full running mean.
        assertEquals(sumAbs / sumAbsTarget,
                Measurement.getMeasurementNamed("relative mean absolute error", ms).getValue(), EPS);
    }
}
