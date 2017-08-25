/*
 *    IademGreenwaldKhannaNumericAttributeClassObserver.java
 *
 *    @author Isvani Frias-Blanco
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

import java.util.Arrays;
import java.util.ArrayList;
import moa.classifiers.core.attributeclassobservers.GreenwaldKhannaNumericAttributeClassObserver;
import moa.core.AutoExpandVector;
import moa.core.GreenwaldKhannaQuantileSummary;
import weka.core.Utils;

public class IademGreenwaldKhannaNumericAttributeClassObserver extends GreenwaldKhannaNumericAttributeClassObserver implements IademNumericAttributeObserver {
    private static final long serialVersionUID = 1L;

    public IademGreenwaldKhannaNumericAttributeClassObserver() {
        super();
    }
    
    public IademGreenwaldKhannaNumericAttributeClassObserver(int maxTuples) {
        super();
        this.numTuplesOption.setValue(maxTuples);
    }

    @Override
    public void observeAttributeClass(double attVal, int classVal, double weight) {
        if (Utils.isMissingValue(attVal)) {
        } else {
            IademGreenwaldKhannaQuantileSummary valDist = (IademGreenwaldKhannaQuantileSummary) this.attValDistPerClass.get(classVal);
            if (valDist == null) {
                valDist = new IademGreenwaldKhannaQuantileSummary(this.numTuplesOption.getValue());
                this.attValDistPerClass.set(classVal, valDist);
            }
            // TODO: not taking weight into account
            valDist.insert(attVal);
        }
    }

    @Override
    public double probabilityOfAttributeValueGivenClass(double attVal, int classVal) {
        IademGreenwaldKhannaQuantileSummary obs = (IademGreenwaldKhannaQuantileSummary) this.attValDistPerClass.get(classVal);
        if (obs == null) {
            return 0.0;
        } else {
            int index = obs.findIndexOfTupleGreaterThan(attVal);
            double total = obs.getTotalCount();
            double partial = obs.maxNumberOfObservation(index);
            return total != 0 ? partial / total : 0.0;
        }
    }

    @Override
    public long getMaxOfValues() {
        return this.numTuplesOption.getValue();
    }

    @Override
    public void addValue(double attValue, int classValue, double weight) {
        observeAttributeClass(attValue, classValue, weight);
    }

    @Override
    public long getValueCount() {
        int totalCount = 0;
        for (GreenwaldKhannaQuantileSummary qs : this.attValDistPerClass) {
            if (qs != null) {
                totalCount += qs.getTotalCount();
            }
        }
        return totalCount;
    }

    @Override
    public long[] getClassDist() {
        long[] classDist = new long[this.attValDistPerClass.size()];
        for (int i = 0; i < this.attValDistPerClass.size(); i++) {
            GreenwaldKhannaQuantileSummary qs = this.attValDistPerClass.get(i);
            if (qs != null) {
                classDist[i] = qs.getTotalCount();
            } else {
                classDist[i] = 0;
            }
        }
        return classDist;
    }

    @Override
    public long getNumberOfCutPoints() {
        int numberOfCutPoints = 0;
        for (GreenwaldKhannaQuantileSummary qs : this.attValDistPerClass) {
            if (qs != null) {
                numberOfCutPoints += qs.getSuggestedCutpoints().length;
            }
        }
        return numberOfCutPoints;
    }

    @Override
    public long[] getLeftClassDist(double cut) {
        long[] lhsDist = new long[this.attValDistPerClass.size()];
        Arrays.fill(lhsDist, 0);
        for (int i = 0; i < this.attValDistPerClass.size(); i++) {
            GreenwaldKhannaQuantileSummary estimator = this.attValDistPerClass.get(i);
            if (estimator != null) {
                long countBelow = estimator.getCountBelow(cut);
                lhsDist[i] += countBelow;
            }
        }
        return lhsDist;
    }

    @Override
    public double getCut(int cutIndex) {
        int currentCut = cutIndex;
        for (GreenwaldKhannaQuantileSummary qs : this.attValDistPerClass) {
            if (qs != null) {
                double[] cuts = qs.getSuggestedCutpoints();
                if (currentCut < cuts.length) {
                    return cuts[currentCut];
                }
                currentCut -= cuts.length;
            }
        }
        return Double.NaN;
    }

    @Override
    public void computeClassDistProbabilities(double[][][] cut_value_classDist_lower, 
            double[][][] cut_value_classDist_upper, 
            double[][] counts_cut_value,
            boolean withIntervalEstimates) {
        ArrayList<Double> cuts = cutPointSuggestion(-1);
        long [] totalDist = getClassDist();
        for (int i = 0; i < cuts.size(); i++) {
            long [] lDist = getLeftClassDist(cuts.get(i)),
                    rDist = new long[lDist.length];
            long totalIzq = sum(lDist);
            long total = sum(totalDist);
            counts_cut_value[i][0] = totalIzq;
            counts_cut_value[i][1] = total - totalIzq;
            for (int j = 0; j < totalDist.length; j++) {
                rDist[j] = totalDist[j] - lDist[j];
                double lEst = 0.0;
                if (counts_cut_value[i][0] != 0) {
                    lEst = (double) lDist[j] / counts_cut_value[i][0];
                }
                double lError = 0.0;
                if (withIntervalEstimates) {
                    lError = IademCommonProcedures.getIADEM_HoeffdingBound(lEst, counts_cut_value[i][0]);
                }
                cut_value_classDist_lower[i][0][j] = Math.max(0.0, lEst - lError);
                cut_value_classDist_upper[i][0][j] = Math.min(1.0, lEst + lError);
                
                double rEst = 0.0;
                if (counts_cut_value[i][1] != 0) {
                    rEst = (double) rDist[j] / counts_cut_value[i][1];
                }
                double rightError = 0.0;
                if (withIntervalEstimates) {
                    rightError = IademCommonProcedures.getIADEM_HoeffdingBound(rEst, counts_cut_value[i][1]);
                }
                cut_value_classDist_lower[i][1][j] = Math.max(0.0, rEst - rightError);
                cut_value_classDist_upper[i][1][j] = Math.min(1.0, rEst + rightError);
            }
        }
    }
    
    protected long sum(long []arr) {
        long counter = 0;
        for (int i = 0; i < arr.length; i++) {
            counter += arr[i];
        }
        return counter;
    }

    @Override
    public ArrayList<Double> cutPointSuggestion(int numCortes) {
        ArrayList<Double> cuts = new ArrayList<Double>();
        for (GreenwaldKhannaQuantileSummary qs : this.attValDistPerClass) {
            if (qs != null) {
                double[] newCuts = qs.getSuggestedCutpoints();
                for (double valor : newCuts) {
                    cuts.add(valor);
                }
            }
        }
        return cuts;
    }

    @Override
    public ArrayList<Double[]> computeConditionalProbPerBin(ArrayList<Double> cuts) {
        ArrayList<Double []> prob = new ArrayList<Double []>();
        long total = getValueCount();
        for (Double currentCut : cuts) {
            long[] numExp = getLeftClassDist(currentCut);
            Double [] tmpProb = new Double[numExp.length];
            for (int j = 0; j < tmpProb.length; j++) {
                tmpProb[j] = (double) numExp[j] / total;
            }
            prob.add(tmpProb);
        }
        return prob;
    }

    @Override
    public double[] computeConditionalProb(ArrayList<Double> cortes, double valor) {
        double [] probabilidadCondicional = new double[this.attValDistPerClass.size()];
        for (int i = 0; i < this.attValDistPerClass.size(); i++) {
            probabilidadCondicional[i] = probabilityOfAttributeValueGivenClass(valor, i);
        }
        return probabilidadCondicional;
    }

    @Override
    public IademNumericAttributeObserver getCopy() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void reset() {
        this.attValDistPerClass = new AutoExpandVector<GreenwaldKhannaQuantileSummary>();
    }
    
    @Override
    public void setMaxBins(int numTuples) {
        this.numTuplesOption.setValue(numTuples);
    }

    @Override
    public void computeClassDist(double[][][] cutClassDist) {
        ArrayList<Double> cuts = cutPointSuggestion(-1);
        long [] totalDist = getClassDist();
        for (int i = 0; i < cuts.size(); i++) {
            long [] lDist = getLeftClassDist(cuts.get(i)),
                    rDist = new long[lDist.length];
            for (int j = 0; j < totalDist.length; j++) {
                rDist[j] = totalDist[j] - lDist[j];
                
                cutClassDist[i][0][j] = lDist[j];
                cutClassDist[i][1][j] = rDist[j];
            }
        }
    }
}