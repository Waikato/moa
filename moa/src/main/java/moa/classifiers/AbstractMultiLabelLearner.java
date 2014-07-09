package moa.classifiers;

import moa.MOAObject;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.gui.AWTRenderer;
import moa.learners.Learner;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.Options;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

public abstract class AbstractMultiLabelLearner extends AbstractClassifier implements MultiLabelLearner {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public abstract void  trainOnInstanceImpl(MultiLabelInstance instance);
	
	public void  trainOnInstanceImpl(Instance instance){
		this.trainOnInstanceImpl((MultiLabelInstance)instance);
	}

}
