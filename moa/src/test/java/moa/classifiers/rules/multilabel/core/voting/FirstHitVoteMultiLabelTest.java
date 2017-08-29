/*
 *    FirstHitVoteMultiLabelTest.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package moa.classifiers.rules.multilabel.core.voting;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Test FirstHitVoteMultiLabel class
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 */
public class FirstHitVoteMultiLabelTest {

	static FirstHitVoteMultiLabel weightedVote;
	static final double EPS=0.0000001;
	@BeforeClass
	    public static void runBefore() {
			weightedVote = new FirstHitVoteMultiLabel();
			Prediction pred= new MultiLabelPrediction(4);
			pred.setVotes(0, new double[]{1});
			pred.setVotes(3, new double[]{4});
			weightedVote.addVote(pred, new double[]{4,Double.MAX_VALUE,Double.MAX_VALUE,3});
			
			pred= new MultiLabelPrediction(4);
			pred.setVotes(0, new double[]{2});
			pred.setVotes(2, new double[]{3});
			pred.setVotes(3, new double[]{2});
			weightedVote.addVote(pred, new double[]{3,Double.MAX_VALUE,1,1});
			
			pred= new MultiLabelPrediction(4);
			pred.setVotes(1, new double[]{2});
			pred.setVotes(3, new double[]{1});
			weightedVote.addVote(pred, new double[]{Double.MAX_VALUE,4,Double.MAX_VALUE,0});
			weightedVote.computeWeightedVote(); 
	     }
	    
	
		@Test
		public void testComputeWeightedVote() {
			Prediction weightedPred=weightedVote.computeWeightedVote();
			assertEquals(1,weightedPred.getVote(0, 0), EPS);
			assertEquals(2,weightedPred.getVote(1, 0), EPS);
			assertEquals(3,weightedPred.getVote(2, 0), EPS);
			assertEquals(4,weightedPred.getVote(3, 0), EPS);
		}
	
		@Test
		public void testGetWeightedError() {
			assertEquals(3,weightedVote.getWeightedError(), EPS);
		}
	
		@Test
		public void testGetWeights() {
			double [][] weights=weightedVote.getWeights();
			assertEquals(1,weights[0][0], EPS);
			assertEquals(1,weights[0][3], EPS);
			
			assertEquals(0,weights[1][0], EPS);
			assertEquals(1,weights[1][2], EPS);
			assertEquals(0,weights[1][3], EPS);
			
			assertEquals(1,weights[2][1], EPS);
			assertEquals(0,weights[2][3], EPS);
			
		}
	
		@Test
		public void testGetNumberVotes() {
			assertEquals(2,weightedVote.getNumberVotes(0));
		}
	
		@Test
		public void testGetNumberVotesInt() {
			assertEquals(2, weightedVote.getNumberVotes(0));
			assertEquals(1, weightedVote.getNumberVotes(1));
			assertEquals(1, weightedVote.getNumberVotes(2));
			assertEquals(3, weightedVote.getNumberVotes(3));
		}
	
		@Test
		public void testGetOutputAttributesErrors() {
			double [] errors=weightedVote.getOutputAttributesErrors();
			assertEquals(4, errors[0],EPS);
			assertEquals(4, errors[1],EPS);
			assertEquals(1, errors[2],EPS);
			assertEquals(3, errors[3],EPS);
		}
}
