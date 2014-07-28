/*
 *    AttributeClassObserver.java
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
package moa.classifiers.rules.multilabel.attributeclassobservers;

import moa.classifiers.rules.multilabel.core.AttributeExpansionSuggestion;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.core.DoubleVector;
import moa.options.OptionHandler;

/**
 * Interface for observing the statistics for an attribute.
 * This observer monitors the statistics for a given attribute.
 * Used in AMRulesMultiLabelLearners
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 * @version $Revision: 1 $ 
 */
public interface AttributeStatisticsObserver extends OptionHandler {

    /**
     * Updates statistics of this observer given an attribute value, the index of the statistic
     * and the weight of the instance observed
     *
     * @param inputAttributeValue the value for the attribute attribute
     * @param statistics numOutputs x numStatistics the index of the statistic to store
     */
    public void observeAttribute(double inputAttributeValue, DoubleVector [] statistics);



    /**
     * Gets the best split suggestion given a criterion and a class distribution
     *
     * @param criterion the split criterion to use
     * @param preSplitDist the class distribution before the split
     * @param attIndex the input attribute index
     * @return suggestion of best attribute split
     */
    public AttributeExpansionSuggestion getBestEvaluatedSplitSuggestion(
            MultiLabelSplitCriterion criterion, DoubleVector [] preSplitStatistics, int inputAttributeIndex);
    
}
