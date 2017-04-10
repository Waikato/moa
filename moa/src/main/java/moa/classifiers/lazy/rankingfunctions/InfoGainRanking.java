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
import java.util.HashMap;
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
 * IG ranking function
 * @author Lanqin Yuan (fyempathy@gmail.com)
 * @version 1
 */
public class InfoGainRanking extends RankingFunction
{
    protected abstract class FeatureStats
    {
        public abstract void addInst(Instance inst);
        public abstract void removeInst(Instance inst);
        // count of how many instances are not missing the attribute
        int instCount = 0;
        // tally number of times each class appears
        int[] classCount;
        int attributeIndex = -1;
        public FeatureStats(int attribute)
        {
            attributeIndex = attribute;

        }


    }
    protected class NominalFeatureStats extends FeatureStats
    {
        // tally of number of times an instance with a value V for the attribute is of a class
        int[][] varCount;
        // count of the number of times each value appears for the attribute in the window
        int[] varTotalCount;
        public void addInst(Instance inst)
        {
            if(varCount == null)
            {
                varCount = new int[inst.attribute(attributeIndex).numValues()][inst.numClasses()];
            }

            if(varTotalCount == null)
            {
                varTotalCount = new int[inst.attribute(attributeIndex).numValues()];

            }

            if(!inst.isMissing(attributeIndex))
            {
                varCount[(int)inst.valueSparse(attributeIndex)][(int)inst.classValue()]++;
                varTotalCount[(int)inst.valueSparse(attributeIndex)]++;
                classCount[(int)inst.classValue()]++;
                instCount++;
            }
        }

        public void removeInst(Instance inst)
        {
            if(varCount == null)
            {
                varCount = new int[inst.attribute(attributeIndex).numValues()][inst.numClasses()];
            }

            if(varTotalCount == null)
            {
                varTotalCount = new int[inst.attribute(attributeIndex).numValues()];

            }

            if(!inst.isMissing(attributeIndex))
            {
                varCount[(int)inst.valueSparse(attributeIndex)][(int)inst.classValue()]--;
                varTotalCount[(int)inst.valueSparse(attributeIndex)]--;
                classCount[(int)inst.classValue()]--;
                instCount--;
            }
        }

        public NominalFeatureStats(int attribute)
        {
            super(attribute);
        }

    }
    protected class NumericFeatureStats extends FeatureStats
    {
        PiD piD;
        public void addInst(Instance inst)
        {
            if(!inst.isMissing(attributeIndex))
            {
                piD.insert(inst.valueSparse(attributeIndex),(int)inst.classValue(),1);
                classCount[(int)inst.classValue()]++;
                instCount++;
            }
        }
        public void removeInst(Instance inst)
        {
            if(!inst.isMissing(attributeIndex))
            {
                piD.remove(inst.valueSparse(attributeIndex),(int)inst.classValue(),1);
                classCount[(int)inst.classValue()]--;
                instCount--;
            }
        }
        public NumericFeatureStats(int attribute)
        {
            super(attribute);
            piD = new PiD();
        }
    }

    private boolean debug = false;
    protected HashMap<Integer,FeatureStats> attributeTableMap = new HashMap<>();

    /**
     * adds an instance to the window
     * @param inst instance to be added
     */
    public void addInstance(Instance inst)
    {
        int c = (int)inst.classValue();
        for(int a = 0; a < inst.numAttributes();a++)
        {
            // check not class index
            if(a != inst.classIndex())
            {
                // check missing
                if(!inst.isMissing(a))
                {
                    if (inst.attribute(a).isNominal())
                    {
                        if (attributeTableMap.containsKey(a))
                        {
                            attributeTableMap.get(a).addInst(inst);
                        }
                        else
                        {
                            attributeTableMap.put(a, new NominalFeatureStats(a));
                            attributeTableMap.get(a).classCount = new int[inst.numClasses()];
                        }
                    }
                    else
                    if (inst.attribute(a).isNumeric())
                    {
                        if (attributeTableMap.containsKey(a))
                        {
                            attributeTableMap.get(a).addInst(inst);
                        }
                        else
                        {
                            attributeTableMap.put(a, new NumericFeatureStats(a));
                            attributeTableMap.get(a).classCount = new int[inst.numClasses()];
                        }

                    }
                }
            }
        }

    }

    /**
     * removes an instance from the window
     * @param inst instance to be removed
     */
    public void removeInstance(Instance inst)
    {
        int c = (int)inst.classValue();
        for(int a = 0; a < inst.numAttributes();a++)
        {
            // check not class index
            if(a != inst.classIndex())
            {
                // check missing
                if(!inst.isMissing(a))
                {
                    if (inst.attribute(a).isNominal())
                    {
                        if (attributeTableMap.containsKey(a))
                        {
                            attributeTableMap.get(a).removeInst(inst);
                        }
                        else
                        {
                            attributeTableMap.put(a, new NominalFeatureStats(a));
                            attributeTableMap.get(a).classCount = new int[inst.numClasses()];
                        }
                    }
                    else
                    if (inst.attribute(a).isNumeric())
                    {
                        if (attributeTableMap.containsKey(a))
                        {
                            attributeTableMap.get(a).removeInst(inst);
                        }
                        else
                        {
                            attributeTableMap.put(a, new NumericFeatureStats(a));
                            attributeTableMap.get(a).classCount = new int[inst.numClasses()];
                        }

                    }
                }
            }
        }

    }

    /**
     * Ranks the actual features
     * @param window
     * @param accuracyGain
     * @param previousBestFeatures
     * @return
     */
    public int[] rankFeatures(Instances window, double[] accuracyGain, int[] previousBestFeatures)
    {
        double[] infogainArray = new double[window.numAttributes()];
        for(int a = 0;a < window.numAttributes(); a++)
        {
            if(a != window.classIndex())
                infogainArray[a] = computeInfoGain(a,window);
            else
            {
                infogainArray[a] = Double.NEGATIVE_INFINITY;
                if(debug)
                    System.out.println("class value IG: " + computeInfoGain(a,window));
            }
        }


        // accuracy gain
        if(accuracyGainWeight != 0 && previousBestFeatures!= null)
        {
            // simply add the accuracy gain ( which is between 0-1) multiplied by the weight onto the feature distance.
            for(int g = 0; g < accuracyGain.length;g++)
            {
                if(accuracyGain[g] > 0)
                {
                    infogainArray[previousBestFeatures[g]] += accuracyGainWeight * accuracyGain[g];
                }
            }
        }
        if(debug)
        {
            System.out.println(Arrays.toString(infogainArray));
        }

        int[] rankedFeatures = sortFeatureArray(infogainArray,numberOfFeatures);
        if(debug) {
            System.out.println(Arrays.toString(infogainArray));
            System.out.println(Arrays.toString(rankedFeatures));
        }
        return rankedFeatures;
    }

    /**
     * computes the info gain of an attribute for a window
     * @param a attribute index
     * @return info gain
     */
    protected double computeInfoGain(int a,Instances window)
    {
        double entropyBefore = 0;
        double entropyAfter = 0;

        // calculating entropy after adding attribute
        if(window.attribute(a).isNominal())
        {
            NominalFeatureStats s = (NominalFeatureStats)attributeTableMap.get(a);
            for(int i = 0; i < window.attribute(a).numValues();i++)
            {
                for(int c = 0; c < window.numClasses();c++)
                {
                    if(s.classCount[c] > 0)
                    {
                        if(s.instCount > 0)
                            entropyAfter += ((double)s.varTotalCount[i]/(double)s.instCount) * RankingUtils.computeEntropyNominal(s.varCount[i][c],s.varTotalCount[i]);
                    }
                }
            }

            if(debug)
                System.out.println("nominal entropy after for " + a + ": " + entropyAfter);
        }
        else
        if(window.attribute(a).isNumeric())
        {
            NumericFeatureStats s = (NumericFeatureStats)attributeTableMap.get(a);
            // [bins][classes]
            int[][] counts = s.piD.generateContingencyTable();
            for(int i = 0; i < counts.length;i++)
            {
                for (int c = 0; c < counts[i].length; c++)
                {
                    if (counts[i][c] > 0) {
                        if (s.instCount > 0)
                            entropyAfter += ((double) Utils.sum(counts[i]) / (double) s.instCount) * RankingUtils.computeEntropyNominal(counts[i][c], Utils.sum(counts[i]));
                    }
                }
            }
        }

        // calculating entropy before
        for(int i = 0; i < attributeTableMap.get(a).classCount.length; i++)
        {
            if(attributeTableMap.get(a).classCount[i] > 0)
            {
                double p = (double)attributeTableMap.get(a).classCount[i] / (double)attributeTableMap.get(a).instCount;
                entropyBefore += -p * Utils.log2(p);
            }
        }

        return (entropyBefore - entropyAfter);
    }

}