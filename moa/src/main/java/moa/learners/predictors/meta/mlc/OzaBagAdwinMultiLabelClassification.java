/*
 *    OzaBagAdwinML.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package moa.learners.predictors.meta.mlc;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.predictions.MultiTargetRegressionPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.DoubleVector;
import moa.learners.predictors.MultiLabelClassifier;
import moa.learners.predictors.meta.AbstractOzaBagAdwin;

/**
 * OzaBagAdwinML: Changes the way to compute accuracy as an input for Adwin
 *
 * @author Jesse Read
 * @version $Revision: 1 $
 */
public class OzaBagAdwinMultiLabelClassification extends AbstractOzaBagAdwin<MultiLabelClassifier>
		implements MultiLabelClassifier {

	private static final long serialVersionUID = 1L;

	public OzaBagAdwinMultiLabelClassification() {
		// TODO Determine the default model for MLC
		super(MultiLabelClassifier.class, "");
	}

	public FloatOption splitConfidenceOption = new FloatOption("classificationThreshold", 't',
			"The threhshold a label must pass to be predicted as present.", 0.5, 0.0, 1.0);

	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		this.trainOnInstanceImpl((Instance) instance);
	}

	public Prediction getPredictionForInstance(MultiLabelInstance instance) {
		return getPredictionForInstance((Instance) instance);
	}

	@Override
	public double getAdwinError(Instance inst, int i) {
		double actual[] = new double[inst.numOutputAttributes()];
		double prediction[] = this.ensemble.get(i).getPredictionForInstance(inst).asDoubleArray();
		for (int j = 0; j < inst.numOutputAttributes(); j++) {
			actual[j] = inst.valueOutputAttribute(j);
		}

		// compute loss
		// double loss = Metrics.L_ZeroOne(A.toIntArray(actual,0.5),
		// A.toIntArray(prediction,0.5));
		// System.err.println("loss["+i+"] = "+loss);

		int set_union = 0;
		int set_inter = 0;
		double t = 0.01;
		for (int j = 0; j < prediction.length; j++) {
			int p = (prediction[j] >= t) ? 1 : 0;
			int R = (int) actual[j];
			if (p == 1) {
				// predt 1, real 1
				if (R == 1) {
					set_inter++;
					set_union++;
				}
				// predt 1, real 0
				else {
					set_union++;
				}
			} else {
				// predt 0, real 1
				if (R == 1) {
					set_union++;
				}
				// predt 0, real 0
				else {
				}
			}
		}
		double accuracy = 0.0;
		if (set_union > 0) // avoid NaN
			accuracy = ((double) set_inter / (double) set_union);
		return 1 - accuracy;
	}

	@Override
	public Prediction combinePredictions(Prediction[] predictions) {
		DoubleVector sums = new DoubleVector();
		for (Prediction p : predictions) {
			sums.addValues(p.asDoubleVector());
		}
		sums.scaleValues(1 / sums.numValues());
		return new MultiTargetRegressionPrediction(sums);
	}

}
