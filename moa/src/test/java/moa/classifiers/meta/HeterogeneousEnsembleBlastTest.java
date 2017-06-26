package moa.classifiers.meta;

import junit.framework.Test;
import junit.framework.TestSuite;
import moa.classifiers.AbstractMultipleClassifierTestCase;
import moa.classifiers.Classifier;

public class HeterogeneousEnsembleBlastTest
		extends AbstractMultipleClassifierTestCase {
	/**
	 * Constructs the test case. Called by subclasses.
	 *
	 * @param name
	 *          the name of the test
	 */
	public HeterogeneousEnsembleBlastTest(String name) {
		super(name);
		this.setNumberTests(2);
	}

	/**
	 * Returns the classifier setups to use in the regression test.
	 *
	 * @return the setups
	 */
	@Override
	protected Classifier[] getRegressionClassifierSetups() {
		return new Classifier[] { new HeterogeneousEnsembleBlast(),
				new HeterogeneousEnsembleBlastFadingFactors(), };
	}

	@org.junit.Test
	protected void testSetup() { // TODO: test case doesn't run
		// checks whether differently initialized ensembles are what they claim to
		// be.

		String[] ensembleComposition = { "trees.HoeffdingTree", "bayes.NaiveBayes",
				"functions.Perceptron" };

		HeterogeneousEnsembleBlastFadingFactors ff = new HeterogeneousEnsembleBlastFadingFactors();

		for (int i = 1; i < ensembleComposition.length; ++i) {
			// makes initialization string. There are more convenient functions in
			// various libraries
			String currentCLIString = ensembleComposition[0];
			for (int j = 0; j < i; ++j) {
				currentCLIString += "," + ensembleComposition[1];
			}
			ff.baselearnersOption.setValueViaCLIString(currentCLIString);
			ff.prepareForUse();
			assertEquals(ff.getEnsembleSize(), i + 1);
			for (int j = 0; j < i; ++j) {
				assertEquals(ff.getMemberCliString(j).trim(),
						ensembleComposition[j].trim());
			}

		}
	}

	/**
	 * Returns a test suite.
	 *
	 * @return the test suite
	 */
	public static Test suite() {
		return new TestSuite(HeterogeneousEnsembleBlastTest.class);
	}

	/**
	 * Runs the test from commandline.
	 *
	 * @param args
	 *          ignored
	 */
	public static void main(String[] args) {
		runTest(suite());
	}
}
