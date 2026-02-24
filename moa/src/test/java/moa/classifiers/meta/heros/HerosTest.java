package moa.classifiers.meta.heros;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultipleClassifierTestCase;
import moa.classifiers.Classifier;

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
        Heros HTest = new Heros();
        HTest.dynamicResourceCosts.setValue(false);
        HTest.aggregationOption.setValue(1);
        HTest.numInstancesToTrainAllModelsOption.setValue(100);
        HTest.policyOption.setValueViaCLIString("ZetaPolicy -e 0.0");

        return new Classifier[] {
                HTest,
        };
    }

    public static Test suite() {
        return new TestSuite(HerosTest.class);
    }

    public static void main(String[] args) {
        runTest(suite());
    }
}
