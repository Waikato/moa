/*
 *    AdaptiveNodePredictor.java
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
package moa.learners.predictors.rules.functions;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.ClassificationPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.Measurement;
import moa.learners.predictors.AbstractClassifier;

/*
 * Chooses between Perceptron and Target Mean make predictions
 * Selection is made according to the current error os each predictor
 */
public class AdaptiveNodePredictor extends AbstractClassifier {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	protected Perceptron p;
	protected TargetMean tm;
	protected boolean hasStarted = false;

	public double getCurrentError() {
		if (p.getCurrentError() < tm.getCurrentError())
			return p.getCurrentError();
		else
			return tm.getCurrentError();
	}

	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public Prediction getPredictionForInstance(Instance inst) {
		double[] ret = null;
		if (hasStarted) {
			if (p.getCurrentError() < tm.getCurrentError())
				ret = p.getPredictionForInstance(inst).asDoubleArray();
			else
				ret = tm.getPredictionForInstance(inst).asDoubleArray();
		} else
			ret = new double[] { 0 };
		return new ClassificationPrediction(ret);

	}

	@Override
	public void resetLearningImpl() {
		hasStarted = false;
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		if (!hasStarted) {
			p = new Perceptron();
			tm = new TargetMean();
			p.resetLearning();
			tm.resetLearning();
			hasStarted = true;
		}
		p.trainOnInstance(inst);
		tm.trainOnInstance(inst);

	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
	}

	@Override
	public String getPurposeString() {
		return "Returns the prediction of a perceptron or the target mean, according to the current mean absolute error.";
	}

}
