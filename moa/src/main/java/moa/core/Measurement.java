/*
 *    Measurement.java
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
package moa.core;

import java.util.ArrayList;
import java.util.List;

import moa.AbstractMOAObject;

/**
 * Class for storing an evaluation measurement.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class Measurement extends AbstractMOAObject {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected double value;

    public Measurement(String name, double value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public double getValue() {
        return this.value;
    }

    public static Measurement getMeasurementNamed(String name,
            Measurement[] measurements) {
        for (Measurement measurement : measurements) {
            if (name.equals(measurement.getName())) {
                return measurement;
            }
        }
        return null;
    }

    public static void getMeasurementsDescription(Measurement[] measurements,
            StringBuilder out, int indent) {
        if (measurements.length > 0) {
            StringUtils.appendIndented(out, indent, measurements[0].toString());
            for (int i = 1; i < measurements.length; i++) {
                StringUtils.appendNewlineIndented(out, indent, measurements[i].toString());
            }

        }
    }

    public static Measurement[] averageMeasurements(Measurement[][] toAverage) {
        List<String> measurementNames = new ArrayList<String>();
        for (Measurement[] measurements : toAverage) {
            for (Measurement measurement : measurements) {
                if (measurementNames.indexOf(measurement.getName()) < 0) {
                    measurementNames.add(measurement.getName());
                }
            }
        }
        GaussianEstimator[] estimators = new GaussianEstimator[measurementNames.size()];
        for (int i = 0; i < estimators.length; i++) {
            estimators[i] = new GaussianEstimator();
        }
        for (Measurement[] measurements : toAverage) {
            for (Measurement measurement : measurements) {
                estimators[measurementNames.indexOf(measurement.getName())].addObservation(measurement.getValue(), 1.0);
            }
        }
        List<Measurement> averagedMeasurements = new ArrayList<Measurement>();
        for (int i = 0; i < measurementNames.size(); i++) {
            String mName = measurementNames.get(i);
            GaussianEstimator mEstimator = estimators[i];
            if (mEstimator.getTotalWeightObserved() > 1.0) {
                averagedMeasurements.add(new Measurement("[avg] " + mName,
                        mEstimator.getMean()));
                averagedMeasurements.add(new Measurement("[err] " + mName,
                        mEstimator.getStdDev()
                        / Math.sqrt(mEstimator.getTotalWeightObserved())));
            }
        }
        return averagedMeasurements.toArray(new Measurement[averagedMeasurements.size()]);
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        sb.append(getName());
        sb.append(" = ");
        if (getValue()>.001) {
                sb.append(StringUtils.doubleToString(getValue(),3));
        } else {
                sb.append(getValue());
        }
    }
}
