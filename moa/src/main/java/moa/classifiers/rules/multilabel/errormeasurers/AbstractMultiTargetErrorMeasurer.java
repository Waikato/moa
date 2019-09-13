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

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.MultiLabelClassificationPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

public abstract class AbstractMultiTargetErrorMeasurer extends AbstractMultiLabelErrorMeasurer implements MultiLabelErrorMeasurer{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void addPrediction(Prediction prediction, Instance inst){
		int numOutputs=inst.numOutputAttributes();
		Prediction trueClass= new MultiLabelClassificationPrediction(numOutputs);
		for (int i=0; i<numOutputs; i++){
			trueClass.setVote(i, inst.valueOutputAttribute(i));
		}
		addPrediction(prediction, trueClass, inst.weight());
	}

}
