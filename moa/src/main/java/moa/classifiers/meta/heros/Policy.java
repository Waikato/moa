/*
 *    Policy.java
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

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;

import moa.classifiers.meta.heros.Heros.PoolItem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <b>Policy </b><br>
 * A policy decides which k models to choose for training in Heros.
 *
 * <p>Parameters:
 *
 * <ul>
 *   <li>-k : Number of models to train
 *   <li>-e : Probability to choose a random k models
 * </ul>
 */
public interface Policy {

    IntOption numModelsToTrainOption =
            new IntOption(
                    "numModelsToTrain", 'k', "Number of models to train.", 1, 0, Integer.MAX_VALUE);
    FloatOption epsilonOption =
            new FloatOption(
                    "epsilon", 'e', "Probability to choose a random action.", 0.1, 0.0, 1.0);
    Random random = new Random();

    default int[] pull(PoolItem[] pool) {
        if (this.numModelsToTrainOption.getValue() <= 0
                | this.numModelsToTrainOption.getValue() > pool.length) {
            throw new ArrayIndexOutOfBoundsException(
                    "The number of models to train by the policy must be greater than 0 and smaller"
                            + " than the pool size.");
        }
        double p = random.nextDouble();
        int[] action;
        if (p < this.epsilonOption.getValue()) {
            action = new int[pool.length]; // default all to 0
            Set<Integer> indices =
                    this.getDistinctRandomIndices(
                            pool.length, this.numModelsToTrainOption.getValue());
            for (int idx : indices) {
                action[idx] = 1;
            }
        } else {
            action = this.pullWithPolicy(pool);
        }
        return action;
    }

    default int[] pullWithPolicy(PoolItem[] pool) {
        return null;
    }

    default Set<Integer> getDistinctRandomIndices(int poolSize, int numModelsToTrain) {
        Random randNum = new Random();
        Set<Integer> set = new LinkedHashSet<>();
        while (set.size() < numModelsToTrain) {
            set.add(randNum.nextInt(poolSize));
        }
        return set;
    }

    default int[] extractArgMax(double[] values) {
        int[] action = new int[values.length];
        List<Integer> sortedIndices =
                IntStream.range(0, values.length)
                        .boxed()
                        .sorted((i, j) -> Double.compare(values[j], values[i])) // descending
                        .collect(Collectors.toList());
        int idx;
        for (int i = 0; i < this.numModelsToTrainOption.getValue(); i++) {
            idx = sortedIndices.get(i);
            action[idx] = 1;
        }
        return action;
    }
}
