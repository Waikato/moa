package moa.classifiers.multilabel;

import java.util.ArrayList;
import java.util.List;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.StructuredInstance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.Classifier;
import moa.classifiers.MultiLabelClassifier;
import moa.classifiers.MultiTargetRegressor;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.options.ClassOption;

public class LocalMultiTargetClassifier extends AbstractMultiLabelLearner
		implements MultiLabelClassifier, MultiTargetRegressor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5437749202546674734L;

	public ClassOption classifierOption = new ClassOption(
			"classifier",
			'c',
			"Classifier to use.",
			Classifier.class,
			"moa.classifiers.trees.FIMTDD");
	
	ArrayList<Classifier> classifiers = null;
	ArrayList<InstancesHeader> headers = null;

	@Override
	public long measureByteSize() {
		long size = 0;
		for (Classifier c : classifiers)
			size += c.measureByteSize();
		for (InstancesHeader h : headers) {
			size += SizeOf.sizeOf(h);
		}
		return size;
	}
	
	@Override
	public boolean isRandomizable() {
		return true;
	}

	private InstancesHeader getHeader(int target) {
		if (headers == null) 
			makeHeaders();
		return headers.get(target);
	
	}
	
	private void makeHeaders() {
		if (headers == null) {
			headers = new ArrayList<InstancesHeader>();
			for (int target = 0; target < getModelContext().numOutputAttributes(); target++) {
				List<Attribute> attributes = new ArrayList<Attribute>();
				for (int i = 0; i < getModelContext().numInputAttributes(); i++) {
					attributes.add(getModelContext().inputAttribute(i));
				}
				attributes.add(getModelContext().outputAttribute(target));
				InstancesHeader STHeader = new InstancesHeader();
				STHeader.setAttributes(attributes);
				STHeader.setInstances(new ArrayList<Instance>()); 
				STHeader.setClassIndex(getModelContext().numInputAttributes());
				headers.add(STHeader);
			}
		}
	}
	
	private Instance getSTInstance(StructuredInstance inst, int target) {
		double[] instanceData = new double[getModelContext().numInputAttributes() + 1];
		for (int i = 0; i < inst.numInputAttributes(); i++)
			instanceData[i] = inst.valueInputAttribute(i);
		instanceData[inst.numInputAttributes()] = inst.valueOutputAttribute(target);
		DenseInstance STInst = new DenseInstance(inst.weight(), instanceData);
		STInst.setDataset(getHeader(target));
		return STInst;
	}
	
	@Override
	public void trainOnInstanceImpl(StructuredInstance instance) {		
		for (int j = 0; j < instance.numberOutputTargets(); j++) {
			Instance STInst = getSTInstance(instance, j);
			
			classifiers.get(j).trainOnInstance(STInst);
		}
	}

	@Override
	public Prediction getPredictionForInstance(StructuredInstance inst) {
		MultiLabelPrediction prediction = new MultiLabelPrediction(getModelContext().numOutputAttributes());
		for (int j = 0; j < inst.numberOutputTargets(); j++) {
			Instance STInst = getSTInstance(inst, j);
			prediction.setVote(j, 1, classifiers.get(j).getVotesForInstance(STInst)[0]);
		}
		return prediction;
	}

	@Override
	public void resetLearningImpl() {
		classifiers = null;
	}
	
	public void modelContextSet() {
		initializeClassifiers();
		makeHeaders();
		for (int i = 0; i < classifiers.size(); i++)
			classifiers.get(i).setModelContext(getHeader(i));
	}

	public void initializeClassifiers() {
		classifiers = new ArrayList<Classifier>();
		for (int i = 0; i < getModelContext().numOutputAttributes(); i++) {
			Classifier learner = ((Classifier) getPreparedClassOption(this.classifierOption)).copy();
			classifiers.add(learner);
			learner.resetLearning();
			((AbstractClassifier) learner).classifierRandom = this.classifierRandom;
		}
	}
	
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		for (int i =0; i < getModelContext().numOutputAttributes(); i++) {
			out.append(getModelContext().outputAttribute(i).name() + '\n');
			((AbstractClassifier) classifiers.get(i)).getModelDescription(out, indent);
		}

	}

}
 