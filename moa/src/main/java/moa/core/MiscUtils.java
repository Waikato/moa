/*
 *    MiscUtils.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
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
package moa.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Random;


/**
 * Class implementing some utility methods.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class MiscUtils {

    public static int chooseRandomIndexBasedOnWeights(double[] weights,
            Random random) {
        double probSum = Utils.sum(weights);
        double val = random.nextDouble() * probSum;
        int index = 0;
        double sum = 0.0;
        while ((sum <= val) && (index < weights.length)) {
            sum += weights[index++];
        }
        return index - 1;
    }

    public static int poisson(double lambda, Random r) {
        if (lambda < 100.0) {
            double product = 1.0;
            double sum = 1.0;
            double threshold = r.nextDouble() * Math.exp(lambda);
            int i = 1;
            int max = Math.max(100, 10 * (int) Math.ceil(lambda));
            while ((i < max) && (sum <= threshold)) {
                product *= (lambda / i);
                sum += product;
                i++;
            }
            return i - 1;
        }
        double x = lambda + Math.sqrt(lambda) * r.nextGaussian();
        if (x < 0.0) {
            return 0;
        }
        return (int) Math.floor(x);
    }

    public static String getStackTraceString(Exception ex) {
        StringWriter stackTraceWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stackTraceWriter));
        return "*** STACK TRACE ***\n" + stackTraceWriter.toString();
    }

    /**
     * Returns index of maximum element in a given array of doubles. First
     * maximum is returned.
     *
     * @param doubles the array of doubles
     * @return the index of the maximum element
     */
    public static /*@pure@*/ int maxIndex(double[] doubles) {

        double maximum = 0;
        int maxIndex = 0;

        for (int i = 0; i < doubles.length; i++) {
            if ((i == 0) || (doubles[i] > maximum)) {
                maxIndex = i;
                maximum = doubles[i];
            }
        }

        return maxIndex;
    }
}
