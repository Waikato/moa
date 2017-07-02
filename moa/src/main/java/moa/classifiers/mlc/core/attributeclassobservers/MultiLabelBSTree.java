package moa.classifiers.mlc.core.attributeclassobservers;

import java.io.Serializable;

import com.github.javacliparser.IntOption;

import moa.classifiers.mlc.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.rules.core.AttributeExpansionSuggestion;
import moa.classifiers.rules.core.NumericRulePredicate;
import moa.classifiers.rules.core.Utils;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class MultiLabelBSTree extends AbstractOptionHandler implements NumericStatisticsObserver {

	/**
	 * 
	 */
	public IntOption maxNodesOption = new IntOption("maxNodesOption", 'z', "Maximum number of nodes", 2000, 0, Integer.MAX_VALUE);
	protected int maxNodes;
	protected int numNodes;

	private static final long serialVersionUID = 1L;

	protected Node root = null;

	protected DoubleVector[] leftStatistics;
	protected DoubleVector[] rightStatistics;

	public static double roundToSignificantFigures(double num, int n) {
	    final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
	    final int power = n - (int) d;

	    final double magnitude = Math.pow(10, power);
	    final long shifted = (long) (num*magnitude);
	    return shifted/magnitude;
	}
	
	@Override
	public void observeAttribute(double inputAttributeValue,
			DoubleVector[] statistics) {
		if (!Double.isNaN(inputAttributeValue))
		{
//			if ((long) inputAttributeValue != inputAttributeValue) 
//				inputAttributeValue = roundToSignificantFigures(inputAttributeValue, 4);
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
		int numOutputs = preSplitStatistics.length;
		leftStatistics = new DoubleVector[numOutputs];
		rightStatistics = new DoubleVector[numOutputs];
		for (int i=0; i< numOutputs; i++)
		{
			leftStatistics[i] = new DoubleVector(new double [preSplitStatistics[i].numValues()]); //sets statistics to zeros
			rightStatistics[i] = new DoubleVector(preSplitStatistics[i]);
		}

		return searchForBestSplitOption(this.root, null, criterion, preSplitStatistics, inputAttributeIndex);
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
			leftStatistics[i].addValues(currentNode.statistics[i]);
			rightStatistics[i].subtractValues(currentNode.statistics[i]);
		}

		DoubleVector[][] postSplitDists = new DoubleVector [leftStatistics.length][2];
		for (int i=0; i<leftStatistics.length; i++)
		{
			postSplitDists[i] = new DoubleVector[2];
			postSplitDists[i][0] = leftStatistics[i];
			postSplitDists[i][1] = rightStatistics[i];
		}

		double merit = criterion.getMeritOfSplit(preSplitStatistics, postSplitDists);

		if ((currentBestOption == null) || (merit > currentBestOption.merit)) {
			currentBestOption = new AttributeExpansionSuggestion(new NumericRulePredicate(inputAttributeIndex, currentNode.cutPoint, true), Utils.copy(postSplitDists), merit);
		}

		if (currentNode.right != null) {
			currentBestOption = searchForBestSplitOption(currentNode.right, currentBestOption, criterion, preSplitStatistics, inputAttributeIndex);
		}
		for (int i = 0; i < leftStatistics.length; i++)
		{
			leftStatistics[i].subtractValues(currentNode.statistics[i]);
			rightStatistics[i].addValues(currentNode.statistics[i]);
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
		private double cutPoint;

		// E-BST statistics
		private DoubleVector[] statistics;

		// Child nodes
		private Node left;
		private Node right;

		public Node(double inputAttributeValue, DoubleVector [] statistics) {
			cutPoint = inputAttributeValue;
			int numOutputAttributes = statistics.length;
			this.statistics=new DoubleVector[numOutputAttributes];
			for (int i=0; i<numOutputAttributes; i++)
			{					
				this.statistics[i]=new DoubleVector(statistics[i]);
			}
		}

		/**
		 * Updates tree with new observation
		 */
		public void observeAttribute(double inputAttributeValue, DoubleVector [] statistics) {
			if (inputAttributeValue <= this.cutPoint) {
				for (int i=0; i < statistics.length; i++)
					this.statistics[i].addValues(statistics[i]);
			}
			else if (inputAttributeValue < this.cutPoint) {
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
