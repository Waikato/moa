/*
 *    MultiTargetLearnerSemiSupervised.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author R. Sousa, J. Gama
 *
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
 *
 *
 */

package moa.classifiers.rules.multilabel;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultiTargetRegressorTestCase;
import moa.learners.MultiTargetRegressor;


/**
 * Created by RSousa on 04/07/2017.
 */
public class AMRulesMultiTargetRegressorSemiSuperTest
        extends AbstractMultiTargetRegressorTestCase {

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
    protected MultiTargetRegressor[] getLearnerSetups() {
        return new MultiTargetRegressor[]{
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

