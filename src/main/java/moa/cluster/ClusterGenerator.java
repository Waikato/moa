/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package moa.cluster;

import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author jansen
 */
public class ClusterGenerator {

    private static double err_intervall_width = 0.0;

    public static Clustering shiftClustering(Clustering clustering, double errorLevel, Random random){

        //percentage of the radius that will be cut off
        //0: no changes to radius
        //1: radius of 0
        double errLevelRadiusDecrease = 0;

        //0: no changes to radius
        //1: radius 100% bigger
        double errLevelRadiusIncrease = 0;

        //0: no changes
        //1: distance between centers is 2 * original radius
        double errLevelPosition = 0;


        Clustering err_clusters = new Clustering();

        int numCluster = clustering.size();
        double[] err_seeds = new double[numCluster];
        double err_seed_sum = 0.0;
        double tmp_seed;
        for (int i = 0; i < numCluster; i++) {
            tmp_seed = random.nextDouble();
            err_seeds[i] = err_seed_sum + tmp_seed;
            err_seed_sum+= tmp_seed;
        }

        int sumWeight = 0;
        for (int i = 0; i <numCluster; i++) {
            sumWeight+= clustering.get(i).getWeight();
        }


        for (int i = 0; i <numCluster; i++) {
            if(!(clustering.get(i) instanceof SphereCluster)){
                System.out.println("Not a Sphere Cluster");
                continue;
            }
            SphereCluster sourceCluster = (SphereCluster)clustering.get(i);
            double[] center = Arrays.copyOf(sourceCluster.getCenter(),sourceCluster.getCenter().length);
            double weight = sourceCluster.getWeight();
            double radius = sourceCluster.getRadius();

            //move cluster center
            if(errLevelPosition >0){
                double errOffset = random.nextDouble()*err_intervall_width/2.0;
                double errOffsetDirection = ((random.nextBoolean())? 1 : -1);
                double level = errLevelPosition + errOffsetDirection * errOffset;
                double[] vector = new double[center.length];
                double vectorLength = 0;
                for (int d = 0; d < center.length; d++) {
                    vector[d] = (random.nextBoolean()?1:-1)*random.nextDouble();
                    vectorLength += Math.pow(vector[d],2);
                }
                vectorLength = Math.sqrt(vectorLength);

                
                //max is when clusters are next to each other
                double length = 2*radius * level;

                for (int d = 0; d < center.length; d++) {
                    //normalize length and then strecht to reach error position
                    vector[d]=vector[d]/vectorLength*length;
                }
//                System.out.println("Center "+Arrays.toString(center));
//                System.out.println("Vector "+Arrays.toString(vector));
                //check if error position is within bounds
                double [] newCenter = new double[center.length];
                for (int d = 0; d < center.length; d++) {
                    //check bounds, otherwise flip vector
                    if(center[d] + vector[d] >= 0 && center[d] + vector[d] <= 1){
                        newCenter[d] = center[d] + vector[d];
                    }
                    else{
                        newCenter[d] = center[d] + (-1)*vector[d];
                    }
                }
                center = newCenter;
                for (int d = 0; d < center.length; d++) {
                    if(center[d] + vector[d] >= 0 && center[d] + vector[d] <= 1){
                    }
                    else{
                        System.out.println("This shouldnt have happend, Cluster center out of bounds");
                    }
                }
                //System.out.println("new Center "+Arrays.toString(newCenter));

            }
            
            //alter radius
            if(errLevelRadiusDecrease > 0 || errLevelRadiusIncrease > 0){
                double errOffset = random.nextDouble()*err_intervall_width/2.0;
                int errOffsetDirection = ((random.nextBoolean())? 1 : -1);

                if(errLevelRadiusDecrease > 0 && (errLevelRadiusIncrease == 0 || random.nextBoolean())){
                    double level = errLevelRadiusDecrease + errOffsetDirection * errOffset;
                    level = (level<0)?0:level;
                    level = (level>1)?1:level;
                    radius*=(1-level);
                }
                else{
                    double level = errLevelRadiusIncrease + errOffsetDirection * errOffset;
                    level = (level<0)?0:level;
                    level = (level>1)?1:level;
                    radius+=radius*level;
                }
            }

            SphereCluster newCluster = new SphereCluster(center, radius, weight);
            newCluster.setMeasureValue("Source Cluster", "C"+sourceCluster.getId());
            err_clusters.add(newCluster);
        }
        return err_clusters;
    }

}
