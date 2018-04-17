/*
 *    MLOzaBagAdwin.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jesse@tsc.uc3m.es)
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
package moa.classifiers.mlc.meta;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.StructuredInstance;
import com.yahoo.labs.samoa.instances.predictions.MultiTargetRegressionPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.classifiers.meta.AbstractOzaBagAdwin;
import moa.classifiers.mtr.trees.ISOUPTree;
import moa.core.DoubleVector;
import moa.learners.MultiTargetRegressor;

public class OzaBagAdwinMultiTargetRegression extends AbstractOzaBagAdwin<MultiTargetRegressor> implements MultiTargetRegressor {

	private static final long serialVersionUID = 1L;

	public OzaBagAdwinMultiTargetRegression() {
		super(MultiTargetRegressor.class, "moa.classifiers.mtr.trees.ISOUPTree");
	}

	public double getAdwinError(Instance inst, int i) {
		// TODO this needs a rework
		double sum = 0.0;
		if (this.ensemble.get(i) instanceof ISOUPTree) {
			double[] normalizedError = ((ISOUPTree) this.ensemble.get(i)).getNormalizedError(inst, this.ensemble.get(i).getPredictionForInstance(inst).asDoubleArray());
			for (int j = 0; j < inst.numOutputAttributes(); j++) {
				sum += normalizedError[j];
			}
		} else {
			Prediction prediction = this.ensemble.get(i).getPredictionForInstance(inst);
			for (int j = 0; j < inst.numOutputAttributes(); j++) {
				sum += Math.abs(prediction.getPrediction(j) - inst.valueOutputAttribute(j));
			}
		}
		sum = sum / inst.numOutputAttributes();
		return sum;
	}

	@Override
	public Prediction combinePredictions(Prediction[] predictions) {
		DoubleVector sums = new DoubleVector();
		for (Prediction p : predictions) {
			sums.addValues(p.asDoubleVector());
		}
		sums.scaleValues(1.0 / predictions.length);
		return new MultiTargetRegressionPrediction(sums);
	}

	@Override
	public void trainOnInstanceImpl(StructuredInstance inst) {
		this.trainOnInstanceImpl((Instance) inst);
	}

	@Override
	public Prediction getPredictionForInstance(StructuredInstance inst) {
		return this.getPredictionForInstance((Instance) inst);
	}

}
