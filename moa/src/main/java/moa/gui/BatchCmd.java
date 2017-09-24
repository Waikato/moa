/**
 * BatchCmd.java
 * 
 * @author Timm Jansen (moa@cs.rwth-aachen.de)
 * @editor Yunsu Kim
 * 
 * Last edited: 2013/06/02
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

package moa.gui;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import moa.cluster.Clustering;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.ClusterGenerator;
import moa.clusterers.clustream.WithKmeans;
import moa.evaluation.CMM;
import moa.evaluation.EntropyCollection;
import moa.evaluation.F1;
import moa.evaluation.General;
import moa.evaluation.MeasureCollection;
import moa.evaluation.SSQ;
import moa.evaluation.Separation;
import moa.evaluation.SilhouetteCoefficient;
import moa.evaluation.StatisticalCollection;
import moa.gui.visualization.DataPoint;
import moa.gui.visualization.RunVisualizer;
import moa.streams.clustering.ClusterEvent;
import com.yahoo.labs.samoa.instances.Instance;
import moa.streams.clustering.ClusterEventListener;
import moa.streams.clustering.ClusteringStream;
import moa.streams.clustering.RandomRBFGeneratorEvents;
import com.yahoo.labs.samoa.instances.DenseInstance;

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
		if(totalInstances == -1)
			this.totalInstances = Integer.MAX_VALUE;
		else
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
	private static ArrayList<Class> getMeasureSelection(boolean[] selection){
		ArrayList<Class>mclasses = new ArrayList<Class>();
		
		if(selection[0])
			mclasses.add(General.class);
		if(selection[1])
			mclasses.add(F1.class);
		if(selection[2])
			mclasses.add(EntropyCollection.class);
		if(selection[3])
			mclasses.add(CMM.class);
		if(selection[4])
			mclasses.add(SSQ.class);
		if(selection[5])
			mclasses.add(Separation.class);
		if(selection[6])
			mclasses.add(SilhouetteCoefficient.class);
		if(selection[7])
			mclasses.add(StatisticalCollection.class);

		return mclasses;
	}


	/* TODO read args from command line */
	public static void main(String[] args){
		RandomRBFGeneratorEvents stream = new RandomRBFGeneratorEvents();
		AbstractClusterer clusterer = new WithKmeans();
		boolean[] measureCollection = {true,true,true,true,true,true,true,true};
		int amountInstances = 20000;
		String testfile = "d:\\data\\test.csv";

		runBatch(stream, clusterer, measureCollection, amountInstances, testfile);
	}


	public static void runBatch(ClusteringStream stream, AbstractClusterer clusterer,
			boolean[] measureCollection, int amountInstances, String outputFile){
		// create the measure collection 
		MeasureCollection[] measures = getMeasures(getMeasureSelection(measureCollection));
		
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
			Instance next = stream.nextInstance().getData();
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
				for(int j = 0 ; j < m.getNumMeasures() ; j++)
				{
					m.setEnabled(j, true);
				}
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
			// Prepare an output file			
			if (!filepath.endsWith(".csv")) {
				filepath += ".csv";
			}
			out = new PrintWriter(new BufferedWriter(new FileWriter(filepath)));
			
			
			String delimiter = ";";

			// Header
			int numValues = 0;
			out.write("Nr" + delimiter);
			out.write("Event" + delimiter);
			for (int m = 0; m < 1; m++) {	// TODO: Multiple group of measures
				for (int i = 0; i < measures.length; i++) {
					for (int j = 0; j < measures[i].getNumMeasures(); j++) {
						if (measures[i].isEnabled(j)) {
							out.write(measures[i].getName(j) + delimiter);
							numValues = measures[i].getNumberOfValues(j);
						}
					}
				}
			}
			out.write("\n");

			// Rows
			Iterator<ClusterEvent> eventIt = null;
			ClusterEvent event = null;
			if (clusterEvents != null) {
				if (clusterEvents.size() > 0) {
					eventIt = clusterEvents.iterator();
					event = eventIt.next();
				}
			}
			for (int v = 0; v < numValues; v++){
				// Nr
				out.write(v + delimiter);
				// Events
				if (event != null && event.getTimestamp() <= ((v+1) * horizon)) {
					out.write(event.getType() + delimiter);
					if (eventIt != null && eventIt.hasNext()) {
						event = eventIt.next();
					} else {
						event = null;
					}
				} else {
					out.write(delimiter);
				}

				// Values
				for (int m = 0; m < 1; m++) {	// TODO: Multiple group of measures
					for (int i = 0; i < measures.length; i++) {
						for (int j = 0; j < measures[i].getNumMeasures(); j++) {
							if (measures[i].isEnabled(j)) {
								out.write(measures[i].getValue(j, v) + delimiter);
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

