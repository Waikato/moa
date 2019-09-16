/*
 *    AbstractAnomalyDetector.java
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

package moa.learners.predictors.rules.core.anomalydetection;

import com.yahoo.labs.samoa.instances.Instance;

import moa.options.AbstractOptionHandler;

public abstract class AbstractAnomalyDetector extends AbstractOptionHandler implements AnomalyDetector {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public abstract boolean updateAndCheckAnomalyDetection(Instance instance);

	@Override
	public AnomalyDetector copy() {
		return (AnomalyDetector) super.copy();
	}

}
