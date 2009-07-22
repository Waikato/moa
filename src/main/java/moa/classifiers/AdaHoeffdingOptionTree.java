/*
 *    AdaHoeffdingOptionTree.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
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


public class AdaHoeffdingOptionTree extends HoeffdingOptionTreeNB {

	private static final long serialVersionUID = 1L;

	public static class AdaLearningNode extends LearningNodeNB {

		private static final long serialVersionUID = 1L;

		protected double mcCorrectWeight = 0.0;

		protected double nbCorrectWeight = 0.0;

		protected double CorrectWeight = 0.0;
		
		protected double alpha = 0.2;

		public AdaLearningNode(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		@Override
		public void learnFromInstance(Instance inst, HoeffdingOptionTree hot) {
			int trueClass = (int) inst.classValue();
			boolean blCorrect = false;
			if (this.observedClassDistribution.maxIndex() == trueClass) {
				this.mcCorrectWeight += inst.weight();
				if (this.mcCorrectWeight > this.nbCorrectWeight)
					blCorrect = true;
			}
			if (Utils.maxIndex(NaiveBayes.doNaiveBayesPrediction(inst,
					this.observedClassDistribution, this.attributeObservers)) == trueClass) {
				this.nbCorrectWeight += inst.weight();
				if (this.mcCorrectWeight <= this.nbCorrectWeight)
					blCorrect = true;
			}
			if (blCorrect == true) {
				this.CorrectWeight += alpha * (1.0 - this.CorrectWeight); //EWMA
			} else {
				this.CorrectWeight -= alpha * this.CorrectWeight; //EWMA
			}
			super.learnFromInstance(inst, hot);
		}

		@Override
		public double[] getClassVotes(Instance inst, HoeffdingOptionTree ht) {
			double[] dist;			
			if (this.mcCorrectWeight > this.nbCorrectWeight) {
				dist = this.observedClassDistribution.getArrayCopy();
			} else
				dist = NaiveBayes.doNaiveBayesPrediction(inst,
					this.observedClassDistribution, this.attributeObservers);
			double distSum = Utils.sum(dist);
			if ( distSum * (1.0- this.CorrectWeight )* (1.0- this.CorrectWeight ) > 0.0) {
				Utils.normalize(dist, distSum * (1.0- this.CorrectWeight ) * (1.0- this.CorrectWeight )); //Adding weight
			}
			return dist;
		}

	}

	@Override
	protected LearningNode newLearningNode(double[] initialClassObservations) {
		return new AdaLearningNode(initialClassObservations);
	}

}
