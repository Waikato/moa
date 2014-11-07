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

/**
 * Multilabel Window Classification Performance Evaluator.
 * 
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public class MultilabelWindowClassificationPerformanceEvaluator extends WindowClassificationPerformanceEvaluator implements MultiTargetPerformanceEvaluator {

	/** running sum of accuracy */
    double sumAccuracy = 0.0;
	double sumHamming = 0.0;

	/** running number of examples */
    int sumExamples = 0;

	/** preset threshold */
    private double t = 0.5;

    @Override
    public void reset() {
		sumAccuracy = 0.0;
		sumHamming = 0.0;
		sumExamples = 0;
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
     */
    @Override
    public void addResult(Example<Instance> example, double[] p_y) {

		//int L = example.numOutputAttributes();      // <-- doesn't work!
		int L = p_y.length;

        Instance x = example.getData();
        if (p_y.length < 2) {
            System.err.println("FATAL ERROR: Not enough labels, we've lost track of the number of labels.");
            System.exit(1);
        }

		//System.out.println("------- new result -------------");
		//System.out.println("x = "+x);
		//System.out.println("p(y) = "+Arrays.toString(p_y));

		// Threshold to binary output (optional)
		int y[] = new int[L];
		for(int j = 0; j < L; j++) {
			y[j] = (p_y[j] > t) ? 1 : 0;
		}
		//System.out.println("y =    "+Arrays.toString(y));
		
		sumExamples++;
		int correct = 0;
		for(int j = 0; j < y.length; j++) {
			//int y_true = //]example.valueOutputAttribute(j);  // <-- doesn't work!
			int y_true = (int)x.value(j); 
			//int y_pred = (p_y[j] > t) ? 1 : 0;
			if (y_true == y[j])
				correct++;
		}

		// Hamming Score
		sumHamming+=(correct/(double)L);

		// Exact Match
		if (correct == L)
			sumAccuracy++;
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
			if (prediction.numOutputAttributes()==0) {
				System.err.println("FATAL ERROR: This is not a multi-label prediction!");
				System.exit(1);
			}
            double[] result = new double[numberOutputs];
            for (int i = 0; i< prediction.size();i++){
				result[i] = prediction.getVote(i,1); 
            }
            addResult(example, result);
        }
            //System.out.println(inst.classValue()+", "+prediction);
    }

    @Override
    public Measurement[] getPerformanceMeasurements() {

        // gather measurements
        Measurement m[] = new Measurement[]{
            new Measurement("Exact Match", sumAccuracy/sumExamples),
			new Measurement("Hamming Score", sumHamming/sumExamples),
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
