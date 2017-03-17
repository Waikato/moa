/*
 *    IademCommonProcedures.java
 *
 *    @author José del Campo-Ávila
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

package moa.classifiers.trees.iadem;

import java.io.Serializable;
import java.util.ArrayList;

public class IademCommonProcedures implements Serializable {

    private static final long serialVersionUID = 1L;
    private static double confidence;
    private static double ln_2_div_confidence;
    private static double square_ln_2_div_confianza;
    
    public IademCommonProcedures(double confidence) {
        IademCommonProcedures.confidence = confidence;
        IademCommonProcedures.ln_2_div_confidence = Math.log(2.0 / confidence);
        IademCommonProcedures.square_ln_2_div_confianza = ln_2_div_confidence
                * ln_2_div_confidence;
    }

    public static double AverageComparitionByHoeffdingCorollary(double n, double n1, double alfa) {
        double m = 1.0 / n + 1.0 / n1;
        double bound = Math.sqrt(m * Math.log(1.0 / alfa) / 2.0);
        return bound;
    }
    
    public static double getConfidence() {
        return confidence;
    }
    
    public static void setConfidence(double confianza) {
        IademCommonProcedures.confidence = confianza;
        IademCommonProcedures.ln_2_div_confidence = Math.log(2.0 / confianza);
        IademCommonProcedures.square_ln_2_div_confianza = ln_2_div_confidence * ln_2_div_confidence;
    }

    public static double getIADEM_HoeffdingBound(double average, double n) {
        return computeBound(average, n);
    }
    
    public static double log(double base, double num) {
        return (Math.log(num) / Math.log(base));
    }
    
    public static void insertLotsHoles(ArrayList<Double> lots,
            ArrayList<Integer> holes, double inf,
            double sup) {
        if (inf != sup) {
            int currentPos = 1;
            if (inf == 0.0) {
                for (int i = 0; i < holes.size(); i++) {
                    int aux = holes.get(i);
                    aux++;
                    holes.set(i, aux);
                }
            } else {
                while (inf > lots.get(currentPos)) {
                    currentPos++;
                }
                if (inf < lots.get(currentPos)) {
                    lots.add(currentPos, inf);
                    holes.add(currentPos, (holes.get(currentPos - 1) + 1));
                } else { 
                    int aux = holes.get(currentPos);
                    aux++;
                    holes.set(currentPos, aux);
                }
                for (int i = (currentPos + 1); i < holes.size(); i++) {
                    int aux = holes.get(i);
                    aux++;
                    holes.set(i, aux);
                }
            }
            while (sup > lots.get(currentPos)) {
                currentPos++;
            }
            if (sup < lots.get(currentPos)) {
                lots.add(currentPos, sup);
                holes.add(currentPos, (holes.get(currentPos - 1) - 1));
            } else if (currentPos < holes.size()) { 
                int aux = holes.get(currentPos);
                aux--;
                holes.set(currentPos, aux);
            }
            for (int i = (currentPos + 1); i < holes.size(); i++) {
                int aux = holes.get(i);
                aux--;
                holes.set(i, aux);
            }
        }
    }
    
    public static double computeLevel(ArrayList<Double> lots, 
            ArrayList<Integer> holes, double amount) {
        double level = 0.0;
        int currentLot = 0;
        double localAmount = amount;
        double upperBound, lowerBound, nLot, fullAmount;
        if (localAmount == 0.0) {
            if ((holes.size() == 1) && (holes.get(0) == 0)) { 
                level = 0.0;
            } else {
                while ((currentLot < holes.size()) && (holes.get(currentLot) == 0)) {
                    currentLot++;
                }
                level = lots.get(currentLot);
            }
        } else {
            while (localAmount > 0.0) {
                upperBound = lots.get(currentLot + 1);
                lowerBound = lots.get(currentLot);
                nLot = holes.get(currentLot);
                fullAmount = (upperBound - lowerBound) * nLot;
                if (localAmount >= fullAmount) {
                    localAmount -= fullAmount;
                    level = lots.get(currentLot + 1);
                } else {
                    level += (localAmount / (double) holes.get(currentLot));
                    localAmount = 0.0;
                }
                currentLot++;
            }
        }
        return level;
    }

    public static double computeBound(double average,
            double n) {
        double bound;
        double hoeffdingBound;
        double chernoffBound;
        if (n == 0.0) { 
            bound = 1.0;
        } else {
            hoeffdingBound = Math.sqrt((1.0 / (2.0 * n))
                    * ln_2_div_confidence);
            if (average >= (1.0 / 6.0)) {
                bound = hoeffdingBound;
            } else {
                chernoffBound = ((3.0 * ln_2_div_confidence) + (Math.sqrt((9.0 * square_ln_2_div_confianza)
                        + (12.0 * n * average * ln_2_div_confidence)))) / (2.0 * n);
                bound = Math.min(Math.min(chernoffBound, hoeffdingBound), 1.0);
            }
        }
        return bound;
    }
}
