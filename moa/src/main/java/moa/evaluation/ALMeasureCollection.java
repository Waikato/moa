/*
 *    ALMeasureCollection.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de)
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

import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;

/**
 * Collection of measures used to evaluate AL tasks.
 *
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 *
 */
public class ALMeasureCollection extends MeasureCollection implements ClassificationMeasureCollection {

	private static final long serialVersionUID = 1L;

    @Override
    protected String[] getNames() {
        return new String[]{"Accuracy", "Kappa", "Kappa Temp", "Ram-Hours",
                    "Time", "Memory", "Label Acq. Rate"};
    }

    @Override
    public void evaluateClustering(Clustering clustering, Clustering trueClsutering, ArrayList<DataPoint> points) {

    }

}