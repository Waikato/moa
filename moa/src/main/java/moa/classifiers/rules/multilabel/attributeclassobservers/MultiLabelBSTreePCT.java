package moa.classifiers.rules.multilabel.attributeclassobservers;

import java.io.Serializable;

import com.github.javacliparser.IntOption;

import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.multilabel.core.splitcriteria.PCTWeightedICVarianceReduction;
import moa.classifiers.rules.multilabel.core.AttributeExpansionSuggestion;
import moa.classifiers.rules.core.NumericRulePredicate;
import moa.classifiers.rules.core.Utils;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class MultiLabelBSTreePCT extends AbstractOptionHandler implements NumericStatisticsObserver {

	/**
	 *
	 */
	public IntOption maxNodesOption = new IntOption("maxNodesOption", 'z', "Maximum number of nodes", 2000, 0,
			Integer.MAX_VALUE);
	protected int maxNodes;
	protected int numNodes;

	private static final long serialVersionUID = 1L;

	protected Node root = null;

	protected DoubleVector[] leftTargetStatistics;
	protected DoubleVector[] rightTargetStatistics;

	protected DoubleVector[] leftInputStatistics;
	protected DoubleVector[] rightInputStatistics;

	public static double roundToSignificantFigures(double num, int n) {
		final double d = Math.ceil(Math.log10(num < 0 ? -num : num));
		final int power = n - (int) d;

		final double magnitude = Math.pow(10, power);
		final long shifted = (long) (num * magnitude);
		return shifted / magnitude;
	}

	@Override
	public void observeAttribute(double inputAttributeValue, DoubleVector[] statistics) {
		throw new UnsupportedOperationException(
				"This is an attribute observer used for PCTs and needs both input and output statistics.");
	}

	public void observeAttribute(double inputAttributeValue, DoubleVector[] targetStatistics,
			DoubleVector[] inputStatistics) {
		if (!Double.isNaN(inputAttributeValue)) {
			if (this.root == null) {
				this.root = new Node(inputAttributeValue, targetStatistics, inputStatistics);
				maxNodes = maxNodesOption.getValue();
			} else {
				this.root.observeAttribute(inputAttributeValue, targetStatistics, inputStatistics);
			}
		}

	}

	@Override
	public AttributeExpansionSuggestion getBestEvaluatedSplitSuggestion(MultiLabelSplitCriterion criterion,
			DoubleVector[] preSplitStatistics, int inputAttributeIndex) {
		throw new UnsupportedOperationException(
				"This is an attribute observer used for PCTs and needs both input and output statistics.");
	}

	public AttributeExpansionSuggestion getBestEvaluatedSplitSuggestion(MultiLabelSplitCriterion criterion,
			DoubleVector[] preSplitTargetStatistics, DoubleVector[] preSplitInputStatistics, int inputAttributeIndex) {
		// Initialize global variables
		int numOutputs = preSplitTargetStatistics.length;
		int numInputs = preSplitInputStatistics.length;

		leftTargetStatistics = new DoubleVector[numOutputs];
		rightTargetStatistics = new DoubleVector[numOutputs];

		leftInputStatistics = new DoubleVector[numInputs];
		rightInputStatistics = new DoubleVector[numInputs];

		for (int i = 0; i < numOutputs; i++) {
			leftTargetStatistics[i] = new DoubleVector(new double[preSplitTargetStatistics[i].numValues()]); // sets
																												// statistics
																												// to
																												// zeros
			rightTargetStatistics[i] = new DoubleVector(preSplitTargetStatistics[i]);
		}
		for (int i = 0; i < numInputs; i++) {
			leftInputStatistics[i] = new DoubleVector(new double[preSplitInputStatistics[i].numValues()]); // sets
																											// statistics
																											// to zeros
			rightInputStatistics[i] = new DoubleVector(preSplitInputStatistics[i]);
		}

		return searchForBestSplitOption(this.root, null, criterion, preSplitTargetStatistics, preSplitInputStatistics,
				inputAttributeIndex);
	}

	protected AttributeExpansionSuggestion searchForBestSplitOption(Node currentNode,
			AttributeExpansionSuggestion currentBestOption, MultiLabelSplitCriterion criterion,
			DoubleVector[] preSplitTargetStatistics, DoubleVector[] preSplitInputStatistics, int inputAttributeIndex) {
		// Return null if the current node is null or we have finished looking through
		// all the possible splits
		if (currentNode == null) { // TODO: JD check || countRightTotal == 0.0
			return currentBestOption;
		}

		if (currentNode.left != null) {
			currentBestOption = searchForBestSplitOption(currentNode.left, currentBestOption, criterion,
					preSplitTargetStatistics, preSplitInputStatistics, inputAttributeIndex);
		}

		for (int i = 0; i < leftTargetStatistics.length; i++) {
			leftTargetStatistics[i].addValues(currentNode.targetStatistics[i]);
			rightTargetStatistics[i].subtractValues(currentNode.targetStatistics[i]);
		}

		for (int i = 0; i < leftInputStatistics.length; i++) {
			leftInputStatistics[i].addValues(currentNode.inputStatistics[i]);
			rightInputStatistics[i].subtractValues(currentNode.inputStatistics[i]);
		}

		DoubleVector[][] postSplitTargetDists = new DoubleVector[leftTargetStatistics.length][2];
		DoubleVector[][] postSplitInputDists = new DoubleVector[leftInputStatistics.length][2];
		for (int i = 0; i < leftTargetStatistics.length; i++) {
			postSplitTargetDists[i] = new DoubleVector[2];
			postSplitTargetDists[i][0] = leftTargetStatistics[i];
			postSplitTargetDists[i][1] = rightTargetStatistics[i];
		}
		for (int i = 0; i < leftInputStatistics.length; i++) {
			postSplitInputDists[i] = new DoubleVector[2];
			postSplitInputDists[i][0] = leftInputStatistics[i];
			postSplitInputDists[i][1] = rightInputStatistics[i];
		}

		double merit = ((PCTWeightedICVarianceReduction) criterion).getMeritOfSplit(preSplitTargetStatistics,
				postSplitTargetDists, preSplitInputStatistics, postSplitInputDists);

		if ((!Double.isNaN(merit)) && (currentBestOption == null || (merit > currentBestOption.merit))) {
			currentBestOption = new AttributeExpansionSuggestion(
					new NumericRulePredicate(inputAttributeIndex, currentNode.cutPoint, true),
					Utils.copy(postSplitTargetDists), merit);
		}

		if (currentNode.right != null) {
			currentBestOption = searchForBestSplitOption(currentNode.right, currentBestOption, criterion,
					preSplitTargetStatistics, preSplitInputStatistics, inputAttributeIndex);
		}
		for (int i = 0; i < leftTargetStatistics.length; i++) {
			leftTargetStatistics[i].subtractValues(currentNode.targetStatistics[i]);
			rightTargetStatistics[i].addValues(currentNode.targetStatistics[i]);
		}
		for (int i = 0; i < leftInputStatistics.length; i++) {
			leftInputStatistics[i].subtractValues(currentNode.inputStatistics[i]);
			rightInputStatistics[i].addValues(currentNode.inputStatistics[i]);
		}

		return currentBestOption;
	}

	@Override
	public String getPurposeString() {
		return "Stores statistics for all output and input attributes for a giver input attribute.";
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {

	}

	// Inner classes inspired in FIMTDDNumericalAttributeClassObserver

	protected class Node implements Serializable {

		private static final long serialVersionUID = 1L;

		// The split point to use
		private double cutPoint;

		// E-BST statistics
		private DoubleVector[] targetStatistics;
		private DoubleVector[] inputStatistics;

		// Child nodes
		private Node left;
		private Node right;

		public Node(double inputAttributeValue, DoubleVector[] targetStatistics, DoubleVector[] inputStatistics) {
			cutPoint = inputAttributeValue;
			int numOutputAttributes = targetStatistics.length;
			int numInputAttributes = inputStatistics.length;
			this.targetStatistics = new DoubleVector[numOutputAttributes];
			this.inputStatistics = new DoubleVector[numInputAttributes];
			for (int i = 0; i < numOutputAttributes; i++) {
				if (targetStatistics[i] != null)
					this.targetStatistics[i] = new DoubleVector(targetStatistics[i]);
				else
					this.targetStatistics[i] = new DoubleVector();
			}
			for (int i = 0; i < numInputAttributes; i++) {
				this.inputStatistics[i] = new DoubleVector(inputStatistics[i]);
			}
		}

		/**
		 * Updates tree with new observation
		 */
		public void observeAttribute(double inputAttributeValue, DoubleVector[] targetStatistics,
				DoubleVector[] inputStatistics) {
			if (inputAttributeValue == this.cutPoint) {
				for (int i = 0; i < targetStatistics.length; i++)
					if (targetStatistics[i] != null)
						this.targetStatistics[i].addValues(targetStatistics[i]);
				for (int i = 0; i < inputStatistics.length; i++)
					this.inputStatistics[i].addValues(inputStatistics[i]);

			} else if (inputAttributeValue < this.cutPoint) {
				if (this.left == null) {
					if (numNodes < maxNodes) {
						this.left = new Node(inputAttributeValue, targetStatistics, inputStatistics);
						numNodes++;
					}
				} else {
					this.left.observeAttribute(inputAttributeValue, targetStatistics, inputStatistics);
				}
			} else {
				if (this.right == null) {
					if (numNodes < maxNodes) {
						this.right = new Node(inputAttributeValue, targetStatistics, inputStatistics);
						numNodes++;
					}
				} else {
					this.right.observeAttribute(inputAttributeValue, targetStatistics, inputStatistics);
				}
			}
		}
	}

}
