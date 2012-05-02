/*
 *    LearningCurve.java
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

import java.util.ArrayList;
import java.util.List;

import moa.AbstractMOAObject;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;

/**
 * Class that stores and keeps the history of evaluation measurements.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class LearningCurve extends AbstractMOAObject {

    private static final long serialVersionUID = 1L;

    protected List<String> measurementNames = new ArrayList<String>();

    protected List<double[]> measurementValues = new ArrayList<double[]>();

    public LearningCurve(String orderingMeasurementName) {
        this.measurementNames.add(orderingMeasurementName);
    }

    public String getOrderingMeasurementName() {
        return this.measurementNames.get(0);
    }

    public void insertEntry(LearningEvaluation learningEvaluation) {
        Measurement[] measurements = learningEvaluation.getMeasurements();
        Measurement orderMeasurement = Measurement.getMeasurementNamed(
                getOrderingMeasurementName(), measurements);
        if (orderMeasurement == null) {
            throw new IllegalArgumentException();
        }
        DoubleVector entryVals = new DoubleVector();
        for (Measurement measurement : measurements) {
            entryVals.setValue(addMeasurementName(measurement.getName()),
                    measurement.getValue());
        }
        double orderVal = orderMeasurement.getValue();
        int index = 0;
        while ((index < this.measurementValues.size())
                && (orderVal > this.measurementValues.get(index)[0])) {
            index++;
        }
        this.measurementValues.add(index, entryVals.getArrayRef());
    }

    public int numEntries() {
        return this.measurementValues.size();
    }

    protected int addMeasurementName(String name) {
        int index = this.measurementNames.indexOf(name);
        if (index < 0) {
            index = this.measurementNames.size();
            this.measurementNames.add(name);
        }
        return index;
    }

    public String headerToString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String name : this.measurementNames) {
            if (!first) {
                sb.append(',');
            } else {
                first = false;
            }
            sb.append(name);
        }
        return sb.toString();
    }

    public String entryToString(int entryIndex) {
        StringBuilder sb = new StringBuilder();
        double[] vals = this.measurementValues.get(entryIndex);
        for (int i = 0; i < this.measurementNames.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            if ((i >= vals.length) || Double.isNaN(vals[i])) {
                sb.append('?');
            } else {
                sb.append(Double.toString(vals[i]));
            }
        }
        return sb.toString();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        sb.append(headerToString());
        for (int i = 0; i < numEntries(); i++) {
            StringUtils.appendNewlineIndented(sb, indent, entryToString(i));
        }
    }

    public double getMeasurement(int entryIndex, int measurementIndex) {
        return this.measurementValues.get(entryIndex)[measurementIndex];
    }

    public String getMeasurementName(int measurementIndex) {
        return this.measurementNames.get(measurementIndex);
    }
}
