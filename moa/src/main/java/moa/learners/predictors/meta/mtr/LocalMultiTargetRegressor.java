package moa.learners.predictors.meta.mtr;

import java.util.ArrayList;
import java.util.List;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.predictions.MultiTargetRegressionPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.core.StringUtils;
import moa.learners.predictors.AbstractMultiTargetRegressor;
import moa.learners.predictors.AbstractRegressor;
import moa.learners.predictors.MultiTargetRegressor;
import moa.learners.predictors.Regressor;
import moa.options.ClassOption;

public class LocalMultiTargetRegressor extends AbstractMultiTargetRegressor implements MultiTargetRegressor {

	private static final long serialVersionUID = 1L;

	public ClassOption regressorOption = new ClassOption("regressor", 'c', "Single-target regressor to use.",
			Regressor.class, "trees.FIMTDD");

	protected List<Regressor> regressors = null;
	protected List<InstancesHeader> headers = null;

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
			headers = new ArrayList<>();
			for (int target = 0; target < getModelContext().numOutputAttributes(); target++) {
				List<Attribute> attributes = new ArrayList<>();
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
		for (int j = 0; j < inst.numOutputAttributes(); j++) {
			Instance STInst = getSTInstance(inst, j);
			pred.setValue(j, regressors.get(j).getPredictionForInstance(STInst).asDouble());
		}
		return new MultiTargetRegressionPrediction(pred);
	}

	
	@Override
	public void resetLearningImpl() {
		regressors = null;
		headers = null;
	}

	@Override
	public void modelContextSet() {
		initializeRegressors();
		makeHeaders();
		for (int i = 0; i < regressors.size(); i++)
			regressors.get(i).setModelContext(getHeader(i));
	}

	public void initializeRegressors() {
		regressors = new ArrayList<>();
		for (int i = 0; i < getModelContext().numOutputAttributes(); i++) {
			Regressor learner = (Regressor) ((Regressor) getPreparedClassOption(this.regressorOption)).copy();
			regressors.add(learner);
			learner.resetLearning();
			((AbstractRegressor) learner).classifierRandom = this.classifierRandom;
		}
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		List<Measurement> combined = new ArrayList<Measurement>();
		for (int i = 0; i < regressors.size(); i++) {
			Measurement[] measurements = regressors.get(i).getModelMeasurements();
			for (Measurement m : measurements) {
				combined.add(new Measurement(m.getName() + "[" + getModelContext().outputAttribute(i).name() + "]", m.getValue()));
			}
					
		}
		return (Measurement[]) combined.toArray();
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		for (int i = 0; i < getModelContext().numOutputAttributes(); i++) {
			StringUtils.appendIndented(out, indent, getModelContext().outputAttribute(i).name() + ":\n");
			regressors.get(i).getModelDescription(out, indent + 2);
		}
	}
	
}
