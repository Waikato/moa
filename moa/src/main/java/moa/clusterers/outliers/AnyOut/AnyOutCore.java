/*
 *    AnyOutCore.java
 *
 *    @author I. Assent, P. Kranen, C. Baldauf, T. Seidl
 *    @author G. Piskas, A. Gounaris
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

package moa.clusterers.outliers.AnyOut;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.DenseInstance;
import java.util.ArrayList;
import java.util.HashMap;
import moa.clusterers.clustree.ClusKernel;
import moa.clusterers.clustree.ClusTree;
import moa.clusterers.clustree.Entry;
import moa.clusterers.clustree.Node;
import moa.clusterers.outliers.AnyOut.util.DataObject;
import moa.clusterers.outliers.AnyOut.util.DataSet;


@SuppressWarnings("serial")
public class AnyOutCore extends ClusTree {
	
	///////////////////////////////////////////////
	// the variables all became HashMaps to easily map the values to the objectIds,
	// i.e. they exist once per object that is currently examined
	// the lists have then to be used by the methods from MultipleDetector (see below)
	// for the static manager, use ID=0 as default for the current object!
	private HashMap<Integer,Double> aggregatedOScoreResult, lastOScoreResult, lastConfidenceResult;
	private HashMap<Integer, ClusKernel> objectAsKernel;
	private HashMap<Integer,ArrayList<Double>> previousOScoreResultList;
	private HashMap<Integer,Node> descendToNode;
	private HashMap<Integer, Integer> currentLevel;
	///////////////////////////////////////////////

	// Outlier score threshold.
	private double threshold;
	
	// Entry weight threshold.
    private double weightThreshold = 0.05;
	private int oScoreK;
	private int confK;

	public IntOption trainingSetSizeOption = new IntOption("TrainingSetSize", 't', "Training Set Size.", 1000, 0, 10000);
	//public FlagOption UseBulkLoadingOption = new FlagOption("UseBulkLoading", 'b', "Use Bulkloading or traditional learning.");
	public IntOption oScoreKOption = new IntOption("OScorek", 'o', "Size of Oscore aggregate.", 2, 1, 10);
	public IntOption confKOption = new IntOption("Confidencek", 'c', "Size of confidence aggregate.", 2, 1, 10);
	public IntOption confidenceChoiceOption = new IntOption("confidence", 'd', "Confidence Measure.", 4, 1, 6);
	public FlagOption UseMeanScoreOption = new FlagOption("UseMeanScore", 'm', "Use Mean score or Density score.");
	public FloatOption threshholdOption = new FloatOption("Threshold", 'z', "Threshold", 0.07, 0, 1);
	
	public AnyOutCore() {
		lastOScoreResult = new HashMap<Integer,Double>();
		lastConfidenceResult = new HashMap<Integer,Double>();
		objectAsKernel = new HashMap<Integer, ClusKernel>();
		aggregatedOScoreResult = new HashMap<Integer,Double>();
		previousOScoreResultList = new HashMap<Integer,ArrayList<Double>>();
		descendToNode = new HashMap<Integer,Node>();
		currentLevel = new HashMap<Integer,Integer>();
	}
	
	public void resetLearning() {
		if (UseMeanScoreOption.isSet()) {
			threshold = threshholdOption.getValue();
		} else {
			threshold = 0.0;
		}
		oScoreK = oScoreKOption.getValue();
		confK = confKOption.getValue();
		super.resetLearningImpl();
	}
	
	public void train(DataSet trainingSet) {
		// TODO fix not working builder!
		// ClusTree private variables are not updated but are mandatory for the algorithm to function.
//		if (UseBulkLoadingOption.isSet()) { 
//			// Use BulkLoading
//			EMTopDownTreeBuilder builder = new EMTopDownTreeBuilder();
//			try {
//				this.root = builder.buildTree(trainingSet);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		} else {
			//Use traditional initialization
			for (DataObject o : trainingSet.getDataObjectArray()){
				DenseInstance inst = new DenseInstance(o.getFeatures().length);
				for(int i=0; i<o.getFeatures().length; i++){
					inst.setValue(i, o.getFeatures()[i]);
				}
				trainOnInstance(inst);
			}
//		}
	}
	
	public void initObject(int objectId, double[] features) {
		previousOScoreResultList.put(objectId, new ArrayList<Double>());
		currentLevel.put(objectId, 0);
		// process root of the tree and set score according to the closest entry
		ClusKernel newKernel = new ClusKernel(features, features.length);
		objectAsKernel.put(objectId, newKernel);
		
		Entry closestEntry = root.nearestEntry(newKernel);
		if (UseMeanScoreOption.isSet()) 
			lastOScoreResult.put(objectId, newKernel.calcDistance(closestEntry.data));
		else
			lastOScoreResult.put(objectId,getDensityOutlierScore(newKernel,closestEntry.data));
		
		aggregatedOScoreResult.put(objectId, lastOScoreResult.get(objectId));
		// remember (store) next Node to descend into for further processing 
		descendToNode.put(objectId, closestEntry.getChild());
    	//update confidence
		updateConfidence(objectId);
	}
	
	public void learnObject(double[] features){
		DenseInstance inst = new DenseInstance(features.length);
		for(int i=0; i<features.length; i++){
			inst.setValue(i, features[i]);
		}
		trainOnInstance(inst);
	}

	public void removeObject(int objectId) {
		lastOScoreResult.remove(objectId);
		lastConfidenceResult.remove(objectId);
		aggregatedOScoreResult.remove(objectId);
		previousOScoreResultList.remove(objectId);
		descendToNode.remove(objectId);
		objectAsKernel.remove(objectId);
		currentLevel.remove(objectId);
	}
	
	// TODO fix not working density measure!!! Must be between 0 and 1.
	private double getDensityOutlierScore(ClusKernel x, ClusKernel entry) {
		double[] sigmaSquared = entry.getVarianceVector();
		// f(x) = factor * e^( -0.5 (x- mu ) * Sigma ^-1 * (x- mu ) )
		double resultDensity = 0;

		// exponent = -0.5 (x- mu ) * Sigma ^-1 * (x- mu ) )
		//                = -0.5 ( sum_d ((x_i - mu_i)^2 / var_i) )
		double exponent = 0.0;
		double[] mu = entry.getCenter();

		// factor = 1 / sqrt ( (2*PI)^d * \prod_d(variance) )
		//        = 1 / sqrt ((2*PI)^d) * sqrt (\prod_d(variance))
		//        = 1 / (2*PI)^(d/2)   * \prod_d( sqrt (variance))
		double factor = Math.pow( (2.0 * Math.PI) , ((sigmaSquared.length/2.0)) );
		for (int i = 0; i < sigmaSquared.length; i++) {
			factor *= Math.sqrt(sigmaSquared[i]);
			exponent += ((x.LS[i] - mu[i])*(x.LS[i] - mu[i])) / sigmaSquared[i];
		}

		// factor = 1 / sqrt ( (2*PI)^d * \prod_d(variance) )
		factor = 1 / factor;
		exponent *= -0.5;
		//System.out.println(factor);
		resultDensity = factor * Math.exp(exponent);
		//System.out.println(resultDensity);

		// TODO: dangerous, since the density is not necessarily between 0 and 1
		return 1-resultDensity;
	}
	
	private void useAggregatedOScoreResults(int objectId) {
		if (oScoreK <= 1){
			aggregatedOScoreResult.put(objectId, lastOScoreResult.get(objectId));
		} else {
			double mu = lastOScoreResult.get(objectId);
			int count=0;
			for (int i=Math.max(0, previousOScoreResultList.get(objectId).size()-(oScoreK-1)); i<previousOScoreResultList.get(objectId).size();i++){
				Double d = previousOScoreResultList.get(objectId).get(i);
				mu+=d.doubleValue();
				count++;
			}
			aggregatedOScoreResult.put(objectId, mu/(count+1));
		}
	}
	
	public boolean moreImprovementsPossible(int objectId, double depthPercentage) {
		if(currentLevel.get(objectId) < maxHeight * depthPercentage && descendToNode.get(objectId)!=null) {
			return true;
		}
		return false;
	}
	
	public void improveObjectOnce(int objectId) {
		currentLevel.put(objectId, currentLevel.get(objectId)+1);
		
		// descend into closest entry and find the next closest entry there
		ClusKernel mKernel = objectAsKernel.get(objectId);
		previousOScoreResultList.get(objectId).add(new Double(lastOScoreResult.get(objectId)));
		Entry closestEntry = descendToNode.get(objectId).nearestEntry(mKernel);
		
		// return if irrelevant and keep the previous score.
        if (closestEntry.data.getWeight()< weightThreshold){
        //if (closestEntry.isIrrelevant(weightThreshold)) {
        	descendToNode.remove((objectId));
        } else {
			// update score and next Node to descend into
			if (UseMeanScoreOption.isSet())
				lastOScoreResult.put(objectId, mKernel.calcDistance(closestEntry.data));
			else
				lastOScoreResult.put(objectId, getDensityOutlierScore(mKernel,closestEntry.data));
			useAggregatedOScoreResults(objectId);
			descendToNode.put(objectId, closestEntry.getChild());
			
			//update confidence
			updateConfidence(objectId);
        }
	}
		
	/**
	 * Calculates the Confidence on the basis of the standard deviation of previous OScore results
	 * @return confidence
	 */
	private double calcC1(int objectId) {
		int nrOfPreviousResults = previousOScoreResultList.get(objectId).size();
		if (nrOfPreviousResults == 0) {
			return 0.0;
		}
		int count=1;
		double difSum_k = Math.abs(lastOScoreResult.get(objectId)-previousOScoreResultList.get(objectId).get(nrOfPreviousResults-1));
		
		//if previousOscoreResultList contains more than two results, this loop sums up further diffs
		for (int i=Math.max(0, nrOfPreviousResults - (confK-1)) + 1; i < nrOfPreviousResults; i++){
			difSum_k += Math.abs(previousOScoreResultList.get(objectId).get(i)-previousOScoreResultList.get(objectId).get(i-1));
			count++;
		}
		// hier msste gelten count==confK-1, d.h. wenn ich die letzten 3 Werte betrachten will, bekomme ich 2 differenzen 
		// XXX SW: Nicht ganz. Wenn ich die letzten 4 Werte betrachten will,  aber erst 2 Ergebnisse zur Verf�gung stehen, bekomme ich an anstatt der 3 Differenzen nur 1
		// dafr die Zhlvariable
		difSum_k /= count;
		return Math.pow(Math.E, (-1.0 * difSum_k));
	}

	private double calcC2(int objectId) {
		int nrOfPreviousResults = previousOScoreResultList.get(objectId).size();
		//consider last confK results (including the current one)
		int count=1;
		double sum_k=lastOScoreResult.get(objectId);
		for (int i = Math.max(0, nrOfPreviousResults - (confK-1)); i < nrOfPreviousResults; i++){
			sum_k += previousOScoreResultList.get(objectId).get(i);
			count++;
		}
		// hier msste gelten count==confK, da wir die letzten confK Werte betrachten
		// XXX SW: hier wieder wie bei C1
		sum_k /= count;
		
		return Math.pow(Math.E, (-1.0 * sum_k));
	}
	
	private double calcC3(int objectId) {
		if (getHeight()==0){
			return (1.0 * currentLevel.get(objectId))/(1.0 * maxHeight);
		}
		return (1.0 * currentLevel.get(objectId))/(1.0 * getHeight());
	}
	
	private void updateConfidence(int objectId) {
		int confChoice = confidenceChoiceOption.getValue();
		if (confChoice == 1)
			lastConfidenceResult.put(objectId, calcC1(objectId));
		if (confChoice == 2)
			lastConfidenceResult.put(objectId, calcC2(objectId));
		if (confChoice == 3)
			lastConfidenceResult.put(objectId, calcC3(objectId));
		
		if (confChoice == 4)
			lastConfidenceResult.put(objectId, calcC1(objectId)*calcC2(objectId));
		if (confChoice == 5)
			lastConfidenceResult.put(objectId, calcC1(objectId)*calcC3(objectId));
		if (confChoice == 6)
			lastConfidenceResult.put(objectId, calcC2(objectId)*calcC3(objectId));
		
		if (confChoice == 7)
			lastConfidenceResult.put(objectId, calcC1(objectId)*calcC2(objectId)*calcC3(objectId));
	}

	public boolean isOutlier(int id) {
		return aggregatedOScoreResult.get(id)/lastConfidenceResult.get(id) > threshold;
	}

	public double getOutlierScore(int id) {
		return aggregatedOScoreResult.get(id)/lastConfidenceResult.get(id);
	}

	public double getConfidence(int id) {
		return lastConfidenceResult.get(id);
	}
}