package moa.classifiers.meta.policy;

import moa.classifiers.meta.Heros.PoolItem;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CandPolicy extends AbstractOptionHandler implements Policy {

    public CandPolicy() {
        super();
    }

    protected void prepareForUseImpl(TaskMonitor taskMonitor, ObjectRepository objectRepository) {
    }

    @Override
    public int[] pullWithPolicy(PoolItem[] pool) {
        int[] action = new int[pool.length];
        int numTopModelsToTrain = this.numModelsToTrainOption.getValue() / 2;
        double[] performances = new double[pool.length];
        for (int i = 0; i < pool.length; i++) {
            performances[i] = pool[i].getEstimation();
        }
        // Sort performances in descending order
        List<Integer> performanceSortedIndices = IntStream.range(0, performances.length)
                .boxed()
                .sorted((i, j) -> Double.compare(performances[j], performances[i])) // descending
                .collect(Collectors.toList());
        // Select each model only once
        List<Integer> notYetSelectedIndices = new ArrayList<>();
        for (int i = 0; i<pool.length; i++) {
            notYetSelectedIndices.add(i);
        }
        // Choose an action (half of the models chosen by best performance, other half random)
        int idx;
        for (int i=0; i<this.numModelsToTrainOption.getValue(); i++) {
            if (i < numTopModelsToTrain) {
                idx = performanceSortedIndices.get(i);
            } else {
                idx = notYetSelectedIndices.get(this.random.nextInt(notYetSelectedIndices.size()));
            }
            notYetSelectedIndices.remove((Integer)idx);     // remove value (not index)
            action[idx] = 1;
        }
        return action;
    }

    public void getDescription(StringBuilder stringBuilder, int i) {

    }
}
