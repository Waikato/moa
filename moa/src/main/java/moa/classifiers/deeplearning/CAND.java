/*
 *    CAND.java
 *    Copyright (C) 2022 University of Waikato, Hamilton, New Zealand
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

package moa.classifiers.deeplearning;

import com.github.javacliparser.*;
import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.core.InstanceExample;
import moa.core.Measurement;
import moa.evaluation.BasicClassificationPerformanceEvaluator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

/**
 * Continuously Adaptive Neural networks for Data streams
 *
 * <p>Continuously Adaptive Neural networks for Data streams (CAND). For every prediction, CAND chooses the current best
 * network from a pool of candidates by continuously monitoring the performance of all candidate networks. The candidates
 * are trained using different optimizers and hyperparameters. There are two orthogonal heuristics (skip back propagation,
 * smaller training pool) for accelerating CAND, which trade-off small amounts of accuracy for significant runtime gains.
 * When training, small mini-batches yields similar accuracy to single-instance fully incremental training, even on
 * evolving data streams.</p>
 *
 * <p>See details in:<br> Nuwan Gunasekara, Heitor Murilo Gomes, Bernhard Pfahringer, Albert Bifet.
 * Online Hyperparameter Optimization for Streaming Neural Networks.
 * International Joint Conference on Neural Networks (IJCNN), 2022.</p>
 *
 * <p>Parameters:</p> <ul>
 * <li>-P : The larger pool type.
 * P10 = { learning rates: 5.0E-(1 to 5), optimizes: SGD,Adam, neurons in 1st layer:  2^(8 to 10) },
 * P30 = { learning rates: 5.0E-(1 to 5), optimizes: Adam, neurons in 1st layer:  2^9 }</li>
 * <li>-o : Number of MLPs to train at a given time (after -s numberOfInstancesToTrainAllMLPsAtStart instances).</li>
 * <li>-L : Number of layers in each MLP.</li>
 * <li>-s : Number of instances to train all MLPs at start.</li>
 * <li>-B : Mini Batch Size.</li>
 * <li>-h : Use one hot encoding.</li>
 * <li>-n : Normalize data.</li>
 * <li>-b : Skip back propagation loss threshold.</li>
 * <li>-d : Choose device to run the model(For GPU, needs CUDA installed on the system. Use CPU if GPUs are not available)</li>
 * <li>-t : Do NOT train each MLP using a separate thread.</li>
 * <li>-f : Votes dump file name.</li>
 * <li>-F : Stats dump file name.</li>
 * </ul>
 *
 * @author Nuwan Gunasekara (ng98 at students dot waikato dot ac dot nz)
 * @version $Revision: 1 $
 */

public class CAND extends AbstractClassifier implements MultiClassClassifier, CapabilitiesHandler {

    private static final long serialVersionUID = 1L;
    protected MLP[] nn = null;
    protected int featureValuesArraySize = 0;
    protected long samplesSeen = 0;
    protected MLP.NormalizeInfo[] normalizeInfo = null;
    private double[] featureValues = null;
    private double [] class_value = null;
    private ExecutorService exService = null;
    private FileWriter statsDumpFile = null;
    private FileWriter votesDumpFile = null;
    private BasicClassificationPerformanceEvaluator performanceEvaluator = new BasicClassificationPerformanceEvaluator();
    private long driftsDetectedPerSampleFrequency = 0;
    private long totalDriftsDetected = 0;
    private long avgMLPsPerSampleFrequency = 0;
    private long lastGetModelMeasurementsImplCalledAt=0;

    private MiniBatch miniBatch = null;

    private ADWIN accEstimator = new ADWIN(1.0E-3);

    public static final int LARGER_P_POOL_10 = 0;
    public static final int LARGER_P_POOL_30 = 1;

    public MultiChoiceOption largerPool = new MultiChoiceOption("largerPool", 'P',
            "The larger pool type",
            new String[]{"P10", "P30"},
            new String[]{
                    "P10 = { learning rates: 5.0E-(1 to 5), optimizes: SGD,Adam, neurons in 1st layer:  2^(8 to 10) }",
                    "P30 = { learning rates: 5.0E-(1 to 5), optimizes: Adam, neurons in 1st layer:  2^9 }"},
            LARGER_P_POOL_30);

    public IntOption numberOfMLPsToTrainOption = new IntOption(
            "numberOfMLPsToTrain",
            'o',
            "Number of MLPs to train at a given time (after numberOfInstancesToTrainAllMLPsAtStart instances)",
            10, 2, Integer.MAX_VALUE);

    public IntOption numberOfLayersInEachMLP = new IntOption(
            "numberOfLayersInEachMLP",
            'L',
            "Number of layers in each MLP",
            1, 1, 4);

    public IntOption numberOfInstancesToTrainAllMLPsAtStartOption = new IntOption(
            "numberOfInstancesToTrainAllMLPsAtStart",
            's',
            "Number of instances to train all MLPs at start",
            100, 0, Integer.MAX_VALUE);

    public IntOption miniBatchSize = new IntOption(
            "miniBatchSize",
            'B',
            "Mini Batch Size",
            1, 1, 2048);


    public FlagOption useOneHotEncode = new FlagOption("useOneHotEncode", 'h',
            "use one hot encoding");

    public FlagOption useNormalization = new FlagOption("useNormalization", 'n',
            "Normalize data");

    public FloatOption backPropLossThreshold = new FloatOption(
            "backPropLossThreshold",
            'b',
            "Skip back propagation loss threshold",
            0.3, 0.0, Math.pow(10,10));

    public MultiChoiceOption deviceTypeOption = new MultiChoiceOption("deviceType", 'd',
            "Choose device to run the model(For GPU, needs CUDA installed on the system. Use CPU if GPUs are not available)",
            new String[]{"GPU","CPU"},
            new String[]{"GPU (Needs CUDA installed on the system. Use CPU if not available)", "CPU"},
            MLP.deviceTypeOptionCPU);

    public FlagOption doNotTrainEachMLPUsingASeparateThread = new FlagOption("doNotTrainEachMLPUsingASeparateThread", 't',
            "Do NOT train each MLP using a separate thread");
    public StringOption votesDumpFileName = new StringOption("votesDumpFileName", 'f',
            "Votes dump file name",
            "" );
    public StringOption statsDumpFileName = new StringOption("statsDumpFileName", 'F',
            "Stats dump file name",
            "" );

    public IntOption djlRandomSeed = new IntOption(
            "djlRandomSeed",
            'S',
            "Random seed for DJL Engine",
            10, 0, Integer.MAX_VALUE);

    @Override
    public void resetLearningImpl() {
        if (nn != null) {
            exService.shutdownNow();
            exService = null;
            for (int i = 0; i < this.nn.length; i++) {
                nn[i] = null;
            }
            nn = null;
            featureValuesArraySize = 0;
            samplesSeen = 0;
            normalizeInfo = null;
            featureValues = null;
            class_value = null;
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        if(this.nn == null){
            initNNs(instance);
        }

        class_value[0] = instance.classValue();

        if (miniBatch == null){
            miniBatch = new MiniBatch(this.nn[0].nnmodel.getNDManager().getDevice(), miniBatchSize.getValue());
//            System.out.println("For training mini batch using device: " + trainingNDManager.getDevice());
        }

        miniBatch.addToMiniBatch(featureValues, class_value);
        if (miniBatch.miniBatchFull() ){
            int numberOfMLPsToTrain = numberOfMLPsToTrainOption.getValue();
            int numberOfTopMLPsToTrain = numberOfMLPsToTrain /2;
            if (samplesSeen < numberOfInstancesToTrainAllMLPsAtStartOption.getValue()){
                numberOfMLPsToTrain = nn.length;
                numberOfTopMLPsToTrain = nn.length;
            }
            avgMLPsPerSampleFrequency += numberOfMLPsToTrain;

            Arrays.sort(this.nn, new Comparator<MLP>() {
                @Override
                public int compare(MLP o1, MLP o2) {
                    return Double.compare(o1.getLossEstimation(), o2.getLossEstimation());
                }
            });

            boolean [] trainNetwork = new boolean[this.nn.length];
            for (int i =0; i < numberOfMLPsToTrain; i++) {
                int nnIndex;
                if (i < numberOfTopMLPsToTrain){
                    // top most train
                    nnIndex = i;
                }else {
                    // Random train
                    int offSet = (int) ((samplesSeen + i) % (this.nn.length  - numberOfTopMLPsToTrain));
                    nnIndex = numberOfTopMLPsToTrain + offSet;
                }
                trainNetwork[nnIndex] = true;
            }

            class TrainThread implements Callable<Boolean> {
                private final MLP mlp;
                private final MiniBatch miniBatch;
                private final boolean trainNet;

                public TrainThread(MLP mlp, MiniBatch miniBatch, boolean trainNet) {
                    this.mlp = mlp;
                    this.miniBatch = miniBatch;
                    this.trainNet = trainNet;
                }

                @Override
                public Boolean call() {
                    try {
                        this.mlp.trainOnMiniBatch(this.miniBatch, this.trainNet);
                    } catch (NullPointerException e){
                        e.printStackTrace();
                        System.exit(1);
                    }
                    return Boolean.TRUE;
                }
            }

            final Future<Boolean> [] runFuture = new Future[this.nn.length];

            for (int i =0; i < this.nn.length; i++) {
                if (! this.doNotTrainEachMLPUsingASeparateThread.isSet()){
                    //start thread
                    runFuture[i] = exService.submit(new TrainThread(this.nn[i], this.miniBatch, trainNetwork[i]));
                }else{
                    this.nn[i].initializeNetwork(instance);
                    this.nn[i].trainOnMiniBatch(miniBatch, trainNetwork[i]);
                }
            }

            if (! this.doNotTrainEachMLPUsingASeparateThread.isSet()){
                // wait for threads to complete
                int runningCount = this.nn.length;
                while (runningCount != 0){
                    runningCount = 0;
                    for (int i =0; i < this.nn.length; i++) {
                        try {
                            final Boolean returnedValue = runFuture[i].get();
                            if (!returnedValue.equals(Boolean.TRUE)){
                                runningCount++;
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            miniBatch.discardMiniBatch();
            miniBatch = null;
        }
    }

    private void printStats(){
        long sampleFrequency = samplesSeen - lastGetModelMeasurementsImplCalledAt;
        if (statsDumpFile != null) {
            for (int i = 0; i < this.nn.length; i++) {
                try {
                    statsDumpFile.write(samplesSeen + ","
                            + this.nn[i].samplesSeen + ","
                            + this.nn[i].trainedCount + ","
                            + this.nn[i].modelName + ","
                            + performanceEvaluator.getPerformanceMeasurements()[1].getValue() + ","
                            + this.nn[i].lossEstimator.getEstimation() + ","
                            + totalDriftsDetected + ","
                            + sampleFrequency + ","
                            + driftsDetectedPerSampleFrequency + ","
                            + avgMLPsPerSampleFrequency / sampleFrequency + "\n");
                    statsDumpFile.flush();
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            }
        }
    }

    private void printVotes(Instance instance){
        if (votesDumpFile != null) {
            for (int i = 0; i < this.nn.length; i++) {
                try {
                    votesDumpFile.write(samplesSeen + ","
                            + this.nn[i].modelName + ","
                            + this.nn[i].lossEstimator.getEstimation() + ","
                            + instance.classValue() + ","
                            + instance.classIndex() + ","
                            + Arrays.toString(this.nn[i].getVotesForFeatureValues(instance, featureValues))
                            + "\n");
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        int chosenIndex = 0;
        double [] votes;
        samplesSeen ++;

        if(this.nn == null) {
            initNNs(instance);
        }else {
            double minEstimation = Double.MAX_VALUE;
            for (int i = 0 ; i < this.nn.length ; i++) {
                if (this.nn[i].getLossEstimation() < minEstimation){
                    minEstimation = this.nn[i].getLossEstimation();
                    chosenIndex = i;
                }
            }
        }
        MLP.setFeatureValuesArray(instance, featureValues, useOneHotEncode.isSet(), true, normalizeInfo, samplesSeen);

        votes = this.nn[chosenIndex].getVotesForFeatureValues(instance, featureValues);
        performanceEvaluator.addResult(new InstanceExample(instance), votes);
        double lastAcc = accEstimator.getEstimation();
        accEstimator.setInput(performanceEvaluator.getPerformanceMeasurements()[1].getValue());
        if (accEstimator.getChange() && (accEstimator.getEstimation() < lastAcc)){
            totalDriftsDetected++;
            driftsDetectedPerSampleFrequency++;
        }
        printVotes(instance);
        return votes;
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public void getModelDescription(StringBuilder arg0, int arg1) {
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        printStats();
        driftsDetectedPerSampleFrequency = 0;
        avgMLPsPerSampleFrequency = 0;
        lastGetModelMeasurementsImplCalledAt = samplesSeen;
        try{
            if (votesDumpFile != null) {
                votesDumpFile.flush();
            }
        }catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return null;
    }

    protected void initNNs(Instance instance) {
        class MLPConfigs{
            private final int numberOfNeuronsInL1InLog2;
            private final int optimizerType;
            private final float learningRate;
            private final double deltaForADWIN;

            MLPConfigs(int numberOfNeuronsInL1InLog2, int optimizerType, float learningRate, double deltaForADWIN){
                this.numberOfNeuronsInL1InLog2 = numberOfNeuronsInL1InLog2;
                this.optimizerType = optimizerType;
                this.learningRate = learningRate;
                this.deltaForADWIN = deltaForADWIN;
            }
        }

        MLPConfigs [] nnConfigs = {};
        List<MLPConfigs> nnConfigsArrayList = new ArrayList<MLPConfigs>(Arrays.asList(nnConfigs));
        float [] denominator = {10.0f, 100.0f, 1000.0f, 10000.0f, 100000.0f};
        float [] numerator = new float [] {5.0f};
        for (int n=0; n < numerator.length; n++){
            for (int d=0; d < denominator.length; d++){
                float lr = numerator[n]/denominator[d];
                for (int numberOfNeuronsInL1InLog2 = 8; numberOfNeuronsInL1InLog2 < 11; numberOfNeuronsInL1InLog2++){

                    if (largerPool.getChosenIndex() == LARGER_P_POOL_30) {
                        nnConfigsArrayList.add(new MLPConfigs(numberOfNeuronsInL1InLog2, MLP.OPTIMIZER_SGD, lr, 1.0E-3));
                    }else{ // LARGER_P_POOL_10
                        if ( numberOfNeuronsInL1InLog2 == 9){
                            continue;
                        }else{
                            // numberOfNeuronsInL1InLog2 = 8 or 10
                        }
                    }
                    nnConfigsArrayList.add(new MLPConfigs(numberOfNeuronsInL1InLog2, MLP.OPTIMIZER_ADAM, lr, 1.0E-3));
                }
            }
        }
        nnConfigs = nnConfigsArrayList.toArray(nnConfigs);

        this.nn = new MLP[nnConfigs.length];
        for(int i=0; i < nnConfigs.length; i++){
            this.nn[i] = new MLP();
            this.nn[i].optimizerTypeOption.setChosenIndex(nnConfigs[i].optimizerType);
            this.nn[i].learningRateOption.setValue(nnConfigs[i].learningRate);
            this.nn[i].useOneHotEncode.setValue(useOneHotEncode.isSet());
            this.nn[i].deviceTypeOption.setChosenIndex(deviceTypeOption.getChosenIndex());
            this.nn[i].numberOfNeuronsInEachLayerInLog2.setValue(nnConfigs[i].numberOfNeuronsInL1InLog2);
            this.nn[i].numberOfLayers.setValue(numberOfLayersInEachMLP.getValue());
            this.nn[i].deltaForADWIN = nnConfigs[i].deltaForADWIN;
            this.nn[i].backPropLossThreshold.setValue(backPropLossThreshold.getValue());
            this.nn[i].djlRandomSeed.setValue(djlRandomSeed.getValue());
            this.nn[i].initializeNetwork(instance);
        }

        try {
            if (statsDumpFileName.getValue().length() > 0) {
                statsDumpFile = new FileWriter(statsDumpFileName.getValue());
                statsDumpFile.write("id," +
                        "samplesSeenAtTrain," +
                        "trainedCount," +
                        "optimizer_type_learning_rate_delta," +
                        "acc," +
                        "estimated_loss," +
                        "totalDriftsDetected," +
                        "sampleFrequency," +
                        "driftsDetectedPerSampleFrequency," +
                        "avgMLPsPerSampleFrequency" +
                        "\n");
                statsDumpFile.flush();
            }
            if (votesDumpFileName.getValue().length() > 0 ) {
                votesDumpFile = new FileWriter(votesDumpFileName.getValue());
                votesDumpFile.write("id," +
                        "modelName," +
                        "estimated_loss," +
                        "classValue," +
                        "classIndex," +
                        "votes," +
                        "\n");
                votesDumpFile.flush();
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        exService = Executors.newFixedThreadPool(nnConfigs.length);

        class_value = new double[1];
        featureValuesArraySize = MLP.getFeatureValuesArraySize(instance, useOneHotEncode.isSet());
        System.out.println("Number of features before one-hot encode: " + instance.numInputAttributes() + " : Number of features after one-hot encode: " + featureValuesArraySize);
        featureValues = new double [featureValuesArraySize];
        if (useNormalization.isSet()) {
            normalizeInfo = new MLP.NormalizeInfo[featureValuesArraySize];
            for(int i=0; i < normalizeInfo.length; i++){
                normalizeInfo[i] = new MLP.NormalizeInfo();
            }
        }
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
    }
}
