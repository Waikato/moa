/*
 *    SplitCriterion.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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

import moa.options.OptionHandler;

/**
 * Interface for computing splitting criteria.
 * with respect to distributions of class values.
 * The split criterion is used as a parameter on
 * decision trees and decision stumps.
 * The two split criteria most used are 
 * Information Gain and Gini. 
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $ 
 */
public interface SplitCriterion extends OptionHandler {

    /**
     * Computes the merit of splitting for a given
     * ditribution before the split and after it.
     *
     * @param preSplitDist the class distribution before the split
     * @param postSplitDist the class distribution after the split
     * @return value of the merit of splitting
     */
    public double getMeritOfSplit(double[] preSplitDist,
            double[][] postSplitDists);

    /**
     * Computes the range of splitting merit
     *
     * @param preSplitDist the class distribution before the split
     * @return value of the range of splitting merit
     */
    public double getRangeOfMerit(double[] preSplitDist);
}
