/*
 *    SSQ.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
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
