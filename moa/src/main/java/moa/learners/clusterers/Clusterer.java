/*
 *    Clusterer.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet@cs.waikato.ac.nz)
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
package moa.learners.clusterers;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.MOAObject;
import moa.cluster.Clustering;
import moa.core.Measurement;
import moa.gui.AWTRenderable;
import moa.options.OptionHandler;

public interface Clusterer extends MOAObject, OptionHandler, AWTRenderable {

	void setModelContext(InstancesHeader ih);

	InstancesHeader getModelContext();

	boolean isRandomizable();

	void setRandomSeed(int s);

	boolean trainingHasStarted();

	double trainingWeightSeenByModel();

	void resetLearning();

	void trainOnInstance(Instance inst);

	Prediction getPredictionForInstance(Instance inst);

	Measurement[] getModelMeasurements();

	Clusterer[] getSubClusterers();

	@Override
	Clusterer copy();

	Clustering getClusteringResult();

	boolean implementsMicroClusterer();

	Clustering getMicroClusteringResult();

	boolean keepClassLabel();

}
