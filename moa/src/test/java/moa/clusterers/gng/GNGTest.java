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

package moa.clusterers.gng;

import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit test suite for {@link GNG} (Fritzke 1995 topology growth, undirected
 * edge pruning, isolated unit removal, deep cloning, and serialization).
 */
public class GNGTest extends TestCase {

    public GNGTest(String name) {
        super(name);
    }

    private static Instance makeInstance(double x, double y, double cls) {
        return new DenseInstance(1.0, new double[] { x, y, cls });
    }

    public void testGrowthAndPruning() {
        GNG gng = new GNG();
        gng.lambdaOption.setValue(5);
        gng.maxAgeOption.setValue(10);
        gng.stoppingCriteriaOption.setValue(50);
        gng.classAsAttributeOption.set();
        gng.setKeepReceivedPatterns(false);
        gng.resetLearning();

        for (int i = 0; i < 60; i++) {
            double x = (i % 2 == 0) ? 0.1 * (i % 5) : 10.0 + 0.1 * (i % 5);
            double y = (i % 3) * 0.2;
            gng.trainOnInstanceImpl(makeInstance(x, y, i % 2));
        }

        assertEquals(60L, gng.getInstancesSeen());
        assertTrue("Expected prototypes to be created", gng.getNumberOfPrototypesCreated() > 0);
        assertTrue("Expected at least 2 active units", gng.getS().size() >= 2);

        for (GUnit u : gng.getS()) {
            assertTrue("No isolated unit should remain in S", u.neighbors.size() > 0);
            for (GEdge e : u.neighbors) {
                GUnit other = (e.p0 == u) ? e.p1 : e.p0;
                assertTrue("Edge must exist on both endpoints", other.neighbors.contains(e));
            }
        }
    }

    public void testDeepCloneIndependence() {
        GNG gng = new GNG();
        gng.lambdaOption.setValue(5);
        gng.maxAgeOption.setValue(20);
        gng.classAsAttributeOption.set();
        gng.resetLearning();

        for (int i = 0; i < 20; i++) {
            gng.trainOnInstanceImpl(makeInstance(i * 0.5, i * 0.25, i % 2));
        }

        GNG copy = gng.clone();
        assertNotSame(gng, copy);
        assertEquals(gng.getS().size(), copy.getS().size());
        for (int i = 0; i < gng.getS().size(); i++) {
            assertNotSame(gng.getS().get(i), copy.getS().get(i));
            assertNotSame(gng.getS().get(i).w, copy.getS().get(i).w);
        }

        double before = copy.getS().get(0).w[0];
        gng.getS().get(0).w[0] += 999.0;
        assertEquals(before, copy.getS().get(0).w[0], 0.0);
    }

    public void testSerializationRoundTrip() throws Exception {
        GNG gng = new GNG();
        gng.lambdaOption.setValue(5);
        gng.classAsAttributeOption.set();
        gng.resetLearning();
        for (int i = 0; i < 15; i++) {
            gng.trainOnInstanceImpl(makeInstance(i * 0.2, i * 0.4, i % 2));
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(gng);
        }
        GNG restored;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            restored = (GNG) ois.readObject();
        }
        assertEquals(gng.getInstancesSeen(), restored.getInstancesSeen());
        assertEquals(gng.getS().size(), restored.getS().size());
    }

    public static Test suite() {
        return new TestSuite(GNGTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
