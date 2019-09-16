/*
 *    OzaBoost.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.learners.predictors.meta;

import com.github.javacliparser.FlagOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.learners.predictors.AbstractEnsembleLearner;
import moa.learners.predictors.InstanceLearner;
import moa.options.ClassOption;

/**
 * Incremental on-line boosting of Oza and Russell.
 *
 * <p>
 * See details in:<br />
 * N. Oza and S. Russell. Online bagging and boosting. In Artiﬁcial Intelligence
 * and Statistics 2001, pages 105–112. Morgan Kaufmann, 2001.
 * </p>
 * <p>
 * For the boosting method, Oza and Russell note that the weighting procedure of
 * AdaBoost actually divides the total example weight into two halves – half of
 * the weight is assigned to the correctly classiﬁed examples, and the other
 * half goes to the misclassiﬁed examples. They use the Poisson distribution for
 * deciding the random probability that an example is used for training, only
 * this time the parameter changes according to the boosting weight of the
 * example as it is passed through each model in sequence.
 * </p>
 *
 * <p>
 * Parameters:
 * </p>
 * <ul>
 * <li>-l : Classiﬁer to train</li>
 * <li>-s : The number of models to boost</li>
 * <li>-p : Boost with weights only; no poisson</li>
 * </ul>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public abstract class AbstractOzaBoost<MLTask extends InstanceLearner> extends AbstractEnsembleLearner<MLTask> {

	private static final long serialVersionUID = 1L;

	@Override
	public String getPurposeString() {
		return "Incremental on-line boosting of Oza and Russell.";
	}

	public AbstractOzaBoost(Class<MLTask> task, String defaultCLIString) {
		super(task);
		this.baseLearnerOption = new ClassOption("baseLearner", 'l', "Classifier to train.", task, defaultCLIString);
	}

	public FlagOption pureBoostOption = new FlagOption("pureBoost", 'p', "Boost with weights only; no poisson.");

	protected double[] scms;

	protected double[] swms;

	@Override
	public void resetLearningImpl() {
		super.resetLearningImpl();

		this.scms = new double[this.ensemble.size()];
		this.swms = new double[this.ensemble.size()];
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		double lambda_d = 1.0;
		for (int i = 0; i < this.ensemble.size(); i++) {
			double k = this.pureBoostOption.isSet() ? lambda_d : MiscUtils.poisson(lambda_d, this.classifierRandom);
			if (k > 0.0) {
				Instance weightedInst = inst.copy();
				weightedInst.setWeight(inst.weight() * k);
				this.ensemble.get(i).trainOnInstance(weightedInst);
			}
			lambda_d = updateWeight(i, inst, lambda_d);
		}
	}

	public abstract double updateWeight(int i, Instance inst, double lambda_d);

	protected double getEnsembleMemberWeight(int i) {
		double em = this.swms[i] / (this.scms[i] + this.swms[i]);
		if ((em == 0.0) || (em > 0.5)) {
			return 0.0;
		}
		double Bm = em / (1.0 - em);
		return Math.log(1.0 / Bm);
	}

	@Override
	public Prediction getPredictionForInstance(Instance inst) {
		Prediction[] predictions = new Prediction[this.ensemble.size()];
		double[] weights = new double[this.ensemble.size()];
		for (int i = 0; i < this.ensemble.size(); i++) {
			weights[i] = getEnsembleMemberWeight(i);
			if (weights[i] > 0.0) {
				predictions[i] = this.ensemble.get(i).getPredictionForInstance(inst);

			} else {
				break;
			}
		}
		return combinePredictions(predictions, weights);
	}

	public abstract Prediction combinePredictions(Prediction[] predictions, double[] weights);

	@Override
	public Prediction combinePredictions(Prediction[] prediction) {
		throw new UnsupportedOperationException(
				"This is a boosting ensemble and does not use unweighted combining of predictions.");
	}

	@Override
	public boolean isRandomizable() {
		return true;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[] { new Measurement("ensemble size", this.ensemble != null ? this.ensemble.size() : 0) };
	}

}
