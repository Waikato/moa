/*
 *    IademVFMLNumericAttributeClassObserver.java
 *
 *    @author Isvani Frias-Blanco
 * 
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
 *    
 *    
 */
package moa.classifiers.trees.iadem;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.VFMLNumericAttributeClassObserver;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;
import weka.core.Utils;

public class IademVFMLNumericAttributeClassObserver extends VFMLNumericAttributeClassObserver
        implements IademNumericAttributeObserver, AttributeClassObserver {

    private static final long serialVersionUID = 1L;

    public IademVFMLNumericAttributeClassObserver() {
        super();
        this.numBinsOption.setValue(500);
    }

    public IademVFMLNumericAttributeClassObserver(int maxBins) {
        super();
        this.numBinsOption.setValue(maxBins);
    }

    @Override
    public void setMaxBins(int numBins) {
        this.numBinsOption.setValue(numBins);
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
    }

    @Override
    public void computeClassDist(double[][][] cutClassDist) {
        int classes = this.classDist.numValues();
        Bin element = binList.get(0);

        double numLeftInst = element.classWeights.sumOfValues();
        double numRightInst = this.classDist.sumOfValues() - numLeftInst;

        double[] leftClassDist = new double[classes];
        double[] rightClassDist = new double[classes];
        for (int i = 0; i < classes; i++) {
            leftClassDist[i] = element.classWeights.getValue(i);
            rightClassDist[i] = this.classDist.getValue(i) - leftClassDist[i];

            cutClassDist[0][0][i] = leftClassDist[i];
            cutClassDist[0][1][i] = rightClassDist[i];
        }

        for (int i = 1; i < this.binList.size() - 1; i++) {
            element = this.binList.get(i);

            double numChangingInst = element.classWeights.sumOfValues();
            numLeftInst += numChangingInst;
            numRightInst -= numChangingInst;

            for (int j = 0; j < classes; j++) {
                double changingClassDist = element.classWeights.getValue(j);
                leftClassDist[j] += changingClassDist;
                rightClassDist[j] -= changingClassDist;

                cutClassDist[i][0][j] = leftClassDist[j];

                cutClassDist[i][1][j] = rightClassDist[j];
            }
        }
    }

    protected class Bin implements Serializable {

        private static final long serialVersionUID = 1L;
        public double lowerBound, upperBound;
        public DoubleVector classWeights = new DoubleVector();
        public int boundaryClass;
        public double boundaryWeight;
    }
    protected List<Bin> binList = new ArrayList<Bin>();
    protected DoubleVector classDist = new DoubleVector();

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

                    // make this a vector of integers
                    weightToShift.scaleValues(percent);
                    for (int i = 0; i < weightToShift.numValues(); i++) {
                        long tmp = Math.round(weightToShift.getValue(i));
                        weightToShift.setValue(i, tmp);
                    }

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
                        new IademNumericAttributeBinaryTest(attIndex,
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
    public double probabilityOfAttributeValueGivenClass(double attVal,
            int classVal) {
        // compute the conditional probability
        double attValClassValCount = 0.0;
        double totalClassValCount = 0.0;
        for (int i = 0; i < binList.size(); i++) {
            if (attVal == this.binList.get(i).lowerBound
                    && classVal == this.binList.get(i).boundaryClass) {
                // attribute and class values are in a boundary
                attValClassValCount = this.binList.get(i).boundaryWeight;
            } else if (((attVal >= this.binList.get(i).lowerBound) && (attVal < this.binList.get(i).upperBound))
                    || ((i == this.binList.size() - 1)
                    && (attVal >= this.binList.get(i).lowerBound) && (attVal <= this.binList.get(i).upperBound))) {
                // attribute value is inside a bin
                attValClassValCount = this.binList.get(i).classWeights.getValue(classVal);
            }
            totalClassValCount += binList.get(i).classWeights.getValue(classVal);
        }
        return totalClassValCount != 0 ? attValClassValCount / totalClassValCount : 0.0;
    }

    public void forgetAttributeClass(double attVal, int classVal, double weight) {
        if (Utils.isMissingValue(attVal)) {
        } else if (this.classDist.sumOfValues() > 0) {
            this.classDist.addToValue(classVal, -weight);

            int i, index;
            boolean found = false;
            int min, max;
            Bin bin;
            int numBins = binList.size();
            // Find the bin holding the example to be forgotten
            index = 0;
            min = 0;
            max = numBins - 1;

            // Binary search
            while (min <= max && !found) {
                i = (min + max) / 2;
                bin = binList.get(i);
                if ((attVal >= bin.lowerBound && attVal < bin.upperBound)
                        || ((i == numBins - 1) && (attVal >= bin.lowerBound)
                        && (attVal <= bin.upperBound))) {
                    found = true;
                    index = i;
                } else if (attVal < bin.lowerBound) {
                    max = i - 1;
                } else {
                    min = i + 1;
                }
            }

            // Reduce counts
            if (!found) {
                //printf("Value %f not found\n", value );
                //printBins( ct->bins );
                // HERE UNREPORTED ERROR

                return;
            }

            bin = binList.get(index);
            // decrement counts
            bin.classWeights.addToValue(classVal, -weight);
            if (bin.classWeights.getValue(classVal) < 0) {
                bin.classWeights.setValue(classVal, 0);
            }
            if (bin.boundaryClass == classVal) {
                bin.boundaryWeight -= weight;
            }
            if (bin.boundaryWeight < 0) {
                bin.boundaryWeight = 0;
            }

            // Remove bin
            if (bin.boundaryWeight == 0 && binList.size() > 0) {
                int numClasses = bin.classWeights.numValues();
                if (index != 0) {
                    int j;
                    Bin prevBin = binList.get(index - 1);
                    prevBin.upperBound = bin.upperBound;
                    // prevBin->exampleCount += bin->exampleCount;
                    for (j = 0; j < numClasses; j++) {
                        prevBin.classWeights.addToValue(j, bin.classWeights.getValue(j));
                    }
                    binList.remove(index);
                    bin = binList.get(index - 1);
                } // Remove first bin
                else if (binList.size() > 1) {
                    int j;
                    Bin newFirstBin = binList.get(1);
                    Bin oldFirstBin = binList.get(0);
                    /*
                     printf( "removing first bin: %f - %f (%f) bc - %ld\n", oldFirstBin->lowerBound,
                     oldFirstBin->upperBound, oldFirstBin->exampleCount, oldFirstBin->boundryCount);
                     */
                    newFirstBin.lowerBound = oldFirstBin.lowerBound;
                    for (j = 0; j < numClasses; j++) {
                        newFirstBin.classWeights.addToValue(j, oldFirstBin.classWeights.getValue(j));
                    }
                    /*
                     printf( "new first bin: %f - %f (%f) bc - %ld\n", newFirstBin->lowerBound,
                     newFirstBin->upperBound, newFirstBin->exampleCount, newFirstBin->boundryCount);
                     */
                    binList.remove(0);
                }
            }
        }
    }

    @Override
    public void reset() {
        this.classDist = new DoubleVector();
        this.binList = new ArrayList<Bin>();
    }

    @Override
    public long getValueCount() {
        return (long) this.classDist.sumOfValues();
    }

    @Override
    public long[] getClassDist() {
        long[] classDistCopy = new long[this.classDist.numValues()];
        for (int i = 0; i < this.classDist.numValues(); i++) {
            classDistCopy[i] = (long) this.classDist.getValue(i);
        }
        return classDistCopy;
    }

    @Override
    public long getNumberOfCutPoints() {
        return binList.size() - 1;
    }

    @Override
    public long[] getLeftClassDist(double corte) {
        long[] rightDist = new long[this.classDist.numValues()];
        Arrays.fill(rightDist, 0);
        for (int i = 0; i < this.binList.size() && corte > this.binList.get(i).upperBound; i++) {
            for (int j = 0; j < this.binList.get(i).classWeights.numValues(); j++) {
                rightDist[j] += this.binList.get(i).classWeights.getValue(j);
            }
        }
        return rightDist;
    }

    @Override
    public double getCut(int indice) {
        return binList.get(indice).upperBound;
    }

    @Override
    public void computeClassDistProbabilities(double[][][] cut_value_classDist_lower,
            double[][][] cut_value_classDist_upper,
            double[][] counts_cut_value,
            boolean withIntervalEstimates) {
        int classes = this.classDist.numValues();
        Bin element = binList.get(0);

        double numLeftInst = element.classWeights.sumOfValues();
        double numRightInst = this.classDist.sumOfValues() - numLeftInst;
        counts_cut_value[0][0] = numLeftInst;
        counts_cut_value[0][1] = numRightInst;

        double[] leftClassDist = new double[classes];
        double[] rightClassDist = new double[classes];
        for (int i = 0; i < classes; i++) {
            leftClassDist[i] = element.classWeights.getValue(i);
            rightClassDist[i] = this.classDist.getValue(i) - leftClassDist[i];

            double lEstimates = 0.0;
            if (numLeftInst != 0) {
                lEstimates = leftClassDist[i] / numLeftInst;
            }
            double lError = 0.0;
            if (withIntervalEstimates) {
                lError = IademCommonProcedures.getIADEM_HoeffdingBound(lEstimates, numLeftInst);
            }
            cut_value_classDist_lower[0][0][i] = Math.max(0.0, lEstimates - lError);
            cut_value_classDist_upper[0][0][i] = Math.min(1.0, lEstimates + lError);

            double rEstimates = 0.0;
            if (numRightInst != 0.0) {
                rEstimates = rightClassDist[i] / numRightInst;
            }
            double rError = 0.0;
            if (withIntervalEstimates) {
                rError = IademCommonProcedures.getIADEM_HoeffdingBound(rEstimates, numRightInst);
            }
            cut_value_classDist_lower[0][1][i] = Math.max(0.0, rEstimates - rError);
            cut_value_classDist_upper[0][1][i] = Math.min(1.0, rEstimates + rError);
        }

        for (int i = 1; i < this.binList.size() - 1; i++) {
            element = this.binList.get(i);

            double numChangingInst = element.classWeights.sumOfValues();
            numLeftInst += numChangingInst;
            numRightInst -= numChangingInst;
            counts_cut_value[i][0] = numLeftInst; // corte i a la izquierda
            counts_cut_value[i][1] = numRightInst; // corte i a la derecha

            for (int j = 0; j < classes; j++) {
                double changingClassDist = element.classWeights.getValue(j);
                leftClassDist[j] += changingClassDist;
                rightClassDist[j] -= changingClassDist;
                double lEstimates = 0.0;
                if (numLeftInst != 0) {
                    lEstimates = leftClassDist[j] / numLeftInst;
                }
                double lError = 0.0;
                if (withIntervalEstimates) {
                    lError = IademCommonProcedures.getIADEM_HoeffdingBound(lEstimates, numLeftInst);
                }
                cut_value_classDist_lower[i][0][j] = Math.max(0.0, lEstimates - lError);
                cut_value_classDist_upper[i][0][j] = Math.min(1.0, lEstimates + lError);
                double rEstimates = 0.0;
                if (numRightInst != 0.0) {
                    rEstimates = rightClassDist[j] / numRightInst;
                }
                double rError = 0.0;
                if (withIntervalEstimates) {
                    rError = IademCommonProcedures.getIADEM_HoeffdingBound(rEstimates, numRightInst);
                }
                cut_value_classDist_lower[i][1][j] = Math.max(0.0, rEstimates - rError);
                cut_value_classDist_upper[i][1][j] = Math.min(1.0, rEstimates + rError);
            }
        }
    }

    @Override
    public ArrayList<Double> cutPointSuggestion(int numMaxIntervalos) {
        ArrayList<Double> cutPoint = new ArrayList<Double>();
        for (int i = 0; i < this.binList.size() - 1; i++) {
            cutPoint.add(this.binList.get(i).upperBound);
        }
        return cutPoint;
    }

    @Override
    public ArrayList<Double[]> computeConditionalProbPerBin(ArrayList<Double> cortes) {
        ArrayList<Double[]> condProb = new ArrayList<Double[]>();
        int numClasses = classDist.numValues();

        double[] numClassesPerBin = new double[numClasses];
        Arrays.fill(numClassesPerBin, 0);

        int currentBin = 0;
        Bin element;
        for (int j = 0; j < this.binList.size(); j++) {
            element = this.binList.get(j);
            double aux = element.upperBound;
            if ((currentBin == cortes.size()) // last bin
                    || (aux <= cortes.get(currentBin))) {// keeps in the same bin
                for (int i = 0; i < numClasses; i++) {
                    numClassesPerBin[i] += element.classWeights.getValue(i);
                }
            } else {// change of bin
                // store the previous data
                Double[] probCond_intervalo = new Double[numClasses];
                for (int i = 0; i < numClasses; i++) {
                    if (this.classDist.getValue(i) == 0) {
                        probCond_intervalo[i] = 0.0;
                    } else {
                        probCond_intervalo[i] = numClassesPerBin[i]
                                / this.classDist.getValue(i);
                    }
                }
                condProb.add(probCond_intervalo);

                currentBin++;

                while ((currentBin < cortes.size())
                        && (aux > cortes.get(currentBin))) {
                    currentBin++;
                    condProb.add(null);
                }

                Arrays.fill(numClassesPerBin, 0);
                // store data in the last bin
                for (int i = 0; i < numClasses; i++) {
                    numClassesPerBin[i] += element.classWeights.getValue(i);
                }
            }
        }
        // store data in the last bin
        Double[] probCond_intervalo = new Double[numClasses];
        for (int i = 0; i < numClasses; i++) {
            if (this.classDist.getValue(i) == 0) {
                probCond_intervalo[i] = 0.0;
            } else {
                probCond_intervalo[i] = numClassesPerBin[i]
                        / this.classDist.getValue(i);
            }
        }
        condProb.add(probCond_intervalo);

        return condProb;
    }

    @Override
    public double[] computeConditionalProb(ArrayList<Double> cuts, double cutValue) {
        int numClasses = this.classDist.numValues();
        double[] instPerClass = new double[numClasses];
        Arrays.fill(instPerClass, 0);

        int keyPos;
        if (cutValue <= cuts.get(0)) {
            keyPos = 0;
            while (keyPos < this.binList.size()
                    && this.binList.get(keyPos).upperBound <= cuts.get(0)) {
                for (int i = 0; i < numClasses; i++) {
                    instPerClass[i] += this.binList.get(keyPos).classWeights.getValue(i);
                }
                keyPos++;
            }
        } else if (cutValue > cuts.get(cuts.size() - 1)) {
            keyPos = this.binList.size() - 1;
            while (keyPos < this.binList.size()
                    && this.binList.get(keyPos).upperBound > cuts.get(cuts.size() - 1)) {
                for (int i = 0; i < numClasses; i++) {
                    instPerClass[i] += this.binList.get(keyPos).classWeights.getValue(i);
                }
                keyPos--;
            }
        } else {
            int currentBin = 1;
            double lastBin = cuts.get(currentBin);
            while (cutValue > lastBin) {
                currentBin++;
                lastBin = cuts.get(currentBin);
            }

            double firstBin = cuts.get(currentBin - 1);
            keyPos = 0;
            while (keyPos < this.binList.size()
                    && this.binList.get(keyPos).upperBound <= firstBin) {
                keyPos++;
            }
            while (keyPos < this.binList.size()
                    && this.binList.get(keyPos).upperBound <= lastBin) {
                for (int i = 0; i < numClasses; i++) {
                    instPerClass[i] += this.binList.get(keyPos).classWeights.getValue(i);
                }
                keyPos++;
            }
        }
        // if there is no information in this bin...
        boolean allZero = true;
        int pos = 0;
        while ((allZero) && (pos < numClasses)) {
            if (instPerClass[pos] > 0.0) {
                allZero = false;
            }
            pos++;
        }

        double[] classVotes = new double[numClasses];
        if (allZero) {
            for (int i = 0; i < numClasses; i++) {
                classVotes[i] = 1.0 / (double) numClasses;
            }
        } else {
            for (int i = 0; i < numClasses; i++) {
                if (classDist.getValue(i) == 0) {
                    classVotes[i] = 0.0;
                } else {
                    classVotes[i] = instPerClass[i]
                            / classDist.getValue(i);
                }
            }
        }
        return classVotes;
    }

    @Override
    public void addValue(double attValue, int classValue, double weight) {
        if (Utils.isMissingValue(attValue)) {
        } else {
            this.classDist.addToValue(classValue, weight);
            observeAttributeClass(attValue, classValue, weight);
        }
    }

    @Override
    public long getMaxOfValues() {
        return this.numBinsOption.getValue();
    }

    @Override
    public IademNumericAttributeObserver getCopy() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
