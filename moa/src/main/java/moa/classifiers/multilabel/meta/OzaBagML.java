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
import moa.core.InstanceExample;
import moa.core.MiscUtils;
import moa.core.Example;

import java.util.Arrays;

/**
 * OzaBag for Multi-label data.
 * 
 * @author Jesse Read
 * @version $Revision: 1 $
 */
public class OzaBagML extends OzaBag implements MultiLabelLearner, MultiTargetRegressor{

	//Training
	@Override
	public void trainOnInstanceImpl(MultiLabelInstance inst) {
		trainOnInstanceImpl((Instance) inst);
	}

	// Predictions
	@Override
	public Prediction getPredictionForInstance(Example<Instance> example) {
		return compilePredictions(this.ensemble, example);
	}

	public static Prediction compilePredictions(Classifier h[], Example example) {
		Prediction[] predictions = new Prediction[h.length];
		for (int i = 0; i < h.length; i++) {
			predictions[i] = h[i].getPredictionForInstance(example);
		}
		return combinePredictions(predictions, (Instance) example.getData());
	}

	public static Prediction combinePredictions(Prediction[] predictions, Instance inst) {
		Prediction result = new MultiLabelPrediction(inst.numOutputAttributes());
		for (int i = 0; i < predictions.length; i++) {
			try {
				Prediction more_votes = predictions[i];
				if (more_votes != null) {
					for (int numOutputAttribute = 0; numOutputAttribute < inst.numOutputAttributes(); numOutputAttribute++) {
						int length = 0;
						if (more_votes.getVotes(numOutputAttribute) != null)
							length = more_votes.getVotes(numOutputAttribute).length;
						for (int numValueAttribute = 0; numValueAttribute < length; numValueAttribute++) {
							result.setVote(numOutputAttribute, numValueAttribute,
									(result.getVote(numOutputAttribute, numValueAttribute) +
											more_votes.getVote(numOutputAttribute, numValueAttribute) / (double) predictions.length));
						}
					}
				}
			} catch (NullPointerException e) {
				System.err.println("NullPointer");
			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println("OutofBounds");
			}
		}
		return result;
	}


	//Legacy code: not used now, only Predictions are used
	@Override
    public double[] getVotesForInstance(Instance inst) {
		return compileVotes(this.ensemble, inst);
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
    public Prediction getPredictionForInstance(MultiLabelInstance instance) {
        return getPredictionForInstance((new InstanceExample(instance)));
    }

}
