/**
 * [CMM.java]
 * 
 * CMM: Main class
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

package moa.evaluation;

import java.util.ArrayList;

import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.evaluation.CMM_GTAnalysis.CMMPoint;
import moa.gui.visualization.DataPoint;


public class CMM extends MeasureCollection{
    
	
	private static final long serialVersionUID = 1L;

	/**
     * found clustering
     */
	private Clustering clustering;

	/**
	 * the ground truth analysis
	 */
    private CMM_GTAnalysis gtAnalysis;

    /**
     * number of points within the horizon
     */
    private int numPoints;
    
    /**
     * number of clusters in the found clustering
     */
    private int numFClusters;
    
    /**
     * number of cluster in the adjusted groundtruth clustering that 
     * was calculated through the groundtruth analysis 
     */
    private int numGT0Classes;

    /**
     * match found clusters to GT clusters
     */
    private int matchMap[]; 
    
    /**
     * pointInclusionProbFC[p][C] contains the probability of point p 
     * being included in cluster C 
     */
    private double[][] pointInclusionProbFC;
    
    /**
     * threshold that defines when a point is being considered belonging to a cluster
     */
    private double pointInclusionProbThreshold = 0.5;
    
    /**
     * parameterize the error weight of missed points (default 1)
     */
    private double lamdaMissed = 1;

    
    /**
     * enable/disable debug mode
     */
    public boolean debug = false;

    
    /**
     * enable/disable class merge (main feature of ground truth analysis)
     */
    public boolean enableClassMerge = true;
    
    /**
     * enable/disable model error
     * when enabled errors that are caused by the underling cluster model will not be counted
     */
    public boolean enableModelError = true;
    

    @Override
    protected String[] getNames() {
        String[] names = {"CMM","CMM Basic","CMM Missed","CMM Misplaced","CMM Noise",
                            "CA Seperability", "CA Noise", "CA Model"};
        return names;
    }

    @Override
    protected boolean[] getDefaultEnabled() {
        boolean [] defaults = {false, false, false, false, false, false, false, false};
        return defaults;
    }

    
    @Override
    public void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points) throws Exception{
        this.clustering = clustering;

        numPoints = points.size();
        numFClusters = clustering.size();

        gtAnalysis = new CMM_GTAnalysis(trueClustering, points, enableClassMerge);

        numGT0Classes = gtAnalysis.getNumberOfGT0Classes();

        addValue("CA Seperability",gtAnalysis.getClassSeparability());
        addValue("CA Noise",gtAnalysis.getNoiseSeparability());
        addValue("CA Model",gtAnalysis.getModelQuality());

        /* init the matching and point distances */
        calculateMatching();

        /* calculate the actual error */
        calculateError();
    }

    
    /**
     * calculates the CMM specific matching between found clusters and ground truth clusters 
     */
    private void calculateMatching(){

    	/**
    	 * found cluster frequencies  
    	 */
        int[][] mapFC = new int[numFClusters][numGT0Classes];

        /**
    	 * ground truth cluster frequencies
    	 */
        int[][] mapGT = new int[numGT0Classes][numGT0Classes];
        int [] sumsFC = new int[numFClusters];

        //calculate fuzzy mapping from
        pointInclusionProbFC = new double[numPoints][numFClusters];
        for (int p = 0; p < numPoints; p++) {
            CMMPoint cmdp = gtAnalysis.getPoint(p);
            //found cluster frequencies
            for (int fc = 0; fc < numFClusters; fc++) {
                Cluster cl = clustering.get(fc);
                pointInclusionProbFC[p][fc] = cl.getInclusionProbability(cmdp);
                if (pointInclusionProbFC[p][fc] >= pointInclusionProbThreshold) {
                    //make sure we don't count points twice that are contained in two merged clusters
                    if(cmdp.isNoise()) continue;
                    mapFC[fc][cmdp.workclass()]++;
                    sumsFC[fc]++;
                }
            }

            //ground truth cluster frequencies
            if(!cmdp.isNoise()){
                for(int hc = 0; hc < numGT0Classes;hc++){
                    if(hc == cmdp.workclass()){
                        mapGT[hc][hc]++;
                    }
                    else{
                        if(gtAnalysis.getGT0Cluster(hc).getInclusionProbability(cmdp) >= 1){
                            mapGT[hc][cmdp.workclass()]++;
                        }
                    }
                }
            }
        }

        //assign each found cluster to a hidden cluster
        matchMap = new int[numFClusters];
        for (int fc = 0; fc < numFClusters; fc++) {
            int matchIndex = -1;
            //check if we only have one entry anyway
            for (int hc0 = 0; hc0 < numGT0Classes; hc0++) {
                if(mapFC[fc][hc0]!=0){
                    if(matchIndex == -1)
                        matchIndex = hc0;
                    else{
                        matchIndex = -1;
                        break;
                    }
                }
            }

            //more then one entry, so look for most similar frequency profile
            int minDiff = Integer.MAX_VALUE;
            if(sumsFC[fc]!=0 && matchIndex == -1){
                ArrayList<Integer> fitCandidates = new ArrayList<Integer>();
                for (int hc0 = 0; hc0 < numGT0Classes; hc0++) {
                    int errDiff = 0;
                    for (int hc1 = 0; hc1 < numGT0Classes; hc1++) {
                        //fc profile doesn't fit into current hc profile
                        double freq_diff = mapFC[fc][hc1] - mapGT[hc0][hc1];
                        if(freq_diff > 0){
                            errDiff+= freq_diff;
                        }
                    }
                    if(errDiff == 0){
                        fitCandidates.add(hc0);
                    }
                    if(errDiff < minDiff){
                        minDiff = errDiff;
                        matchIndex = hc0;
                    }
                    if(debug){
                        //System.out.println("FC"+fc+"("+Arrays.toString(mapFC[fc])+") - HC0_"+hc0+"("+Arrays.toString(mapGT[hc0])+"):"+errDiff);
                    }
                }
                //if we have a fitting profile overwrite the min error choice
                //if we have multiple fit candidates, use majority vote of corresponding classes
                if(fitCandidates.size()!=0){
                    int bestGTfit = fitCandidates.get(0);
                    for(int i = 1; i < fitCandidates.size(); i++){
                        int GTfit = fitCandidates.get(i);
                        if(mapFC[fc][GTfit] > mapFC[fc][bestGTfit])
                            bestGTfit=fitCandidates.get(i);
                    }
                    matchIndex = bestGTfit;
                }
            }
            
            matchMap[fc] = matchIndex;
            int realMatch = -1;
            if(matchIndex==-1){
                if(debug)
                    System.out.println("No cluster match: needs to be implemented?");
            }
            else{
                realMatch = gtAnalysis.getGT0Cluster(matchMap[fc]).getLabel();
            }
            clustering.get(fc).setMeasureValue("CMM Match", "C"+realMatch);
            clustering.get(fc).setMeasureValue("CMM Workclass", "C"+matchMap[fc]);
        }

        //print matching table
        if(debug){
            for (int i = 0; i < numFClusters; i++) {
                    System.out.print("C"+((int)clustering.get(i).getId()) + " N:"+((int)clustering.get(i).getWeight())+"  |  ");
                    for (int j = 0; j < numGT0Classes; j++) {
                          System.out.print(mapFC[i][j] + " ");
                    }
                    System.out.print(" = "+sumsFC[i] + " | ");
                    String match = "-";
                    if (matchMap[i]!=-1) {
                        match = Integer.toString(gtAnalysis.getGT0Cluster(matchMap[i]).getLabel());
                    }
                    System.out.println(" --> " + match + "(work:"+matchMap[i]+")");
             }
        }
    }
    
    
    /**
     * Calculate the actual error values
     */
    private void calculateError(){
        int totalErrorCount = 0;
        int totalRedundancy = 0;
        int trueCoverage = 0;
        int totalCoverage = 0;

        int numNoise = 0;
        double errorNoise = 0;
        double errorNoiseMax = 0;

        double errorMissed = 0;
        double errorMissedMax = 0;

        double errorMisplaced = 0;
        double errorMisplacedMax = 0;

        double totalError = 0.0;
        double totalErrorMax = 0.0;

        /** mainly iterate over all points and find the right error value for the point.
         *  within the same run calculate various other stuff like coverage etc...
         */
        for (int p = 0; p < numPoints; p++) {
            CMMPoint cmdp = gtAnalysis.getPoint(p);
            double weight = cmdp.weight();
            //noise counter
            if(cmdp.isNoise()){
                numNoise++;
                //this is always 1
                errorNoiseMax+=cmdp.connectivity*weight;
            }
            else{
                errorMissedMax+=cmdp.connectivity*weight;
                errorMisplacedMax+=cmdp.connectivity*weight;
            }
            //sum up maxError as the individual errors are the quality weighted between 0-1
            totalErrorMax+=cmdp.connectivity*weight;


            double err = 0;
            int coverage = 0;

            //check every FCluster
            for (int c = 0; c < numFClusters; c++) {
                //contained in cluster c?
                if(pointInclusionProbFC[p][c] >= pointInclusionProbThreshold){
                    coverage++;

                    if(!cmdp.isNoise()){
                        //PLACED CORRECTLY
                        if(matchMap[c] == cmdp.workclass()){
                        }
                        //MISPLACED
                        else{
                            double errvalue = misplacedError(cmdp, c);
                            if(errvalue > err)
                                err = errvalue;
                        }
                    }
                    else{
                        //NOISE
                        double errvalue = noiseError(cmdp, c);
                        if(errvalue > err) err = errvalue;
                    }
                }
            }
            //not in any cluster
            if(coverage == 0){
                //MISSED
                if(!cmdp.isNoise()){
                    err = missedError(cmdp,true);
                    errorMissed+= weight*err;
                }
                //NOISE
                else{
                }
            }
            else{
                if(!cmdp.isNoise()){
                    errorMisplaced+= err*weight;
                }
                else{
                    errorNoise+= err*weight;
                }
            }

            /* processing of other evaluation values */
            totalError+= err*weight;
            if(err!=0)totalErrorCount++;
            if(coverage>0) totalCoverage++;  //points covered by clustering (incl. noise)
            if(coverage>0 && !cmdp.isNoise()) trueCoverage++; //points covered by clustering, don't count noise
            if(coverage>1) totalRedundancy++; //include noise

            cmdp.p.setMeasureValue("CMM",err);
            cmdp.p.setMeasureValue("Redundancy", coverage);
        }

        addValue("CMM", (totalErrorMax!=0)?1-totalError/totalErrorMax:1);
        addValue("CMM Missed", (errorMissedMax!=0)?1-errorMissed/errorMissedMax:1);
        addValue("CMM Misplaced", (errorMisplacedMax!=0)?1-errorMisplaced/errorMisplacedMax:1);
        addValue("CMM Noise", (errorNoiseMax!=0)?1-errorNoise/errorNoiseMax:1);
        addValue("CMM Basic", 1-((double)totalErrorCount/(double)numPoints));

        if(debug){
            System.out.println("-------------");
        }
    }


    private double noiseError(CMMPoint cmdp, int assignedClusterID){
        int gtAssignedID = matchMap[assignedClusterID];
        double error;
        
        //Cluster wasn't matched, so just contains noise
        //TODO: Noiscluster?
        //also happens when we decrease the radius and there is only a noise point in the center
        if(gtAssignedID==-1){
            error = 1;
            cmdp.p.setMeasureValue("CMM Type","noise - cluster");
        }
        else{
            if(enableModelError && gtAnalysis.getGT0Cluster(gtAssignedID).getInclusionProbability(cmdp) >= pointInclusionProbThreshold){
                //set to MIN_ERROR so we can still track the error
            	error = 0.00001;
                cmdp.p.setMeasureValue("CMM Type","noise - byModel");
            }
            else{
                error = 1 - gtAnalysis.getConnectionValue(cmdp, gtAssignedID);
                cmdp.p.setMeasureValue("CMM Type","noise");
            }
        }

        return error;
    }

    private double missedError(CMMPoint cmdp, boolean useHullDistance){
        cmdp.p.setMeasureValue("CMM Type","missed");
        if(!useHullDistance){
            return cmdp.connectivity;
        }
        else{
            //main idea: look at relative distance of missed point to cluster
            double minHullDist = 1;
            for (int fc = 0; fc < numFClusters; fc++){
                //if fc is mappend onto the class of the point, check it for its hulldist
                if(matchMap[fc]!=-1 && matchMap[fc] == cmdp.workclass()){
                    if(clustering.get(fc) instanceof SphereCluster){
                        SphereCluster sc = (SphereCluster)clustering.get(fc);
                        double distanceFC = sc.getCenterDistance(cmdp);
                        double radius = sc.getRadius();
                        double hullDist = (distanceFC-radius)/(distanceFC+radius);
                        if(hullDist < minHullDist)
                            minHullDist = hullDist;
                    }
                    else{
                        double min = 1;
                        double max = 1;

                        //TODO: distance for random shape
                        //generate X points from the cluster with clustering.get(fc).sample(null)
                        //and find Min and Max values

                        double hullDist = min/max;
                        if(hullDist < minHullDist)
                            minHullDist = hullDist;
                    }
                }
            }

            //use distance as weight
            if(minHullDist>1) minHullDist = 1;

            double weight = (1-Math.exp(-lamdaMissed*minHullDist));
            cmdp.p.setMeasureValue("HullDistWeight",weight);

            return weight*cmdp.connectivity;
        }
    }


    private double misplacedError(CMMPoint cmdp, int assignedClusterID){
        double weight = 0;

        int gtAssignedID = matchMap[assignedClusterID];
        //TODO take care of noise cluster?
        if(gtAssignedID ==-1){
            System.out.println("Point "+cmdp.getTimestamp()+" from gtcluster "+cmdp.trueClass+" assigned to noise cluster "+assignedClusterID);
            return 1;
        }

        if(gtAssignedID == cmdp.workclass())
            return 0;
        else{
            //assigned and real GT0 cluster are not connected, but does the model have the
            //chance of separating this point after all?
            if(enableModelError && gtAnalysis.getGT0Cluster(gtAssignedID).getInclusionProbability(cmdp) >= pointInclusionProbThreshold){
                weight = 0;
                cmdp.p.setMeasureValue("CMM Type","missplaced - byModel");
            }
            else{
                //point was mapped onto wrong cluster (assigned), so check how far away
                //the nearest point is within the wrongly assigned cluster
                weight = 1 - gtAnalysis.getConnectionValue(cmdp, gtAssignedID);
            }
        }
        double err_value;
        //set to MIN_ERROR so we can still track the error
        if(weight == 0){
            err_value= 0.00001;
        }
        else{
            err_value = weight*cmdp.connectivity;
            cmdp.p.setMeasureValue("CMM Type","missplaced");
        }

        return err_value;
    }

    public String getParameterString(){
        String para = gtAnalysis.getParameterString();
        para+="lambdaMissed="+lamdaMissed+";";
        return para;
    }

}


