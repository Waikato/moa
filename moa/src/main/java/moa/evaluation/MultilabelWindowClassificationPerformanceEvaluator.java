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
import java.util.HashMap;
import moa.core.Measurement;
import moa.core.utils.EvalUtils;
import weka.core.Instance;

/**
 * Multilabel Window Classification Performance Evaluator.
 * 
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public class MultilabelWindowClassificationPerformanceEvaluator extends WindowClassificationPerformanceEvaluator {

    //ArrayList<Pair<double[],int[]>> result = new ArrayList<Pair<double[],int[]>>();
    ArrayList<double[]> result_pred = new ArrayList<double[]>();

    ArrayList<int[]> result_real = new ArrayList<int[]>();

    // We have to keep track of Label Cardinality for thresholding.
    private double LC = -1.0;

    @Override
    public void reset() {
        result_pred = new ArrayList<double[]>(widthOption.getValue());
        result_real = new ArrayList<int[]>(widthOption.getValue());
    }

    @Override
    public void reset(int L) {
        numClasses = L;
        result_pred = new ArrayList<double[]>(widthOption.getValue());
        result_real = new ArrayList<int[]>(widthOption.getValue());
    }

    /**
     * Add a Result. NOTE: In theory, the size of y[] could change, although we
     * do not take into account this possibility *yet*. (for this, we would have
     * to use y[] differently, another format for y[] e.g. HashMap, or store
     * more info in x)
     */
    @Override
    public void addResult(Instance x, double[] y) {
        if (y.length <= 2) {
            System.err.println("y.length too short (" + y.length + "). We've lost track of L at some point, unable to continue");
            System.exit(1);
        }
        // add to the current evaluation window
        result_real.add(EvalUtils.toIntArray(x, y.length));
        result_pred.add(y);
    }

    @Override
    public Measurement[] getPerformanceMeasurements() {

        // calculate threshold
        double t = 0.5;
        try {
            t = (LC > 0.0) ? EvalUtils.calibrateThreshold(result_pred, LC) : 0.5;
        } catch (Exception e) {
            System.err.println("Warning: failed to calibrate threshold, continuing with default: t = " + t);
            e.printStackTrace();
        }

        // calculate performance
        HashMap<String, Double> result = EvalUtils.evaluateMultiLabel(result_pred, result_real, t);

        // gather measurements
        Measurement m[] = new Measurement[]{
            new Measurement("Subset Accuracy", result.get("Accuracy")),
            new Measurement("Exact Match", result.get("Exact_match")),
            new Measurement("Hamming Accucaracy", result.get("H_acc")),
            new Measurement("Log Loss_D", result.get("LogLossD")),
            //new Measurement( "F1-macro_L", result.get("F1_macro_L")),
            //new Measurement( "LCard_real", result.get("LCard_real")),
            //new Measurement( "LCard_pred", result.get("LCard_pred")),
            new Measurement("Threshold", result.get("Threshold")), //new Measurement( "L", result.get("L")),
        //new Measurement( "N", result.get("N")),
        };

        // save label cardinality for the next window
        LC = result.get("LCard_real");

        // reset
        reset();

        return m;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        sb.append("Multi-label Window Classification Performance Evaluator");
    }
}
