/*
 *    LimAttHoeffdingTreeNB.java
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

import moa.options.IntOption;
import weka.core.Instance;

/**
 * Hoeffding decision trees with a limited number of attributes for data streams that uses naive Bayes learners at the leaves.
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
public class LimAttHoeffdingTreeNB extends LimAttHoeffdingTree {

	private static final long serialVersionUID = 1L;

	public IntOption nbThresholdOption = new IntOption(
			"nbThreshold",
			'q',
			"The number of instances a leaf should observe before permitting Naive Bayes.",
			0, 0, Integer.MAX_VALUE);

	public static class LearningNodeNB extends LimAttLearningNode {

		private static final long serialVersionUID = 1L;

		public LearningNodeNB(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		@Override
		public double[] getClassVotes(Instance inst, HoeffdingTree ht) {
			if (getWeightSeen() >= ((HoeffdingTreeNB) ht).nbThresholdOption
					.getValue()) {
				return NaiveBayes
						.doNaiveBayesPrediction(inst,
								this.observedClassDistribution,
								this.attributeObservers);
			}
			return super.getClassVotes(inst, ht);
		}

		@Override
		public void disableAttribute(int attIndex) {
			// should not disable poor atts - they are used in NB calc
		}

	}

	public LimAttHoeffdingTreeNB() {
		this.removePoorAttsOption = null;
	}

	@Override
	protected LearningNode newLearningNode(double[] initialClassObservations) {
		return new LearningNodeNB(initialClassObservations);
	}

}
