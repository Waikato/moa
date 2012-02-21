/*
 *    SSQ.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
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

public class SSQ extends MeasureCollection{

    public SSQ() {
        super();
    }

    @Override
    public String[] getNames() {
        String[] names = {"SSQ"};
        return names;
    }
    
  @Override
  protected boolean[] getDefaultEnabled() {
      boolean [] defaults = {false};
      return defaults;
  }
    
    public void evaluateClustering(Clustering clustering, Clustering trueClsutering, ArrayList<DataPoint> points) {
        double sum = 0.0;
        for (int p = 0; p < points.size(); p++) {
            //don't include noise
            if(points.get(p).classValue()==-1) continue;

            double minDistance = Double.MAX_VALUE;
            for (int c = 0; c < clustering.size(); c++) {
                double distance = 0.0;
                double[] center = clustering.get(c).getCenter();
                for (int i = 0; i < center.length; i++) {
                    double d = points.get(p).value(i) - center[i];
                    distance += d * d;
                }
                minDistance = Math.min(distance, minDistance);
            }
            
            sum+=minDistance;
        }
        
        addValue(0,sum);
    }





}
