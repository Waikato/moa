/*
 *    IADEM2.java
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
package moa.classifiers.trees;

import java.util.logging.Level;
import java.util.logging.Logger;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.trees.iadem2.IADEM2cTree;
import moa.classifiers.trees.iademutils.IademException;
import moa.classifiers.trees.iademutils.IademSplitMeasure;
import moa.classifiers.trees.iademutils.IademCommonProcedures;
import moa.core.DoubleVector;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.Measurement;
import moa.options.ClassOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;

/**
 * J. del Campo, G. Ramos, J. Gama, R. Morales, Improving the performance 
 * of an incremental algorithm driven by error margins. 
 * Intelligent Data Analysis 12 (2008) 305–318.
 * 
 */

public class IADEM2 extends AbstractClassifier {

    private static final long serialVersionUID = 1L;
    public ClassOption numericEstimatorOption = new ClassOption("numericEstimator",
            'z', "Numeric estimator to use.", IademNumericAttributeObserver.class,
            "IademGaussianNumericAttributeClassObserver");
    public IntOption gracePeriodOption = new IntOption("gracePeriod", 'n',
            "The number of instances the tree should observe between splitting attempts.",
            200, 1, Integer.MAX_VALUE);
    public MultiChoiceOption splitCriterionOption = new MultiChoiceOption("splitCriterion", 's',
            "Split criterion to use.", new String[]{
                "entropy", "entropy_logVar", "entropy_logVar+Peso", "entropy_Peso", "beta1", "gamma1", "beta2", "gamma2", "beta4", "gamma4"}, new String[]{
                "entropy", "entropy_logVar", "entropy_logVar+Peso", "entropy_Peso", "beta1", "gamma1", "beta2", "gamma2", "beta4", "gamma4"}, 0);
    public FloatOption splitConfidenceOption = new FloatOption("splitConfidence", 'c',
            "The allowable error in split decision, values closer to 0 will take longer to decide.",
            0.01, 0.0, 1.0);
    public MultiChoiceOption splitTestsOption = new MultiChoiceOption("splitChoice", 'i',
            "Methods for splitting leaf nodes.",
            new String[]{
                "onlyBinarySplit", "onlyMultiwaySplit", "bestSplit"}, new String[]{
                "onlyBinary", "onlyMultiway", "bestSplit"},
            2);

    public MultiChoiceOption leafPredictionOption = new MultiChoiceOption("leafPrediction", 'b',
            "Leaf prediction to use.", new String[]{
                "MC", "NB", "NBKirkby", "WeightedVote"},
            new String[]{"MC: Majority class.",
                "NB: Naïve Bayes.",
                "NBKirkby.",
                "WeightedVote: Weighted vote between NB and MC."},
            1);
    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'd',
            "Drift detection method to use.", AbstractChangeDetector.class, "HDDM_A_Test");

    // fixed option...
    public FloatOption attributeDiferentiation = new FloatOption("attritubeDiferentiation", 'a',
            "Attribute differenciation",
            0.1, 0.0, 1.0);
    
    public int naiveBayesLimit = 0;
    public double percentInCommon = 0.75;
    protected IADEM2cTree tree;
    protected IademSplitMeasure splitMeasure;
    protected int instancesProcessed;

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public void resetLearningImpl() {
        try {
            this.splitMeasure = new IademSplitMeasure(this.splitCriterionOption.getChosenLabel());
            IademCommonProcedures.setConfidence(this.splitConfidenceOption.getValue());
            if (this.modelContext != null) {
                IademNumericAttributeObserver obsContinuos = newNumericClassObserver();
                this.tree = new IADEM2cTree(this.modelContext,
                        this.attributeDiferentiation.getValue(),
                        this.splitMeasure,
                        this.leafPredictionOption.getChosenIndex(),
                        this.naiveBayesLimit,
                        this.percentInCommon,
                        obsContinuos,
                        (int) obsContinuos.getMaxOfValues(),
                        this.splitTestsOption.getChosenIndex() == 1,
                        this.splitTestsOption.getChosenIndex() == 0,
                        (AbstractChangeDetector) ((AbstractChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy(),
                        this.gracePeriodOption.getValue());
            } else {
                this.tree = null;
            }

        } catch (IademException ex) {
            Logger.getLogger(IADEM2.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.instancesProcessed = 0;
    }

    @Override
    public void setModelContext(InstancesHeader ih) {
        super.setModelContext(ih);
        IademNumericAttributeObserver obsContinuos = newNumericClassObserver();
        this.tree = new IADEM2cTree(ih,
                this.attributeDiferentiation.getValue(),
                this.splitMeasure,
                this.leafPredictionOption.getChosenIndex(),
                this.naiveBayesLimit,
                this.percentInCommon,
                obsContinuos,
                (int) obsContinuos.getMaxOfValues(),
                this.splitTestsOption.getChosenIndex() == 1,
                this.splitTestsOption.getChosenIndex() == 0,
                (AbstractChangeDetector) ((AbstractChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy(),
                this.gracePeriodOption.getValue());
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        try {
            if (this.tree == null) {
                IademNumericAttributeObserver obsContinuos = newNumericClassObserver();
                this.tree = new IADEM2cTree(this.modelContext,
                        this.attributeDiferentiation.getValue(),
                        this.splitMeasure,
                        this.leafPredictionOption.getChosenIndex(),
                        this.naiveBayesLimit,
                        this.percentInCommon,
                        obsContinuos,
                        (int) obsContinuos.getMaxOfValues(),
                        this.splitTestsOption.getChosenIndex() == 1,
                        this.splitTestsOption.getChosenIndex() == 0,
                        (AbstractChangeDetector) ((AbstractChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy(),
                        this.gracePeriodOption.getValue());
            }
            this.instancesProcessed++;
            this.tree.learnFromInstance(inst);
        } catch (IademException ex) {
            Logger.getLogger(IADEM2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected IademNumericAttributeObserver newNumericClassObserver() {
        IademNumericAttributeObserver numericClassObserver = (IademNumericAttributeObserver) getPreparedClassOption(this.numericEstimatorOption);
        return (IademNumericAttributeObserver) numericClassObserver.copy();
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{
            new Measurement("tree size (nodes)", this.tree.getNumberOfNodes()),
            new Measurement("tree size (leaves)", this.tree.getNumberOfLeaves()),
            new Measurement("instances seen", this.tree.getNumberOfInstancesProcessed())
        };
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (this.tree == null) {
            DoubleVector classVotes = new DoubleVector();
            double estimation = 1.0 / inst.classAttribute().numValues();
            for (int i = 0; i < inst.classAttribute().numValues(); i++) {
                classVotes.addToValue(i, estimation);
            }
            return classVotes.getArrayCopy();

        }
        DoubleVector predicciones = new DoubleVector(this.tree.getClassVotes(inst));
        return predicciones.getArrayCopy();

    }
}
