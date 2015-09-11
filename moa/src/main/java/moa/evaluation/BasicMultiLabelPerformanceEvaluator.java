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

import moa.AbstractMOAObject;
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
public class BasicMultiLabelPerformanceEvaluator extends AbstractMOAObject implements MultiTargetPerformanceEvaluator {

	protected int L;

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
	public void addResult(Example<Instance> example, Prediction y) {

		MultiLabelInstance x = (MultiLabelInstance) example.getData();

		if (L == 0) {
			L = x.numberOutputTargets();
		}

		if (y != null && (y.numOutputAttributes() != 0)) {
			sumExamples++;
			int correct = 0;
			for (int j = 0; j< L; j++){
				int yp = (y.getVote(j,1) > t) ? 1 : 0;
				correct += ((int)x.classValue(j) == yp) ? 1 : 0;
			}
			// Hamming Score
			sumHamming+=(correct/(double)L);
			// Exact Match
			sumAccuracy += (correct == L) ? 1 : 0;
		}
		else {
			System.err.println("[WARNING]: Not a multi-label prediction! Continuing ...");
			if (y != null) {
				System.err.println(""+y);
				System.err.println(""+y.numOutputAttributes());
			}
			else
				System.err.println("Prediction is null!");
		}

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
        sb.append("Basic Multi-label Performance Evaluator");
    }

	@Override
	public void addResult(Example<Instance> example, double[] classVotes) {
		// TODO Auto-generated method stub
		
	}
}
