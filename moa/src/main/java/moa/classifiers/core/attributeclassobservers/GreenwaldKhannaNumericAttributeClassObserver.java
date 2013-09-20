/*
 *    GreenwaldKhannaNumericAttributeClassObserver.java
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
package moa.classifiers.core.attributeclassobservers;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.Utils;

import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.GreenwaldKhannaQuantileSummary;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import com.github.javacliparser.IntOption;
import moa.tasks.TaskMonitor;

/**
 * Class for observing the class data distribution for a numeric attribute using Greenwald and Khanna methodology.
 * This observer monitors the class distribution of a given attribute.
 * Used in naive Bayes and decision trees to monitor data statistics on leaves.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class GreenwaldKhannaNumericAttributeClassObserver extends AbstractOptionHandler implements NumericAttributeClassObserver {

    private static final long serialVersionUID = 1L;

    protected AutoExpandVector<GreenwaldKhannaQuantileSummary> attValDistPerClass = new AutoExpandVector<GreenwaldKhannaQuantileSummary>();

    public IntOption numTuplesOption = new IntOption("numTuples", 'n',
        "The number of tuples.", 10, 1, Integer.MAX_VALUE);

    @Override
    public void observeAttributeClass(double attVal, int classVal, double weight) {
        if (Utils.isMissingValue(attVal)) {
        } else {
            GreenwaldKhannaQuantileSummary valDist = this.attValDistPerClass.get(classVal);
            if (valDist == null) {
                valDist = new GreenwaldKhannaQuantileSummary(this.numTuplesOption.getValue());
                this.attValDistPerClass.set(classVal, valDist);
            }
            // TODO: not taking weight into account
            valDist.insert(attVal);
        }
    }

    @Override
    public double probabilityOfAttributeValueGivenClass(double attVal,
            int classVal) {
        // TODO: NaiveBayes broken until implemented
        return 0.0;
    }

    @Override
    public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(
            SplitCriterion criterion, double[] preSplitDist, int attIndex,
            boolean binaryOnly) {
        AttributeSplitSuggestion bestSuggestion = null;
        for (GreenwaldKhannaQuantileSummary qs : this.attValDistPerClass) {
            if (qs != null) {
                double[] cutpoints = qs.getSuggestedCutpoints();
                for (double cutpoint : cutpoints) {
                    double[][] postSplitDists = getClassDistsResultingFromBinarySplit(cutpoint);
                    double merit = criterion.getMeritOfSplit(preSplitDist,
                            postSplitDists);
                    if ((bestSuggestion == null)
                            || (merit > bestSuggestion.merit)) {
                        bestSuggestion = new AttributeSplitSuggestion(
                                new NumericAttributeBinaryTest(attIndex,
                                cutpoint, true), postSplitDists, merit);
                    }
                }
            }
        }
        return bestSuggestion;
    }

    // assume all values equal to splitValue go to lhs
    public double[][] getClassDistsResultingFromBinarySplit(double splitValue) {
        DoubleVector lhsDist = new DoubleVector();
        DoubleVector rhsDist = new DoubleVector();
        for (int i = 0; i < this.attValDistPerClass.size(); i++) {
            GreenwaldKhannaQuantileSummary estimator = this.attValDistPerClass.get(i);
            if (estimator != null) {
                long countBelow = estimator.getCountBelow(splitValue);
                lhsDist.addToValue(i, countBelow);
                rhsDist.addToValue(i, estimator.getTotalCount() - countBelow);
            }
        }
        return new double[][]{lhsDist.getArrayRef(), rhsDist.getArrayRef()};
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }

    @Override
    public void observeAttributeTarget(double attVal, double target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
