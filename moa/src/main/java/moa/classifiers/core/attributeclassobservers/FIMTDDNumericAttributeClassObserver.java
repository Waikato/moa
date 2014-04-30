/*
 *    FIMTDDNumericAttributeClassObserver.java
 *    Copyright (C) 2013 University of Porto, Portugal
 *    @author Katie de Lange, E. Almeida, J. Gama
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
 *    
 */

/* Project Knowledge Discovery from Data Streams, FCT LIAAD-INESC TEC, 
 *
 * Contact: jgama@fep.up.pt
 */

package moa.classifiers.core.attributeclassobservers;

import java.io.Serializable;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class FIMTDDNumericAttributeClassObserver extends BinaryTreeNumericAttributeClassObserver implements NumericAttributeClassObserver {

    private static final long serialVersionUID = 1L;

    protected class Node implements Serializable {

        private static final long serialVersionUID = 1L;

        // The split point to use
        public double cut_point;

        // E-BST statistics
        public DoubleVector leftStatistics = new DoubleVector();
        public DoubleVector rightStatistics = new DoubleVector();

        // Child nodes
        public Node left;
        public Node right;

        public Node(double val, double label, double weight) {
            this.cut_point = val;
            this.leftStatistics.addToValue(0, 1);
            this.leftStatistics.addToValue(1, label);
            this.leftStatistics.addToValue(2, label * label);
        }

        /**
         * Insert a new value into the tree, updating both the sum of values and
         * sum of squared values arrays
         */
        public void insertValue(double val, double label, double weight) {

		// If the new value equals the value stored in a node, update
            // the left (<=) node information
            if (val == this.cut_point) {
                this.leftStatistics.addToValue(0, 1);
                this.leftStatistics.addToValue(1, label);
                this.leftStatistics.addToValue(2, label * label);
            } // If the new value is less than the value in a node, update the
            // left distribution and send the value down to the left child node.
            // If no left child exists, create one
            else if (val <= this.cut_point) {
                this.leftStatistics.addToValue(0, 1);
                this.leftStatistics.addToValue(1, label);
                this.leftStatistics.addToValue(2, label * label);
                if (this.left == null) {
                    this.left = new Node(val, label, weight);
                } else {
                    this.left.insertValue(val, label, weight);
                }
            } // If the new value is greater than the value in a node, update the
            // right (>) distribution and send the value down to the right child node.
            // If no right child exists, create one
            else { // val > cut_point
                this.rightStatistics.addToValue(0, 1);
                this.rightStatistics.addToValue(1, label);
                this.rightStatistics.addToValue(2, label * label);
                if (this.right == null) {
                    this.right = new Node(val, label, weight);
                } else {
                    this.right.insertValue(val, label, weight);
                }
            }
        }
    }

    // Root node of the E-BST structure for this attribute
    protected Node root = null;

    // Global variables for use in the FindBestSplit algorithm
    double sumTotalLeft;
    double sumTotalRight;
    double sumSqTotalLeft;
    double sumSqTotalRight;
    double countRightTotal;
    double countLeftTotal;

    public void observeAttributeClass(double attVal, double classVal, double weight) {
        if (Double.isNaN(attVal)) { //Instance.isMissingValue(attVal)
        } else {
            if (this.root == null) {
                this.root = new Node(attVal, classVal, weight);
            } else {
                this.root.insertValue(attVal, classVal, weight);
            }
        }
    }

    @Override
    public double probabilityOfAttributeValueGivenClass(double attVal,
            int classVal) {
        // TODO: NaiveBayes broken until implemented
        return 0.0;
    }

    @Override
    public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(SplitCriterion criterion, double[] preSplitDist, int attIndex, boolean binaryOnly) {

        // Initialise global variables
        sumTotalLeft = 0;
        sumTotalRight = preSplitDist[1];
        sumSqTotalLeft = 0;
        sumSqTotalRight = preSplitDist[2];
        countLeftTotal = 0;
        countRightTotal = preSplitDist[0];
        return searchForBestSplitOption(this.root, null, criterion, attIndex);
    }

    /**
     * Implementation of the FindBestSplit algorithm from E.Ikonomovska et al.
     */
    protected AttributeSplitSuggestion searchForBestSplitOption(Node currentNode, AttributeSplitSuggestion currentBestOption, SplitCriterion criterion, int attIndex) {
        // Return null if the current node is null or we have finished looking through all the possible splits
        if (currentNode == null || countRightTotal == 0.0) {
            return currentBestOption;
        }

        if (currentNode.left != null) {
            currentBestOption = searchForBestSplitOption(currentNode.left, currentBestOption, criterion, attIndex);
        }

        sumTotalLeft += currentNode.leftStatistics.getValue(1);
        sumTotalRight -= currentNode.leftStatistics.getValue(1);
        sumSqTotalLeft += currentNode.leftStatistics.getValue(2);
        sumSqTotalRight -= currentNode.leftStatistics.getValue(2);
        countLeftTotal += currentNode.leftStatistics.getValue(0);
        countRightTotal -= currentNode.leftStatistics.getValue(0);

        double[][] postSplitDists = new double[][]{{countLeftTotal, sumTotalLeft, sumSqTotalLeft}, {countRightTotal, sumTotalRight, sumSqTotalRight}};
        double[] preSplitDist = new double[]{(countLeftTotal + countRightTotal), (sumTotalLeft + sumTotalRight), (sumSqTotalLeft + sumSqTotalRight)};
        double merit = criterion.getMeritOfSplit(preSplitDist, postSplitDists);

        if ((currentBestOption == null) || (merit > currentBestOption.merit)) {
            currentBestOption = new AttributeSplitSuggestion(
                    new NumericAttributeBinaryTest(attIndex,
                            currentNode.cut_point, true), postSplitDists, merit);

        }

        if (currentNode.right != null) {
            currentBestOption = searchForBestSplitOption(currentNode.right, currentBestOption, criterion, attIndex);
        }
        sumTotalLeft -= currentNode.leftStatistics.getValue(1);
        sumTotalRight += currentNode.leftStatistics.getValue(1);
        sumSqTotalLeft -= currentNode.leftStatistics.getValue(2);
        sumSqTotalRight += currentNode.leftStatistics.getValue(2);
        countLeftTotal -= currentNode.leftStatistics.getValue(0);
        countRightTotal += currentNode.leftStatistics.getValue(0);

        return currentBestOption;
    }

    /**
     * A method to remove all nodes in the E-BST in which it and all it's
     * children represent 'bad' split points
     */
    public void removeBadSplits(SplitCriterion criterion, double lastCheckRatio, double lastCheckSDR, double lastCheckE) {
        removeBadSplitNodes(criterion, this.root, lastCheckRatio, lastCheckSDR, lastCheckE);
    }

    /**
     * Recursive method that first checks all of a node's children before
     * deciding if it is 'bad' and may be removed
     */
    private boolean removeBadSplitNodes(SplitCriterion criterion, Node currentNode, double lastCheckRatio, double lastCheckSDR, double lastCheckE) {
        boolean isBad = false;

        if (currentNode == null) {
            return true;
        }

        if (currentNode.left != null) {
            isBad = removeBadSplitNodes(criterion, currentNode.left, lastCheckRatio, lastCheckSDR, lastCheckE);
        }

        if (currentNode.right != null && isBad) {
            isBad = removeBadSplitNodes(criterion, currentNode.left, lastCheckRatio, lastCheckSDR, lastCheckE);
        }

        if (isBad) {

            double[][] postSplitDists = new double[][]{{currentNode.leftStatistics.getValue(0), currentNode.leftStatistics.getValue(1), currentNode.leftStatistics.getValue(2)}, {currentNode.rightStatistics.getValue(0), currentNode.rightStatistics.getValue(1), currentNode.rightStatistics.getValue(2)}};
            double[] preSplitDist = new double[]{(currentNode.leftStatistics.getValue(0) + currentNode.rightStatistics.getValue(0)), (currentNode.leftStatistics.getValue(1) + currentNode.rightStatistics.getValue(1)), (currentNode.leftStatistics.getValue(2) + currentNode.rightStatistics.getValue(2))};
            double merit = criterion.getMeritOfSplit(preSplitDist, postSplitDists);

            if ((merit / lastCheckSDR) < (lastCheckRatio - (2 * lastCheckE))) {
                currentNode = null;
                return true;
            }
        }

        return false;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }
}
