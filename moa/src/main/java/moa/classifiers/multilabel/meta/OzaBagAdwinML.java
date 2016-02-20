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
package moa.classifiers.multilabel.meta;

import moa.classifiers.Classifier;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.classifiers.meta.OzaBagAdwin;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.InstanceExample;
import moa.core.MiscUtils;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.core.Example;
import meka.core.Metrics;
import meka.core.A;

/**
 * OzaBagAdwinML: Changes the way to compute accuracy as an input for Adwin
 * 
 * @author Jesse Read
 * @version $Revision: 1 $
 */
public class OzaBagAdwinML extends OzaBagAdwin implements MultiLabelLearner, MultiTargetRegressor {

    @Override
    public void trainOnInstanceImpl(Instance inst) {
		// train
		try {
			super.trainOnInstanceImpl(inst);
		} catch(NullPointerException e) {
			System.err.println("[Warning] NullPointer on train.");
			//e.printStackTrace();
		}

		for (int i = 0; i < this.ensemble.length; i++) {

			// get prediction
			double prediction[] = this.ensemble[i].getVotesForInstance(inst);
			if (prediction == null) {
				prediction = new double[]{};
			}

			// get true value
			double actual[] = new double[prediction.length];
			for (int j = 0; j < prediction.length; j++) {
				actual[j] = (double)inst.classValue(j);
			}

			// compute loss
			double loss = Metrics.L_ZeroOne(A.toIntArray(actual,0.5), A.toIntArray(prediction,0.5));
			//System.err.println("loss["+i+"] = "+loss);

			// adwin stuff
			double ErrEstim = this.ADError[i].getEstimation();
			if (this.ADError[i].setInput(loss)) {
				if (this.ADError[i].getEstimation() > ErrEstim) {
					System.err.println("Change model "+i+"!");
					this.ensemble[i].resetLearning();
					this.ensemble[i].trainOnInstance(inst);
					this.ADError[i] = new ADWIN();
				}
			}
		}
	}

	@Override
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		trainOnInstanceImpl((Instance) instance);
	}

	@Override
    public Prediction getPredictionForInstance(Example<Instance> example) {
        return OzaBagML.compilePredictions(this.ensemble, example);
    }


	//Legacy code: not used now, only Predictions are used
	@Override
	public double[] getVotesForInstance(Instance inst) {
		return OzaBagML.compileVotes(this.ensemble, inst);
	}

	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance instance) {
		return getPredictionForInstance((new InstanceExample(instance)));
	}

}

