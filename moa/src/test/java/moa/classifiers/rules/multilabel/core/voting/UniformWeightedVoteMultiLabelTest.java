package moa.classifiers.rules.multilabel.core.voting;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Test UniformWeightedVoteMultiLabel
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 */
public class UniformWeightedVoteMultiLabelTest {
	
	static UniformWeightedVoteMultiLabel weightedVote;
	static final double EPS=0.0000001;
	
    @BeforeClass
    public static void runBefore() {
		weightedVote = new UniformWeightedVoteMultiLabel();
		Prediction pred= new MultiLabelPrediction(4);
		pred.setVotes(0, new double[]{1});
		pred.setVotes(1, new double[]{2});
		pred.setVotes(2, new double[]{3});
		pred.setVotes(3, new double[]{4});
		weightedVote.addVote(pred, new double[]{4,3,2,1});
		
		pred= new MultiLabelPrediction(4);
		pred.setVotes(0, new double[]{1});
		pred.setVotes(1, new double[]{2});
		pred.setVotes(3, new double[]{4});
		weightedVote.addVote(pred, new double[]{3,2,Double.MAX_VALUE,0});
		
		pred= new MultiLabelPrediction(4);
		pred.setVotes(0, new double[]{4});
		pred.setVotes(1, new double[]{2});
		pred.setVotes(3, new double[]{1});
		weightedVote.addVote(pred, new double[]{1,0,Double.MAX_VALUE,0});
		weightedVote.computeWeightedVote(); 
     }
    

	@Test
	public void testComputeWeightedVote() {
		Prediction weightedPred=weightedVote.computeWeightedVote();
		assertEquals(2,weightedPred.getVote(0, 0), EPS);
		assertEquals(2,weightedPred.getVote(1, 0), EPS);
		assertEquals(3,weightedPred.getVote(2, 0), EPS);
		assertEquals(3,weightedPred.getVote(3, 0), EPS);
	}

	@Test
	public void testGetWeightedError() {
		assertEquals(1.66666666666666,weightedVote.getWeightedError(), EPS);
	}

	@Test
	public void testGetWeights() {
		double [][] weights=weightedVote.getWeights();
		assertEquals(1.0/3,weights[0][0], EPS);
		assertEquals(1.0/3,weights[1][0], EPS);
		assertEquals(1.0/3,weights[2][0], EPS);
		
		assertEquals(1.0/3,weights[0][1], EPS);
		assertEquals(1.0/3,weights[1][1], EPS);
		assertEquals(1.0/3,weights[2][1], EPS);
		
		assertEquals(1,weights[0][2], EPS);
		assertEquals(0,weights[1][2], EPS);
		assertEquals(0,weights[2][2], EPS);
		
		assertEquals(1.0/3,weights[0][3], EPS);
		assertEquals(1.0/3,weights[1][3], EPS);
		assertEquals(1.0/3,weights[2][3], EPS);
	}

	@Test
	public void testGetNumberVotes() {
		assertEquals(3,weightedVote.getNumberVotes(0));
	}

	@Test
	public void testGetNumberVotesInt() {
		assertEquals(3, weightedVote.getNumberVotes(0));
		assertEquals(3, weightedVote.getNumberVotes(1));
		assertEquals(1, weightedVote.getNumberVotes(2));
		assertEquals(3, weightedVote.getNumberVotes(3));
	}

	@Test
	public void testGetOutputAttributesErrors() {
		double [] errors=weightedVote.getOutputAttributesErrors();
		assertEquals(8.0/3, errors[0],EPS);
		assertEquals(5.0/3, errors[1],EPS);
		assertEquals(2, errors[2],EPS);
		assertEquals(1.0/3, errors[3],EPS);
		
	}

}
