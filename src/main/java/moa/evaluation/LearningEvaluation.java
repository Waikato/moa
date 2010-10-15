/*
 *    LearningEvaluation.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.evaluation;

import java.util.LinkedList;
import java.util.List;

import moa.AbstractMOAObject;
import moa.classifiers.Classifier;
import moa.clusterers.Clusterer;
import moa.core.Measurement;

public class LearningEvaluation extends AbstractMOAObject {

	private static final long serialVersionUID = 1L;

	protected Measurement[] measurements;

	public LearningEvaluation(Measurement[] measurements) {
		this.measurements = measurements.clone();
	}

	public LearningEvaluation(Measurement[] evaluationMeasurements,
			ClassificationPerformanceEvaluator cpe, Classifier model) {
		List<Measurement> measurementList = new LinkedList<Measurement>();
		for (Measurement measurement : evaluationMeasurements) {
			measurementList.add(measurement);
		}
		for (Measurement measurement : cpe.getPerformanceMeasurements()) {
			measurementList.add(measurement);
		}
		for (Measurement measurement : model.getModelMeasurements()) {
			measurementList.add(measurement);
		}
		this.measurements = measurementList
				.toArray(new Measurement[measurementList.size()]);
	}
	
	// Must change to Learner model
	public LearningEvaluation(Measurement[] evaluationMeasurements,
			LearningPerformanceEvaluator cpe, Clusterer model) {
		List<Measurement> measurementList = new LinkedList<Measurement>();
		for (Measurement measurement : evaluationMeasurements) {
			measurementList.add(measurement);
		}
		for (Measurement measurement : cpe.getPerformanceMeasurements()) {
			measurementList.add(measurement);
		}
		for (Measurement measurement : model.getModelMeasurements()) {
			measurementList.add(measurement);
		}
		this.measurements = measurementList
				.toArray(new Measurement[measurementList.size()]);
	}

	public Measurement[] getMeasurements() {
		return this.measurements.clone();
	}

	public void getDescription(StringBuilder sb, int indent) {
		Measurement.getMeasurementsDescription(this.measurements, sb, indent);
	}

}
