/*
 *    HeterogeneousEnsembleAbstract.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *    @author Jan N. van Rijn (j.n.van.rijn@liacs.leidenuniv.nl)
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
package moa.classifiers.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.ListOption;
import com.github.javacliparser.Option;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

/**
 * BLAST (Best Last) for Heterogeneous Ensembles Abstract Base Class
 *
 * <p>
 * Given a set of (heterogeneous) classifiers, BLAST builds an ensemble, and
 * determines the weights of all ensemble members based on their performance on
 * recent observed instances. Used as Abstact Base Class for 
 * HeterogeneousEnsembleBlast and HeterogeneousEnsembleBlastFadingFactors. 
 * </p>
 *
 * <p>
 * J. N. van Rijn, G. Holmes, B. Pfahringer, J. Vanschoren. Having a Blast:
 * Meta-Learning and Heterogeneous Ensembles for Data Streams. In 2015 IEEE
 * International Conference on Data Mining, pages 1003-1008. IEEE, 2015.
 * </p>
 *
 * <p>
 * Parameters:
 * </p>
 * <ul>
 * <li>-b : Comma-separated string of classifiers</li>
 * <li>-g : Grace period (1 = optimal)</li>
 * <li>-k : Number of active classifiers</li>
 * </ul>
 *
 * @author Jan N. van Rijn (j.n.van.rijn@liacs.leidenuniv.nl)
 * @version $Revision: 1 $
 */
public abstract class HeterogeneousEnsembleAbstract extends AbstractClassifier implements MultiClassClassifier {

	private static final long serialVersionUID = 1L;

	public ListOption baselearnersOption = new ListOption("baseClassifiers", 'b',
			"The classifiers the ensemble consists of.",
			new ClassOption("learner", ' ', "", Classifier.class,
					"trees.HoeffdingTree"),
			new Option[] {
					new ClassOption("", ' ', "", Classifier.class, "bayes.NaiveBayes"),
					new ClassOption("", ' ', "", Classifier.class,
							"functions.Perceptron"),
					new ClassOption("", ' ', "", Classifier.class, "functions.SGD"),
					new ClassOption("", ' ', "", Classifier.class, "lazy.kNN"),
					new ClassOption("", ' ', "", Classifier.class,
							"trees.HoeffdingTree") },
			',');

	public IntOption gracePerionOption = new IntOption("gracePeriod", 'g',
			"How many instances before we reevalate the best classifier", 1, 1,
			Integer.MAX_VALUE);

	public IntOption activeClassifiersOption = new IntOption("activeClassifiers",
			'k', "The number of active classifiers (used for voting)", 1, 1,
			Integer.MAX_VALUE);

	public FlagOption weightClassifiersOption = new FlagOption(
			"weightClassifiers", 'p',
			"Uses online performance estimation to weight the classifiers");

	protected Classifier[] ensemble;

	protected double[] historyTotal;

	protected Integer instancesSeen;

	List<Integer> topK;

	@Override
	public String getPurposeString() {
		return "The model-free heterogeneous ensemble as presented in "
				+ "'Having a Blast: Meta-Learning and Heterogeneous Ensembles "
				+ "for Data Streams' (ICDM 2015).";
	}

	public int getEnsembleSize() {
		// mainly for testing, @throws exception if called before initialization
		return this.ensemble.length;
	}

	public String getMemberCliString(int idx) {
		// mainly for testing, @pre: idx < getEnsembleSize()
		return this.ensemble[idx].getCLICreationString(Classifier.class);
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		double[] votes = new double[inst.classAttribute().numValues()];

		for (int i = 0; i < topK.size(); ++i) {
			double[] memberVotes = normalize(
					ensemble[topK.get(i)].getVotesForInstance(inst));
			double weight = 1.0;

			if (weightClassifiersOption.isSet()) {
				weight = historyTotal[topK.get(i)];
			}

			// make internal classifiers so-called "hard classifiers"
			votes[maxIndex(memberVotes)] += 1.0 * weight;
		}

		return votes;
	}

	@Override
	public void setModelContext(InstancesHeader ih) {
		super.setModelContext(ih);

		for (int i = 0; i < this.ensemble.length; ++i) {
			this.ensemble[i].setModelContext(ih);
		}
	}

	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public void getModelDescription(StringBuilder arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

		Option[] learnerOptions = this.baselearnersOption.getList();
		this.ensemble = new Classifier[learnerOptions.length];
		for (int i = 0; i < learnerOptions.length; i++) {
			monitor.setCurrentActivity("Materializing learner " + (i + 1) + "...",
					-1.0);
			this.ensemble[i] = (Classifier) ((ClassOption) learnerOptions[i])
					.materializeObject(monitor, repository);
			if (monitor.taskShouldAbort()) {
				return;
			}
			monitor.setCurrentActivity("Preparing learner " + (i + 1) + "...", -1.0);
			this.ensemble[i].prepareForUse(monitor, repository);
			if (monitor.taskShouldAbort()) {
				return;
			}
		}
		super.prepareForUseImpl(monitor, repository);

		topK = topK(historyTotal, activeClassifiersOption.getValue());
	}

	protected static List<Integer> topK(double[] scores, int k) {
		double[] scoresWorking = Arrays.copyOf(scores, scores.length);

		List<Integer> topK = new ArrayList<Integer>();

		for (int i = 0; i < k; ++i) {
			int bestIdx = maxIndex(scoresWorking);
			topK.add(bestIdx);
			scoresWorking[bestIdx] = -1;
		}

		return topK;
	}

	protected static int maxIndex(double[] scores) {
		int bestIdx = 0;
		for (int i = 1; i < scores.length; ++i) {
			if (scores[i] > scores[bestIdx]) {
				bestIdx = i;
			}
		}
		return bestIdx;
	}

	protected static double[] normalize(double[] input) {
		double sum = 0.0;
		for (int i = 0; i < input.length; ++i) {
			sum += input[i];
		}
		for (int i = 0; i < input.length; ++i) {
			input[i] /= sum;
		}
		return input;
	}
}
