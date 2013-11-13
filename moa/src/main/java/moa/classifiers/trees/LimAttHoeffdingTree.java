/*
 *    LimAttHoeffdingTree.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
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
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.core.Utils;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Hoeffding decision trees with a restricted number of attributes for data
 * streams. LimAttClassifier is the stacking method that can be used with these
 * decision trees. For more information see,<br/> <br/> Albert Bifet, Eibe
 * Frank, Geoffrey Holmes, Bernhard Pfahringer: Accurate Ensembles for Data
 * Streams: Combining Restricted Hoeffding Trees using Stacking. Journal of
 * Machine Learning Research - Proceedings Track 13: 225-240 (2010) * <!--
 * technical-bibtex-start --> BibTeX:
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
 * <!-- technical-bibtex-end -->
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class LimAttHoeffdingTree extends HoeffdingTree {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Hoeffding decision trees with a restricted number of attributes for data streams.";
    }

    protected int[] listAttributes;

    public void setlistAttributes(int[] list) {
        this.listAttributes = list;
    }

    public static class LimAttLearningNode extends ActiveLearningNode {

        private static final long serialVersionUID = 1L;

        protected double weightSeenAtLastSplitEvaluation;

        protected int[] listAttributes;

        protected int numAttributes;

        public LimAttLearningNode(double[] initialClassObservations) {
            super(initialClassObservations);
        }

        public void setlistAttributes(int[] list) {
            this.listAttributes = list;
            this.numAttributes = list.length;
        }

        @Override
        public void learnFromInstance(Instance inst, HoeffdingTree ht) {
            this.observedClassDistribution.addToValue((int) inst.classValue(),
                    inst.weight());
            if (this.listAttributes == null) {
                setlistAttributes(((LimAttHoeffdingTree) ht).listAttributes);
            }

            for (int j = 0; j < this.numAttributes; j++) {
                int i = this.listAttributes[j];
                int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
                AttributeClassObserver obs = this.attributeObservers.get(i);
                if (obs == null) {
                    obs = inst.attribute(instAttIndex).isNominal() ? ht.newNominalClassObserver() : ht.newNumericClassObserver();
                    this.attributeObservers.set(i, obs);
                }
                obs.observeAttributeClass(inst.value(instAttIndex), (int) inst.classValue(), inst.weight());
            }
        }
    }

    public LimAttHoeffdingTree() {
        this.removePoorAttsOption = null;
    }

    public static class LearningNodeNB extends LimAttLearningNode {

        private static final long serialVersionUID = 1L;

        public LearningNodeNB(double[] initialClassObservations) {
            super(initialClassObservations);
        }

        @Override
        public double[] getClassVotes(Instance inst, HoeffdingTree ht) {
            if (getWeightSeen() >= ht.nbThresholdOption.getValue()) {
                return NaiveBayes.doNaiveBayesPrediction(inst,
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
            for (int i = 0; i < ret.length; i++) {
                ret[i] *= this.observedClassDistribution.sumOfValues();
            }
            return ret;
        }
    }

    @Override
    protected LearningNode newLearningNode(double[] initialClassObservations) {
        LearningNode ret;
        int predictionOption = this.leafpredictionOption.getChosenIndex();
        if (predictionOption == 0) { //MC
            ret = new LimAttLearningNode(initialClassObservations);
        } else if (predictionOption == 1) { //NB
            ret = new LearningNodeNB(initialClassObservations);
        } else { //NBAdaptive
            ret = new LearningNodeNBAdaptive(initialClassObservations);
        }
        return ret;
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }
}
