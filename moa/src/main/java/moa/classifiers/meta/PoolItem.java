package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.Classifier;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.core.Utils;

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
