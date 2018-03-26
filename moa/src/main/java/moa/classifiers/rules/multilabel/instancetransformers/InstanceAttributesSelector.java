/*
 *    InstanceAttributesSelector.java
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


package moa.classifiers.rules.multilabel.instancetransformers;

import java.util.ArrayList;
import java.util.List;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceImpl;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.Range;

/**
 * Transforms instances considering both a subset of input attributes
 * and a subset of output attributes
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 */
public class InstanceAttributesSelector extends InstanceOutputAttributesSelector implements InstanceTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InstancesHeader targetInstances;
	public int [] targetInputIndices;
	public int [] targetOutputIndices;
	public int numSourceInstancesOutputs;

	public InstanceAttributesSelector(InstancesHeader sourceInstances, int [] targetInputIndices, int [] targetOutputIndices){
		this.targetInputIndices=targetInputIndices;
		this.targetOutputIndices=targetOutputIndices;
		this.numSourceInstancesOutputs=sourceInstances.numOutputAttributes();


		int totAttributes=this.targetInputIndices.length+this.targetOutputIndices.length;
		targetInstances= new InstancesHeader();

		List<Attribute> v = new ArrayList<Attribute>(totAttributes);
		List<Integer> indexValues = new ArrayList<Integer>(totAttributes);
		int ct=0;
		for (int i=0; i<this.targetInputIndices.length;i++)
		{
			v.add(sourceInstances.inputAttribute(this.targetInputIndices[i]));
			indexValues.add(ct);
			ct++;
		}

		for (int i=0; i<this.targetOutputIndices.length;i++)
		{
			v.add(sourceInstances.outputAttribute(this.targetOutputIndices[i]));
			indexValues.add(ct);
			ct++;
		}

		targetInstances.setAttributes(v,indexValues);
		Range r= new Range("-" + targetOutputIndices.length);
		r.setUpper(totAttributes);
		targetInstances.setRangeOutputIndices(r);
	}

	@Override
	public Instance sourceInstanceToTarget(Instance sourceInstance) {
		double [] attValues = new double[targetInstances.numAttributes()];
		Instance newInstance=new InstanceImpl(sourceInstance.weight(),attValues);
		for (int i=0; i<this.targetInputIndices.length; i++){
			newInstance.setValue(i, sourceInstance.valueInputAttribute(targetInputIndices[i]));
		}
		for (int i=0; i<this.targetOutputIndices.length; i++){
			newInstance.setValue(i, sourceInstance.valueOutputAttribute(targetOutputIndices[i]));
		}
		newInstance.setDataset(targetInstances);
		return newInstance;
	}



	@Override
	public void getDescription(StringBuilder sb, int indent) {	
	}



}
