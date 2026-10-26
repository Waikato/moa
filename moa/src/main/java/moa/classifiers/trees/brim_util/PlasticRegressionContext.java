package moa.classifiers.trees.brim_util;

import com.yahoo.labs.samoa.instances.Instance;
import moa.core.DoubleVector;

import java.io.Serializable;
import java.util.Random;

/**
 * State shared by all nodes of a {@link moa.classifiers.trees.BRIMRegressor}.
 *
 * <p>Holds the leaf model settings and, as the tree object of {@link moa.classifiers.trees.FIMTDD} does,
 * the statistics of the target and of the attributes over all instances seen by the tree.
 * They are used to normalize the inputs and the target of the perceptrons in the leaves.</p>
 */
public class PlasticRegressionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    final boolean modelTree;
    final double learningRatio;
    final double learningRateDecayFactor;
    final boolean learningRatioConst;
    final Random random;
    final double quantizationRadius;
    final boolean adaptiveLeaves;
    final double fadingFactor;
    final int maxInnerNodeAttributes;
    // Ablation switches (see the -O and -C options of BRIMRegressor): the E-BST instead of the Quantization
    // Observer, and perceptrons without clipping
    private boolean useEBST = false;
    private boolean clipping = true;

    /**
     * Sets the ablation switches
     * @param useEBST if the numeric attributes are monitored with the E-BST instead of the Quantization Observer
     * @param clipping if the normalized inputs and errors of the perceptrons are clipped
     */
    public void setAblation(boolean useEBST, boolean clipping) {
        this.useEBST = useEBST;
        this.clipping = clipping;
    }

    /**
     * @return true if numeric attributes are monitored with the E-BST (ablation)
     */
    boolean usesEBST() {
        return useEBST;
    }

    // The standard deviation of an attribute is only used for quantization once this many instances were seen
    static final int MIN_INSTANCES_FOR_STD_DEV = 100;

    double examplesSeen = 0.0;
    // Incremented whenever a node is created (by a split or by restructuring), so that the memory limit can be
    // checked as soon as the tree changes
    int numNodesCreated = 0;
    private double sumOfValues = 0.0;
    private double sumOfSquares = 0.0;
    private final DoubleVector sumOfAttrValues = new DoubleVector();
    private final DoubleVector sumOfAttrSquares = new DoubleVector();

    /**
     * @param modelTree if the leaves predict with a perceptron (model tree) instead of the mean target (regression tree)
     * @param learningRatio the (initial) learning ratio of the perceptrons
     * @param learningRateDecayFactor the decay factor of the learning ratio
     * @param learningRatioConst if the learning ratio is kept constant
     * @param random the random generator used to initialize the perceptron weights
     * @param quantizationRadius the width of the slots of the quantization observers, relative to the standard deviation of the attribute
     * @param adaptiveLeaves if the leaves of a model tree choose between the perceptron and the mean target
     * @param fadingFactor the fading factor of the errors used to choose between the leaf models
     * @param maxInnerNodeAttributes the maximum number of attributes monitored by an inner node (0 for all)
     */
    public PlasticRegressionContext(boolean modelTree, double learningRatio, double learningRateDecayFactor,
                                    boolean learningRatioConst, Random random, double quantizationRadius,
                                    boolean adaptiveLeaves, double fadingFactor, int maxInnerNodeAttributes) {
        this.modelTree = modelTree;
        this.learningRatio = learningRatio;
        this.learningRateDecayFactor = learningRateDecayFactor;
        this.learningRatioConst = learningRatioConst;
        this.random = random;
        this.quantizationRadius = quantizationRadius;
        this.adaptiveLeaves = adaptiveLeaves;
        this.fadingFactor = fadingFactor;
        this.maxInnerNodeAttributes = maxInnerNodeAttributes;
    }

    /**
     * Updates the target and attribute statistics. Called once per instance, before the tree learns it.
     * @param inst the instance
     */
    public void update(Instance inst) {
        examplesSeen += inst.weight();
        sumOfValues += inst.weight() * inst.classValue();
        sumOfSquares += inst.weight() * inst.classValue() * inst.classValue();

        for (int i = 0; i < inst.numAttributes() - 1; i++) {
            int aIndex = CustomEFDTNode.modelAttIndexToInstanceAttIndex(i, inst);
            sumOfAttrValues.addToValue(i, inst.weight() * inst.value(aIndex));
            sumOfAttrSquares.addToValue(i, inst.weight() * inst.value(aIndex) * inst.value(aIndex));
        }
    }

    /**
     * The standard deviation of an attribute over all instances seen by the tree. It sets the radius of the
     * quantization observers.
     * @param attIndex the model index of the attribute
     * @return the standard deviation, or 0 if fewer than {@link #MIN_INSTANCES_FOR_STD_DEV} instances were seen
     */
    /**
     * @return the number of nodes created so far
     */
    public int getNumNodesCreated() {
        return numNodesCreated;
    }

    double attributeStdDev(int attIndex) {
        if (examplesSeen < MIN_INSTANCES_FOR_STD_DEV)
            return 0.0;
        return computeSD(sumOfAttrSquares.getValue(attIndex), sumOfAttrValues.getValue(attIndex), examplesSeen);
    }

    /**
     * @return the standard deviation of the target over all instances seen by the tree
     */
    public double targetStdDev() {
        return computeSD(sumOfSquares, sumOfValues, examplesSeen);
    }

    /**
     * Clips a normalized value to [-1, 1], unless clipping is disabled for the ablation
     */
    double clip(double normalizedValue) {
        return clipping ? Math.max(-1.0, Math.min(1.0, normalizedValue)) : normalizedValue;
    }

    /**
     * Normalizes a target as FIMTDD does: (value - mean) / (3 * sd)
     */
    double normalizeTargetValue(double value) {
        if (examplesSeen > 1) {
            double sd = computeSD(sumOfSquares, sumOfValues, examplesSeen);
            double average = sumOfValues / examplesSeen;
            if (sd > 0)
                return (value - average) / (3 * sd);
        }
        return 0.0;
    }

    /**
     * Inverse of {@link #normalizeTargetValue(double)}
     */
    double denormalizePrediction(double normalizedPrediction) {
        if (examplesSeen > 1) {
            double mean = sumOfValues / examplesSeen;
            double sd = computeSD(sumOfSquares, sumOfValues, examplesSeen);
            return normalizedPrediction * sd * 3 + mean;
        }
        return 0.0;
    }

    /**
     * Normalizes the numeric attributes to (x - mean) / (3 * sd), clipped to [-1, 1] (i.e., to 3 standard deviations)
     * so that outliers cannot destabilize the perceptrons. Nominal attributes are set to 0.
     * The last entry is the constant 1 that is multiplied with the bias.
     */
    DoubleVector normalizedInstance(Instance inst) {
        DoubleVector normalizedInstance = new DoubleVector();
        for (int j = 0; j < inst.numAttributes() - 1; j++) {
            int instAttIndex = CustomEFDTNode.modelAttIndexToInstanceAttIndex(j, inst);
            double mean = sumOfAttrValues.getValue(j) / examplesSeen;
            double sd = computeSD(sumOfAttrSquares.getValue(j), sumOfAttrValues.getValue(j), examplesSeen);
            if (inst.attribute(instAttIndex).isNumeric() && examplesSeen > 1 && sd > 0)
                normalizedInstance.setValue(j, clip((inst.value(instAttIndex) - mean) / (3 * sd)));
            else
                normalizedInstance.setValue(j, 0);
        }
        normalizedInstance.setValue(inst.numAttributes() - 1, examplesSeen > 1 ? 1.0 : 0.0);
        return normalizedInstance;
    }

    /**
     * @return the standard deviation given the sum of squares, the sum and the weight
     */
    private static double computeSD(double squaredVal, double val, double size) {
        // The variance can be slightly negative due to rounding (e.g., identical values with weights > 1)
        if (size > 1)
            return Math.sqrt(Math.max(0.0, (squaredVal - ((val * val) / size)) / size));
        return 0.0;
    }
}
