/*
 *    MultilabelHoeffdingTree.java
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
package moa.classifiers.multilabel;

import moa.classifiers.trees.HoeffdingTreeClassifLeaves;
import java.io.StringReader;
import java.util.List;
import moa.classifiers.Classifier;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.trees.HoeffdingTree;
import moa.core.StringUtils;
import moa.core.utils.Converter;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import java.util.LinkedList;
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.core.Example;
import java.util.Arrays;

/**
 * Hoeffding Tree for classifying multi-label data.
 *
 * A Hoeffding tree is an incremental, anytime decision tree induction algorithm
 * that is capable of learning from massive data streams, assuming that the
 * distribution generating examples does not change over time.
 * 
 * 
 */ 
public class MultilabelHoeffdingTree extends HoeffdingTreeClassifLeaves implements MultiLabelLearner, MultiTargetRegressor { 
// Needs to use InfoGainSplitCriterionMultiLabel, since multilabel entropy is calculated in a different way 
// Trains a mlinstance adding statistics of several class values and training node classifiers
// Get votes from the classifier in the learning node of the tree

	private static final long serialVersionUID = 1L;


	/*public MultilabelHoeffdingTree() {
		this.removePoorAttsOption = null;
	}*/

	@Override
	protected LearningNode newLearningNode(double[] initialClassObservations) {
		// Create new Learning Node with a null classifier
		return new MultilabelLearningNodeClassifier(initialClassObservations,null,this);
	}

	//@Override
	protected LearningNode newLearningNode(double[] initialClassObservations, Classifier cl) {
		// Create new Learning Node
		return new MultilabelLearningNodeClassifier(initialClassObservations,cl,this);
	}

	public static class MultilabelInactiveLearningNode extends InactiveLearningNode {

		private static final long serialVersionUID = 1L;

		public MultilabelInactiveLearningNode(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		@Override
		public void learnFromInstance(Instance inst, HoeffdingTree ht) {
			List<Integer> labels = ((MultilabelHoeffdingTree) ht).getRelevantLabels(inst);
			for (int l : labels){
				this.observedClassDistribution.addToValue( l, inst.weight());
			}
		}
	}

	public class MultilabelLearningNodeClassifier extends LearningNodeClassifier {
		// It uses classifier at nodes, and to be able to train with several class values

		private static final long serialVersionUID = 1L;

		public MultilabelLearningNodeClassifier(double[] initialClassObservations, Classifier cl, MultilabelHoeffdingTree ht ) {
			super(initialClassObservations);
		
			if (cl== null) {
				MEKAClassifier learner = new MEKAClassifier();
				learner.baseLearnerOption.setValueViaCLIString("meka.classifiers.multilabel.incremental.PSUpdateable");
				learner.prepareForUse();
				learner.setModelContext(ht.getModelContext());

				this.classifier = learner;
				this.classifier.resetLearning();
			}
			else{
				this.classifier = cl.copy();
			}
		}

		@Override
		public double[] getClassVotes(Instance inst, HoeffdingTree ht) {

			return this.classifier.getVotesForInstance(inst); 			
		}

		public Prediction getPredictionForInstance(Instance inst, HoeffdingTree ht) {

			return this.classifier.getPredictionForInstance(inst);
		}

		@Override
		public void disableAttribute(int attIndex) {
			// should not disable poor atts - they are used in NB calc
		}
		
		public Classifier getClassifier() {
			return this.classifier;
		}

	    @Override
		public void learnFromInstance(Instance inst, HoeffdingTree ht) {

			//It uses different class values, not only one
			this.classifier.trainOnInstance(inst);
			MultilabelHoeffdingTree mht = ((MultilabelHoeffdingTree) ht);
			List<Integer> labels = mht.getRelevantLabels(inst);
			for (int l : labels){
				this.observedClassDistribution.addToValue( l, inst.weight());
			}

			for (int i = 0; i < inst.numInputAttributes(); i++) {
				int instAttIndex = i;
				AttributeClassObserver obs = this.attributeObservers.get(instAttIndex); //i
				if (obs == null) {
					obs = inst.inputAttribute(i).isNominal() ? mht.newNominalClassObserver() : mht.newNumericClassObserver();
					this.attributeObservers.set(i, obs);
				}
				for (int l : labels){
					obs.observeAttributeClass(inst.valueInputAttribute(i), l, inst.weight());
				}
			}
		}

		public void describeSubtree(HoeffdingTree ht, StringBuilder out,
									int indent) {
			StringUtils.appendIndented(out, indent, "Leaf ");
			out.append(" = ");
			out.append(" weights: ");
			this.observedClassDistribution.getSingleLineDescription(out,
					this.observedClassDistribution.numValues());
			StringUtils.appendNewline(out);
		}
	}

	@Override
	protected void deactivateLearningNode(ActiveLearningNode toDeactivate,
			SplitNode parent, int parentBranch) {
		//It uses MultilabelInactiveLearningNode since there are several class values
		Node newLeaf = new MultilabelInactiveLearningNode(toDeactivate
				.getObservedClassDistribution());
		if (parent == null) {
			this.treeRoot = newLeaf;
		} else {
			parent.setChild(parentBranch, newLeaf);
		}
		this.activeLeafNodeCount--;
		this.inactiveLeafNodeCount++;
	}


	@Override
	public Prediction getPredictionForInstance(Example<Instance> example) {
		return getPredictionForInstance((MultiLabelInstance)example.getData());
	}

	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance inst){

		if (this.treeRoot != null) {
			FoundNode foundNode = this.treeRoot.filterInstanceToLeaf(inst, null, -1);
			Node leafNode = (MultilabelLearningNodeClassifier) foundNode.node;
			MultilabelLearningNodeClassifier multilabelLeafNode = (MultilabelLearningNodeClassifier) leafNode;
			if (leafNode == null) {
				leafNode = foundNode.parent;
			}
			return multilabelLeafNode.getPredictionForInstance(inst, this);
		}
		else {
			System.err.println("[WARNING] Root Node == Null !!!!!!");
		}

		// Return empty array (this should only happen once! -- before we build the root node).
		return null;
	}

	@Override
	public void trainOnInstance(Instance inst) {
		boolean isTraining = (inst.weight() > 0.0);
		if (isTraining) {
			this.trainingWeightSeenByModel += inst.weight();
			trainOnInstanceImpl((MultiLabelInstance) inst);
		}
	}

	@Override
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		trainOnInstanceImpl((Instance) instance);
	}
        
	public static List<Integer> getRelevantLabels(Instance x) {
		List<Integer> classValues = new LinkedList<Integer>();
		//get all class attributes
		for (int j = 0; j < x.numberOutputTargets(); j++) {
			if (x.classValue(j) > 0.0) {
				classValues.add(j);
			}
		}
		return classValues;
	}

}
