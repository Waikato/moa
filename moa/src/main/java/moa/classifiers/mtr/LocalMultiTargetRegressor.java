package moa.classifiers.mtr;

import java.util.ArrayList;
import java.util.List;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.predictions.MultiTargetRegressionPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.classifiers.AbstractMultiTargetRegressor;
import moa.classifiers.AbstractRegressor;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.learners.MultiTargetRegressor;
import moa.learners.Regressor;
import moa.options.ClassOption;

public class LocalMultiTargetRegressor extends AbstractMultiTargetRegressor implements  MultiTargetRegressor {

	private static final long serialVersionUID = 1L;

	public ClassOption regressorOption = new ClassOption(
			"regressor",
			'c',
			"Single-target regressor to use.",
			Regressor.class,
			"moa.classifiers.trees.FIMTDD");
	
	ArrayList<Regressor> regressors = null;
	ArrayList<InstancesHeader> headers = null;

	@Override
	public long measureByteSize() {
		long size = 0;
		for (Regressor c : regressors)
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
	
	private Instance getSTInstance(Instance inst, int target) {
		double[] instanceData = new double[getModelContext().numInputAttributes() + 1];
		for (int i = 0; i < inst.numInputAttributes(); i++)
			instanceData[i] = inst.valueInputAttribute(i);
		instanceData[inst.numInputAttributes()] = inst.valueOutputAttribute(target);
		DenseInstance STInst = new DenseInstance(inst.weight(), instanceData);
		STInst.setDataset(getHeader(target));
		return STInst;
	}
	
	@Override
	public void trainOnInstanceImpl(Instance instance) {		
		for (int j = 0; j < instance.numOutputAttributes(); j++) {
			Instance STInst = getSTInstance(instance, j);
			
			regressors.get(j).trainOnInstance(STInst);
		}
	}

	@Override
	public Prediction getPredictionForInstance(Instance inst) {
		DoubleVector pred = new DoubleVector();
		// MultiTargetRegressionPrediction prediction = new MultiTargetRegressionPrediction(getModelContext().numOutputAttributes());
		for (int j = 0; j < inst.numOutputAttributes(); j++) {
			Instance STInst = getSTInstance(inst, j);
			pred.setValue(j, regressors.get(j).getPredictionForInstance(STInst).asDouble());
		}
		return new MultiTargetRegressionPrediction(pred);
	}

	@Override
	public void resetLearningImpl() {
		regressors = null;
	}
	
	public void modelContextSet() {
		initializeClassifiers();
		makeHeaders();
		for (int i = 0; i < regressors.size(); i++)
			regressors.get(i).setModelContext(getHeader(i));
	}

	public void initializeClassifiers() {
		regressors = new ArrayList<Regressor>();
		for (int i = 0; i < getModelContext().numOutputAttributes(); i++) {
			Regressor learner = ((Regressor) getPreparedClassOption(this.regressorOption)).copy();
			regressors.add(learner);
			learner.resetLearning();
			((AbstractRegressor) learner).classifierRandom = this.classifierRandom;
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
			regressors.get(i).getModelDescription(out, indent);
		}
	}

}
 