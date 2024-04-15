/*
 *    StreamingGradientBoostedTrees.java
 *    Copyright (C) 2024 University of Waikato, Hamilton, New Zealand
 *    @author Nuwan Gunasekara (ng98@students.waikato.ac.nz)
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
package moa.classifiers.meta;

import com.github.javacliparser.*;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.AbstractMOAObject;
import moa.capabilities.Capabilities;
import moa.classifiers.*;
import moa.core.*;
import moa.options.ClassOption;
import moa.classifiers.meta.StreamingRandomPatches;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Gradient boosted trees for evolving data streams
 *
 * <p>Streaming Gradient Boosted Trees (SGBT), which is trained using weighted squared loss elicited
 * in XGBoost. SGBT exploits trees with a replacement strategy to detect and
 * recover from drifts, thus enabling the ensemble to adapt without sacrificing the
 * predictive performance.</p>
 *
 * <p>See details in:<br> Nuwan Gunasekara, Bernhard Pfahringer, Heitor Murilo Gomes, Albert Bifet.
 * Gradient Boosted Trees for Evolving Data Streams.
 * Machine Learning, Springer, 2024.
 * <a href="https://doi.org/10.1007/s10994-024-06517-y">DOI</a>. </p>
 *
 * <p>Parameters:</p> <ul>
 * <li>-l : Classifier to train on instances.</li>
 * <li>-s : The number of boosting iterations.</li>
 * <li>-m : Percentage (%) of attributes for each boosting iteration.</li>
 * <li>-L : Learning rate.</li>
 * <li>-H : Disable one-hot encoding for regressors that supports nominal attributes.</li>
 * <li>-M : Multiple training iterations by Ceiling (Hessian * M).</li>
 * <li>-S : Randomly skipp 1/S th of instances at training (S=1: No Skip, use all instances for training).</li>
 * <li>-K : Use Squared Loss for Classification.</li>
 * </ul>
 *
 * @author Nuwan Gunasekara (ng98 at students dot waikato dot ac dot nz)
 * @version $Revision: 1 $
 */
public class StreamingGradientBoostedTrees extends AbstractClassifier implements MultiClassClassifier, Regressor {

    private static final long serialVersionUID = 1L;

    //region ================ OPTIONS ================
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train on instances.", Classifier.class, "trees.FIMTDD -s VarianceReductionSplitCriterion -g 25 -c 0.05 -e -p");
    public IntOption numberOfboostingIterations = new IntOption("numberOfboostingIterations", 's',
            "The number of boosting iterations.", 100, 1, Integer.MAX_VALUE);
    public IntOption percentageOfAttributesForEachBoostingIteration = new IntOption("percentageOfAttributesForEachBoostingIteration", 'm',
            "Percentage of attributes % for each boosting iteration.", 75, Integer.MIN_VALUE, Integer.MAX_VALUE);
    public FloatOption learningRateOption = new FloatOption(
            "learningRate", 'L', "Learning rate",
            0.0125, 0, 1.00);
    public FlagOption disableOneHotEncoding = new FlagOption("disableOneHotEncoding", 'H', "disable one-hot encoding for regressors that supports nominal attributes.");
    public IntOption multipleIterationByCeilingOfHessianTimesM = new IntOption("multipleIterationByCeilingOfHessianTimesM", 'M',
            "Multiple training iterations by Ceiling (Hessian * M). M = 1: No multiple iterations.", 1, 1, 100);
    public IntOption randomlySkip1SthOfInstancesAtTraining = new IntOption("randomlySkip1SthOfInstancesAtTraining", 'S',
            "Randomly skip 1/S th of instances at training (S=1: No Skip, use all instances for training).", 1, 1, Integer.MAX_VALUE);
    public FlagOption useSquaredLossForClassification = new FlagOption("useSquaredLossForClassification", 'K', "Use Squared Loss for Classification.");
    public IntOption randomSeedOption = new IntOption("randomSeed", 'r', "The random seed", 1);
    //endregion ================ OPTIONS ================

    //region ================ VARIABLES ================
    protected SGBT[] SGBTCommittee;
    protected boolean reset;
    protected int numberClasses;
    protected double[] lastPrediction = null;
    //endregion ================ VARIABLES ================

    //region ================ OVERRIDDEN METHODS ================
    @Override
    public void resetLearningImpl() {
        if(super.randomSeedOption.getValue() != Integer.parseInt(super.randomSeedOption.getDefaultCLIString())){ // super random set
            this.randomSeedOption.setValue(super.randomSeedOption.getValue()); // override current random
        }
        this.reset = true;
        this.classifierRandom = new Random(randomSeedOption.getValue());
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public Capabilities getCapabilities() {
        return super.getCapabilities();
    }
    @Override
    public boolean correctlyClassifies(Instance inst) {
//        Assumes test then train evaluation set up
        return Utils.maxIndex(lastPrediction) == (int) inst.classValue();
    }
    @Override
    public int measureByteSize() {
        long b = 0;
        return (int) b;
    }
    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.reset) { // init
            this.reset = false;
            if (inst.classAttribute().isNominal()){
                this.numberClasses = inst.numClasses();
            }else{
                this.numberClasses = 1;
            }
            createSGBTs(this.numberClasses <= 2 ? 1 : this.numberClasses);
        }

        if (this.numberClasses <= 2){ // regression or binary classification
            SGBTCommittee[0].trainOnInstance(inst);
        }else {  // multi class classification
            Instance[] binaryClassInstanceArray = getBinaryClassInstanceArray(inst);
            // train each learner
            IntStream.range(0, SGBTCommittee.length)
                    .parallel()
                    .forEach(i -> SGBTCommittee[i].trainOnInstance(binaryClassInstanceArray[i]));
        }
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        double[] votes = new double[inst.classAttribute().isNominal() ? inst.numClasses(): 1];
        if (!this.reset) {
            if (this.numberClasses <= 2){ // regression or binary classification
                return SGBTCommittee[0].getVotesForInstance(inst);
            }else { // multi class classification
                Instance[] binaryClassInstanceArray = getBinaryClassInstanceArray(inst);
                // get prediction from each base learner
                IntStream.range(0, SGBTCommittee.length)
                        .parallel()
                        .forEach(i -> votes[i] = getVoteForPositiveClass(SGBTCommittee[i], binaryClassInstanceArray[i]));

                if (Utils.sum(votes) > 0.0) {
                    try {
                        Utils.normalize(votes);
                    } catch (Exception e) {
                        System.out.println("Error");
                        // ignore all zero votes error
                    }
                }
            }
        }
        lastPrediction = votes;
        return votes;
    }
    //endregion ================ OVERRIDDEN METHODS ================

    //region ================ STATIC METHODS ================
    static public Instance newBinaryClassInstance(Instance instance){
        int classIndex = instance.classIndex();
        ArrayList<Attribute> attributes = new ArrayList<>();
        ArrayList<Double> v = new ArrayList<>();
        List<String> classAttributeValues = new ArrayList<>();

        // set attributes and values for all except class attribute
        for (int i = 0; i < instance.numAttributes(); i++){
            if (i != classIndex){
                attributes.add(instance.attribute(i));
                v.add(instance.value(i));
            }
        }

        // set class information
        classAttributeValues.add(0,"0");
        classAttributeValues.add(1,"1");
        Attribute classAttribute = new Attribute("classAttribute", classAttributeValues);

        attributes.add(classAttribute);
        v.add(0.0);

        Instances newInstances = new Instances("Copy", attributes, 100);
        newInstances.setClassIndex(newInstances.numAttributes()-1);

        double[] values = v.stream()
//                .parallel()
                .mapToDouble(Double::doubleValue).toArray();
        double weight = instance.weight();

        DenseInstance newInstance = new DenseInstance(weight, values);
        newInstance.setWeight(weight);
        newInstance.setDataset(newInstances);

        newInstances.add(newInstance);
        return newInstance;
    }
    static double getVoteForPositiveClass(SGBT binarySgbt, Instance inst){
        DoubleVector votes = new DoubleVector(binarySgbt.getVotesForInstance(inst));
        if (votes.sumOfValues() > 0.0) {
            votes.normalize();
        }
        int numOfClassValues = votes.numValues();
        if (numOfClassValues > 0){
            if (numOfClassValues == 1){ // sometimes you would only get one vote, if the base learner has only seen one class
                return votes.getArrayRef()[0];
            }else{
                return votes.getArrayRef()[1];
            }
        }else{
            return 0.0;
        }
    }
    public static Instance getSubInstance(Instance instance, double weight, ArrayList<Integer> subSpaceFeaturesIndexes, boolean setNumericClassAttribute, double numericClassValue, boolean useOneHotEncoding) {
        Instances subset;
        ArrayList<Attribute> attSub = new ArrayList<>();
        ArrayList<Double> v = new ArrayList<>();
        Attribute classAttribute;
        int totalOneHotEncodedSize = 0;
        int totalOneHotEncodedInstances = 0;
        int i = 0;
        // Add attributes of the selected subset
        for (Integer featuresIndex : subSpaceFeaturesIndexes) {
            int index = i + totalOneHotEncodedSize - totalOneHotEncodedInstances;
            if (useOneHotEncoding && instance.attribute(featuresIndex).isNominal()) {
                if (instance.attribute(featuresIndex).numValues() > 2){
                    // Do one hot-encoding
                    for (int j = 0; j < instance.attribute(featuresIndex).numValues(); j++) {
                        attSub.add(new Attribute(""));
                        v.add(0.0);
                    }
                    v.set(index + (int) instance.value(featuresIndex), 1.0);

                    totalOneHotEncodedSize += instance.attribute(featuresIndex).numValues();
                    totalOneHotEncodedInstances++;
                }else{ // binary feature
                    attSub.add(new Attribute("")); // create a numeric attribute
                    v.add(instance.value(featuresIndex)); // sets the index as value
                }
            } else {
                attSub.add(instance.attribute(featuresIndex));
                v.add(instance.value(featuresIndex));
            }
            i++;
        }
        // add class attribute
        if (setNumericClassAttribute) {
            // adds a numeric class attribute
            classAttribute = new Attribute("classAttribute");
        } else {
            classAttribute = instance.classAttribute();
        }
        attSub.add(classAttribute);
        v.add(setNumericClassAttribute ? numericClassValue : instance.classValue());
        subset = new Instances("Subsets Candidate Instances", attSub, 100);
        subset.setClassIndex(subset.numAttributes() - 1);

        // Set the class value for each value array.
        double[] values = new double[v.size()];
        for (int k = 0; k < v.size(); k++) {
            values[k] = v.get(k);
        }

        DenseInstance subInstance = new DenseInstance(weight, values);
        subInstance.setWeight(weight);
        subInstance.setDataset(subset);

        subset.add(subInstance);
        return subInstance;
    }

    public static double[] getScoresWhenNullTree(int outputSize) {
        return new double[outputSize];
    }

    static double[] getScoreFromSubInstance(Instance inst, ArrayList<Integer> subSpaceFeaturesIndexes, boolean setNumericClassAttribute, SGBT.BoostingCommittee b, boolean useOneHotEncoding) {
        Instance subInstance = getSubInstance(inst, 1.0, subSpaceFeaturesIndexes, setNumericClassAttribute, -1, useOneHotEncoding);
        return b.getScoresForInstance(subInstance);
    }
    //endregion ================ STATIC METHODS ================

    //region ================ OTHER METHODS ================
    protected  void createSGBTs(int numSGBTs) {
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        SGBT base = new SGBT(baseLearner, this.classifierRandom);
        base.numberOfboostingIterations.setValue(numberOfboostingIterations.getValue());
        base.percentageOfAttributesForEachBoostingIteration.setValue(percentageOfAttributesForEachBoostingIteration.getValue());
        base.learningRateOption.setValue(learningRateOption.getValue());
        if (this.disableOneHotEncoding.isSet()){
            if (this.baseLearnerOption.getDefaultCLIString().contains("trees.FIMTDD")){
                System.out.println("WARNING: One-hot encoding is DISABLED for baseLearner '" +this.baseLearnerOption.getDefaultCLIString() + "' which does NOT support nominal attributes.");
            }
        }
        base.useOneHotEncoding.setValue(!disableOneHotEncoding.isSet());
        base.multipleIterationByCeilingOfHessianTimesM.setValue(multipleIterationByCeilingOfHessianTimesM.getValue());
        base.randomlySkip1SthOfInstancesAtTraining.setValue(randomlySkip1SthOfInstancesAtTraining.getValue());
        base.useSquaredLossForClassification.setValue(useSquaredLossForClassification.isSet());

        SGBTCommittee = new SGBT[numSGBTs];
        for (int i = 0; i < SGBTCommittee.length; i++) {
            SGBTCommittee[i] = (SGBT) base.copy();
        }
    }

    Instance[] getBinaryClassInstanceArray(Instance inst){
        int actualClass = (int) inst.classValue();

        // create binaryClassInstanceArray
        Instance binaryInstance = newBinaryClassInstance(inst);

        // generate multiple instances
        Instance[] binaryClassInstanceArray = new Instance[SGBTCommittee.length];
        IntStream.range(0, SGBTCommittee.length)
//                .parallel()
                .forEach(i -> binaryClassInstanceArray[i] = binaryInstance.copy());

        // set label based on actualClass binaryClassInstanceArray
        IntStream.range(0, SGBTCommittee.length)
//                .parallel()
                .forEach(i -> binaryClassInstanceArray[i].setClassValue((i == actualClass) ? 1.0 : 0.0));

        return binaryClassInstanceArray;
    }
    //endregion ================ OTHER METHODS ================

    public static class SGBT extends AbstractMOAObject {
            private static final long serialVersionUID = 1L;

            // region ================ SGBT OPTIONS ================
            public IntOption numberOfboostingIterations = new IntOption("numberOfboostingIterations", 's',
                    "The number of boosting iterations.", 100, 1, Integer.MAX_VALUE);
            public IntOption percentageOfAttributesForEachBoostingIteration = new IntOption("percentageOfAttributesForEachBoostingIteration", 'm',
                    "Percentage of attributes % for each boosting iteration.", 75, Integer.MIN_VALUE, Integer.MAX_VALUE);
            public FloatOption learningRateOption = new FloatOption(
                    "learningRate", 'L', "Learning rate",
                    0.0125, 0, 1.00);
            public FlagOption useOneHotEncoding = new FlagOption("useOneHotEncoding", 'h', "useOneHotEncoding");
            public IntOption multipleIterationByCeilingOfHessianTimesM = new IntOption("multipleIterationByCeilingOfHessianTimesM", 'M',
                    "Multiple training iterations by Ceiling (Hessian * M).", 1, 1, 100);
            public IntOption randomlySkip1SthOfInstancesAtTraining = new IntOption("randomlySkip1SthOfInstancesAtTraining", 'S',
                    "Randomly skipp 1/S th of instances at training (S=1: No Skip, use all instances for training).", 1, 1, Integer.MAX_VALUE);
            public FlagOption useSquaredLossForClassification = new FlagOption("useSquaredLossForClassification", 'K', "Use Squared Loss for Classification.");
            // endregion ================ SGBT OPTIONS ================

            // region ================ SGBT VARIABLES ================
            protected ArrayList<BoostingCommittee> booster;
            private int committeeSize;
            protected ArrayList<ArrayList<Integer>> subspaces;
            protected ArrayList<ArrayList<Integer>> subSpacesForEachBoostingIteration;
            protected Objective mObjective;
            private long instancesSeenAtTrain;
            private Classifier baseLearner = null;
            private Random classifierRandom = null;
            // endregion ================ SGBT VARIABLES ================

            // region ================ SGBT METHODS ================
            public SGBT(Classifier baseLearner, Random classifierRandom) {
                this.classifierRandom = classifierRandom;
                this.baseLearner = baseLearner;
            }
            public void trainOnInstance(Instance inst) {
                if ((this.instancesSeenAtTrain % 1000) == 0) {
                    System.gc();
                }
                if ((this.randomlySkip1SthOfInstancesAtTraining.getValue() > 1) && (this.classifierRandom.nextInt(this.randomlySkip1SthOfInstancesAtTraining.getValue()) == 0)) {
                    // skip training
                    return;
                }
                trainBoosterUsingSoftmaxCrossEntropyLoss(inst);
            }

            @Override
            public void getDescription(StringBuilder sb, int indent) {

            }
            @Override
            public int measureByteSize() {
                long b = 0;
                // get shallow size of this
        //        b = SizeOf.sizeOf(this);
        //        if (booster != null) {
        //            long[] byteSize = new long[booster.size()];
        //            // get deep size of each item
        //            if (booster.size() == 1) {
        //                IntStream.range(0, booster.size())
        //                        .forEach(i -> byteSize[i] = booster.get(i).measureByteSize());
        //            }else{
        //                IntStream.range(0, booster.size())
        //                        .parallel()
        //                        .forEach(i -> byteSize[i] = booster.get(i).measureByteSize());
        //            }
        //            for (int i = 0; i < booster.size(); i++) {
        //                b += byteSize[i];
        //            }
        //        }
                return (int) b;
            }
            public void initEnsemble(Instance inst) {
                Attribute target = inst.classAttribute();

                if (booster == null) {
                    booster = new ArrayList<>();
                } else {
                    while (!booster.isEmpty())
                        booster.remove(0);
                }

                if (target.isNominal()) {
                    committeeSize = target.numValues() - 1;
                    if (useSquaredLossForClassification.isSet()) {
                        mObjective = new SquaredError();
                    } else {
                        mObjective = new SoftmaxCrossEntropy();
                    }
                } else {
                    mObjective = new SquaredError();
                    committeeSize = 1;
                }

                for (int i = 0; i < numberOfboostingIterations.getValue(); i++) {
                    booster.add(new BoostingCommittee(this.baseLearner, committeeSize));
                }

//                TODO: Assumes class attribute as the last one. We could get into trouble if it is not the case
                // #1 Select the size of k, it depends on 2 parameters (subspaceSizeOption and subspaceModeOption).
                int k = this.percentageOfAttributesForEachBoostingIteration.getValue();
                int n = inst.numAttributes() - 1; // Ignore the class label by subtracting 1

                double percent = k < 0 ? (100 + k) / 100.0 : k / 100.0;
                k = (int) Math.round(n * percent);

                if (Math.round(n * percent) < 2)
                    k = (int) Math.round(n * percent) + 1;

                // k is negative, use size(features) + -k
                if (k < 0)
                    k = n + k;

                // #2 generate the subspaces
                if (k != 0 && k < n) {
                    // For low dimensionality it is better to avoid more than 1 classifier with the same subspaces,
                    // thus we generate all possible combinations of subsets of features and select without replacement.
                    // n is the total number of features and k is the actual size of the subspaces.
                    if (n <= 20 || k < 2) {
                        if (k == 1 && inst.numAttributes() > 2)
                            k = 2;
                        // Generate all possible combinations of size k
                        this.subspaces = StreamingRandomPatches.allKCombinations(k, n);
                        for (int i = 0; this.subspaces.size() < this.numberOfboostingIterations.getValue(); ++i) {
                            i = i == this.subspaces.size() ? 0 : i;
                            ArrayList<Integer> copiedSubspace = new ArrayList<>(this.subspaces.get(i));
                            this.subspaces.add(copiedSubspace);
                        }
                    }
                    // For high dimensionality we can't generate all combinations as it is too expensive (memory).
                    // On top of that, the chance of repeating a subspace is lower, so we can just randomly generate
                    // subspaces without worrying about repetitions.
                    else {
                        this.subspaces = StreamingRandomPatches.localRandomKCombinations(k, n,
                                this.numberOfboostingIterations.getValue(), this.classifierRandom);
                    }
                } else if (k == n) {
                    this.subspaces = StreamingRandomPatches.localRandomKCombinations(k, n,
                            this.numberOfboostingIterations.getValue(), this.classifierRandom);
                }

                int[] subSpaceIndexes = this.classifierRandom.ints(0, subspaces.size()).distinct().limit(numberOfboostingIterations.getValue()).toArray();
                subSpacesForEachBoostingIteration = new ArrayList<>();
                for (int i = 0; i < numberOfboostingIterations.getValue(); i++) {
                    subSpacesForEachBoostingIteration.add(this.subspaces.get(subSpaceIndexes[i]));
                }
            }

            public void trainBoosterUsingSoftmaxCrossEntropyLoss(Instance inst) {
                instancesSeenAtTrain++;
                double[] groundTruth;
                if (inst.classAttribute().isNominal()) {
                    groundTruth = new double[inst.numClasses()];
                    groundTruth[(int) inst.classValue()] = 1.0;
                } else {
                    groundTruth = new double[1];
                    groundTruth[0] = inst.classValue();
                }

                if (booster == null) {
                    initEnsemble(inst);
                }
                // get initial score, this is 0.0 for all the trees in the committee
                DoubleVector rawScore = new DoubleVector(getScoresWhenNullTree(committeeSize));

                for (int m = 0; m < booster.size(); m++) {
                    Instance subInstance;
                    // compute Derivatives (g and h) using y and sum up raw scores, for all the trees in the committee
                    // at m th iteration, gets the adjustment by the m th committee considering all the previous adjustments
                    GradHess[] gradHess = mObjective.computeDerivatives(groundTruth, rawScore.getArrayRef(), false, false);
                    // create a sub instance from the inst
                    subInstance = getSubInstance(inst, 1.0, subSpacesForEachBoostingIteration.get(m), true, -1, useOneHotEncoding.isSet());
                    //create sub instance for each committee member
                    Instance[] subInstArray = new Instance[gradHess.length];
                    if (gradHess.length == 1) {
                        subInstArray[0] = subInstance;
                    } else {
                        IntStream.range(0, gradHess.length)
                                .forEach(i -> subInstArray[i] = subInstance.copy());
                    }
        //            by default weight is set 1 at instance creation
        //            set each sub instance pseudo label to gradient/hessian
                    IntStream.range(0, subInstArray.length)
        //                        .parallel()
                            .forEach(i -> subInstArray[i].setClassValue(gradHess[i].gradient / gradHess[i].hessian));
                    // train each member of the committee using sub instance with relevant weight and pseudo-label
                    booster.get(m).trainOnInstanceImpl(subInstArray, multipleIterationByCeilingOfHessianTimesM.getValue(), gradHess);
                    // get the score from the committee for current subInstance (here we use subInstance for useWeightedInstances==true, as we do not need the label)
                    DoubleVector currentScore = new DoubleVector(booster.get(m).getScoresForInstance(subInstance));
                    // scale the score by learning rate
                    double learningRate = learningRateOption.getValue();
                    currentScore.scaleValues(learningRate);
                    // add the current sore to existing raw score
                    rawScore.addValues(currentScore);
                }
            }
            public DoubleVector getRawScoreForInstance(Instance inst) {
                DoubleVector rawScore = new DoubleVector(getScoresWhenNullTree(committeeSize));

                double[][] s = new double[booster.size()][];
                if (booster.size() == 1) {
                    s[0] = getScoreFromSubInstance(inst, subSpacesForEachBoostingIteration.get(0), true, booster.get(0), useOneHotEncoding.isSet());
                } else {
                        IntStream.range(0, booster.size())
                                .parallel()
                                .forEach(m -> s[m] = getScoreFromSubInstance(inst, subSpacesForEachBoostingIteration.get(m), true, booster.get(m), useOneHotEncoding.isSet()));
                }
                for (int i = 0; i < booster.size(); i++) {
                    rawScore.addValues(s[i]);
                }
                return rawScore;
            }
            public double[] getVotesForInstance(Instance inst) {
                double[] prediction = null;
                if (booster == null) {
                    initEnsemble(inst);
                }

                if (inst.classAttribute().isNominal()) {
                    prediction = mObjective.transfer(getRawScoreForInstance(inst).getArrayCopy());
                } else {
                    prediction = getRawScoreForInstance(inst).getArrayCopy();
                }
                return prediction;
            }
            // endregion ================ SGBT METHODS ================

            // region ================ SGBT INNER CLASSES ================

            public static class GradHess implements Serializable {
                private static final long serialVersionUID = 1L;
                public double gradient;
                public double hessian;
                public GradHess(double gradient, double hessian) {
                    this.gradient = gradient;
                    this.hessian = hessian;
                }
            }

            //  --------------------------------------------------------------------------------------------------------------------
            public abstract static class Objective {
                public abstract GradHess[] computeDerivatives(double[] groundTruth, double[] raw, boolean computeNegativeResidual, boolean clipPredictions);
                public double[] transfer(double[] raw) {
                    return raw;
                }
            }
            //  --------------------------------------------------------------------------------------------------------------------

            public static class SoftmaxCrossEntropy extends Objective implements Serializable {
                private static final long serialVersionUID = 1L;

                @Override
                public double[] transfer(double[] raw) {
                    double[] result = new double[raw.length + 1];

                    System.arraycopy(raw, 0, result, 0, raw.length);

                    double max = Double.NEGATIVE_INFINITY;
                    double sum = 0.0;

                    for (double v : result) {
                        max = Math.max(max, v);
                    }

                    for (int i = 0; i < result.length; i++) {
                        result[i] = Math.exp(result[i] - max);
                        sum += result[i];
                    }

                    for (int i = 0; i < result.length; i++) {
                        result[i] /= sum;
                    }
                    return result;
                }

                @Override
                public GradHess[] computeDerivatives(double[] groundTruth, double[] raw, boolean computeNegativeResidual, boolean clipPredictions) {
                    GradHess[] result = new GradHess[raw.length];
                    double[] predictions = transfer(raw);

                    for (int i = 0; i < result.length; i++) {
                        if (clipPredictions) {
                            predictions[i] = Math.max(predictions[i], 0.0001);
                            predictions[i] = Math.min(predictions[i], 0.9999);
                        }
                        if (computeNegativeResidual) {
                            result[i] = new GradHess(predictions[i] - groundTruth[i], predictions[i] * (1.0 - predictions[i]));
                        } else {
                            result[i] = new GradHess(groundTruth[i] - predictions[i], predictions[i] * (1.0 - predictions[i]));
                        }
                    }
                    return result;
                }
            }

            //  --------------------------------------------------------------------------------------------------------------------
            public static class SquaredError extends Objective implements Serializable {
                private static final long serialVersionUID = 1L;

                @Override
                public GradHess[] computeDerivatives(double[] groundTruth, double[] raw, boolean computeNegativeResidual, boolean clipPredictions) {
                    GradHess[] result = new GradHess[raw.length];

                    for (int i = 0; i < result.length; i++) {
                        if (computeNegativeResidual) {
                            result[i] = new GradHess(raw[i] - groundTruth[i], 1.0);
                        } else {
                            result[i] = new GradHess(groundTruth[i] - raw[i], 1.0);
                        }
                    }
                    return result;
                }
            }

            //  --------------------------------------------------------------------------------------------------------------------
            static class TreeCommittee implements Serializable {
                private static final long serialVersionUID = 1L;
                protected Classifier[] treesCommittee;
                public TreeCommittee(Classifier baseLearner, int numTrees) {
                    treesCommittee = new Classifier[numTrees];
                    for (int i = 0; i < treesCommittee.length; i++) {
                        treesCommittee[i] = baseLearner.copy();
                    }
                }
                void modelUpdate(Classifier c, Instance inst, int multipleIterationByHessianCeiling, double hessian) {
                    double trainTimes = multipleIterationByHessianCeiling > 1 ? Math.ceil(hessian * multipleIterationByHessianCeiling) : 1.0;
                    for (int i = 0; i < (int) trainTimes; i++) {
                        c.trainOnInstance(inst);
                    }
                }
                public void update(Instance[] instArray, int multipleIterationByHessianCeiling, GradHess[] gradHess) {
                    if (treesCommittee.length == 1) {
                        IntStream.range(0, treesCommittee.length)
                                .forEach(i -> modelUpdate(treesCommittee[i], instArray[i], multipleIterationByHessianCeiling, gradHess[i].hessian));
                    } else {
                        IntStream.range(0, treesCommittee.length)
                                .parallel()
                                .forEach(i -> modelUpdate(treesCommittee[i], instArray[i], multipleIterationByHessianCeiling, gradHess[i].hessian));
                    }

                }
                public void update(Instance inst) {
                    if (treesCommittee.length == 1) {
                        IntStream.range(0, treesCommittee.length)
                                .forEach(i -> treesCommittee[i].trainOnInstance(inst));
                    } else {
                        IntStream.range(0, treesCommittee.length)
                                .parallel()
                                .forEach(i -> treesCommittee[i].trainOnInstance(inst));
                    }
                }
                public double[] predict(Instance inst) {
                    double[] v = new double[treesCommittee.length];
                    if (treesCommittee.length == 1) {
                        IntStream.range(0, treesCommittee.length)
                                .forEach(i -> v[i] = treesCommittee[i].getVotesForInstance(inst)[0]);
                    } else {
                        IntStream.range(0, treesCommittee.length)
                                .parallel()
                                .forEach(i -> v[i] = treesCommittee[i].getVotesForInstance(inst)[0]);
                    }
                    return v;
                }
            }

            //  --------------------------------------------------------------------------------------------------------------------
            public static class BoostingCommittee implements Serializable {
                private static final long serialVersionUID = 1L;
                private TreeCommittee treeCommittee = null;
                protected int mInstances = 0;
                private Classifier baseLearner = null;
                public int committeeSize = 0;

                public BoostingCommittee(Classifier baseLearner, int committeeSize) {
                    this.baseLearner = baseLearner;
                    this.committeeSize = committeeSize;
                }

                //        @Override
                public int measureByteSize() {
                    long b = 0;
    //            // get shallow size of this
    //            b = SizeOf.sizeOf(this);
    //            if ((treeCommittee!= null) && (treeCommittee.treesCommittee != null)){
    //
    //                long[] byteSize = new long[treeCommittee.treesCommittee.length];
    //                // get deep size of each item
    //                if (treeCommittee.treesCommittee.length == 1) {
    //                    IntStream.range(0, treeCommittee.treesCommittee.length)
    //                            .forEach(i -> byteSize[i] = treeCommittee.treesCommittee[i].measureByteSize());
    //                }else{
    //                    IntStream.range(0, treeCommittee.treesCommittee.length)
    //                            .parallel()
    //                            .forEach(i -> byteSize[i] = treeCommittee.treesCommittee[i].measureByteSize());
    //                }
    //                for (int i = 0; i < treeCommittee.treesCommittee.length; i++) {
    //                    b += byteSize[i];
    //                }
    //            }
                    return (int) b;
                }

                private TreeCommittee createTrees(Classifier baseLearner, int numOutputs) {
                    return new TreeCommittee(baseLearner, numOutputs);
                }

                public void trainOnInstanceImpl(Instance[] instances, int multipleIterationByHessianCeiling, GradHess[] gradHess) {
                    mInstances++;
                    if (treeCommittee == null) {
                        treeCommittee = createTrees(this.baseLearner.copy(), committeeSize);
                    }
                    treeCommittee.update(instances, multipleIterationByHessianCeiling, gradHess);
                }

                public void trainOnInstanceImpl(Instance inst) {
                    mInstances++;

                    if (treeCommittee == null) {
                        treeCommittee = createTrees(this.baseLearner.copy(), committeeSize);
                    }
                    treeCommittee.update(inst);
                }

                public double[] getScoresForInstance(Instance inst) {
                    if (treeCommittee == null) {
                        return getScoresWhenNullTree(committeeSize);
                    }
                    return treeCommittee.predict(inst);
                }
            }
            // endregion ================ SGBT INNER CLASSES ================
    }
}
