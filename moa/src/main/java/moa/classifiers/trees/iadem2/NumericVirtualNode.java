/*
 *    NumericVirtualNode.java
 *
 *    @author José del Campo-Ávila
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

package moa.classifiers.trees.iadem2;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.trees.iademutils.IademAttributeSplitSuggestion;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import moa.classifiers.trees.iademutils.IademException;
import moa.classifiers.trees.iademutils.IademSplitMeasure;
import moa.classifiers.trees.iademutils.IademVFMLNumericAttributeClassObserver;
import moa.classifiers.trees.iademutils.IademCommonProcedures;
import moa.classifiers.trees.iademutils.IademNumericAttributeBinaryTest;
import moa.core.DoubleVector;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;

public class NumericVirtualNode extends VirtualNode { 
    private static final long serialVersionUID = 1L; 
    private static final int MAX_BINS_EQUAL_WIDTH = 10;
    protected IademNumericAttributeObserver numericAttClassObserver;
    protected double bestCutPoint;
    
    public NumericVirtualNode(IADEM2cTree tree,
            Node parent,
            int attIndex,
            IademNumericAttributeObserver numericAttClassObs) {
        super(tree, parent, attIndex);
        int numIntervalos = this.tree.getMaxNumberOfBins();
        this.numericAttClassObserver = (IademNumericAttributeObserver) numericAttClassObs.copy();
        this.numericAttClassObserver.setMaxBins(numIntervalos);
        this.bestCutPoint = 0.0;
    }

    public IademNumericAttributeObserver getNumericAttClassObserver() {
        return numericAttClassObserver;
    }
    
    @Override
    public Node learnFromInstance(Instance instance) {
        this.numericAttClassObserver.addValue(instance.value(this.attIndex),
                (int) instance.value(instance.classIndex()),
                instance.weight());
        this.classValueDist.addToValue((int) instance.value(instance.classIndex()), instance.weight());
        this.heuristicMeasureUpdated = false;
        return this;
    }

    long arrSum(long[] arr) {
        long count = 0;
        for (int i = 0; i < arr.length; i++) {
            count += arr[i];
        }
        return count;
    }
    
    @Override
    public SplitNode getNewSplitNode(long newTotal,
            Node parent,
            IademAttributeSplitSuggestion bestSuggestion) {
        double[] cut = new double[]{this.bestCutPoint};

        Node[] children = new Node[2]; // a traditional binary split test for numeric attributes

        long[] newClassVotesAndTotal = this.numericAttClassObserver.getLeftClassDist(this.bestCutPoint);
        long totalLeft = arrSum(newClassVotesAndTotal); 
        
        long total = this.numericAttClassObserver.getValueCount();
        long[] classVotesTotal = this.numericAttClassObserver.getClassDist();
        boolean equalsPassesTest = true;
        if (this.numericAttClassObserver instanceof IademVFMLNumericAttributeClassObserver) {
            equalsPassesTest = false;
        }
        SplitNode splitNode = new SplitNode(this.tree,
                parent,
                null,
                ((LeafNode) this.parent).getMajorityClassVotes(),
                new IademNumericAttributeBinaryTest(this.attIndex,
                cut[0],
                equalsPassesTest));
        
        long newTotalLeft = totalLeft;
        long newTotalRight = total - newTotalLeft;
        double[] newClassVotesLeft = new double[this.tree.problemDescription.attribute(this.tree.problemDescription.classIndex()).numValues()];
        double[] newClassVotesRight = new double[this.tree.problemDescription.attribute(this.tree.problemDescription.classIndex()).numValues()];

        Arrays.fill(newClassVotesLeft, 0);
        Arrays.fill(newClassVotesRight, 0);

        for (int i = 0; i < newClassVotesAndTotal.length; i++) {
            newClassVotesLeft[i] = newClassVotesAndTotal[i];
            newClassVotesRight[i] = classVotesTotal[i] - newClassVotesLeft[i];
        }

        splitNode.setChildren((Node[]) null);
        children[0] = this.tree.newLeafNode(splitNode, newTotal, newTotalLeft, newClassVotesLeft);
        children[1] = this.tree.newLeafNode(splitNode, newTotal, newTotalRight, newClassVotesRight);


        splitNode.setChildren(children);

        return splitNode;
    }
    
    @Override
    public void updateHeuristicMeasure() throws IademException {
        if (!this.heuristicMeasureUpdated) {
            if (this.numericAttClassObserver.getNumberOfCutPoints() < 2) {
                this.bestSplitSuggestion = null;
            } else {
                IademSplitMeasure measure = ((LeafNode) this.parent).getTree().getMeasure();
                int numberOfClasses = this.tree.problemDescription.attribute(this.tree.problemDescription.classIndex()).numValues();
                
                int numberOfSplits = 2;
                int numberOfCuts = (int) this.numericAttClassObserver.getNumberOfCutPoints();

                double[] measureLower = new double[numberOfCuts];
                double[] measureUpper = new double[numberOfCuts];

                double[][][] classVotesPerCutAndSplit_lower = new double[numberOfCuts][numberOfSplits][numberOfClasses];
                double[][][] classVotesPerCutAndSplit_upper = new double[numberOfCuts][numberOfSplits][numberOfClasses];

                double[][] totalPerCutAndSplit = new double[numberOfCuts][numberOfSplits];

                computeClassVoteBounds(classVotesPerCutAndSplit_lower, classVotesPerCutAndSplit_upper,
                        totalPerCutAndSplit, true);

                for (int k = 0; k < numberOfCuts; k++) {
                    double[] countPerSplit_lower = new double[numberOfSplits];
                    double[] availableErrorPerSplit = new double[numberOfSplits];
                    Arrays.fill(countPerSplit_lower, 0.0);
                    for (int i = 0; i < numberOfSplits; i++) {
                        for (int j = 0; j < numberOfClasses; j++) {
                            countPerSplit_lower[i] += classVotesPerCutAndSplit_lower[k][i][j];
                        }
                        availableErrorPerSplit[i] = 1.0 - countPerSplit_lower[i];

                        if (availableErrorPerSplit[i] < 0.0) {
                            if (Math.abs(availableErrorPerSplit[i]) < IADEM2cTree.ERROR_MARGIN) {
                                availableErrorPerSplit[i] = 0.0;
                            } else {
                                throw new IademException("NumericVirtualNode", "updateHeuristicMeasure",
                                        "Problems when calculating measures");
                            }
                        }
                    }

                    // compute measure upper bounds
                    double[] measurePerSplit_upper = new double[numberOfSplits];
                    
                    double[] splitLevels = new double[numberOfSplits];
                    for (int i = 0; i < numberOfSplits; i++) { 
                        ArrayList<Double> lot = new ArrayList<Double>();
                        lot.add(0.0); 
                        lot.add(1.0); 
                        ArrayList<Integer> hole = new ArrayList<Integer>();
                        hole.add(0); 
                        for (int j = 0; j < numberOfClasses; j++) { 
                            IademCommonProcedures.insertLotsHoles(lot, hole,
                                    classVotesPerCutAndSplit_lower[k][i][j],
                                    classVotesPerCutAndSplit_upper[k][i][j]);
                        } 
                        splitLevels[i] = IademCommonProcedures.computeLevel(lot, hole,
                                availableErrorPerSplit[i]);
                    }
                    
                    for (int i = 0; i < numberOfSplits; i++) {
                        ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                        for (int j = 0; j < numberOfClasses; j++) {
                            double tmpMeasureUpper = Math.max(splitLevels[i],
                                    classVotesPerCutAndSplit_lower[k][i][j]);
                            tmpMeasureUpper = Math.min(classVotesPerCutAndSplit_upper[k][i][j],
                                    tmpMeasureUpper);
                            vectorToMeasure.add(tmpMeasureUpper);
                        }
                        measurePerSplit_upper[i] = measure.doMeasure(vectorToMeasure, null, null);
                    }
                    
                    double[] measurePerSplit_lower = new double[numberOfSplits];
                    
                    for (int i = 0; i < numberOfSplits; i++) {
                        double availableProb = availableErrorPerSplit[i];
                        
                        ArrayList<Integer> decOrderClassVotes_upper = new ArrayList<Integer>();

                        ArrayList<Integer> unusedClasses = new ArrayList<Integer>();
                        for (int j = 0; j < numberOfClasses; j++) {
                            unusedClasses.add(j);
                        }

                        double auxAvailable = availableErrorPerSplit[i];
                        for (int j = 0; j < numberOfClasses; j++) {
                            if (auxAvailable < 0.0) {
                                if (Math.abs(auxAvailable) < IADEM2cTree.ERROR_MARGIN) {
                                    auxAvailable = 0.0;
                                } else {
                                    throw new IademException("NodoVirtualContinuo", "actualizaMedida",
                                            "Problems calculating measure");
                                }
                            }
                            
                            int classIndex = getClassValueProbabilities(i, unusedClasses,
                                    classVotesPerCutAndSplit_lower[k],
                                    classVotesPerCutAndSplit_upper[k], auxAvailable);
                            double probUpper = Math.min(classVotesPerCutAndSplit_upper[k][i][classIndex],
                                    classVotesPerCutAndSplit_lower[k][i][classIndex]
                                    + auxAvailable);

                            auxAvailable -= (probUpper - classVotesPerCutAndSplit_lower[k][i][classIndex]);

                            unusedClasses.remove(new Integer(classIndex));
                            decOrderClassVotes_upper.add(new Integer(classIndex));
                        }
                        
                        ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                        for (int j = 0; j < decOrderClassVotes_upper.size(); j++) {
                            int classIndex = decOrderClassVotes_upper.get(j);

                            if (availableProb < 0.0) {
                                if (Math.abs(availableProb) < IADEM2cTree.ERROR_MARGIN) {
                                    availableProb = 0.0;
                                } else {
                                    throw new IademException("NumericVirtualNode", "updateMeasure",
                                            "Problems when calculating measures");
                                }
                            }
                            double probUpper = Math.min(classVotesPerCutAndSplit_upper[k][i][classIndex],
                                    classVotesPerCutAndSplit_lower[k][i][classIndex]
                                    + availableProb);
                            availableProb -= (probUpper - classVotesPerCutAndSplit_lower[k][i][classIndex]);
                            vectorToMeasure.add(probUpper);
                        }

                        measurePerSplit_lower[i] = measure.doMeasure(vectorToMeasure, null, null);
                    }
                    
                    double dividendUpper = 0.0;
                    double dividendLower = 0.0;
                    double divisor = 0.0;
                    
                    for (int i = 0; i < totalPerCutAndSplit[k].length; i++) {
                        dividendUpper += (measurePerSplit_upper[i] * totalPerCutAndSplit[k][i]);
                        dividendLower += (measurePerSplit_lower[i] * totalPerCutAndSplit[k][i]);
                        divisor += totalPerCutAndSplit[k][i];
                    }
                    
                    measureLower[k] = dividendLower / divisor;
                    measureUpper[k] = dividendUpper / divisor;
                }
                
                ArrayList<Integer> indMinSupMedida = new ArrayList<Integer>();
                double minSupMedida = measureUpper[0];
                indMinSupMedida.add(0);
                
                for (int i = 1; i < measureUpper.length; i++) {
                    if (measureUpper[i] < minSupMedida) {
                        minSupMedida = measureUpper[i];
                        indMinSupMedida.clear();
                        indMinSupMedida.add(i);
                    } else if (measureUpper[i] == minSupMedida) {
                        indMinSupMedida.add(i);
                    }
                }
                
                Iterator<Integer> iterator = indMinSupMedida.iterator();
                Integer element = iterator.next();
                int minMeasureLowerIndex = element;
                double minMeasureLower = measureLower[element];

                while (iterator.hasNext()) {
                    element = iterator.next();
                    if (measureLower[element] < minMeasureLower) {
                        minMeasureLower = measureLower[element];
                        minMeasureLowerIndex = element;
                    }
                }
                
                this.bestCutPoint = this.numericAttClassObserver.getCut(minMeasureLowerIndex);
                boolean equalsPassesTest = true;
                if (this.numericAttClassObserver instanceof IademVFMLNumericAttributeClassObserver) {
                    equalsPassesTest = false;
                }
                IademNumericAttributeBinaryTest test = new IademNumericAttributeBinaryTest(this.attIndex,
                        this.bestCutPoint,
                        equalsPassesTest);
                this.bestSplitSuggestion = new IademAttributeSplitSuggestion(test,
                        new double[0][0],
                        minSupMedida,
                        minMeasureLower);
            }
            this.heuristicMeasureUpdated = true;
        }
    }
    
    private void computeClassVoteBounds(double[][][] classVotesPerCutAndSplit_lower,
            double[][][] classVotesPerCutAndSplit_upper,
            double[][] totalPerCutAndSplit,
            boolean withIntervalEstimates) {
        this.numericAttClassObserver.computeClassDistProbabilities(classVotesPerCutAndSplit_lower, 
                classVotesPerCutAndSplit_upper,
                totalPerCutAndSplit,
                withIntervalEstimates);
    }
    
    private int getClassValueProbabilities(int value, ArrayList<Integer> valueList,
            double[][] classValuePerSplitLower, double[][] classValuePerSplitUpper,
            double availableProb) {
        int max, newValue;
        double max_up, newValue_up;
        if (valueList.isEmpty()) {
            return -1;
        } else {
            max = valueList.get(0);
            max_up = Math.min(classValuePerSplitUpper[value][max],
                    classValuePerSplitLower[value][max] + availableProb);

            for (int i = 1; i < valueList.size(); i++) {
                newValue = valueList.get(i);
                newValue_up = Math.min(classValuePerSplitUpper[value][newValue],
                        classValuePerSplitLower[value][newValue] + availableProb);
                if (newValue_up > max_up) {
                    max = newValue;
                    max_up = newValue_up;
                }
            }
            return max;
        }
    }
    
    @Override
    public boolean hasInformation() {
        return (this.numericAttClassObserver.getNumberOfCutPoints() > 1);
    }

    private ArrayList<Double> getCuts() {
        return this.numericAttClassObserver.cutPointSuggestion(this.MAX_BINS_EQUAL_WIDTH);
    }

    @Override
    public double getPercent() {
        long[] classVotesLeft = this.numericAttClassObserver.getLeftClassDist(this.bestCutPoint);

        double leftCount = (double) arrSum(classVotesLeft);
        double total = (double) this.numericAttClassObserver.getValueCount();

        double leftPercent = leftCount / total;
        double rightPercent = 1.0 - leftPercent;

        if (rightPercent < leftPercent) {
            return rightPercent;
        } else {
            return leftPercent;
        }
    }

    @Override
    public DoubleVector computeConditionalProbability(double value) {
        ArrayList<Double> cut = getCuts();
        return new DoubleVector(this.numericAttClassObserver.computeConditionalProb(cut, value));
    }

    @Override
    public void getNumberOfNodes(int[] count) {
        count[1]++;
    }
}
