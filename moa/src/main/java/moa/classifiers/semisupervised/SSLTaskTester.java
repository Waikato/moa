package moa.classifiers.semisupervised;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

/***
 * This class shall be removed later. Just used to verify the EvaluateInterleavedTestThenTrainSSLDelayed
 * works as expected.
 */
public class SSLTaskTester extends AbstractClassifier implements SemiSupervisedLearner{

	protected long instancesWarmupCounter;
	protected long instancesLabeledCounter;
	protected long instancesUnlabeledCounter;
	protected long instancesTestCounter;
	
    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        super.prepareForUseImpl(monitor, repository);

        this.instancesTestCounter = 0;
        this.instancesUnlabeledCounter = 0;
        this.instancesLabeledCounter = 0;
    }

	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		// TODO Auto-generated method stub
		++this.instancesTestCounter;
		double[] dummy = new double[inst.numClasses()];
		return dummy;
	}

	@Override
	public void resetLearningImpl() {
		// TODO Auto-generated method stub
	}

	@Override
	public void addInitialWarmupTrainingInstances() {
    	++this.instancesWarmupCounter;
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		// TODO Auto-generated method stub
		++this.instancesLabeledCounter;
	}

	@Override
	public int trainOnUnlabeledInstance(Instance instance) {
    	++this.instancesUnlabeledCounter;
    	return -1;
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[]{
				new Measurement("#labeled", this.instancesLabeledCounter),
				new Measurement("#unlabeled", this.instancesUnlabeledCounter),
				new Measurement("#warmup", this.instancesWarmupCounter)
//                new Measurement("accuracy supervised learner", this.evaluatorSupervisedDebug.getPerformanceMeasurements()[1].getValue())
		};
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub
		
	}

}
