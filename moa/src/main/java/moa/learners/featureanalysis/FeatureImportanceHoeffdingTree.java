package moa.learners.featureanalysis;

import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.CapabilitiesHandler;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.splitcriteria.InfoGainSplitCriterion;
import moa.classifiers.trees.HoeffdingTree;
import moa.core.Measurement;
import moa.core.Utils;
import moa.options.ClassOption;

/**
 * HoeffdingTree Feature Importance extends the traditional HoeffdingTree classifier to also yield feature importances.
 *
 * <p>This class uses the HoeffdingTree structure to produce feature importances.
 * This class does not interfere with the training algorithm of the underlying HoeffdingTree model.
 * Any subclass of the HoeffdingTree class can be set as the treeLearnerOption.</p>
 *
 * <p>See details in:<br> Heitor Murilo Gomes, Rodrigo Fernandes de Mello, Bernhard Pfahringer, Albert Bifet.
 * Feature Scoring using Tree-Based Ensembles for Evolving Data Streams.
 * IEEE International Conference on Big Data (pp. 761-769), 2019</p>
 * </p>
 *
 * <p>Parameters:</p> <ul>
 * <li>-l : HoeffdingTree or subclass to train and to be analyzed. </li>
 * <li>-s : The feature importance estimation metric: MDI (Mean Decrease in Impurity) and COVER. </li>
 * </ul>
 *
 * @author Heitor Murilo Gomes (heitor dot gomes at waikato dot ac dot nz)
 * @version $Revision: 1 $
 */
public class FeatureImportanceHoeffdingTree extends AbstractClassifier implements MultiClassClassifier,
        CapabilitiesHandler, FeatureImportanceClassifier {

    public ClassOption treeLearnerOption = new ClassOption("treeLearner", 'l',
            "Decision Tree learner.", HoeffdingTree.class,
            "HoeffdingTree");

    // MDI: Mean Decrease in Impurity, COVER: Number of instances reaching a node.
    public MultiChoiceOption featureImportanceOption = new MultiChoiceOption("featureImportance", 'o',
            "Which method to use for feature importance estimations.",
            new String[]{"MDI", "COVER"},
            new String[]{"MDI", "COVER"}, 0);

    // The internal tree learner object
    protected HoeffdingTree treeLearner = null;

    // Used for feature importance calculation.
    protected double[] featureImportances;
    protected int nodeCountAtLastFeatureImportanceInquiry = 0;


    protected int featureImportancesInquiries = 0;

    protected static final int FEATURE_IMPORTANCE_MDI = 0;
    protected static final int FEATURE_IMPORTANCE_COVER  = 1;

    @Override
    public double[] getFeatureImportances(boolean normalize) {
        if (this.treeLearner.getTreeRoot() != null) {
            // Check if there were changes to the tree topology (new nodes)
            //  If not, it is not necessary to recalculated featureScores,
            //  just return the current values.
            if (this.treeLearner.getNodeCount() > this.nodeCountAtLastFeatureImportanceInquiry) {
                // Debug
                featureImportancesInquiries++;

                this.featureImportances = new double[this.featureImportances.length];
                this.nodeCountAtLastFeatureImportanceInquiry = this.treeLearner.getNodeCount();

                // If there was a split, then recalculate scores.
                switch(this.featureImportanceOption.getChosenIndex()) {
                    case FEATURE_IMPORTANCE_MDI:
                        this.calcMeanDecreaseImpurity(this.treeLearner.getTreeRoot());
                        break;
                    case FEATURE_IMPORTANCE_COVER:
                        this.calcMeanCover(this.treeLearner.getTreeRoot());
                        break;
                }

                if (normalize) {
                    double sumFeatureScores = Utils.sum(this.featureImportances);
                    for (int i = 0; i < this.featureImportances.length; ++i) {
                        this.featureImportances[i] /= sumFeatureScores;
                    }
                }
            }
        }

        return this.featureImportances;
    }

    @Override
    public int[] getTopKFeatures(int k, boolean normalize) {
        if (this.getFeatureImportances(normalize) == null)
            return null;
        if (k > this.getFeatureImportances(normalize).length)
            k = this.getFeatureImportances(normalize).length;

        int[] topK = new int[k];
        double[] currentFeatureScores = new double[this.getFeatureImportances(normalize).length];
        for (int i = 0; i < currentFeatureScores.length; ++i)
            currentFeatureScores[i] = this.getFeatureImportances(normalize)[i];

        for (int i = 0; i < k; ++i) {
            int currentTop = Utils.maxIndex(currentFeatureScores);
            topK[i] = currentTop;
            currentFeatureScores[currentTop] = -1;
        }

        return topK;
    }

    // TODO: Merge this method and calcMeanDecreaseImpurity as they are very similar in their structure.
    private void calcMeanCover(HoeffdingTree.Node node) {
        if (node instanceof HoeffdingTree.SplitNode) {
            HoeffdingTree.SplitNode splitNode = (HoeffdingTree.SplitNode) node;
            int attributeIndex = splitNode.getSplitTest().getAttsTestDependsOn()[0];

            if (this.featureImportances.length <= attributeIndex) {
                System.out.println("Error with attributeIndex");
                assert(this.featureImportances.length <= attributeIndex);
            }

            this.featureImportances[attributeIndex] += calcNodeCover(splitNode);

            for (HoeffdingTree.Node childNode : splitNode.getChildren()) {
                if (childNode != null)
                    calcMeanCover(childNode);
            }
        }
    }

    public double calcNodeCover(HoeffdingTree.SplitNode splitNode) {
        double[] thisNodeClassDistributionAtLeaves = splitNode
                .getObservedClassDistributionAtLeavesReachableThroughThisNode();
        return Utils.sum(thisNodeClassDistributionAtLeaves);
    }

    private void calcMeanDecreaseImpurity(HoeffdingTree.Node node) {
        if (node instanceof HoeffdingTree.SplitNode) {
            HoeffdingTree.SplitNode splitNode = (HoeffdingTree.SplitNode) node;
            int attributeIndex = splitNode.getSplitTest().getAttsTestDependsOn()[0];

            if (this.featureImportances.length <= attributeIndex) {
                System.out.println("Error with attributeIndex");
                assert(this.featureImportances.length <= attributeIndex);
            }

            this.featureImportances[attributeIndex] += calcNodeDecreaseImpurity(splitNode);

            for (HoeffdingTree.Node childNode : splitNode.getChildren()) {
                if (childNode != null)
                    calcMeanDecreaseImpurity(childNode);
            }
        }
    }

    public double calcNodeDecreaseImpurity(HoeffdingTree.SplitNode splitNode) {
        double[] thisNodeClassDistributionAtLeaves = splitNode
                .getObservedClassDistributionAtLeavesReachableThroughThisNode();
        double thisNodeEntropy = InfoGainSplitCriterion.computeEntropy(thisNodeClassDistributionAtLeaves);
        double sumChildrenImpurityDecrease = 0;
        double thisNodeWeight = Utils.sum(thisNodeClassDistributionAtLeaves);

        for (HoeffdingTree.Node childNode : splitNode.getChildren()) {
            if (childNode != null) {
                int childNumInstances = (int) Utils.sum(childNode
                        .getObservedClassDistributionAtLeavesReachableThroughThisNode());

                double childEntropy = InfoGainSplitCriterion.computeEntropy(childNode
                        .getObservedClassDistributionAtLeavesReachableThroughThisNode());

                sumChildrenImpurityDecrease += (childNumInstances/thisNodeWeight) * childEntropy;
            }
        }
        double DI = thisNodeEntropy - sumChildrenImpurityDecrease;
        return DI;
    }


    @Override
    public double[] getVotesForInstance(Instance instance) {
        return this.treeLearner.getVotesForInstance(instance);
    }

    @Override
    public void resetLearningImpl() {
        this.featureImportances = null;
        this.nodeCountAtLastFeatureImportanceInquiry = 0;
        this.featureImportancesInquiries = 0;
        this.treeLearner = (HoeffdingTree) getPreparedClassOption(this.treeLearnerOption);
        this.treeLearner.resetLearning();
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        // Initialize the featureImportances array.
        if (this.featureImportances == null)
            this.featureImportances = new double[instance.numAttributes() - 1];

        this.treeLearner.trainOnInstance(instance);
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return this.treeLearner.getModelMeasurements();
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        this.treeLearner.getModelDescription(out, indent);
    }

    @Override
    public boolean isRandomizable() {
        if(this.treeLearner == null)
            return false;
        return this.treeLearner.isRandomizable();
    }
}