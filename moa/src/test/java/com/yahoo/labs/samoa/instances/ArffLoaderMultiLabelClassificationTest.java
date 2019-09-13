package com.yahoo.labs.samoa.instances;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Multi-label classification test for ArffLoader
 *
 * @author Aljaž Osojnik (aljaz.osojnik@ijs.si)
 */

public class ArffLoaderMultiLabelClassificationTest {
	private static double EPS = 0.00000001;

	private static InstancesHeader instancesHeader;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			InputStream fileStream;
			Reader reader;

			fileStream = new FileInputStream(ClassLoader.getSystemResource("moa/classifiers/data/small_classification.arff").getPath());
			reader = new BufferedReader(new InputStreamReader(fileStream));
			instancesHeader = new InstancesHeader(reader, "1,2");
		} catch (IOException ioe) {
			throw new RuntimeException("ArffFileStream restart failed.", ioe);
		}
	}

	@Test
	public void testHeaderMultiLabelClassification() {
		// check number of attributes
		assertEquals(4, instancesHeader.arff.instanceInformation.numInputAttributes());
		assertEquals(2, instancesHeader.arff.instanceInformation.numOutputAttributes());
		
		// check attribute types for each position
		assertTrue(instancesHeader.arff.instanceInformation.attribute(0).isNominal());
		assertTrue(instancesHeader.arff.instanceInformation.attribute(1).isNominal());
		assertTrue(instancesHeader.arff.instanceInformation.attribute(2).isNominal);
		assertTrue(instancesHeader.arff.instanceInformation.attribute(3).isNumeric());
		assertTrue(instancesHeader.arff.instanceInformation.attribute(4).isNominal());
		assertTrue(instancesHeader.arff.instanceInformation.attribute(5).isNumeric());

		// check names
		assertTrue(instancesHeader.arff.instanceInformation.attribute(0).name.equals("C1"));
		assertTrue(instancesHeader.arff.instanceInformation.attribute(1).name.equals("C2"));
		assertTrue(instancesHeader.arff.instanceInformation.attribute(2).name.equals("N1"));
		assertTrue(instancesHeader.arff.instanceInformation.attribute(3).name.equals("R1"));
		assertTrue(instancesHeader.arff.instanceInformation.attribute(4).name.equals("N2"));
		assertTrue(instancesHeader.arff.instanceInformation.attribute(5).name.equals("R2"));
	}

	@Test
	public void testInstancesMultiLabelClassification() {
		// ---------------------- instance 1 ----------------------
		Instance instance = instancesHeader.arff.readInstance();
		instance.setDataset(instancesHeader);
		// check attributes
		assertEquals(0, instance.value(0), EPS);
		assertEquals(1, instance.value(1), EPS);
		assertEquals(0, instance.value(2), EPS);
		assertEquals(1.1, instance.value(3), EPS);
		assertEquals(2, instance.value(4), EPS);
		assertEquals(1.1, instance.value(5), EPS);

		// check input values
		assertEquals(0, instance.valueInputAttribute(0), EPS);
		assertEquals(1.1, instance.valueInputAttribute(1), EPS);
		assertEquals(2, instance.valueInputAttribute(2), EPS);
		assertEquals(1.1, instance.valueInputAttribute(3), EPS);
		// check output values
		assertEquals(0, instance.valueOutputAttribute(0), EPS);
		assertEquals(1, instance.valueOutputAttribute(1), EPS);

		// ---------------------- instance 2 ----------------------
		instance = instancesHeader.arff.readInstance();
		instance.setDataset(instancesHeader);
		// check attributes
		assertEquals(1, instance.value(0), EPS);
		assertEquals(0, instance.value(1), EPS);
		assertEquals(1, instance.value(2), EPS);
		assertEquals(1.2, instance.value(3), EPS);
		assertEquals(1, instance.value(4), EPS);
		assertEquals(2.2, instance.value(5), EPS);

		// check input values
		assertEquals(1, instance.valueInputAttribute(0), EPS);
		assertEquals(1.2, instance.valueInputAttribute(1), EPS);
		assertEquals(1, instance.valueInputAttribute(2), EPS);
		assertEquals(2.2, instance.valueInputAttribute(3), EPS);
		// check output values
		assertEquals(1, instance.valueOutputAttribute(0), EPS);
		assertEquals(0, instance.valueOutputAttribute(1), EPS);

		// ---------------------- instance 3 ----------------------
		instance = instancesHeader.arff.readInstance();
		instance.setDataset(instancesHeader);
		// check attributes
		assertEquals(0, instance.value(0), EPS);
		assertEquals(0, instance.value(1), EPS);
		assertEquals(2, instance.value(2), EPS);
		assertEquals(1.3, instance.value(3), EPS);
		assertEquals(0, instance.value(4), EPS);
		assertEquals(3.3, instance.value(5), EPS);

		// check input values
		assertEquals(2, instance.valueInputAttribute(0), EPS);
		assertEquals(1.3, instance.valueInputAttribute(1), EPS);
		assertEquals(0, instance.valueInputAttribute(2), EPS);
		assertEquals(3.3, instance.valueInputAttribute(3), EPS);

		// check output values
		assertEquals(0, instance.valueOutputAttribute(0), EPS);
		assertEquals(0, instance.valueOutputAttribute(1), EPS);
	}
}
