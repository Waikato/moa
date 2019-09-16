package moa.learners.predictors.rules.multilabel.instancetransformers;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.predictions.MultiLabelClassificationPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.learners.predictors.core.instancetransformers.InstanceOutputAttributesSelector;
import moa.streams.ArffFileStream;

public class InstanceOutputAttributesSelectorTest {
	private static Instance instance1, instance2, transformed1, transformed2;
	private static double EPS = 0.00000001;
	private static InstanceOutputAttributesSelector transformer;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ArffFileStream stream = new ArffFileStream(
				ClassLoader.getSystemResource("moa/learners/predictors/data/small_regression.arff").getPath(), "4-6");
		instance1 = stream.nextInstance().getData();
		instance2 = stream.nextInstance().getData();
		transformer = new InstanceOutputAttributesSelector(new InstancesHeader(instance1.dataset()),
				new int[] { 0, 2 });
		transformed1 = transformer.sourceInstanceToTarget(instance1);
		transformed2 = transformer.sourceInstanceToTarget(instance2);
	}

	@Test
	public void sourceInstanceToTarget() {
		assertEquals(1, transformed1.valueOutputAttribute(0), EPS);
		assertEquals(3, transformed1.valueOutputAttribute(1), EPS);
		assertEquals(1.1, transformed1.valueInputAttribute(0), EPS);
		assertEquals(-2.3, transformed1.valueInputAttribute(1), EPS);
		assertEquals(2, transformed1.valueInputAttribute(2), EPS);
		assertEquals(3.3, transformed1.valueInputAttribute(3), EPS);
		assertEquals(0, transformed1.valueInputAttribute(4), EPS);

		assertEquals(1.2, transformed2.valueInputAttribute(0), EPS);
		assertEquals(-2.2, transformed2.valueInputAttribute(1), EPS);
		assertEquals(0, transformed2.valueInputAttribute(2), EPS);
		assertEquals(3.2, transformed2.valueInputAttribute(3), EPS);
		assertEquals(0, transformed2.valueInputAttribute(4), EPS);

		assertEquals(3, transformed2.valueOutputAttribute(0), EPS);
		assertEquals(2, transformed2.valueOutputAttribute(1), EPS);
	}

	@Test
	public void testTargetPredictionToSource() {
		Prediction targetPred = new MultiLabelClassificationPrediction(2);
		targetPred.setVotes(new double[] { 0.5, 1.5 });

		assertEquals(0.5, targetPred.getVotes()[0], EPS);
		assertEquals(1.5, targetPred.getVotes()[1], EPS);
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
