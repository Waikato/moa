package moa.classifiers.trees.brim_util;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.DoubleVector;

/**
 * The E-BST of FIMTDD, extended with what PLASTIC needs from a numeric observer: evaluating a given threshold
 * ({@link #forceSplit}), a fast copy and a size estimate.
 *
 * <p>Only used for the ablation of the Quantization Observer ({@code -O} option of
 * {@link moa.classifiers.trees.BRIMRegressor}). Unlike the QO, the E-BST stores a node per distinct value,
 * so its size and its split evaluation cost grow with the number of instances of the tree node.</p>
 */
public class PlasticEBSTObserver extends FIMTDDNumericAttributeClassObserver {

    private static final long serialVersionUID = 1L;

    // Estimated sizes in bytes of the observer without nodes and of an E-BST node (with its two statistics vectors)
    static final long BASE_BYTES = 80, NODE_BYTES = 150;

    private int numNodes = 0;

    /**
     * Inserts a value into the E-BST, counting the nodes for the size estimate
     */
    @Override
    public void observeAttributeClass(double attVal, double classVal, double weight) {
        if (Double.isNaN(attVal))
            return;
        if (root == null) {
            root = new Node(attVal, classVal, weight);
            numNodes = 1;
            return;
        }
        // The same updates as Node.insertValue, without recursion and counting the nodes
        Node current = root;
        while (true) {
            if (attVal <= current.cut_point) {
                addTo(current.leftStatistics, classVal);
                if (attVal == current.cut_point)
                    return;
                if (current.left == null) {
                    current.left = new Node(attVal, classVal, weight);
                    numNodes++;
                    return;
                }
                current = current.left;
            } else {
                addTo(current.rightStatistics, classVal);
                if (current.right == null) {
                    current.right = new Node(attVal, classVal, weight);
                    numNodes++;
                    return;
                }
                current = current.right;
            }
        }
    }

    /**
     * Adds a target to statistics [count, sum, sum of squares]
     */
    private static void addTo(DoubleVector statistics, double label) {
        // As in the E-BST of FIMTDD, every value counts once regardless of its weight
        statistics.addToValue(0, 1);
        statistics.addToValue(1, label);
        statistics.addToValue(2, label * label);
    }

    /**
     * Evaluates the binary split at the given threshold by descending the E-BST and collecting the statistics of all
     * values that are not larger than the threshold.
     */
    public AttributeSplitSuggestion forceSplit(SplitCriterion criterion, double[] preSplitDist, int attIndex, double threshold) {
        double[] left = new double[3];
        Node current = root;
        while (current != null) {
            if (threshold >= current.cut_point) {
                for (int i = 0; i < 3; i++)
                    left[i] += current.leftStatistics.getValue(i);
                current = current.right;
            } else {
                current = current.left;
            }
        }
        double[] right = new double[3];
        for (int i = 0; i < 3; i++)
            right[i] = (i < preSplitDist.length ? preSplitDist[i] : 0.0) - left[i];
        double[][] postSplitDists = {left, right};
        return new AttributeSplitSuggestion(new NumericAttributeBinaryTest(attIndex, threshold, true),
                postSplitDists, criterion.getMeritOfSplit(preSplitDist, postSplitDists));
    }

    /**
     * Copies the observer without serialization. Copies are made when nodes are restructured.
     * @return a deep copy of this observer
     */
    @Override
    public PlasticEBSTObserver copy() {
        PlasticEBSTObserver copy = new PlasticEBSTObserver();
        copy.root = copySubtree(copy, root);
        copy.numNodes = numNodes;
        return copy;
    }

    /**
     * @return a deep copy of an E-BST subtree, owned by the given observer
     */
    private static Node copySubtree(PlasticEBSTObserver owner, Node node) {
        if (node == null)
            return null;
        Node copy = owner.new Node(node.cut_point, 0.0, 0.0);
        copy.leftStatistics = new DoubleVector(node.leftStatistics.getArrayCopy());
        copy.rightStatistics = new DoubleVector(node.rightStatistics.getArrayCopy());
        copy.left = copySubtree(owner, node.left);
        copy.right = copySubtree(owner, node.right);
        return copy;
    }

    /**
     * @return the estimated size of this observer in bytes
     */
    public long estimateByteSize() {
        return BASE_BYTES + numNodes * NODE_BYTES;
    }
}
