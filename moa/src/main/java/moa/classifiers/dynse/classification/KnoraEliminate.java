/*
 *    KnoraEliminate.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Vinicius H.A Souza (alves.vinicius@ufpr.br)
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
 */

package moa.classifiers.dynse.classification;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.classifiers.Classifier;
import moa.classifiers.dynse.Dynse;
import moa.classifiers.lazy.neighboursearch.NearestNeighbourSearch;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class KnoraEliminate extends AbstractOptionHandler implements ClassificationEngine {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "KNORA-Eliminate dynamic classifier selection engine.";
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
    }

    @Override
    public void reset() {

    }

    @Override
    public double[] classify(Instance inst, NearestNeighbourSearch nnSearch,
                             Map<Instance, Dynse.MappedCompetence> competenceMap,
                             List<Classifier> pool, int k, Classifier incompleteClassifier) {

        // KNN
        Instances neighbours;
        try {
            neighbours = nnSearch.kNearestNeighbours(inst, Math.min(k, nnSearch.getInstances().numInstances()));
        } catch (Exception e) {
            // Returns zeros if the neighbour search fails.
            return new double[inst.numClasses()];
        }

        int numNeighborsCorrect = neighbours.numInstances();
        List<Classifier> competent = new ArrayList<>();
        Map<Classifier, Integer> hitMapping = buildHitMapping(neighbours, competenceMap);

        // Evaluates the incomplete separately
        if (incompleteClassifier != null) {
            int hits = 0;
            for (int i = 0; i < neighbours.numInstances(); i++) {
                Instance neighbour = neighbours.instance(i);
                double[] votes = incompleteClassifier.getVotesForInstance(neighbour);
                if (argMax(votes) == (int) neighbour.classValue()) {
                    hits++;
                }
            }
            hitMapping.put(incompleteClassifier, hits);
        }

        while (competent.isEmpty() && numNeighborsCorrect > 0) {
            for (Classifier c : pool) {
                Integer hits = hitMapping.get(c);
                if (hits != null && hits >= numNeighborsCorrect) {
                    competent.add(c);
                }
            }
            numNeighborsCorrect--;
        }

        // Combines the votes from competent classifiers.
        return combineVotes(competent, inst);
    }

    private Map<Classifier, Integer> buildHitMapping(Instances neighbours,
                                                     Map<Instance, Dynse.MappedCompetence> competenceMap) {
        Map<Classifier, Integer> hitMapping = new LinkedHashMap<>();
        for (int i = 0; i < neighbours.numInstances(); i++) {
            Dynse.MappedCompetence mappedCompetence = competenceMap.get(neighbours.instance(i));
            if (mappedCompetence == null) {
                continue;
            }
            for (Classifier classifier : mappedCompetence.getClassifiers()) {
                Integer hits = hitMapping.get(classifier);
                if (hits == null) {
                    hits = 0;
                }
                hitMapping.put(classifier, hits + 1);
            }
        }
        return hitMapping;
    }

    // Combines competent classifiers using majority voting.
    private double[] combineVotes(List<Classifier> competent, Instance inst) {
        double[] combined = new double[inst.numClasses()];
        if (competent.isEmpty()) {
            int randomClass = ThreadLocalRandom.current().nextInt(inst.numClasses());
            combined[randomClass] = 1.0;
            return combined;
        }
        for (Classifier c : competent) {
            double[] votes = c.getVotesForInstance(inst);
            if (votes.length > 0) {
                int maxIndex = argMax(votes);
                for (int i = 0; i < votes.length; i++) {
                    if (votes[i] == votes[maxIndex]) {
                        combined[i]++;
                    }
                }
            }
        }

        double[] result = new double[inst.numClasses()];
        result[argMax(combined)] = 1.0;
        return result;
    }

    // Returns the index of the largest value in the array.
    private int argMax(double[] votes) {
        int best = 0;
        for (int i = 1; i < votes.length; i++) {
            if (votes[i] > votes[best]) best = i;
        }
        return best;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }
}
