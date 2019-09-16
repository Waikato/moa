/*
 *    MultiTargetRegressor.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package moa.learners.predictors;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

/**
 * MultiTargetRegressor interface for incremental MultiTarget regression models.
 * It is used only in the GUI MultiTargetRegression Tab.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public interface MultiTargetRegressor extends InstanceLearner {

	/**
	 * Gets whether this classifier correctly classifies an instance. Uses
	 * getPredictionForInstance to obtain the prediction and the instance to obtain
	 * its true class.
	 *
	 *
	 * @param inst the instance to be classified
	 * @return true if the instance is correctly classified
	 */
	// TODO Add this to AbstractLearner
	// public boolean correctlyClassifies(MultilabelInstance inst);

	/**
	 * Trains this learner incrementally using the given example.
	 *
	 * @param inst the instance to be used for training
	 */
	void trainOnInstanceImpl(Instance inst);

	/**
	 * Sets the reference to the header of the data stream. The header of the data
	 * stream is extended from WEKA <code>Instances</code>. This header is needed to
	 * know the number of classes and attributes
	 *
	 * @param ih the reference to the data stream header
	 */
	// public void setModelContext(InstancesHeader ih);

	/**
	 * Gets the reference to the header of the data stream. The header of the data
	 * stream is extended from WEKA <code>Instances</code>. This header is needed to
	 * know the number of classes and attributes
	 *
	 * @return the reference to the data stream header
	 */
	// public InstancesHeader getModelContext();

	@Override
	Prediction getPredictionForInstance(Instance inst);

	@Override
	InstanceLearner copy();
}
