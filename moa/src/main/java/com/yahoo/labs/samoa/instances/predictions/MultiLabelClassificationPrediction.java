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

public class MultiLabelClassificationPrediction implements Prediction, Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	protected DoubleVector prediction;

	public MultiLabelClassificationPrediction() {
		this(0);
	}

	public MultiLabelClassificationPrediction(int numOutputAttributes) {
		prediction = new DoubleVector();
		for (int i = 0; i < numOutputAttributes; i++)
			prediction.setValue(i, 0.0);
	}

	public MultiLabelClassificationPrediction(DoubleVector prediction) {
		this.prediction = prediction;
	}

	public MultiLabelClassificationPrediction(double[] prediction) {
		this.prediction = new DoubleVector(prediction);
	}

	@Override
	public int numOutputAttributes() {
		return prediction.numValues();
	}

	@Override
	public int numClasses(int outputAttributeIndex) {
		throw new UnsupportedOperationException(
				"This is a multi-label classification prediction. Each output attribute corresponds to a label.");
	}

	@Override
	public double[] getVotes(int outputAttributeIndex) {
		throw new UnsupportedOperationException(
				"This is a multi-label classification prediction. Each output attribute corresponds to a label.");
	}

	@Override
	public double[] getVotes() {
		return this.asDoubleArray();
	}

	@Override
	public double getVote(int outputAttributeIndex, int classIndex) {
		if (classIndex != 0) {
			throw new UnsupportedOperationException(
					"This is a multi-label classification prediction. Each output attribute corresponds to a label.");
		} else {
			// Simulate multi-nominal output with 2 class variables on each target.
			return getPrediction(outputAttributeIndex);
		}
	}

	@Override
	public void setVotes(int outputAttributeIndex, double[] votes) {
		throw new UnsupportedOperationException(
				"This is a multi-label classification prediction. Each output attribute corresponds to a label.");
	}

	@Override
	public void setVotes(double[] votes) {
		this.prediction = new DoubleVector(votes);
	}

	@Override
	public void setVote(int outputAttributeIndex, int classIndex, double vote) {
		if (classIndex != 0) {
			throw new UnsupportedOperationException(
					"This is a multi-label classification prediction. Each output attribute corresponds to a label.");
		} else {
			// Simulate multi-nominal output with 2 class variables on each target.
			setVote(outputAttributeIndex, vote);
		}
	}

	@Override
	public void setVote(int outputAttributeIndex, double vote) {
		this.prediction.setValue(outputAttributeIndex, vote);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		// TODO
//		for (int i=0; i<prediction.length; i++){
//			sb.append("Out " + i + ": ");
//			for (int c=0; c<prediction[i].numValues(); c++)
//			{
//				sb.append(((int)(prediction[i].getValue(c)*1000)/1000.0)+ " ");
//			}
//		}
		return sb.toString();
	}

	@Override
	public boolean hasVotesForAttribute(int outputAttributeIndex) {
		return prediction.getValue(outputAttributeIndex) != 0;
	}

	@Override
	public int size() {
		return prediction.numValues();
	}

	@Override
	public double asDouble() {
		throw new UnsupportedOperationException("This is a multi-label classification prediction.");
	}

	@Override
	public DoubleVector asDoubleVector() {
		return (DoubleVector) prediction.copy();
	}

	@Override
	public double[] asDoubleArray() {
		return prediction.getArrayCopy();
	}

	@Override
	public String asPredictionString() {
		return String.valueOf(prediction.toString());
	}

	@Override
	public double getPrediction(int outputAttributeIndex) {
		return prediction.getValue(outputAttributeIndex);
	}

}
