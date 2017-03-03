/*
 *    HoeffdingAdaptiveTreeClassifLeaves.java
 *    Copyright (C) 2016 Pontifical Catholic University of Paraná, Curitiba, Brazil
 *    @author Jean Paul Barddal (jean.barddal@ppgia.pucpr.br)
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

import com.yahoo.labs.samoa.instances.Instance;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import moa.classifiers.Classifier;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.trees.HoeffdingAdaptiveTree;
import moa.classifiers.trees.HoeffdingTree;
import moa.options.ClassOption;

/**
 * Hoeffding Adaptive Tree for evolving data streams that has a classifier at
 * the leaves.
 *
 * @version 1.0
 * @see First used in the data stream configuration in J. P. Barddal, H. M.
 * Gomes, F. Enembreck, B. Pfahringer & A. Bifet. ON DYNAMIC FEATURE WEIGHTING
 * FOR FEATURE DRIFTING DATA STREAMS. In European Conference on Machine Learning
 * and Principles and Practice of Knowledge Discovery (ECML/PKDD'16). 2016.
 */
public class HoeffdingAdaptiveTreeClassifLeaves extends HoeffdingAdaptiveTree {

    public ClassOption leaveLearnerOption = new ClassOption("leaveLearner", 'a',
	    "Classifier to train.", Classifier.class, "bayes.NaiveBayes");

    public HoeffdingAdaptiveTreeClassifLeaves() {
	this.removePoorAttsOption = null;
    }

    @Override
    protected LearningNode newLearningNode(double[] initialClassObservations) {
	return new LearningNodeHATClassifier(initialClassObservations, null, this);
    }

    //@Override
    protected LearningNode newLearningNode(double[] initialClassObservations, Classifier cl) {
	return new LearningNodeHATClassifier(initialClassObservations, cl, this);
    }

    @Override
    protected void attemptToSplit(ActiveLearningNode node,
	    SplitNode parent,
	    int parentIndex) {
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

		if ((bestSuggestion.merit - secondBestSuggestion.merit > hoeffdingBound)
			|| (hoeffdingBound < this.tieThresholdOption.getValue())) {
		    shouldSplit = true;
		}

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
			Node newChild = newLearningNode(splitDecision.resultingClassDistributionFromSplit(i), ((LearningNodeHATClassifier) node).getClassifier());
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

    public class LearningNodeHATClassifier extends AdaLearningNode  {

	protected Classifier classifier;

	public LearningNodeHATClassifier(double[] initialClassObservations) {
	    super(initialClassObservations);
	}

	public LearningNodeHATClassifier(double[] initialClassObservations,
		Classifier cl,
		HoeffdingAdaptiveTreeClassifLeaves ht) {
	    super(initialClassObservations);

	    if (cl == null) {
		this.classifier = (Classifier) getPreparedClassOption(ht.leaveLearnerOption);
	    } else {
		this.classifier = cl.copy();
	    }
	}

	@Override
	public double[] getClassVotes(Instance inst, HoeffdingTree ht) {
	    if (getWeightSeen() >= ((HoeffdingAdaptiveTreeClassifLeaves) ht).nbThresholdOption.getValue()) {
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

}
