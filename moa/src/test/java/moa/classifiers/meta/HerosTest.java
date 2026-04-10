package moa.classifiers.meta;

import org.junit.Ignore;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultipleClassifierTestCase;
import moa.classifiers.Classifier;
import moa.classifiers.meta.heros.Heros;

/**
 * Tests the Heros classifier.
 */
@Ignore("TODO test is platform specific and fails on GitHub due to DJL.")
public class HerosTest extends AbstractMultipleClassifierTestCase {
    public HerosTest(String name) {
        super(name);
        this.setNumberTests(1);
    }

    @Override
    protected Classifier[] getRegressionClassifierSetups() {
        return new Classifier[] { new Heros(), };
    }

    public static Test suite() {
        return new TestSuite(HerosTest.class);
    }

    public static void main(String[] args) {
        runTest(suite());
    }
}
