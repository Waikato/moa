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
 * DynamicWeightedMajorityTest.java
 * Copyright (C) 2016 Instituto Federal de Pernambuco, Recife, Brazil
 */
package moa.classifiers.meta;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultipleClassifierTestCase;
import moa.classifiers.Classifier;
import static moa.test.MoaTestCase.runTest;

/**
 * Tests the Dynamic Weighted Majority classifier.
 *
 * @author Paulo Gonçalves (paulogoncalves at recife dot ifpe dot edu dot br)
 * @version $Revision$
 */
public class DynamicWeightedMajorityTest
        extends AbstractMultipleClassifierTestCase {

    /**
     * Constructs the test case. Called by subclasses.
     *
     * @param name the name of the test
     */
    public DynamicWeightedMajorityTest(String name) {
        super(name);
        this.setNumberTests(1);
    }

    /**
     * Returns the classifier setups to use in the regression test.
     *
     * @return	the setups
     */
    @Override
    protected Classifier[] getRegressionClassifierSetups() {
        return new Classifier[]{
            new DynamicWeightedMajority(),};
    }

    /**
     * Returns a test suite.
     *
     * @return	the test suite
     */
    public static Test suite() {
        return new TestSuite(DynamicWeightedMajorityTest.class);
    }

    /**
     * Runs the test from command line.
     *
     * @param args	ignored
     */
    public static void main(String[] args) {
        runTest(suite());
    }
}
