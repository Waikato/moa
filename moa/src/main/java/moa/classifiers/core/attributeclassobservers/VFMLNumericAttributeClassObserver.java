/*
 *    VFMLNumericAttributeClassObserver.java
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

import moa.core.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;

import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import com.github.javacliparser.IntOption;
import moa.tasks.TaskMonitor;

/**
 * Class for observing the class data distribution for a numeric attribute as in VFML.
 * Used in naive Bayes and decision trees to monitor data statistics on leaves.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class VFMLNumericAttributeClassObserver extends AbstractOptionHandler
        implements NumericAttributeClassObserver {

    private static final long serialVersionUID = 1L;

    @Override
    public void observeAttributeTarget(double attVal, double target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected class Bin implements Serializable {

        private static final long serialVersionUID = 1L;

        public double lowerBound, upperBound;

        public DoubleVector classWeights = new DoubleVector();

        public int boundaryClass;

        public double boundaryWeight;
    }

    protected List<Bin> binList = new ArrayList<Bin>();

    public IntOption numBinsOption = new IntOption("numBins", 'n',
        "The number of bins.", 10, 1, Integer.MAX_VALUE);


    @Override
    public void observeAttributeClass(double attVal, int classVal, double weight) {
        if (Utils.isMissingValue(attVal)) {
        } else {
            if (this.binList.size() < 1) {
                // create the first bin
                Bin newBin = new Bin();
                newBin.classWeights.addToValue(classVal, weight);
                newBin.boundaryClass = classVal;
                newBin.boundaryWeight = weight;
                newBin.upperBound = attVal;
                newBin.lowerBound = attVal;
                this.binList.add(newBin);
            } else {
                // find bin containing new example with binary search
                int index = -1;
                boolean found = false;
                int min = 0;
                int max = this.binList.size() - 1;
                index = 0;
                while ((min <= max) && !found) {
                    int i = (min + max) / 2;
                    Bin bin = this.binList.get(i);
                    if (((attVal >= bin.lowerBound) && (attVal < bin.upperBound))
                            || ((i == this.binList.size() - 1)
                            && (attVal >= bin.lowerBound) && (attVal <= bin.upperBound))) {
                        found = true;
                        index = i;
                    } else if (attVal < bin.lowerBound) {
                        max = i - 1;
                    } else {
                        min = i + 1;
                    }
                }
                boolean first = false;
                boolean last = false;
                if (!found) {
                    // determine if it is before or after the existing range
                    Bin bin = this.binList.get(0);
                    if (bin.lowerBound > attVal) {
                        // go before the first bin
                        index = 0;
                        first = true;
                    } else {
                        // if we haven't found it yet value must be > last bins
                        // upperBound
                        index = this.binList.size() - 1;
                        last = true;
                    }
                }
                Bin bin = this.binList.get(index); // VLIndex(ct->bins, index);
                if ((bin.lowerBound == attVal)
                        || (this.binList.size() >= this.numBinsOption.getValue())) {// Option.getValue())
                    // {//1000)
                    // {
                    // if this is the exact same boundary and class as the bin
                    // boundary or we aren't adding new bins any more then
                    // increment
                    // boundary counts
                    bin.classWeights.addToValue(classVal, weight);
                    if ((bin.boundaryClass == classVal)
                            && (bin.lowerBound == attVal)) {
                        // if it is also the same class then special case it
                        bin.boundaryWeight += weight;
                    }
                } else {
                    // create a new bin
                    Bin newBin = new Bin();
                    newBin.classWeights.addToValue(classVal, weight);
                    newBin.boundaryWeight = weight;
                    newBin.boundaryClass = classVal;
                    newBin.upperBound = bin.upperBound;
                    newBin.lowerBound = attVal;

                    double percent = 0.0;
                    // estimate initial counts with a linear interpolation
                    if (!((bin.upperBound - bin.lowerBound == 0) || last || first)) {
                        percent = 1.0 - ((attVal - bin.lowerBound) / (bin.upperBound - bin.lowerBound));
                    }

                    // take out the boundry points, they stay with the old bin
                    bin.classWeights.addToValue(bin.boundaryClass,
                            -bin.boundaryWeight);
                    DoubleVector weightToShift = new DoubleVector(
                            bin.classWeights);
                    weightToShift.scaleValues(percent);
                    newBin.classWeights.addValues(weightToShift);
                    bin.classWeights.subtractValues(weightToShift);
                    // put the boundry examples back in
                    bin.classWeights.addToValue(bin.boundaryClass,
                            bin.boundaryWeight);

                    // insert the new bin in the right place
                    if (last) {
                        bin.upperBound = attVal;
                        newBin.upperBound = attVal;
                        this.binList.add(newBin);
                    } else if (first) {
                        newBin.upperBound = bin.lowerBound;
                        this.binList.add(0, newBin);
                    } else {
                        newBin.upperBound = bin.upperBound;
                        bin.upperBound = attVal;
                        this.binList.add(index + 1, newBin);
                    }
                }
            }
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
        DoubleVector rightDist = new DoubleVector();
        for (Bin bin : this.binList) {
            rightDist.addValues(bin.classWeights);
        }
        DoubleVector leftDist = new DoubleVector();
        for (Bin bin : this.binList) {
            leftDist.addValues(bin.classWeights);
            rightDist.subtractValues(bin.classWeights);
            double[][] postSplitDists = new double[][]{
                leftDist.getArrayCopy(), rightDist.getArrayCopy()};
            double merit = criterion.getMeritOfSplit(preSplitDist,
                    postSplitDists);
            if ((bestSuggestion == null) || (merit > bestSuggestion.merit)) {
                bestSuggestion = new AttributeSplitSuggestion(
                        new NumericAttributeBinaryTest(attIndex,
                        bin.upperBound, false), postSplitDists, merit);
            }
        }
        return bestSuggestion;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }
}
