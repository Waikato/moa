/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package moa.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;

/**
 *
 * @author jansen
 */
public class F1 extends MeasureCollection{
	private static final long serialVersionUID = 1L;
	private final double pointInclusionProbThreshold = 0.5;

	public F1() {
		super();

	}

	@Override
	protected String[] getNames() {
		String[] names = {"Precision","Recall","F1"};
		return names;
	}


	@SuppressWarnings("unchecked")
	public void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points) {

		if (clustering.size()<0){
			addValue(0,0);
			addValue(1,0);
			addValue(2,0);
			return;
		}

		//init labelmap
		HashMap<Integer, Integer> labelMap = new HashMap<Integer, Integer>();
		int numGTCluster = trueClustering.size();
		for (int c = 0; c < numGTCluster; c++) {
			labelMap.put((int)trueClustering.get(c).getGroundTruth(),c);
		}

		//init lists to hold points according to found clusters
		ArrayList<Integer>[] foundClusters = (ArrayList<Integer>[]) new ArrayList[clustering.size()];
		for (int i = 0; i < foundClusters.length; i++) {
			foundClusters[i] = new ArrayList<Integer>();
		}

		int classWeightsFoundClusters[][] = new int[clustering.size()][numGTCluster];
		int classWeightsHidden[] = new int[numGTCluster];

		//map points to clusters
		for (int p = 0; p < points.size(); p++) {
			int worklabel = -1;
			if(points.get(p).classValue()!=-1)
				worklabel = labelMap.get((int)points.get(p).classValue());
			for (int c = 0; c < clustering.size(); c++) {
				double prob = clustering.get(c).getInclusionProbability(points.get(p));
				//we need to change this in case we get real probabilities and not just 0 / 1
				if(prob >= pointInclusionProbThreshold){
					foundClusters[c].add(p);
					if(worklabel!=-1)
						classWeightsFoundClusters[c][worklabel]++;
				}
			}
			//real class distribution
			if(worklabel!=-1)
				classWeightsHidden[worklabel]++;

		}

		//figure out f1 per cluster
		double[] precision = new double[clustering.size()];
		double[] recall = new double[clustering.size()];
		double[] f1 = new double[clustering.size()];
		double F1 = 0.0;
		double precisionTotal = 0.0;
		double recallTotal = 0.0;

		int realClusters = 0;

		//F1 as defined in P3C, try using F1 optimization
		for (int i = 0; i < clustering.size(); i++) {
			int max_weight = 0;
			int max_weight_index = -1;
			int cluster_weight = 0;
			for (int j = 0; j < numGTCluster; j++) {
				if(classWeightsFoundClusters[i][j] > max_weight){
					max_weight = classWeightsFoundClusters[i][j];
					max_weight_index = j;
				}
				cluster_weight+=classWeightsFoundClusters[i][j];
			}
			if(max_weight_index!=-1){
				realClusters++;
				precision[i] = (double)classWeightsFoundClusters[i][max_weight_index]/(double)cluster_weight;
				recall[i] = (double)classWeightsFoundClusters[i][max_weight_index]/(double)classWeightsHidden[max_weight_index];
				if(precision[i] > 0 || recall[i] > 0)
					f1[i] = 2*precision[i]*recall[i]/(precision[i]+recall[i]);
				clustering.get(i).setMeasureValue("Precision", Double.toString(precision[i]));
				clustering.get(i).setMeasureValue("Recall", Double.toString(recall[i]));
				clustering.get(i).setMeasureValue("F1", Double.toString(f1[i]));

				precisionTotal += precision[i];
				recallTotal += recall[i];
				F1 += f1[i];
			}
		}

		if(realClusters > 0){
			F1/=realClusters;
			recallTotal/=realClusters;
			precisionTotal/=realClusters;
		}

		addValue("F1",F1);
		addValue("Precision",precisionTotal);
		addValue("Recall",recallTotal);

	}
}
