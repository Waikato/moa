/*
 *    InfoGainSplitCriterionMultilabel.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Jesse Read (jesse@tsc.uc3m.es)
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
package moa.classifiers.core.splitcriteria;

import moa.core.Utils;

/**
 * Class for computing splitting criteria using information gain with respect to
 * distributions of class values for Multilabel data. The split criterion is
 * used as a parameter on decision trees and decision stumps.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public class InfoGainSplitCriterionMultilabel extends InfoGainSplitCriterion {

    private static final long serialVersionUID = 1L;

    public static double computeEntropy(double[] dist) {
        double entropy = 0.0;
        double sum = 0.0;
        for (double d : dist) {
            sum += d;
        }
        if (sum > 0.0) {
            for (double num : dist) {
                double d = num / sum;
                if (d > 0.0) { // TODO: how small can d be before log2 overflows?
                    entropy -= d * Utils.log2(d) + (1 - d) * Utils.log2(1 - d); //Extension to Multilabel
                }
            }
        }
        return sum > 0.0 ? entropy : 0.0;
    }
}
