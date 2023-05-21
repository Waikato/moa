/*
 *    FastNominalAttributeClassObserver.java
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
import moa.classifiers.core.conditionaltests.NominalAttributeBinaryTest;
import moa.classifiers.core.conditionaltests.NominalAttributeMultiwayTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import java.util.HashMap;
import java.util.Map;


/**
 * Class for observing the class data distribution for a nominal attribute.
 * This observer monitors the class distribution of a given attribute.
 * Used in naive Bayes and decision trees to monitor data statistics on leaves.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Eugene Kamenev (eugene.kamenev@gmail.com)
 * @version $Revision: 7 $
 */
public class FastNominalAttributeClassObserver extends AbstractOptionHandler implements DiscreteAttributeClassObserver {

    private static final long serialVersionUID = 1L;

    protected double totalWeightObserved = 0.0;

    protected double missingWeightObserved = 0.0;

    protected Map<Integer, Map<Integer, Double>> attValDistPerClassCount = new HashMap<>();

    protected Map<Integer, Double> classTotalCount = new HashMap<>();

    protected Map<Integer, Integer> maxAttrValue = new HashMap<>();

    @Override
    public void observeAttributeClass(double attVal, int classVal, double weight) {
        if (Utils.isMissingValue(attVal)) {
            this.missingWeightObserved += weight;
        } else {
            int attValInt = (int) attVal;
            Map<Integer, Double> valDistCount = this.attValDistPerClassCount.computeIfAbsent(classVal, k -> new HashMap<>());
            // update distribution count
            valDistCount.put(attValInt, valDistCount.getOrDefault(attValInt, 0.0) + weight);
            Integer maxValue = this.maxAttrValue.get(classVal);
            if (maxValue == null || attVal > maxValue) {
                // update max attribute value
                this.maxAttrValue.put(classVal, attValInt);
            }
            // update the total count for the class
            this.classTotalCount.put(classVal, this.classTotalCount.getOrDefault(classVal, 0.0) + weight);
        }
        this.totalWeightObserved += weight;
    }

    @Override
    public double probabilityOfAttributeValueGivenClass(double attVal, int classVal) {
        Map<Integer, Double> obs = this.attValDistPerClassCount.get(classVal);
        Double sumCounts = this.classTotalCount.getOrDefault(classVal, 0.0);
        Integer max = this.maxAttrValue.getOrDefault(classVal, (int) attVal) + 1;
        return obs != null ? (obs.getOrDefault((int) attVal, 0.0) + 1.0) / (sumCounts + max) : 0.0;
    }

    public double[][] getClassDistsResultingFromMultiwaySplit(int maxAttValsObserved) {
        DoubleVector[] resultingDists = new DoubleVector[maxAttValsObserved];
        for (int i = 0; i < resultingDists.length; i++) {
            resultingDists[i] = new DoubleVector();
        }
        for (Map.Entry<Integer, Map<Integer, Double>> entry : this.attValDistPerClassCount.entrySet()) {
            int classVal = entry.getKey();
            Map<Integer, Double> attValDistCount = entry.getValue();
            for (int j = 0; j < maxAttValsObserved; j++) {
                resultingDists[j].addToValue(classVal, attValDistCount.getOrDefault(j, 0.0));
            }
        }
        double[][] distributions = new double[maxAttValsObserved][];
        for (int i = 0; i < distributions.length; i++) {
            distributions[i] = resultingDists[i].getArrayRef();
        }
        return distributions;
    }

    public double[][] getClassDistsResultingFromBinarySplit(int valIndex) {
        DoubleVector equalsDist = new DoubleVector();
        DoubleVector notEqualDist = new DoubleVector();
        for (Map.Entry<Integer, Map<Integer, Double>> entry : this.attValDistPerClassCount.entrySet()) {
            int classVal = entry.getKey();
            Map<Integer, Double> attValDistCount = entry.getValue();
            double count = attValDistCount.getOrDefault(valIndex, 0.0);
            equalsDist.addToValue(classVal, count);
            notEqualDist.addToValue(classVal, this.classTotalCount.get(classVal) - count);
        }
        return new double[][]{equalsDist.getArrayRef(),
                notEqualDist.getArrayRef()};
    }

    @Override
    public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(SplitCriterion criterion, double[] preSplitDist,
                                                                    int attIndex, boolean binaryOnly) {
        AttributeSplitSuggestion bestSuggestion = null;
        int maxAttValsObserved = 0;
        for (Integer max : this.maxAttrValue.values()) {
            if (max > maxAttValsObserved) {
                maxAttValsObserved = max + 1;
            }
        }
        if (!binaryOnly) {
            double[][] postSplitDists = getClassDistsResultingFromMultiwaySplit(maxAttValsObserved);
            double merit = criterion.getMeritOfSplit(preSplitDist,
                    postSplitDists);
            bestSuggestion = new AttributeSplitSuggestion(
                    new NominalAttributeMultiwayTest(attIndex), postSplitDists,
                    merit);
        }
        for (int valIndex = 0; valIndex < maxAttValsObserved; valIndex++) {
            double[][] postSplitDists = getClassDistsResultingFromBinarySplit(valIndex);
            double merit = criterion.getMeritOfSplit(preSplitDist,
                    postSplitDists);
            if ((bestSuggestion == null) || (merit > bestSuggestion.merit)) {
                bestSuggestion = new AttributeSplitSuggestion(
                        new NominalAttributeBinaryTest(attIndex, valIndex),
                        postSplitDists, merit);
            }
        }
        return bestSuggestion;
    }

    @Override
    public void observeAttributeTarget(double v, double v1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    public double totalWeightOfClassObservations() {
        return this.totalWeightObserved;
    }

    public double weightOfObservedMissingValues() {
        return this.missingWeightObserved;
    }
}
