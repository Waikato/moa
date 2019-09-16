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

import com.yahoo.labs.samoa.instances.Attribute;

import moa.core.DoubleVector;

public class ClassificationPrediction implements Prediction, Serializable {

	private static final long serialVersionUID = 1L;

	protected double[] prediction = new double[0];

	protected Attribute classAttribute;

	public ClassificationPrediction() {
	}

	public ClassificationPrediction(double[] prediction) {
		this.prediction = prediction;
	}

	public void setClassAttribute(Attribute a) {
		this.classAttribute = a;
	}

	@Override
	public int numOutputAttributes() {
		return 1;
	}

	@Override
	public int numClasses(int outputAttributeIndex) {
		throw new UnsupportedOperationException("This is a single-target classification prediction.");
	}

	@Override
	public double[] getVotes(int outputAttributeIndex) {
		throw new UnsupportedOperationException("This is a single-target classification prediction.");
	}

	@Override
	public double[] getVotes() {
		return prediction;
	}

	@Override
	public double getVote(int outputAttributeIndex, int classIndex) {
		throw new UnsupportedOperationException("This is a single-target classification prediction.");
	}

	@Override
	public void setVotes(int outputAttributeIndex, double[] votes) {
		throw new UnsupportedOperationException("This is a single-target classification prediction.");
	}

	@Override
	public void setVotes(double[] votes) {
		this.prediction = votes;
	}

	@Override
	public void setVote(int outputAttributeIndex, int classIndex, double vote) {
		throw new UnsupportedOperationException("This is a single-target classification prediction.");
	}

	@Override
	public void setVote(int outputAttributeIndex, double vote) {
		throw new UnsupportedOperationException("This is a single-target classification prediction.");
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Out: ");
		int id = 0;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < prediction.length; i++) {
			if (prediction[i] > max) {
				id = i;
				max = prediction[i];
			}
		}
		if (classAttribute != null) {
			sb.append(classAttribute.value(id));
		} else
			sb.append(id);
		return sb.toString();
	}

	@Override
	public boolean hasVotesForAttribute(int outputAttributeIndex) {
		throw new UnsupportedOperationException("This is a single-target classification prediction.");
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public double asDouble() {
		throw new UnsupportedOperationException("This is a classification prediction.");
	}

	@Override
	public DoubleVector asDoubleVector() {
		return new DoubleVector(prediction);
	}

	@Override
	public double[] asDoubleArray() {
		return prediction;
	}

	@Override
	public String asPredictionString() {
		return String.valueOf(prediction.toString());
	}

	@Override
	public double getPrediction(int outputAttributeIndex) {
		throw new UnsupportedOperationException("This is a single-target classification prediction.");
	}

}
