package moa.classifiers;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.StructuredInstance;

import moa.core.Example;

public abstract class AbstractMultiLabelLearner extends AbstractClassifier implements MultiLabelLearner {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    abstract public void trainOnInstanceImpl(StructuredInstance instance);

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        trainOnInstanceImpl((StructuredInstance) instance);
    }

    @Override
    public Prediction getPredictionForInstance(Example<Instance> example) {
        return getPredictionForInstance(example.getData());
    }

    @Override
    public Prediction getPredictionForInstance(Instance inst) {
        return getPredictionForInstance((StructuredInstance) inst);
    }

    abstract public Prediction getPredictionForInstance(StructuredInstance inst);

    @Override
    public double[] getVotesForInstance(Instance inst) {
        Prediction pred = getPredictionForInstance(inst);
        if (pred != null) {
            return pred.getVotes();
        } else {
            return new double[]{0}; //for compatibility with single target code
        }
    }

}
