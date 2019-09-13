/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.  
 */
package com.yahoo.labs.samoa.instances.predictions;

import java.io.Serializable;

import moa.core.DoubleVector;

public class RegressionPrediction implements Prediction, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	protected double prediction;

	public RegressionPrediction() {
	}

	public RegressionPrediction(double prediction) {
		this.prediction = prediction;
	}
	
	@Override
	public int numOutputAttributes() {
		return 1;
	}

	@Override
	public int numClasses(int outputAttributeIndex) {
		throw new UnsupportedOperationException("This is a single-target regression prediction.");
	}

	@Override
	public double[] getVotes(int outputAttributeIndex) {
		throw new UnsupportedOperationException("This is a single-target regression prediction.");
	}
	
	@Override
	public double[] getVotes() {
		throw new UnsupportedOperationException("This is a single-target regression prediction.");
	}

	@Override
	public double getVote(int outputAttributeIndex, int classIndex) {
		if (outputAttributeIndex != 0)
			throw new UnsupportedOperationException("This is a single-target regression prediction.");
		return prediction;
	}

	@Override
	public void setVotes(int outputAttributeIndex, double[] votes) {
		throw new UnsupportedOperationException("This is a single-target regression prediction.");
	}
	
	@Override
	public void setVotes(double[] votes) {
		throw new UnsupportedOperationException("This is a single-target regression prediction.");
	}

	@Override
	public void setVote(int outputAttributeIndex, int classIndex, double vote) {
		throw new UnsupportedOperationException("This is a single-target regression prediction.");
	}


	@Override
	public void setVote(int outputAttributeIndex, double vote) {
		throw new UnsupportedOperationException("This is a single-target regression prediction.");		
	}
	
	@Override
	public String toString(){
		StringBuffer sb= new StringBuffer();
		sb.append("Out " + 0 + ": ");
		sb.append(((int) (prediction*1000)/1000.0)+ " ");
		return sb.toString();
	}

	@Override
	public boolean hasVotesForAttribute(int outputAttributeIndex) {
		throw new UnsupportedOperationException("This is a single-target regression prediction.");
	}

    @Override
    public int size() {
        return 1;
    }
    
    public double asDouble() {
    	return prediction;
    }
    
    public DoubleVector asDoubleVector() {
    	return new DoubleVector(new double[] {prediction});
    }

    public double[] asDoubleArray() {
    	return new double[] {prediction};
    }

	@Override
	public String asPredictionString() {
		return String.valueOf(prediction);
	}

	@Override
	public double getPrediction(int outputAttributeIndex) {
		if (outputAttributeIndex != 0)
			throw new UnsupportedOperationException("This is a single-target regression prediction.");
		return prediction;
	}
}
