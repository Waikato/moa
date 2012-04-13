/*
 *    EvalUtils.java
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
package moa.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import weka.core.Instance;
import weka.core.Utils;

/**
 * Evaluation Utilities.
 * 
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public abstract class EvalUtils {

    /**
     * Convert Instance to bit array. Returns the labels of this instance as an
     * int array relevance representation.
     */
    public static final int[] toIntArray(Instance x, int L) {
        int y[] = new int[L];
        for (int j = 0; j < L; j++) {
            y[j] = (int) Math.round(x.value(j));
        }
        return y;
    }

    /**
     * Calibrate a threshold. Based upon a supplied label cardinality from the
     * training set.
     */
    public static double calibrateThreshold(ArrayList<double[]> Y, double LC_train) {
        int N = Y.size();
        ArrayList<Double> big = new ArrayList<Double>();
        for (double y[] : Y) {
            for (double y_ : y) {
                big.add(y_);
            }
        }
        Collections.sort(big);
        int i = big.size() - (int) Math.round(LC_train * (double) N);
        return Math.max(((double) (big.get(i) + big.get(Math.max(i + 1, N - 1)))) / 2.0, 0.00001);
    }

    /**
     * Calibrate thresholds. One threshold for each label, based on training-set
     * label cardinality. Assumes that each double[] in Y is of length
     * LC_train.length;
     */
    public static double[] calibrateThresholds(ArrayList<double[]> Y, double LC_train[]) {
        int L = LC_train.length;
        double t[] = new double[L];
        ArrayList<double[]> Y_[] = new ArrayList[Y.size()];
        for (double y[] : Y) {
            for (int j = 0; j < y.length; j++) {
                Y_[j].add(y);
            }
        }
        for (int j = 0; j < L; j++) {
            t[j] = calibrateThreshold(Y_[j], LC_train[j]);
        }
        return t;
    }

    /**
     * Calculate Performance Measures. Given a threshold t on a batch of
     * predicted Distributions and their corresponding TrueRelevances.
     * (Rankings.size() must equal Actuals.size()).
     */
    public static HashMap<String, Double> evaluateMultiLabel(ArrayList<double[]> Distributions, ArrayList<int[]> TrueRelevances, double t) {

        double N = Distributions.size();
        int L = Distributions.iterator().next().length;
        int fp = 0, tp = 0, tn = 0, fn = 0;
        int p_sum_total = 0, r_sum_total = 0;
        double log_loss_D = 0.0, log_loss_L = 0.0;
        int set_empty_total = 0, set_inter_total = 0;
        int exact_match = 0, one_error = 0, coverage = 0;
        double accuracy = 0.0, f1_macro_D = 0.0, f1_macro_L = 0.0;
        int hloss_total = 0;
        int[] o_tp = new int[L], o_fp = new int[L], o_fn = new int[L], o_tn = new int[L];
        int[] d_tp = new int[(int) N], d_fp = new int[(int) N], d_fn = new int[(int) N], d_tn = new int[(int) N];
        //double average_accuracy_online = 0.0;


        for (int i = 0; i < N; i++) {
            double ranking[] = Distributions.get(i);
            int actual[] = TrueRelevances.get(i);

            int pred[] = new int[actual.length];
            for (int j = 0; j < L; j++) {
                pred[j] = (ranking[j] >= t) ? 1 : 0;
            }

            // calculate
            int p_sum = 0, r_sum = 0;
            int set_union = 0;
            int set_inter = 0;
            int doc_inter = 0;
            int doc_union = 0;
            for (int j = 0; j < L; j++) {
                int p = pred[j];
                int R = actual[j];
                if (p == 1) {
                    p_sum++;
                    // predt 1, real 1
                    if (R == 1) {
                        r_sum++;
                        tp++;
                        o_tp[j]++;          // f1 macro (L)
                        d_tp[i]++;          // f1 macro (D)
                        set_inter++;
                        set_union++;
                    } // predt 1, real 0
                    else {
                        fp++;
                        o_fp[j]++;          // f1 macro (L)
                        d_fp[i]++;          // f1 macro (D)
                        hloss_total++;
                        set_union++;
                    }
                } else {
                    // predt 0, real 1
                    if (R == 1) {
                        r_sum++;
                        fn++;
                        o_fn[j]++;          // f1 macro (L)
                        d_fn[i]++;          // f1 macro (D)
                        hloss_total++;
                        set_union++;
                    } // predt 0, real 0
                    else {
                        tn++;
                        o_tn[j]++;          // f1 macro (L)
                        d_tn[i]++;          // f1 macro (D)
                    }
                }

                // log losses: 
                log_loss_D += calcLogLoss((double) R, ranking[j], Math.log(N));
                log_loss_L += calcLogLoss((double) R, ranking[j], Math.log(L));
            }

            set_inter_total += set_inter;

            p_sum_total += p_sum;
            r_sum_total += r_sum;

            if (set_union > 0) //avoid NaN
            {
                accuracy += ((double) set_inter / (double) set_union);
            }
            //System.out.println(""+set_inter+","+set_union);

            if (p_sum <= 0) //empty set
            {
                set_empty_total++;
            }

            // exact match (eval. by example)
            if (set_inter == set_union) {
                exact_match++;
            }

            // f1 macro average by example
            if (p_sum > 0 && r_sum > 0 && set_inter > 0) {
                double prec = (double) set_inter / (double) p_sum;
                double rec = (double) set_inter / (double) r_sum;
                if (prec > 0 || rec > 0) {
                    f1_macro_D += ((2.0 * prec * rec) / (prec + rec));
                }
            }

            //one error: how many times the top ranked label is NOT in the label set
            if (actual[Utils.maxIndex(ranking)] <= 0) {
                one_error++;
            }

        }

        double fms[] = new double[L];
        for (int j = 0; j < L; j++) {
            // micro average
            if (o_tp[j] <= 0) {
                fms[j] = 0.0;
            } else {
                double prec = (double) o_tp[j] / ((double) o_tp[j] + (double) o_fp[j]);
                double recall = (double) o_tp[j] / ((double) o_tp[j] + (double) o_fn[j]);
                fms[j] = 2 * ((prec * recall) / (prec + recall));
            }
        }

        double precision = (double) set_inter_total / (double) p_sum_total;
        double recall = (double) set_inter_total / (double) r_sum_total;

        HashMap<String, Double> results = new HashMap<String, Double>();
        results.put("N", N);
        results.put("L", (double) L);
        results.put("Accuracy", (accuracy / N));
        results.put("F1_micro", (2.0 * precision * recall) / (precision + recall));
        results.put("F1_macro_D", (f1_macro_D / N));
        results.put("F1_macro_L", (Utils.sum(fms) / (double) L));
        results.put("H_loss", ((double) hloss_total / ((double) N * (double) L)));
        results.put("H_acc", 1.0 - ((double) hloss_total / ((double) N * (double) L)));
        results.put("LCard_pred", (double) p_sum_total / N);
        results.put("LCard_real", (double) r_sum_total / N);
        results.put("LCard_diff", Math.abs((((double) p_sum_total / N) - (double) r_sum_total / N)));
        results.put("Coverage", ((double) coverage / N));
        results.put("One_error", ((double) one_error / N));
        results.put("Exact_match", ((double) exact_match / N));
        results.put("ZeroOne_loss", 1.0 - ((double) exact_match / N));
        results.put("LogLossD", (log_loss_D / N));
        results.put("LogLossL", (log_loss_L / N));
        results.put("Empty", (double) set_empty_total / (double) N);
        results.put("EmptyAccuracy", (accuracy / (N - set_empty_total)));
        results.put("EmptyMacroF1", f1_macro_D / (N - (double) set_empty_total));
        //results.put("Build_time"      ,s.vals.get("Build_time"));
        //results.put("Test_time"       ,s.vals.get("Test_time"));
        //results.put("Total_time"      ,s.vals.get("Build_time") + s.vals.get("Test_time"));
        results.put("TPR", (double) tp / (double) (tp + fn));
        results.put("FPR", (double) fp / (double) (fp + tn));
        results.put("Precision", precision);
        results.put("Recall", recall);
        results.put("Threshold", t);

        return results;
    }

    /**
     * Calculate Log Loss. See Jesse Read. <i>Scalable Multi-label
     * Classification</i>. PhD thesis, University of Waikato, 2010.
     */
    public static double calcLogLoss(double R, double P, double C) {
        double ans = Math.min(Utils.eq(R, P) ? 0.0 : -((R * Math.log(P)) + ((1.0 - R) * Math.log(1.0 - P))), C);
        return (Double.isNaN(ans) ? 0.0 : ans);
    }
}
