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
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import moa.core.Utils;

/**
 * class for ranking features
 * @author Lanqin Yuan (fyempathy@gmail.com)
 * @version 1
 */
public class Feature
{
    // numeric or nominal
    boolean isNum = true;
    double sum = 0;
    int count = 0;
    // tally for nominal values
    int[] nomTally;
    public Feature()
    {

    }
    public double getAverageValue()
    {
        if(!isNum)
        {
            int maxIndex = -1;
            int maxValue = -1;
            for (int i = 0; i < nomTally.length;i++)
            {
                if(nomTally[i] > maxValue)
                {
                    maxIndex = i;
                    maxValue = nomTally[i];
                }
            }
            return maxIndex;
        } else
        {
            return (sum/count);
        }
    }
}