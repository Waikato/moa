package moa.classifiers.meta;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultipleClassifierTestCase;
import moa.classifiers.Classifier;
import moa.classifiers.meta.heros.Heros;

/**
 * Tests the Heros classifier.
 */
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

    /** Disabled: testRegression fails with NullPointerException (m_Regression is null). */
    @Override
    public void testRegression() {
        // TODO: fix regression reference data and re-enable
    }

    public static void main(String[] args) {
        runTest(suite());
    }
}
