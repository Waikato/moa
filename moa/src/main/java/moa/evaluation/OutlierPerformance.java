/*
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
import java.util.Arrays;
import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;


public class OutlierPerformance extends MeasureCollection{
    @Override
    protected String[] getNames() {
        String[] names = {"time per object","needed?###"};
        return names;
    }

    @Override
    protected boolean[] getDefaultEnabled() {
        boolean [] defaults = {false, false};
        return defaults;
    }

    @Override
    public void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points) throws Exception {
        // nothing to do
    }
    
    public void addTimePerObject(double time) {
        addValue("time per object", time);
    }
}
