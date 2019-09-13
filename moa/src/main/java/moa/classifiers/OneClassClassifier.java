/*
 *    OneClassClassifier.java
 *    Copyright (C) 2018 Richard Hugh Moulton
 *    @author Richard Hugh Moulton
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
package moa.classifiers;

import java.util.Collection;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * An interface for incremental classifier models. As an extension of MultiClassClassifier, these
 * classifiers appear in the GUI Classification Tab. Marking them as OneClassClassifiers also
 * allows them to appear specifically when one-class classifiers are needed.
 * 
 * @author Richard Hugh Moulton
 *
 */
public interface OneClassClassifier extends MultiClassClassifier
{
	/**
	 * Allows a one class classifier to be initialized with a starting set of training instances.
	 */
	public void initialize(Collection<Instance> trainingPoints);
	
	/**
	 * For use when an anomaly score is needed instead of a vote.
	 * The higher an instance's anomaly score is, the more likely it is an anomaly.
	 */
	public double getAnomalyScore(Instance inst);
}
