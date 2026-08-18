/*
 *    CandPolicy.java
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
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <b>CandPolicy </b><br>
 * The Cand policy chooses k/2 models random and k/2 models with best estimated performance for
 * training in Heros.
 */
public class CandPolicy extends AbstractOptionHandler implements Policy {

    public CandPolicy() {
        super();
    }

    protected void prepareForUseImpl(TaskMonitor taskMonitor, ObjectRepository objectRepository) {}

    @Override
    public int[] pullWithPolicy(PoolItem[] pool) {
        int[] action = new int[pool.length];
        int numTopModelsToTrain = this.numModelsToTrainOption.getValue() / 2;
        double[] performances = new double[pool.length];
        for (int i = 0; i < pool.length; i++) {
            performances[i] = pool[i].getEstimation();
        }
        // Sort performances in descending order
        List<Integer> performanceSortedIndices =
                IntStream.range(0, performances.length)
                        .boxed()
                        .sorted(
                                (i, j) ->
                                        Double.compare(
                                                performances[j], performances[i])) // descending
                        .collect(Collectors.toList());
        // Select each model only once
        List<Integer> notYetSelectedIndices = new ArrayList<>();
        for (int i = 0; i < pool.length; i++) {
            notYetSelectedIndices.add(i);
        }
        // Choose an action (half of the models chosen by best performance, other half random)
        int idx;
        for (int i = 0; i < this.numModelsToTrainOption.getValue(); i++) {
            if (i < numTopModelsToTrain) {
                idx = performanceSortedIndices.get(i);
            } else {
                idx = notYetSelectedIndices.get(this.random.nextInt(notYetSelectedIndices.size()));
            }
            notYetSelectedIndices.remove((Integer) idx); // remove value (not index)
            action[idx] = 1;
        }
        return action;
    }

    public void getDescription(StringBuilder stringBuilder, int i) {}
}
