package moa.classifiers.semisupervised;

import com.github.javacliparser.FlagOption;
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

import java.util.ArrayList;
import java.util.List;

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

    private double LS;
    private double SS;
    private double N;
    private double lastConfidenceScore;
    private List<Double> confidences = new ArrayList<>();
    private int numKeptInstances = 0;

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

        if (!inst.classIsMasked() && !inst.classIsMissing()) {
            learner.trainOnInstance(inst);
        } else {
            double prediction = getPrediction(inst);
            double confidenceScore = learner.getConfidenceForPrediction(inst, prediction);
            if (confidenceScore >= threshold) {
                Instance instCopy = inst.copy();
                instCopy.setClassValue(prediction);
                learner.trainOnInstance(instCopy);
                numKeptInstances++;
            }
            // accumulate the statistics
            LS += confidenceScore;
            SS += confidenceScore * confidenceScore;
            N++;
            lastConfidenceScore = confidenceScore;
            confidences.add(confidenceScore);
        }

        t++;
    }

    private void updateThreshold() {
        if (thresholdChoiceOption.getChosenIndex() == 1) updateThresholdWindowing();
        if (thresholdChoiceOption.getChosenIndex() == 2) updateThresholdVariance();
    }

    private void updateThresholdWindowing() {
        if (t % horizon == 0) {
            if (N == 0 || LS == 0 || SS == 0) return;
            threshold = (LS / N) * ratio;
            threshold = (Math.min(threshold, 1.0));
            // N = LS = SS = 0; // to reset or not?
            t = 0;
        }
    }

    private void updateThresholdVariance() {
        // TODO update right when it detects a drift, or to wait until H drifts have happened?
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
        List<Measurement> measures = new ArrayList<>();

        // confidence threshold
        measures.add(new Measurement("threshold", threshold));

        // average confidence score
        double avgConfidence = 0;
        for (Double confidence : confidences) avgConfidence += confidence;
        avgConfidence /= confidences.size();
        measures.add(new Measurement("average confidence", avgConfidence));

        // number of instances kept as training data
        measures.add(new Measurement("kept instances", numKeptInstances));
        numKeptInstances = 0;

        Measurement[] result = new Measurement[measures.size()];
        return measures.toArray(result);
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {

    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
