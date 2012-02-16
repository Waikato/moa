/*
 *    HoeffdingTreeNBAdaptive.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.classifiers.trees;

import moa.classifiers.bayes.NaiveBayes;
import weka.core.Instance;
import weka.core.Utils;

/**
 * Hoeffding Tree with majority class and naive Bayes learners at the leaves.
 * It uses for each leaf the classifier with higher accuracy.
 *
 * <p>This adaptive Naive Bayes prediction method monitors the error rate of
 * majority class and Naive Bayes decisions in every leaf, and chooses
 * to employ Naive Bayes decisions only where they have been more accurate
 * in past cases.</p>
 * <ul>
 * <li> Same parameters as HoeffdingTreeNB</li>
 * </ul>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class HoeffdingTreeNBAdaptive extends HoeffdingTreeNB {

    private static final long serialVersionUID = 1L;
    
    @Override
    public String getPurposeString() {
        return "Hoeffding Tree with majority class and naive Bayes learners at the leaves.";
    }
    
    
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
            return NaiveBayes.doNaiveBayesPrediction(inst,
                    this.observedClassDistribution, this.attributeObservers);
        }
    }

    @Override
    protected LearningNode newLearningNode(double[] initialClassObservations) {
        return new LearningNodeNBAdaptive(initialClassObservations);
    }
}
