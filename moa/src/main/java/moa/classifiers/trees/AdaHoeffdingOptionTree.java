/*
 *    AdaHoeffdingOptionTree.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
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
package moa.classifiers.trees;

import moa.classifiers.bayes.NaiveBayes;
import moa.core.Utils;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Adaptive decision option tree for streaming data with adaptive Naive
 * Bayes classification at leaves.
 * An Adaptive Hoeffding Option Tree is a Hoeffding Option Tree with the
 * following improvement: each leaf stores an estimation of the current error.
 * It uses an EWMA estimator with alpha = .2. The weight of each node in the
 * voting process is proportional to the square of the inverse of the error.
 * <br/><br/>
 * Example:<br/>
 * <code>AdaHoeffdingOptionTree -o 50 </code>
 * Parameters:<ul>
 * <li>Same parameters as <code>HoeffdingOptionTreeNB<code></ul>
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class AdaHoeffdingOptionTree extends HoeffdingOptionTree {
    
    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Adaptive decision option tree for streaming data with adaptive Naive Bayes classification at leaves.";
    }
     
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
                if (this.mcCorrectWeight > this.nbCorrectWeight) {
                    blCorrect = true;
                }
            }
            if (Utils.maxIndex(NaiveBayes.doNaiveBayesPrediction(inst,
                    this.observedClassDistribution, this.attributeObservers)) == trueClass) {
                this.nbCorrectWeight += inst.weight();
                if (this.mcCorrectWeight <= this.nbCorrectWeight) {
                    blCorrect = true;
                }
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
            } else {
                dist = NaiveBayes.doNaiveBayesPrediction(inst,
                        this.observedClassDistribution, this.attributeObservers);
            }
            double distSum = Utils.sum(dist);
            if (distSum * (1.0 - this.CorrectWeight) * (1.0 - this.CorrectWeight) > 0.0) {
                Utils.normalize(dist, distSum * (1.0 - this.CorrectWeight) * (1.0 - this.CorrectWeight)); //Adding weight
            }
            return dist;
        }
    }

    @Override
    protected LearningNode newLearningNode(double[] initialClassObservations) {
        return new AdaLearningNode(initialClassObservations);
    }
}
