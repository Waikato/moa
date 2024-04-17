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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Self-training classifier: it is trained with a limited number of labeled data at first,
 * then it predicts the labels of unlabeled data, the most confident predictions are used
 * for training in the next iteration.
 */
public class SelfTrainingClassifier extends AbstractClassifier implements SemiSupervisedLearner {

    private static final long serialVersionUID = 1L;

    /* -------------------
     * GUI options
     * -------------------*/
    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Any learner to be self-trained", AbstractClassifier.class,
            "moa.classifiers.trees.HoeffdingTree");

    public IntOption batchSizeOption = new IntOption("batchSize", 'b',
            "Size of one batch to self-train",
            1000, 1, Integer.MAX_VALUE);

    public MultiChoiceOption thresholdChoiceOption = new MultiChoiceOption("thresholdValue", 't',
            "Ways to define the confidence threshold",
            new String[] { "Fixed", "AdaptiveWindowing", "AdaptiveVariance" },
            new String[] {
                    "The threshold is input once and remains unchanged",
                    "The threshold is updated every h-interval of time",
                    "The threshold is updated if the confidence score drifts off from the average"
            }, 0);

    public FloatOption thresholdOption = new FloatOption("confidenceThreshold", 'c',
            "Threshold to evaluate the confidence of a prediction",
            0.7, 0.0, Double.MAX_VALUE);

    public IntOption horizonOption = new IntOption("horizon", 'h',
            "The interval of time to update the threshold", 1000);

    public FloatOption ratioThresholdOption = new FloatOption("ratioThreshold", 'r',
            "How large should the threshold be wrt to the average confidence score",
            0.8, 0.0, Double.MAX_VALUE);

    public MultiChoiceOption confidenceOption = new MultiChoiceOption("confidenceComputation",
            's', "Choose the method to estimate the prediction uncertainty",
            new String[]{ "DistanceMeasure", "FromLearner" },
            new String[]{ "Confidence score from pair-wise distance with the ground truth",
              "Confidence score estimated by the learner itself" }, 1);

    /* -------------------
     * Attributes
     * -------------------*/

    /** A learner to be self-trained */
    private Classifier learner;

    /** The size of one batch */
    private int batchSize;

    /** The confidence threshold to decide which predictions to include in the next training batch */
    private double threshold;

    /** Contains the unlabeled instances */
    private List<Instance> U;

    /** Contains the labeled instances */
    private List<Instance> L;

    /** Contains the predictions of one batch's training */
//    private List<Instance> Uhat;

    /** Contains the most confident prediction */
//    private List<Instance> mostConfident;

    private int horizon;
    private int t;
    private double ratio;
    private double LS;
    private double SS;
    private double N;
    private double lastConfidenceScore;

    // Statistics
    protected long instancesSeen;
    protected long instancesPseudoLabeled;
    protected long instancesCorrectPseudoLabeled;


    @Override
    public String getPurposeString() { return "A self-training classifier"; }

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.learner = (Classifier) getPreparedClassOption(learnerOption);
        this.batchSize = batchSizeOption.getValue();
        this.threshold = thresholdOption.getValue();
        this.ratio = ratioThresholdOption.getValue();
        this.horizon = horizonOption.getValue();
        LS = SS = N = t = 0;
        allocateBatch();
        super.prepareForUseImpl(monitor, repository);
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        return learner.getVotesForInstance(inst);
    }

    @Override
    public void resetLearningImpl() {
        this.learner.resetLearning();
        lastConfidenceScore = LS = SS = N = t = 0;
        allocateBatch();

        this.instancesSeen = 0;
        this.instancesCorrectPseudoLabeled = 0;
        this.instancesPseudoLabeled = 0;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        this.instancesSeen++;
        updateThreshold();
        t++;

        L.add(inst);
        learner.trainOnInstance(inst);


        /* if batch B is full, launch the self-training process */
        if (isBatchFull()) {
            trainOnUnlabeledBatch();
        }
    }

    private void trainOnUnlabeledBatch() {
        List<AbstractMap.SimpleEntry<Instance, Double>> Uhat = predictOnBatch(U);
        List<AbstractMap.SimpleEntry<Instance, Double>> mostConfident = null;

        // chose the method to estimate prediction uncertainty
//        if (confidenceOption.getChosenIndex() == 0)
//            mostConfident = getMostConfidentDistanceBased(Uhat);
//        else
        mostConfident = getMostConfidentFromLearner(Uhat);
        // train from the most confident examples
        for(AbstractMap.SimpleEntry<Instance, Double> x : mostConfident) {
            learner.trainOnInstance(x.getKey());
            if(x.getKey().classValue() == x.getValue())
                ++this.instancesCorrectPseudoLabeled;
            ++this.instancesPseudoLabeled;
        }
        cleanBatch();
    }

    @Override
    public void addInitialWarmupTrainingInstances() {
        // TODO: add counter, but this may not be necessary for this class
    }

    // TODO: Verify if we need to do something else.
    @Override
    public int trainOnUnlabeledInstance(Instance instance) {
        this.instancesSeen++;
        U.add(instance);

        if (isBatchFull()) {
            trainOnUnlabeledBatch();
        }
//        this.trainOnInstanceImpl(instance);
        return -1;
    }

    private void updateThreshold() {
        if (thresholdChoiceOption.getChosenIndex() == 1) updateThresholdWindowing();
        if (thresholdChoiceOption.getChosenIndex() == 2) updateThresholdVariance();
    }

    /**
     * Dynamically updates the confidence threshold at the end of each labeledInstancesBuffer horizon
     */
    private void updateThresholdWindowing() {
        if (t % horizon == 0) {
            if (N == 0 || LS == 0 || SS == 0) return;
            threshold = (LS / N) * ratio;
            t = 0;
        }
    }

    /**
     * Dynamically updates the confidence threshold:
     * adapt the threshold if the last confidence score falls out of z-index = 1 zone
     */
    private void updateThresholdVariance() {
        // TODO update right when it detects a drift, or to wait until H drifts have happened?
        if (N == 0 || LS == 0 || SS == 0) return;
        double variance = (SS - LS * LS / N) / (N - 1);
        double mean = LS / N;
        double zscore = (lastConfidenceScore - mean) / variance;
        if (Math.abs(zscore) > 1.0) {
            threshold = mean * ratio;
        }
    }

    /**
     * Gives prediction for each instance in a given batch.
     * @param batch the batch containing unlabeled instances
     * @return result the result to save the prediction in
     */
    private List<AbstractMap.SimpleEntry<Instance, Double>> predictOnBatch(List<Instance> batch) {
        List<AbstractMap.SimpleEntry<Instance, Double>> batchWithPredictions = new ArrayList<>();


        for (Instance instance : batch) {
            Instance copy = instance.copy(); // use copy because we do not want to modify the original data
            double classValue = -1.0;
            if(!instance.classIsMissing()) // if it is not missing, assume this is a debug execution and store it for checking pseudo-labelling accuracy.
                classValue = instance.classValue();

            copy.setClassValue(Utils.maxIndex(learner.getVotesForInstance(copy)));
            batchWithPredictions.add(new AbstractMap.SimpleEntry<Instance, Double> (copy, classValue));
        }

        return batchWithPredictions;
    }

    /**
     * Gets the most confident predictions
     * @param batch batch of instances to give prediction to
     * @return mostConfident instances that are more confidence than a threshold
     */
    private List<AbstractMap.SimpleEntry<Instance, Double>> getMostConfidentFromLearner(List<AbstractMap.SimpleEntry<Instance, Double>> batch) {
        List<AbstractMap.SimpleEntry<Instance, Double>> mostConfident = new ArrayList<>();
        for (AbstractMap.SimpleEntry<Instance, Double> x : batch) {
            double[] votes = learner.getVotesForInstance(x.getKey());
            if (votes[Utils.maxIndex(votes)] >= threshold) {
                mostConfident.add(x);
            }
        }
        return mostConfident;
    }

    /**
     * Gets the most confident predictions that exceed the indicated threshold
     * @param batch the batch containing the predictions
     * @return mostConfident the result containing the most confident prediction from the given batch
     */
//    private List<AbstractMap.SimpleEntry<Instance, Double>> getMostConfidentDistanceBased(List<AbstractMap.SimpleEntry<Instance, Double>> batch) {
//        /*
//         * Use distance measure to estimate the confidence of a prediction
//         *
//         * for each instance X in the batch:
//         *    for each instance XL in the labeled data: (ground-truth)
//         *       if X.label == XL.label: (only consider instances sharing the same label)
//         *          confidence[X] += distance(X, XL)
//         *    confidence[X] = confidence[X] / |L| (taking the average)
//         */
//        List<AbstractMap.SimpleEntry<Instance, Double>> mostConfident = new ArrayList<>();
//
//        double[] confidences = new double[batch.size()];
//        double conf;
//        int i = 0;
//        for (AbstractMap.SimpleEntry<Instance, Double> X : batch) {
//            conf = 0;
//            for (Instance XL : this.L) {
//                if (XL.classValue() == X.getKey().classValue()) {
//                    conf += Clusterer.distance(XL.toDoubleArray(), X.getKey().toDoubleArray()) / this.L.size();
//                }
//            }
//            conf = (1.0 / conf > 1.0 ? 1.0 : 1 / conf); // reverse so the distance becomes the confidence
//            confidences[i++] = conf;
//            // accumulate the statistics
//            LS += conf;
//            SS += conf * conf;
//            N++;
//        }
//
//        for (double confidence : confidences) lastConfidenceScore += confidence / confidences.length;
//
//        /* The confidences are computed using the distance measures,
//         * so naturally, the lower the score, the more certain the prediction is.
//         * Here we simply retrieve the instances whose confidence score are below a threshold */
//        for (int j = 0; j < confidences.length; j++) {
//            if (confidences[j] >= threshold) {
//                mostConfident.add(batch.get(j));
//            }
//        }
//
//        return mostConfident;
//    }

    /**
     * Checks whether the batch is full
     * @return <code>true</code> if the batch is full, <code>false</code> otherwise
     */
    private boolean isBatchFull() {
        return U.size() + L.size() >= batchSize;
    }

    /** Cleans the batch (and its associated variables) */
    private void cleanBatch() {
        L.clear();
        U.clear();
//        mostConfident.clear();
    }

    /** Allocates memory to the batch */
    private void allocateBatch() {
        this.U = new ArrayList<>();
        this.L = new ArrayList<>();
//        this.mostConfident = new ArrayList<>();
    }


    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        // instances seen * the number of ensemble members
        return new Measurement[]{
                new Measurement("#pseudo-labeled", this.instancesPseudoLabeled),
                new Measurement("#correct pseudo-labeled", this.instancesCorrectPseudoLabeled),
                new Measurement("accuracy pseudo-labeled", this.instancesCorrectPseudoLabeled / (double) this.instancesPseudoLabeled * 100)
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
