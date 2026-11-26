/*
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
 */

package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

/**
 * Extension of MOA's {@link EuclideanDistance} that optionally includes the
 * class attribute in the distance calculation (used for joint feature+class
 * concept topology comparison).
 */
public class EuclideanDistanceModified extends EuclideanDistance {

    private static final long serialVersionUID = 1068606253458807903L;

    public EuclideanDistanceModified() {
        super();
    }

    public EuclideanDistanceModified(Instances data) {
        super(data);
    }

    public double distanceUsingClass(Instance first, Instance second) {
        return Math.sqrt(distanceWithClass(first, second, Double.POSITIVE_INFINITY, true));
    }

    public double distanceWithClass(Instance first, Instance second, double cutOffValue, boolean useClass) {
        double distance = 0;
        int firstI, secondI;
        int firstNumValues = first.numValues();
        int secondNumValues = second.numValues();
        int numAttributes = m_Data.numAttributes();
        int classIndex = m_Data.classIndex();

        validate();

        for (int p1 = 0, p2 = 0; p1 < firstNumValues || p2 < secondNumValues; ) {
            firstI = (p1 >= firstNumValues) ? numAttributes : first.index(p1);
            secondI = (p2 >= secondNumValues) ? numAttributes : second.index(p2);

            if (!useClass && firstI == classIndex) {
                p1++;
                continue;
            }
            if ((firstI < numAttributes) && !m_ActiveIndices[firstI]) {
                p1++;
                continue;
            }
            if (!useClass && secondI == classIndex) {
                p2++;
                continue;
            }
            if ((secondI < numAttributes) && !m_ActiveIndices[secondI]) {
                p2++;
                continue;
            }

            double diff;
            if (firstI == secondI) {
                diff = difference(firstI, first.valueSparse(p1), second.valueSparse(p2));
                p1++;
                p2++;
            } else if (firstI > secondI) {
                diff = difference(secondI, 0, second.valueSparse(p2));
                p2++;
            } else {
                diff = difference(firstI, first.valueSparse(p1), 0);
                p1++;
            }

            distance = updateDistance(distance, diff);
            if (distance > cutOffValue) {
                return Double.POSITIVE_INFINITY;
            }
        }
        return distance;
    }
}
