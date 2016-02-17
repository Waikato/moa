/*
 *    OzaBagML.java
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
import moa.classifiers.meta.OzaBag;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.core.MiscUtils;
import moa.core.Example;

/**
 * OzaBag for Multi-label data.
 * 
 * @author Jesse Read
 * @version $Revision: 1 $
 */
public class OzaBagML extends OzaBag implements MultiLabelLearner, MultiTargetRegressor{

	/*
   @Override
	public double[] getVotesForInstance(Instance x) {

		int m_L = x.numClasses();

        double y[] = new double[m_L];

        for (int i = 0; i < this.ensemble.length; i++) {
			try {
				double w[] = this.ensemble[i].getVotesForInstance(x);
				System.out.println("w"+w.length);
				System.out.println("y"+y.length);
				for (int j = 0; j < w.length; j++) {
					y[j] += w[j];
				}
			} catch(NullPointerException e) {
				System.err.println("NullPointer");
			} catch(ArrayIndexOutOfBoundsException e) {
				System.err.println("OutofBounds");
			}

        }

        return y;
    }
	*/

	public static Prediction makePrediction(double predictionArray[]) {
		//System.out.println("y = "+Arrays.toString(predictionArray));
        Prediction prediction = new MultiLabelPrediction(predictionArray.length);
        for (int j = 0; j < predictionArray.length; j++) {
            prediction.setVote(j, 1, predictionArray[j]);
            //prediction.setVote(j, 0, 1. - predictionArray[j]);
        }
        return prediction;
	}

	public static double[] compileVotes(Classifier h[], Instance inst) {
		double votes[] = h[0].getVotesForInstance(inst);
		for (int i = 1; i < h.length; i++) {
			try {
				double more_votes[] = h[i].getVotesForInstance(inst);
				for(int j = 0; j < more_votes.length; j++) {
					votes[j] = votes[j] + more_votes[j];
				}
			} catch(NullPointerException e) {
				System.err.println("NullPointer");
			} catch(ArrayIndexOutOfBoundsException e) {
				System.err.println("OutofBounds");
			}
		}
		try {
			for(int j = 0; j < votes.length; j++) {
				votes[j] = votes[j] / h.length;
			}
		} catch(NullPointerException e) {
			System.err.println("NullPointer");
		}
		return votes;
	}

	@Override
    public double[] getVotesForInstance(Instance inst) {
		return compileVotes(this.ensemble, inst);
	}

	@Override
	public void trainOnInstanceImpl(MultiLabelInstance inst) {
		super.trainOnInstance((Instance)inst);
		/*
        for (int i = 0; i < this.ensemble.length; i++) {
            int k = MiscUtils.poisson(1.0, this.classifierRandom);
            if (k > 0) {
                Instance weightedInst = (Instance) inst.copy();
                weightedInst.setWeight(inst.weight() * k);
                this.ensemble[i].trainOnInstance(weightedInst);
            }
        }
		*/
    }

	@Override
    public Prediction getPredictionForInstance(Example<Instance> example) {
        return getPredictionForInstance((MultiLabelInstance) example.getData());
    }

	@Override
    public Prediction getPredictionForInstance(MultiLabelInstance instance) {

        double[] predictionArray = this.getVotesForInstance(instance);
		if (predictionArray == null)
			return null;
		else
			return OzaBagML.makePrediction(predictionArray);
    }

}
