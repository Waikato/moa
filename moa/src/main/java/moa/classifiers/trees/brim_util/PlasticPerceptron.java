package moa.classifiers.trees.brim_util;

import com.yahoo.labs.samoa.instances.Instance;
import moa.core.DoubleVector;

import java.io.Serializable;

/**
 * The linear leaf model of {@link moa.classifiers.trees.FIMTDD} (a perceptron as specified by Ikonomovska et al.),
 * used by {@link moa.classifiers.trees.BRIMRegressor}.
 *
 * <p>Inputs and target are normalized with the statistics of the whole tree, kept in a
 * {@link PlasticRegressionContext}. Unlike in FIMTDD, the normalized inputs and the error of an update are clipped
 * to [-1, 1] (3 standard deviations), which bounds every update: outliers of the target or of the inputs (e.g., price
 * spikes) otherwise make the weights diverge.</p>
 */
public class PlasticPerceptron implements Serializable {

    private static final long serialVersionUID = 1L;

    // The perceptron weights. The last index corresponds to the constant b
    private DoubleVector weightAttribute = new DoubleVector();

    // The number of instances contributing to this model, used for the decay of the learning ratio
    private double instancesSeen = 0;

    // If the weights still have to be initialized
    private boolean reset;

    /**
     * Creates a perceptron whose weights are randomly initialized when it learns its first instance
     */
    public PlasticPerceptron() {
        reset = true;
    }

    /**
     * Copies the weights of another perceptron.
     * @param original the perceptron to copy
     * @param keepLearningRate if false, the learning ratio decay restarts as for the children of a split in FIMTDD
     */
    public PlasticPerceptron(PlasticPerceptron original, boolean keepLearningRate) {
        weightAttribute = (DoubleVector) original.weightAttribute.copy();
        reset = original.reset;
        instancesSeen = keepLearningRate ? original.instancesSeen : 0;
    }

    /**
     * Restarts the decay of the learning ratio, keeping the weights
     */
    public void restartLearningRate() {
        instancesSeen = 0;
    }

    /**
     * Updates the model using the provided instance
     * @return the prediction for the instance before the update
     */
    public double update(Instance inst, PlasticRegressionContext context) {
        if (reset) {
            reset = false;
            weightAttribute = new DoubleVector();
            instancesSeen = 0;
            for (int j = 0; j < inst.numAttributes(); j++) {
                weightAttribute.setValue(j, 2 * context.random.nextDouble() - 1);
            }
        }

        instancesSeen += inst.weight();

        double learningRatio;
        if (context.learningRatioConst) {
            learningRatio = context.learningRatio;
        } else {
            learningRatio = context.learningRatio / (1 + instancesSeen * context.learningRateDecayFactor);
        }

        double prediction = Double.NaN;
        // Loop for compatibility with bagging methods
        for (int i = 0; i < (int) inst.weight(); i++) {
            double normalizedPrediction = updateWeights(inst, learningRatio, context);
            if (i == 0)
                prediction = context.denormalizePrediction(normalizedPrediction);
        }
        return Double.isNaN(prediction) ? predict(inst, context) : prediction;
    }

    /**
     * @return the normalized prediction before the update
     */
    private double updateWeights(Instance inst, double learningRatio, PlasticRegressionContext context) {
        DoubleVector normalizedInstance = context.normalizedInstance(inst);
        double normalizedPrediction = scalarProduct(weightAttribute, normalizedInstance);
        double normalizedValue = context.normalizeTargetValue(inst.classValue());
        double delta = context.clip(normalizedValue - normalizedPrediction);
        normalizedInstance.scaleValues(delta * learningRatio);
        weightAttribute.addValues(normalizedInstance);
        return normalizedPrediction;
    }

    /**
     * Outputs the prediction made by this perceptron on the given instance
     */
    public double predict(Instance inst, PlasticRegressionContext context) {
        double normalizedPrediction = scalarProduct(weightAttribute, context.normalizedInstance(inst));
        return context.denormalizePrediction(normalizedPrediction);
    }

    /**
     * @return the scalar product of the weights and a normalized instance
     */
    private static double scalarProduct(DoubleVector u, DoubleVector v) {
        double ret = 0.0;
        for (int i = 0; i < Math.max(u.numValues(), v.numValues()); i++) {
            ret += u.getValue(i) * v.getValue(i);
        }
        return ret;
    }

    // Estimated size in bytes of a perceptron without its weights
    static final long BASE_BYTES = 60;

    /**
     * @return the estimated size of this perceptron in bytes
     */
    public long estimateByteSize() {
        return BASE_BYTES + 8L * weightAttribute.numValues();
    }
}
