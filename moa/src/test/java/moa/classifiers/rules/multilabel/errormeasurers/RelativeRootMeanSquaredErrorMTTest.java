package moa.classifiers.rules.multilabel.errormeasurers;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

public class RelativeRootMeanSquaredErrorMTTest {
	static protected MultiTargetErrorMeasurer m; 

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		m= new RelativeRootMeanSquaredErrorMT();
		Prediction trueTarget=new MultiLabelPrediction(3);
		Prediction prediction=new MultiLabelPrediction(3);
		prediction.setVote(0,0,4);
		prediction.setVote(1,0,50);
		prediction.setVote(2,0,-80);
		trueTarget.setVote(0,0,3);
		trueTarget.setVote(1,0,30);
		trueTarget.setVote(2,0,-100);
		m.addPrediction(prediction, trueTarget);
		
		trueTarget=new MultiLabelPrediction(3);
		prediction=new MultiLabelPrediction(3);
		prediction.setVote(0,0,3);
		prediction.setVote(1,0,2);
		prediction.setVote(2,0,-80);
		trueTarget.setVote(0,0,2);
		trueTarget.setVote(1,0,12);
		trueTarget.setVote(2,0,-75);
		m.addPrediction(prediction, trueTarget);
		
		trueTarget=new MultiLabelPrediction(3);
		prediction=new MultiLabelPrediction(3);
		prediction.setVote(0,0,9);
		prediction.setVote(1,0,-40);
		prediction.setVote(2,0,-80);
		trueTarget.setVote(0,0,3);
		trueTarget.setVote(1,0,-50);
		trueTarget.setVote(2,0,-50);
		m.addPrediction(prediction, trueTarget);
		
		trueTarget=new MultiLabelPrediction(3);
		prediction=new MultiLabelPrediction(3);

		prediction.setVote(0,0,-8);
		prediction.setVote(1,0,30);
		prediction.setVote(2,0,-80);
		trueTarget.setVote(0,0,-4);
		trueTarget.setVote(1,0,40);
		trueTarget.setVote(2,0,-80);
		m.addPrediction(prediction, trueTarget);
	}
	@Test
	public void testGetCurrentError() {
		assertEquals(0.9385045092, m.getCurrentError(), 0.0000001);
	}

	@Test
	public void testGetCurrentErrorInt() {
		assertEquals(0.6643044666, m.getCurrentError(0), 0.0000001);
		assertEquals(0.5150136019, m.getCurrentError(1), 0.0000001);
		assertEquals(1.6361954593, m.getCurrentError(2), 0.0000001);
	}

	@Test
	public void testGetCurrentErrors() {
		assertArrayEquals(new double []{0.6643044666,0.5150136019,1.6361954593}, m.getCurrentErrors(), 0.0000001);
	}
}
