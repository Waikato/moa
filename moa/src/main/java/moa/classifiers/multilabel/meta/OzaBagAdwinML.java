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

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.StructuredInstance;

import moa.classifiers.MultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.classifiers.meta.OzaBagAdwin;
import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.MiscUtils;

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
        boolean Change = false;
        for (int i = 0; i < this.ensemble.length; i++) {
            int k = MiscUtils.poisson(1.0, this.classifierRandom);
            if (k > 0) {
                Instance weightedInst = (Instance) inst.copy();
                weightedInst.setWeight(inst.weight() * k);
                this.ensemble[i].trainOnInstance(weightedInst);
            }

			// get prediction
			Prediction P = this.ensemble[i].getPredictionForInstance(inst);
			if (P == null) {
				continue; // TODO what to do here?
			}

			// get true value and prediction arrays
			double actual[] = new double[inst.numOutputAttributes()];
			double prediction[] = new double[inst.numOutputAttributes()];
			for (int j = 0; j < inst.numOutputAttributes(); j++) {
				actual[j] = (double)inst.valueOutputAttribute(j);
				prediction[j] = P.getVote(j, 1);
			}

			// compute loss
			//double loss = Metrics.L_ZeroOne(A.toIntArray(actual,0.5), A.toIntArray(prediction,0.5));
			//System.err.println("loss["+i+"] = "+loss);

			int p_sum = 0, r_sum = 0;
			int set_union = 0;
			int set_inter = 0;
			double t = 0.01;
			for(int j = 0; j < prediction.length; j++) {
				int p = (prediction[j] >= t) ? 1 : 0;
				int R = (int) actual[j];
				if (p==1) {
					p_sum++;
					// predt 1, real 1
					if(R==1) {
						set_inter++;
						set_union++;
					}
					// predt 1, real 0
					else {
						set_union++;
					}
				}
				else {                      
					// predt 0, real 1
					if(R==1) { 
						set_union++;
					}
					// predt 0, real 0
					else {   
					}
				}
			}
			double accuracy = 0.0;
			if(set_union > 0)	//avoid NaN
				accuracy = ((double)set_inter / (double)set_union);
			
			// adwin stuff
			double ErrEstim = this.ADError[i].getEstimation();
			
			if (this.ADError[i].setInput(1-accuracy)) {
				if (this.ADError[i].getEstimation() > ErrEstim) {
				Change = true;
				}
			}
		}
        
        if (Change) {
			double max=0.0; int imax=-1;
			for (int i = 0; i < this.ensemble.length; i++) {
				if (max<this.ADError[i].getEstimation()) {
					max=this.ADError[i].getEstimation();
					imax=i;
				}
			}
			if (imax!=-1) {
				 
				this.ensemble[imax].resetLearning();
				this.ensemble[imax].trainOnInstance(inst);
				this.ADError[imax]=new ADWIN();
			}
        }
	}
    
	@Override
	public void trainOnInstanceImpl(StructuredInstance instance) {
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
	public Prediction getPredictionForInstance(StructuredInstance instance) {
		return getPredictionForInstance((new InstanceExample(instance)));
	}

}

