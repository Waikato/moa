/*
 *    HoeffdingTreeClassifLeaves.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jesse@tsc.uc3m.es)
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import moa.classifiers.Classifier;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.trees.HoeffdingTree;
import moa.options.ClassOption;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Hoeffding Tree that have a classifier at the leaves.
 *
 * A Hoeffding tree is an incremental, anytime decision tree induction algorithm
 * that is capable of learning from massive data streams, assuming that the
 * distribution generating examples does not change over time.
 * 
 * 
 */ 
public class HoeffdingTreeClassifLeaves extends HoeffdingTree {

    private static final long serialVersionUID = 1L;

    public ClassOption learnerOption = new ClassOption("learner", 'a',
            "Classifier to train.", Classifier.class, "bayes.NaiveBayes");

    public class LearningNodeClassifier extends ActiveLearningNode {

        protected Classifier classifier;

        private static final long serialVersionUID = 1L;

        public LearningNodeClassifier(double[] initialClassObservations) {
            super(initialClassObservations);
        }

        public LearningNodeClassifier(double[] initialClassObservations, Classifier cl, HoeffdingTreeClassifLeaves ht) {
            super(initialClassObservations);
            //public void LearningNodeClassifier1(double[] initialClassObservations, Classifier cl, HoeffdingTreeClassifLeaves ht ) {

            if (cl == null) {
                this.classifier = (Classifier) getPreparedClassOption(ht.learnerOption);
            } else {
                this.classifier = cl.copy();
            }
        }
	
        @Override
        public double[] getClassVotes(Instance inst, HoeffdingTree ht) {
            if (getWeightSeen() >= ((HoeffdingTreeClassifLeaves) ht).nbThresholdOption.getValue()) {
                return this.classifier.getVotesForInstance(inst);
            }
            return super.getClassVotes(inst, ht);
        }

        @Override
        public void disableAttribute(int attIndex) {
            // should not disable poor atts - they are used in NB calc
        }

        @Override
        public void learnFromInstance(Instance inst, HoeffdingTree ht) {
            this.classifier.trainOnInstance(inst);
            super.learnFromInstance(inst, ht);
        }

        public Classifier getClassifier() {
            return this.classifier;
        }
    }

    public HoeffdingTreeClassifLeaves() {
        this.removePoorAttsOption = null;
    }

    @Override
    protected LearningNode newLearningNode(double[] initialClassObservations) {
        return new LearningNodeClassifier(initialClassObservations, null, this);
    }

    //@Override
    protected LearningNode newLearningNode(double[] initialClassObservations, Classifier cl) {
        return new LearningNodeClassifier(initialClassObservations, cl, this);
    }

    @Override
    protected void attemptToSplit(ActiveLearningNode node, SplitNode parent,
            int parentIndex) {
        //ßSystem.out.println("Attempt to Split");
        if (!node.observedClassDistributionIsPure()) {
            SplitCriterion splitCriterion = (SplitCriterion) getPreparedClassOption(this.splitCriterionOption);
            AttributeSplitSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion, this);
            Arrays.sort(bestSplitSuggestions);
            boolean shouldSplit = false;
            if (bestSplitSuggestions.length < 2) {
                shouldSplit = bestSplitSuggestions.length > 0;
            } else {
                double hoeffdingBound = computeHoeffdingBound(splitCriterion.getRangeOfMerit(node.getObservedClassDistribution()),
                        this.splitConfidenceOption.getValue(), node.getWeightSeen());
                AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
                AttributeSplitSuggestion secondBestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 2];
               // System.out.println(bestSuggestion.merit+" - "+secondBestSuggestion.merit+":"+
               //         (bestSuggestion.merit - secondBestSuggestion.merit)+" > "+hoeffdingBound+ "<"+this.tieThresholdOption.getValue());
                if ((bestSuggestion.merit - secondBestSuggestion.merit > hoeffdingBound)
                        || (hoeffdingBound < this.tieThresholdOption.getValue())) {
                    shouldSplit = true;
                }
                // }
                if ((this.removePoorAttsOption != null)
                        && this.removePoorAttsOption.isSet()) {
                    Set<Integer> poorAtts = new HashSet<Integer>();
                    // scan 1 - add any poor to set
                    for (int i = 0; i < bestSplitSuggestions.length; i++) {
                        if (bestSplitSuggestions[i].splitTest != null) {
                            int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
                            if (splitAtts.length == 1) {
                                if (bestSuggestion.merit
                                        - bestSplitSuggestions[i].merit > hoeffdingBound) {
                                    poorAtts.add(new Integer(splitAtts[0]));
                                }
                            }
                        }
                    }
                    // scan 2 - remove good ones from set
                    for (int i = 0; i < bestSplitSuggestions.length; i++) {
                        if (bestSplitSuggestions[i].splitTest != null) {
                            int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
                            if (splitAtts.length == 1) {
                                if (bestSuggestion.merit
                                        - bestSplitSuggestions[i].merit < hoeffdingBound) {
                                    poorAtts.remove(new Integer(splitAtts[0]));
                                }
                            }
                        }
                    }
                    for (int poorAtt : poorAtts) {
                        node.disableAttribute(poorAtt);
                    }
                }
            }
            if (shouldSplit) {
                AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];
                if (splitDecision.splitTest == null) {
                    // preprune - null wins
                    deactivateLearningNode(node, parent, parentIndex);
                } else {
                    SplitNode newSplit = newSplitNode(splitDecision.splitTest,
                            node.getObservedClassDistribution());
                    for (int i = 0; i < splitDecision.numSplits(); i++) {
                        //Unique Change of HoeffdingTree
                        Node newChild = newLearningNode(splitDecision.resultingClassDistributionFromSplit(i), ((LearningNodeClassifier) node).getClassifier());
                        newSplit.setChild(i, newChild);
                    }
                    this.activeLeafNodeCount--;
                    this.decisionNodeCount++;
                    this.activeLeafNodeCount += splitDecision.numSplits();
                    if (parent == null) {
                        this.treeRoot = newSplit;
                    } else {
                        parent.setChild(parentIndex, newSplit);
                    }
                }
                // manage memory
                enforceTrackerLimit();
            }
        }
    }
}
