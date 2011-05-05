package moa.evaluation;

import java.util.ArrayList;
import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;

/**
 *
 * @author jansen
 */
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
            int closest_cluster = -1;
            for (int c = 0; c < clustering.size(); c++) {
                double distance = 0.0;
                double[] center = clustering.get(c).getCenter();
                for (int i = 0; i < center.length; i++) {
                    double d = points.get(p).value(i) - center[i];
                    distance += d * d;
                    closest_cluster = c;
                }
                minDistance = Math.min(distance, minDistance);
            }
            
            //points.get(p).setSSQvalues(minDistance, closest_cluster);
            sum+=minDistance;
        }
        
        addValue(0,sum);
    }





}
