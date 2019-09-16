/*
 *    DominantLabelsClassifier.java
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
package moa.learners.predictors.core.functions;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.MultiLabelClassificationPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.Measurement;
import moa.learners.predictors.AbstractMultiLabelClassifier;
import moa.learners.predictors.MultiLabelClassifier;

public class DominantLabelsClassifier extends AbstractMultiLabelClassifier
		implements MultiLabelClassifier, AMRulesFunction {

	/**
	 * Predicts for each label by majority voting
	 */
	private static final long serialVersionUID = 1L;

	private double countVector[];
	private double numInstances;

	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public void resetWithMemory() {
		// 0-1 vector with majority class by label? Use fading strategy instead?

	}

	@Override
	public void selectOutputsToLearn(int[] outputAttributes) {
		int n = outputAttributes.length;
		double[] newCountVector = new double[n];
		for (int i = 0; i < n; i++)
			newCountVector[i] = countVector[outputAttributes[i]];
		newCountVector = countVector;
	}

	@Override
	public void trainOnInstanceImpl(Instance instance) {
		int numOutputs = instance.numOutputAttributes();
		if (countVector == null) {
			countVector = new double[numOutputs];
		}
		double weight = instance.weight();
		for (int i = 0; i < numOutputs; i++) {
			countVector[i] += weight * instance.valueOutputAttribute(i);
		}
		numInstances += weight;
	}

	@Override
	public Prediction getPredictionForInstance(Instance inst) {
		int numOutputs = inst.numOutputAttributes();
		Prediction p = new MultiLabelClassificationPrediction(numOutputs);
		if (countVector != null) {
			for (int i = 0; i < numOutputs; i++) {
				double frac = countVector[i] / numInstances;
				p.setVote(i, 1, frac);
				p.setVote(i, 0, 1 - frac);
			}
		}
		return p;
	}

	@Override
	public void resetLearningImpl() {
		countVector = null;
		numInstances = 0;
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
	}

}
