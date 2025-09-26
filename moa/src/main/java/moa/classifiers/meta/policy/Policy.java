package moa.classifiers.meta.policy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.classifiers.meta.Heros.PoolItem;

/**
 * <b>Policy </b><br>
 *
 * A policy decides which k models to choose for training in Heros.

 *  <p>Parameters:</p> <ul>
 *      <li>-k : Number of models to train</li>
 *      <li>-e : Probability to choose a random k models</li>
 *  </ul>

 *
 */
public interface Policy {

    IntOption numModelsToTrainOption = new IntOption("numModelsToTrain", 'k', "Number of models to train.", 1, 0, Integer.MAX_VALUE);
    FloatOption epsilonOption = new FloatOption("epsilon", 'e', "Probability to choose a random action.", 0.1, 0.0, 1.0);
    Random random = new Random();

    default int[] pull(PoolItem[] pool) {
        if (this.numModelsToTrainOption.getValue() <= 0 | this.numModelsToTrainOption.getValue() > pool.length) {
            throw new ArrayIndexOutOfBoundsException("The number of models to train by the policy must be greater than 0 and smaller than the pool size.");
        }
        double p = random.nextDouble();
        int[] action;
        if (p < this.epsilonOption.getValue()) {
            action = new int[pool.length]; // default all to 0
            Set<Integer> indices = this.getDistinctRandomIndices(pool.length, this.numModelsToTrainOption.getValue());
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
        List<Integer> sortedIndices = IntStream.range(0, values.length)
                .boxed()
                .sorted((i, j) -> Double.compare(values[j], values[i])) // descending
                .collect(Collectors.toList());
        int idx;
        for (int i=0; i<this.numModelsToTrainOption.getValue(); i++) {
            idx = sortedIndices.get(i);
            action[idx] = 1;
        }
        return action;
    }

}
