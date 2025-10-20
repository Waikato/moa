/*
 *    AdaptiveRandomForest.java
 *
 *    @author Heitor Murilo Gomes (hmugomes at gmail dot com)
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
 */
package moa.classifiers.semisupervised;

import com.github.javacliparser.*;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.meta.StreamingRandomPatches;
import moa.core.*;
import moa.evaluation.BasicClassificationPerformanceEvaluator;
import moa.evaluation.WindowClassificationPerformanceEvaluator;
import moa.options.ClassOption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SLEADE extends AbstractClassifier implements SemiSupervisedLearner, MultiClassClassifier {

    public ClassOption baseEnsembleOption = new ClassOption("baseEnsemble", 'l',
            "Ensemble to train on instances.", StreamingRandomPatches.class, "StreamingRandomPatches");

    public MultiChoiceOption confidenceStrategyOption = new MultiChoiceOption("confidenceStrategy", 'b',
            "Defines the strategy to calculate the confidence. SumVotes = uses votes directly, ArgMax = sets 1 to the argmax of each vote array",
            new String[]{"Sum", "ArgMax"},
            new String[]{"Sum", "ArgMax"}, 1);

    public FlagOption enableRandomThresholdOption = new FlagOption("enableRandomThreshold", 'q',
            "If set, the minConfidenceThreshold is ignored and a random value is used.");

    public MultiChoiceOption autoWeightShrinkageOption = new MultiChoiceOption("autoWeightShrinkage", 'e',
            "Different strategies for automatically setting the weight shrinkage (ws) value",
            new String[]{"Constant", "LabeledDivTotal", "LabeledNoWarmupDivTotal"},
            new String[]{"Constant", "LabeledDivTotal", "LabeledNoWarmupDivTotal"}, 2);

    public MultiChoiceOption SSLStrategyOption = new MultiChoiceOption("SSLStrategy", 'p',
            "Defines the SSL strategy.",
            new String[]{"PseudoLabelAll", "PseudoLabelCheckConfidence"},
            new String[]{"PseudoLabelAll", "PseudoLabelCheckConfidence"}, 1);

    public FloatOption SSL_minConfidenceOption = new FloatOption("SSL_minConfidence", 'm',
            "Minimum confidence to use unlabeled instance to train.", 0.00, 0.0, 1.0);

    public MultiChoiceOption weightFunctionOption = new MultiChoiceOption("weightFunction", 'w',
            "Defines the function to weight pseudo-labeled instances. ",
            new String[]{"Constant1", "Confidence", "ConfidenceWeightShrinkage", "UnsupervisedDetectionWeightShrinkage"},
            new String[]{"Constant1", "Confidence", "ConfidenceWeightShrinkage", "UnsupervisedDetectionWeightShrinkage"}, 2);

    public MultiChoiceOption pairingFunctionOption = new MultiChoiceOption("pairingFunction", 't',
            "Defines the function to pair learners, default is minimum Kappa",
            new String[]{"MinKappa", "Random", "MajorityTrainsMinority"},
            new String[]{"MinKappa", "Random", "MajorityTrainsMinority"}, 2);

    public FloatOption SSL_weightShrinkageOption = new FloatOption("SSL_weightShrinkageOption", 'n',
            "The pseudo-labeled instances will be weighted according to instance weight * 1/WS.",
            100.0, 0.0, Integer.MAX_VALUE);

    public FlagOption useUnsupervisedDriftDetectionOption = new FlagOption("useUnsupervisedDriftDetection", 's',
            "Whether or not to use the unsupervised drift detection reaction strategy.");

    public ClassOption studentLearnerForUnsupervisedDriftDetectionOption =
            new ClassOption("studentLearnerForUnsupervisedDriftDetection", 'g',
                    "Student to mimic the ensemble. It is used for unsupervised drift detection.", Classifier.class,
                    "trees.HoeffdingTree -g 50 -c 0.01");

    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'x',
            "Change detector for drifts and its parameters", ChangeDetector.class,
            "ADWINChangeDetector -a 1.0E-5");

    public IntOption unsupervisedDetectionWeightWindowOption = new IntOption("unsupervisedDetectionWeightWindow", 'z',
            "The labeledInstancesBuffer length of the sigmoid functions for the unsupervised drift detection pseudo-labeling weighting.",
            20, 0, Integer.MAX_VALUE);

    public IntOption labeledWindowLimitOption = new IntOption( "labeledWindowLimit", 'j',
            "The maximum number of labeled instances to store in the sliding window used for quick start training learners",
            100, 0, Integer.MAX_VALUE);

    public FlagOption debugEnsembleDiversityOption = new FlagOption("debugEnsembleDiversity", 'v',
            "Calculates the kappa statistic among ensemble members' predictions");

    public FileOption debugOutputFileOption = new FileOption("debugOutputFile", 'o',
            "File to append debug information.", null, "csv", true);

    public FileOption debugOutputDriftFileOption = new FileOption("debugOutputDriftFile", 'c',
            "File to append drift debug information.", null, "csv", true);

    public FileOption debugOutputConfidencePredictionsFileOption = new FileOption(
            "debugOutputConfidencePredictionsFile", 'f',
            "File to append confidences and predictions information.", null, "csv", true);

    public IntOption debugFrequencyOption = new IntOption("debugFrequency", 'i',
            "The labeledInstancesBuffer length of the sigmoid functions for the unsupervised drift detection pseudo-labeling weighting.", 1000, 0, Integer.MAX_VALUE);

    public FlagOption debugPerfectPseudoLabelingOption = new FlagOption("debugPerfectPseudoLabeling", 'd',
            "CAUTION: THIS OVERRIDES THE PSEUDO-LABELING STRATEGY AND ALWAYS CHOOSE THE CORRECT LABEL AS THE PSEUDO-LABEL, FOR DEBUG ONLY!");

    public StringOption gtDriftLocationOption = new StringOption("gtDriftLocation", 'a',
            "If set, it is used to calculate drift detection accuracy. format: 'dd_location1,dd_location2,...,dd_locationX", "");

    StreamingRandomPatches baseEnsemble;
    Classifier detectionStudent;
    protected ChangeDetector driftDetectionMethod;
    protected C[][] pairsOutputsKappa;

    /** Sliding window of labeled instances **/
    protected Instances labeledInstancesBuffer;

    /** TestThenTrain accuracy of each member. NOT FOR DEBUG. **/
    /** This is only called during supervised training, thus it is a fair estimation of the predictive performance **/
    private BasicClassificationPerformanceEvaluator[] evaluatorTestThenTrainEnsembleMembers;


    /** Only used when executing with a warmup training problem setting. **/
    protected long initialWarmupTrainingCounter;

    /** autoWeightShrinkageOption **/
    public static final int AUTO_WEIGHT_SHRINKAGE_CONSTANT = 0; // use the default constant value from SSL_weightShrinkageOption
    public static final int AUTO_WEIGHT_SHRINKAGE_LABELED_DIV_TOTAL = 1;
    public static final int AUTO_WEIGHT_SHRINKAGE_LABELED_IGNORE_WARMUP_DIV_TOTAL = 2;

    /** SSL Strategy: if pseudo-labels are used without doing any checks or if confidence is used **/
    public static final int SSL_STRATEGY_NO_CHECKS = 0;
    public static final int SSL_STRATEGY_CHECK_CONFIDENCE_AND_OTHERS = 1;

    /** Pseudo-label weighting function **/
    public static final int SSL_WEIGHT_CONSTANT_1 = 0;
    public static final int SSL_WEIGHT_CONFIDENCE = 1;
    public static final int SSL_WEIGHT_CONFIDENCE_WS = 2;
    public static final int SSL_WEIGHT_UNSUPERVISED_DETECTION_WEIGHT_SHRINKAGE = 3;

    /** Pairing function: default is majority trains minority **/
    public static final int SSL_PAIRING_MIN_KAPPA = 0;
    public static final int SSL_PAIRING_RANDOM = 1;
    public static final int SSL_PAIRING_MAJORITY_TRAINS_MINORITY = 2;

    /** Confidence calculation strategy for the majority trains minority **/
    public static final int SSL_CONFIDENCE_STRATEGY_SUM = 0;
    public static final int SSL_CONFIDENCE_STRATEGY_ARGMAX = 1;

    private boolean shouldUpdateKappaDiversity;

    /*****************************************************************************/
    /*********************************** DEBUG ***********************************/
    /*****************************************************************************/
    /** Debug Statistics **/
    protected long instancesSeen; // count only labeled (updated only on trainOnInstance()
    protected long allInstancesSeen; // Unlabeled and labeled
    protected long instancesPseudoLabeled;
    protected long instancesCorrectPseudoLabeled;
    protected double currentAverageKappa;
    protected double currentMinKappa;
    protected double currentMaxKappa;
    protected int numberOfUnsupervisedDriftsDetected;
    protected int currentNumberOfUnsupervisedDriftsDetected;
    protected boolean gtDriftCurrentDebugPeriod;
    protected long lastDriftDetectedAt; // last drift detected using the instance counter that only considers labeled data
    protected long lastDriftDetectedAtUnlabelled; // similar to the previous one, but using all instances (including unlabeled).

    protected long learnerLabeledSeen[];
    protected long learnerPseudoLabeledSeen[];
    protected long learnerPseudoLabeledCorrectSeen[];

    /** IMPORTANT: These evaluators are used for debugging purposes only, they assume immediate access to labels **/
    private WindowClassificationPerformanceEvaluator evaluatorWindowEnsembleDebug;
    private WindowClassificationPerformanceEvaluator[] evaluatorWindowEnsembleMembersDebug;
    private BasicClassificationPerformanceEvaluator evaluatorTestThenTrainEnsembleDebug;

    PrintStream outputAccuracyStatisticsDebugStream = null;
    PrintStream outputDriftDetectionDebugStream = null;
    PrintStream outputConfidencePredictionDebugStream = null;
    /** Specify the location of known concept drifts (e.g. synthetic streams) **/
    int[] gtDriftLocations = null;
    /*****************************************************************************/
    /*********************************** DEBUG ***********************************/
    /*****************************************************************************/

    @Override
    public void setModelContext(InstancesHeader context) {
        try {
            this.labeledInstancesBuffer = new Instances(context,0); //new StringReader(context.toString())
            this.labeledInstancesBuffer.setClassIndex(context.classIndex());
        } catch(Exception e) {
            System.err.println("Error: no Model Context available.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /***
     * Basic strategy to react to concept drifts.
     *
     * @param instance
     */
    protected void reactToDrift(Instance instance) {
        double sum = 0.0, mean = 0.0;
        int minIndex = 0, maxIndex = 0;

        /** Use the estimated accuracy (based only on the labeled instances) for each member of the ensemble **/
        for(int i = 0; i < this.evaluatorTestThenTrainEnsembleMembers.length ; ++i) {
            double acc = this.evaluatorTestThenTrainEnsembleMembers[i].getPerformanceMeasurements()[1].getValue();
            if(this.evaluatorTestThenTrainEnsembleMembers[minIndex].getPerformanceMeasurements()[1].getValue() > acc)
                minIndex = i;
            if(this.evaluatorTestThenTrainEnsembleMembers[maxIndex].getPerformanceMeasurements()[1].getValue() <= acc)
                maxIndex = i;
            sum += acc;
        }
        mean = sum / this.evaluatorTestThenTrainEnsembleMembers.length;

        StreamingRandomPatches.StreamingRandomPatchesClassifier[] ensemble = this.baseEnsemble.getEnsembleMembers();
        ArrayList<Integer> resetIndexes = new ArrayList<>();

        for(int i = 0 ; i < ensemble.length ; ++i) {
            if (this.evaluatorTestThenTrainEnsembleMembers[i].getPerformanceMeasurements()[1].getValue() < mean) {
                ensemble[i].reset(instance, this.instancesSeen, this.classifierRandom);
                resetIndexes.add(i);
                /** Debug **/
                if(outputConfidencePredictionDebugStream != null) {
                    this.learnerLabeledSeen[i] = 0;
                    this.learnerPseudoLabeledSeen[i] = 0;
                    this.learnerPseudoLabeledCorrectSeen[i] = 0;
                }
                /** Debug **/
            }
        }

        // Train the newly added learners on the buffered labeled instances
        for(int i = 0 ; i < resetIndexes.size() ; ++i) {
            for(int j = 0 ; j < this.labeledInstancesBuffer.numInstances() ; ++j) {
                ensemble[resetIndexes.get(i)].trainOnInstance(this.labeledInstancesBuffer.get(j), 1,
                        this.instancesSeen, this.classifierRandom, true);
            }
        }
    }

    private boolean trainAndUpdateStudentDetector(Instance instance) {
        double[] ensembleVotes = this.baseEnsemble.getVotesForInstance(instance);
        int ensemblePrediction = Utils.maxIndex(ensembleVotes);

        Instance instanceWithEnsemblePrediction = instance.copy();
        instanceWithEnsemblePrediction.setClassValue(ensemblePrediction);

        this.detectionStudent.trainOnInstance(instanceWithEnsemblePrediction);
        boolean correctlyClassifies = this.detectionStudent.correctlyClassifies(instanceWithEnsemblePrediction);

        /*********** drift detection ***********/
        // Update the DRIFT detection method
        this.driftDetectionMethod.input(correctlyClassifies ? 0 : 1);
        // Check if there was a change
        if (this.driftDetectionMethod.getChange()) {
            this.numberOfUnsupervisedDriftsDetected++;
            // There was a change, this model must be reset
            // this.reset(instance, instancesSeen, random);
//            if(this.outputAccuracyStatisticsDebugStream != null)
//                this.outputAccuracyStatisticsDebugStream.println(this.allInstancesSeen + ", " + this.instancesSeen + ", "+ this.evaluatorWindowEnsembleDebug.getPerformanceMeasurements()[1].getValue() +", SLEAD");

//            if(this.outputDriftDetectionDebugStream != null)
//                this.outputDriftDetectionDebugStream.println(this.allInstancesSeen + ", " + this.instancesSeen + "," + this.numberOfUnsupervisedDriftsDetected);

            // should change order?
            this.detectionStudent = (Classifier) getPreparedClassOption(this.studentLearnerForUnsupervisedDriftDetectionOption);
            this.detectionStudent.resetLearning();

            this.driftDetectionMethod = (ChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption);
            this.driftDetectionMethod.resetLearning();

            this.lastDriftDetectedAt = this.instancesSeen;
            this.lastDriftDetectedAtUnlabelled = this.allInstancesSeen;

            return true;
        }
        return false;
    }

    @Override
    public void resetLearningImpl() {
        this.baseEnsemble = (StreamingRandomPatches) getPreparedClassOption(this.baseEnsembleOption);
        this.baseEnsemble.resetLearning();

        this.detectionStudent = (Classifier) getPreparedClassOption(this.studentLearnerForUnsupervisedDriftDetectionOption);
        this.detectionStudent.resetLearning();
        this.lastDriftDetectedAt = 0;
        this.lastDriftDetectedAtUnlabelled = 0;

        this.driftDetectionMethod = (ChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption);
        this.driftDetectionMethod.resetLearning();

        this.instancesSeen = 0;
        this.allInstancesSeen = 0;
        this.instancesCorrectPseudoLabeled = 0;
        this.instancesPseudoLabeled = 0;
        this.currentAverageKappa = 0.0;
        this.currentMaxKappa = -1.0;
        this.currentMinKappa = 1.0;

        // Define whether to calculate the kappa diversity or not. If we are using the pairing function that relies on
        // minKappa, then it must be updated. Another option is to just calculate it to have the ensemble diversity
        // statistics.
        this.shouldUpdateKappaDiversity =
                this.pairingFunctionOption.getChosenIndex() == SSL_PAIRING_MIN_KAPPA ||
                        this.debugEnsembleDiversityOption.isSet();

        this.initialWarmupTrainingCounter = 0;

        this.numberOfUnsupervisedDriftsDetected = 0;
        this.currentNumberOfUnsupervisedDriftsDetected = 0;
        this.gtDriftCurrentDebugPeriod = false;

        this.evaluatorWindowEnsembleDebug = new WindowClassificationPerformanceEvaluator();
        this.evaluatorWindowEnsembleDebug.widthOption.setValue(this.debugFrequencyOption.getValue());
        this.evaluatorWindowEnsembleDebug.reset();

        this.evaluatorTestThenTrainEnsembleDebug = new BasicClassificationPerformanceEvaluator();
        this.evaluatorTestThenTrainEnsembleDebug.reset();

        // Drift detector ground-truth
        String ddStr = this.gtDriftLocationOption.getValue();
        if(ddStr.length() != 0) {
            String[] ddStrArr = ddStr.split(",");
            this.gtDriftLocations = new int[ddStrArr.length];
            for(int i = 0 ; i < ddStrArr.length ; ++i)
                this.gtDriftLocations[i] = Integer.parseInt(ddStrArr[i]);
        }

        // File for debug
        File outputDebugFile = this.debugOutputFileOption.getFile();

        if (outputDebugFile != null) {
            try {
                if (outputDebugFile.exists()) {
                    outputAccuracyStatisticsDebugStream = new PrintStream(
                            new FileOutputStream(outputDebugFile, true), true);
                } else {
                    outputAccuracyStatisticsDebugStream = new PrintStream(
                            new FileOutputStream(outputDebugFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open debug file: " + outputDebugFile, ex);
            }
        }

        // File for drift debug
        File outputDebugDriftFile = this.debugOutputDriftFileOption.getFile();
        if (outputDebugDriftFile != null) {
            try {
                if (outputDebugDriftFile.exists()) {
                    outputDriftDetectionDebugStream = new PrintStream(
                            new FileOutputStream(outputDebugDriftFile, true), true);
                } else {
                    outputDriftDetectionDebugStream = new PrintStream(
                            new FileOutputStream(outputDebugDriftFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open drift debug file: " + outputDebugFile, ex);
            }
        }

        // File for debug confidence
        File outputConfidencePredictionsDebugFile = this.debugOutputConfidencePredictionsFileOption.getFile();
        if (outputConfidencePredictionsDebugFile != null) {
            try {
                if (outputConfidencePredictionsDebugFile.exists()) {
                    outputConfidencePredictionDebugStream = new PrintStream(
                            new FileOutputStream(outputConfidencePredictionsDebugFile, true), true);
                } else {
                    outputConfidencePredictionDebugStream = new PrintStream(
                            new FileOutputStream(outputConfidencePredictionsDebugFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open confidence prediction debug file: " + outputConfidencePredictionsDebugFile, ex);
            }
        }

    }

    private void initDebugMode() {

        if(this.evaluatorWindowEnsembleMembersDebug == null)
            // This is for debug purposes only as it is invoked during getVotesForInstance
            this.evaluatorWindowEnsembleMembersDebug = new WindowClassificationPerformanceEvaluator[this.baseEnsemble.getEnsembleMembers().length];

        // Gambi!
        if(this.outputDriftDetectionDebugStream != null) {
            this.outputDriftDetectionDebugStream.println("#instances,#labeled_instances,#drift_window,#drift_total,gtDrift");
        }

        if(this.outputAccuracyStatisticsDebugStream != null) {
            for (int i = 0; i < this.baseEnsemble.getEnsembleMembers().length; ++i) {
                // nao precisa fazer isso! Os membros do ensemble nao precisam escrever na saida de debug...
//                this.baseEnsemble.getEnsembleMembers()[i].outputDebugStream = this.outputDebugStream;
                this.evaluatorWindowEnsembleMembersDebug[i] = new WindowClassificationPerformanceEvaluator();
                this.evaluatorWindowEnsembleMembersDebug[i].widthOption.setValue(this.debugFrequencyOption.getValue());
            }

            // Even more imaginative code
            this.outputAccuracyStatisticsDebugStream.print("#instances-labeled,#instances,#labeled_instances,windowed_accuracy(w=" + this.debugFrequencyOption.getValue() + "),");

            for (int i = 0; i < this.baseEnsemble.getEnsembleMembers().length; ++i) {
                this.outputAccuracyStatisticsDebugStream.print("TTT_acc(" + this.baseEnsemble.getEnsembleMembers()[i].indexOriginal + "),");
            }

            for (int i = 0; i < this.baseEnsemble.getEnsembleMembers().length; ++i) {
                this.outputAccuracyStatisticsDebugStream.print("win_acc(" + this.baseEnsemble.getEnsembleMembers()[i].indexOriginal + "),");
            }

            for (int i = 0; i < this.baseEnsemble.getEnsembleMembers().length; ++i) {
                this.outputAccuracyStatisticsDebugStream.print("#lab_inst("
                        + this.baseEnsemble.getEnsembleMembers()[i].indexOriginal + "),");
            }
            this.outputAccuracyStatisticsDebugStream.println();
        }

        // To initialize the confidence prediction debug stream
        if(outputConfidencePredictionDebugStream != null) {
            StringBuilder stbPred = new StringBuilder();
            StringBuilder stbConf = new StringBuilder();
            StringBuilder stbTTTacc = new StringBuilder();
            StringBuilder stbWINacc = new StringBuilder();
            StringBuilder stbComplementPred = new StringBuilder();
            StringBuilder stbComplementConf = new StringBuilder();
            StringBuilder stbWeight = new StringBuilder();
            StringBuilder stbPseudoState = new StringBuilder();
            StringBuilder stbLabeledSeen = new StringBuilder();
            StringBuilder stbPseudoLabeledTotal = new StringBuilder();
            StringBuilder stbPseudoLabeledCorrect = new StringBuilder();

            this.learnerLabeledSeen = new long[baseEnsemble.ensembleSizeOption.getValue()];
            this.learnerPseudoLabeledSeen = new long[baseEnsemble.ensembleSizeOption.getValue()];
            this.learnerPseudoLabeledCorrectSeen = new long[baseEnsemble.ensembleSizeOption.getValue()];

            for(int i = 0 ; i < this.baseEnsemble.ensembleSizeOption.getValue() ; ++i) {
                stbPred.append("Pred(" + i + "),");
                stbConf.append("Conf(" + i + "),");
                stbComplementPred.append("Pred(L\\" + i + "),");
                stbComplementConf.append("Conf(L\\" + i + "),");
                stbPseudoState.append("PseudoState(" + i + "),");

                stbLabeledSeen.append("LabeledSeen(" + i + "),");
                stbPseudoLabeledTotal.append("PseudoTotal(" + i + "),");
                stbPseudoLabeledCorrect.append("PseudoCorrect(" + i + "),");

                stbTTTacc.append("TTT_acc(" + i + "),");
                stbWINacc.append("WIN_acc(" + i + "),");
                stbWeight.append("weightPse(" + i + "),");
            }

            // Predicao do ensemble, predicao de cada membro
            this.outputConfidencePredictionDebugStream.print("#instance,groundTruthLabel,Ensemble_Pred,Ensemble_Conf,EnsembleAvgLabel,EnsembleAvgPseudoLabel," +
                    "Ensemble_TTT_acc,Ensemble_WIN_acc,");
            this.outputConfidencePredictionDebugStream.print(stbPred.toString());
            this.outputConfidencePredictionDebugStream.print(stbConf.toString());
            this.outputConfidencePredictionDebugStream.print(stbComplementPred.toString());
            this.outputConfidencePredictionDebugStream.print(stbComplementConf.toString());
            this.outputConfidencePredictionDebugStream.print(stbPseudoState.toString());

            this.outputConfidencePredictionDebugStream.print(stbLabeledSeen.toString());
            this.outputConfidencePredictionDebugStream.print(stbPseudoLabeledTotal.toString());
            this.outputConfidencePredictionDebugStream.print(stbPseudoLabeledCorrect.toString());

            this.outputConfidencePredictionDebugStream.print(stbTTTacc.toString());
            this.outputConfidencePredictionDebugStream.print(stbWINacc.toString());
            this.outputConfidencePredictionDebugStream.print(stbWeight.toString());

            this.outputConfidencePredictionDebugStream.println();
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        ++this.instancesSeen;
        ++this.allInstancesSeen;

//        estimateSupervisedLearningAccuracy(instance);

        if (this.baseEnsemble.getEnsembleMembers() == null) {
            this.baseEnsemble.initEnsemble(instance);
            initDebugMode();
            this.evaluatorTestThenTrainEnsembleMembers = new BasicClassificationPerformanceEvaluator[this.baseEnsemble.getEnsembleMembers().length];
            for (int i = 0; i < this.baseEnsemble.getEnsembleMembers().length; ++i)
                this.evaluatorTestThenTrainEnsembleMembers[i] = new BasicClassificationPerformanceEvaluator();
        }

        if(this.baseEnsemble != null && this.baseEnsemble.getEnsembleMembers() != null) {
            if(this.evaluatorTestThenTrainEnsembleMembers != null) {
                InstanceExample example = new InstanceExample(instance);
                for (int i = 0; i < this.baseEnsemble.getEnsembleMembers().length; ++i) {
                    double[] voteEnsembleMember = this.baseEnsemble.getEnsembleMembers()[i].getVotesForInstance(instance);
                    this.evaluatorTestThenTrainEnsembleMembers[i].addResult(example, voteEnsembleMember);
                }
            }
            else {
                // This is not for debug! This one is only updated during supervised learning.
                this.evaluatorTestThenTrainEnsembleMembers = new BasicClassificationPerformanceEvaluator[this.baseEnsemble.getEnsembleMembers().length];

                for (int i = 0; i < this.baseEnsemble.getEnsembleMembers().length; ++i)
                    this.evaluatorTestThenTrainEnsembleMembers[i] = new BasicClassificationPerformanceEvaluator();
            }
        }

        // **** DEBUG CODE **** //
//        if(/*this.instancesSeen >= 1000 && */ this.instancesSeen == 1970) { // && this.instancesSeen <= 2000) {
//            System.out.println("x, prob_from_drift_point (1000), prob_from_instancesSeen_point (1600), min(pfdp, pfisp)");
//            for(int x = 500 ; x <= 1800 ; x+=5) {
//                double prob_drift = sigmoid(x, 1000, 20);
//                double prob_current = 1 - sigmoid(x, 1600-20, 20);
//
//                System.out.println(/*1000 + ", " + */x + ", "
//                        + prob_drift + "," +
//                        prob_current + "," +
//                        min(prob_drift, prob_current) + "," +
//                        weightAge(x, 1000, 1600/*this.lastDriftDetectedAt, this.instancesSeen*/, 20));
//            }
//        }
        // **** DEBUG CODE **** //

        // TODO: add methods to access base models for other ensembles
        StreamingRandomPatches.StreamingRandomPatchesClassifier[] ensembleMembers = this.baseEnsemble.getEnsembleMembers();
//        Classifier[] ensembleMembers = this.baseEnsemble.getSubClassifiers();

        int [][] changeStatus = null;
        if(shouldUpdateKappaDiversity)
            changeStatus = kappaUpdateFirst(instance, ensembleMembers);

        // Store the predictions of each learner.
        HashMap<Integer, Integer> classifiersExactPredictions = classifiersPredictions(instance, ensembleMembers);

        /********* TRAIN THE ENSEMBLE *********/
        this.baseEnsemble.trainOnInstanceImpl(instance);
        /********* TRAIN THE ENSEMBLE *********/

        /** Debug **/
        if(outputConfidencePredictionDebugStream != null)
            for(int i = 0; i < this.learnerLabeledSeen.length ; ++i)
                ++this.learnerLabeledSeen[i];
        /** Debug **/

        if(useUnsupervisedDriftDetectionOption.isSet()) {
            if (this.labeledInstancesBuffer == null) {
                this.labeledInstancesBuffer = new Instances(instance.dataset());
            }
            // Just in case the window is set to 0, then it should never add any instance to it.
            if (this.labeledWindowLimitOption.getValue() > 0) {
                if (this.labeledWindowLimitOption.getValue() <= this.labeledInstancesBuffer.numInstances()) {
                    this.labeledInstancesBuffer.delete(0);
                }
                this.labeledInstancesBuffer.add(instance);
            }

            boolean driftDetected = trainAndUpdateStudentDetector(instance);
            if (driftDetected)
                reactToDrift(instance);
        }

        if(this.shouldUpdateKappaDiversity)
            kappaUpdateSecond(changeStatus, ensembleMembers, classifiersExactPredictions);
    }

    @Override
    public void addInitialWarmupTrainingInstances() {
        this.initialWarmupTrainingCounter++;
    }

    @Override
    public int trainOnUnlabeledInstance(Instance instance) {
        this.allInstancesSeen++;

//        estimateSupervisedLearningAccuracy(instance);

        if(debugPerfectPseudoLabelingOption.isSet() && instance.classIsMissing()) {
            throw new RuntimeException("Pseudo-labeling debug is on, but the true class is not available");
        }
        // DEBUG confidence and accuracy
        int groundTruthLabel = -1;
        int ensemblePred = -1;
        double ensembleConf = -1.0;
        double[] learnersPred = new double[this.baseEnsemble.ensembleSizeOption.getValue()];
        double[] learnersConf = new double[this.baseEnsemble.ensembleSizeOption.getValue()];
        double[] complementPred = new double[this.baseEnsemble.ensembleSizeOption.getValue()];
        double[] complementConf = new double[this.baseEnsemble.ensembleSizeOption.getValue()];
        int[] pseudoUsed = new int[this.baseEnsemble.ensembleSizeOption.getValue()];
        double[] learnerTTT_acc = new double[this.baseEnsemble.ensembleSizeOption.getValue()];
        double[] learnerWIN_acc = new double[this.baseEnsemble.ensembleSizeOption.getValue()];
        double[] learnersWeight = new double[this.baseEnsemble.ensembleSizeOption.getValue()];

        int predictedIndex = -1;

        // TODO: add methods to access base models for other ensembles
        StreamingRandomPatches.StreamingRandomPatchesClassifier[] ensembleMembers = this.baseEnsemble.getEnsembleMembers();

        int [][] changeStatus = null;
        if(this.shouldUpdateKappaDiversity)
            changeStatus = kappaUpdateFirst(instance, ensembleMembers);

////         Store the predictions of each learner.
        HashMap<Integer, Integer> classifiersExactPredictions = classifiersPredictions(instance, ensembleMembers);

        if(this.pairsOutputsKappa == null && this.shouldUpdateKappaDiversity)
            throw new RuntimeException("No kappa network for MULTI_VIEW_MIN_KAPPA_CONFIDENCE ");

        ArrayList<Integer> indicesNoReposition = new ArrayList<>();
        for(int i = 0 ; i < ensembleMembers.length ; ++i)
            indicesNoReposition.add(i);

        double[] votesRawOthers = new double[instance.numClasses()];
        double minIdxEstimatedAccuracy = -1.0;
        double confidenceForMajMin = -1;

        for(int i = 0 ; i < ensembleMembers.length ; ++i) {

            int idxSelectedByPairing = -1;
            switch(this.pairingFunctionOption.getChosenIndex()) {
                /* choose the furthest learner or most dissimilar to the currently selected, i.e. the smallest kappa */
                case SSL_PAIRING_MIN_KAPPA:
                    for (int j = 0; j < ensembleMembers.length; ++j) {
                        if (j != i) {
                            // Find the smallest kappa, i.e., far from
                            double currentKappa = this.getKappa(i, j);
                            if (idxSelectedByPairing == -1 || currentKappa < this.getKappa(i, idxSelectedByPairing))
                                idxSelectedByPairing = j;
                        }
                    }
                    votesRawOthers = ensembleMembers[idxSelectedByPairing].getVotesForInstance(instance);
                    minIdxEstimatedAccuracy = ensembleMembers[idxSelectedByPairing].evaluator.getPerformanceMeasurements()[1].getValue() / 100.0;
                    break;
                case SSL_PAIRING_RANDOM:
                    /* keep randomly choosing until idxMinKappa != i */
                    do {
                        idxSelectedByPairing = Math.abs(this.classifierRandom.nextInt()) % ensembleMembers.length;
                    } while(idxSelectedByPairing == i);
                    votesRawOthers = ensembleMembers[idxSelectedByPairing].getVotesForInstance(instance);
                    minIdxEstimatedAccuracy = ensembleMembers[idxSelectedByPairing].evaluator.getPerformanceMeasurements()[1].getValue() / 100.0;
                    break;
                case SSL_PAIRING_MAJORITY_TRAINS_MINORITY:
                    // TODO: Extremely naive approach, getting the same predictions (Ensemble size - 1) times.
                    // Could create an array with predictions the first time and lazily select which learners are used.
                    // For the future! Make it faster once it is right!
                    // Versao antiga!
//                    votesRawOthers = getVotesForComplementSubEnsemble(instance, i, this.baseEnsemble.getEnsembleMembers());
                    // Assumption: the complement ensemble will not yield a random prediction! Thus we can assume a performance of 0.5
                    // We could also estimate it by summing all the performance measurements for it and dividing by the ensemble size

                    /****** OLD strategy for the confidence ******/
                    if(confidenceStrategyOption.getChosenIndex() == SSL_CONFIDENCE_STRATEGY_SUM) {
                        votesRawOthers = getVotesForComplementSubEnsemble(instance, i, this.baseEnsemble.getEnsembleMembers());
                    } else { /****** NEW strategy for the confidence ******/
                        votesRawOthers = getExactVotesForComplementSubEnsemble(instance, i, this.baseEnsemble.getEnsembleMembers());
                    }
                    DoubleVector votes = new DoubleVector(votesRawOthers);
                    int predictedIndexByVotes = votes.maxIndex();
                    confidenceForMajMin = votes.getValue(predictedIndexByVotes) / votes.sumOfValues();

                    minIdxEstimatedAccuracy = 0.5;//ensembleMembers[idxMinKappa].evaluator.getPerformanceMeasurements()[1].getValue() / 100.0;
                    break;
            }

//            if(this.pairingFunctionOption.getChosenIndex() != SSL_PAIRING_MAJORITY_TRAINS_MINORITY) ;

            DoubleVector votesMinKappa = new DoubleVector(votesRawOthers);
            int predictedIndexByMinKappa = votesMinKappa.maxIndex();

            Instance instanceWithPseudoLabel = instance.copy();

            /*** DEBUG PURPOSES ONLY, ON REAL TESTS, THIS SHOULD NEVER BE USED ***/
            if (this.debugPerfectPseudoLabelingOption.isSet())
                instanceWithPseudoLabel.setClassValue(instance.classValue());
            else
                instanceWithPseudoLabel.setClassValue(predictedIndexByMinKappa);

            // Sanity check...
            if (predictedIndexByMinKappa >= 0 && votesMinKappa.sumOfValues() > 0.0 && minIdxEstimatedAccuracy > 0.0) {
                double weightMinKappa = -1;
                double weightShrinkage = 1 / this.SSL_weightShrinkageOption.getValue();

                switch(autoWeightShrinkageOption.getChosenIndex()) {
                    case AUTO_WEIGHT_SHRINKAGE_CONSTANT:
                        weightShrinkage = 1 / this.SSL_weightShrinkageOption.getValue();
                        break;
                    case AUTO_WEIGHT_SHRINKAGE_LABELED_DIV_TOTAL:
                        weightShrinkage = this.instancesSeen / (this.allInstancesSeen + 0.0000001);
                        break;
                    /** If there is a warmup period, this is taken into account when calculating the auto-shrinkage **/
                    case AUTO_WEIGHT_SHRINKAGE_LABELED_IGNORE_WARMUP_DIV_TOTAL:
                        weightShrinkage = (this.instancesSeen - this.initialWarmupTrainingCounter) / (this.allInstancesSeen - this.initialWarmupTrainingCounter + 0.0000001);
                        break;
                }

                switch(weightFunctionOption.getChosenIndex()) {
                    case SSL_WEIGHT_CONSTANT_1:
                        weightMinKappa = 1;
                        break;
                    case SSL_WEIGHT_CONFIDENCE:
                        weightMinKappa = votesMinKappa.getValue(predictedIndexByMinKappa) / votesMinKappa.sumOfValues();
                        break;
                    case SSL_WEIGHT_CONFIDENCE_WS:
                        weightMinKappa = votesMinKappa.getValue(predictedIndexByMinKappa) / votesMinKappa.sumOfValues()
                                * weightShrinkage;
                        break;
                    case SSL_WEIGHT_UNSUPERVISED_DETECTION_WEIGHT_SHRINKAGE:
                        weightMinKappa = votesMinKappa.getValue(predictedIndexByMinKappa) / votesMinKappa.sumOfValues()
                                * weightAge(ensembleMembers[idxSelectedByPairing].createdOn, this.lastDriftDetectedAt, this.instancesSeen,
                                this.unsupervisedDetectionWeightWindowOption.getValue());
                        break;
                }

                switch (SSLStrategyOption.getChosenIndex()) {
                    /** Pseudo-label without checking for confidence or if labels match. **/
                    case SSL_STRATEGY_NO_CHECKS:
                        ensembleMembers[i].trainOnInstance(
                                instanceWithPseudoLabel, weightMinKappa, this.instancesSeen, this.classifierRandom,
                                false);

                        if (!instance.classIsMissing() && ((int) instanceWithPseudoLabel.classValue()) == ((int) instance.classValue())) {
                            ++this.instancesCorrectPseudoLabeled;
                            ++this.learnerPseudoLabeledCorrectSeen[i];
                        }
                        ++this.instancesPseudoLabeled;
                        ++this.learnerPseudoLabeledSeen[i];

                        break;
                    case SSL_STRATEGY_CHECK_CONFIDENCE_AND_OTHERS:
                        // if majority trains minority
//                        double confidenceByMinKappa = -1; // votesMinKappa.getValue(predictedIndexByMinKappa) / votesMinKappa.sumOfValues();

                        DoubleVector votesByCurrent = new DoubleVector(ensembleMembers[i].getVotesForInstance(instance));
                        int predictedByCurrent = votesByCurrent.maxIndex();
                        double confidenceByCurrent = votesByCurrent.getValue(predictedByCurrent) / votesByCurrent.sumOfValues();

                        double minConfidenceThreshold = this.SSL_minConfidenceOption.getValue();

                        // NEW: this overwrite the minimum confidence threshold set by the user and apply a random one.
                        if(this.enableRandomThresholdOption.isSet()) {
                            minConfidenceThreshold = classifierRandom.nextDouble();
                        }

                        if (confidenceForMajMin > minConfidenceThreshold &&
                                predictedByCurrent != predictedIndexByMinKappa &&
                                confidenceForMajMin > confidenceByCurrent) {


                            /***** TRAIN MEMBER i WITH PSEUDO-LABELED INSTANCE *****/
                            ensembleMembers[i].trainOnInstance(
                                    instanceWithPseudoLabel, weightMinKappa, this.instancesSeen, this.classifierRandom,
                                    false);

                            if (!instance.classIsMissing() && ((int) instanceWithPseudoLabel.classValue()) == ((int) instance.classValue())) {
                                ++this.instancesCorrectPseudoLabeled;
                                // DEBUG
                                if(outputConfidencePredictionDebugStream != null) {
                                    pseudoUsed[i] = 1;
                                    ++this.learnerPseudoLabeledSeen[i];
                                }
                            }
                            else {
                                // DEBUG
                                if(outputConfidencePredictionDebugStream != null)
                                    pseudoUsed[i] = -1;
                            }
                            ++this.instancesPseudoLabeled;
                            if(outputConfidencePredictionDebugStream != null)
                                ++this.learnerPseudoLabeledSeen[i];

                        }

                        // DEBUG
                        if(this.outputConfidencePredictionDebugStream != null) {

                            double[] votes = this.baseEnsemble.getVotesForInstance(instance);
                            double sumVotes = Utils.sum(votes);

                            // the weight after considering the confidence as well
//                            groundTruthLabel = (int) instance.classValue();
                            ensemblePred = Utils.maxIndex(votes);
                            ensembleConf = votes[ensemblePred] / sumVotes;

                            learnersWeight[i] = weightMinKappa;
                            learnersPred[i] = predictedByCurrent;
                            learnersConf[i] = confidenceByCurrent;
                            complementPred[i] = predictedIndexByMinKappa;
                            complementConf[i] = confidenceForMajMin;
                            learnerTTT_acc[i] = this.baseEnsemble.getEnsembleMembers()[i].evaluator.getPerformanceMeasurements()[1].getValue();
                            learnerWIN_acc[i] = this.evaluatorWindowEnsembleMembersDebug[i].getPerformanceMeasurements()[1].getValue();
                        }
                        break;
                }
            }
        }

        // Check if changes occurred.
        if(this.shouldUpdateKappaDiversity)
            kappaUpdateSecond(changeStatus, ensembleMembers, classifiersExactPredictions);

        if(useUnsupervisedDriftDetectionOption.isSet()) {
            boolean driftDetected = trainAndUpdateStudentDetector(instance);
            if (driftDetected)
                reactToDrift(instance);
        }

        // Debug accuracy confidence
        if(this.outputConfidencePredictionDebugStream != null) {
            groundTruthLabel = (int) instance.classValue();
            long ensembleSumLabeled = 0;
            long ensembleSumPseudoLabeled = 0;

            for(int i = 0 ; i < this.learnerLabeledSeen.length ; ++i) {
                ensembleSumLabeled += learnerLabeledSeen[i];
                ensembleSumPseudoLabeled += learnerPseudoLabeledSeen[i];
            }

            printConfidenceAccuracyDebug(groundTruthLabel, ensemblePred,
                    ensembleConf,
                    ensembleSumLabeled/learnerLabeledSeen.length,
                    ensembleSumPseudoLabeled/learnerPseudoLabeledSeen.length,
                    learnersPred, learnersConf, complementPred, complementConf,
                    pseudoUsed, this.learnerLabeledSeen, this.learnerPseudoLabeledSeen,
                    this.learnerPseudoLabeledCorrectSeen,
                    learnerTTT_acc, learnerWIN_acc, learnersWeight);
        }
//        else {
//            System.out.println("this.outputConfidencePredictionDebugStream == null");
//        }
        return predictedIndex;
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        if (this.baseEnsemble.getEnsembleMembers() == null) {
            this.baseEnsemble.initEnsemble(instance);
            initDebugMode();
        }

        if(this.evaluatorWindowEnsembleMembersDebug != null && this.evaluatorWindowEnsembleMembersDebug[0] != null)
            estimateLearningAccuracy(instance);
        calculateDriftDetectionMetrics();

        double[] votes = this.baseEnsemble.getVotesForInstance(instance);

        if(this.outputConfidencePredictionDebugStream != null) {
            InstanceExample example = new InstanceExample(instance);
            this.evaluatorTestThenTrainEnsembleDebug.addResult(example, votes);
        }
        return votes;
    }


    private void printConfidenceAccuracyDebug(int groundTruthLabel, int ensemblePred, double ensembleConf,
                                              double ensembleAvgLabel, double ensembleAvgPseudoLabel,
                                              double[] learnersPred, double[] learnersConf,
                                              double[] complementPred, double[] complementConf,
                                              int[] pseudoUsed,
                                              long[] labeledSeen, long[] pseudoLabeledTotal, long[] pseudoLabeledCorrect,
                                              double[] learnerTTT_acc,
                                              double[] learnerWIN_acc, double[] learnersWeight) {
        // DEBUG
        if(this.outputConfidencePredictionDebugStream != null) {
// #instance Ensemble_TTT_acc Ensemble_WIN_acc Pred(0)	Conf(0)	Pred(L\0) Conf(L\0)	Pseudo(0) TTT_acc(0) WIN_acc(0)

            // #instance
            outputConfidencePredictionDebugStream.print(this.allInstancesSeen + ",");

            outputConfidencePredictionDebugStream.print(groundTruthLabel + ",");

            outputConfidencePredictionDebugStream.print(ensemblePred + ",");

            // Confidence
            outputConfidencePredictionDebugStream.print(ensembleConf + ",");

            // EnsembleAvgLabel
            outputConfidencePredictionDebugStream.print(ensembleAvgLabel + ",");

            // EnsembleAvgPseudoLabel
            outputConfidencePredictionDebugStream.print(ensembleAvgPseudoLabel + ",");

            // Ensemble_TTT_acc
            outputConfidencePredictionDebugStream.print(
                    this.evaluatorTestThenTrainEnsembleDebug.getPerformanceMeasurements()[1].getValue() + ",");

            outputConfidencePredictionDebugStream.print(
                    this.evaluatorWindowEnsembleDebug.getPerformanceMeasurements()[1].getValue() + ",");

            for(int i = 0 ; i < learnersPred.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(learnersPred[i] + ",");
            }

            for(int i = 0 ; i < learnersConf.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(learnersConf[i] + ",");
            }

            for(int i = 0 ; i < complementPred.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(complementPred[i] + ",");
            }

            for(int i = 0 ; i < complementConf.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(complementConf[i] + ",");
            }

            for(int i = 0 ; i < pseudoUsed.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(pseudoUsed[i] + ",");
            }

            for(int i = 0 ; i < labeledSeen.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(labeledSeen[i] + ",");
            }

            for(int i = 0 ; i < pseudoLabeledTotal.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(pseudoLabeledTotal[i] + ",");
            }

            for(int i = 0 ; i < pseudoLabeledCorrect.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(pseudoLabeledCorrect[i] + ",");
            }

            for(int i = 0 ; i < learnerTTT_acc.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(learnerTTT_acc[i] + ",");
            }

            for(int i = 0 ; i < learnerWIN_acc.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(learnerWIN_acc[i] + ",");
            }

            for(int i = 0 ; i < learnersWeight.length ; ++i) {
                this.outputConfidencePredictionDebugStream.print(learnersWeight[i] + ",");
            }

//            for(int i = 0 ; i < this.baseEnsemble.getEnsembleMembers().length ; ++i) {
//                this.outputDebugStream.print(this.evaluatorEnsembleMembersDebug[i].getPerformanceMeasurements()[1].getValue() + ",");
//            }
//
//            for(int i = 0 ; i < this.baseEnsemble.getEnsembleMembers().length ; ++i) {
//                this.outputDebugStream.print((this.instancesSeen - this.baseEnsemble.getEnsembleMembers()[i].createdOn) + ",");
//            }
            this.outputConfidencePredictionDebugStream.println();
        }
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        // instances seen * the number of ensemble members
//        return new Measurement[]{new Measurement("pseudo labeled",
//                this.instancesPseudoLabeled / (double) (this.instancesSeen * this.baseEnsemble.ensembleSizeOption.getValue()) * 100),
//                new Measurement("correct pseudo labeled",
//                        this.instancesPseudoLabeled != 0 ? this.instancesCorrectPseudoLabeled / (double) this.instancesPseudoLabeled * 100 : 0)
//        };

        int totalDriftResets = 0;
        for(int i = 0 ; i < this.baseEnsemble.getEnsembleMembers().length ; ++i)
            totalDriftResets += this.baseEnsemble.getEnsembleMembers()[i].numberOfDriftsDetected;

        // DEBUG
        if(/*this.debugShowTTTAccuracyConfidenceOption.isSet() &&*/ this.outputAccuracyStatisticsDebugStream != null) {
//            if(this.allInstancesSeen % this.debugFrequencyOption.getValue() == 0)
            outputAccuracyStatisticsDebugStream.print((this.allInstancesSeen-this.instancesSeen) + "," + this.allInstancesSeen + "," + this.instancesSeen + "," +
                    this.evaluatorWindowEnsembleDebug.getPerformanceMeasurements()[1].getValue() + ",,"); //+ ", avg conf incorr ???, avg conf corr ???");

            for(int i = 0 ; i < this.baseEnsemble.getEnsembleMembers().length ; ++i) {
                this.outputAccuracyStatisticsDebugStream.print(this.baseEnsemble.getEnsembleMembers()[i].evaluator.getPerformanceMeasurements()[1].getValue() + ",");
            }

            for(int i = 0 ; i < this.baseEnsemble.getEnsembleMembers().length ; ++i) {
                this.outputAccuracyStatisticsDebugStream.print(this.evaluatorWindowEnsembleMembersDebug[i].getPerformanceMeasurements()[1].getValue() + ",");
            }

            for(int i = 0 ; i < this.baseEnsemble.getEnsembleMembers().length ; ++i) {
                this.outputAccuracyStatisticsDebugStream.print((this.instancesSeen - this.baseEnsemble.getEnsembleMembers()[i].createdOn) + ",");
            }
            this.outputAccuracyStatisticsDebugStream.println();
        }
        return new Measurement[]{
                new Measurement("#pseudo-labeled", this.instancesPseudoLabeled),
                new Measurement("#correct pseudo-labeled", this.instancesCorrectPseudoLabeled),
                new Measurement("accuracy pseudo-labeled", this.instancesCorrectPseudoLabeled / (double) this.instancesPseudoLabeled * 100),
                new Measurement("#drift resets", totalDriftResets),
                new Measurement("avg kappa", this.currentAverageKappa),
                new Measurement("max kappa", this.currentMaxKappa),
                new Measurement("min kappa", this.currentMinKappa),
                new Measurement("#unsup drifts", this.numberOfUnsupervisedDriftsDetected),
                new Measurement("latest detection", this.lastDriftDetectedAtUnlabelled)
//                new Measurement("accuracy supervised learner", this.evaluatorSupervisedDebug.getPerformanceMeasurements()[1].getValue())
        };
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {

    }

    @Override
    public boolean isRandomizable() {
        return true;
    }


    private double[] getVotesForComplementSubEnsemble(Instance instance, int idxLearner, StreamingRandomPatches.StreamingRandomPatchesClassifier[] ensembleMembers) {
        // Store the predictions of each learner.
        DoubleVector votes = new DoubleVector();

        for (int i = 0; i < ensembleMembers.length; i++) {
            if(i != idxLearner) {
                double[] rawVote = ensembleMembers[i].getVotesForInstance(instance);
                DoubleVector vote = new DoubleVector(rawVote);
                votes.addValues(vote);
            }

//          TODO: This is internal to SRP, when we move to a generic approach, we may need to do that in here as well, but with a dedicated evaluator.
//          ensembleMembers[i].evaluator.addResult(example, vote.getArrayRef());
        }
        return votes.getArrayCopy();
    }


    private double[] getExactVotesForComplementSubEnsemble(Instance instance, int idxLearner,
                                                           StreamingRandomPatches.StreamingRandomPatchesClassifier[] ensembleMembers) {
        // Store the predictions of each learner.
        DoubleVector votes = new DoubleVector();


        for (int i = 0; i < ensembleMembers.length; i++) {
            if(i != idxLearner) {
                double[] rawVote = ensembleMembers[i].getVotesForInstance(instance);
                DoubleVector vote = new DoubleVector(rawVote);
                int maxIndex = vote.maxIndex();
                double[] dummyVote = new double[instance.numClasses()];
                dummyVote[maxIndex] = 1;
                if(vote.getValue(maxIndex) > 0)
                    votes.addValues(dummyVote);
                else
                    votes.addValues(vote); // add the array with all 0's
            }
//          TODO: This is internal to SRP, when we move to a generic approach, we may need to do that in here as well, but with a dedicated evaluator.
//          ensembleMembers[i].evaluator.addResult(example, vote.getArrayRef());
        }
        return votes.getArrayCopy();
    }


    private int[][] kappaUpdateFirst(Instance instance, StreamingRandomPatches.StreamingRandomPatchesClassifier[] ensembleMembers) {
//    private int[][] kappaUpdateFirst(Instance instance, Classifier[] ensembleMembers) {
        /******************* K STATISTIC *******************/
        if (this.pairsOutputsKappa == null) {
            this.pairsOutputsKappa = new C[ensembleMembers.length][ensembleMembers.length];
            for (int i = 0; i < this.pairsOutputsKappa.length; ++i) {
                for (int j = 0; j < this.pairsOutputsKappa[i].length; ++j) {
                    this.pairsOutputsKappa[i][j] = new C(instance.numClasses());
                }
            }
        }

        // Save the number of drifts detected per base model
        int driftStatus[] = new int[ensembleMembers.length];
        int warningStatus[] = new int[ensembleMembers.length];

        for (int i = 0; i < driftStatus.length; ++i) {
            driftStatus[i] = ensembleMembers[i].numberOfDriftsDetected;
            warningStatus[i] = ensembleMembers[i].numberOfWarningsDetected;
        }

        return new int[][]{driftStatus, driftStatus};
    }

    private void kappaUpdateSecond(int[][] changeStatus, StreamingRandomPatches.StreamingRandomPatchesClassifier[] ensembleMembers,
                                   HashMap<Integer, Integer> classifiersExactPredictions) {
        /******************* K STATISTIC *******************/
        int currentDriftStatus[] = new int[ensembleMembers.length];
        int currentWarningStatus[] = new int[ensembleMembers.length];
        // Compare the number of drifts detected per base model
        for (int i = 0; i < changeStatus[0].length; ++i) {
            currentDriftStatus[i] = ensembleMembers[i].numberOfDriftsDetected;
            currentWarningStatus[i] = ensembleMembers[i].numberOfWarningsDetected;
        }

        for (int i = 0; i < ensembleMembers.length; ++i) {
            for (int j = i + 1; j < ensembleMembers.length; ++j) {
                if (currentDriftStatus[i] - changeStatus[0][i] == 0)
                    this.pairsOutputsKappa[i][j].update(classifiersExactPredictions.get(i),
                            classifiersExactPredictions.get(j));
                else
                    this.pairsOutputsKappa[i][j].reset();
            }
        }

        double sumKappa = 0.0;
        this.currentMinKappa = 1.0;
        this.currentMaxKappa = -1.0;
        double countKappa = (double) (ensembleMembers.length * (ensembleMembers.length - 1) / 2);
        for (int i = 0; i < ensembleMembers.length; ++i) {
            for (int j = i + 1; j < ensembleMembers.length; ++j) {
                double kappa = this.pairsOutputsKappa[i][j].k();
                if(kappa < this.currentMinKappa)
                    this.currentMinKappa = kappa;
                if(kappa > this.currentMaxKappa)
                    this.currentMaxKappa = kappa;
                sumKappa += Double.isNaN(kappa) ? 1.0 : kappa;
            }
        }
        this.currentAverageKappa = sumKappa / countKappa;
    }

    private HashMap<Integer, Integer> classifiersPredictions(Instance instance, StreamingRandomPatches.StreamingRandomPatchesClassifier[] ensembleMembers) {
        // Store the predictions of each learner.
        HashMap<Integer, Integer> classifiersExactPredictions = new HashMap<>();
        for (int i = 0; i < ensembleMembers.length; i++) {
            double[] rawVote = ensembleMembers[i].getVotesForInstance(instance);
            DoubleVector vote = new DoubleVector(rawVote);

            int maxIndex = vote.maxIndex();
            if (maxIndex < 0) {
                maxIndex = this.classifierRandom.nextInt(instance.numClasses());
            }
            classifiersExactPredictions.put(i, maxIndex);

//          TODO: This is internal to SRP, when we move to a generic approach, we may need to do that in here as well, but with a dedicated evaluator.
//          ensembleMembers[i].evaluator.addResult(example, vote.getArrayRef());
        }
        return classifiersExactPredictions;
    }



    private double sigmoid(long t, long t0, int window) {
        return 1/(1 + Math.exp(-4*(t-t0)/window));
    }

    private double weightAge(long createdOn, long lstDriftDetectedOn, long labelledInstancesSeen, int window) {
        // lastDrift
        // instancesSeen (labelled data seen so far)
        double prob_drift = sigmoid(createdOn, lstDriftDetectedOn, window);
        double prob_current = 1 - sigmoid(createdOn, labelledInstancesSeen - window, window);

        return prob_drift <= prob_current ? prob_drift : prob_current;
    }

    // Debug
    private void estimateLearningAccuracy(Instance instance) {
        if(this.baseEnsemble != null && /*this.debugShowTTTAccuracyConfidenceOption.isSet()*/ this.outputAccuracyStatisticsDebugStream != null) {
            InstanceExample example = new InstanceExample(instance);
            double[] votes = this.baseEnsemble.getVotesForInstance(instance);
            this.evaluatorWindowEnsembleDebug.addResult(example, votes);

            if(this.evaluatorWindowEnsembleMembersDebug != null){
                for(int i = 0 ; i < this.baseEnsemble.getEnsembleMembers().length ; ++i) {
                    double[] voteEnsembleMember = this.baseEnsemble.getEnsembleMembers()[i].getVotesForInstance(instance);
                    this.evaluatorWindowEnsembleMembersDebug[i].addResult(example, voteEnsembleMember);
                }
            }
        }
    }

    private void calculateDriftDetectionMetrics() {
        // TODO: implement metrics
        if(outputDriftDetectionDebugStream != null) {
            if(gtDriftLocations != null) {
                for (int i = 0; i < this.gtDriftLocations.length; ++i) {
                    if (this.gtDriftLocations[i] == this.allInstancesSeen) {
                        gtDriftCurrentDebugPeriod = true;
//                        this.outputDriftDetectionDebugStream.println(this.allInstancesSeen + ", " + this.instancesSeen + ",-1,"+this.numberOfUnsupervisedDriftsDetected);
                    }
                }
            }
            if(this.allInstancesSeen % this.debugFrequencyOption.getValue() == 0 && this.allInstancesSeen != 0) {
                int gtDrift = gtDriftCurrentDebugPeriod ? 1 : 0;
                int numberDriftsSinceLastOutput = this.numberOfUnsupervisedDriftsDetected - this.currentNumberOfUnsupervisedDriftsDetected;
                this.outputDriftDetectionDebugStream.println(this.allInstancesSeen + "," + this.instancesSeen + ","
                        + numberDriftsSinceLastOutput + "," + this.numberOfUnsupervisedDriftsDetected + "," + gtDrift);
                this.currentNumberOfUnsupervisedDriftsDetected = this.numberOfUnsupervisedDriftsDetected;
                gtDriftCurrentDebugPeriod = false;
            }
        }

    }

    protected double getKappa(int i, int j) {
        assert(i != j);
        if(this.pairsOutputsKappa == null)
            return -6;
        return i > j ? this.pairsOutputsKappa[j][i].k() : this.pairsOutputsKappa[i][j].k();
    }

    // For Kappa calculation
    protected class C{
        private final long[][] C;
        private final int numClasses;
        private int instancesSeen = 0;

        public C(int numClasses) {
            this.numClasses = numClasses;
            this.C = new long[numClasses][numClasses];
        }

        public void reset() {
            for(int i = 0 ; i < C.length ; ++i)
                for(int j = 0 ; j < C.length ; ++j)
                    C[i][j] = 0;
            this.instancesSeen = 0;
        }

        public void update(int iOutput, int jOutput) {
//            System.out.println("iOutput = " + iOutput + " jOutput = " + jOutput);
            this.C[iOutput][jOutput]++;
            this.instancesSeen++;
        }

        public double theta1() {
            double sum = 0.0;
            for(int i = 0 ; i < C.length ; ++i)
                sum += C[i][i];

            return sum / (double) instancesSeen;
        }

        public double theta2() {
            double sum1 = 0.0, sum2 = 0.0, sum = 0.0;

            for(int i = 0 ; i < C.length ; ++i) {
                for(int j = 0 ; j < C.length ; ++j) {
                    sum1 += C[i][j];
                    sum2 += C[j][i];
                }
                //      System.out.println("column = (" + sum1 + "," + (sum1 / (double) instancesSeen) + ") row = (" + sum2 + "," + (sum2 / (double) instancesSeen) + ")");
                sum += (sum1 / (double) instancesSeen) * (sum2 / (double) instancesSeen);
                sum1 = sum2 = 0;
            }
            return sum;
        }

        public int getInstancesSeen() {
            return this.instancesSeen;
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder();

            buffer.append("Instances seen = ");
            buffer.append(this.instancesSeen);
            buffer.append(" Theta1 = ");
            buffer.append(this.theta1());
            buffer.append(" Theta2 = ");
            buffer.append(this.theta2());
            buffer.append(" K = ");
            buffer.append(k());
            buffer.append("\n");

            buffer.append('*');
            buffer.append('\t');
            for(int i = 0 ; i < numClasses ; ++i) {
                buffer.append(i);
                buffer.append('\t');
            }
            buffer.append('\n');
            for(int i = 0 ; i < numClasses ; ++i){
                buffer.append(i);
                buffer.append('\t');
                for(int j = 0 ; j < numClasses ; ++j) {
                    buffer.append(C[i][j]);
                    buffer.append('\t');
                }
                buffer.append('\n');
            }
            return buffer.toString();
        }

        public double k() {
            double t1 = theta1(), t2 = theta2();
            return (t1 - t2) / (double) (1.0 - t2);
        }
    }
}
