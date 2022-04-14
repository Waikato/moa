/*
 *    SAMkNN.java
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
package moa.classifiers.lazy;
import java.util.*;

import com.yahoo.labs.samoa.instances.InstanceImpl;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.FlagOption;
import moa.clusterers.kmeanspm.CoresetKMeans;
/**
 * Self Adjusting Memory (SAM) coupled with the k Nearest Neighbor classifier (kNN) .<p>
 *
 * Valid options are:<p>
 *
 * -k number of neighbours <br> -w max instances <br> -m minimum number of instances in the STM <br> -p LTM size relative to max instances <br> -r Recalculation of the STM error <br>
 *
 * @author Viktor Losing (vlosing@techfak.uni-bielefeld.de)
 * Paper:
 * "KNN Classifier with Self Adjusting Memory for Heterogeneous Concept Drift"
 *  Viktor Losing, Barbara Hammer and Heiko Wersing
 * http://ieeexplore.ieee.org/document/7837853
 * PDF can be found at https://pub.uni-bielefeld.de/download/2907622/2907623
 * BibTex:
 * "@INPROCEEDINGS{7837853,
 * author={V. Losing and B. Hammer and H. Wersing},
 * booktitle={2016 IEEE 16th International Conference on Data Mining (ICDM)},
 * title={KNN Classifier with Self Adjusting Memory for Heterogeneous Concept Drift},
 * year={2016},
 * pages={291-300},
 * keywords={data mining;optimisation;pattern classification;Big Data;Internet of Things;KNN classifier;SAM-kNN robustness;data mining;k nearest neighbor algorithm;metaparameter optimization;nonstationary data streams;performance evaluation;self adjusting memory model;Adaptation models;Benchmark testing;Biological system modeling;Data mining;Heuristic algorithms;Prediction algorithms;Predictive models;Data streams;concept drift;data mining;kNN},
 * doi={10.1109/ICDM.2016.0040},
 * month={Dec}
 * }"
 */
public class SAMkNN extends AbstractClassifier implements MultiClassClassifier,
  							  CapabilitiesHandler {
    private static final long serialVersionUID = 1L;

    public IntOption kOption = new IntOption( "k", 'k', "The number of neighbors", 5, 1, Integer.MAX_VALUE);

    public IntOption limitOption = new IntOption( "limit", 'w', "The maximum number of instances to store", 5000, 1, Integer.MAX_VALUE);
    public IntOption minSTMSizeOption = new IntOption( "minSTMSize", 'm', "The minimum number of instances in the STM", 50, 1, Integer.MAX_VALUE);

    public FloatOption relativeLTMSizeOption = new FloatOption(
            "relativeLTMSize",
            'p',
            "The allowed LTM size relative to the total limit.",
            0.4, 0.0, 1.0);

    public FlagOption recalculateSTMErrorOption = new FlagOption("recalculateError", 'r',
            "Recalculates the error rate of the STM for size adaption (Costly operation). Otherwise, an approximation is used.");
	private int maxClassValue = 0;

    @Override
    public String getPurposeString() {
        return "SAMkNN: special.";
    }

    private Instances stm;
	private Instances ltm;
	private int maxLTMSize;
	private int maxSTMSize;
	private List<Integer> stmHistory;
	private List<Integer> ltmHistory;
	private List<Integer> cmHistory;
	private double[][] distanceMatrixSTM;
	//private int trainStepCount;
	private Map<Integer, List<Integer>> predictionHistories;
	private Random random;

    protected void init(){
    	this.maxLTMSize = (int)(relativeLTMSizeOption.getValue() * limitOption.getValue());
    	this.maxSTMSize = limitOption.getValue() - this.maxLTMSize;
    	this.stmHistory = new ArrayList<>();
    	this.ltmHistory = new ArrayList<>();
    	this.cmHistory = new ArrayList<>();
    	//store calculated STM distances in a matrix to avoid recalculation, are reused in the STM adaption phase
		this.distanceMatrixSTM = new double[limitOption.getValue()+1][limitOption.getValue()+1];
		this.predictionHistories = new HashMap<>();
		this.random = new Random();

    }

	@Override
	public void setModelContext(InstancesHeader context) {
		try {
			this.stm = new Instances(context,0);
			this.stm.setClassIndex(context.classIndex());
			this.ltm = new Instances(context,0); 
			this.ltm.setClassIndex(context.classIndex());
			this.init();
		} catch(Exception e) {
			System.err.println("Error: no Model Context available.");
			e.printStackTrace();
			System.exit(1);
		}
	}

    @Override
    public void resetLearningImpl() {
		this.stm = null;
		this.ltm = null;
		this.stmHistory = null;
		this.ltmHistory = null;
		this.cmHistory = null;
		this.distanceMatrixSTM = null;
		this.predictionHistories = null;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        //this.trainStepCount++;
		if (inst.classValue() > maxClassValue)
			maxClassValue = (int)inst.classValue();
		this.stm.add(inst);
		memorySizeCheck();
		clean(this.stm, this.ltm, true);
		double distancesSTM[] = this.get1ToNDistances(inst, this.stm);
		for (int i =0; i < this.stm.numInstances();i++){
			this.distanceMatrixSTM[this.stm.numInstances()-1][i] = distancesSTM[i];
		}
		int oldWindowSize = this.stm.numInstances();
		int newWindowSize = this.getNewSTMSize(recalculateSTMErrorOption.isSet());

		if (newWindowSize < oldWindowSize) {
			int diff = oldWindowSize - newWindowSize;
			Instances discardedSTMInstances = new Instances(this.stm, 0);

			for (int i = diff; i>0;i--){
				discardedSTMInstances.add(this.stm.get(0).copy());
				this.stm.delete(0);
			}
			for (int i = 0; i < this.stm.numInstances(); i++){
				for (int j = 0; j < this.stm.numInstances(); j++){
					this.distanceMatrixSTM[i][j] = this.distanceMatrixSTM[diff+i][diff+j];
				}
			}
			for (int i = 0; i < diff; i++) {
				if(this.stmHistory.size() > 0)	this.stmHistory.remove(0);
				if(this.ltmHistory.size() > 0)	this.ltmHistory.remove(0);
				if(this.cmHistory.size()  > 0)	 this.cmHistory.remove(0);
			}

			this.clean(this.stm, discardedSTMInstances, false);
			for (int i = 0; i < discardedSTMInstances.numInstances(); i++){
				this.ltm.add(discardedSTMInstances.get(i).copy());
			}
			memorySizeCheck();
		}
    }
	/**
	 * Predicts the label of a given sample by using the STM, LTM and the CM.
     */
    @Override
    public double[] getVotesForInstance(Instance inst) {

		double vSTM[];
		double vLTM[];
		double vCM[];
		double v[];
		double distancesSTM[];
		double distancesLTM[];
        int predClassSTM = 0;
        int predClassLTM = 0;
        int predClassCM = 0;
		try {
			if (this.stm.numInstances()>0) {
				distancesSTM = get1ToNDistances(inst, this.stm);
				int nnIndicesSTM[] = nArgMin(Math.min(distancesSTM.length, this.kOption.getValue()), distancesSTM);
				vSTM = getDistanceWeightedVotes(distancesSTM, nnIndicesSTM, this.stm);
                predClassSTM = this.getClassFromVotes(vSTM);
                distancesLTM = get1ToNDistances(inst, this.ltm);
                vCM = getCMVotes(distancesSTM, this.stm, distancesLTM, this.ltm);
                predClassCM = this.getClassFromVotes(vCM);
				if (this.ltm.numInstances() >= 0) {
                    int nnIndicesLTM[] = nArgMin(Math.min(distancesLTM.length, this.kOption.getValue()), distancesLTM);
                    vLTM = getDistanceWeightedVotes(distancesLTM, nnIndicesLTM, this.ltm);
                    predClassLTM = this.getClassFromVotes(vLTM);
                }else{
                    vLTM = new double[inst.numClasses()];
                }
                int correctSTM = historySum(this.stmHistory);
                int correctLTM = historySum(this.ltmHistory);
                int correctCM = historySum(this.cmHistory);
                if(correctSTM>=correctLTM && correctSTM>=correctCM){
                    v=vSTM;
                }else if(correctLTM>correctSTM && correctLTM>=correctCM){
                    v=vLTM;
                }else{
                    v=vCM;
                }
            }else {
                v = new double[inst.numClasses()];
            }
            this.stmHistory.add((predClassSTM==inst.classValue()) ? 1 : 0);
            this.ltmHistory.add((predClassLTM==inst.classValue()) ? 1 : 0);
            this.cmHistory.add((predClassCM==inst.classValue()) ? 1 : 0);
		} catch(Exception e) {
			return new double[inst.numClasses()];
		}
		return v;
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


	private int historySum(List<Integer> history){
		int sum = 0;
		for (Integer e : history) {
			sum += e;
		}
		return sum;
	}

	private List<double[]> kMeans(List<double[]> points, int k){
		List<double[]> centroids = CoresetKMeans
				.generatekMeansPlusPlusCentroids(k,
						points, this.random);
		CoresetKMeans.kMeans(centroids, points);
		return centroids;
	}
	/**
	 * Performs classwise kMeans++ clustering for given samples with corresponding labels. The number of samples is halved per class.
	 */
	private void clusterDown(){
		int classIndex = this.ltm.classIndex();
		for (int c = 0; c <= this.maxClassValue; c++){
			List<double[]> classSamples = new ArrayList<>();
			for (int i = this.ltm.numInstances()-1; i >-1 ; i--) {
				if (this.ltm.get(i).classValue() == c) {
					classSamples.add(this.ltm.get(i).toDoubleArray());
					this.ltm.delete(i);
				}
			}
			if (classSamples.size() > 0) {
				//used kMeans++ implementation expects the weight of each sample at the first index,
				// make sure that the first value gets the uniform weight 1, overwrite class value
				for (double[] sample : classSamples) {
					if (classIndex != 0) {
						sample[classIndex] = sample[0];
					}
					sample[0] = 1;
				}

				List<double[]> centroids = this.kMeans(classSamples, Math.max(classSamples.size() / 2, 1));

				for (double[] centroid : centroids) {

					double[] attributes = new double[this.ltm.numAttributes()];
					//returned centroids do not contain the weight anymore, but simply the data
					System.arraycopy(centroid, 0, attributes, 1, this.ltm.numAttributes() - 1);
					//switch back if necessary
					if (classIndex != 0) {
						attributes[0] = attributes[classIndex];
					}
					attributes[classIndex] = c;
					Instance inst = new InstanceImpl(1, attributes);
					inst.setDataset(this.ltm);
					this.ltm.add(inst);
				}
			}

		}
	}

    /**
     * Makes sure that the STM and LTM combined doe not surpass the maximum size.
     */
	private void memorySizeCheck(){
		if (this.stm.numInstances() + this.ltm.numInstances() > this.maxSTMSize + this.maxLTMSize){
			if (this.ltm.numInstances() > this.maxLTMSize){
				this.clusterDown();
			}else{ //shift values from STM directly to LTM since STM is full
				int numShifts = this.maxLTMSize - this.ltm.numInstances() + 1;
				for (int i = 0; i < numShifts; i++){
					this.ltm.add(this.stm.get(0).copy());
					this.stm.delete(0);
					this.stmHistory.remove(0);
					this.ltmHistory.remove(0);
					this.cmHistory.remove(0);
				}
				this.clusterDown();
				this.predictionHistories.clear();
				for (int i = 0; i < this.stm.numInstances(); i++){
					for (int j = 0; j < this.stm.numInstances(); j++){
						this.distanceMatrixSTM[i][j] = this.distanceMatrixSTM[numShifts+i][numShifts+j];
					}
				}
			}
		}
	}

	private void cleanSingle(Instances cleanAgainst, int cleanAgainstindex, Instances toClean){
		Instances cleanAgainstTmp = new Instances(cleanAgainst);
		cleanAgainstTmp.delete(cleanAgainstindex);
		double distancesSTM[] = get1ToNDistances(cleanAgainst.get(cleanAgainstindex), cleanAgainstTmp);
		int nnIndicesSTM[] = nArgMin(Math.min(this.kOption.getValue(), distancesSTM.length), distancesSTM);

		double distancesLTM[] = get1ToNDistances(cleanAgainst.get(cleanAgainstindex), toClean);
		int nnIndicesLTM[] = nArgMin(Math.min(this.kOption.getValue(), distancesLTM.length), distancesLTM);
		double distThreshold = 0;
		for (int nnIdx: nnIndicesSTM){
			if (cleanAgainstTmp.get(nnIdx).classValue() == cleanAgainst.get(cleanAgainstindex).classValue()){
				if (distancesSTM[nnIdx] > distThreshold){
					distThreshold = distancesSTM[nnIdx];
				}
			}
		}
		List<Integer> delIndices = new ArrayList<>();
        for (int nnIdx: nnIndicesLTM){
			if (toClean.get(nnIdx).classValue() != cleanAgainst.get(cleanAgainstindex).classValue()) {
				if (distancesLTM[nnIdx] <= distThreshold){
					delIndices.add(nnIdx);
				}
			}
		}
		Collections.sort(delIndices, Collections.reverseOrder());
		for (Integer idx : delIndices)
			toClean.delete(idx);
	}
    /**
     * Removes distance-based all instances from the input samples that contradict those in the STM.
     */
	private void clean(Instances cleanAgainst, Instances toClean, boolean onlyLast) {
		if (cleanAgainst.numInstances() > this.kOption.getValue() && toClean.numInstances() > 0){
			if (onlyLast){
				cleanSingle(cleanAgainst, (cleanAgainst.numInstances()-1), toClean);
			}else{
				for (int i=0; i < cleanAgainst.numInstances(); i++){
					cleanSingle(cleanAgainst, i, toClean);
				}
			}
		}
	}
    /**
     * Returns the distance weighted votes.
     */
	private double [] getDistanceWeightedVotes(double distances[], int[] nnIndices, Instances instances){

		double v[] = new double[this.maxClassValue +1];
        for (int nnIdx : nnIndices) {
            v[(int)instances.instance(nnIdx).classValue()] += 1./Math.max(distances[nnIdx], 0.000000001);
        }
		return v;
	}

	private double [] getDistanceWeightedVotesCM(double distances[], int[] nnIndices, Instances stm, Instances ltm){
		double v[] = new double[this.maxClassValue +1];
        for (int nnIdx : nnIndices) {
			if (nnIdx < stm.numInstances()) {
				v[(int) stm.instance(nnIdx).classValue()] += 1. / Math.max(distances[nnIdx], 0.000000001);
			} else{
				v[(int) ltm.instance((nnIdx-stm.numInstances())).classValue()] += 1. / Math.max(distances[nnIdx], 0.000000001);
			}
		}
		return v;
	}

    /**
     * Returns the distance weighted votes for the combined memory (CM).
     */
	private double [] getCMVotes(double distancesSTM[], Instances stm, double distancesLTM[], Instances ltm){
		double[] distancesCM = new double[distancesSTM.length + distancesLTM.length];
		System.arraycopy(distancesSTM, 0, distancesCM, 0, distancesSTM.length);
		System.arraycopy(distancesLTM, 0, distancesCM, distancesSTM.length, distancesLTM.length);
		int nnIndicesCM[] = nArgMin(Math.min(distancesCM.length, this.kOption.getValue()), distancesCM);
		return getDistanceWeightedVotesCM(distancesCM, nnIndicesCM, stm, ltm);
	}

    /**
     * Returns the class with maximum vote.
     */
	private int getClassFromVotes(double votes[]){
		double maxVote = -1;
		int maxVoteClass = -1;
		for (int i = 0; i < votes.length; i++){
			if (votes[i] > maxVote){
				maxVote = votes[i];
                maxVoteClass = i;
			}
		}
		return maxVoteClass;
	}

	private int getLabelFct(double distances[], Instances instances, int startIdx, int endIdx){
		int nnIndices[] = nArgMin(Math.min(this.kOption.getValue(), distances.length), distances, startIdx, endIdx);
		double votes[] = getDistanceWeightedVotes(distances, nnIndices, instances);
		return this.getClassFromVotes(votes);
	}

    /**
     * Returns the Euclidean distance.
     */
	private double getDistance(Instance sample, Instance sample2)
    {
        double sum=0;
        for (int i=0; i<sample.numInputAttributes(); i++)
        {
            double diff = sample.valueInputAttribute(i)-sample2.valueInputAttribute(i);
            sum += diff*diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Returns the Euclidean distance between one sample and a collection of samples in an 1D-array.
     */
	private double[] get1ToNDistances(Instance sample, Instances samples){
		double distances[] = new double[samples.numInstances()];
		for (int i=0; i<samples.numInstances(); i++){
			distances[i] = this.getDistance(sample, samples.get(i));
		}
		return distances;
	}

    /**
     * Returns the n smallest indices of the smallest values (sorted).
     */
	private int[] nArgMin(int n, double[] values, int startIdx, int endIdx){
		int indices[] = new int[n];
		for (int i=0; i<n; i++){
			double minValue = Double.MAX_VALUE;
			for (int j=startIdx; j<endIdx+1; j++){
				if (values[j] < minValue){
					boolean alreadyUsed = false;
					for (int k=0; k<i; k++){
						if (indices[k]==j){
							alreadyUsed = true;
						}
					}
					if (!alreadyUsed){
						indices[i] = j;
						minValue = values[j];
					}
				}
			}
		}
		return indices;
	}

	private int[] nArgMin(int n, double[] values){
		return nArgMin(n, values, 0, values.length-1);
	}

    /**
     * Removes predictions of the largest window size and shifts the remaining ones accordingly.
     */
	private void adaptHistories(int numberOfDeletions){
		for (int i = 0; i < numberOfDeletions; i++){
			SortedSet<Integer> keys = new TreeSet<>(this.predictionHistories.keySet());
			this.predictionHistories.remove(keys.first());
			keys = new TreeSet<>(this.predictionHistories.keySet());
			for (Integer key : keys){
				List<Integer> predHistory = this.predictionHistories.remove(key);
				this.predictionHistories.put(key-keys.first(), predHistory);
			}
		}
	}

    /**
     * Creates a prediction history incrementally by using the previous predictions.
     */
	private List<Integer> getIncrementalTestTrainPredHistory(Instances instances, int startIdx, List<Integer> predictionHistory){
		for (int i= startIdx + this.kOption.getValue() + predictionHistory.size(); i < instances.numInstances(); i++){
			predictionHistory.add((this.getLabelFct(distanceMatrixSTM[i], instances, startIdx,  i-1)==instances.get(i).classValue()) ? 1 : 0);
		}
		return predictionHistory;
	}
    /**
     * Creates a prediction history from the scratch.
     */
	private List<Integer> getTestTrainPredHistory(Instances instances, int startIdx){
		List<Integer> predictionHistory = new ArrayList<>();
		for (int i= startIdx + this.kOption.getValue(); i < instances.numInstances(); i++){
			predictionHistory.add((this.getLabelFct(distanceMatrixSTM[i], instances, startIdx, i-1)==instances.get(i).classValue()) ? 1 : 0);
		}
		return predictionHistory;
	}
    /**
     * Returns the window size with the minimum Interleaved test-train error, using bisection (with recalculation of the STM error).
     */
	private int getMinErrorRateWindowSize() {

		int numSamples = this.stm.numInstances();
		if (numSamples < 2 * this.minSTMSizeOption.getValue()) {
			return numSamples;
		} else {
			List<Integer> numSamplesRange = new ArrayList<>();
			numSamplesRange.add(numSamples);
			while (numSamplesRange.get(numSamplesRange.size() - 1) >= 2 * this.minSTMSizeOption.getValue())
				numSamplesRange.add(numSamplesRange.get(numSamplesRange.size() - 1) / 2);

			Iterator it = this.predictionHistories.keySet().iterator();
			while (it.hasNext()) {
				Integer key = (Integer) it.next();
				if (!numSamplesRange.contains(numSamples - key)) {
					it.remove();
				}
			}
			List<Double> errorRates = new ArrayList<>();
			for (Integer numSamplesIt : numSamplesRange) {
				int idx = numSamples - numSamplesIt;
				List<Integer> predHistory;
				if (this.predictionHistories.containsKey(idx)) {
					predHistory = this.getIncrementalTestTrainPredHistory(this.stm, idx, this.predictionHistories.get(idx));
				} else {
					predHistory = this.getTestTrainPredHistory(this.stm, idx);
				}
				this.predictionHistories.put(idx, predHistory);
				errorRates.add(this.getHistoryErrorRate(predHistory));
			}
			int minErrorRateIdx = errorRates.indexOf(Collections.min(errorRates));
			int windowSize = numSamplesRange.get(minErrorRateIdx);
			if (windowSize < numSamples) {
				this.adaptHistories(minErrorRateIdx);
			}
			return windowSize;
		}
	}
    /**
     * Calculates the achieved error rate of a history.
     */
	private double getHistoryErrorRate(List<Integer> predHistory){
		double sumCorrect = 0;
		for (Integer e : predHistory) {
			sumCorrect += e;
		}
		return 1. - (sumCorrect / predHistory.size());
	}

    /**
     * Returns the window size with the minimum Interleaved test-train error, using bisection (without recalculation using an incremental approximation).
     */
	private int getMinErrorRateWindowSizeIncremental() {
		int numSamples = this.stm.numInstances();
		if (numSamples < 2 * this.minSTMSizeOption.getValue()) {
			return numSamples;
		} else {
			List<Integer> numSamplesRange = new ArrayList<>();
			numSamplesRange.add(numSamples);
			while (numSamplesRange.get(numSamplesRange.size() - 1) >= 2 * this.minSTMSizeOption.getValue())
				numSamplesRange.add(numSamplesRange.get(numSamplesRange.size() - 1) / 2);
			List<Double> errorRates = new ArrayList<>();
			for (Integer numSamplesIt : numSamplesRange) {
				int idx = numSamples - numSamplesIt;
				List<Integer> predHistory;
				if (this.predictionHistories.containsKey(idx)) {
					predHistory = this.getIncrementalTestTrainPredHistory(this.stm, idx, this.predictionHistories.get(idx));
				} else if (this.predictionHistories.containsKey(idx-1)){
					predHistory = this.predictionHistories.remove(idx-1);
					predHistory.remove(0);
					predHistory = this.getIncrementalTestTrainPredHistory(this.stm, idx, predHistory);
					this.predictionHistories.put(idx, predHistory);
				} else {
					predHistory = this.getTestTrainPredHistory(this.stm, idx);
					this.predictionHistories.put(idx, predHistory);
				}
				errorRates.add(this.getHistoryErrorRate(predHistory));
			}
			int minErrorRateIdx = errorRates.indexOf(Collections.min(errorRates));
			if (minErrorRateIdx > 0) {
				for (int i = 1; i < errorRates.size(); i++){
					if (errorRates.get(i) < errorRates.get(0)){
						int idx = numSamples - numSamplesRange.get(i);
						List<Integer> predHistory = this.getTestTrainPredHistory(this.stm, idx);
						errorRates.set(i, this.getHistoryErrorRate(predHistory));
						this.predictionHistories.remove(idx);
						this.predictionHistories.put(idx, predHistory);
					}
				}
				minErrorRateIdx = errorRates.indexOf(Collections.min(errorRates));
			}
			int windowSize = numSamplesRange.get(minErrorRateIdx);
			if (windowSize < numSamples) {
				this.adaptHistories(minErrorRateIdx);
			}
			return windowSize;
		}
	}
    /**
     * Returns the bisected STM size which minimizes the interleaved-test-train error.
     */
	private int getNewSTMSize(boolean recalculateErrors){
		if (recalculateErrors)
			return this.getMinErrorRateWindowSize();
		else
			return this.getMinErrorRateWindowSizeIncremental();
	}

  @Override
  public ImmutableCapabilities defineImmutableCapabilities() {
    if (this.getClass() == SAMkNN.class)
      return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
    else
      return new ImmutableCapabilities(Capability.VIEW_STANDARD);
  }
}
