/*
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instances;
import java.util.ArrayList;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit test suite for {@link PerGroupMahalanobisDistance} verifying empty/mismatched
 * input guards, zero distance for identical distributions, and analytical
 * pooled-covariance Mahalanobis distance.
 */
public class PerGroupMahalanobisDistanceTest extends TestCase {

    public PerGroupMahalanobisDistanceTest(String name) {
        super(name);
    }

    private static Instances buildInstances(double[][] rows, int numAttrs) {
        ArrayList<Attribute> attrs = new ArrayList<Attribute>(numAttrs);
        for (int i = 0; i < numAttrs; i++) {
            attrs.add(new Attribute("a" + i));
        }
        Instances insts = new Instances("test", attrs, rows.length);
        for (double[] row : rows) {
            DenseInstance inst = new DenseInstance(1.0, row);
            inst.setDataset(insts);
            insts.add(inst);
        }
        return insts;
    }

    public void testEmptyAndDimensionMismatchReturnMinusOne() {
        PerGroupMahalanobisDistance metric = new PerGroupMahalanobisDistance();
        Instances empty = buildInstances(new double[0][0], 2);
        Instances twoDim = buildInstances(new double[][] { { 0.0, 0.0 }, { 1.0, 1.0 } }, 2);
        Instances threeDim = buildInstances(new double[][] { { 0.0, 0.0, 0.0 }, { 1.0, 1.0, 1.0 } }, 3);

        assertEquals(-1.0, metric.distance(empty, twoDim), 0.0);
        assertEquals(-1.0, metric.distance(twoDim, empty), 0.0);
        assertEquals(-1.0, metric.distance(twoDim, threeDim), 0.0);
    }

    public void testIdenticalDistributionsHaveZeroDistance() {
        PerGroupMahalanobisDistance metric = new PerGroupMahalanobisDistance();
        double[][] data = {
            { 1.0, 2.0 },
            { 3.0, 4.0 },
            { 1.0, 4.0 },
            { 3.0, 2.0 }
        };
        Instances a = buildInstances(data, 2);
        Instances b = buildInstances(data, 2);
        assertEquals(0.0, metric.distance(a, b), 1e-12);
    }

    public void testAnalyticalPooledCovarianceDistance() {
        PerGroupMahalanobisDistance metric = new PerGroupMahalanobisDistance();
        // Group A centered at (0, 0) with independent axes: var_x = 1.0, var_y = 4.0
        Instances a = buildInstances(new double[][] {
            { -1.0, -2.0 },
            {  1.0, -2.0 },
            { -1.0,  2.0 },
            {  1.0,  2.0 }
        }, 2);
        // Group B centered at (3, 8) with identical covariance: var_x = 1.0, var_y = 4.0
        Instances b = buildInstances(new double[][] {
            { 2.0,  6.0 },
            { 4.0,  6.0 },
            { 2.0, 10.0 },
            { 4.0, 10.0 }
        }, 2);
        // Pooled cov = diag(1, 4), meanDiff = (-3, -8) -> sqrt(9/1 + 64/4) = sqrt(25) = 5.0
        assertEquals(5.0, metric.distance(a, b), 1e-12);
    }

    public static Test suite() {
        return new TestSuite(PerGroupMahalanobisDistanceTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
