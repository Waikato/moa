/*
 *   StreamingRandomPatchesTest.java
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
package moa.classifiers.deeplearning;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultipleClassifierTestCase;
import moa.classifiers.Classifier;

/**
 * Tests the MLP classifier.
 *
 * @author  Nuwan Gunasekara (ng98 at students dot waikato dot ac dot nz)
 * @version $Revision$
 */
public class MLPTest
		extends AbstractMultipleClassifierTestCase {

	/**
	 * Constructs the test case. Called by subclasses.
	 *
	 * @param name 	the name of the test
	 */
	public MLPTest(String name) {
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
		MLP MLPtest = new MLP();
        MLPtest.learningRateOption.setValue(0.03);
        MLPtest.backPropLossThreshold.setValue(0.3);
        MLPtest.optimizerTypeOption.setChosenIndex(5);
		MLPtest.useOneHotEncode.setValue(true);
		MLPtest.useNormalization.setValue(true);
		MLPtest.numberOfNeuronsInEachLayerInLog2.setValue(9);
		MLPtest.numberOfLayers.setValue(1);
		MLPtest.miniBatchSize.setValue(4);
		MLPtest.deviceTypeOption.setChosenIndex(1);

		return new Classifier[]{
				MLPtest,
		};
	}

	/**
	 * Returns a test suite.
	 *
	 * @return		the test suite
	 */
	public static Test suite() {
		return new TestSuite(MLPTest.class);
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