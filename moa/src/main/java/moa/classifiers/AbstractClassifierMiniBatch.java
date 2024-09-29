/*
 *    AbstractClassifier.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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

package moa.classifiers;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.*;
import moa.capabilities.CapabilitiesHandler;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractClassifierMiniBatch extends AbstractClassifier
        implements Classifier, CapabilitiesHandler { //Learner<Example<Instance>> {

    @Override
    public String getPurposeString() {
        return "MOA Parallel Classifier with MiniBatch: " + getClass().getCanonicalName();
    }

    public IntOption numberOfCoresOption = new IntOption("numCores", 'c',
            "The amount of CPU Cores used for multi-threading", 1,
            -1, Runtime.getRuntime().availableProcessors());

    public IntOption batchSizeOption = new IntOption("batchSize", 'b',
            "The amount of instances the classifier should buffer before training.",
            1, 1, Integer.MAX_VALUE);

    // The amount of CPU cores to be run in parallel
    public int numOfCores;

    // The threadpool to be used, based on the number of cores
    protected ExecutorService threadpool;
    protected ArrayList<Instance> myBatch;

    public AbstractClassifierMiniBatch() {
        if (isRandomizable()) {
            this.randomSeedOption = new IntOption("randomSeed", 'r',
                    "Seed for random behaviour of the classifier.", 1);
        }
    }

    /**
     * Trains this classifier "incrementally" using the given instance.<br><br>
     *
     * The reason for ...Impl methods: ease programmer burden by not requiring
     * them to remember calls to super in overridden methods.
     * Note that this will produce compiler errors if not overridden.
     *
     * @param inst the instance to be used for training
     */
    public void trainOnInstanceImpl(Instance inst) {
        if (myBatch != null) {
            this.myBatch.add(inst);
            if (this.myBatch.size() == this.batchSizeOption.getValue()){
                this.trainOnInstances(this.myBatch);
                this.myBatch.clear();
            }
        }
    }

    public abstract void trainOnInstances(ArrayList<Instance> instances);


    public void trainingHasEnded() {
        if (this.threadpool != null)
            this.threadpool.shutdown();
        this.myBatch = null;
    }

    @Override
    public void resetLearning() {
        this.numOfCores = this.numberOfCoresOption.getValue();
        int maxCores = Runtime.getRuntime().availableProcessors();
        if (this.numOfCores == 1 || this.numOfCores == 0) {
            this.threadpool = Executors.newFixedThreadPool(1);
        } else if (this.numOfCores < 0 || this.numOfCores > maxCores) {
            this.threadpool = Executors.newFixedThreadPool(maxCores);
        } else {
            this.threadpool = Executors.newFixedThreadPool(this.numOfCores);
        }
        this.trainingWeightSeenByModel = 0.0;
        if (isRandomizable()) {
            this.classifierRandom = new Random(this.randomSeed);
        }
        this.myBatch = new ArrayList<>();
        resetLearningImpl();
    }
}
