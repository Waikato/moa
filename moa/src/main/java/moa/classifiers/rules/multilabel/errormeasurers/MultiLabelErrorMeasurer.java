/*
 *    MultiLabelErrorMeasurer.java
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
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.options.OptionHandler;

public interface MultiLabelErrorMeasurer extends OptionHandler {

	public void addPrediction(Prediction prediction, Prediction trueClass, double weight);

	public void addPrediction(Prediction prediction, Prediction trueClass);

	public void addPrediction(Prediction prediction, Instance inst);

	public double getCurrentError();

	public double getCurrentError(int index);

	public double [] getCurrentErrors();

}
