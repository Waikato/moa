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

import java.io.StringReader;
import java.util.List;
import moa.classifiers.Classifier;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.trees.HoeffdingTree;
import moa.core.InstancesHeader;
import moa.core.utils.Converter;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Hoeffding Tree for classifying multi-label data.
 *
 * A Hoeffding tree is an incremental, anytime decision tree induction algorithm
 * that is capable of learning from massive data streams, assuming that the
 * distribution generating examples does not change over time.
 * 
 * 
 */ 
public class MultilabelHoeffdingTree extends HoeffdingTreeClassifLeaves { 
// Needs to use InfoGainSplitCriterionMultiLabel, since multilabel entropy is calculated in a different way 
// Trains a mlinstance adding statistics of several class values and training node classifiers
// Get votes from the training node classifier

	private static final long serialVersionUID = 1L;

	public int m_L = -1;

	// Converts multi-label format to single-label format
	protected Converter converter = null;
	
	@Override
	public void setModelContext(InstancesHeader raw_header) {
		//set the multilabel model context
		this.modelContext = raw_header;
	}

	//It uses several class values
	public static class MultilabelInactiveLearningNode extends InactiveLearningNode {

		private static final long serialVersionUID = 1L;

		public MultilabelInactiveLearningNode(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		@Override
		public void learnFromInstance(Instance inst, HoeffdingTree ht) {
			List<Integer> labels = ((MultilabelHoeffdingTree) ht).converter.getRelevantLabels(inst);
			for (int l : labels){
				this.observedClassDistribution.addToValue( l, inst.weight());
			}
		}
	}
	
	// It uses classifier at nodes, and to be able to train with several class values
	public class MultilabelLearningNodeClassifier extends LearningNodeClassifier {
		
		//protected Classifier classifier; 

		private static final long serialVersionUID = 1L;

		public MultilabelLearningNodeClassifier(double[] initialClassObservations, Classifier cl, MultilabelHoeffdingTree ht ) {
			super(initialClassObservations);
		
			if (cl== null) {
				this.classifier = ((Classifier) getPreparedClassOption(ht.learnerOption)).copy();
				this.classifier.resetLearning();

				InstancesHeader raw_header = ht.getModelContext();
				this.classifier.setModelContext(raw_header);
			}
			else{
				this.classifier = cl.copy();
			}
		}

		@Override
		public double[] getClassVotes(Instance inst, HoeffdingTree ht) {

			if (this.classifier == null) {
				return new double[((MultilabelHoeffdingTree) ht).converter.getL()];
			}
			return this.classifier.getVotesForInstance(inst); 			
		}

		@Override
		public void disableAttribute(int attIndex) {
			// should not disable poor atts - they are used in NB calc
		}
		
		public Classifier getClassifier() {
			return this.classifier;
		}
		
		//It uses different class values
	    @Override
		public void learnFromInstance(Instance mlinst, HoeffdingTree ht) {
			this.classifier.trainOnInstance(mlinst);  
			MultilabelHoeffdingTree mht = ((MultilabelHoeffdingTree) ht);
			List<Integer> labels = mht.converter.getRelevantLabels(mlinst);
			for (int l : labels){
				this.observedClassDistribution.addToValue( l, mlinst.weight());
			}
			Instance inst = mht.converter.formatInstance(mlinst);
			for (int i = 0; i < inst.numAttributes() - 1; i++) {
			//for (int i = 1; i < inst.numAttributes(); i++) {
				int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
				AttributeClassObserver obs = this.attributeObservers.get(i);
				if (obs == null) {
					obs = inst.attribute(instAttIndex).isNominal() ? mht.newNominalClassObserver() : mht.newNumericClassObserver();
					this.attributeObservers.set(i, obs);
				}
				for (int l : labels){
					obs.observeAttributeClass(inst.value(instAttIndex), l, inst.weight());
					//obs.observeAttributeClass(inst.value(instAttIndex), 0, inst.weight());
				}
			}
		}
	}

	public MultilabelHoeffdingTree() {
		this.removePoorAttsOption = null;
	}

	@Override
	protected LearningNode newLearningNode(double[] initialClassObservations) {
		// Create new Learning Node null
		return new MultilabelLearningNodeClassifier(initialClassObservations,null,this);
	}
	
	//@Override
	protected LearningNode newLearningNode(double[] initialClassObservations, Classifier cl) {
		// Create new Learning Node
		return new MultilabelLearningNodeClassifier(initialClassObservations,cl,this);
	}
	
	//It uses MultilabelInactiveLearningNode since there are several class values
	@Override
	protected void deactivateLearningNode(ActiveLearningNode toDeactivate,
			SplitNode parent, int parentBranch) {
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
	public double[] getVotesForInstance(Instance inst) {

		int L = inst.classIndex()+1;
		if (m_L != L) {
			// Update class labels
			m_L = L;
			// Create a converter, and its template
			converter = new Converter(m_L);
			try {
				converter.createTemplate(new Instances(new StringReader(this.modelContext.toString()),0));
			} catch(Exception e) {
				System.err.println("Error, failed to create a multi-label Instances template with L = "+m_L);
				System.out.println("Instances: "+this.modelContext.toString());
				e.printStackTrace();
				System.exit(1);
			}
		}

		if (this.treeRoot != null) {
			FoundNode foundNode = this.treeRoot.filterInstanceToLeaf(inst, null, -1);
			Node leafNode = foundNode.node;
			if (leafNode == null) {
				leafNode = foundNode.parent;
			}
			//System.out.println("y[] = "+Arrays.toString(leafNode.getClassVotes(inst,this)));
			return leafNode.getClassVotes(inst, this);
		}
		// Return empty array (this should only happen once! -- before we build the root node).
		return new double[this.m_L];
	}
}
