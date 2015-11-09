package moa.classifiers.multilabel;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.Classifier;
import moa.classifiers.MultiLabelClassifier;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.trees.FIMTDD;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.learners.Learner;
import moa.options.ClassOption;

public class LocalMultiTargetClassifier extends AbstractMultiLabelLearner
		implements MultiLabelClassifier, MultiTargetRegressor {

	public ClassOption classifierOption = new ClassOption(
			"classifier",
			'c',
			"Classifier to use.",
			Classifier.class,
			"moa.classifiers.trees.FIMTDD");
	
	LinkedList<Classifier> classifiers = null;
	LinkedList<InstancesHeader> headers = null;
	
	@Override
	public boolean isRandomizable() {
		// TODO Auto-generated method stub
		return true;
	}

	private InstancesHeader getHeader(int target) {
		if (headers == null) 
			makeHeaders();
		return headers.get(target);
	
	}
	
	private void makeHeaders() {
		if (headers == null) {
			headers = new LinkedList<InstancesHeader>();
			for (int target = 0; target < getModelContext().numOutputAttributes(); target++) {
				List<Attribute> attributes = new LinkedList<Attribute>();
				List<Integer> indexValues = new LinkedList<Integer>();
				for (int i = 0; i < getModelContext().numInputAttributes(); i++) {
					attributes.add(getModelContext().inputAttribute(i));
					indexValues.add(i);
				}
				attributes.add(getModelContext().outputAttribute(target));
				indexValues.add(getModelContext().numInputAttributes());
				InstancesHeader STHeader = new InstancesHeader();
				STHeader.setAttributes(attributes, indexValues);
				STHeader.setClassIndex(getModelContext().numInputAttributes());
				headers.add(STHeader);
			}
		}
	}
	
	private Instance getSTInstance(MultiLabelInstance inst, int target) {
		double[] instanceData = new double[getModelContext().numInputAttributes() + 1];
		for (int i = 0; i < inst.numInputAttributes(); i++)
			instanceData[i] = inst.valueInputAttribute(i);
		instanceData[inst.numInputAttributes()] = inst.valueOutputAttribute(target);
		DenseInstance STInst = new DenseInstance(inst.weight(), instanceData);
		STInst.setDataset(getHeader(target));
		return STInst;
	}
	
	@Override
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		// TODO Auto-generated method stub
		initializeClassifiers();
		
		for (int j = 0; j < instance.numberOutputTargets(); j++) {
			Instance STInst = getSTInstance(instance, j);
			
			classifiers.get(j).trainOnInstance(STInst);
		}
	}

	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance inst) {
		initializeClassifiers();

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

	public void initializeClassifiers() {
		this.classifierRandom.setSeed(1234);
		if (classifiers == null) {
			makeHeaders();
			classifiers = new LinkedList<Classifier>();
			for (int i = 0; i < getModelContext().numOutputAttributes(); i++) {
				Classifier learner = (Classifier) getPreparedClassOption(this.classifierOption);
				classifiers.add(learner);
				learner.resetLearning();
				((AbstractClassifier) learner).classifierRandom = this.classifierRandom;
				learner.setModelContext(getHeader(i));
			}
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
 