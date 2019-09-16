/*
 *    InstanceOutputAttributesSelector.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package moa.learners.predictors.core.instancetransformers;

import java.util.ArrayList;
import java.util.List;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceImpl;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.predictions.MultiLabelClassificationPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.AbstractMOAObject;

/**
 * Transforms instances considering only a subset of output attributes
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 */
public class InstanceOutputAttributesSelector extends AbstractMOAObject implements InstanceTransformer {

	private static final long serialVersionUID = 1L;

	public InstancesHeader targetInstances;
	public int[] targetOutputIndices;
	public int numSourceInstancesOutputs;

	public InstanceOutputAttributesSelector() {

	}

	public InstanceOutputAttributesSelector(InstancesHeader sourceInstances, int[] targetOutputIndices) {
		this.targetOutputIndices = targetOutputIndices;
		this.numSourceInstancesOutputs = sourceInstances.numOutputAttributes();

		int totAttributes = sourceInstances.numInputAttributes() + this.targetOutputIndices.length;
		targetInstances = new InstancesHeader();

		List<Attribute> v = new ArrayList<>(totAttributes);
		int numInputs = sourceInstances.numInputAttributes();
		for (int i = 0; i < numInputs; i++) {
			v.add(sourceInstances.inputAttribute(i));
		}

		ArrayList<Integer> outputIndices = new ArrayList<>();
		for (int i = 0; i < this.targetOutputIndices.length; i++) {
			v.add(sourceInstances.outputAttribute(this.targetOutputIndices[i]));
			outputIndices.add(numInputs + i);
		}

		targetInstances.setAttributes(v);

		targetInstances.setOutputIndexes(outputIndices);
		targetInstances.setInputIndexes();
	}

	@Override
	public Instance sourceInstanceToTarget(Instance sourceInstance) {
		double[] attValues = new double[targetInstances.numAttributes()];
		Instance newInstance = new InstanceImpl(sourceInstance.weight(), attValues);
		int numInputs = this.targetInstances.numInputAttributes();
		for (int i = 0; i < numInputs; i++) {
			newInstance.setValue(i, sourceInstance.valueInputAttribute(i));
		}
		for (int i = 0; i < this.targetOutputIndices.length; i++) {
			newInstance.setValue(numInputs + i, sourceInstance.valueOutputAttribute(targetOutputIndices[i]));
		}
		newInstance.setDataset(targetInstances);
		return newInstance;
	}

	@Override
	public Prediction targetPredictionToSource(Prediction targetPrediction) {
		Prediction sourcePrediction = new MultiLabelClassificationPrediction(this.numSourceInstancesOutputs);
		for (int i = 0; i < targetPrediction.numOutputAttributes(); i++) {
			sourcePrediction.setVotes(this.targetOutputIndices[i], targetPrediction.getVotes(i));
		}
		return sourcePrediction;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

}
