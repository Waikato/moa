package moa.classifiers.rules.multilabel;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultipleRegressorTestCase;
import moa.classifiers.Classifier;
import moa.classifiers.rules.AMRulesRegressor;
import moa.classifiers.rules.AMRulesRegressorTest;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultipleRegressorTestCase;
import moa.classifiers.Classifier;
import static moa.test.MoaTestCase.runTest;


/**
 * Created by RSousa on 04/07/2017.
 */
public class AMRulesMultiTargetRegressorSemiSuperTest
        extends AbstractMultipleRegressorTestCase {

    /**
     * Constructs the test case. Called by subclasses.
     *
     * @param name 	the name of the test
     */
    public AMRulesMultiTargetRegressorSemiSuperTest(String name) {
        super(name);
        this.setNumberTests(1);
    }

    /**
     * Returns the classifier setups to use in the regression test.
     *
     * @return		the setups
     */
    @Override
    protected Classifier[] getRegressionClassifierSetups() {
        return new Classifier[]{
                new AMRulesMultiTargetRegressorSemiSuper(),
        };
    }

    /**
     * Returns a test suite.
     *
     * @return		the test suite
     */
    public static Test suite() {
        return new TestSuite(moa.classifiers.rules.multilabel.AMRulesMultiTargetRegressorSemiSuperTest.class);
    }

    /**
     * Runs the test from commandline.
     *
     * @param args	ignored
     */
    public static void main(String[] args) {
        runTest(suite());
    }
}

