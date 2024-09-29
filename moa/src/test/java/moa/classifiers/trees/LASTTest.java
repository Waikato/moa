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
 * LASTTest.java
 * Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */
package moa.classifiers.trees;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultipleClassifierTestCase;
import moa.classifiers.Classifier;

/**
 * Tests the HoeffdingTree classifier.
 * 
 * @author  Daniel Nowak Assis (daniel dot nowak dot assis at gmail dot com)
 * @version $Revision 1$ 
 */
public class LASTTest
  extends AbstractMultipleClassifierTestCase {

  /**
   * Constructs the test case. Called by subclasses.
   *
   * @param name 	the name of the test
   */
  public LASTTest(String name) {
    super(name);
    this.setNumberTests(2);
  }

  
  /**
   * Returns the classifier setups to use in the regression test.
   *
   * @return		the setups
   */
  @Override
  protected Classifier[] getRegressionClassifierSetups() {
    LAST[]	result;
    result    = new LAST[2];
    result[0] = new LAST();
    result[1] = new LAST();
    result[0].changeDetectionMethodOption.setValueViaCLIString("RDDM -y 300");
    	
    //result[1].distributionOption.setValue(true);
    return result;
  }
  
  /**
   * Returns a test suite.
   *
   * @return		the test suite
   */
  public static Test suite() {
    return new TestSuite(LASTTest.class);
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
