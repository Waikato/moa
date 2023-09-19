/*
 *    ARFHoeffdingTree.java
 * 
 *    @author Heitor Murilo Gomes (heitor_murilo_gomes at yahoo dot com dot br)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package moa.classifiers.trees;

import com.github.javacliparser.IntOption;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.core.Utils;
import com.yahoo.labs.samoa.instances.Instance;

import java.util.ArrayList;

/**
 * Adaptive Random Forest Hoeffding Tree.
 * 
 * <p>Adaptive Random Forest Hoeffding Tree. This is the base model for the 
 * Adaptive Random Forest ensemble learner 
 * (See moa.classifiers.meta.AdaptiveRandomForest.java). This Hoeffding Tree
 * includes a subspace size k parameter, which defines the number of randomly 
 * selected features to be considered at each split. </p>
 * 
 * <p>See details in:<br> Heitor Murilo Gomes, Albert Bifet, Jesse Read, 
 * Jean Paul Barddal, Fabricio Enembreck, Bernhard Pfharinger, Geoff Holmes, 
 * Talel Abdessalem. Adaptive random forests for evolving data stream classification. 
 * In Machine Learning, DOI: 10.1007/s10994-017-5642-8, Springer, 2017.</p>
 *
 * @author Heitor Murilo Gomes (heitor_murilo_gomes at yahoo dot com dot br)
 * @version $Revision: 1 $
 */
public class ARFHoeffdingTree extends HoeffdingTree {

    private static final long serialVersionUID = 1L;
    
    public IntOption subspaceSizeOption = new IntOption("subspaceSizeSize", 'k',
            "Number of features per subset for each node split.",
            2, 1, Integer.MAX_VALUE);
    
    @Override
    public String getPurposeString() {
        return "Adaptive Random Forest Hoeffding Tree for data streams. "
                + "Base learner for AdaptiveRandomForest.";
    }

    public static class RandomLearningNode extends ActiveLearningNode {
        
        private static final long serialVersionUID = 1L;

        protected int[] listAttributes;

        protected int numAttributes;
        
        public RandomLearningNode(double[] initialClassObservations, int subspaceSize) {
            super(initialClassObservations);
            this.numAttributes = subspaceSize;
        }

        @Override
        public void learnFromInstance(Instance inst, HoeffdingTree ht) {
            this.observedClassDistribution.addToValue((int) inst.classValue(),
                    inst.weight());
            if (this.listAttributes == null) {
                // -1 to not count the class attribute
                int totalInstanceNumberOfAttributes = (inst.numAttributes()-1);
                // Check if the subspaceSize (numAttributes) is greater than the number of attributes in the instance.
                // If yes, then override numAttributes to match the maximum number of attributes. Also, there is no
                // need to randomly select features, then just add all to the listAttributes.
                if(this.numAttributes >= totalInstanceNumberOfAttributes || this.numAttributes < 0) {
                    this.numAttributes = totalInstanceNumberOfAttributes;
                    this.listAttributes = new int[this.numAttributes];
                    for (int i = 0; i < totalInstanceNumberOfAttributes ; i++)
                        this.listAttributes[i] = i;
                } else {
                    this.listAttributes = new int[this.numAttributes];
                    // Creates a list with all possible indexes
                    ArrayList<Integer> allFeatureIndexes = new ArrayList<>();
                    for (int i = 0; i < totalInstanceNumberOfAttributes ; i++)
                        allFeatureIndexes.add(i);
                    // Randomly assign attributes to the list of attributes.
                    for(int i = 0 ; i < this.listAttributes.length ; ++i) {
                        int randIndex = ht.classifierRandom.nextInt(allFeatureIndexes.size());
                        this.listAttributes[i] = allFeatureIndexes.get(randIndex);
                        allFeatureIndexes.remove(randIndex);
                    }
                }
            }

            for (int j = 0; j < this.listAttributes.length ; j++) {
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

    public static class LearningNodeNB extends RandomLearningNode {

        private static final long serialVersionUID = 1L;

        public LearningNodeNB(double[] initialClassObservations, int subspaceSize) {
            super(initialClassObservations, subspaceSize);
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

        public LearningNodeNBAdaptive(double[] initialClassObservations, int subspaceSize) {
            super(initialClassObservations, subspaceSize);
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

    public ARFHoeffdingTree() {
        this.removePoorAttsOption = null;
    }
    
    @Override
    protected LearningNode newLearningNode(double[] initialClassObservations) {
        LearningNode ret;
        int predictionOption = this.leafpredictionOption.getChosenIndex();
        if (predictionOption == 0) { //MC
            ret = new RandomLearningNode(initialClassObservations, this.subspaceSizeOption.getValue());
        } else if (predictionOption == 1) { //NB
            ret = new LearningNodeNB(initialClassObservations, this.subspaceSizeOption.getValue());
        } else { //NBAdaptive
            ret = new LearningNodeNBAdaptive(initialClassObservations, this.subspaceSizeOption.getValue());
        }
        return ret;
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }
}
