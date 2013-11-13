/**
 * [CMM_GTAnalysis.java]
 * 
 * CMM: Ground truth analysis
 * 
 * Reference: Kremer et al., "An Effective Evaluation Measure for Clustering on Evolving Data Streams", KDD, 2011
 * 
 * @author Timm jansen
 * Data Management and Data Exploration Group, RWTH Aachen University
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

/*
 * TODO:
 * - try to avoid calcualting the radius multiple times
 * - avoid the full distance map?
 * - knn functionality in clusters
 * - noise error
 */

package moa.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import moa.cluster.Clustering;
import moa.core.AutoExpandVector;
import moa.gui.visualization.DataPoint;
import com.yahoo.labs.samoa.instances.Instance;

public class CMM_GTAnalysis{
	
    /**
     * the given ground truth clustering
     */
    private Clustering gtClustering;
    
    /**
     * list of given points within the horizon  
     */
    private ArrayList<CMMPoint> cmmpoints;
    
    /**
     * the newly calculate ground truth clustering
     */
    private ArrayList<GTCluster> gt0Clusters;

    /**
     * IDs of noise points
     */
    private ArrayList<Integer> noise;
    
    /**
     * total number of points
     */
    private int numPoints;

    /**
     * number of clusters of the original ground truth 
     */
    private int numGTClusters;

    /**
     *    number of classes of the original ground truth, in case of a 
     *    micro clustering ground truth this differs from numGTClusters
     */
    private int numGTClasses;

    /**
     * number of classes after we are done with the analysis 
     */
    private int numGT0Classes;

    /**
     * number of dimensions
     */
    private int numDims;

    /**
     * mapping between true cluster ID/class label of the original ground truth 
     * and the internal cluster ID/working class label. 
     * 
     * different original cluster IDs might map to the same new cluster ID due to merging of two clusters 
     */
    private HashMap<Integer, Integer> mapTrueLabelToWorkLabel;

    /**
     * log of how clusters have been merged (for debugging)
     */
    private int[] mergeMap;

    /**
     * number of non-noise points that will create an error due to the underlying clustering model
     * (e.g. point being covered by two clusters representing different classes)
     */
    private int noiseErrorByModel;

    /**
     * number of noise points that will create an error due to the underlying clustering model
     * (e.g. noise point being covered by a cluster)
     */
    private int pointErrorByModel;    
    
    /**
     * CMM debug mode
     */
    private boolean debug = false;

    
    /******* CMM parameter ***********/

    /**
     * defines how many nearest neighbors will be used
     */
    private int knnNeighbourhood = 2;

    /**
     * the threshold which defines when ground truth clusters will be merged.
     * set to 1 to disable merging 
     */
    private double tauConnection = 0.5;
    
    /**
     *  experimental (default: disabled)
     *  separate k for points to cluster and cluster to cluster 
     */
    private double clusterConnectionMaxPoints = knnNeighbourhood;
    
    /** 
     * experimental (default: disabled)
     * use exponential connectivity function to model different behavior: 
     * closer points will have a stronger connection compared to the linear function.
     * Use ConnRefXValue and ConnX to better parameterize lambda, which controls 
     * the decay of the connectivity
     */
    private boolean useExpConnectivity = false;
    private double lambdaConnRefXValue = 0.01;
    private double lambdaConnX = 4;
    private double lamdaConn;
    
    
    /******************************************/
    
    
    /**
     * Wrapper class for data points to store CMM relevant attributes
     *
     */
    protected class CMMPoint extends DataPoint{
        /**
         * Reference to original point
         */
        protected DataPoint p = null;
        
        /**
         * point ID
         */
        protected int pID = 0;
        
        
        /**
         * true class label
         */
        protected int trueClass = -1;

        
        /**
         * the connectivity of the point to its cluster
         */
        protected double connectivity = 1.0;
        
        
        /**
         * knn distnace within own cluster
         */
        protected double knnInCluster = 0.0; 
        
        
        /**
         * knn indices (for debugging only)
         */
        protected ArrayList<Integer> knnIndices;

        public CMMPoint(DataPoint point, int id) {
            //make a copy, but keep reference
            super(point,point.getTimestamp());
            p = point;
            pID = id;
            trueClass = (int)point.classValue();
        }

        
        /**
         * Retruns the current working label of the cluster the point belongs to. 
         * The label can change due to merging of clusters.  
         * 
         * @return the current working class label
         */
        protected int workclass(){
            if(trueClass == -1 )
                return -1;
            else
                return mapTrueLabelToWorkLabel.get(trueClass);
        }
    }

    
    
    /**
     * Main class to model the new clusters that will be the output of the cluster analysis
     *
     */
    protected class GTCluster{
    	/** points that are per definition in the cluster */
        private ArrayList<Integer> points = new ArrayList<Integer>();
        
        /** a new GT cluster consists of one or more "old" GT clusters. 
         * Connected/overlapping clusters cannot be merged directly because of the 
         * underlying cluster model. E.g. for merging two spherical clusters the new 
         * cluster sphere can cover a lot more space then two separate smaller spheres. 
         * To keep the original coverage we need to keep the orignal clusters and merge
         * them on an abstract level. */
        private ArrayList<Integer> clusterRepresentations = new ArrayList<Integer>();
     
        /** current work class (changes when merging) */
        private int workclass;
        
        /** original work class */
        private final int orgWorkClass;
        
        /** original class label*/
        private final int label;
        
        /** clusters that have been merged into this cluster (debugging)*/
        private ArrayList<Integer> mergedWorkLabels = null;
        
        /** average knn distance of all points in the cluster*/
        private double knnMeanAvg = 0;
        
        /** average deviation of knn distance of all points*/
        private double knnDevAvg = 0;
        
        /** connectivity of the cluster to all other clusters */
        private ArrayList<Double> connections = new ArrayList<Double>();
        

        private GTCluster(int workclass, int label, int gtClusteringID) {
           this.orgWorkClass = workclass;
           this.workclass = workclass;
           this.label = label;
           this.clusterRepresentations.add(gtClusteringID);
        }

        
        /**
         * The original class label the cluster represents
         * @return original class label
         */
        protected int getLabel(){
            return label;
        }

        /**
         * Calculate the probability of the point being covered through the cluster
         * @param point to calculate the probability for
         * @return probability of the point being covered through the cluster
         */
        protected double getInclusionProbability(CMMPoint point){
            double prob = Double.MIN_VALUE;
            //check all cluster representatives for coverage
            for (int c = 0; c < clusterRepresentations.size(); c++) {
               double tmp_prob = gtClustering.get(clusterRepresentations.get(c)).getInclusionProbability(point);
               if(tmp_prob > prob) prob = tmp_prob;
            }
            return prob;
        }

        
        /**
         * calculate knn distances of points within own cluster 
         * + average knn distance and average knn distance deviation of all points 
         */
        private void calculateKnn(){
            for (int p0 : points) {
                CMMPoint cmdp = cmmpoints.get(p0);
                if(!cmdp.isNoise()){
                    AutoExpandVector<Double> knnDist = new AutoExpandVector<Double>();
                    AutoExpandVector<Integer> knnPointIndex = new AutoExpandVector<Integer>();
                    
                    //calculate nearest neighbours 
                    getKnnInCluster(cmdp, knnNeighbourhood, points, knnDist,knnPointIndex);

                    //TODO: What to do if we have less then k neighbours?
                    double avgKnn = 0;
                    for (int i = 0; i < knnDist.size(); i++) {
                        avgKnn+= knnDist.get(i);
                    }
                    if(knnDist.size()!=0)
                        avgKnn/=knnDist.size();
                    cmdp.knnInCluster = avgKnn;
                    cmdp.knnIndices = knnPointIndex;
                    cmdp.p.setMeasureValue("knnAvg", cmdp.knnInCluster);

                    knnMeanAvg+=avgKnn;
                    knnDevAvg+=Math.pow(avgKnn,2);
                }
            }
            knnMeanAvg=knnMeanAvg/(double)points.size();
            knnDevAvg=knnDevAvg/(double)points.size();

            double variance = knnDevAvg-Math.pow(knnMeanAvg,2.0);
            // Due to numerical errors, small negative values can occur.
            if (variance <= 0.0) variance = 1e-50;
            knnDevAvg = Math.sqrt(variance);

        }

        
        /**
         * Calculate the connection of a cluster to this cluster
         * @param otherCid cluster id of the other cluster
         * @param initial flag for initial run
         */
        private void calculateClusterConnection(int otherCid, boolean initial){
            double avgConnection = 0;
            if(workclass==otherCid){
                avgConnection = 1;
            }
            else{
                AutoExpandVector<Double> kmax = new AutoExpandVector<Double>();
                AutoExpandVector<Integer> kmaxIndexes = new AutoExpandVector<Integer>();

                for(int p : points){
                    CMMPoint cmdp = cmmpoints.get(p);
                    double con_p_Cj = getConnectionValue(cmmpoints.get(p), otherCid);
                    double connection = cmdp.connectivity * con_p_Cj;
                    if(initial){
                        cmdp.p.setMeasureValue("Connection to C"+otherCid, con_p_Cj);
                    }

                    //connection
                    if(kmax.size() < clusterConnectionMaxPoints || connection > kmax.get(kmax.size()-1)){
                        int index = 0;
                        while(index < kmax.size() && connection < kmax.get(index)) {
                            index++;
                        }
                        kmax.add(index, connection);
                        kmaxIndexes.add(index, p);
                        if(kmax.size() > clusterConnectionMaxPoints){
                            kmax.remove(kmax.size()-1);
                            kmaxIndexes.add(kmaxIndexes.size()-1);
                        }
                    }
                }
                //connection
                for (int k = 0; k < kmax.size(); k++) {
                    avgConnection+= kmax.get(k);
                }
                avgConnection/=kmax.size();
            }

            if(otherCid<connections.size()){
                connections.set(otherCid, avgConnection);
            }
            else
                if(connections.size() == otherCid){
                    connections.add(avgConnection);
                }
                else
                    System.out.println("Something is going really wrong with the connection listing!"+knnNeighbourhood+" "+tauConnection);
        }

        
        /**
         * Merge a cluster into this cluster
         * @param mergeID the ID of the cluster to be merged
         */
        private void mergeCluster(int mergeID){
            if(mergeID < gt0Clusters.size()){
                //track merging (debugging)
            	for (int i = 0; i < numGTClasses; i++) {
                    if(mergeMap[i]==mergeID)
                        mergeMap[i]=workclass;
                    if(mergeMap[i]>mergeID)
                        mergeMap[i]--;
                }
                GTCluster gtcMerge  = gt0Clusters.get(mergeID);
                if(debug)
                    System.out.println("Merging C"+gtcMerge.workclass+" into C"+workclass+
                            " with Con "+connections.get(mergeID)+" / "+gtcMerge.connections.get(workclass));


                //update mapTrueLabelToWorkLabel
                mapTrueLabelToWorkLabel.put(gtcMerge.label, workclass);
                Iterator iterator = mapTrueLabelToWorkLabel.keySet().iterator();
                while (iterator.hasNext()) {
                    Integer key = (Integer)iterator.next();
                    //update pointer of already merged cluster
                    int value = mapTrueLabelToWorkLabel.get(key);
                    if(value == mergeID)
                        mapTrueLabelToWorkLabel.put(key, workclass);
                    if(value > mergeID)
                        mapTrueLabelToWorkLabel.put(key, value-1);
                }

                //merge points from B into A
                points.addAll(gtcMerge.points);
                clusterRepresentations.addAll(gtcMerge.clusterRepresentations);
                if(mergedWorkLabels==null){
                    mergedWorkLabels = new ArrayList<Integer>();
                }
                mergedWorkLabels.add(gtcMerge.orgWorkClass);
                if(gtcMerge.mergedWorkLabels!=null)
                    mergedWorkLabels.addAll(gtcMerge.mergedWorkLabels);

                gt0Clusters.remove(mergeID);

                //update workclass labels
                for(int c=mergeID; c < gt0Clusters.size(); c++){
                    gt0Clusters.get(c).workclass = c;
                }

                //update knn distances
                calculateKnn();
                for(int c=0; c < gt0Clusters.size(); c++){
                    gt0Clusters.get(c).connections.remove(mergeID);
                    
                    //recalculate connection from other clusters to the new merged one
                    gt0Clusters.get(c).calculateClusterConnection(workclass,false);
                    //and from new merged one to other clusters
                    gt0Clusters.get(workclass).calculateClusterConnection(c,false);
                }
            }
            else{
                System.out.println("Merge indices are not valid");
            }
        }
    }

    
    /**
     * @param trueClustering the ground truth clustering
     * @param points data points
     * @param enableClassMerge allow class merging (should be set to true on default)
     */
    public CMM_GTAnalysis(Clustering trueClustering, ArrayList<DataPoint> points, boolean enableClassMerge){
        if(debug)
            System.out.println("GT Analysis Debug Output");

        noiseErrorByModel = 0;
        pointErrorByModel = 0;
        if(!enableClassMerge){
        	tauConnection = 1.0;
        }

        lamdaConn = -Math.log(lambdaConnRefXValue)/Math.log(2)/lambdaConnX;
        
        this.gtClustering = trueClustering;

        numPoints = points.size();
        numDims = points.get(0).numAttributes()-1;
        numGTClusters = gtClustering.size();

        //init mappings between work and true labels
        mapTrueLabelToWorkLabel = new HashMap<Integer, Integer>();
        
        //set up base of new clustering
        gt0Clusters = new ArrayList<GTCluster>();
        int numWorkClasses = 0;
        //create label to worklabel mapping as real labels can be just a set of unordered integers
        for (int i = 0; i < numGTClusters; i++) {
            int label = (int)gtClustering.get(i).getGroundTruth();
            if(!mapTrueLabelToWorkLabel.containsKey(label)){
                gt0Clusters.add(new GTCluster(numWorkClasses,label,i));
                mapTrueLabelToWorkLabel.put(label,numWorkClasses);
                numWorkClasses++;
            }
            else{
                gt0Clusters.get(mapTrueLabelToWorkLabel.get(label)).clusterRepresentations.add(i);
            }
        }
        numGTClasses = numWorkClasses;

        mergeMap = new int[numGTClasses];
        for (int i = 0; i < numGTClasses; i++) {
            mergeMap[i]=i;
        }

        //create cmd point wrapper instances
        cmmpoints = new ArrayList<CMMPoint>();
        for (int p = 0; p < points.size(); p++) {
            CMMPoint cmdp = new CMMPoint(points.get(p), p);
            cmmpoints.add(cmdp);
        }


        //split points up into their GTClusters and Noise (according to class labels)
        noise = new ArrayList<Integer>();
        for (int p = 0; p < numPoints; p++) {
            if(cmmpoints.get(p).isNoise()){
                noise.add(p);
            }
            else{
                gt0Clusters.get(cmmpoints.get(p).workclass()).points.add(p);
            }
        }

        //calculate initial knnMean and knnDev
        for (GTCluster gtc : gt0Clusters) {
            gtc.calculateKnn();
        }

        //calculate cluster connections
        calculateGTClusterConnections();

        //calculate point connections with own clusters
        calculateGTPointQualities();

        if(debug)
            System.out.println("GT Analysis Debug End");

   }

    /**
     * Calculate the connection of a point to a cluster
     *  
     * @param cmmp the point to calculate the connection for
     * @param clusterID the corresponding cluster
     * @return the connection value
     */
    //TODO: Cache the connection value for a point to the different clusters???
    protected double getConnectionValue(CMMPoint cmmp, int clusterID){
        AutoExpandVector<Double> knnDist = new AutoExpandVector<Double>();
        AutoExpandVector<Integer> knnPointIndex = new AutoExpandVector<Integer>();
        
        //calculate the knn distance of the point to the cluster
        getKnnInCluster(cmmp, knnNeighbourhood, gt0Clusters.get(clusterID).points, knnDist, knnPointIndex);

        //TODO: What to do if we have less then k neighbors?
        double avgDist = 0;
        for (int i = 0; i < knnDist.size(); i++) {
            avgDist+= knnDist.get(i);
        }
        //what to do if we only have a single point???
        if(knnDist.size()!=0)
            avgDist/=knnDist.size();
        else
            return 0;

        //get the upper knn distance of the cluster
        double upperKnn = gt0Clusters.get(clusterID).knnMeanAvg + gt0Clusters.get(clusterID).knnDevAvg;
        
        /* calculate the connectivity based on knn distance of the point within the cluster
           and the upper knn distance of the cluster*/ 
        if(avgDist < upperKnn){
            return 1;
        }
        else{
            //value that should be reached at upperKnn distance
            //Choose connection formula
            double conn;
            if(useExpConnectivity)
                conn = Math.pow(2,-lamdaConn*(avgDist-upperKnn)/upperKnn);
            else
                conn = upperKnn/avgDist;

            if(Double.isNaN(conn))
                System.out.println("Connectivity NaN at "+cmmp.p.getTimestamp());

            return conn;
        }
    }

    
    /**
     * @param cmmp point to calculate knn distance for
     * @param k number of nearest neighbors to look for
     * @param pointIDs list of point IDs to check
     * @param knnDist sorted list of smallest knn distances (can already be filled to make updates possible)  
     * @param knnPointIndex list of corresponding knn indices
     */
    private void getKnnInCluster(CMMPoint cmmp, int k,
                                 ArrayList<Integer> pointIDs,
                                 AutoExpandVector<Double> knnDist,
                                 AutoExpandVector<Integer> knnPointIndex) {

        //iterate over every point in the choosen cluster, cal distance and insert into list
        for (int p1 = 0; p1 < pointIDs.size(); p1++) {
            int pid = pointIDs.get(p1);
            if(cmmp.pID == pid) continue;
            double dist = distance(cmmp,cmmpoints.get(pid));
            if(knnDist.size() < k || dist < knnDist.get(knnDist.size()-1)){
                int index = 0;
                while(index < knnDist.size() && dist > knnDist.get(index)) {
                    index++;
                }
                knnDist.add(index, dist);
                knnPointIndex.add(index,pid);
                if(knnDist.size() > k){
                    knnDist.remove(knnDist.size()-1);
                    knnPointIndex.remove(knnPointIndex.size()-1);
                }
            }
        }
    }


    
    /**
     * calculate initial connectivities
     */
    private void calculateGTPointQualities(){
        for (int p = 0; p < numPoints; p++) {
            CMMPoint cmdp = cmmpoints.get(p);
            if(!cmdp.isNoise()){
                cmdp.connectivity = getConnectionValue(cmdp, cmdp.workclass());
                cmdp.p.setMeasureValue("Connectivity", cmdp.connectivity);
            }
        }
    }

    
    
    /**
     * Calculate connections between clusters and merge clusters accordingly as 
     * long as connections exceed threshold 
     */
    private void calculateGTClusterConnections(){
        for (int c0 = 0; c0 < gt0Clusters.size(); c0++) {
            for (int c1 = 0; c1 < gt0Clusters.size(); c1++) {
                    gt0Clusters.get(c0).calculateClusterConnection(c1, true);
            }
        }

        boolean changedConnection = true;
        while(changedConnection){
            if(debug){
                System.out.println("Cluster Connection");
                for (int c = 0; c < gt0Clusters.size(); c++) {
                    System.out.print("C"+gt0Clusters.get(c).label+" --> ");
                    for (int c1 = 0; c1 < gt0Clusters.get(c).connections.size(); c1++) {
                        System.out.print(" C"+gt0Clusters.get(c1).label+": "+gt0Clusters.get(c).connections.get(c1));
                    }
                    System.out.println("");
                }
                System.out.println("");
            }

            double max = 0;
            int maxIndexI = -1;
            int maxIndexJ = -1;

            changedConnection = false;
            for (int c0 = 0; c0 < gt0Clusters.size(); c0++) {
                for (int c1 = c0+1; c1 < gt0Clusters.size(); c1++) {
                    if(c0==c1) continue;
                        double min =Math.min(gt0Clusters.get(c0).connections.get(c1), gt0Clusters.get(c1).connections.get(c0));
                        if(min > max){
                            max = min;
                            maxIndexI = c0;
                            maxIndexJ = c1;
                        }
                }
            }
            if(maxIndexI!=-1 && max > tauConnection){
                gt0Clusters.get(maxIndexI).mergeCluster(maxIndexJ);
                if(debug)
                    System.out.println("Merging "+maxIndexI+" and "+maxIndexJ+" because of connection "+max);

                changedConnection = true;
            }
        }
        numGT0Classes = gt0Clusters.size();
    }

    
    /** 
     * Calculates how well the original clusters are separable. 
     * Small values indicate bad separability, values close to 1 indicate good separability 
     * @return index of seperability 
     */
    public double getClassSeparability(){
//        int totalConn = numGTClasses*(numGTClasses-1)/2;
//        int mergedConn = 0;
//        for(GTCluster gt : gt0Clusters){
//            int merged = gt.clusterRepresentations.size();
//            if(merged > 1)
//                mergedConn+=merged * (merged-1)/2;
//        }
//        if(totalConn == 0)
//            return 0;
//        else
//            return 1-mergedConn/(double)totalConn;
        return numGT0Classes/(double)numGTClasses;

    }

    
    /**
     * Calculates how well noise is separable from the given clusters
     * Small values indicate bad separability, values close to 1 indicate good separability
     * @return index of noise separability
     */
    public double getNoiseSeparability(){
        if(noise.isEmpty()) 
            return 1;

        double connectivity = 0;
        for(int p : noise){
            CMMPoint npoint = cmmpoints.get(p);
            double maxConnection = 0;

            //TODO: some kind of pruning possible. what about weighting?
            for (int c = 0; c < gt0Clusters.size(); c++) {
                double connection = getConnectionValue(npoint, c);
                if(connection > maxConnection)
                    maxConnection = connection;
            }
            connectivity+=maxConnection;
            npoint.p.setMeasureValue("MaxConnection", maxConnection);
        }

        return 1-(connectivity / noise.size());
    }

    
    /** 
     * Calculates the relative number of errors being caused by the underlying cluster model
     *  @return quality of the model 
     */
    public double getModelQuality(){
        for(int p = 0; p < numPoints; p++){
            CMMPoint cmdp = cmmpoints.get(p);
            for(int hc = 0; hc < numGTClusters;hc++){
                if(gtClustering.get(hc).getGroundTruth() != cmdp.trueClass){
                    if(gtClustering.get(hc).getInclusionProbability(cmdp) >= 1){
                        if(!cmdp.isNoise())
                            pointErrorByModel++;
                        else
                            noiseErrorByModel++;
                        break;
                    }
                }
            }
        }
        if(debug)
            System.out.println("Error by model: noise "+noiseErrorByModel+" point "+pointErrorByModel);

        return 1-((pointErrorByModel + noiseErrorByModel)/(double) numPoints);
    }

    
    /**
     * Get CMM internal point
     * @param index of the point
     * @return cmm point
     */
    protected CMMPoint getPoint(int index){
        return cmmpoints.get(index);
    }

    
    /**
     * Return cluster
     * @param index of the cluster to return
     * @return cluster
     */
    protected GTCluster getGT0Cluster(int index){
        return gt0Clusters.get(index);
    }

    /**
     * Number of classes/clusters of the new clustering
     * @return number of new clusters
     */
    protected int getNumberOfGT0Classes() {
        return numGT0Classes;
    }
    
    /**
     * Calculates Euclidian distance 
     * @param inst1 point as double array
     * @param inst2 point as double array
     * @return euclidian distance
     */
    private double distance(Instance inst1, Instance inst2){
          return distance(inst1, inst2.toDoubleArray());

    }
    
    /**
     * Calculates Euclidian distance 
     * @param inst1 point as an instance
     * @param inst2 point as double array
     * @return euclidian distance
     */
    private double distance(Instance inst1, double[] inst2){
        double distance = 0.0;
        for (int i = 0; i < numDims; i++) {
            double d = inst1.value(i) - inst2[i];
            distance += d * d;
        }
        return Math.sqrt(distance);
    }
    
    /**
     * String with main CMM parameters
     * @return main CMM parameter
     */
    public String getParameterString(){
        String para = "";
        para+="k="+knnNeighbourhood+";";
        if(useExpConnectivity){
	        para+="lambdaConnX="+lambdaConnX+";";
	        para+="lambdaConn="+lamdaConn+";";
	        para+="lambdaConnRef="+lambdaConnRefXValue+";";
        }
        para+="m="+clusterConnectionMaxPoints+";";
        para+="tauConn="+tauConnection+";";

        return para;
    }    
}


