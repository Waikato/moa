package moa.classifiers.meta;

import com.github.javacliparser.*;

import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.Capabilities;
import moa.capabilities.CapabilitiesHandler;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;
import moa.classifiers.trees.HoeffdingTree;
import moa.classifiers.lazy.kNN;
import moa.classifiers.deeplearning.MLP;
import moa.classifiers.meta.policy.Policy;

import java.util.*;

/**
 * <b>Heterogeneous Online Ensemble (Heros)</b><br>
 *
 * This class implements the Heros ensemble algorithm for classification.
 * For every training step, HEROS chooses a subset of models from a pool of models initialized with diverse
 * hyperparameter choices under resource constraints to train. Different policies for choosing which models
 * to train on incoming data can be selected. Among the following policies can be chosen:
 * -ZetaPolicy: Focuses on training near-optimal models at reduced costs.
 * -CandPolicy: Half of the models random, other half with best estimated performance.
 *
 *  <p>Parameters:</p> <ul>
 *      <li>-P : Pool of models</li>
 *      <li>-N : Number of instances to train all models on</li>
 *      <li>-e : Evaluate all models on each instance</li>
 *      <li>-a : Aggregation method for prediction. (1: Best, 2: Average over k models, 3: PoE over k models, 4: Weighted)</li>
 *      <li>-r : Dynamic resource cost computation</li>
 *      <li>-p : Policy to use</li>
 *      <li>-d : ADWIN delta to estimate the predictive performance</li>
 *      <li>-R : Reset a learner after a drift is detected</li>
 *  </ul>
 *
 * Reference (pre-print): <a href="url">https://arxiv.org/abs/2509.18962</a>
 *
 */

public class Heros extends AbstractClassifier implements MultiClassClassifier, CapabilitiesHandler {

    public ListOption poolOption = new ListOption(
            "pool",
            'P',
            "Ensemble of heterogeneous classifiers.",
            new ClassOption("learner", ' ', "", Classifier.class,
                    "trees.HoeffdingTree"),
            new Option[]{
                    new ClassOption("", ' ', "", Classifier.class,
                            "trees.HoeffdingTree -m 2000"),
                    new ClassOption("", ' ', "", Classifier.class,
                            "trees.HoeffdingTree -m 4000"),
                    new ClassOption("", ' ', "", Classifier.class,
                            "trees.HoeffdingTree -m 8000"),
                    new ClassOption("", ' ', "", Classifier.class,
                            "trees.HoeffdingTree -m 16000"),
                    new ClassOption("", ' ', "", Classifier.class,
                            "trees.HoeffdingTree -m 32000"),
                    new ClassOption("", ' ', "", Classifier.class,
                            "trees.HoeffdingTree -m 64000"),
                    new ClassOption("", ' ', "", Classifier.class,
                            "trees.HoeffdingTree -m 128000"),
                    new ClassOption("", ' ', "", Classifier.class,
                            "trees.HoeffdingTree -m 256000"),
                    new ClassOption("", ' ', "", Classifier.class,
                            "trees.HoeffdingTree -m 512000"),
                    new ClassOption("", ' ', "", Classifier.class,
                            "trees.HoeffdingTree -m 1024000"),
            },
            ','
    );
    public IntOption numInstancesToTrainAllModelsOption = new IntOption("numInstancesToTrainAllModels",
            'N', "Number of instances to train all models on.", 100, 0,
            Integer.MAX_VALUE);

    public FlagOption evaluateNotAllModelsOption = new FlagOption("evaluateNotAllModels", 'e',
            "Evaluate all models on each instance.");
    public IntOption aggregationOption = new IntOption("aggregation", 'a',
            "Aggregation method for prediction. (1: Best, 2: Average over k models, 3: PoE over k models, 4: Weighted)",
            1, 1, 4);
    public FlagOption dynamicResourceCosts = new FlagOption("dynamicResourceCosts",
            'r', "Dynamic resource cost computation.");
    public ClassOption policyOption = new ClassOption("policy",
            'p', "Policy to use.", Policy.class, "ZetaPolicy");
    protected Policy policy;
    public FloatOption deltaOption = new FloatOption("delta",
            'd', "ADWIN delta to estimate the predictive performance.",
            0.002, 0.0, 1.);
    public FlagOption resetLearnerAfterDriftOption = new FlagOption("resetLearnerAfterDrift",
            'R', "Reset a learner after a drift is detected.");

    protected PoolItem[] pool;
    protected int samplesSeen;
    protected int[] lastAction;
    protected float resourceNormFactor;

    @Override
    public String getPurposeString() {
        return "Heterogeneous Online Ensemble method with resource-efficient training.";
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        this.samplesSeen++;
        // Use only one model with the highest estimated accuracy for prediction
        if (this.aggregationOption.getValue() == 1) {
           return this.predictOnBestModels(instance);
        } else if (this.aggregationOption.getValue() == 2) {        // Use all models from the last applied action for prediction (apply an average on the votes)
            return this.predictOnKModels(instance);
        } else if (this.aggregationOption.getValue() == 3) {    // Normalize and multiply votes to a product of experts
            return this.predictProductOfExperts(instance);
        } else {        // Multiply the estimated predictive performance of each learner by the vote and normalize
            return this.predictWeighted(instance);
        }
    }

    private double[] predictOnBestModels(Instance instance) {
        // If no performance is tracked yet, select first model randomly
        if (this.samplesSeen == 1) {
            Random randNum = new Random();
            return this.pool[randNum.nextInt(this.pool.length)].model.getVotesForInstance(instance);
        }
        // Get model with minimum estimated loss
        PoolItem minPoolItem = Arrays.stream(this.pool).min(Comparator.naturalOrder()).get();
        return minPoolItem.model.getVotesForInstance(instance);
    }

    private double[] predictOnKModels(Instance instance) {
        double[] votesForInstance = null;
        double[] votesForInstanceTemp;
        int numberOfAppliedActions = 0;
        int[] lastAction = this.getLastAction();
        for (int i=0; i < this.pool.length; i++) {
            if (lastAction[i] == 0) {
                continue;
            }

            numberOfAppliedActions += 1;
            // Collect and sum all votes for instance of models from the last applied action
            if (votesForInstance == null) {
                votesForInstance = this.pool[i].model.getVotesForInstance(instance);
            } else {
                votesForInstanceTemp = this.pool[i].model.getVotesForInstance(instance);
                for (int j = 0; j < votesForInstance.length; j++) {
                    try {
                        votesForInstance[j] = votesForInstance[j] + votesForInstanceTemp[j];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // continue if both length of votes are unequal, could happen especially for untrained models
                        continue;
                    }
                }
            }
        }
        // Compute the average
        for (int i = 0; i < votesForInstance.length; i++) {
            votesForInstance[i] /= numberOfAppliedActions;
        }
        return votesForInstance;
    }

    private double[] normalize(double[] votes) {
        double lenVotes = 0;
        for (double vote : votes) {
            lenVotes += vote;
        }
        for (int j = 0; j < votes.length; j++) {
            votes[j] /= lenVotes;
        }
        return votes;
    }

    private double[] predictProductOfExperts(Instance instance) {
        double[] votesForInstance = null;
        double[] votesForInstanceTemp;
        int[] lastAction = this.getLastAction();
        for (int i=0; i < this.pool.length; i++) {
            if (lastAction[i] == 0) {
                continue;
            }
            votesForInstanceTemp = this.normalize(this.pool[i].model.getVotesForInstance(instance));

            // Multiply votes of models from the last applied action
            if (votesForInstance == null) {
                votesForInstance = votesForInstanceTemp;
            } else {
                for (int j = 0; j < votesForInstance.length; j++) {
                    try {
                        votesForInstance[j] = votesForInstance[j] * votesForInstanceTemp[j];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // continue if both length of votes are unequal, could happen especially for untrained models
                    }
                }
            }
        }
        return this.normalize(votesForInstance);
    }

    private double[] predictWeighted(Instance instance) {
        double[] votesForInstance = null;
        double[] votesForInstanceTemp;
        int[] lastAction = this.getLastAction();

        // Get estimated predictive performance from each ensemble member as voting weight
        double[] votingWeight = new double[this.pool.length];
        for (int i=0; i < votingWeight.length; i++) {
            votingWeight[i] = this.pool[i].getEstimation();
        }

        for (int i=0; i < this.pool.length; i++) {
            if (lastAction[i] == 0) {
                continue;
            }
            votesForInstanceTemp = this.normalize(this.pool[i].model.getVotesForInstance(instance));

            // Multiply votes of models from the last applied action
            if (votesForInstance == null) {
                votesForInstance = votesForInstanceTemp;
            } else {
                for (int j = 0; j < votesForInstance.length; j++) {
                    try {
                        votesForInstance[j] += votingWeight[i] * votesForInstanceTemp[j];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // continue if both length of votes are unequal, could happen especially for untrained models
                    }
                }
            }
        }
        return this.normalize(votesForInstance);
    }

    private int[] getLastAction() {
        if (this.lastAction == null) {
            this.lastAction = new int[this.pool.length];
            Arrays.fill(this.lastAction, 1);
        }
        return this.lastAction;
    }

    private float computeStaticResourceCosts(Classifier model) {
        float resourceCost = 1.0F;
        // Hoeffding tree: tree depth
        if (model instanceof HoeffdingTree) {
            resourceCost = (float) ((HoeffdingTree) model).maxByteSizeOption.getValue();
        }
        // kNN: number of k nearest neighbors
        else if (model instanceof kNN) {
            resourceCost = (float) ((kNN) model).kOption.getValue();
        }
        // MLP: number of neurons
        else if (model instanceof MLP) {
            resourceCost = (float) (((MLP) model).numberOfNeuronsInEachLayerInLog2.getValue() * ((MLP) model).numberOfLayers.getValue());
        }
        return resourceCost;
    }


    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.samplesSeen = 0;
        this.policy = (Policy) this.getPreparedClassOption(this.policyOption);
        // Create heterogeneous pool
        Option[] poolOptions = this.poolOption.getList();
        this.pool = new PoolItem[poolOptions.length];
        float sumResourceCost = 0.0F;
        for (int i = 0; i < poolOptions.length; i++) {
            monitor.setCurrentActivity("Materializing learner " + (i + 1) + "...", -1.0);
            Classifier model = (Classifier) ((ClassOption) poolOptions[i]).materializeObject(monitor, repository);
            // Compute static resource cost for each model at the beginning
            float resourceCost = this.computeStaticResourceCosts(model);
            sumResourceCost += resourceCost;
            this.pool[i] = new PoolItem(model, new ADWIN(deltaOption.getValue()), resourceCost,
                    this.dynamicResourceCosts.isSet(), resetLearnerAfterDriftOption.isSet());
            if (monitor.taskShouldAbort()) {
                return;
            }

            monitor.setCurrentActivity("Preparing learner " + (i + 1) + "...", -1.0);
            this.pool[i].model.prepareForUse(monitor, repository);
            if (monitor.taskShouldAbort()) {
                return;
            }
        }
        // Normalize resource costs
        for (PoolItem poolItem : this.pool) {
            poolItem.setResourceCost(poolItem.getResourceCost() / sumResourceCost);
        }
        // Set last action to null
        this.lastAction = null;
        super.prepareForUseImpl(monitor, repository);
    }

    @Override
    public void resetLearningImpl() {
        this.samplesSeen = 0;
        for (int i = 0; i < this.pool.length; i++) {
            this.pool[i].model.resetLearning();
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        int[] action = new int[this.pool.length];
        // Select all models at the beginning
        if (this.samplesSeen < this.numInstancesToTrainAllModelsOption.getValue()) {
            Arrays.fill(action, 1);
        } else {
            action = this.policy.pull(this.pool);
        }
        this.lastAction = action;
        // Perform action (environment step)
        for (int i = 0; i < this.pool.length; i++) {
            if (action[i] > 0 | !this.evaluateNotAllModelsOption.isSet()) {
                // Forward instance to model and update estimator
                double[] classVotes = this.pool[i].model.getVotesForInstance(instance);
                this.pool[i].updateEstimator(instance, classVotes);
                if (action[i] > 0) {    // train model (and update resource costs)
                    float resCost = 0;
                    long startTime = System.nanoTime();

                    this.pool[i].model.trainOnInstance(instance);

                    if (this.dynamicResourceCosts.isSet()) {
                        resCost = System.nanoTime() - startTime;
                    }
                    this.pool[i].updateAfterTraining(resCost);
                }
            }
        }
        // In case of dynamic resource costs: normalize resource costs and set it dynamically
        if (this.dynamicResourceCosts.isSet()) {
            this.resourceNormFactor = 0;
            for (int i = 0; i < this.pool.length; i++) {
                // Do not use normalized resource costs here, set factor to one such that they are not normalized
                this.pool[i].setResourceNormFactor(1);
                this.resourceNormFactor += this.pool[i].getResourceCost();
            }
            // Set new normalization factor (especially for policies)
            for (int i = 0; i < this.pool.length; i++) {
                this.pool[i].setResourceNormFactor(this.resourceNormFactor);
            }
        }
    }


    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{new Measurement("pool size (models)", this.pool.length),
                new Measurement("samples seen", this.samplesSeen)};
    }

    @Override
    public void getModelDescription(StringBuilder stringBuilder, int i) {
//        for (PoolItem poolItem : this.pool) {
//            poolItem.model.getModelDescription(stringBuilder, i);
//            stringBuilder.append(", Resource cost: " + poolItem.getResourceCost());
//            StringUtils.appendNewline(stringBuilder);
//        }
    }


    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public Capabilities getCapabilities() {
        return super.getCapabilities();
    }

    public float[] getResourceCostsOfEachModel() {
        float[] resourceCosts = new float[this.pool.length];
        for (int i = 0; i < this.pool.length; i++) {
            resourceCosts[i] = this.pool[i].getResourceCost();
        }
        return resourceCosts;
    }

    /*
     * PoolItem describes a member of the pool.
     */
    public class PoolItem implements Comparable<PoolItem> {

        protected Classifier model;
        protected ADWIN estimator;
        protected ADWIN resourceEstimator;
        protected float resourceCost;
        protected int numTrainingSteps;
        private final boolean dynamicResources;
        protected float resourceNormFactor;
        private boolean resetLearnerAfterDrift;

        public PoolItem(Classifier model, ADWIN estimator, float resourceCost, boolean dynamicResources,
                        boolean resetLearnerAfterDrift) {
            this.model = model;
            this.estimator = estimator;
            this.resourceEstimator = new ADWIN();
            this.resourceCost = resourceCost;
            this.numTrainingSteps = 0;
            this.dynamicResources = dynamicResources;
            this.resetLearnerAfterDrift = resetLearnerAfterDrift;
        }

        @Override
        public int compareTo(PoolItem o) {
            return Double.compare(this.estimator.getEstimation(), o.estimator.getEstimation());
        }

        public void updateAfterTraining(float resCost) {
            this.numTrainingSteps++;
            if (this.dynamicResources) {
                this.resourceEstimator.setInput(resCost);
                this.setResourceCost((float) this.resourceEstimator.getEstimation());
            }
        }

        public float getResourceCost() {
            if (!this.dynamicResources) {
                return this.resourceCost;
            }
            return this.resourceCost / this.resourceNormFactor;
        }

        public void setResourceCost(float resourceCost) {
            this.resourceCost = resourceCost;
        }

        public void updateEstimator(Instance instance, double[] classVotes) {
            int trueClass = (int) instance.classValue();
            int predictedClass = Utils.maxIndex(classVotes);
            this.estimator.setInput(predictedClass == trueClass ? 1.0 : 0.0);
            // Reset learner after concept drift recognized in performance estimation
            if (this.resetLearnerAfterDrift && this.estimator.getChange()) {
//            System.out.println("Change detected");
                this.model.resetLearning();
            }
        }

        public double getEstimation() {
            return this.estimator.getEstimation();
        }

        public int getNumTrainingSteps() {
            return this.numTrainingSteps;
        }

        public void setResourceNormFactor(float resourceNormFactor) {
            this.resourceNormFactor = resourceNormFactor;
        }
    }

}
