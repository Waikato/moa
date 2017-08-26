package moa.classifiers.rules.multilabel.instancetransformers;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import moa.streams.MultiTargetArffFileStream;
import moa.streams.filters.SelectAttributesFilter;

import org.junit.BeforeClass;
import org.junit.Test;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.Range;

public class InstanceOutputAttributesSelectorTest {
	private static Instance instance1, instance2, transformed1, transformed2;
	private static double EPS=0.00000001;
	private static InstanceOutputAttributesSelector transformer;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
        MultiTargetArffFileStream stream =  new MultiTargetArffFileStream(ClassLoader.getSystemResource("moa/classifiers/data/small_regression.arff").getPath(), "4-6");
		instance1=stream.nextInstance().getData();
		instance2=stream.nextInstance().getData();
		transformer= new InstanceOutputAttributesSelector(new InstancesHeader(instance1.dataset()), new int[]{0,2});
		transformed1=transformer.sourceInstanceToTarget(instance1);
		transformed2=transformer.sourceInstanceToTarget(instance2);
	}
	@Test
	public void sourceInstanceToTarget() {
		assertEquals(1,transformed1.valueOutputAttribute(0), EPS);
		assertEquals(3,transformed1.valueOutputAttribute(1), EPS);
		assertEquals(1.1,transformed1.valueInputAttribute(0), EPS);
		assertEquals(-2.3,transformed1.valueInputAttribute(1), EPS);
		assertEquals(2,transformed1.valueInputAttribute(2), EPS);
		assertEquals(3.3,transformed1.valueInputAttribute(3), EPS);
		assertEquals(0,transformed1.valueInputAttribute(4), EPS);
		
		assertEquals(1.2,transformed2.valueInputAttribute(0), EPS);
		assertEquals(-2.2,transformed2.valueInputAttribute(1), EPS);
		assertEquals(0,transformed2.valueInputAttribute(2), EPS);
		assertEquals(3.2,transformed2.valueInputAttribute(3), EPS);
		assertEquals(0,transformed2.valueInputAttribute(4), EPS);
		
		assertEquals(3,transformed2.valueOutputAttribute(0), EPS);
		assertEquals(2, transformed2.valueOutputAttribute(1), EPS);
	}

	@Test
	public void testTargetPredictionToSource() {
		Prediction targetPred= new MultiLabelPrediction(2);
		targetPred.setVotes(0, new double []{0.5});
		targetPred.setVotes(1, new double []{1.5});
		Prediction pred=transformer.targetPredictionToSource(targetPred);
		
		assertEquals(0.5, pred.getVotes(0)[0], EPS);
		assertTrue(pred.getVotes(1).length==0);
		assertEquals(1.5, pred.getVotes(2)[0], EPS);
	}
	
	@Test
	public void testNumInputs() {
		assertEquals(5, transformed1.numInputAttributes(), EPS);
		assertEquals(5, transformed2.numInputAttributes(), EPS);
	}
	
	@Test
	public void testNumOutputs() {
		assertEquals(2, transformed1.numOutputAttributes(), EPS);
		assertEquals(2, transformed2.numOutputAttributes(), EPS);
	}

}
