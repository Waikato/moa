/*
 *    NominalVirtualNode.java
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

import moa.classifiers.trees.iademutils.IademAttributeSplitSuggestion;
import java.util.Arrays;
import java.util.ArrayList;
import moa.classifiers.core.conditionaltests.NominalAttributeBinaryTest;

import moa.classifiers.trees.iademutils.IademException;
import moa.classifiers.trees.iademutils.IademSplitMeasure;
import moa.classifiers.trees.iademutils.IademNominalAttributeBinaryTest;
import moa.classifiers.trees.iademutils.IademCommonProcedures;
import moa.classifiers.trees.iademutils.IademNominalAttributeMultiwayTest;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.Instance;
import weka.core.Utils;

public class NominalVirtualNode extends VirtualNode {

    private static final long serialVersionUID = 1L;

    protected AutoExpandVector<DoubleVector> nominalAttClassObserver = new AutoExpandVector<DoubleVector>();

    protected DoubleVector attValueDist;
    protected boolean onlyMultiwayTest = false;
    protected boolean onlyBinaryTest = false;

    public NominalVirtualNode(IADEM2cTree tree,
            Node parent,
            int attIndex,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest) {
        super(tree, parent, attIndex);

        this.attValueDist = new DoubleVector();
        this.onlyMultiwayTest = onlyMultiwayTest;
        this.onlyBinaryTest = onlyBinaryTest;
    }

    public AutoExpandVector<DoubleVector> getNominalAttClassObserver() {
        return nominalAttClassObserver;
    }

    @Override
    public Node learnFromInstance(Instance inst) {
        double attValue = inst.value(this.attIndex);
        if (Utils.isMissingValue(attValue)) {
        } else {
            int intAttValue = (int) attValue;

            this.attValueDist.addToValue(intAttValue, inst.weight());
            this.classValueDist.addToValue((int) inst.value(inst.classIndex()), inst.weight());

            DoubleVector valDist = this.nominalAttClassObserver.get(intAttValue);
            if (valDist == null) {
                valDist = new DoubleVector();
                this.nominalAttClassObserver.set(intAttValue, valDist);
            }
            int classValue = (int) inst.classValue();
            valDist.addToValue(classValue, inst.weight());

            this.heuristicMeasureUpdated = false;
        }
        return this;
    }

    @Override
    public SplitNode getNewSplitNode(long newTotal,
            Node parent,
            IademAttributeSplitSuggestion bestSuggestion) {
        InstancesHeader problemDescription = this.tree.problemDescription;
        SplitNode splitNode = new SplitNode(this.tree,
                parent,
                null,
                ((LeafNode) this.parent).getMajorityClassVotes(),
                bestSuggestion.splitTest);

        Node[] children;
        if (bestSuggestion.splitTest instanceof IademNominalAttributeMultiwayTest) {
            children = new Node[this.tree.getProblemDescription().attribute(this.attIndex).numValues()];
            for (int i = 0; i < children.length; i++) {
                long count = 0;
                double[] tmpClassDist = new double[problemDescription.attribute(problemDescription.classIndex()).numValues()];
                Arrays.fill(tmpClassDist, 0);
                for (int j = 0; j < tmpClassDist.length; j++) {

                    DoubleVector classCount = nominalAttClassObserver.get(i);
                    double contadorAtributoClase = classCount != null ? classCount.getValue(j) : 0.0;
                    tmpClassDist[j] = contadorAtributoClase;
                    count += tmpClassDist[j];
                }
                children[i] = tree.newLeafNode(splitNode,
                        newTotal,
                        count,
                        tmpClassDist);
            }
        } else { // binary split
            children = new Node[2];
            IademNominalAttributeBinaryTest binarySplit = (IademNominalAttributeBinaryTest) bestSuggestion.splitTest;
            // to the left
            double[] tmpClassDist = new double[problemDescription.attribute(problemDescription.classIndex()).numValues()];
            // total de valores
            double tmpCount = 0;
            Arrays.fill(tmpClassDist, 0);
            DoubleVector classDist = nominalAttClassObserver.get(binarySplit.getAttValue());
            if (classDist != null) {
                for (int i = 0; i < tmpClassDist.length; i++) {
                    tmpClassDist[i] = classDist.getValue(i);
                    tmpCount += classDist.getValue(i);
                }
            }
            children[0] = tree.newLeafNode(splitNode,
                    newTotal,
                    (int) tmpCount,
                    tmpClassDist);
            // to the right
            tmpCount = this.classValueDist.sumOfValues() - tmpCount;
            for (int i = 0; i < tmpClassDist.length; i++) {
                tmpClassDist[i] = this.classValueDist.getValue(i) - tmpClassDist[i];
            }
            children[1] = tree.newLeafNode(splitNode,
                    newTotal,
                    (int) tmpCount,
                    tmpClassDist);

        }
        splitNode.setChildren(children);
        return splitNode;
    }

    protected boolean moreThanOneAttValueObserved() {
        int count = 0;
        for (DoubleVector tmpClassDist : this.nominalAttClassObserver) {
            if (tmpClassDist != null) {
                count++;
            }
            if (count > 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateHeuristicMeasure() throws IademException {
        if (moreThanOneAttValueObserved()/**/) {
            if (!this.onlyBinaryTest) {
                updateHeuristicMeasureMultiwayTest();
            }
            if (!this.onlyMultiwayTest) {
                updateHeuristicMeasureBinaryTest();
            }
        } else {
            this.bestSplitSuggestion = null;
        }
        this.heuristicMeasureUpdated = true;
    }

    public void updateHeuristicMeasureBinaryTest() throws IademException {
        if (!this.heuristicMeasureUpdated) {
            double measureLower, measureUpper;
            InstancesHeader problemDescription = this.tree.problemDescription;
            IademSplitMeasure measure = tree.getMeasure();
            if (this.bestSplitSuggestion != null
                    && this.bestSplitSuggestion.splitTest instanceof NominalAttributeBinaryTest) {
                this.bestSplitSuggestion = null;
            }

            int numberOfSplits = 2, // binary test
                    numberOfTests = this.tree.getValuesOfNominalAttributes(this.attIndex);
            int numberOfClasses = problemDescription.attribute(problemDescription.classIndex()).numValues();

            double[][][] classDistPerTestAndSplit_lower = new double[numberOfTests][numberOfSplits][numberOfClasses];
            double[][][] classDistPerTestAndSplit_upper = new double[numberOfTests][numberOfSplits][numberOfClasses];
            computeClassDistBinaryTest(classDistPerTestAndSplit_lower,
                    classDistPerTestAndSplit_upper);

            for (int k = 0; k < numberOfTests; k++) {
                double[] sumPerSplit_lower = new double[numberOfSplits];
                double[] availableErrorPerSplit = new double[numberOfSplits];
                Arrays.fill(sumPerSplit_lower, 0.0);
                for (int i = 0; i < numberOfSplits; i++) {
                    for (int j = 0; j < numberOfClasses; j++) {
                        sumPerSplit_lower[i] += classDistPerTestAndSplit_lower[k][i][j];
                    }
                    availableErrorPerSplit[i] = 1.0 - sumPerSplit_lower[i];

                    if (availableErrorPerSplit[i] < 0.0) {
                        if (Math.abs(availableErrorPerSplit[i]) < IADEM2cTree.ERROR_MARGIN) {
                            availableErrorPerSplit[i] = 0.0;
                        } else {
                            throw new IademException("NominalVirtualNode", "updateHeuristicMeasureBinaryTest",
                                    "Problems when calculating measures");
                        }
                    }
                }

                double[] measurePerSplit_upper = new double[numberOfSplits];

                double[] valueLevels = new double[numberOfSplits];
                for (int i = 0; i < numberOfSplits; i++) {
                    ArrayList<Double> lot = new ArrayList<Double>();
                    lot.add(0.0);
                    lot.add(1.0);

                    ArrayList<Integer> hole = new ArrayList<Integer>();
                    hole.add(0);
                    for (int j = 0; j < numberOfClasses; j++) {
                        IademCommonProcedures.insertLotsHoles(lot, hole, classDistPerTestAndSplit_lower[k][i][j],
                                classDistPerTestAndSplit_upper[k][i][j]);
                    }
                    valueLevels[i] = IademCommonProcedures.computeLevel(lot, hole,
                            availableErrorPerSplit[i]);
                }
                for (int i = 0; i < numberOfSplits; i++) {
                    ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                    for (int j = 0; j < numberOfClasses; j++) {
                        double measureProb_uppper = Math.max(valueLevels[i], classDistPerTestAndSplit_lower[k][i][j]);
                        measureProb_uppper = Math.min(classDistPerTestAndSplit_upper[k][i][j], measureProb_uppper);
                        vectorToMeasure.add(measureProb_uppper);
                    }
                    measurePerSplit_upper[i] = measure.doMeasure(vectorToMeasure, null, null);
                }

                double[] measurePerSplit_lower = new double[numberOfSplits];

                for (int i = 0; i < numberOfSplits; i++) {
                    double tmpAvailable = availableErrorPerSplit[i];

                    ArrayList<Integer> decOrderClassDist_upper = new ArrayList<Integer>();

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
                                throw new IademException("NominalVirtualNode", "updateHeuristicMeasureBinaryTest",
                                        "Problems when calculating measures");
                            }
                        }
                        int classIndex = getClassProbabilities(i, unusedClasses,
                                classDistPerTestAndSplit_lower[k],
                                classDistPerTestAndSplit_upper[k],
                                auxAvailable);
                        double probUp = Math.min(classDistPerTestAndSplit_upper[k][i][classIndex],
                                classDistPerTestAndSplit_lower[k][i][classIndex] + auxAvailable);

                        auxAvailable -= (probUp - classDistPerTestAndSplit_lower[k][i][classIndex]);

                        unusedClasses.remove(new Integer(classIndex));
                        decOrderClassDist_upper.add(new Integer(classIndex));
                    }

                    ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                    for (int j = 0; j < decOrderClassDist_upper.size(); j++) {
                        int classIndex = decOrderClassDist_upper.get(j);

                        if (tmpAvailable < 0.0) {
                            if (Math.abs(tmpAvailable) < IADEM2cTree.ERROR_MARGIN) {
                                tmpAvailable = 0.0;
                            } else {
                                throw new IademException("NominalVirtualNode", "updateHeuristicMeasureBinaryTest",
                                        "Problems when calculating measures");
                            }
                        }
                        double probUp = Math.min(classDistPerTestAndSplit_upper[k][i][classIndex],
                                classDistPerTestAndSplit_lower[k][i][classIndex] + tmpAvailable);
                        tmpAvailable -= (probUp - classDistPerTestAndSplit_lower[k][i][classIndex]);
                        vectorToMeasure.add(probUp);
                    }

                    measurePerSplit_lower[i] = measure.doMeasure(vectorToMeasure, null, null);
                }

                double dividendUpper = 0.0;
                double dividendLower = 0.0;

                double leftDivUpper = measurePerSplit_upper[0] * attValueDist.getValue(k),
                        leftDivLower = measurePerSplit_lower[0] * attValueDist.getValue(k);
                double divisor = classValueDist.sumOfValues(),
                        rightTotal = divisor - attValueDist.getValue(k);
                double rightDivUpper = measurePerSplit_upper[1] * rightTotal,
                        rightDivLower = measurePerSplit_lower[1] * rightTotal;
                dividendUpper = leftDivUpper + rightDivUpper;
                dividendLower = leftDivLower + rightDivLower;

                if (divisor != 0) {
                    measureLower = dividendLower / divisor;
                    measureUpper = dividendUpper / divisor;
                    if (this.bestSplitSuggestion == null) {
                        DoubleVector tmpClassDist = this.nominalAttClassObserver.get(k);
                        if (tmpClassDist != null) { // is it a useful split?
                            NominalAttributeBinaryTest test = new IademNominalAttributeBinaryTest(this.attIndex, k);
                            this.bestSplitSuggestion = new IademAttributeSplitSuggestion(test,
                                    new double[0][0],
                                    measureUpper,
                                    measureLower);
                        }
                    } // compete with multiway test 
                    else if (!this.onlyBinaryTest) {
                        if ((measureUpper < this.bestSplitSuggestion.merit)
                                || (measureUpper == this.bestSplitSuggestion.merit
                                && measureLower < this.bestSplitSuggestion.getMeritLowerBound())) {
                            DoubleVector tmpClassDist = this.nominalAttClassObserver.get(k);
                            if (tmpClassDist != null) { // is it a useful split?
                                NominalAttributeBinaryTest test = new IademNominalAttributeBinaryTest(this.attIndex, k);
                                this.bestSplitSuggestion = new IademAttributeSplitSuggestion(test,
                                        new double[0][0],
                                        measureUpper,
                                        measureLower);
                            }
                        }
                    }
                }
            }

        }
    }

    protected void computeClassDistBinaryTest(double[][][] classDistPerTestAndSplit_lower,
            double[][][] classDistPerTestAndSplit_upper) {
        int numberOfClasses = classDistPerTestAndSplit_lower[0][0].length;
        double estimator, bound;
        double leftTotal = this.classValueDist.sumOfValues();
        for (int currentAttIndex = 0; currentAttIndex < classDistPerTestAndSplit_lower.length; currentAttIndex++) {
            for (int j = 0; j < numberOfClasses; j++) {
                // compute probabilities in the left branch
                DoubleVector classCounter = nominalAttClassObserver.get(currentAttIndex);
                double attClassCounter = classCounter != null ? classCounter.getValue(j) : 0.0;
                if (attValueDist.getValue(currentAttIndex) != 0) {
                    estimator = attClassCounter / attValueDist.getValue(currentAttIndex);
                    bound = IademCommonProcedures.getIADEM_HoeffdingBound(estimator, attValueDist.getValue(currentAttIndex));
                    classDistPerTestAndSplit_lower[currentAttIndex][0][j] = Math.max(0.0, estimator - bound);
                    classDistPerTestAndSplit_upper[currentAttIndex][0][j] = Math.min(1.0, estimator + bound);
                } else {
                    classDistPerTestAndSplit_lower[currentAttIndex][0][j] = 0.0;
                    classDistPerTestAndSplit_upper[currentAttIndex][0][j] = 1.0;
                }
                // compute probabilities in the right branch
                attClassCounter = classValueDist.getValue(j) - attClassCounter;
                double rightTotal = leftTotal - attValueDist.getValue(currentAttIndex);
                if (rightTotal != 0) {
                    estimator = attClassCounter / rightTotal;
                    bound = IademCommonProcedures.getIADEM_HoeffdingBound(estimator, rightTotal);
                    classDistPerTestAndSplit_lower[currentAttIndex][1][j] = Math.max(0.0, estimator - bound);
                    classDistPerTestAndSplit_upper[currentAttIndex][1][j] = Math.min(1.0, estimator + bound);
                } else {
                    classDistPerTestAndSplit_lower[currentAttIndex][1][j] = 0.0;
                    classDistPerTestAndSplit_upper[currentAttIndex][1][j] = 1.0;
                }
            }
        }
    }

    public void updateHeuristicMeasureMultiwayTest() throws IademException {
        if (!this.heuristicMeasureUpdated) {
            this.bestSplitSuggestion = null;
            double measureLower, measureUpper;
            InstancesHeader problemDescription = this.tree.problemDescription;
            IademSplitMeasure measure = tree.getMeasure();

            int numberOfValues = tree.getValuesOfNominalAttributes(attIndex);
            int numberOfClasses = problemDescription.attribute(problemDescription.classIndex()).numValues();
            double[][] classDist_lower = new double[numberOfValues][numberOfClasses];
            double[][] classDist_upper = new double[numberOfValues][numberOfClasses];

            computeClassDistPerValue(classDist_lower, classDist_upper);

            double[] sumPerValue_lower = new double[numberOfValues];
            double[] availableErrorPerValue = new double[numberOfValues];
            Arrays.fill(sumPerValue_lower, 0.0);
            for (int i = 0; i < numberOfValues; i++) {
                for (int j = 0; j < numberOfClasses; j++) {
                    sumPerValue_lower[i] += classDist_lower[i][j];
                }
                availableErrorPerValue[i] = 1.0 - sumPerValue_lower[i];

                if (availableErrorPerValue[i] < 0.0) {
                    if (Math.abs(availableErrorPerValue[i]) < IADEM2cTree.ERROR_MARGIN) {
                        availableErrorPerValue[i] = 0.0;
                    } else {
                        throw new IademException("NominalVirtualNode", "updateHeuristicMeasureMultiwayTest",
                                "Problems when calculating measures");
                    }
                }
            }

            double[] measuerPerValue_upper = new double[numberOfValues];

            double[] valueLevels = new double[numberOfValues];
            for (int i = 0; i < numberOfValues; i++) {
                ArrayList<Double> lot = new ArrayList<Double>();
                lot.add(0.0);
                lot.add(1.0);

                ArrayList<Integer> hole = new ArrayList<Integer>();
                hole.add(0);
                for (int j = 0; j < numberOfClasses; j++) {
                    IademCommonProcedures.insertLotsHoles(lot, hole, classDist_lower[i][j],
                            classDist_upper[i][j]);
                }

                valueLevels[i] = IademCommonProcedures.computeLevel(lot, hole,
                        availableErrorPerValue[i]);
            }

            for (int i = 0; i < numberOfValues; i++) {
                ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                for (int j = 0; j < numberOfClasses; j++) {
                    double p_sup_medida = Math.max(valueLevels[i], classDist_lower[i][j]);
                    p_sup_medida = Math.min(classDist_upper[i][j], p_sup_medida);
                    vectorToMeasure.add(p_sup_medida);
                }
                measuerPerValue_upper[i] = measure.doMeasure(vectorToMeasure, null, null);
            }
            // compute measure lower bounds
            double[] measurePerValue_lower = new double[numberOfValues];
            for (int i = 0; i < numberOfValues; i++) {
                double availableError = availableErrorPerValue[i];
                ArrayList<Integer> decOrderClassDist_upper = new ArrayList<Integer>();

                ArrayList<Integer> unusedClasses = new ArrayList<Integer>();
                for (int j = 0; j < numberOfClasses; j++) {
                    unusedClasses.add(j);
                }

                double auxAvailable = availableErrorPerValue[i];
                for (int j = 0; j < numberOfClasses; j++) {
                    if (auxAvailable < 0.0) {
                        if (Math.abs(auxAvailable) < IADEM2cTree.ERROR_MARGIN) {
                            auxAvailable = 0.0;
                        } else {
                            throw new IademException("NominalVirtualNode", "updateHeuristicMeasureMultiwayTest",
                                    "Problems when calculating measures");
                        }
                    }

                    int classID = getClassProbabilities(i, unusedClasses, classDist_lower,
                            classDist_upper, auxAvailable);
                    double probUp = Math.min(classDist_upper[i][classID],
                            classDist_lower[i][classID] + auxAvailable);

                    auxAvailable -= (probUp - classDist_lower[i][classID]);

                    unusedClasses.remove(new Integer(classID));
                    decOrderClassDist_upper.add(new Integer(classID));
                }

                ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                for (int j = 0; j < decOrderClassDist_upper.size(); j++) {
                    int classID = decOrderClassDist_upper.get(j);

                    if (availableError < 0.0) {
                        if (Math.abs(availableError) < IADEM2cTree.ERROR_MARGIN) {
                            availableError = 0.0;
                        } else {
                            throw new IademException("NominalVirtualNode", "updateHeuristicMeasureMultiwayTest",
                                    "Problems when calculating measures");
                        }
                    }
                    double probUp = Math.min(classDist_upper[i][classID],
                            classDist_lower[i][classID] + availableError);
                    availableError -= (probUp - classDist_lower[i][classID]);
                    vectorToMeasure.add(probUp);
                }

                measurePerValue_lower[i] = measure.doMeasure(vectorToMeasure, null, null);
            }

            double dividendUpper = 0.0;
            double dividendLower = 0.0;
            double divisor = 0.0;

            for (int i = 0; i < attValueDist.numValues(); i++) {
                dividendUpper += (measuerPerValue_upper[i] * attValueDist.getValue(i));
                dividendLower += (measurePerValue_lower[i] * attValueDist.getValue(i));
                divisor += attValueDist.getValue(i);
            }
            measureLower = dividendLower / divisor;
            measureUpper = dividendUpper / divisor;

            int maxBranches = this.tree.getProblemDescription().attribute(attIndex).numValues();
            IademNominalAttributeMultiwayTest test = new IademNominalAttributeMultiwayTest(this.attIndex, maxBranches);
            bestSplitSuggestion = new IademAttributeSplitSuggestion(test,
                    new double[0][0],
                    measureUpper,
                    measureLower);
        }
    }

    private void computeClassDistPerValue(double[][] classDistLower,
            double[][] classDistUpper) {
        double estimator, classDistError;

        int numberOfValues = classDistLower.length;
        int numberOfClasses = classDistLower[0].length;

        for (int i = 0; i < numberOfValues; i++) {
            for (int j = 0; j < numberOfClasses; j++) {
                if (attValueDist.getValue(i) == 0.0) {
                    classDistLower[i][j] = 0.0;
                    classDistUpper[i][j] = 1.0;
                } else {
                    DoubleVector classCounter = nominalAttClassObserver.get(i);
                    double attValuePerClassCounter = classCounter != null ? classCounter.getValue(j) : 0.0;
                    estimator = attValuePerClassCounter / attValueDist.getValue(i);
                    classDistError = IademCommonProcedures.getIADEM_HoeffdingBound(estimator, attValueDist.getValue(i));
                    classDistLower[i][j] = Math.max(0.0, estimator - classDistError);
                    classDistUpper[i][j] = Math.min(1.0, estimator + classDistError);
                }
            }
        }
    }

    private int getClassProbabilities(int attributeValue, ArrayList<Integer> attValueList,
            double[][] classDistPerValueLower, double[][] classDistPerValueUpper,
            double available) {
        int max, tmp;
        double maxProbUp, newProbUp;
        if (attValueList.isEmpty()) {
            return -1;
        } else {
            max = attValueList.get(0);
            maxProbUp = Math.min(classDistPerValueUpper[attributeValue][max],
                    classDistPerValueLower[attributeValue][max] + available);

            for (int i = 1; i < attValueList.size(); i++) {
                tmp = attValueList.get(i);
                newProbUp = Math.min(classDistPerValueUpper[attributeValue][tmp],
                        classDistPerValueLower[attributeValue][tmp] + available);
                if (newProbUp > maxProbUp) {
                    max = tmp;
                    maxProbUp = newProbUp;
                }
            }
            return max;
        }
    }

    @Override
    public DoubleVector computeConditionalProbability(double valor) {
        int numberOfValues = nominalAttClassObserver.size();
        DoubleVector conditionalProbability = new DoubleVector();

        DoubleVector sumsPerClass = new DoubleVector();
        for (int i = 0; i < numberOfValues; i++) {
            DoubleVector classCounter = nominalAttClassObserver.get(i);
            int numberOfClasses = classCounter != null ? classCounter.numValues() : 0;
            for (int j = 0; j < numberOfClasses; j++) {
                double attClassCounter = classCounter.getValue(j);
                sumsPerClass.addToValue(j, attClassCounter);
            }
        }

        for (int i = 0; i < sumsPerClass.numValues(); i++) {
            if (sumsPerClass.getValue(i) != 0.0) {
                DoubleVector contadorClase = nominalAttClassObserver.get((int) valor);
                double attClassCounter = contadorClase != null ? contadorClase.getValue(i) : 0.0;
                conditionalProbability.setValue(i, attClassCounter / sumsPerClass.getValue(i));
            }
        }

        return conditionalProbability;
    }

    @Override
    public double getPercent() {
        double counter = 0;
        double maxInstances = 0;
        for (int i = 0; i < attValueDist.numValues(); i++) {
            counter += attValueDist.getValue(i);
            if (attValueDist.getValue(i) > maxInstances) {
                maxInstances = attValueDist.getValue(i);
            }
        }
        double maxPercent = maxInstances / counter;
        return maxPercent;
    }

    @Override
    public boolean hasInformation() {
        return true;
    }

    @Override
    public void getNumberOfNodes(int[] count) {
        count[1]++;
    }
}
