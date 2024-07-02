package moa.classifiers.predictioninterval;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.Capabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Regressor;
import moa.core.InstanceExample;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.evaluation.BasicRegressionPerformanceEvaluator;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;
import org.apache.commons.math3.distribution.NormalDistribution;

public class MVEPredictionInterval extends AbstractClassifier implements PredictionIntervalLearner {

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Learner to train.", Regressor.class, "moa.classifiers.meta.SelfOptimisingKNearestLeaves");

    public FloatOption confidenceLevelOption = new FloatOption("confidence",'c',"confidence level", 0.95,0,1);

    private NormalDistribution normalDistribution;

    private Regressor regressor;

    private BasicRegressionPerformanceEvaluator evaluator;

    @Override
    public void resetLearningImpl() {
        if (this.regressor == null) this.regressor = (Regressor) getPreparedClassOption(this.learnerOption);
        if (this.evaluator == null) this.evaluator = new BasicRegressionPerformanceEvaluator();

    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        InstanceExample example = new InstanceExample(inst);

        if (this.regressor == null) this.regressor = (Regressor) getPreparedClassOption(this.learnerOption);
        if (this.evaluator == null) this.evaluator = new BasicRegressionPerformanceEvaluator();

        double prediction = ((Learner) this.regressor).getVotesForInstance(example).length > 0 ? ((Learner) this.regressor).getVotesForInstance(example)[0] : 0;
        double interval = calculateBounds();


        return new double[]{prediction-interval, prediction, prediction + interval};
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        InstanceExample example = new InstanceExample(inst);
        this.evaluator.addResult(example, new double[]{((Learner) this.regressor).getVotesForInstance(example).length > 0 ? ((Learner) this.regressor).getVotesForInstance(example)[0] : 0});
        ((Learner)this.regressor).trainOnInstance(example);

    }

    @Override
    public void trainOnInstance(Instance inst) {
        trainOnInstanceImpl(inst);
    }


    private double calculateBounds(){
        if (this.normalDistribution == null) this.normalDistribution = new NormalDistribution();
        return this.normalDistribution.inverseCumulativeProbability(0.5 + this.confidenceLevelOption.getValue() / 2) * this.evaluator.getSquareError();
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
