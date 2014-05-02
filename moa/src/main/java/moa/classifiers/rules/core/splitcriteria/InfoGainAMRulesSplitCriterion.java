/*
 *    InfoGainSplitCriterion.java
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
package moa.classifiers.rules.core.splitcriteria;

import com.github.javacliparser.FloatOption;

import moa.classifiers.core.splitcriteria.InfoGainSplitCriterion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

/**
 * Class for computing splitting criteria using information gain
 * with respect to distributions of class values.
 * The split criterion is used as a parameter on
 * decision trees and decision stumps.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class InfoGainAMRulesSplitCriterion extends InfoGainSplitCriterion implements
        SplitCriterion {

	
	public static double[] computeBranchSplitMerits(double[][] dists) {
        double totalWeight = 0.0;
        double[] distWeights = new double[dists.length];
        for (int i = 0; i < dists.length; i++) {
            distWeights[i] = Utils.sum(dists[i]);
            totalWeight += distWeights[i];
        }
        double[] entropy = new double[dists.length];
        for (int i = 0; i < dists.length; i++) {
        	if (distWeights[i] < 5) { 
        		entropy[i] = Double.MAX_VALUE;
        	} else {
        		entropy[i] = distWeights[i] * computeEntropy(dists[i]) / totalWeight;
        	}
        }
        return entropy;
    }

   
}
