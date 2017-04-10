/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    CumulativeLinearNNSearch.java
 *    Copyright (C) 1999-2012 University of Waikato
 */

package moa.classifiers.lazy.neighboursearch;


import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceImpl;
import com.yahoo.labs.samoa.instances.Instances;
import moa.core.DoubleVector;
import moa.core.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class implementing cumulative sums of features to the brute force search algorithm for nearest neighbour search as an optimisation for feature selection of feature subsets.
 *
 * @author Lanqin Yuan (fyempathy@gmail.com)
 * @version 01.2016
 */
public class CumulativeLinearNNSearch
        implements Serializable
{

    /** for serialization. */
    private static final long serialVersionUID = 12345L;// some random numbers I decided to enter

    private int[] activeFeatures; // lower index = higher importance
    private double[][] featureDistance;
    private Instances window;

    private boolean print = false;

    private Inst[] instanceDistance;

    protected EuclideanDistance distanceFunction = new EuclideanDistance();

    private class Inst
    {
        double distance = -1;
        int index = -1;
        int skipCount = 0;
        public Inst(int index)
        {
            this.index = index;
        }
    }

    public CumulativeLinearNNSearch()
    {

    }

    /**
     * Sets the ranked list of features and the window to conduct the knn search on
     * @param target Instance to classify
     * @param window Window to conduct the search with
     * @param activeFeatureIndices Int array containing the indexes of the ranked features
     * @param upperBound Int containing the upper bound of features currently active (used for hill climbing)
     */
    public void initialiseCumulativeSearch(Instance target, Instances window,  int[] activeFeatureIndices,int upperBound)
    {
        this.window = window;
        distanceFunction.setInstances(window);
        activeFeatures = activeFeatureIndices;
        featureDistance = new double[upperBound][window.numInstances()];




        // f = index of index of best feature
        for (int f = 0; f < upperBound; f++)
        {
            for(int i = 0; i < window.numInstances();i++)
            {
                // we don't take care of the class index here, the active features array is assumed to NEVER contain the class index.
                // puts squared distance for a feature f between instance i and the target into array.
                double d = distanceFunction.attributeSqDistance(target,window.instance(i),activeFeatures[f]);//= sqDistance(target,window.instance(i),activeFeatures[f]);
				
                featureDistance[f][i] = d;


                //System.out.println("inst: " + i + " feature " + f + " dist: "+  d);
                //System.out.println(target.valueSparse(f) + " window "+ window.instance(i).valueSparse(f));



                /*if( Double.isNaN(d))
                {
                    System.out.println("SOMETHING SCREWED UP BIG TIME!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    System.out.println("feature being added: "+ activeFeatures[f] + " dist: " + d);
                    System.out.println( "tar value " + target.valueSparse(activeFeatures[f]));
                    System.out.println( "win value " + window.instance(i).valueSparse(activeFeatures[f]));
                    System.out.println("target values: " + target.toString());
                    System.out.println("instance values: " + window.instance(i).toString());
                }*/

            }
        }


        instanceDistance = new Inst[window.numInstances()];
        for(int i = 0; i < window.numInstances();i++)
        {
            instanceDistance[i] = new Inst(i);
        }

    }

    /**
     * Sets the number of features to be considered in kNN search.
     * Should be run before kNNSearch is run.
     * initialiseCumulativeSearch should be run before running this method
     * @param n number of features to consider
     */
    public void setNumberOfActiveFeatures(int n)
    {

        //System.out.println("it " + n);
        if(print)
        {
            System.out.print("asd[ ");
            for (int fea = 0; fea < n; fea++) {
                System.out.print(activeFeatures[fea] + ", ");
            }
            System.out.print("]\n");
        }

        for (int w = 0; w < window.numInstances();w++)
        {
            instanceDistance[w].distance = 0;
            for(int f = 0; f < n; f++)
            {
                //System.out.println("f distance: " + featureDistance[f][w]);
                instanceDistance[w].distance += featureDistance[f][w];
                /*if( Double.isNaN(instanceDistance[w]))
                {
                    System.out.println("SOMETHING SCREWED UP BIG TIME!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    System.out.println("instance distance: "+ w + " dist: " + instanceDistance[w]);
                    System.out.println("feature distance being added on distance: "+ f + " dist: " + featureDistance[f][w]);
                    System.out.println("instance values: " + window.instance(w).toString());
                }*/
            }
        }
    }


    /**
     * Actual knn search
     * initialise must be called beforehand
     * @param target Instances to search
     * @param kNN Number of nearest neighbours
     * @return A Instances of the k nearest neighbours
     */
    public Instances kNNSearchderp(Instance target,int kNN)
    {
        if(window.numInstances() < kNN)
        {
            //System.out.println("returned window");
            // less instances in window than k
            return window;
        }

        double[] searchArray = new double[instanceDistance.length];
        for(int i = 0; i < instanceDistance.length;i++)
        {
            searchArray[i] = instanceDistance[i].distance;
            // System.out.println("i " + i);
            // System.out.println("index " + searchList.get(i).index);
        }

        // System.out.println(Arrays.toString(searchArray));

        // find index of k-th smallest value
        int pivot = Utils.kthSmallestValueIndex(searchArray, kNN);

        Instances neighbours = new Instances(window,1);

        for(int i = 0; i < window.numInstances();i++)
        {
            if(instanceDistance[i].distance <= instanceDistance[pivot].distance)
            {
                neighbours.add(window.instance(i));
            }
        }
        //System.out.println("nei " + neighbours.numInstances());
        return neighbours;
    }

    /**
     * Actual knn search
     * initialise must be called beforehand
     * @param target Instances to search
     * @param kNN Number of nearest neighbours
     * @return A Instances of the k nearest neighbours
     */
    public Instances kNNSearch(Instance target,int kNN)
    {
        if(window.numInstances() < kNN)
        {
            //System.out.println("returned window");
            // less instances in window than k
            return window;
        }

        // list of inst to be searched in this knn search
        List<Inst> searchList = new ArrayList<Inst>();
        for(int i = 0; i < instanceDistance.length; i++)
        {
            if(instanceDistance[i].skipCount <= 0)
            {
                searchList.add(instanceDistance[i]);
            }
            else
            {
                instanceDistance[i].skipCount--;
            }
        }

        double[] searchArray = new double[searchList.size()];
        for(int i = 0; i < searchList.size();i++)
        {
            searchArray[i] = searchList.get(i).distance;
            // System.out.println("i " + i);
            // System.out.println("index " + searchList.get(i).index);
        }
        // System.out.println(Arrays.toString(searchArray));

        // find index of k-th smallest value
        int pivot = Utils.kthSmallestValueIndex(searchArray, kNN);

        Instances neighbours = new Instances(window,1);

        for(int i = 0; i < searchArray.length;i++)
        {
            if(searchArray[i] <= searchArray[pivot])
            {
                neighbours.add(window.instance(searchList.get(i).index));
            }
            else
            {
                int skip = (int)(Math.ceil(searchArray[i]) - searchArray[pivot]) - 1;
                //System.out.println("skip = " + skip);
                if (skip < 0)
                    skip = 0;
                searchList.get(i).skipCount = skip;
            }
        }
        return neighbours;
    }
}