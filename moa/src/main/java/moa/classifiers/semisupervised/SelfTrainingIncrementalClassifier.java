package moa.classifiers.semisupervised;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
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

    private static final long serialVersionUID = 1L;

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Any learner to be self-trained", AbstractClassifier.class,
            "moa.classifiers.trees.HoeffdingTree");

    public MultiChoiceOption thresholdChoiceOption = new MultiChoiceOption("thresholdValue", 't',
            "Ways to define the confidence threshold",
            new String[] { "Fixed", "AdaptiveWindowing", "AdaptiveVariance" },
            new String[] {
                    "The threshold is input once and remains unchanged",
                    "The threshold is updated every h-interval of time",
                    "The threshold is updated if the confidence score drifts off from the average"
            }, 0);

    public FloatOption thresholdOption = new FloatOption("confidenceThreshold", 'c',
            "Threshold to evaluate the confidence of a prediction", 0.9, 0.0, 1.0);

    public IntOption horizonOption = new IntOption("horizon", 'h',
            "The interval of time to update the threshold", 1000);

    public FloatOption ratioThresholdOption = new FloatOption("ratioThreshold", 'r',
            "How large should the threshold be wrt to the average confidence score",
            0.95, 0.0, Double.MAX_VALUE);

    /* -------------------
     * Attributes
     * -------------------*/
    /** A learner to be self-trained */
    private Classifier learner;

    /** The confidence threshold to decide which predictions to include in the next training batch */
    private double threshold;

    /** Whether the threshold is to be adaptive or fixed*/
    private boolean adaptiveThreshold;

    /** Interval of time to update the threshold */
    private int horizon;

    /** Keep track of time */
    private int t;

    /** Ratio of the threshold wrt the average confidence score*/
    private double ratio;

    // statistics needed to update the confidence threshold
    private double LS;
    private double SS;
    private double N;
    private double lastConfidenceScore;

    // Statistics
    protected long instancesSeen;
    protected long instancesPseudoLabeled;
    protected long instancesCorrectPseudoLabeled;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.learner = (Classifier) getPreparedClassOption(learnerOption);
        this.threshold = thresholdOption.getValue();
        this.horizon = horizonOption.getValue();
        this.ratio = ratioThresholdOption.getValue();
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
        LS = SS = N = lastConfidenceScore = 0;
        this.instancesSeen = 0;
        this.instancesCorrectPseudoLabeled = 0;
        this.instancesPseudoLabeled = 0;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        /*
         * update the threshold
         *
         * if X is labeled:
         *      L.train(X)
         * else:
         *      X_hat <- L.predict_probab(X)
         *      if X_hat.highest_proba > threshold:
         *          L.train(X_hat)
         */

        updateThreshold();

        ++this.instancesSeen;

        if (/*!inst.classIsMasked() &&*/ !inst.classIsMissing()) {
            learner.trainOnInstance(inst);
        } else {
            double pseudoLabel = getPrediction(inst);
            double confidenceScore = learner.getConfidenceForPrediction(inst, pseudoLabel);
            if (confidenceScore >= threshold) {
                Instance instCopy = inst.copy();
                instCopy.setClassValue(pseudoLabel);
                learner.trainOnInstance(instCopy);
            }
            // accumulate the statistics to update the adaptive threshold
            LS += confidenceScore;
            SS += confidenceScore * confidenceScore;
            N++;
            lastConfidenceScore = confidenceScore;

//            if(pseudoLabel == inst.maskedClassValue()) {
//                ++this.instancesCorrectPseudoLabeled;
//            }
            ++this.instancesPseudoLabeled;
        }

        t++;
    }

    private void updateThreshold() {
        if (thresholdChoiceOption.getChosenIndex() == 1) updateThresholdWindowing();
        if (thresholdChoiceOption.getChosenIndex() == 2) updateThresholdVariance();
    }

    @Override
    public void addInitialWarmupTrainingInstances() {
        // TODO: add counter, but this may not be necessary for this class
    }

    // TODO: Verify if we need to do something else.
    @Override
    public int trainOnUnlabeledInstance(Instance instance) {
        this.trainOnInstanceImpl(instance);
        return -1;
    }

    /**
     * Updates the threshold after each labeledInstancesBuffer horizon
     */
    private void updateThresholdWindowing() {
        if (t % horizon == 0) {
            if (N == 0 || LS == 0 || SS == 0) return;
            threshold = (LS / N) * ratio;
            threshold = (Math.min(threshold, 1.0));
            // N = LS = SS = 0; // to reset or not?
            t = 0;
        }
    }

    /**
     * Update the thresholds based on the variance:
     * if the z-score of the last confidence score wrt the mean is more than 1.0,
     * update the confidence threshold
     */
    private void updateThresholdVariance() {
        if (N == 0 || LS == 0 || SS == 0) return;
        double variance = (SS - LS * LS / N) / (N - 1);
        double mean = LS / N;
        double zscore = (lastConfidenceScore - mean) / variance;
        if (Math.abs(zscore) > 1.0) {
            threshold = mean * ratio;
            threshold = (Math.min(threshold, 1.0));
        }
    }

    /**
     * Gets the prediction from an instance (a shortcut to pass getVotesForInstance)
     * @param inst the instance
     * @return the most likely prediction (the label with the highest probability in <code>getVotesForInstance</code>)
     */
    private double getPrediction(Instance inst) {
        return Utils.maxIndex(this.getVotesForInstance(inst));
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        // instances seen * the number of ensemble members
        return new Measurement[]{
                new Measurement("#pseudo-labeled", -1), // this.instancesPseudoLabeled),
                new Measurement("#correct pseudo-labeled", -1), //this.instancesCorrectPseudoLabeled),
                new Measurement("accuracy pseudo-labeled", -1) //this.instancesCorrectPseudoLabeled / (double) this.instancesPseudoLabeled * 100)
        };
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {

    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
