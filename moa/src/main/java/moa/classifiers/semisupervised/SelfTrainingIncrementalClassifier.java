package moa.classifiers.semisupervised;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

/**
 * Self-training classifier: Incremental version.
 * Instead of using a batch, the model will be update with every instance that arrives.
 */
public class SelfTrainingIncrementalClassifier extends AbstractClassifier implements SemiSupervisedLearner {

    /* -------------------
     * GUI options
     * -------------------*/
    private static final long serialVersionUID = 1L;

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Any learner to be self-trained", AbstractClassifier.class,
            "moa.classifiers.trees.HoeffdingTree");

    public FloatOption thresholdOption = new FloatOption("confidenceThreshold", 'c',
            "Threshold to evaluate the confidence of a prediction", 0.7, 0.0, 1.0);

    /* -------------------
     * Attributes
     * -------------------*/
    /** A learner to be self-trained */
    private Classifier learner;

    /** The confidence threshold to decide which predictions to include in the next training batch */
    private double threshold;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.learner = (Classifier) getPreparedClassOption(learnerOption);
        this.threshold = thresholdOption.getValue();
        super.prepareForUseImpl(monitor, repository);
    }

    @Override
    public String getPurposeString() {
        return "A self-training classifier that trains at every instance (not using a batch)";
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        return learner.getVotesForInstance(inst);
    }

    @Override
    public void resetLearningImpl() {

    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        /*
         * if X is labeled:
         *      L.train(X)
         * else:
         *      X_hat <- L.predict_probab(X)
         *      if X_hat.highest_proba > threshold:
         *          L.train(X_hat)
         */

        if (!inst.classIsMasked() && !inst.classIsMissing()) {
            learner.trainOnInstance(inst);
        } else {
            double prediction = getPrediction(inst);
            double confidenceScore = learner.getConfidenceForPrediction(inst, prediction);
            if (confidenceScore >= threshold) {
                learner.trainOnInstance(inst);
            }
        }
    }

    private double getPrediction(Instance inst) {
        return Utils.maxIndex(this.getVotesForInstance(inst));
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {

    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
