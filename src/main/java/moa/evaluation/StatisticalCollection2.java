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
public class StatisticalCollection2 extends MeasureCollection{
	private static final long serialVersionUID = 1L;
	protected Random instanceRandom;
	private boolean debug = false;
	private final double beta = 0.5;

	public StatisticalCollection2() {
		super();
		instanceRandom = new Random(117);
	}

	@Override
	protected String[] getNames() {
		String[] names = {"GT cross entropy","FC cross entropy","Homogeneity","Completeness","V-Measure"};
		return names;
	}

	@Override
	protected boolean[] getDefaultEnabled() {
		boolean [] defaults = {false, false, false, false, false};
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
			System.out.println("Sum FC "+Arrays.toString(sumsFC));
			System.out.println("Sum HC "+Arrays.toString(sumsHC));
		}

		double FCentropy = 0;
		for (int fc = 0; fc < clustering.size()+1; fc++){
			double weight = sumsFC[fc]/(double)n;
			if(weight > 0)
				FCentropy+= weight * Math.log10(weight);
		}
		FCentropy/=(-1*Math.log10(clustering.size()+1));
		if(debug){
			System.out.println("FC entropy "+FCentropy);
		}

		double GTentropy = 0;
		for (int hc = 0; hc < trueClustering.size()+1; hc++){
			double weight = sumsHC[hc]/(double)n;
			if(weight > 0)
				GTentropy+= weight * Math.log10(weight);
		}
		GTentropy/=(-1*Math.log10(trueClustering.size()+1));
		if(debug){
			System.out.println("GT entropy "+GTentropy);
		}


		//cluster based entropy
		double FCcrossEntropy = 0;
		for (int fc = 0; fc < clustering.size()+1; fc++){
			double e = 0;
			if(sumsFC[fc]>0){
				for (int hc = 0; hc < trueClustering.size()+1; hc++) {
					if(counts[hc][fc]==0) continue;
					double prob = (double)counts[hc][fc]/(double)sumsFC[fc];
					e+=prob * Math.log10(prob);
				}
				FCcrossEntropy+=((sumsFC[fc]/(double)n) * e);
			}

		}
		FCcrossEntropy/=-1*Math.log10(trueClustering.size()+1);

		addValue("FC cross entropy", 1-FCcrossEntropy);
		if(debug){
			System.out.println("FC cross entropy "+(1-FCcrossEntropy));
		}


		//class based entropy
		double GTcrossEntropy = 0;
		for (int hc = 0; hc < trueClustering.size()+1; hc++){
			double e = 0;
			if(sumsHC[hc]>0){
				for (int fc = 0; fc < clustering.size()+1; fc++) {
					if(counts[hc][fc]==0) continue;
					double prob = (double)counts[hc][fc]/(double)sumsHC[hc];
					e+=prob * Math.log10(prob);
				}
			}
			GTcrossEntropy+=((sumsHC[hc]/(double)n) * e);
		}
		GTcrossEntropy/=-1*Math.log10(trueClustering.size()+1);
		addValue("GT cross entropy", 1-GTcrossEntropy);
		if(debug){
			System.out.println("GT cross entropy "+(1-GTcrossEntropy));
		}

		double homogeneity;
		if(FCentropy == 0)
			homogeneity = 1;
		else
			homogeneity = 1 - FCcrossEntropy/FCentropy;
		//set err values for now, needs to be debugged
		if(homogeneity > 1 || homogeneity < 0)
			addValue("Homogeneity",-1);
		else
			addValue("Homogeneity",homogeneity);

		double completeness;
		if(GTentropy == 0)
			completeness = 1;
		else
			completeness = 1 - GTcrossEntropy/GTentropy;
		addValue("Completeness",completeness);

		double vmeasure = (1+beta)*homogeneity*completeness/(beta*homogeneity+completeness);
		if(Double.isNaN(vmeasure)){

		}

		if(vmeasure > 1 || homogeneity < 0)
			addValue("V-Measure",-1);
		else
			addValue("V-Measure",vmeasure);



	}




}
