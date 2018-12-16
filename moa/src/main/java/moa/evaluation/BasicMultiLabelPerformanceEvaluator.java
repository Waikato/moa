/*
 *    BasicMultiLabelPerformanceEvaluator.java
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
 *
 * Added instance based F-Measure, Recall, Precision and Accuracy
 * @author R. Sousa, J.Gama
 * @version $Revision: 2 $
 *
 */
public class BasicMultiLabelPerformanceEvaluator extends AbstractMOAObject implements MultiTargetPerformanceEvaluator {

    protected int L;

    /** running sum of accuracy */
    double sumAccuracy = 0.0;
    double sumHamming = 0.0;
    double sumAccuracy2 = 0.0;
    double sumPrecision=0.0;
    double sumRecall=0.0;
    double sumFmeasure=0.0;


    /** running number of examples */
    int sumExamples = 0;

    @Override
    public void reset() {
        sumAccuracy = 0.0;
        sumHamming = 0.0;
        sumExamples = 0;
        sumAccuracy2 = 0.0;
        sumPrecision=0.0;
        sumRecall=0.0;
        sumFmeasure=0.0;
    }

    @Override
    public void addResult(Example<Instance> example, Prediction y) {

        int sumReunion= 0;
        int sumInterse= 0;
        int sumOnesTrue=0;
        int sumOnesPred=0;

        MultiLabelInstance x = (MultiLabelInstance) example.getData();

        if (L == 0) {
            L = x.numberOutputTargets();
        }

        if (y == null) {
            System.err.print("[WARNING] Prediction is null! (Ignoring this prediction)");
        }
        else if (y.numOutputAttributes() < x.numOutputAttributes()) {
            System.err.println("[WARNING] Only "+y.numOutputAttributes()+" labels found! (Expecting "+x.numOutputAttributes()+")\n (Ignoring this prediction)");
        }
        else {
            sumExamples++;
            int correct = 0;
            for (int j = 0; j < y.numOutputAttributes(); j++) {
                int yp = (y.getVote(j,1) > y.getVote(j, 0)) ? 1 : 0;

                int y_true = (int)x.valueOutputAttribute(j);
                if (y_true == yp)
                    correct++;

                if(y_true==1 || yp==1)
                    sumReunion++;

                if(y_true==1 &&  yp==1)
                    sumInterse++;

                if(y_true==1)
                    sumOnesTrue++;

                if(yp==1)
                    sumOnesPred++;

            }

            //Accuracy by instance(Jaccard Index)
            if(sumReunion>0){
                sumAccuracy2 += (double)sumInterse/sumReunion;
            }
            else{
                sumAccuracy2+=0.0;
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

            sumHamming+=(correct/(double)L); 			// Hamming Score
            sumAccuracy += (correct == L) ? 1 : 0; 		// Exact Match
        }
    }

    @Override
    public Measurement[] getPerformanceMeasurements() {

        // gather measurements
        Measurement m[] = new Measurement[]{
                new Measurement("Exact Match", sumAccuracy/sumExamples),
                new Measurement("Accuracy", (double) sumAccuracy2/sumExamples),
                new Measurement("Hamming Score", sumHamming/sumExamples),
                new Measurement("Precision",((double) sumPrecision)/sumExamples),
                new Measurement("Recall",(double) sumRecall/sumExamples),
                new Measurement("F-Measure", (double) sumFmeasure/sumExamples),
        };

        // reset
        // reset();

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
