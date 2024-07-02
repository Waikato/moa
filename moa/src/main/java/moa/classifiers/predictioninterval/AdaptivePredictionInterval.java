package moa.classifiers.predictioninterval;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.Capabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Regressor;
import moa.core.InstanceExample;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.evaluation.BasicPredictionIntervalEvaluator;
import moa.evaluation.BasicRegressionPerformanceEvaluator;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;
import org.apache.commons.math3.distribution.NormalDistribution;

public class AdaptivePredictionInterval extends AbstractClassifier implements PredictionIntervalLearner {

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Learner to train.", Regressor.class, "moa.classifiers.meta.SelfOptimisingKNearestLeaves");

    public FloatOption confidenceLevelOption = new FloatOption("confidence",'c',"confidence level", 0.95,0,1);

    public FloatOption scalarLimitOption = new FloatOption("limit",'t',"lower limit of the scalar", 0.1, 0, 1);

    private NormalDistribution normalDistribution;

    private Regressor regressor;

    private BasicRegressionPerformanceEvaluator evaluator;

    private BasicPredictionIntervalEvaluator metaEvaluator;

    @Override
    public void resetLearningImpl() {
        if (this.regressor == null) this.regressor = (Regressor) getPreparedClassOption(this.learnerOption);
        if (this.evaluator == null) this.evaluator = new BasicRegressionPerformanceEvaluator();
        if (this.metaEvaluator == null) this.metaEvaluator = new BasicPredictionIntervalEvaluator();

    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        InstanceExample example = new InstanceExample(inst);

        if (this.regressor == null) this.regressor = (Regressor) getPreparedClassOption(this.learnerOption);
        if (this.evaluator == null) this.evaluator = new BasicRegressionPerformanceEvaluator();
        if (this.metaEvaluator == null) this.metaEvaluator = new BasicPredictionIntervalEvaluator();

        double prediction = ((Learner) this.regressor).getVotesForInstance(example).length > 0 ? ((Learner) this.regressor).getVotesForInstance(example)[0] : 0;
        double interval = calculateBounds();


        return new double[]{prediction-interval, prediction, prediction + interval};
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        InstanceExample example = new InstanceExample(inst);
        this.evaluator.addResult(example, new double[]{((Learner) this.regressor).getVotesForInstance(example).length > 0 ? ((Learner) this.regressor).getVotesForInstance(example)[0] : 0});
        this.metaEvaluator.addResult(example, this.getVotesForInstance(example));
        ((Learner)this.regressor).trainOnInstance(example);

    }

    @Override
    public void trainOnInstance(Instance inst) {
        trainOnInstanceImpl(inst);
    }


    private double calculateBounds(){
        if (this.normalDistribution == null) this.normalDistribution = new NormalDistribution();
        return getScalar(this.metaEvaluator.getCoverage()) * this.normalDistribution.inverseCumulativeProbability(0.5 + this.confidenceLevelOption.getValue() / 2) * this.evaluator.getSquareError();
    }

    private double getScalar(double coverage) {
        double scalar = 100 - coverage;
        if( this.evaluator.getTotalWeightObserved() >=100) {
            if (coverage >= this.confidenceLevelOption.getValue() * 100.0) {
                scalar = (Math.log(-(1 / (100.0 - this.confidenceLevelOption.getValue() * 100)) * (coverage - 100.0)) / Math.log(this.confidenceLevelOption.getValue() * 100) + 1) * (1 - this.scalarLimitOption.getValue()) + this.scalarLimitOption.getValue();
            } else if (coverage > 200 * this.confidenceLevelOption.getValue() - 100.0 && coverage < this.confidenceLevelOption.getValue() * 100.0) {
                scalar = -(100.0 - coverage) * Math.log(1 / (100.0 - this.confidenceLevelOption.getValue() * 100.0) * (coverage - (200 * this.confidenceLevelOption.getValue() - 100.0))) / Math.log(this.confidenceLevelOption.getValue() * 100) + 1;
            }
        }else{
            scalar = 1.0;
        }
        if (scalar <= this.scalarLimitOption.getValue())
            return this.scalarLimitOption.getValue();
        else
            return scalar;
    }


    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {

    }

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {

    }



    @Override
    public Capabilities getCapabilities() {
        return super.getCapabilities();
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}

