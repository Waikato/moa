/*
 *    IADEM2cTree.java
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

import java.io.Serializable;
import java.util.Arrays;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;

import moa.classifiers.trees.iademutils.IademException;
import moa.classifiers.trees.iademutils.IademSplitMeasure;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;

public class IADEM2cTree implements Serializable {

    private static final long serialVersionUID = 1L;

    final public static double ERROR_MARGIN = 1.0e-9;
    protected InstancesHeader problemDescription;
    protected double attributeDifferentiation;
    protected IademSplitMeasure measure;
    protected Node treeRoot;
    
    protected int predictionType;
    protected int naiveBayesLimit;
    protected double percentInCommon;

    protected IademNumericAttributeObserver numericAttObserver;
    protected long numberOfInstancesProcessed = 0;
    protected boolean hasNumericAttributes;
    protected int maxNumberOfBins = 100;
    protected boolean onlyMultiwayTest = false;
    protected boolean onlyBinaryTest = false;
    protected AbstractChangeDetector estimator;
    protected int gracePeriod = 100;
    
    public int numberOfNodes = 1,
            numberOfLeaves = 1;

    public IADEM2cTree(InstancesHeader problemDescription,
            double attributeDifferentiation,
            IademSplitMeasure measure,
            int predictionType,
            int naiveBayesLimit,
            double percentInCommon,
            IademNumericAttributeObserver numericAttObserver,
            int maxNumberOfBins,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest,
            AbstractChangeDetector estimator,
            int gracePeriod) {
        this.problemDescription = problemDescription;
        this.attributeDifferentiation = attributeDifferentiation;
        this.measure = measure;
        this.predictionType = predictionType;
        this.naiveBayesLimit = naiveBayesLimit;
        this.percentInCommon = percentInCommon;
        this.maxNumberOfBins = maxNumberOfBins;
        this.numericAttObserver = numericAttObserver;
        this.setHasNumericAttributes();
        this.onlyMultiwayTest = onlyMultiwayTest;
        this.onlyBinaryTest = onlyBinaryTest;
        this.estimator = (AbstractChangeDetector) estimator.copy();
        this.gracePeriod = gracePeriod;
        createRoot(problemDescription, numericAttObserver);
    }

    public AbstractChangeDetector newEstimator() {
        return (AbstractChangeDetector) this.estimator.copy();
    }

    public void createRoot(InstancesHeader descr,
            IademNumericAttributeObserver attObserver) {
        double[] arrayCounter = new double[descr.attribute(descr.classIndex()).numValues()];
        Arrays.fill(arrayCounter, 0);
        
        this.treeRoot = newLeafNode(null, 0, 0, arrayCounter);
    }

    public int getGracePeriod() {
        return gracePeriod;
    }

    public int getMaxNumberOfBins() {
        return maxNumberOfBins;
    }

    public void setMaxNumberOfBins(int maxNumberOfBins) {
        this.maxNumberOfBins = maxNumberOfBins;
    }

    public IademNumericAttributeObserver getNumericAttObserver() {
        return numericAttObserver;
    }

    public void setNumericAttObserver(IademNumericAttributeObserver numericAttObserver) {
        this.numericAttObserver = numericAttObserver;
    }

    public long getNumberOfInstancesProcessed() {
        return numberOfInstancesProcessed;
    }
    
    public InstancesHeader getProblemDescription() {
        return this.problemDescription;
    }

    public LeafNode newLeafNode(Node parent,
            long instTreeCountSinceVirtual,
            long instNodeCountSinceVirtual,
            double[] classDist) {
        switch (this.predictionType) {
            case 0: {
                return new LeafNode(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        classDist,
                        (IademNumericAttributeObserver) this.numericAttObserver.copy(),
                        this.onlyMultiwayTest,
                        this.onlyBinaryTest);
            }
            case 1: {
                return new LeafNodeNB(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        classDist,
                        (IademNumericAttributeObserver) this.numericAttObserver.copy(),
                        this.naiveBayesLimit,
                        this.onlyMultiwayTest,
                        this.onlyBinaryTest);
            }
            case 2: {
                return new LeafNodeNBKirkby(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        classDist,
                        (IademNumericAttributeObserver) this.numericAttObserver.copy(),
                        this.naiveBayesLimit,
                        this.onlyMultiwayTest,
                        this.onlyBinaryTest,
                        this.estimator);
            }
            default: {
                return new LeafNodeWeightedVote(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        classDist,
                        (IademNumericAttributeObserver) this.numericAttObserver.copy(),
                        this.naiveBayesLimit,
                        this.onlyMultiwayTest,
                        this.onlyBinaryTest,
                        this.estimator);
            }
        }
    }

    final protected void setHasNumericAttributes() {
        this.hasNumericAttributes = false;
        for (int i = 0; i < problemDescription.numAttributes(); i++) {
                if (problemDescription.attribute(i).isNumeric()) {
                    hasNumericAttributes = true;
                    return;
                }
            }
    }

    public void setAttributeDifferentiation(double attributeDifferentiation) {
        this.attributeDifferentiation = attributeDifferentiation;
    }

    public double getAttributeDifferentiation() {
        return attributeDifferentiation;
    }

    public void setMeasure(IademSplitMeasure measure) {
        this.measure = measure;
    }

    public IademSplitMeasure getMeasure() {
        return this.measure;
    }

    public void setTreeRoot(Node newRoot) {
        this.treeRoot = newRoot;
    }

    public void learnFromInstance(Instance instance)
            throws IademException {
        this.numberOfInstancesProcessed++;
        this.treeRoot.learnFromInstance(instance);
    }
    
    public Node getTreeRoot() {
        return this.treeRoot;
    }
    
    public double[] getClassVotes(Instance instance) {
        return this.treeRoot.getClassVotes(instance);
    }

    public double getPercentInCommon() {
        return this.percentInCommon;
    }

    public void setPercentInCommon(double percentInCommon) {
        this.percentInCommon = percentInCommon;
    }

    public boolean hasNumericAttributes() {
        return this.hasNumericAttributes;
    }

    public int getValuesOfNominalAttributes(int attIndex) {
        return this.problemDescription.attribute(attIndex).numValues();
    }
    
    public int getPredictionType() {
        return this.predictionType;
    }
    
    public int getNaiveBayesLimit() {
        return this.naiveBayesLimit;
    }

    public boolean isOnlyMultiwayTest() {
        return onlyMultiwayTest;
    }

    public void setOnlyMultiwayTest(boolean onlyMultiwayTest) {
        this.onlyMultiwayTest = onlyMultiwayTest;
    }

    public boolean isOnlyBinaryTest() {
        return onlyBinaryTest;
    }

    public void setOnlyBinaryTest(boolean onlyBinaryTest) {
        this.onlyBinaryTest = onlyBinaryTest;
    }

    public void incrNumberOfInstancesProcessed() {
        this.numberOfInstancesProcessed++;
    }
    
    public void getNumberOfNodes(int [] count) {
        this.treeRoot.getNumberOfNodes(count);
    }
    
    public void newSplit(int numOfLeaves) {
        this.numberOfLeaves += numOfLeaves - 1;
        this.numberOfNodes += numOfLeaves;
    }

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public void setNumberOfNodes(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    public int getNumberOfLeaves() {
        return numberOfLeaves;
    }

    public void setNumberOfLeaves(int numberOfLeaves) {
        this.numberOfLeaves = numberOfLeaves;
    }
    
    
}
