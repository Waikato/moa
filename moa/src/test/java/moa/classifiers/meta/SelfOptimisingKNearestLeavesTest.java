/*
 *   SelfOptimisingKNearestLeavesTest.java
 *
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
 * 
 */
package moa.classifiers.meta;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultipleClassifierTestCase;
import moa.classifiers.Classifier;

/**
 * Tests the SelfOptimisingKNearestLeaves classifier.
 * 
 * @author  Yibin Sun (ys388 at students dot waikato dot ac dot nz)
 * @version $Revision$
 */
public class SelfOptimisingKNearestLeavesTest
  extends AbstractMultipleClassifierTestCase {

  /**
   * Constructs the test case. Called by subclasses.
   *
   * @param name 	the name of the test
   */
  public SelfOptimisingKNearestLeavesTest(String name) {
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
    SelfOptimisingKNearestLeaves SOKNLTest = new SelfOptimisingKNearestLeaves();
    SOKNLTest.ensembleSizeOption.setValue(5);
    SOKNLTest.mFeaturesModeOption.setChosenIndex(0);
    SOKNLTest.mFeaturesPerTreeSizeOption.setValue(2);
    SOKNLTest.DisableSelfOptimisingOption.set();
    SOKNLTest.kOption.setValue(5);


    return new Classifier[]{
            SOKNLTest,
    };
  }
  
  /**
   * Returns a test suite.
   *
   * @return		the test suite
   */
  public static Test suite() {
    return new TestSuite(SelfOptimisingKNearestLeavesTest.class);
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
