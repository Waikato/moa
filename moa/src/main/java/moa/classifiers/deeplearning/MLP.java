/*
 *    MLP.java
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

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.MultiChoiceOption;

import ai.djl.engine.Engine;
import ai.djl.Device;
import ai.djl.Model;
import ai.djl.basicmodelzoo.basic.Mlp;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.GradientCollector;
import ai.djl.training.Trainer;
import ai.djl.training.loss.Loss;
import ai.djl.training.tracker.Tracker;
import ai.djl.training.optimizer.Optimizer;

import java.text.DecimalFormat;
import java.lang.Math;
import java.util.*;

import static ai.djl.ndarray.types.DataType.FLOAT32;

class MiniBatch {
	private transient NDManager trainingNDManager;
	public transient NDArray trainMiniBatchData = null;
	public transient NDArray trainMiniBatchLabels = null;
	public int itemsInMiniBatch = 0;
	public int miniBatchSize = 1;
	public NDList d = null;
	public NDList l = null;

	public MiniBatch(Device device, int miniBatchSize) {
		this.trainingNDManager = NDManager.newBaseManager(device);
		this.miniBatchSize = miniBatchSize;
	}

	public void addToMiniBatch(double[] featureValues, double [] classValue) {
		if (itemsInMiniBatch == 0) {
			// initialize and create NDArrays
			trainMiniBatchData = trainingNDManager.create(featureValues).toType(FLOAT32, false);
			trainMiniBatchLabels = trainingNDManager.create(classValue);
		} else {
			// add item to the mini batch
			if (itemsInMiniBatch == 1) {
				trainMiniBatchData = trainMiniBatchData.stack(trainingNDManager.create(featureValues).toType(FLOAT32, false), 0);
				trainMiniBatchLabels = trainMiniBatchLabels.stack(trainingNDManager.create(classValue), 0);
			} else {
				trainMiniBatchData = trainMiniBatchData.concat(trainingNDManager.create(featureValues, new Shape(1, featureValues.length)).toType(FLOAT32, false), 0);
				trainMiniBatchLabels = trainMiniBatchLabels.concat(trainingNDManager.create(classValue, new Shape(1, classValue.length)), 0);
			}
		}
		itemsInMiniBatch++;
		if (itemsInMiniBatch == miniBatchSize){
			d = new NDList(trainMiniBatchData);
			l = new NDList(trainMiniBatchLabels);
		}
	}

	public void addToMiniBatch(Instance inst) {
		double [] instDoubleA = inst.toDoubleArray();
		NDArray dataAndLabel = trainingNDManager.create(instDoubleA).toType(FLOAT32, false);
		int featureLength = instDoubleA.length -1;
		int classLength = 1;
		NDArray data = dataAndLabel.get("0:" + Integer.toString(featureLength));
		NDArray label = dataAndLabel.get(Integer.toString(featureLength) +":" + Integer.toString(featureLength+1));

		if (itemsInMiniBatch == 0) {
			// initialize and create NDArrays
			trainMiniBatchData = data;
			trainMiniBatchLabels = label;
		} else {
			// add item to the mini batch
			if (itemsInMiniBatch == 1) {
				trainMiniBatchData = trainMiniBatchData.stack(data, 0);
				trainMiniBatchLabels = trainMiniBatchLabels.stack(label, 0);
			} else {
				trainMiniBatchData = trainMiniBatchData.concat(data.reshape(new Shape(1, featureLength)), 0);
				trainMiniBatchLabels = trainMiniBatchLabels.concat(label.reshape(new Shape(1, classLength)), 0);
			}
		}
		itemsInMiniBatch++;
		if (itemsInMiniBatch == miniBatchSize){
			d = new NDList(trainMiniBatchData);
			l = new NDList(trainMiniBatchLabels);
		}
	}

	public boolean miniBatchFull(){
		return (itemsInMiniBatch == miniBatchSize);
	}

	public void discardMiniBatch(){
		if (d != null){
			d.close();
			d = null;
		}
		if (trainMiniBatchData != null){
			trainMiniBatchData.close();
			trainMiniBatchData = null;
		}
		if (l != null){
			l.close();
			l =  null;
		}
		if (trainMiniBatchLabels != null){
			trainMiniBatchLabels.close();
			trainMiniBatchLabels = null;
		}
		trainingNDManager.close();
		trainingNDManager = null;
		itemsInMiniBatch = 0;
	}
}

public class MLP extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

	public static class NormalizeInfo{
		double sumOfValues = 0.0f;
		double sumOfSquares = 0.0f;
	}

	public static final int OPTIMIZER_SGD = 0;
	public static final int OPTIMIZER_RMSPROP = 1;
	public static final int OPTIMIZER_RMSPROP_RESET = 2;
	public static final int OPTIMIZER_ADAGRAD = 3;
	public static final int OPTIMIZER_ADAGRAD_RESET = 4;
	public static final int OPTIMIZER_ADAM = 5;
	public static final int OPTIMIZER_ADAM_RESET = 6;

	protected long samplesSeen = 0;
	protected long trainedCount = 0;
	protected NormalizeInfo[] normalizeInfo = null;

	private double[] pFeatureValues = null;
	private double [] pClassValue = null;

	public FloatOption learningRateOption = new FloatOption(
			"learningRate",
			'r',
			"Learning Rate",
			0.03, 0.0000001, 1.0);

	public FloatOption backPropLossThreshold = new FloatOption(
			"backPropLossThreshold",
			'b',
			"Back propagation loss threshold",
			0.0, 0.0, Math.pow(10,10));

	public MultiChoiceOption optimizerTypeOption = new MultiChoiceOption("optimizer", 'o',
			"Choose optimizer",
			new String[]{"SGD", "RMSPROP", "RMSPROP_RESET", "ADAGRAD", "ADAGRAD_RESET", "ADAM", "ADAM_RESET"},
			new String[]{"oSGD", "oRMSPROP", "oRMSPROP_RESET", "oADAGRAD", "oADAGRAD_RESET", "oADAM", "oADAM_RESET"},
			0);

	public FlagOption useOneHotEncode = new FlagOption("useOneHotEncode", 'h',
			"use one hot encoding");

	public FlagOption useNormalization = new FlagOption("useNormalization", 'n',
			"Normalize data");

	public IntOption numberOfNeuronsInEachLayerInLog2 = new IntOption(
			"numberOfNeuronsInEachLayerInLog2",
			'N',
			"Number of neurons in the each layer in log2",
			10, 0, 20);

	public IntOption numberOfLayers = new IntOption(
			"numberOfLayers",
			'L',
			"Number of layers",
			1, 1, 4);

	public IntOption miniBatchSize = new IntOption(
			"miniBatchSize",
			'B',
			"Mini Batch Size",
			1, 1, 2048);

	public static final int deviceTypeOptionGPU = 0;
	public static final int deviceTypeOptionCPU = 1;
	public MultiChoiceOption deviceTypeOption = new MultiChoiceOption("deviceType", 'd',
			"Choose device to run the model(use CPU if GPUs are not available)",
			new String[]{"GPU","CPU"},
			new String[]{"GPU (use CPU if not available)", "CPU"},
			deviceTypeOptionCPU);

	public IntOption djlRandomSeed = new IntOption(
			"djlRandomSeed",
			'S',
			"Random seed for DJL Engine",
			10, 0, Integer.MAX_VALUE);


	public double deltaForADWIN = 1.0E-5;

	@Override
    public String getPurposeString() {
        return "NN: special.";
    }

	public ADWIN lossEstimator;
	public String modelName;
	protected Model nnmodel = null;
	protected Trainer trainer = null;
	protected int featureValuesArraySize = 0;
	private MiniBatch miniBatch = null;
	private int numberOfClasses;
	private double [] votes;
	private int gpuCount;
	private final static DecimalFormat decimalFormat = new DecimalFormat("0.00000");


    @Override
    public void resetLearningImpl() {
    }

	public void trainOnMiniBatch(MiniBatch batch, boolean trainNet){
		NDList d = batch.d;
		NDList l = batch.l;
		try{
			samplesSeen ++;
			// train mini batch
			GradientCollector collector = trainer.newGradientCollector();
			NDList preds = trainer.forward(d, l);
			NDArray lossValue = trainer.getLoss().evaluate(l, preds);
			double loss = lossValue.getFloat();

			if ((trainNet) && (loss > backPropLossThreshold.getValue())){
				trainedCount++;
				try {
					collector.backward(lossValue);
					trainer.step(); // enforce the calculated weights
				}catch (IllegalStateException e)
				{
					//					trainer.step() throws above exception if all gradients are zero.
				}
			}
			this.lossEstimator.setInput(loss);
			collector.close();
			preds.close();
			lossValue.close();
		}catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(1);
		}
	}

    @Override
    public void trainOnInstanceImpl(Instance inst) {
		initializeNetwork(inst);


		if (miniBatch == null){
			miniBatch = new MiniBatch(nnmodel.getNDManager().getDevice(), miniBatchSize.getValue());
		}

		if (useNormalization.isSet() || useOneHotEncode.isSet()){
			pClassValue[0] = inst.classValue();
			miniBatch.addToMiniBatch(pFeatureValues, pClassValue);
		}else {
			miniBatch.addToMiniBatch(inst);
		}

		if (miniBatch.miniBatchFull() ){
			trainOnMiniBatch(miniBatch, true);
			miniBatch.discardMiniBatch();
			miniBatch = null;
		}
    }

	public double[] getVotesForFeatureValues(Instance inst, double[] featureValues) {
		initializeNetwork(inst);

		try {
			NDManager testingNDManager = NDManager.newBaseManager(nnmodel.getNDManager().getDevice());
			NDList d = new NDList(testingNDManager.create(featureValues).toType(FLOAT32, false));
			NDList preds = trainer.evaluate(d);

			for (int i = 0; i < inst.numClasses(); i++) {
				votes[i] = (double) preds.get(0).toFloatArray()[i];
			}
			preds.close();
			d.close();
			testingNDManager.close();
		}catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(1);
		}

		return votes;
	}


	public double[] getVotesForFeatureValues(Instance inst) {
		initializeNetwork(inst);

		try {
			NDManager testingNDManager = NDManager.newBaseManager(nnmodel.getNDManager().getDevice());
			double [] instDoubleA = inst.toDoubleArray();
			int featureLength = instDoubleA.length -1;
			NDArray dataAndLabel = testingNDManager.create(instDoubleA).toType(FLOAT32, false);
			NDArray data = dataAndLabel.get("0:" + Integer.toString(featureLength));

			NDList d = new NDList(data);
			NDList preds = trainer.evaluate(d);

			for (int i = 0; i < inst.numClasses(); i++) {
				votes[i] = (double) preds.get(0).toFloatArray()[i];
			}
			preds.close();
			d.close();
			testingNDManager.close();
		}catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(1);
		}

		return votes;
    }

	@Override
	public double[] getVotesForInstance(Instance inst) {

		initializeNetwork(inst);

		if (useNormalization.isSet() || useOneHotEncode.isSet()){
			setFeatureValuesArray(inst, pFeatureValues, useOneHotEncode.isSet(), true, normalizeInfo, samplesSeen);
			return getVotesForFeatureValues(inst, pFeatureValues);
		}else{
			return getVotesForFeatureValues(inst);
		}
	}


	@Override
	public ImmutableCapabilities defineImmutableCapabilities() {
		return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
	}

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    public boolean isRandomizable() {
        return false;
    }

    public static int getFeatureValuesArraySize(Instance inst, boolean useOneHotEncoding){
		int totalOneHotEncodedSize = 0;
		int totalOneHotEncodedInstances = 0;
		for(int i=0; i < inst.numInputAttributes(); i++){
			if (useOneHotEncoding && inst.attribute(i).isNominal() && (inst.attribute(i).numValues() > 2) ){
				totalOneHotEncodedSize += inst.attribute(i).numValues();
				totalOneHotEncodedInstances ++;
			}
		}
		return inst.numInputAttributes() + totalOneHotEncodedSize - totalOneHotEncodedInstances;
	}

	public static double getNormalizedValue(double value, double sumOfValues, double sumOfSquares, long samplesSeen){
		// Normalize data
		double variance = 0.0f;
		double sd;
		double mean = 0.0f;
		if (samplesSeen > 1){
			mean = sumOfValues / samplesSeen;
			variance = (sumOfSquares - ((sumOfValues * sumOfValues) / samplesSeen)) / samplesSeen;
		}
		sd = Math.sqrt(variance);
		if (sd > 0.0f){
			return (value - mean) / (3 * sd);
		} else{
			return 0.0f;
		}
	}

    public static void setFeatureValuesArray(Instance inst, double[] featureValuesArrayToSet, boolean useOneHotEncoding, boolean testing, NormalizeInfo[] normalizeInfo, long samplesSeen){
		int totalOneHotEncodedSize = 0;
		int totalOneHotEncodedInstances = 0;
		for(int i=0; i < inst.numInputAttributes(); i++){
			int index = i + totalOneHotEncodedSize - totalOneHotEncodedInstances;
			if (useOneHotEncoding && inst.attribute(i).isNominal() && (inst.attribute(i).numValues() > 2) ){
				// Do one hot-encoding
				featureValuesArrayToSet[index + (int)inst.value(i)] = 1.0f;
				totalOneHotEncodedSize += inst.attribute(i).numValues();
				totalOneHotEncodedInstances ++;
			}else
			{
				if( inst.attribute(i).isNumeric() && (normalizeInfo != null) && (normalizeInfo[index] != null) ){
					// Normalize data
					if (testing) {
						normalizeInfo[index].sumOfSquares += inst.value(i) * inst.value(i);
						normalizeInfo[index].sumOfValues += inst.value(i);
					}
					featureValuesArrayToSet[index] = getNormalizedValue(inst.value(i), normalizeInfo[index].sumOfValues, normalizeInfo[index].sumOfSquares, samplesSeen);
				}else{
					featureValuesArrayToSet[index] = inst.value(i);
				}
			}
		}
	}

	public void initializeNetwork(Instance inst) {
		if (nnmodel != null){
			return;
		}

		Set<String> engines = Engine.getAllEngines();
		Iterator<String> engineIterator = engines.iterator();
		while (engineIterator.hasNext()){
			Engine.getEngine(engineIterator.next()).setRandomSeed(djlRandomSeed.getValue());
		}

		votes = new double [inst.numClasses()];

		if (useNormalization.isSet() || useOneHotEncode.isSet()) {
			pClassValue = new double[1];
			featureValuesArraySize = getFeatureValuesArraySize(inst, useOneHotEncode.isSet());
			pFeatureValues = new double[featureValuesArraySize];
			if (useNormalization.isSet()) {
				normalizeInfo = new NormalizeInfo[featureValuesArraySize];
				for (int i = 0; i < normalizeInfo.length; i++) {
					normalizeInfo[i] = new NormalizeInfo();
				}
			}
		}else{
			featureValuesArraySize = inst.numInputAttributes();
		}

		try {
			gpuCount = Device.getGpuCount();

			numberOfClasses = inst.numClasses();
			setModel();

			lossEstimator = new ADWIN(deltaForADWIN);

			switch (this.optimizerTypeOption.getChosenIndex()){
				case MLP.OPTIMIZER_RMSPROP_RESET:
				case MLP.OPTIMIZER_ADAGRAD_RESET:
				case MLP.OPTIMIZER_ADAM_RESET:
					break;
				default:
					break;
			}
			setTrainer();
		}catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
		modelName = "L" + numberOfLayers.getValue() + "_N" + numberOfNeuronsInEachLayerInLog2.getValue() +"_" + optimizerTypeOption.getChosenLabel() +"_" + decimalFormat.format(learningRateOption.getValue());
	}

	public double getLossEstimation(){
		return lossEstimator.getEstimation();
	}

	protected void setModel(){
		try{
			if ((deviceTypeOption.getChosenIndex() == deviceTypeOptionGPU) && (gpuCount == 0)){
				throw new RuntimeException("GPU selected as device. But NO GPUs detected.");
			}
			nnmodel = Model.newInstance("mlp", (deviceTypeOption.getChosenIndex() == deviceTypeOptionGPU) ? Device.gpu() : Device.cpu());
			// Construct a neural network and set it in the block
			Integer [] neuronsInEachHiddenLayer = {};
			List<Integer> neuronsInEachHiddenLayerArrayList = new ArrayList<Integer>(Arrays.asList(neuronsInEachHiddenLayer));
			for(int i=0; i < numberOfLayers.getValue(); i++){
				neuronsInEachHiddenLayerArrayList.add((int) Math.pow(2, numberOfNeuronsInEachLayerInLog2.getValue()));
			}
			neuronsInEachHiddenLayer = neuronsInEachHiddenLayerArrayList.toArray(neuronsInEachHiddenLayer);
			Block block = new Mlp(featureValuesArraySize, numberOfClasses, Arrays.stream(neuronsInEachHiddenLayer).mapToInt(Integer::intValue).toArray());
			nnmodel.setBlock(block);
			System.out.println("System GPU count: "+ gpuCount + " Model using Device: " + nnmodel.getNDManager().getDevice());

		}catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

	protected void setTrainer(){
		if (trainer != null){
			trainer.close();
			trainer = null;
		}
		try {
		Tracker learningRateTracker = Tracker.fixed((float) this.learningRateOption.getValue());
		Optimizer optimizer;

		switch(this.optimizerTypeOption.getChosenIndex()) {
			case MLP.OPTIMIZER_RMSPROP_RESET:
			case MLP.OPTIMIZER_RMSPROP:
				optimizer = Optimizer.rmsprop().optLearningRateTracker(learningRateTracker).build();
				break;
			case MLP.OPTIMIZER_ADAGRAD_RESET:
			case MLP.OPTIMIZER_ADAGRAD:
				optimizer = Optimizer.adagrad().optLearningRateTracker(learningRateTracker).build();
				break;
			case MLP.OPTIMIZER_ADAM_RESET:
			case MLP.OPTIMIZER_ADAM:
				optimizer = Optimizer.adam().optLearningRateTracker(learningRateTracker).build();
				break;
			case MLP.OPTIMIZER_SGD:
			default:
				optimizer = Optimizer.sgd().setLearningRateTracker(learningRateTracker).build();
				break;
		}
		Loss loss = Loss.softmaxCrossEntropyLoss();
		DefaultTrainingConfig config = new DefaultTrainingConfig(loss);
		config.optOptimizer(optimizer);
		trainer = nnmodel.newTrainer(config);
		trainer.initialize(new Shape(miniBatchSize.getValue(), featureValuesArraySize));
		}catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}
}