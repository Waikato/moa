/*
 *    LimAttHoeffdingTreeNBAdaptive.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.classifiers;

import weka.core.Instance;
import weka.core.Utils;

/**
 * Hoeffding decision trees with a limited number of attributes,
 * with majority class and naive Bayes learners at the leaves.
 * It uses for each leaf the classifier with higher accuracy.
 * LimAttClassifier is the stacking method that can be used with these decision trees.
 * For more information see,<br/>
 * <br/>
 * Albert Bifet, Eibe Frank, Geoffrey Holmes, Bernhard Pfahringer: Accurate
 * Ensembles for Data Streams: Combining Restricted Hoeffding Trees using Stacking.
 * Journal of Machine Learning Research - Proceedings Track 13: 225-240 (2010)
 *
<!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{BifetFHP10,
 * author    = {Albert Bifet and
 *              Eibe Frank and
 *              Geoffrey Holmes and
 *              Bernhard Pfahringer},
 * title     = {Accurate Ensembles for Data Streams: Combining Restricted
 *              Hoeffding Trees using Stacking},
 * journal   = {Journal of Machine Learning Research - Proceedings Track},
 * volume    = {13},
 * year      = {2010},
 * pages     = {225-240}
 * }
 * </pre>
 * <p/>
<!-- technical-bibtex-end -->
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class LimAttHoeffdingTreeNBAdaptive extends LimAttHoeffdingTreeNB {

	private static final long serialVersionUID = 1L;

	public static class LearningNodeNBAdaptive extends LearningNodeNB {

		private static final long serialVersionUID = 1L;

		protected double mcCorrectWeight = 0.0;

		protected double nbCorrectWeight = 0.0;

		public LearningNodeNBAdaptive(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		@Override
		public void learnFromInstance(Instance inst, HoeffdingTree ht) {
			int trueClass = (int) inst.classValue();
			if (this.observedClassDistribution.maxIndex() == trueClass) {
				this.mcCorrectWeight += inst.weight();
			}
			if (Utils.maxIndex(NaiveBayes.doNaiveBayesPrediction(inst,
					this.observedClassDistribution, this.attributeObservers)) == trueClass) {
				this.nbCorrectWeight += inst.weight();
			}
			super.learnFromInstance(inst, ht);
		}

		@Override
		public double[] getClassVotes(Instance inst, HoeffdingTree ht) {
			if (this.mcCorrectWeight > this.nbCorrectWeight) {
				return this.observedClassDistribution.getArrayCopy();
			}
			double ret[] = NaiveBayes.doNaiveBayesPrediction(inst,
					this.observedClassDistribution, this.attributeObservers);
			for ( int i = 0; i<ret.length; i++) {
				ret[i] *= this.observedClassDistribution.sumOfValues();
			}
			return ret;
		}

	}

	@Override
	protected LearningNode newLearningNode(double[] initialClassObservations) {
		return new LearningNodeNBAdaptive(initialClassObservations);
	}

}
