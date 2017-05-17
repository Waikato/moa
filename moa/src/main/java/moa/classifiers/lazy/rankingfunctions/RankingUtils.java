/*
 *    RankingUtils.java
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
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import moa.core.Utils;

/**
 * utilities for ranking functions
 * @author Lanqin Yuan (fyempathy@gmail.com)
 * @version 1
 */
public class RankingUtils
{


    /**
     * calculates the probability for a given z score on a standard normal distribution
     * @param z
     * @return
     */
    public static double calculateProb(double z)
    {
        if ( z < -6.5)
            return 0.0;
        if( z > 6.5)
            return 1.0;

        double factK = 1;
        double sum = 0;
        double term = 1;
        double k = 0;
        double loopStop = Math.exp(-23);
        while(Math.abs(term) > loopStop)
        {
            term = 0.3989422804 * Math.pow(-1,k) * Math.pow(z,k) / (2 * k + 1) / Math.pow(2,k) * Math.pow(z,k+1) / factK;
            sum += term;
            k++;
            factK *= k;
        }
        sum += 0.5;

        return sum;
    }

    /**
     * computes entropy for a nominal attribute, does not get weight
     * @param count count of a value's occurrence of nominal attribute
     * @param totalCount total of all occurrences of nominal attribute
     * @return
     */
    public static double computeEntropyNominal (int count, int totalCount)
    {
        if(totalCount <= 0)
            return 0;

        double p =  ((double)count / (double)totalCount);
        double entropy = 0;
        double log =  Utils.log2(p);

        if(Double.isNaN(log))
            log = 0;
        if (p != 0)
            entropy =  -p * log;

        return entropy;
    }

    /**
     * computes entropy for a numeric attribute using the normal distribution
     * @param value
     * @param mean
     * @param SD
     * @return
     */
    public static double computeEntropyNumeric(double value,double mean, double SD)
    {
        double entropy = 0;
        double z = calculateZScore(mean,SD,value);
        double p = calculateProb(z + 0.1) - calculateProb(z - 0.1);
        double log =  Utils.log2(p);
        if(Double.isNaN(log))
            log = 0;

        if (p != 0)
            entropy =  -p * log;

        if(Double.isNaN(entropy))
        {
            System.out.println("entropy is NaN for some reason. value: " + value + " mean " + mean+ " sd " + SD  + " z score " + z + " p " + p +" log p " + Utils.log2(p));
            System.console().readLine();
        }
        return entropy;

    }

    /**
     * calculates the z score
     * @param mean
     * @param sd
     * @param value
     * @return
     */
    public static double calculateZScore(double mean,double sd, double value)
    {
        return (value - mean)/sd;
    }

    /**
     * calculates the Standard deviation for a given attribute
     * @param mean
     * @param n
     * @param attributeIndex
     * @param window
     * @return
     */
    public static double calculateSD(double mean,int n, int attributeIndex, Instances window)
    {
        double sd = 0;

        for (int i = 0; i < window.numInstances();i++)
        {
            if(!window.instance(i).isMissing(attributeIndex))
            {
                sd += (window.instance(i).valueSparse(attributeIndex) - mean) * (window.instance(i).valueSparse(attributeIndex) - mean);
            }
        }
        sd = Math.sqrt(sd/n);
        return sd;
    }
}