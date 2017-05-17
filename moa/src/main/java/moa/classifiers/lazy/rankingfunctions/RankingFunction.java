/*
 *    kNNBFE.java
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

package moa.classifiers.lazy.rankingfunctions;
import java.io.Serializable;
import java.io.StringReader;
import java.sql.Array;
import java.util.Arrays;
import java.util.List;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.lazy.neighboursearch.*;
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import moa.core.Utils;

/**
 * Ranking function base class
 * @author Lanqin Yuan (fyempathy@gmail.com)
 * @version 1
 */
public abstract class RankingFunction implements Serializable
{
    protected int classIndex = -1; // uninitialised = -1
    protected int numberOfFeatures = -1;
    protected double accuracyGainWeight = 0;

    public abstract int[] rankFeatures(Instances window, double[] accuracyGain, int[] previousBestFeatures);

    /**
     * adds an instance to the window
     * Implementation dependent on individual functions
     * @param inst instance to be added
     */
    public abstract void addInstance(Instance inst);

    /**
     * removes an instance from the window.
     * assumes that the instance has already been added before to the window as otherwise counts may go into negatives and crash stuff.
     * Implementation dependent on individual functions
     * @param inst instance to be removed
     */
    public abstract void removeInstance(Instance inst);

    public void initialise(int numberOfFeatures, double accuracyGainWeight, int classIndex)
    {
        this.numberOfFeatures = numberOfFeatures;
        this.accuracyGainWeight = accuracyGainWeight;
        this.classIndex = classIndex;
    }



    //@TODO make better
    // sorts Descending
    protected int[] sortFeatureArray(double[] distances, int numFeatures)
    {
        int[] returnArray = new int[numFeatures];

        for(int i = 0; i <  numFeatures; ++i)
        {
            returnArray[i] = -1;
        }
		
        for(int i = 0; i <  numFeatures; i++)
        {
            double largest= Double.NEGATIVE_INFINITY;
            int largestIndex = -1;
            for(int z = 0; z < distances.length;z++)
            {
                if(z!=classIndex)
                {


                    if (distances[z] > largest&& !contains(returnArray, z))
                    {
                        largest = distances[z];
                        largestIndex = z;
                    }
                }
            }
            returnArray[i] = largestIndex;
        }

        return returnArray;
    }

    /**
     * checks if the index is in the array
     *
     * @param array
     * @param index
     * @return true if it is, false if no
     */
    private boolean contains (int[] array, int index)
    {
        for(int i = 0; i < array.length;i++)
        {
            if(array[i] == index)
                return true;
        }
        return false;
    }

}