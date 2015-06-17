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
 * AbstractClassifierTestCase.java
 * Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */
package moa.classifiers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;

import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.Measurement;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.test.AbstractTestHelper;
import moa.test.MoaTestCase;
import moa.test.TestHelper;
import moa.test.TmpFile;
import weka.core.MOAUtils;

import com.yahoo.labs.samoa.instances.ArffLoader;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.Range;

/**
 * Ancestor for all classifier test cases.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public abstract class AbstractClassifierTestCase
extends MoaTestCase {

	/**
	 * Container for the data collected from a classifier at a specified
	 * inspection point in the stream.
	 * 
	 * @author  fracpete (fracpete at waikato dot ac dot nz)
	 * @version $Revision$
	 */
	public static class InspectionData {

		/** the inspection point. */
		public int index = -1;

		/** the votes. */
		public double[] votes = new double[0];

		/** the measurements. */
		public Measurement[] measurements = new Measurement[0];

		/** the model measurements. */
		public Measurement[] modelMeasurements = new Measurement[0];

		/**
		 * Returns the container data as string.
		 * 
		 * @return		the string representation
		 */
		@Override
		public String toString() {
			StringBuilder	result;
			int		i;

			result = new StringBuilder();

			result.append("Index\n");
			result.append("  " + index + "\n");

			result.append("Votes\n");
			for (i = 0; i < votes.length; i++)
				result.append("  " + i + ": " + MoaTestCase.doubleToString(votes[i], 8) + "\n");

			result.append("Measurements\n");
			for (Measurement m: measurements)
				result.append("  " + m.getName() + ": " + MoaTestCase.doubleToString(m.getValue(), 8) + "\n");

			result.append("Model measurements\n");
			for (Measurement m: modelMeasurements) {
				if (m.getName().indexOf("serialized") > -1)
					continue;
				result.append("  " + m.getName() + ": " + MoaTestCase.doubleToString(m.getValue(), 8) + "\n");
			}

			return result.toString();
		}
	}

	/**
	 * Constructs the test case. Called by subclasses.
	 *
	 * @param name 	the name of the test
	 */
	public AbstractClassifierTestCase(String name) {
		super(name);
	}

	/**
	 * Returns the test helper class to use.
	 *
	 * @return		the helper class instance
	 */
	@Override
	protected AbstractTestHelper newTestHelper() {
		return new TestHelper(this, "moa/classifiers/data");
	}

	/**
	 * Loads the data to process.
	 *
	 * @param filename	the filename to load (without path)
	 * @param classIndex	the class index to use
	 * @return		the data, null if it could not be loaded
	 * @see		#getDataDirectory()
	 */
	protected Instances load(String filename, int classIndex) {
		Instances	result = null;
		//ArffLoader 	loader;

		//result = null;

		try {
			/*loader = new ArffLoader();
      loader.setFile(new TmpFile(filename));
      result = loader.getDataSet();*/ // JD: weka's ARffLoader
			TmpFile tmp=new TmpFile(filename);
			FileInputStream fileStream = new FileInputStream(tmp.getAbsolutePath());
			Reader reader=new BufferedReader(new InputStreamReader(fileStream));
			Range range = new Range("-1");
			result = new Instances(reader,range);
			result.setClassIndex(classIndex);
			while (result.readInstance(null));
		}
		catch (Exception e) {
			System.err.println("Failed to load dataset: " + filename);
			e.printStackTrace();
			result = null;
		}

		return result;
	}

	/**
	 * Processes the input data and returns the inspection data.
	 *
	 * @param data		the data to work on
	 * @param inspectionPoints	the inspection points
	 * @param evaluator		the evaluator to use
	 * @param scheme		the scheme to process the data with
	 * @return			the processed data
	 */
	protected InspectionData[] inspect(Instances data, int[] inspectionPoints, LearningPerformanceEvaluator<Example<Instance>>  evaluator, Classifier scheme) {
		InspectionData[]	result;
		int			i;
		int			point;
		Instance		inst;
		double[]		votes;

		result = new InspectionData[inspectionPoints.length];

		scheme.prepareForUse();

		point = 0;
		for (i = 0; i < data.numInstances(); i++) {
			inst = data.instance(i);
			if (i > 0) {
				votes = scheme.getVotesForInstance(inst);

				evaluator.addResult((Example<Instance>)new InstanceExample(inst), votes);

				if (point < inspectionPoints.length) {
					if (i == inspectionPoints[point] - 1) {
						result[point]                   = new InspectionData();
						result[point].index             = inspectionPoints[point];
						result[point].votes             = votes;
						result[point].measurements      = evaluator.getPerformanceMeasurements();
						result[point].modelMeasurements = scheme.getModelMeasurements();
						point++;
					}
				}
			}

			scheme.trainOnInstance(inst);
		}

		return result;
	}

	/**
	 * Saves the data in the tmp directory.
	 *
	 * @param data	the data to save
	 * @param filename	the filename to save to (without path)
	 * @return		true if successfully saved
	 */
	protected boolean save(Classifier cls, InspectionData[] data, String filename) {
		StringBuilder	str;

		str = new StringBuilder();

		str.append(MOAUtils.toCommandLine(cls) + "\n");
		str.append("\n");

		for (InspectionData d: data) {
			str.append(d.toString());
			str.append("\n");
		}

		return m_TestHelper.save(str, filename);
	}

	/**
	 * Returns the filenames (without path) of the input data files to use
	 * in the regression test.
	 *
	 * @return		the filenames
	 */
	protected abstract String[] getRegressionInputFiles();

	/**
	 * Returns the class index for the datasets.
	 * 
	 * @return		the class indices (0-based)
	 */
	protected abstract int[] getRegressionInputClassIndex();

	/**
	 * Returns the index of the instances in the stream to inspect the 
	 * performance/classification output of the classifiers.
	 * 
	 * @return		the inspection indices
	 */
	protected abstract int[][] getRegressionInspectionPoints();

	/**
	 * Returns the classifier setups to use in the regression test.
	 *
	 * @return		the setups
	 */
	protected abstract Classifier[] getRegressionClassifierSetups();

	/**
	 * Returns the evaluator setups to use in the regression test.
	 *
	 * @return		the setups
	 */
	protected abstract LearningPerformanceEvaluator<Example<Instance>> [] getRegressionEvaluatorSetups();

	/**
	 * Creates an output filename based on the input filename.
	 *
	 * @param input	the input filename (no path)
	 * @param no		the number of the test
	 * @return		the generated output filename (no path)
	 */
	protected String createOutputFilename(String input, int no) {
		String	result;
		int		index;
		String	ext;

		ext = "-out" + no;

		index = input.lastIndexOf('.');
		if (index == -1) {
			result = input + ext;
		}
		else {
			result  = input.substring(0, index);
			result += ext;
			result += input.substring(index);
		}

		return result;
	}

	/**
	 * Compares the processed data against previously saved output data.
	 */
	public void testRegression() {
		Instances					data;
		InspectionData[]				processed;
		boolean					ok;
		String					regression;
		int						i;
		String[]					input;
		int[]					cindices;
		Classifier[]				setups;
		LearningPerformanceEvaluator<Example<Instance>> []	evals;
		int[][]					points;
		Classifier					current;
		String[]					output;
		TmpFile[]					outputFiles;

		if (m_NoRegressionTest)
			return;

		input    = getRegressionInputFiles();
		cindices = getRegressionInputClassIndex();
		output   = new String[input.length];
		setups   = getRegressionClassifierSetups();
		evals    = getRegressionEvaluatorSetups();
		points   = getRegressionInspectionPoints();
		assertEquals("Number of files and class indices differ!", input.length, cindices.length);
		assertEquals("Number of files and classifier setups differ!", input.length, setups.length);
		assertEquals("Number of classifier setups and evaluator setups differ!", setups.length, evals.length);
		assertEquals("Number of classifier setups and inspection points differ!", setups.length, points.length);

		// process data
		for (i = 0; i < input.length; i++) {
			data = load(input[i], cindices[i]);
			assertNotNull("Could not load data for regression test from " + input[i], data);

			current = setups[i].copy();
			current.prepareForUse();
			current.setModelContext(new InstancesHeader(data));
			assertNotNull("Failed to create copy of algorithm: " + MOAUtils.toCommandLine(setups[i]), current);

			processed = inspect(data, points[i], evals[i], current);
			assertNotNull("Failed to process data?", processed);

			output[i] = createOutputFilename(input[i], i);
			ok        = save(current, processed, output[i]);
			assertTrue("Failed to save regression data?", ok);
		}

		// test regression
		outputFiles = new TmpFile[output.length];
		for (i = 0; i < output.length; i++)
			outputFiles[i] = new TmpFile(output[i]);
		regression = m_Regression.compare(outputFiles);
		assertNull("Output differs:\n" + regression, regression);

		// remove output
		for (i = 0; i < output.length; i++)
			m_TestHelper.deleteFileFromTmp(output[i]);
	}
}
