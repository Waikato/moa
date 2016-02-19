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

import java.util.Arrays;

/**
 * OzaBag for Multi-label data.
 * 
 * @author Jesse Read
 * @version $Revision: 1 $
 */
public class OzaBagML extends OzaBag implements MultiLabelLearner, MultiTargetRegressor{

	@Override
	public void resetLearningImpl() {
		this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
		Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
		baseLearner.resetLearning();
		for (int i = 0; i < this.ensemble.length; i++) {
			this.ensemble[i] = baseLearner.copy();
		}
	}


	public static Prediction makePrediction(double predictionArray[]) {
		//System.out.println("y = "+Arrays.toString(predictionArray));
        Prediction prediction = new MultiLabelPrediction(predictionArray.length);
        for (int j = 0; j < predictionArray.length; j++) {
            prediction.setVote(j, 1, predictionArray[j]);
            prediction.setVote(j, 0, 1. - predictionArray[j]);
        }
        return prediction;
	}

	public static double[] compileVotes(Classifier h[], Instance inst) {
		double votes[] = h[0].getVotesForInstance(inst);
		//System.err.println(Arrays.toString(votes));
		for (int i = 1; i < h.length; i++) {
			try {
				double more_votes[] = h[i].getVotesForInstance(inst);
				//System.err.println(Arrays.toString(more_votes));
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

	public static Prediction compilePredictions(Classifier h[], Instance inst) {
		Prediction[] predictions = new Prediction[h.length];
		for (int i = 0; i < h.length; i++) {
			//try {
			predictions[i] = h[i].getPredictionForInstance(inst);
		}
		return combinePredictions(predictions, inst);
	}

	public static Prediction compilePredictions(Classifier h[], Example example) {
		return makePrediction(compileVotes(h, (Instance) example.getData()));
		/*Prediction[] predictions = new Prediction[h.length];
		for (int i = 0; i < h.length; i++) {
			//try {
			predictions[i] = h[i].getPredictionForInstance(example);
		}
		return combinePredictions(predictions, (Instance) example.getData());
		*/
	}

	public static Prediction compilePredictions1(Classifier h[], Instance inst) {
		//return makePrediction(compileVotes(h, inst));
		//System.err.println(Arrays.toString(compileVotes(h, inst)));
		Prediction result = new MultiLabelPrediction(inst.numOutputAttributes());
		for (int i = 0; i < h.length; i++) {
			//try {
			Prediction more_votes = h[i].getPredictionForInstance(inst);
			//System.err.println(more_votes);
			if (more_votes != null) {
				for (int numOutputAttribute = 0; numOutputAttribute < inst.numOutputAttributes(); numOutputAttribute++) {
					int length = 0;
					if (more_votes.getVotes(numOutputAttribute) != null)
						length = more_votes.getVotes(numOutputAttribute).length;
					for (int numValueAttribute = 0; numValueAttribute < length  //inst..numClasses(numOutputAttribute)
							; numValueAttribute++) {
						result.setVote(numOutputAttribute, numValueAttribute,
								(result.getVote(numOutputAttribute, numValueAttribute) +
										more_votes.getVote(numOutputAttribute, numValueAttribute) / (double) h.length));
						//System.err.println(result.getVote(numOutputAttribute, numValuesAttribute)+" : "+
						//		more_votes.getVote(numOutputAttribute, numValuesAttribute));
					}
				}
			}
			//} catch (NullPointerException e) {
			//	System.err.println("NullPointer");
			//} catch (ArrayIndexOutOfBoundsException e) {
			//	System.err.println("-OutofBounds");
			//}
		}
		//System.err.println(result);
		return result;
	}

	public static Prediction combinePredictions(Prediction[] predictions, Instance inst) {
		//return makePrediction(compileVotes(h, inst));
		//System.err.println(Arrays.toString(compileVotes(h, inst)));
		Prediction result = new MultiLabelPrediction(inst.numOutputAttributes());
		for (int i = 0; i < predictions.length; i++) {
			//try {
			Prediction more_votes = predictions[i];
			//System.err.println(more_votes);
			if (more_votes != null) {
				for (int numOutputAttribute = 0; numOutputAttribute < inst.numOutputAttributes(); numOutputAttribute++) {
					int length = 0;
					if (more_votes.getVotes(numOutputAttribute) != null)
						length = more_votes.getVotes(numOutputAttribute).length;
					for (int numValueAttribute = 0; numValueAttribute < length  //inst..numClasses(numOutputAttribute)
							; numValueAttribute++) {
						result.setVote(numOutputAttribute, numValueAttribute,
								(result.getVote(numOutputAttribute, numValueAttribute) +
										more_votes.getVote(numOutputAttribute, numValueAttribute) / (double) predictions.length));
						//System.err.println(result.getVote(numOutputAttribute, numValuesAttribute)+" : "+
						//		more_votes.getVote(numOutputAttribute, numValuesAttribute));
					}
				}
			}
			//} catch (NullPointerException e) {
			//	System.err.println("NullPointer");
			//} catch (ArrayIndexOutOfBoundsException e) {
			//	System.err.println("-OutofBounds");
			//}
		}
		//System.err.println(result);
		return result;
	}

	@Override
    public double[] getVotesForInstance(Instance inst) {
		return compileVotes(this.ensemble, inst);
	}

	@Override
	public void trainOnInstanceImpl(MultiLabelInstance inst) {
		trainOnInstanceImpl((Instance) inst);
	}
	@Override
	public void trainOnInstanceImpl(Instance inst) {
		//super.trainOnInstance((Instance)inst);
		//System.err.println(" Bag-trainOnInstance "+this.ensemble.length);
        for (int i = 0; i < this.ensemble.length; i++) {
			//System.err.println(" Bag-trainOnInstance "+i);
            int k = MiscUtils.poisson(1.0, this.classifierRandom);
            if (k > 0) {
                MultiLabelInstance weightedInst = (MultiLabelInstance) inst.copy();
                weightedInst.setWeight(inst.weight() * k);
                this.ensemble[i].trainOnInstance(weightedInst);
            }
        }

    }

	@Override
    public Prediction getPredictionForInstance(Example<Instance> example) {
        return compilePredictions(this.ensemble, example);//getPredictionForInstance((MultiLabelInstance) example.getData());
    }

	@Override
    public Prediction getPredictionForInstance(MultiLabelInstance instance) {
		//System.err.println("BAG getPredictionForInstance");
        return  //this.ensemble[0].getPredictionForInstance((Instance) instance);
				compilePredictions(this.ensemble, instance);
        /*double[] predictionArray = this.getVotesForInstance(instance);
		if (predictionArray == null)
			return null;
		else
			return OzaBagML.makePrediction(predictionArray);
			*/
    }

}
