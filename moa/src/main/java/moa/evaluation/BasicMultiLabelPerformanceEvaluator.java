/*
 *    ClassificationPerformanceEvaluator.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jesse@tsc.uc3m.es)
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
package moa.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import moa.core.Example;
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

public class BasicMultiLabelPerformanceEvaluator extends WindowClassificationPerformanceEvaluator implements MultiLabelPerformanceEvaluator {

	/** running sum of accuracy */
	double sumAccuracy = 0.0;
	double sumHamming = 0.0;
    double sumPrecision=0.0;
    double sumRecall=0.0;
    double sumFmeasure=0.0;
    int sumExactMatch=0;

	/** running number of examples */
	int sumExamples = 0;

	/** preset threshold */
	private double t = 0.5;

	@Override
	public void reset() {
		sumAccuracy = 0.0;
		sumHamming = 0.0;
		sumExamples = 0;
	    sumPrecision=0.0;
	    sumRecall=0.0;
	    sumFmeasure=0.0;
	    sumExactMatch=0;
	}

	@Override
	public void reset(int L) {
		numClasses = L;
		reset();
	}

	/**
	 * Add a Result. NOTE: In theory, the size of y[] could change, although we
	 * do not take into account this possibility *yet*. (for this, we would have
	 * to use y[] differently, another format for y[] e.g. HashMap, or store
	 * more info in x)
	 *
	 * Added instance based F-Measure, Recall, Precision and Accuracy
	 * @ author R. Sousa, J.Gama
	 *
	 */
	@Override
	public void addResult(Example<Instance> example, double[] p_y) {

        int sumReunion= 0;
        int sumInterse= 0;
        int sumOnesTrue=0;
        int sumOnesPred=0;
	
		int L = p_y.length;

		Instance x = example.getData();
		if (p_y.length < 2) {
			System.err.println("FATAL ERROR: Not enough labels, we've lost track of the number of labels.");
			System.exit(1);
		}


		// Threshold to binary output (optional)
		int y[] = new int[L];
		for(int j = 0; j < L; j++) {
			y[j] = (p_y[j] > t) ? 1 : 0;
		}


		sumExamples++;
		int correct = 0;
		for(int j = 0; j < y.length; j++) { 
			int y_true = (int)x.valueOutputAttribute(j);
			if (y_true == y[j])
				correct++;
			
            if(y_true==1 || y[j]==1)
                sumReunion++;
                
            if(y_true==1 &&  y[j]==1)
                sumInterse++;
            
            if(y_true==1)
                sumOnesTrue++;
             
             if(y[j]==1)
                sumOnesPred++;
		}

	    	// Hamming Score
		    sumHamming+=(correct/(double)L);
		
        double tmp=0;
        
        //Accuracy by instance(Jaccard Index)
        if(sumReunion>0){
            tmp=(double)sumInterse/sumReunion;
            sumAccuracy += (double)sumInterse/sumReunion;
        }
        else{
            tmp=1;
            sumAccuracy+=0.0;
        }

        //Precision by instance
        if(sumOnesTrue>0){
            sumPrecision+= (double) sumInterse/sumOnesTrue;
        }

        //Recall by instance                       
        if(sumOnesPred>0){
            sumRecall+= (double)(sumInterse/sumOnesPred);
        }
        
        //F-Measure by instance 
        if((sumOnesPred+sumOnesTrue)>0){
            sumFmeasure+= (double) 2*sumInterse/(sumOnesPred+sumOnesTrue);
        }
        else{
            sumFmeasure+=0.0;
        }
	    	
        // Exact Match
		    if (correct == L)
			    sumExactMatch++;
		
	}


	@Override
	public void addResult(Example<Instance> example, Prediction prediction) {

		MultiLabelInstance inst = (MultiLabelInstance) example.getData();
		if (inst.weight() > 0.0) {
			int numberOutputs = inst.numOutputAttributes();
			if (numberOutputs <= 1) {
				System.err.println("FATAL ERROR: This is not a multi-label dataset!");
				System.exit(1);
			}

			double[] result = new double[numberOutputs];
			//prediction==null should only occur  the first example. Make zero prediction
			if(prediction!=null){
				if (prediction.numOutputAttributes()==0) {
					System.err.println("FATAL ERROR: This is not a multi-label prediction!");
					System.exit(1);
				}
				for (int i = 0; i< prediction.size();i++){
					result[i] = prediction.getVote(i,0);
				}
			}
			addResult(example, result);
		}
		//System.out.println(inst.classValue()+", "+prediction);
	}

	@Override
	public Measurement[] getPerformanceMeasurements() {

		// gather measurements
		Measurement m[] = new Measurement[]{
				new Measurement("Exact Match", ((double)sumExactMatch)/sumExamples),
				new Measurement("Hamming Score", sumHamming/sumExamples),
                new Measurement("Accuracy", (double) sumAccuracy/sumExamples),
                new Measurement("Precision",((double) sumPrecision)/sumExamples),
                new Measurement("Recall",(double) sumRecall/sumExamples),
                new Measurement("F-Measure", (double) sumFmeasure/sumExamples),
		};

		// reset
		reset();

		return m;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		sb.append("Multi-label Window Classification Performance Evaluator");
	}
}
