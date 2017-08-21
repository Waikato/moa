/*
 *    NoInstanceTransformation.java
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

import moa.AbstractMOAObject;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Performs no transformation. Returns
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 */

public class NoInstanceTransformation extends AbstractMOAObject implements InstanceTransformer{
	private static final long serialVersionUID = 1L;

	@Override
	public Instance sourceInstanceToTarget(Instance sourceInstance) {
		return sourceInstance; //.copy?
	}

	@Override
	public Prediction targetPredictionToSource(Prediction targetPrediction) {
		return targetPrediction; //.copy?
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}

}
