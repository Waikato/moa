/*
 *    ZetaPolicy.java
 *    Copyright (C) 2025 University of Waikato, Hamilton, New Zealand
 *    @author Kirsten Köbschall (koebschall@uni-mainz.de)
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
package moa.classifiers.meta.heros;

import moa.classifiers.meta.heros.Heros.PoolItem;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.javacliparser.FloatOption;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;


/**
 * <b>ZetaPolicy </b><br>
 *
 * The zeta policy chooses the k models with lowest resource cost and at most $\zeta$
 * worse in their estimated performance for training in Heros.

 *  <p>Parameters:</p> <ul>
 *      <li>-z : $\zeta$ parameter</li>
 *  </ul>

 *
 */
public class ZetaPolicy extends AbstractOptionHandler implements Policy {

    public FloatOption zetaOption = new FloatOption("zeta", 'z', "Maximum predictive performance loss to save resources while training.", 0.01, 0.0, 1.0);

    private static final long serialVersionUID = 1L;

    @Override
    protected void prepareForUseImpl(TaskMonitor taskMonitor, ObjectRepository objectRepository) {

    }

    @Override
    public int[] pullWithPolicy(PoolItem[] pool) {
        int[] action = new int[pool.length];
        double[] performances = new double[pool.length];
        for (int i = 0; i < pool.length; i++) {
            performances[i] = pool[i].getEstimation();
        }
        // Sort performances in descending order
        List<Integer> performanceSortedIndices = IntStream.range(0, performances.length)
                .boxed()
                .sorted((i, j) -> Double.compare(performances[j], performances[i])) // descending
                .collect(Collectors.toList());

        for (int i = 0; i < this.numModelsToTrainOption.getValue(); i++) {
            // 1. Select model with highest performance
            int poolItemIndexBestPerf = performanceSortedIndices.get(0);
            int poolItemIndexLowerRes = poolItemIndexBestPerf;
            int poolItemSortedIndexTrain = 0;
            int jIndex = 0;
            //for (int j : performanceSortedIndices) {
            for (int ij = 1; ij < performanceSortedIndices.size(); ij++) {
                int j = performanceSortedIndices.get(ij);
                // 2. Select model with lower resource costs but within performance range
                if (performances[j] >= (1. - this.zetaOption.getValue()) * performances[poolItemIndexBestPerf]) {
                    if (pool[j].getResourceCost() < pool[poolItemIndexBestPerf].getResourceCost()) {
                        poolItemIndexLowerRes = j;
                        poolItemSortedIndexTrain = jIndex + 1;
                    }
                } else {    // Since list is sorted, there cannot be a model with a performance in this range anymore
                    break;
                }
                jIndex += 1;
            }
            // 3. Select model and remove index from performanceSortedIndices
            action[poolItemIndexLowerRes] = 1;
            // After a model is selected for training, it will be removed from performanceSortedIndices
            performanceSortedIndices.remove(poolItemSortedIndexTrain);
        }
        return action;
    }

    @Override
    public void getDescription(StringBuilder stringBuilder, int i) {

    }
}
