package com.yahoo.labs.samoa.instances;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import moa.core.InputStreamProgressMonitor;
import moa.streams.ArffFileStream;
import moa.streams.MultiTargetArffFileStream;
import moa.test.MoaTestCase;
import moa.test.TmpFile;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
/**
 * Test multilabel instances read from MultiTargetArffLoader
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 */
public class MultiTargetArffLoaderTest {
	private static double EPS=0.00000001;
	private static Instances instancesRegression;
	private static Instances instancesClassification;
	
	@BeforeClass
	public static void setUpBeforeClassRegression() throws Exception {
		try {
            InputStream fileStream = new FileInputStream(ClassLoader.getSystemResource("moa/classifiers/data/small_regression.arff").getPath());
            Reader  reader= new BufferedReader(new InputStreamReader(fileStream));
    		instancesRegression = new Instances(reader,new Range("4-6"));
    		fileStream = new FileInputStream(ClassLoader.getSystemResource("moa/classifiers/data/small_classification.arff").getPath());
    		reader = new BufferedReader(new InputStreamReader(fileStream));
    		instancesClassification = new Instances(reader,new Range("2"));
        } catch (IOException ioe) {
            throw new RuntimeException("ArffFileStream restart failed.", ioe);
        }
	}

	/*
	 * TESTS REGRESSION DATASET
	 */
	@Test
	public void testRangeRegression(){
		assertEquals(3,instancesRegression.arff.range.getStart());
		assertEquals(5,instancesRegression.arff.range.getEnd());
	}
	
	public void testClassRegression(){
		assertEquals(3,instancesRegression.arff.instanceInformation.numOutputAttributes());
		assertTrue(instancesRegression.arff.instanceInformation.outputAttribute(0).name.equals("R1"));
		assertTrue(instancesRegression.arff.instanceInformation.outputAttribute(1).name.equals("R2"));
		assertTrue(instancesRegression.arff.instanceInformation.outputAttribute(2).name.equals("R3"));
	}
	
	@Test
	public void testHeaderRegression(){
		assertEquals(8,instancesRegression.arff.instanceInformation.numAttributes());
		//check attribute types for each position
		assertTrue(instancesRegression.arff.instanceInformation.attribute(0).isNumeric());
		assertTrue(instancesRegression.arff.instanceInformation.attribute(1).isNumeric());
		assertTrue(instancesRegression.arff.instanceInformation.attribute(2).isNominal);
		assertTrue(instancesRegression.arff.instanceInformation.attribute(3).isNumeric());
		assertTrue(instancesRegression.arff.instanceInformation.attribute(4).isNumeric());
		assertTrue(instancesRegression.arff.instanceInformation.attribute(5).isNumeric());
		assertTrue(instancesRegression.arff.instanceInformation.attribute(6).isNumeric());
		assertTrue(instancesRegression.arff.instanceInformation.attribute(7).isNominal);
		
		//check names
		assertTrue(instancesRegression.arff.instanceInformation.attribute(0).name.equals("F1"));
		assertTrue(instancesRegression.arff.instanceInformation.attribute(1).name.equals("F2"));
		assertTrue(instancesRegression.arff.instanceInformation.attribute(2).name.equals("N1"));
		assertTrue(instancesRegression.arff.instanceInformation.attribute(3).name.equals("R1"));
		assertTrue(instancesRegression.arff.instanceInformation.attribute(4).name.equals("R2"));
		assertTrue(instancesRegression.arff.instanceInformation.attribute(5).name.equals("R3"));
		assertTrue(instancesRegression.arff.instanceInformation.attribute(6).name.equals("F3"));
		assertTrue(instancesRegression.arff.instanceInformation.attribute(7).name.equals("N2"));
	}
	@Test
	public void testInstancesRegression() {
		//instance 1
		Instance instance=instancesRegression.arff.readInstance();
		instance.setDataset(instancesRegression);
		//check attributes
		assertEquals(1.1,instance.value(0),EPS);
		assertEquals(-2.3,instance.value(1),EPS);
		assertEquals(2,instance.value(2),EPS);
		assertEquals(1,instance.value(3),EPS);
		assertEquals(2,instance.value(4),EPS);
		assertEquals(3,instance.value(5),EPS);
		assertEquals(3.3,instance.value(6),EPS);
		assertEquals(0,instance.value(7),EPS);
		//check input values
		assertEquals(1.1,instance.valueInputAttribute(0),EPS);
		assertEquals(-2.3,instance.valueInputAttribute(1),EPS);
		assertEquals(2,instance.valueInputAttribute(2),EPS);
		assertEquals(3.3,instance.valueInputAttribute(3),EPS);
		assertEquals(0,instance.valueInputAttribute(4),EPS);
		//check output values
		assertEquals(1,instance.valueOutputAttribute(0),EPS);
		assertEquals(2,instance.valueOutputAttribute(1),EPS);
		assertEquals(3,instance.valueOutputAttribute(2),EPS);	
		
		//instance 2
		instance=instancesRegression.arff.readInstance();
		instance.setDataset(instancesRegression);
		//check attributes
		assertEquals(1.2,instance.value(0),EPS);
		assertEquals(-2.2,instance.value(1),EPS);
		assertEquals(0,instance.value(2),EPS);
		assertEquals(3,instance.value(3),EPS);
		assertEquals(1,instance.value(4),EPS);
		assertEquals(2,instance.value(5),EPS);
		assertEquals(3.2,instance.value(6),EPS);
		assertEquals(0,instance.value(7),EPS);
		//check input values
		assertEquals(1.2,instance.valueInputAttribute(0),EPS);
		assertEquals(-2.2,instance.valueInputAttribute(1),EPS);
		assertEquals(0,instance.valueInputAttribute(2),EPS);
		assertEquals(3.2,instance.valueInputAttribute(3),EPS);
		assertEquals(0,instance.valueInputAttribute(4),EPS);
		//check output values
		assertEquals(3,instance.valueOutputAttribute(0),EPS);
		assertEquals(1,instance.valueOutputAttribute(1),EPS);
		assertEquals(2,instance.valueOutputAttribute(2),EPS);	
		
		//instance 3
		instance=instancesRegression.arff.readInstance();
		instance.setDataset(instancesRegression);
		//check attributes
		assertEquals(1.3,instance.value(0),EPS);
		assertEquals(-2.1,instance.value(1),EPS);
		assertEquals(1,instance.value(2),EPS);
		assertEquals(2,instance.value(3),EPS);
		assertEquals(3,instance.value(4),EPS);
		assertEquals(1,instance.value(5),EPS);
		assertEquals(3.1,instance.value(6),EPS);
		assertEquals(2,instance.value(7),EPS);
		//check input values
		assertEquals(1.3,instance.valueInputAttribute(0),EPS);
		assertEquals(-2.1,instance.valueInputAttribute(1),EPS);
		assertEquals(1,instance.valueInputAttribute(2),EPS);
		assertEquals(3.1,instance.valueInputAttribute(3),EPS);
		assertEquals(2,instance.valueInputAttribute(4),EPS);
		//check output values
		assertEquals(2,instance.valueOutputAttribute(0),EPS);
		assertEquals(3,instance.valueOutputAttribute(1),EPS);
		assertEquals(1,instance.valueOutputAttribute(2),EPS);	
	}

	/*
	 * TESTS REGRESSION DATASET
	 */

	@Test
	public void testRangeClassification(){
		assertEquals(0,instancesClassification.arff.range.getStart());
		assertEquals(1,instancesClassification.arff.range.getEnd());
	}
	
	public void testClassClassification(){
		assertEquals(2,instancesClassification.arff.instanceInformation.numOutputAttributes());
		assertTrue(instancesClassification.arff.instanceInformation.outputAttribute(0).name.equals("C1"));
		assertTrue(instancesClassification.arff.instanceInformation.outputAttribute(1).name.equals("C2"));
	}
	
	@Test
	public void testHeaderClassification(){
		assertEquals(6,instancesClassification.arff.instanceInformation.numAttributes());
		//check attribute types for each position
		assertTrue(instancesClassification.arff.instanceInformation.attribute(0).isNominal());
		assertTrue(instancesClassification.arff.instanceInformation.attribute(1).isNominal());
		assertTrue(instancesClassification.arff.instanceInformation.attribute(2).isNominal);
		assertTrue(instancesClassification.arff.instanceInformation.attribute(3).isNumeric());
		assertTrue(instancesClassification.arff.instanceInformation.attribute(4).isNominal());
		assertTrue(instancesClassification.arff.instanceInformation.attribute(5).isNumeric());
		
		//check names
		assertTrue(instancesClassification.arff.instanceInformation.attribute(0).name.equals("C1"));
		assertTrue(instancesClassification.arff.instanceInformation.attribute(1).name.equals("C2"));
		assertTrue(instancesClassification.arff.instanceInformation.attribute(2).name.equals("N1"));
		assertTrue(instancesClassification.arff.instanceInformation.attribute(3).name.equals("R1"));
		assertTrue(instancesClassification.arff.instanceInformation.attribute(4).name.equals("N2"));
		assertTrue(instancesClassification.arff.instanceInformation.attribute(5).name.equals("R2"));
	}
	@Test
	public void testInstancesClassification() {
		//instance 1
		Instance instance=instancesClassification.arff.readInstance();
		instance.setDataset(instancesClassification);
		//check attributes
		assertEquals(0,instance.value(0),EPS);
		assertEquals(1,instance.value(1),EPS);
		assertEquals(0,instance.value(2),EPS);
		assertEquals(1.1,instance.value(3),EPS);
		assertEquals(2,instance.value(4),EPS);
		assertEquals(1.1,instance.value(5),EPS);

		//check input values
		assertEquals(0,instance.valueInputAttribute(0),EPS);
		assertEquals(1.1,instance.valueInputAttribute(1),EPS);
		assertEquals(2,instance.valueInputAttribute(2),EPS);
		assertEquals(1.1,instance.valueInputAttribute(3),EPS);
		//check output values
		assertEquals(0,instance.valueOutputAttribute(0),EPS);
		assertEquals(1,instance.valueOutputAttribute(1),EPS);
		
		//instance 2
		instance=instancesClassification.arff.readInstance();
		instance.setDataset(instancesClassification);
		//check attributes
		assertEquals(1,instance.value(0),EPS);
		assertEquals(0,instance.value(1),EPS);
		assertEquals(1,instance.value(2),EPS);
		assertEquals(1.2,instance.value(3),EPS);
		assertEquals(1,instance.value(4),EPS);
		assertEquals(2.2,instance.value(5),EPS);

		//check input values
		assertEquals(1,instance.valueInputAttribute(0),EPS);
		assertEquals(1.2,instance.valueInputAttribute(1),EPS);
		assertEquals(1,instance.valueInputAttribute(2),EPS);
		assertEquals(2.2,instance.valueInputAttribute(3),EPS);
		//check output values
		assertEquals(1,instance.valueOutputAttribute(0),EPS);
		assertEquals(0,instance.valueOutputAttribute(1),EPS);
		
		//instance 3
		instance=instancesClassification.arff.readInstance();
		instance.setDataset(instancesClassification);
		//check attributes
		assertEquals(0,instance.value(0),EPS);
		assertEquals(0,instance.value(1),EPS);
		assertEquals(2,instance.value(2),EPS);
		assertEquals(1.3,instance.value(3),EPS);
		assertEquals(0,instance.value(4),EPS);
		assertEquals(3.3,instance.value(5),EPS);

		//check input values
		assertEquals(2,instance.valueInputAttribute(0),EPS);
		assertEquals(1.3,instance.valueInputAttribute(1),EPS);
		assertEquals(0,instance.valueInputAttribute(2),EPS);
		assertEquals(3.3,instance.valueInputAttribute(3),EPS);

		//check output values
		assertEquals(0,instance.valueOutputAttribute(0),EPS);
		assertEquals(0,instance.valueOutputAttribute(1),EPS);
	}
}
