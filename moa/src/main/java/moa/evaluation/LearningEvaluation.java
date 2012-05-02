/*
 *    LearningEvaluation.java
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
package moa.evaluation;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import moa.AbstractMOAObject;
import moa.classifiers.Classifier;
import moa.clusterers.Clusterer;
import moa.core.Measurement;

/**
 * Class that stores an array of evaluation measurements.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class LearningEvaluation extends AbstractMOAObject {

    private static final long serialVersionUID = 1L;

    protected Measurement[] measurements;

    public LearningEvaluation(Measurement[] measurements) {
        this.measurements = measurements.clone();
    }

    public LearningEvaluation(Measurement[] evaluationMeasurements,
            ClassificationPerformanceEvaluator cpe, Classifier model) {
        List<Measurement> measurementList = new LinkedList<Measurement>();
        measurementList.addAll(Arrays.asList(evaluationMeasurements));
        measurementList.addAll(Arrays.asList(cpe.getPerformanceMeasurements()));
        measurementList.addAll(Arrays.asList(model.getModelMeasurements()));
        this.measurements = measurementList.toArray(new Measurement[measurementList.size()]);
    }

    // Must change to Learner model
    public LearningEvaluation(Measurement[] evaluationMeasurements,
            LearningPerformanceEvaluator cpe, Clusterer model) {
        List<Measurement> measurementList = new LinkedList<Measurement>();
        measurementList.addAll(Arrays.asList(evaluationMeasurements));
        measurementList.addAll(Arrays.asList(cpe.getPerformanceMeasurements()));
        measurementList.addAll(Arrays.asList(model.getModelMeasurements()));
        this.measurements = measurementList.toArray(new Measurement[measurementList.size()]);
    }

    public Measurement[] getMeasurements() {
        return this.measurements.clone();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        Measurement.getMeasurementsDescription(this.measurements, sb, indent);
    }
}
