/*
 *    OutlierPerformance.java
 *    Copyright (C) 2013 Aristotle University of Thessaloniki, Greece
 *    @author D. Georgiadis, A. Gounaris, A. Papadopoulos, K. Tsichlas, Y. Manolopoulos
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
