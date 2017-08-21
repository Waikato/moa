/*
 *    AbstractFeatureRanking.java
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
package moa.classifiers.rules.featureranking;

import moa.classifiers.rules.multilabel.core.ObservableMOAObject;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

abstract public class AbstractFeatureRanking extends AbstractOptionHandler implements FeatureRanking{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	abstract public void update(ObservableMOAObject o, Object arg);
	

	abstract public DoubleVector getFeatureRankings();
	
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
	
	}

}
