/*
 *    StatisticalCollection.java
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
import java.util.Arrays;
import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;


public class StatisticalCollection extends MeasureCollection{
    private boolean debug = false;

    @Override
    protected String[] getNames() {
        //String[] names = {"van Dongen","Rand statistic", "C Index"};
        String[] names = {"van Dongen","Rand statistic"};
        return names;
    }

    @Override
    protected boolean[] getDefaultEnabled() {
        boolean [] defaults = {false, false};
        return defaults;
    }

    @Override
    public void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points) throws Exception {


    	MembershipMatrix mm = new MembershipMatrix(clustering, points);
        int numClasses = mm.getNumClasses();
        int numCluster = clustering.size()+1;
        int n = mm.getTotalEntries();

        double dongenMaxFC = 0;
        double dongenMaxSumFC = 0;
        for (int i = 0; i < numCluster; i++){
                double max = 0;
                for (int j = 0; j < numClasses; j++) {
                    if(mm.getClusterClassWeight(i, j)>max) max = mm.getClusterClassWeight(i, j);
                }
                dongenMaxFC+=max;
                if(mm.getClusterSum(i)>dongenMaxSumFC) dongenMaxSumFC = mm.getClusterSum(i);
        }

        double dongenMaxHC = 0;
        double dongenMaxSumHC = 0;
        for (int j = 0; j < numClasses; j++) {
                double max = 0;
                for (int i = 0; i < numCluster; i++){
                    if(mm.getClusterClassWeight(i, j)>max) max = mm.getClusterClassWeight(i, j);
                }
                dongenMaxHC+=max;
                if(mm.getClassSum(j)>dongenMaxSumHC) dongenMaxSumHC = mm.getClassSum(j);
        }

        double dongen = (dongenMaxFC + dongenMaxHC)/(2*n);
        //normalized dongen
        //double dongen = 1-(2*n - dongenMaxFC - dongenMaxHC)/(2*n - dongenMaxSumFC - dongenMaxSumHC);
        if(debug)
            System.out.println("Dongen HC:"+dongenMaxHC+" FC:"+dongenMaxFC+" Total:"+dongen+" n "+n);

        addValue("van Dongen", dongen);


        //Rand index
        //http://www.cais.ntu.edu.sg/~qihe/menu4.html
        double m1 = 0;
        for (int j = 0; j < numClasses; j++) {
            double v = mm.getClassSum(j);
            m1+= v*(v-1)/2.0;
        }
        double m2 = 0;
        for (int i = 0; i < numCluster; i++){
            double v = mm.getClusterSum(i);
            m2+= v*(v-1)/2.0;
        }

        double m = 0;
        for (int i = 0; i < numCluster; i++){
            for (int j = 0; j < numClasses; j++) {
                    double v = mm.getClusterClassWeight(i, j);
                    m+= v*(v-1)/2.0;
                }
        }
        double M = n*(n-1)/2.0;
        double rand = (M - m1 - m2 +2*m)/M;
        //normalized rand
        //double rand = (m - m1*m2/M)/(m1/2.0 + m2/2.0 - m1*m2/M);

        addValue("Rand statistic", rand);


        //addValue("C Index",cindex(clustering, points));
    }



    public double cindex(Clustering clustering,  ArrayList<DataPoint> points){
        int numClusters = clustering.size();
        double withinClustersDistance = 0;
        int numDistancesWithin = 0;
        double numDistances = 0;

        //double[] withinClusters = new double[numClusters];
        double[] minWithinClusters = new double[numClusters];
        double[] maxWithinClusters = new double[numClusters];
        ArrayList<Integer>[] pointsInClusters = new ArrayList[numClusters];
        for (int c = 0; c < numClusters; c++) {
            pointsInClusters[c] = new ArrayList<Integer>();
            minWithinClusters[c] = Double.MAX_VALUE;
            maxWithinClusters[c] = Double.MIN_VALUE;
        }

        for (int p = 0; p < points.size(); p++) {
            for (int c = 0; c < clustering.size(); c++) {
                if(clustering.get(c).getInclusionProbability(points.get(p)) > 0.8){
                    pointsInClusters[c].add(p);
                    numDistances++;
                }
            }
        }

        //calc within cluster distances + min and max values
        for (int c = 0; c < numClusters; c++) {
            int numDistancesInC = 0;
            ArrayList<Integer> pointsInC = pointsInClusters[c];
            for (int p = 0; p < pointsInC.size(); p++) {
                DataPoint point = points.get(pointsInC.get(p));
                for (int p1 = p+1; p1 < pointsInC.size(); p1++) {
                    numDistancesWithin++;
                    numDistancesInC++;
                    DataPoint point1 = points.get(pointsInC.get(p1));
                    double dist = point.getDistance(point1);
                    withinClustersDistance+=dist;
                    if(minWithinClusters[c] > dist) minWithinClusters[c] = dist;
                    if(maxWithinClusters[c] < dist) maxWithinClusters[c] = dist;
                }
            }
        }

        double minWithin = Double.MAX_VALUE;
        double maxWithin = Double.MIN_VALUE;
        for (int c = 0; c < numClusters; c++) {
            if(minWithinClusters[c] < minWithin)
               minWithin = minWithinClusters[c];
            if(maxWithinClusters[c] > maxWithin)
               maxWithin = maxWithinClusters[c];
        }

        double cindex = 0;
        if(numDistancesWithin != 0){
            double meanWithinClustersDistance = withinClustersDistance/numDistancesWithin;
            cindex = (meanWithinClustersDistance - minWithin)/(maxWithin-minWithin);
        }


        if(debug){
            System.out.println("Min:"+Arrays.toString(minWithinClusters));
            System.out.println("Max:"+Arrays.toString(maxWithinClusters));
            System.out.println("totalWithin:"+numDistancesWithin);
        }
        return cindex;
    }


}
