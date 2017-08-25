package moa.evaluation;

/*
 * #%L
 * SAMOA
 * %%
 * Copyright (C) 2014 - 2015 Apache Software Foundation
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.gui.visualization.DataPoint;

import java.util.ArrayList;
import java.util.List;

public class Separation extends MeasureCollection {

  public Separation() {
    super();
  }

  @Override
  protected String[] getNames() {
    return new String[] { "BSS", "BSS-GT", "BSS-Ratio" };
  }

  private double getBSS(Clustering clustering, double[] mean) {
    double bss = 0.0;
    for (int i = 0; i < clustering.size(); i++) {
      double weight = clustering.get(i).getWeight();
      double sum = 0.0;
      for (int j = 0; j < mean.length; j++) {
        sum += Math.pow((mean[j] - clustering.get(i).getCenter()[j]), 2);
      }
      bss += weight * sum;
    }

    return bss;
  }

  @Override
  protected void evaluateClustering(Clustering clustering,
      Clustering trueClustering, ArrayList<DataPoint> points)
      throws Exception {
    double BSS_GT = 1.0;
    double BSS;
    int dimension = points.get(0).numAttributes() - 1;
    SphereCluster sc = new SphereCluster(points, dimension);

    // DO INTERNAL EVALUATION
    // clustering.getClustering().get(0).getCenter();

    BSS = getBSS(clustering, sc.getCenter());

    if (trueClustering != null) {
      String s = "";
      List<Instance> listInstances = new ArrayList<Instance>();
      for (Cluster c : trueClustering.getClustering()) {
        DenseInstance inst = new DenseInstance(c.getWeight(), c.getCenter());
        listInstances.add(inst);
        s += " " + c.getWeight();
      }
      SphereCluster gt = new SphereCluster(listInstances, dimension);
      BSS_GT = getBSS(trueClustering, gt.getCenter());
    }

    addValue("BSS", BSS);
    addValue("BSS-GT", BSS_GT);
    addValue("BSS-Ratio", BSS / BSS_GT);
  }
}
