/*
 *    Dynse.java
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

package moa.classifiers.dynse;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.dynse.classification.ClassificationEngine;
import moa.classifiers.dynse.classification.KnoraEliminate;
import moa.classifiers.dynse.pruning.AgePruningEngine;
import moa.classifiers.dynse.pruning.PruningEngine;
import moa.classifiers.lazy.neighboursearch.LinearNNSearch;
import moa.classifiers.lazy.neighboursearch.NearestNeighbourSearch;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.core.StringUtils;
import moa.options.ClassOption;

import java.util.*;

public class Dynse extends AbstractClassifier implements MultiClassClassifier, CapabilitiesHandler {

    private static final long serialVersionUID = 1L;

    public ClassOption classificationEngineOption = new ClassOption(
            "classificationEngine", 'c',
            "Classification engine used to select the classifier ensemble.",
            ClassificationEngine.class,
            KnoraEliminate.class.getName()
    );

    public ClassOption pruningEngineOption = new ClassOption(
            "pruningEngine", 'p',
            "Pruning engine used to maintain the classifier pool.",
            PruningEngine.class,
            AgePruningEngine.class.getName()
    );

    public ClassOption baseClassifierOption = new ClassOption(
            "baseClassifier", 'b',
            "Base classifier to train.",
            Classifier.class,
            "trees.HoeffdingTree"
    );

    public IntOption kOption = new IntOption(
            "k", 'k',
            "The number of nearest neighbours used to estimate classifier competence.",
            5, 1, Integer.MAX_VALUE
    );

    public IntOption mOption = new IntOption(
            "m", 'm',
            "The maximum number of instances kept in the dynamic selection evaluation dataset.",
            250, 1, Integer.MAX_VALUE
    );

    public IntOption dOption = new IntOption(
            "d", 'd',
            "The maximum number of classifiers in the pool.",
            25, 1, Integer.MAX_VALUE
    );

    public IntOption trainSizeOption = new IntOption(
            "trainSize", 'n',
            "The number of instances collected before training a new classifier.",
            100, 1, Integer.MAX_VALUE
    );

    protected List<Classifier> pool;

    protected Instances buffer;

    protected Instances accuracyEstimationWindow;

    protected Map<Instance, MappedCompetence> competenceMap;

    protected NearestNeighbourSearch nnSearch;

    protected boolean updateNNSearch;

    protected ClassificationEngine classificationEngine;

    protected PruningEngine pruningEngine;

    protected Classifier incompleteClassifier;

    protected boolean pruneIncomplete;

    @Override
    public String getPurposeString() {
        return "Dynamic Selection-based Ensemble for data stream classification.";
    }

    @Override
    public void resetLearningImpl() {
        classificationEngine = (ClassificationEngine) getPreparedClassOption(classificationEngineOption);
        buffer = null;
        accuracyEstimationWindow = null;
        competenceMap = new TreeMap<>(new InstanceValueComparator());
        nnSearch = new LinearNNSearch();
        updateNNSearch = true;
        pool = new ArrayList<>();
        pruneIncomplete = false;
        incompleteClassifier = null;
        pruningEngine = (PruningEngine) getPreparedClassOption(pruningEngineOption);
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (buffer == null) {
            buffer = new Instances(inst.dataset(), 0);
            buffer.setClassIndex(inst.classIndex());
        }
        if (accuracyEstimationWindow == null) {
            accuracyEstimationWindow = new Instances(inst.dataset(), 0);
            accuracyEstimationWindow.setClassIndex(inst.classIndex());
        }

        if(pruneIncomplete){
            pool.remove(incompleteClassifier);
            incompleteClassifier = null;
            pruneIncomplete = false;
        }
        accuracyEstimationWindow.add(inst);
        updateCompetenceMapForInstance(inst);
        updateNNSearch = true;
        if (accuracyEstimationWindow.numInstances() > mOption.getValue()) {
            Instance removedInstance = accuracyEstimationWindow.instance(0);
            accuracyEstimationWindow.delete(0);
            removeInstanceFromCompetenceMap(removedInstance);
            updateNNSearch = true;
        }

        buffer.add(inst);
        if (buffer.numInstances() >= trainSizeOption.getValue()) {

            Classifier newClassifier = ((Classifier) getPreparedClassOption(baseClassifierOption)).copy();
            newClassifier.resetLearning();
            for (int i = 0; i < buffer.numInstances(); i++) {
                newClassifier.trainOnInstance(buffer.get(i));
            }

            List<Classifier> beforePruning = new ArrayList<>(pool);
            pruningEngine.prune(pool, accuracyEstimationWindow, newClassifier, dOption.getValue());
            removePrunedClassifiersFromCompetenceMap(beforePruning);
            if (pool.contains(newClassifier)) {
                mapClassifierCompetence(newClassifier);
            }

            buffer.delete();
        }
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (pool == null || pool.isEmpty() || classificationEngine == null) {
            return new double[inst.numClasses()];
        }

        if (accuracyEstimationWindow == null || accuracyEstimationWindow.numInstances() == 0) {
            return new double[inst.numClasses()];
        }

        if(!pruneIncomplete){
            incompleteClassifier = ((Classifier) getPreparedClassOption(baseClassifierOption)).copy();
            incompleteClassifier.resetLearning();
            for (int i = 0; i < buffer.numInstances(); i++) {
                incompleteClassifier.trainOnInstance(buffer.get(i));
            }

            List<Classifier> beforePruning = new ArrayList<>(pool);
            pruningEngine.prune(pool, accuracyEstimationWindow, incompleteClassifier, dOption.getValue());
            removePrunedClassifiersFromCompetenceMap(beforePruning);
            pruneIncomplete = true;
        }

        updateNNSearchIfNeeded();
        return classificationEngine.classify(inst, nnSearch, competenceMap, pool, kOption.getValue(), incompleteClassifier);
    }

    @Override
    public long measureByteSize() {
        long byteSize = SizeOf.sizeOf(this);
        if (pool != null) {
            for (Classifier classifier : pool) {
                if (classifier != null) {
                    byteSize += classifier.measureByteSize();
                }
            }
        }
        if (accuracyEstimationWindow != null) {
            byteSize += SizeOf.fullSizeOf(accuracyEstimationWindow);
        }
        if (buffer != null) {
            byteSize += SizeOf.fullSizeOf(buffer);
        }
        return byteSize;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{
                new Measurement("pool size", pool != null ? pool.size() : 0),
                new Measurement("accuracy estimation window size",
                        accuracyEstimationWindow != null ? accuracyEstimationWindow.numInstances() : 0),
                new Measurement("buffer size", buffer != null ? buffer.numInstances() : 0)
        };
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        StringUtils.appendIndented(out, indent, "Dynamic Selection-based Ensemble");
        out.append('\n');
        StringUtils.appendIndented(out, indent, "Pool size: ");
        out.append(pool != null ? pool.size() : 0);
        out.append('\n');
    }

    @Override
    public Classifier[] getSubClassifiers() {
        return pool != null ? pool.toArray(new Classifier[0]) : new Classifier[0];
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == Dynse.class) {
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        }
        return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }

    protected void updateCompetenceMapForInstance(Instance inst) {
        MappedCompetence mappedCompetence = competenceMap.get(inst);
        if (mappedCompetence != null) {
            mappedCompetence.incrementInstanceCount();
        } else {
            mappedCompetence = new MappedCompetence(inst);
            competenceMap.put(inst, mappedCompetence);
        }

        for (Classifier classifier : pool) {
            if (classifierPredictsCorrectly(classifier, inst)) {
                mappedCompetence.addClassifier(classifier);
            }
        }
    }

    protected void mapClassifierCompetence(Classifier classifier) {
        for (MappedCompetence mappedCompetence : competenceMap.values()) {
            if (classifierPredictsCorrectly(classifier, mappedCompetence.getInstance())) {
                mappedCompetence.addClassifier(classifier);
            }
        }
    }

    protected void removePrunedClassifiersFromCompetenceMap(List<Classifier> oldPool) {
        for (Classifier oldClassifier : oldPool) {
            if (!pool.contains(oldClassifier)) {
                removeClassifierFromCompetenceMap(oldClassifier);
            }
        }
    }

    protected void removeClassifierFromCompetenceMap(Classifier classifier) {
        for (MappedCompetence mappedCompetence : competenceMap.values()) {
            mappedCompetence.removeClassifier(classifier);
        }
    }

    protected void removeInstanceFromCompetenceMap(Instance inst) {
        MappedCompetence mappedCompetence = competenceMap.get(inst);
        if (mappedCompetence == null) {
            return;
        }
        if (mappedCompetence.getInstanceCount() > 1) {
            mappedCompetence.decrementInstanceCount();
        } else {
            competenceMap.remove(inst);
        }
    }

    protected boolean classifierPredictsCorrectly(Classifier classifier, Instance inst) {
        double[] votes = classifier.getVotesForInstance(inst);
        return argMax(votes) == (int) inst.classValue();
    }

    protected int argMax(double[] votes) {
        int best = 0;
        for (int i = 1; i < votes.length; i++) {
            if (votes[i] > votes[best]) {
                best = i;
            }
        }
        return best;
    }

    protected void updateNNSearchIfNeeded() {
        if (!updateNNSearch) {
            return;
        }
        try {
            nnSearch.setInstances(accuracyEstimationWindow);
            updateNNSearch = false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class MappedCompetence {
        private final Instance instance;
        private int instanceCount;
        private final Set<Classifier> classifiers;

        public MappedCompetence(Instance instance) {
            this.instance = instance;
            this.instanceCount = 1;
            this.classifiers = new LinkedHashSet<>();
        }

        public Instance getInstance() {
            return instance;
        }

        public int getInstanceCount() {
            return instanceCount;
        }

        public void incrementInstanceCount() {
            instanceCount++;
        }

        public void decrementInstanceCount() {
            instanceCount--;
        }

        public Set<Classifier> getClassifiers() {
            return classifiers;
        }

        public void addClassifier(Classifier classifier) {
            classifiers.add(classifier);
        }

        public void removeClassifier(Classifier classifier) {
            classifiers.remove(classifier);
        }
    }

    protected static class InstanceValueComparator implements Comparator<Instance> {

        @Override
        public int compare(Instance inst1, Instance inst2) {
            for (int i = 0; i < inst1.numAttributes(); i++) {
                double value1 = inst1.value(i);
                double value2 = inst2.value(i);
                if (Double.isNaN(value1) || Double.isNaN(value2)) {
                    if (Double.isNaN(value1) && Double.isNaN(value2)) {
                        continue;
                    }
                    return Double.isNaN(value1) ? -1 : 1;
                }
                if (value1 < value2) {
                    return -1;
                }
                if (value1 > value2) {
                    return 1;
                }
            }
            return 0;
        }
    }
}
