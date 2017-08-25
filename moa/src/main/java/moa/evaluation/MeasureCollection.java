/*
 *    MeasureCollection.java
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
import java.util.HashMap;
import moa.AbstractMOAObject;
import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;

public abstract class MeasureCollection extends AbstractMOAObject{
    private String[] names;
    private ArrayList<Double>[] values;
    private ArrayList<Double>[] sortedValues;
    private ArrayList<String> events;
    
    private double[] minValue;
    private double[] maxValue;
    private double[] sumValues;
    private boolean[] enabled;
    private boolean[] corrupted;
    private double time;
    private boolean debug = true;
    private MembershipMatrix mm = null;

    private HashMap<String, Integer> map;

    private int numMeasures = 0;
    
    


     public MeasureCollection() {
        names = getNames();
        numMeasures = names.length;
        map = new HashMap<String, Integer>(numMeasures);        
        for (int i = 0; i < names.length; i++) {
             map.put(names[i],i);
        }
        values = (ArrayList<Double>[]) new ArrayList[numMeasures];
        sortedValues = (ArrayList<Double>[]) new ArrayList[numMeasures];
        maxValue = new double[numMeasures];
        minValue = new double[numMeasures];
        sumValues = new double[numMeasures];
        corrupted = new boolean[numMeasures];
        enabled = getDefaultEnabled();
        time = 0;
        events = new ArrayList<String>();

        for (int i = 0; i < numMeasures; i++) {
                values[i] = new ArrayList<Double>();
                sortedValues[i] = new ArrayList<Double>();
                maxValue[i] = Double.MIN_VALUE;
                minValue[i] = Double.MAX_VALUE;
                corrupted[i] = false;
                sumValues[i] = 0.0;
        }

    }

    protected abstract String[] getNames();

     public void addValue(int index, double value){
         if(Double.isNaN(value)){
        	 if(debug)
        		 System.out.println("NaN for "+names[index]);
             corrupted[index] = true;
         }
         //if(value < 0){
        //	 if(debug)
        //		 System.out.println("Negative value for "+names[index]);
        // }

         values[index].add(value);
         sumValues[index]+=value;
         if(value < minValue[index]) minValue[index] = value;
         if(value > maxValue[index]) maxValue[index] = value;
     }

     protected void addValue(String name, double value){
        if(map.containsKey(name)){
            addValue(map.get(name),value);
        }
        else{
            System.out.println(name+" is not a valid measure key, no value added");
        }
     }
     
     //add an empty entry e.g. if evaluation crashed internally
     public void addEmptyValue(int index){
         values[index].add(Double.NaN);
         corrupted[index] = true;
     }

     public int getNumMeasures(){
         return numMeasures;
     }

     public String getName(int index){
        return names[index];
     }

     public double getMaxValue(int index){
         return maxValue[index];
     }

     public double getMinValue(int index){
         return minValue[index];
     }

     public double getLastValue(int index){
         if(values[index].size()<1) return Double.NaN;
         return values[index].get(values[index].size()-1);
     }

     public double getMean(int index){
         if(corrupted[index] || values[index].size()<1)
             return Double.NaN;

         return sumValues[index]/values[index].size();
     }

     private void updateSortedValues(int index){
         //naive implementation of insertion sort
         for (int i = sortedValues[index].size(); i < values[index].size(); i++) {
             double v = values[index].get(i);
             int insertIndex = 0;
             while(!sortedValues[index].isEmpty() && insertIndex < sortedValues[index].size() && v > sortedValues[index].get(insertIndex))
                 insertIndex++;
             sortedValues[index].add(insertIndex,v);
         }
//         for (int i = 0; i < sortedValues[index].size(); i++) {
//             System.out.print(sortedValues[index].get(i)+" ");
//         }
//         System.out.println();
     }

     public void clean(int index){
         sortedValues[index].clear();
     }

     public double getMedian(int index){
         updateSortedValues(index);
         int size = sortedValues[index].size();

         if(size > 0){
             if(size%2 == 1)
                 return sortedValues[index].get((int)(size/2));
             else
                 return (sortedValues[index].get((size-1)/2)+sortedValues[index].get((size-1)/2+1))/2.0;
         }
         return Double.NaN;
    }

     public double getLowerQuartile(int index){
         updateSortedValues(index);
         int size = sortedValues[index].size();
         if(size > 11){
             return sortedValues[index].get(Math.round(size*0.25f));
         }
         return Double.NaN;
     }

     public double getUpperQuartile(int index){
         updateSortedValues(index);
         int size = sortedValues[index].size();
         if(size > 11){
             return sortedValues[index].get(Math.round(size*0.75f-1));
         }
         return Double.NaN;
     }


     public int getNumberOfValues(int index){
         return values[index].size();
     }

     public double getValue(int index, int i){
         if(i>=values[index].size()) return Double.NaN;
         return values[index].get(i);
     }

     public ArrayList<Double> getAllValues(int index){
         return values[index];
     }

     public void setEnabled(int index, boolean value){
         enabled[index] = value;
     }

     public boolean isEnabled(int index){
         return enabled[index];
     }

     public double getMeanRunningTime(){
         if(values[0].size()!=0)
            return (time/10e5/values[0].size());
         else
             return 0;
     }

     protected boolean[] getDefaultEnabled(){
         boolean[] defaults = new boolean[numMeasures];
         for (int i = 0; i < defaults.length; i++) {
             defaults[i] = true;
         }
         return defaults;
     }

     protected abstract void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points) throws Exception;

     /*
      * Evaluate Clustering
      *
      * return Time in milliseconds
      */
     public double evaluateClusteringPerformance(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points) throws Exception{
        long start = System.nanoTime();
        evaluateClustering(clustering, trueClustering, points);
        long duration = System.nanoTime()-start;
        time+=duration;
        duration/=10e5;
        return duration;
     }

     public void getDescription(StringBuilder sb, int indent) {

    }

     public void addEventType(String type){
    	 events.add(type);
     }
     public String getEventType(int index){
    	 if(index < events.size())
    		 return events.get(index);
    	 else
    		 return null;
     }
}

