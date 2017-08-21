/*
 *    AbstractMultiTargetErrorMeasurer.java
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
package moa.classifiers.rules.multilabel.errormeasurers;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

public abstract class AbstractMultiTargetErrorMeasurer extends AbstractMultiLabelErrorMeasurer implements MultiLabelErrorMeasurer{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void addPrediction(Prediction prediction, MultiLabelInstance inst){
		int numOutputs=inst.numberOutputTargets();
		Prediction trueClass= new MultiLabelPrediction(numOutputs);
		for (int i=0; i<numOutputs; i++){
			trueClass.setVotes(i, new double[]{inst.valueOutputAttribute(i)});
		}
		addPrediction(prediction, trueClass, inst.weight());
	}

}
