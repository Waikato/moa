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

package moa.learners.predictors.trees.iadem;

import java.util.ArrayList;

import moa.learners.predictors.core.attributeclassobservers.AttributeClassObserver;

public interface IademNumericAttributeObserver extends AttributeClassObserver {

	long getMaxOfValues();

	void addValue(double attValue, int classValue, double weight);

	long getValueCount();

	long[] getClassDist();

	long getNumberOfCutPoints();

	long[] getLeftClassDist(double cut);

	double getCut(int index);

	void computeClassDistProbabilities(double[][][] inf_p_corte_valor_z, double[][][] sup_p_corte_valor_z,
			double[][] n_corte_valor, boolean withIntervalEstimates);

	void computeClassDist(double[][][] cutClassDist);

	ArrayList<Double> cutPointSuggestion(int numCortes);

	ArrayList<Double[]> computeConditionalProbPerBin(ArrayList<Double> cuts);

	double[] computeConditionalProb(ArrayList<Double> cuts, double value);

	void reset();

	void setMaxBins(int numIntervalos);

	@Deprecated
	IademNumericAttributeObserver getCopy();
}
