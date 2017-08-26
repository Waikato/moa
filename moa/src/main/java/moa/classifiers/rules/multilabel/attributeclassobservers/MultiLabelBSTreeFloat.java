/*
 *    MultiLabelBSTreeFloat.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
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
 *
 */
package moa.classifiers.rules.multilabel.attributeclassobservers;

import java.io.Serializable;

import moa.classifiers.rules.core.NumericRulePredicate;
import moa.classifiers.rules.core.Utils;
import moa.classifiers.rules.multilabel.core.AttributeExpansionSuggestion;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.IntOption;

public class MultiLabelBSTreeFloat extends AbstractOptionHandler implements NumericStatisticsObserver {

	/**
	 * 
	 */
	public IntOption maxNodesOption = new IntOption("maxNodesOption", 'z', "Maximum number of nodes", 50, 0, Integer.MAX_VALUE);
	protected int maxNodes;
	protected int numNodes;

	private static final long serialVersionUID = 1L;

	protected Node root = null;

	protected DoubleVector [] leftStatistics;
	protected DoubleVector [] rightStatistics;

	@Override
	public void observeAttribute(double inputAttributeValue,
			DoubleVector[] statistics) {
		if (!Double.isNaN(inputAttributeValue))
		{
			if (this.root == null) {
				this.root = new Node(inputAttributeValue, statistics);
				maxNodes=maxNodesOption.getValue();
			} else {
				this.root.observeAttribute(inputAttributeValue, statistics);
			}
		}

	}


	@Override
	public AttributeExpansionSuggestion getBestEvaluatedSplitSuggestion(
			MultiLabelSplitCriterion criterion, DoubleVector[] preSplitStatistics, int inputAttributeIndex) {
		// Initialize global variables
		int numOutputs=preSplitStatistics.length;
		leftStatistics=new DoubleVector[numOutputs];
		rightStatistics=new DoubleVector[numOutputs];
		for (int i=0; i< numOutputs; i++)
		{
			leftStatistics[i]=new DoubleVector(new double [preSplitStatistics[i].numValues()]); //sets statistics to zeros
			rightStatistics[i]=new DoubleVector(preSplitStatistics[i]);
		}
		AttributeExpansionSuggestion ret=searchForBestSplitOption(this.root, null, criterion, preSplitStatistics, inputAttributeIndex);
		leftStatistics=null;
		rightStatistics=null;
		return ret;
	}

	protected AttributeExpansionSuggestion searchForBestSplitOption(Node currentNode, AttributeExpansionSuggestion currentBestOption, MultiLabelSplitCriterion criterion, DoubleVector [] preSplitStatistics, int inputAttributeIndex) {
		// Return null if the current node is null or we have finished looking through all the possible splits
		if (currentNode == null) { // TODO: JD check || countRightTotal == 0.0
			return currentBestOption;
		}

		if (currentNode.left != null) {
			currentBestOption = searchForBestSplitOption(currentNode.left, currentBestOption, criterion, preSplitStatistics, inputAttributeIndex);
		}
		for (int i=0; i<leftStatistics.length; i++)
		{
			DoubleVector leftStatsDouble=Utils.floatToDoubleVector(currentNode.leftStatistics[i]);
			leftStatistics[i].addValues(leftStatsDouble);
			rightStatistics[i].subtractValues(leftStatsDouble);
		}

		DoubleVector[][] postSplitDists = new DoubleVector [leftStatistics.length][2];
		for (int i=0; i<leftStatistics.length; i++)
		{
			postSplitDists[i]= new DoubleVector[2];
			postSplitDists[i][0]=leftStatistics[i];
			postSplitDists[i][1]=rightStatistics[i];
		}

		double merit = criterion.getMeritOfSplit(preSplitStatistics, postSplitDists);

		if ((currentBestOption == null) || (merit > currentBestOption.merit)) {
			currentBestOption= new AttributeExpansionSuggestion(new NumericRulePredicate(inputAttributeIndex, currentNode.cutPoint, true), Utils.copy(postSplitDists), merit);
		}

		if (currentNode.right != null) {
			currentBestOption = searchForBestSplitOption(currentNode.right, currentBestOption, criterion, preSplitStatistics, inputAttributeIndex);
		}
		for (int i=0; i<leftStatistics.length; i++)
		{
			DoubleVector leftStatsDouble=Utils.floatToDoubleVector(currentNode.leftStatistics[i]);
			leftStatistics[i].subtractValues(leftStatsDouble);
			rightStatistics[i].addValues(leftStatsDouble);
		}
		return currentBestOption;
	}




	@Override
	public String getPurposeString() {
		return "Stores statistics for all output attributes for a giver input attribute.";
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {	
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

	}


	//Inner classes inspired in FIMTDDNumericalAttributeClassObserver

	protected class Node implements Serializable {

		private static final long serialVersionUID = 1L;

		// The split point to use
		private float cutPoint;

		// E-BST statistics
		private SingleVector [] leftStatistics;
		private SingleVector [] rightStatistics;

		// Child nodes
		private Node left;
		private Node right;

		public Node(float inputAttributeValue, SingleVector [] statistics) {
			cutPoint = inputAttributeValue;
			int numOutputAttributes=statistics.length;
			leftStatistics=new SingleVector[numOutputAttributes];
			for (int i=0; i<numOutputAttributes; i++)
			{					
				leftStatistics[i]=statistics[i];;
			}
			//this.leftStatistics=statistics.clone();

			this.rightStatistics=new SingleVector[numOutputAttributes];
			for (int i=0; i<numOutputAttributes; i++)
				this.rightStatistics[i]= new SingleVector();
		}
		
		public Node(double inputAttributeValue, DoubleVector [] statistics) {
			this((float)inputAttributeValue, Utils.copyAsFloatVector(statistics));
		}

		/**
		 * Updates tree with new observation
		 */
		public void observeAttribute(double inputAttributeValue, DoubleVector [] statistics) {
			this.observeAttribute((float)inputAttributeValue, Utils.copyAsFloatVector(statistics));
			
		}
		public void observeAttribute(float inputAttributeValue, SingleVector [] statistics) {
			if (inputAttributeValue == this.cutPoint) {
				for (int i=0; i<statistics.length; i++)
					this.leftStatistics[i].addValues(statistics[i]);
			}
			else if (inputAttributeValue < this.cutPoint) {
				for (int i=0; i<statistics.length; i++)
					this.leftStatistics[i].addValues(statistics[i]);
				
				if (this.left == null) {
					if(numNodes<maxNodes){
						this.left = new Node(inputAttributeValue, statistics);
						++numNodes;
					}
				} else {
					this.left.observeAttribute(inputAttributeValue, statistics);
				}
			}
			else { 
				for (int i=0; i<statistics.length; i++)
					this.rightStatistics[i].addValues(statistics[i]);
				if (this.right == null) {
					if(numNodes<maxNodes){
						this.right = new Node(inputAttributeValue, statistics);
						++numNodes;
					}
				} else {
					this.right.observeAttribute(inputAttributeValue, statistics);
				}
			}
		}
	}


}
