package moa.classifiers.semisupervised;

import com.github.javacliparser.FlagOption;
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
 * Variance of Self-training: all instances are used to self-train the learner, but each has a weight, depending
 * on the confidence of their prediction
 */
public class SelfTrainingWeightingClassifier extends AbstractClassifier implements SemiSupervisedLearner {


    @Override
    public String getPurposeString() {
        return "Self-training classifier that weights instances by confidence score (threshold not used)";
    }

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Any learner to be self-trained", AbstractClassifier.class,
            "moa.classifiers.trees.HoeffdingTree");

    public FlagOption equalWeightOption = new FlagOption("equalWeight", 'w',
            "Assigns to all instances a weight equal to 1");

    /** If set to True, all instances have weight 1; otherwise, the weights are based on the confidence score */
    private boolean equalWeight;

    /** The learner to be self-trained */
    private Classifier learner;

    // Statistics
    protected long instancesSeen;
    protected long instancesPseudoLabeled;
    protected long instancesCorrectPseudoLabeled;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.learner = (Classifier) getPreparedClassOption(learnerOption);
        this.equalWeight = equalWeightOption.isSet();
        super.prepareForUseImpl(monitor, repository);
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        return learner.getVotesForInstance(inst);
    }

    @Override
    public void resetLearningImpl() {
        this.learner.resetLearning();
        this.instancesSeen = 0;
        this.instancesCorrectPseudoLabeled = 0;
        this.instancesPseudoLabeled = 0;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        ++this.instancesSeen;

        if (/*!inst.classIsMasked() &&*/ !inst.classIsMissing()) {
            learner.trainOnInstance(inst);
        } else {
            Instance instCopy = inst.copy();
            int pseudoLabel = Utils.maxIndex(learner.getVotesForInstance(instCopy));
            instCopy.setClassValue(pseudoLabel);
            if (!equalWeight) instCopy.setWeight(learner.getConfidenceForPrediction(instCopy, pseudoLabel));
            learner.trainOnInstance(instCopy);

//            if(pseudoLabel == inst.maskedClassValue()) {
//                ++this.instancesCorrectPseudoLabeled;
//            }
            ++this.instancesPseudoLabeled;
        }
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
    public void getModelDescription(StringBuilder out, int indent) {}

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
