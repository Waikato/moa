package moa.classifiers.core.attributeclassobservers;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.conditionaltests.NominalAttributeMultiwayTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;
import moa.classifiers.core.conditionaltests.NominalAttributeBinaryTest;

import java.io.Serializable;

public class HoeffdingNominalAttributeClassObserver extends AbstractOptionHandler implements
        DiscreteAttributeClassObserver  {

    private static final long serialVersionUID = 1L;

    protected class Node implements Serializable {

        private static final long serialVersionUID = 1L;

        // The split point to use
        public double cut_point;

        // statistics
        public DoubleVector statistics = new DoubleVector();


        // Child node
        public HoeffdingNominalAttributeClassObserver.Node child;


        public Node(double val, double label) {
            this.cut_point = val;
            this.statistics.addToValue(0, 1);
            this.statistics.addToValue(1, label);
            this.statistics.addToValue(2, label * label);
        }

        /**
         * Insert a new value into the tree, updating both the sum of values and
         * sum of squared values arrays
         */
        public void insertValue(double val, double label) {
            //System.out.println(val);
            // If the new value equals the value stored in a node, update
            // the  node information
            if (val == this.cut_point) {
                this.statistics.addToValue(0, 1);
                this.statistics.addToValue(1, label);
                this.statistics.addToValue(2, label * label);
            } // If the new value is less or greater than the value in a node, send the value down to the  child node.
            // If no left child exists, create one
            else  {

                if (this.child == null) {
                    this.child = new HoeffdingNominalAttributeClassObserver.Node(val, label);
                    numberOfPossibleValues += 1 ;
                } else {
                    this.child.insertValue(val, label);
                }

            }
        }
    }

    // Root node of the tree structure for this attribute
    protected HoeffdingNominalAttributeClassObserver.Node root = null;

    // Global variables for use in the FindBestSplit algorithm
    double sumOne;
    double sumRest;
    double sumSqOne;
    double sumSqRest;
    double countOne;
    double countRest;
    double sumTotal;
    double sumSqTotal;
    double count ;
    boolean binaryOnly;
    int numberOfPossibleValues  ;

    public void observeAttributeClass(double attVal, int classVal, double weight) {


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
        sumOne = 0;
        sumRest = 0;
        sumSqOne = 0;
        sumSqRest = 0;
        countOne = 0;
        countRest = 0;
        sumTotal = preSplitDist[1];
        sumSqTotal = preSplitDist[2];
        count = preSplitDist[0];
        this.binaryOnly = binaryOnly;
        if (binaryOnly) {
            return searchForBestBinarySplitOption(this.root, null, criterion, attIndex);
        } else {
            return searchForBestMultiwaySplitOption(this.root, null, criterion, attIndex);
        }
    }

    /**
     * Implementation of the FindBestSplit algorithm
     */
    protected AttributeSplitSuggestion searchForBestBinarySplitOption(HoeffdingNominalAttributeClassObserver.Node currentNode, AttributeSplitSuggestion currentBestOption, SplitCriterion criterion, int attIndex) {



            // Return null if the current node is null or we have finished looking through all the possible splits
            if (currentNode == null || countRest == 0.0) {
                return currentBestOption;
            }

            if (currentNode.child != null) {
                currentBestOption = searchForBestBinarySplitOption(currentNode.child, currentBestOption, criterion, attIndex);
            }

            sumOne = currentNode.statistics.getValue(1);
            sumRest = sumTotal - sumOne;
            sumSqOne = currentNode.statistics.getValue(2);
            sumSqRest = sumSqTotal - sumSqOne;
            countOne = currentNode.statistics.getValue(0);
            countRest = count - countOne;

            double[][] postSplitDists = new double[][]{{countOne, sumOne, sumSqOne}, {countRest, sumRest, sumSqRest}};
            double[] preSplitDist = new double[]{(count), (sumTotal), (sumSqTotal)};
            double merit = criterion.getMeritOfSplit(preSplitDist, postSplitDists);

            if ((currentBestOption == null) || (merit > currentBestOption.merit)) {
                currentBestOption = new AttributeSplitSuggestion(
                        new NominalAttributeBinaryTest(attIndex,
                                (int) currentNode.cut_point), postSplitDists, merit);


            }

        return currentBestOption;

        }
    protected AttributeSplitSuggestion searchForBestMultiwaySplitOption(HoeffdingNominalAttributeClassObserver.Node currentNode, AttributeSplitSuggestion currentBestOption, SplitCriterion criterion, int attIndex)
    {

            double[][] postSplitDists = new double[numberOfPossibleValues][3];
            for (int i = 0; i < numberOfPossibleValues; i++)
            {

                if (currentNode == null || countRest == 0.0) {
                    return currentBestOption;
                }
                postSplitDists[i][0] = currentNode.statistics.getValue(0);
                postSplitDists[i][1] =  currentNode.statistics.getValue(1);
                postSplitDists[i][2] =  currentNode.statistics.getValue(2);
                currentNode = currentNode.child ;

            }
            double[] preSplitDist = new double[]{(count), (sumTotal), (sumSqTotal)};
            double merit = criterion.getMeritOfSplit(preSplitDist, postSplitDists);
            if ((currentBestOption == null) || (merit > currentBestOption.merit)) {
                currentBestOption = new AttributeSplitSuggestion(
                        new NominalAttributeMultiwayTest(attIndex), postSplitDists, merit);
            }



        return currentBestOption;

            }







    public void observeAttributeTarget(double attVal, double classVal) {
        if (Double.isNaN(attVal)) { //Instance.isMissingValue(attVal)
        } else {
            if (this.root == null) {
                numberOfPossibleValues= 1 ;
                this.root = new HoeffdingNominalAttributeClassObserver.Node(attVal, classVal);
            } else {
                this.root.insertValue(attVal, classVal);
            }
        }
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
