/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package moa.clusterers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.core.Measurement;
import moa.gui.visualization.DataPoint;
import moa.options.FloatOption;
import moa.options.IntOption;
import weka.core.Instance;

/**
 *
 * @author jansen
 */
public class ClusterGenerator extends AbstractClusterer{

    public IntOption timeWindowOption = new IntOption("timeWindow",
			't', "Rang of the window.", 1000);

    public FloatOption radiusDecreaseOption = new FloatOption("radiusDecrease", 'r',
                "The average radii of the centroids in the model.", 0, 0, 1);

    public FloatOption radiusIncreaseOption = new FloatOption("radiusIncrease", 'R',
                "The average radii of the centroids in the model.", 0, 0, 1);

    public FloatOption positionOffsetOption = new FloatOption("positionOffset", 'p',
                "The average radii of the centroids in the model.", 0, 0, 1);

//    public FloatOption microclusterOverlapOption = new FloatOption("microclusterOverlap", 'o',
//                "Allowed overlap of microclusters", 0.5, 0.0001, 1);

    private static double err_intervall_width = 0.0;
    private ArrayList<DataPoint> points;
    private int instanceCounter;
    private int windowCounter;
    private Random random;
    private double overlapThreshold;
    private int microInitMinPoints = 5;
    private Clustering sourceClustering = null;

    @Override
    public void resetLearningImpl() {
        points = new ArrayList<DataPoint>();
        instanceCounter = 0;
        windowCounter = 0;
        random = new Random(227);
//        overlapThreshold = microclusterOverlapOption.getValue();
        //evaluateMicroClusteringOption.set();
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if(windowCounter >= timeWindowOption.getValue()){
            points.clear();
            windowCounter = 0;
        }
        windowCounter++;
        instanceCounter++;
        points.add( new DataPoint(inst,instanceCounter));
    }

    @Override
    public boolean implementsMicroClusterer() {
        return true;
    }


    public void setSourceClustering(Clustering source){
        sourceClustering = source;
    }
    
    @Override
    public Clustering getMicroClusteringResult() {
        //System.out.println("Numcluster:"+clustering.size()+" / "+num);
        //Clustering source_clustering = new Clustering(points, overlapThreshold, microInitMinPoints);
        if(sourceClustering == null){
            System.out.println("You need to set a source clustering for the ClusterGenerator to work");
            return null;
        }
        return alterClustering(sourceClustering);
    }

    public Clustering getClusteringResult(){
        sourceClustering = new Clustering(points);
//        if(sourceClustering == null){
//            System.out.println("You need to set a source clustering for the ClusterGenerator to work");
//            return null;
//        }
        return alterClustering(sourceClustering);
    }


    private Clustering alterClustering(Clustering scclustering){
        //percentage of the radius that will be cut off
        //0: no changes to radius
        //1: radius of 0
        double errLevelRadiusDecrease = radiusDecreaseOption.getValue();

        //0: no changes to radius
        //1: radius 100% bigger
        double errLevelRadiusIncrease = radiusIncreaseOption.getValue();

        //0: no changes
        //1: distance between centers is 2 * original radius
        double errLevelPosition = positionOffsetOption.getValue();

        int numCluster = scclustering.size();
        double[] err_seeds = new double[numCluster];
        double err_seed_sum = 0.0;
        double tmp_seed;
        for (int i = 0; i < numCluster; i++) {
            tmp_seed = random.nextDouble();
            err_seeds[i] = err_seed_sum + tmp_seed;
            err_seed_sum+= tmp_seed;
        }

        double sumWeight = 0;
        for (int i = 0; i <numCluster; i++) {
            sumWeight+= scclustering.get(i).getWeight();
        }

        Clustering clustering = new Clustering();

        for (int i = 0; i <numCluster; i++) {
            if(!(scclustering.get(i) instanceof SphereCluster)){
                System.out.println("Not a Sphere Cluster");
                continue;
            }
            SphereCluster sourceCluster = (SphereCluster)scclustering.get(i);
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
                double length = 2 * radius * level;

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
                    if(newCenter[d] >= 0 && newCenter[d] <= 1){
                    }
                    else{
                        System.out.println("This shouldnt have happend, Cluster center out of bounds:"+Arrays.toString(newCenter));
                    }
                }
                //System.out.println("new Center "+Arrays.toString(newCenter));

            }
            
            //alter radius
            if(errLevelRadiusDecrease > 0 || errLevelRadiusIncrease > 0){
                double errOffset = random.nextDouble()*err_intervall_width/2.0;
                int errOffsetDirection = ((random.nextBoolean())? 1 : -1);

                if(errLevelRadiusDecrease > 0 && (errLevelRadiusIncrease == 0 || random.nextBoolean())){
                    double level = (errLevelRadiusDecrease + errOffsetDirection * errOffset);//*sourceCluster.getWeight()/sumWeight;
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

            clustering.add(newCluster);
        }
        return clustering;

    }






    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isRandomizable() {
        return false;
    }

    public double[] getVotesForInstance(Instance inst) {
        throw new UnsupportedOperationException("Not supported yet.");
    }



}


