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
import java.io.StringReader;
import java.sql.Array;
import java.util.Arrays;
import java.util.List;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.lazy.neighboursearch.*;
import moa.core.AutoExpandVector;
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

/**
 * AED ranking function
 * @author Lanqin Yuan (fyempathy@gmail.com)
 * @version 1
 */
public class MeanEuclideanDistanceRanking extends RankingFunction
{
    Feature[][] classFeatureSums;

    /**
     * adds an instance to window
     * @param inst instance to be added
     */
    public void addInstance(Instance inst)
    {
        if( classFeatureSums == null)
            classFeatureSums = new Feature[inst.numClasses()][inst.numAttributes()];

        for(int a = 0;a < inst.numAttributes(); a++)
        {
            if(a != inst.classIndex() && !inst.isMissing(a))
            {
                if (inst.attribute(a).isNumeric())
                {
                    double v = inst.valueSparse(a);
                    if (!Double.isNaN(v))
                    {
                        if (classFeatureSums[(int) inst.classValue()][a] == null)
                        {
                            classFeatureSums[(int) inst.classValue()][a] = new Feature();
                            classFeatureSums[(int) inst.classValue()][a].isNum = true;
                        }
                        classFeatureSums[(int) inst.classValue()][a].sum += v;
                        classFeatureSums[(int) inst.classValue()][a].count++;
                    }
                }
                else if (inst.attribute(a).isNominal())
                {
                    if (classFeatureSums[(int) inst.classValue()][a] == null)
                    {
                        classFeatureSums[(int) inst.classValue()][a] = new Feature();
                        classFeatureSums[(int) inst.classValue()][a].isNum = false;
                    }
                    if (classFeatureSums[(int) inst.classValue()][a].nomTally == null)
                    {
                        classFeatureSums[(int) inst.classValue()][a].nomTally = new int[inst.attribute(a).numValues()];
                    }
                    // add a -1 if nominial values start at 1 to this line
                    classFeatureSums[(int) inst.classValue()][a].nomTally[(int) (inst.valueSparse(a))]++;
                    classFeatureSums[(int) inst.classValue()][a].count++;
                }
            }
        }
    }

    /**
     * remove instance from window
     * @param inst instance to be removed
     */
    public void removeInstance(Instance inst)
    {
        if( classFeatureSums == null)
            classFeatureSums = new Feature[inst.numClasses()][inst.numAttributes()];

        for(int a = 0;a < inst.numAttributes(); a++)
        {
            if(a != inst.classIndex() && !inst.isMissing(a))
            {
                if (inst.attribute(a).isNumeric())
                {
                    double v = inst.valueSparse(a);
                    if (!Double.isNaN(v))
                    {
                        if (classFeatureSums[(int) inst.classValue()][a] == null)
                        {
                            classFeatureSums[(int) inst.classValue()][a] = new Feature();
                            classFeatureSums[(int) inst.classValue()][a].isNum = true;
                        }
                        classFeatureSums[(int) inst.classValue()][a].sum -= v;
                        classFeatureSums[(int) inst.classValue()][a].count--;
                    }
                }
                else
                if (inst.attribute(a).isNominal())
                {
                    if (classFeatureSums[(int) inst.classValue()][a] == null)
                    {
                        classFeatureSums[(int) inst.classValue()][a] = new Feature();
                        classFeatureSums[(int) inst.classValue()][a].isNum = false;
                    }
                    if (classFeatureSums[(int) inst.classValue()][a].nomTally == null)
                    {
                        classFeatureSums[(int) inst.classValue()][a].nomTally = new int[inst.attribute(a).numValues()];
                    }
                    // add a -1 if nominial values start at 1 to this line
                    classFeatureSums[(int) inst.classValue()][a].nomTally[(int) (inst.valueSparse(a))]--;
                    classFeatureSums[(int) inst.classValue()][a].count--;
                }
            }
        }
    }

    /**
     * ranks features in the window
     * @param window
     * @param accuracyGain Array of previous accruacy difference (currently not used due to inconsistency)
     * @param previousBestFeatures Array of indiciesof previous ranked features (currently not used due to inconsistency)
     * @return
     */
    public int[] rankFeatures(Instances window, double[] accuracyGain, int[] previousBestFeatures)
    {

        // use EuclideanDistance class to normalise stuff
        EuclideanDistance distanceFunction = new EuclideanDistance();
        distanceFunction.setInstances(window);

        if( classFeatureSums == null)
            classFeatureSums = new Feature[window.numClasses()][window.numAttributes()];

        // using mean to determine average numeric value of class and mode to determine average nominal value of class

        double[] featureDistances = new double[window.numAttributes()];
        // calculating distance

        for (int c = 0; c < window.numClasses(); c++)
        {
            for(int a = 0;a < window.numAttributes(); a++)
            {
                if(a != window.classIndex())
                {
                    for (int j = c + 1; j < window.numClasses(); ++j)
                    {
                        if (classFeatureSums[c][a] != null && classFeatureSums[j][a] != null)
                        {
                            if (window.attribute(a).isNumeric())
                            {
                                double eucD = distanceFunction.normalise((classFeatureSums[c][a].getAverageValue()),a) - distanceFunction.normalise(classFeatureSums[j][a].getAverageValue(),a);
                                featureDistances[a] += eucD * eucD;
                            }
                            else
                            if (window.attribute(a).isNominal())
                            {
                                double eucD = 0;
                                for (int i = 0; i < window.attribute(a).numValues(); i++)
                                {
                                    double diff = (double)classFeatureSums[c][a].nomTally[i] / (double)classFeatureSums[c][a].count - (double)classFeatureSums[j][a].nomTally[i] / (double)classFeatureSums[j][a].count;
                                    //System.out.println("diff " + diff);
                                    eucD += diff * diff;
                                }
                                eucD /= window.attribute(a).numValues();
                                featureDistances[a] += eucD;
                            }

                        }
                    }
                }
            }
        }


        /*
        // accuracy gain (not used atm)
        if(accuracyGainWeight != 0 && previousBestFeatures!= null)
        {
            // simply add the accuracy gain ( which is between 0-1) multiplied by the weight onto the feature distance.
            for(int g = 0; g < accuracyGain.length;g++)
            {
                if(accuracyGain[g]>0)
                {
                    featureDistances[previousBestFeatures[g]] += accuracyGainWeight * accuracyGain[g];
                }
            }
        }
        */

        // set class index to -1 so its never selected (unless something is screwed up)
        featureDistances[window.classIndex()] = -1;

        for(int i = 0; i < featureDistances.length;i++) {
            // if distance is NaN, set it to -0.5 which is still larger than the class index but worse than every achievable squared distance.
            // this should only occur if there are so many missing attributes that there are no instances with the attribute for one or more classes
            if (Double.isNaN(featureDistances[i]))
            {
                featureDistances[i] = -0.5;
            }
        }

        // sort the features based on their distances between each class
        int[] rankedFeatures = sortFeatureArray(featureDistances,numberOfFeatures);
        return rankedFeatures;
    }


}