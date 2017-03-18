/*
 *    IademNumericAttributeObserver.java
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

import java.util.ArrayList;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;

public interface IademNumericAttributeObserver extends AttributeClassObserver {
    
    public long getMaxOfValues();
    
    public void addValue(double attValue, int classValue, double weight);
    
    public long getValueCount();
    
    public long[] getClassDist();
    
    public long getNumberOfCutPoints();
    
    public long[] getLeftClassDist(double cut);
    
    public double getCut(int index);
    
    public void computeClassDistProbabilities(double[][][] inf_p_corte_valor_z,
            double[][][] sup_p_corte_valor_z,
            double[][] n_corte_valor,
            boolean withIntervalEstimates);
    
    public void computeClassDist(double[][][] cutClassDist);
    
    public ArrayList<Double> cutPointSuggestion(int numCortes);
    
    public ArrayList<Double[]> computeConditionalProbPerBin(ArrayList<Double> cuts);
    
    public double[] computeConditionalProb(ArrayList<Double> cuts, double value);

    public void reset();

    public void setMaxBins(int numIntervalos);
    
    @Deprecated
    public IademNumericAttributeObserver getCopy();
}
