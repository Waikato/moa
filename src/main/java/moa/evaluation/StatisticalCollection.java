/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package moa.evaluation;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;

/**
 *
 * @author jansen
 */
public class StatisticalCollection extends MeasureCollection{
	private static final long serialVersionUID = 1L;
	protected Random instanceRandom;
	private boolean debug = false;

	public StatisticalCollection() {
		super();
		instanceRandom = new Random(117);
	}

	@Override
	protected String[] getNames() {
		String[] names = {"van Dongen","Rand statistic","VarInformation"};
		return names;
	}

	@Override
	protected boolean[] getDefaultEnabled() {
		boolean [] defaults = {false, false, false, false};
		return defaults;
	}

	@Override
	public void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points) throws Exception {

		int[][] counts = new int [trueClustering.size()+1][clustering.size()+1];
		int [] sumsHC = new int[trueClustering.size()+1];
		int [] sumsFC = new int[clustering.size()+1];
		int n = 0;

		for (int p = 0; p < points.size(); p++) {
			DataPoint point = points.get(p);
			boolean hc_noise = true;
			for (int i = 0; i < trueClustering.size()+1; i++) {
				boolean check = false;
				if(i < trueClustering.size()){
					Cluster hc = trueClustering.get(i);
					if(hc.getInclusionProbability(point)>= 1){
						check = true;
						hc_noise = false;
					}
				}
				else{
					if(hc_noise)
						check = true;
				}
				if(check){
					boolean fc_noise = true;
					for (int j = 0; j < clustering.size()+1; j++) {
						if(j < clustering.size()){
							Cluster fc = clustering.get(j);
							if(fc.getInclusionProbability(point)>= 1){
								counts[i][j]++;
								sumsFC[j]++;
								sumsHC[i]++;
								n++;
								fc_noise = false;
							}
						}
						else{
							if(fc_noise){
								counts[i][j]++;
								sumsFC[j]++;
								sumsHC[i]++;
								n++;
							}
						}
					}
				}
			}
		}
		if(debug){
			for (int i = 0; i < counts.length; i++) {
				System.out.println("Con "+i+": "+Arrays.toString(counts[i]));
			}
			System.out.println("Sum FC"+Arrays.toString(sumsFC));
			System.out.println("Sum HC"+Arrays.toString(sumsHC));
		}


		double mutual = 0;
		for (int j = 0; j < clustering.size()+1; j++){
			for (int i = 0; i < trueClustering.size()+1; i++) {
				if(counts[i][j]==0) continue;
				double m = (double)counts[i][j]/(double)n * Math.log((double)counts[i][j]/(double)sumsFC[j]/(double)sumsHC[i]*(double) n);
				if(debug)
					System.out.println("("+i+"/"+ j + "): "+m);
				mutual+=m;
			}
		}
		double mutualraw = mutual;
		//mutual/=Math.log(trueClustering.size()+1);


		double varInfo = 0;
		double varInfoFC = 0;
		for (int j = 0; j < clustering.size()+1; j++){
			if(sumsFC[j]==0) continue;
			varInfoFC+=sumsFC[j]/(double)n*Math.log(sumsFC[j]/(double)n);
		}
		double varInfoHC = 0;
		for (int i = 0; i < trueClustering.size()+1; i++) {
			if(sumsHC[i]==0) continue;
			varInfoHC+=sumsHC[i]/(double)n*Math.log(sumsHC[i]/(double)n);
		}
		if(debug){
			System.out.println("FC "+varInfoFC+ " / HC "+varInfoHC+" / mutual "+mutual);
		}
		//varInfo = -varInfoFC - varInfoHC - 2*mutualraw ;
		//varInfo = 1-varInfo/(2*Math.log(Math.max(clustering.size()+1, trueClustering.size()+1)));
		if(Math.abs(mutualraw + varInfoFC + varInfoHC) < 1e-10){
			varInfo = 1;
		}
		else{
			varInfo = 2*mutualraw/(-varInfoFC - varInfoHC );
		}
		addValue("VarInformation", varInfo);


		double dongen = 0;
		double dongenMaxFC = 0;
		double dongenMaxSumFC = 0;
		for (int j = 0; j < clustering.size()+1; j++){
			double max = 0;
			for (int i = 0; i < trueClustering.size()+1; i++) {
				if(counts[i][j]>max) max = counts[i][j];
			}
			dongenMaxFC+=max;
			if(sumsFC[j]>dongenMaxSumFC) dongenMaxSumFC = sumsFC[j];
		}

		double dongenMaxHC = 0;
		double dongenMaxSumHC = 0;
		for (int i = 0; i < trueClustering.size()+1; i++) {
			double max = 0;
			for (int j = 0; j < clustering.size()+1; j++){
				if(counts[i][j]>max) max = counts[i][j];
			}
			dongenMaxHC+=max;
			if(sumsHC[i]>dongenMaxSumHC) dongenMaxSumHC = sumsHC[i];
		}

		dongen = 1-(2*n - dongenMaxFC - dongenMaxHC)/(2*n - dongenMaxSumFC - dongenMaxSumHC);
		if(debug)
			System.out.println("Dongen HC:"+dongenMaxHC+" FC:"+dongenMaxFC+" Total:"+dongen+" n "+n);

		addValue("van Dongen", dongen);


		//Rand index
		//http://www.cais.ntu.edu.sg/~qihe/menu4.html
		double m1 = 0;
		for (int i = 0; i < trueClustering.size()+1; i++) {
			double v = sumsHC[i];
			m1+= v*(v-1)/2.0;
		}
		double m2 = 0;
		for (int j = 0; j < clustering.size()+1; j++){
			double v = sumsFC[j];
			m2+= v*(v-1)/2.0;
		}

		double m = 0;
		for (int i = 0; i < trueClustering.size()+1; i++) {
			for (int j = 0; j < clustering.size()+1; j++){
				double v = counts[i][j];
				m+= v*(v-1)/2.0;
			}
		}
		double M = n*(n-1)/2.0;
		double normalizedRand = (m - m1*m2/M)/(m1/2.0 + m2/2.0 - m1*m2/M);
		//double rand = (M - m1 - m2 +2*m)/M;
		addValue("Rand statistic", normalizedRand);


	}




}
