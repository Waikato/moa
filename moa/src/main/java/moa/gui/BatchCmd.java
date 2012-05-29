/*
 *    BatchCmd.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
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

package moa.gui;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.clusterers.ClusterGenerator;
import moa.cluster.Clustering;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.clustream.Clustream;
import moa.evaluation.F1;
import moa.evaluation.General;
import moa.evaluation.MeasureCollection;
import moa.evaluation.SSQ;
import moa.evaluation.SilhouetteCoefficient;
import moa.evaluation.StatisticalCollection;
import moa.evaluation.EntropyCollection;
import moa.gui.visualization.DataPoint;
import moa.gui.visualization.RunVisualizer;
import moa.streams.clustering.ClusterEvent;
import weka.core.Instance;
import moa.streams.clustering.ClusterEventListener;
import moa.streams.clustering.ClusteringStream;
import moa.streams.clustering.RandomRBFGeneratorEvents;
import weka.core.DenseInstance;

public class BatchCmd implements ClusterEventListener{

	private ArrayList<ClusterEvent> clusterEvents;
	private AbstractClusterer clusterer;
	private ClusteringStream stream;
	private MeasureCollection[] measures;

	private int totalInstances;
	public boolean useMicroGT = false;


	public BatchCmd(AbstractClusterer clusterer, ClusteringStream stream, MeasureCollection[] measures, int totalInstances){
		this.clusterer = clusterer;
		this.stream = stream;
		this.totalInstances = totalInstances;
		this.measures = measures;

		if(stream instanceof RandomRBFGeneratorEvents){
			((RandomRBFGeneratorEvents)stream).addClusterChangeListener(this);
			clusterEvents = new ArrayList<ClusterEvent>();
		}
		else{
			clusterEvents = null;
		}
		stream.prepareForUse();
		clusterer.prepareForUse();
	}

	private ArrayList<ClusterEvent> getEventList(){
		return clusterEvents;
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<Class> getMeasureSelection(int selection){
		ArrayList<Class>mclasses = new ArrayList<Class>();
		mclasses.add(EntropyCollection.class);
		mclasses.add(F1.class);
		mclasses.add(General.class);
		mclasses.add(SSQ.class);
		mclasses.add(SilhouetteCoefficient.class);
		mclasses.add(StatisticalCollection.class);

		return mclasses;
	}


	/* TODO read args from command line */
	public static void main(String[] args){
		RandomRBFGeneratorEvents stream = new RandomRBFGeneratorEvents();
		AbstractClusterer clusterer = new Clustream();
		int measureCollectionType = 0;
		int amountInstances = 20000;
		String testfile = "d:\\data\\test.csv";

		runBatch(stream, clusterer, measureCollectionType, amountInstances, testfile);
	}


	public static void runBatch(ClusteringStream stream, AbstractClusterer clusterer,
			int measureCollectionType, int amountInstances, String outputFile){
		// create the measure collection 
		MeasureCollection[] measures = getMeasures(getMeasureSelection(measureCollectionType));
		
		// run the batch job
		BatchCmd batch = new BatchCmd(clusterer, stream, measures, amountInstances);
		batch.run();

		// read events and horizon
		ArrayList<ClusterEvent> clusterEvents = batch.getEventList();
		int horizon = stream.decayHorizonOption.getValue();
		
		// write results to file
		exportCSV(outputFile, clusterEvents, measures, horizon);
	}


	public void run(){
		ArrayList<DataPoint> pointBuffer0 = new ArrayList<DataPoint>();
		int m_timestamp = 0;
		int decayHorizon = stream.getDecayHorizon();

		double decay_threshold = stream.getDecayThreshold();
		double decay_rate = (-1*Math.log(decay_threshold)/decayHorizon);

		int counter = decayHorizon;

		while(m_timestamp < totalInstances && stream.hasMoreInstances()){
			m_timestamp++;
			counter--;
			Instance next = stream.nextInstance();
			DataPoint point0 = new DataPoint(next,m_timestamp);
			pointBuffer0.add(point0);

			Instance traininst0 = new DenseInstance(point0);
			if(clusterer instanceof ClusterGenerator)
				traininst0.setDataset(point0.dataset());
			else
				traininst0.deleteAttributeAt(point0.classIndex());

			clusterer.trainOnInstanceImpl(traininst0);

			if(counter <= 0){
				//                if(m_timestamp%(totalInstances/10) == 0)
					//                    System.out.println("Thread"+threadID+":"+(m_timestamp*100/totalInstances)+"% ");
				for(DataPoint p:pointBuffer0)
					p.updateWeight(m_timestamp, decay_rate);

				Clustering gtClustering0;
				Clustering clustering0 = null;

				gtClustering0 = new Clustering(pointBuffer0);
				if(useMicroGT && stream instanceof RandomRBFGeneratorEvents){
					gtClustering0 = ((RandomRBFGeneratorEvents)stream).getMicroClustering();
				}

				clustering0 = clusterer.getClusteringResult();
				if(clusterer.implementsMicroClusterer()){
					if(clusterer instanceof ClusterGenerator
							&& stream instanceof RandomRBFGeneratorEvents){
						((ClusterGenerator)clusterer).setSourceClustering(((RandomRBFGeneratorEvents)stream).getMicroClustering());
					}
					Clustering microC = clusterer.getMicroClusteringResult();
					if(clusterer.evaluateMicroClusteringOption.isSet()){
						clustering0 = microC;
					}
					else{
						if(clustering0 == null && microC != null)
							clustering0 = moa.clusterers.KMeans.gaussianMeans(gtClustering0, microC);
					}
				}


				//evaluate
				for (int i = 0; i < measures.length; i++) {
					try {
						/*double sec =*/ measures[i].evaluateClusteringPerformance(clustering0, gtClustering0, pointBuffer0);
						//System.out.println("Eval of "+measures[i].getClass().getSimpleName()+" at "+m_timestamp+" took "+sec);
					} catch (Exception ex) { ex.printStackTrace(); }
				}

				pointBuffer0.clear();
				counter = decayHorizon;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static MeasureCollection[] getMeasures(ArrayList<Class> measure_classes){
		MeasureCollection[] measures = new MeasureCollection[measure_classes.size()];
		for (int i = 0; i < measure_classes.size(); i++) {
			try {
				MeasureCollection m = (MeasureCollection)measure_classes.get(i).newInstance();
				measures[i] = m;

			} catch (Exception ex) {
				Logger.getLogger("Couldn't create Instance for "+measure_classes.get(i).getName());
				ex.printStackTrace();
			}
		}
		return measures;
	}

	public void changeCluster(ClusterEvent e) {
		if(clusterEvents!=null) clusterEvents.add(e);
	}


	public static void exportCSV(String filepath, ArrayList<ClusterEvent> clusterEvents, MeasureCollection[] measures, int horizon) {
		PrintWriter out = null;
		try {
			if(!filepath.endsWith(".csv"))
				filepath+=".csv";
			out = new PrintWriter(new BufferedWriter(new FileWriter(filepath)));
			String del = ";";

			Iterator<ClusterEvent> eventIt = null;
			ClusterEvent event = null;
			if(clusterEvents.size() > 0){
				eventIt = clusterEvents.iterator();
				event = eventIt.next();
			}

			int numValues = 0;
			//header
			out.write("Nr"+del);
			out.write("Event"+del);
			for (int m = 0; m < 1; m++) {
				for (int i = 0; i < measures.length; i++) {
					for (int j = 0; j < measures[i].getNumMeasures(); j++) {
						if(measures[i].isEnabled(j)){
							out.write(measures[i].getName(j)+del);
							numValues = measures[i].getNumberOfValues(j);
						}
					}
				}
			}
			out.write("\n");


			//rows
			for (int v = 0; v < numValues; v++){
				//Nr
				out.write(v+del);

				//events
				if(event!=null && event.getTimestamp()<=horizon){
					out.write(event.getType()+del);
					if(eventIt!= null && eventIt.hasNext()){
						event=eventIt.next();
					}
					else
						event = null;
				}
				else
					out.write(del);

				//values
				for (int m = 0; m < 1; m++) {
					for (int i = 0; i < measures.length; i++) {
						for (int j = 0; j < measures[i].getNumMeasures(); j++) {
							if(measures[i].isEnabled(j)){
								out.write(measures[i].getValue(j, v)+del);
							}
						}
					}
				}
				out.write("\n");
			}
			out.close();
		} catch (IOException ex) {
			Logger.getLogger(RunVisualizer.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			out.close();
		}
	}
}

