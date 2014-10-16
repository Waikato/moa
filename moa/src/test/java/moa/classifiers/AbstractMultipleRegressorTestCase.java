/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * AbstractMultipleRegressorTestCase.java Copyright (C) 2013 University of Waikato, Hamilton, New
 * Zealand
 */
package moa.classifiers;

import com.yahoo.labs.samoa.instances.Instance;

import moa.core.Example;
import moa.evaluation.BasicRegressionPerformanceEvaluator;
import moa.evaluation.ClassificationPerformanceEvaluator;
import moa.evaluation.LearningPerformanceEvaluator;

/**
 * Ancestor that defines a setting to test a classifier several times with
 * different parameters using this predefined same setting.
 *
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public abstract class AbstractMultipleRegressorTestCase
        extends AbstractClassifierTestCase {

    protected int numberTests = 1;

    /**
     * Constructs the test case. Called by subclasses.
     *
     * @param name the name of the test
     */
    public AbstractMultipleRegressorTestCase(String name) {
        super(name);
    }

    /**
     * Sets the number of tests to run with this classifier.
     *
     * @param name the name of the test
     * @param numberTests the numbers of tests to run
     */

    public void setNumberTests(int numberTests) {
        this.numberTests = numberTests;
    }

    /**
     * Called by JUnit before each test method.
     *
     * @throws Exception if an error occurs.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        m_TestHelper.copyResourceToTmp("regression.arff");
    }

    /**
     * Called by JUnit after each test method.
     *
     * @throws Exception	if tear-down fails
     */
    @Override
    protected void tearDown() throws Exception {
        m_TestHelper.deleteFileFromTmp("regression.arff");

        super.tearDown();
    }

    /**
     * Returns the filenames (without path) of the input data files to use in
     * the regression test.
     *
     * @return	the filenames
     */
    @Override
    protected String[] getRegressionInputFiles() {
        String value = "regression.arff";
        String[] ret = new String[this.numberTests];
        for (int i = 0; i < this.numberTests; i++) {
            ret[i] = value;
        }
        return ret;
    }

    /**
     * Returns the class index for the datasets.
     *
     * @return	the class indices (0-based)
     */
    @Override
    protected int[] getRegressionInputClassIndex() {
        int value = 8;
        int[] ret = new int[this.numberTests];
        for (int i = 0; i < this.numberTests; i++) {
            ret[i] = value;
        }
        return ret;
    }

    /**
     * Returns the index of the instances in the stream to inspect the
     * performance/classification output of the classifiers.
     *
     * @return	the inspection indices
     */
    @Override
    protected int[][] getRegressionInspectionPoints() {
        int[] value = new int[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
        int[][] ret = new int[this.numberTests][value.length];
        for (int i = 0; i < this.numberTests; i++) {
            ret[i] = value.clone();
        }
        return ret;
    }

    /**
     * Returns the classifier setups to use in the regression test.
     *
     * @return	the setups
     */
    @Override
    protected abstract Classifier[] getRegressionClassifierSetups();

    /**
     * Returns the evaluator setups to use in the regression test.
     *
     * @return	the setups
     */
    @Override
    protected LearningPerformanceEvaluator<Example<Instance>> [] getRegressionEvaluatorSetups() {
    	LearningPerformanceEvaluator<Example<Instance>>  value = new BasicRegressionPerformanceEvaluator();
    	LearningPerformanceEvaluator<Example<Instance>> [] ret = new BasicRegressionPerformanceEvaluator[this.numberTests];
        for (int i = 0; i < this.numberTests; i++) {
            ret[i] = (LearningPerformanceEvaluator<Example<Instance>> ) value.copy();
        }
        return ret;
    }

}
