/*
 *    EvaluatePeriodicHeldOutTest.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Ammar Shaker (shaker@mathematik.uni-marburg.de)
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
package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.StringUtils;
import moa.core.TimingUtils;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.preview.LearningCurve;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.streams.CachedInstancesStream;
import moa.streams.ExampleStream;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

/**
 * Task for evaluating a classifier on a stream by periodically testing on a heldout set.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class EvaluatePeriodicHeldOutTest extends ClassificationMainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a classifier on a stream by periodically testing on a heldout set.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Classifier to train.", MultiClassClassifier.class, "moa.classifiers.trees.HoeffdingTree");

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Learning performance evaluation method.",
            LearningPerformanceEvaluator.class,
            "BasicClassificationPerformanceEvaluator");

    public IntOption testSizeOption = new IntOption("testSize", 'n',
            "Number of testing examples.", 1000000, 0, Integer.MAX_VALUE);

    public IntOption trainSizeOption = new IntOption("trainSize", 'i',
            "Number of training examples, <1 = unlimited.", 0, 0,
            Integer.MAX_VALUE);

    public IntOption trainTimeOption = new IntOption("trainTime", 't',
            "Number of training seconds.", 10 * 60 * 60, 0, Integer.MAX_VALUE);

    public IntOption sampleFrequencyOption = new IntOption(
            "sampleFrequency",
            'f',
            "Number of training examples between samples of learning performance.",
            100000, 0, Integer.MAX_VALUE);

    public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
            "File to append intermediate csv results to.", null, "csv", true);

    public FlagOption cacheTestOption = new FlagOption("cacheTest", 'c',
            "Cache test instances in memory.");

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        Learner learner = (Learner) getPreparedClassOption(this.learnerOption);
        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
        LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
        learner.setModelContext(stream.getHeader());
        long instancesProcessed = 0;
        LearningCurve learningCurve = new LearningCurve("evaluation instances");
        File dumpFile = this.dumpFileOption.getFile();
        PrintStream immediateResultStream = null;
        if (dumpFile != null) {
            try {
                if (dumpFile.exists()) {
                    immediateResultStream = new PrintStream(
                            new FileOutputStream(dumpFile, true), true);
                } else {
                    immediateResultStream = new PrintStream(
                            new FileOutputStream(dumpFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open immediate result file: " + dumpFile, ex);
            }
        }
        boolean firstDump = true;
        ExampleStream testStream = null;
        int testSize = this.testSizeOption.getValue();
        if (this.cacheTestOption.isSet()) {
            monitor.setCurrentActivity("Caching test examples...", -1.0);
            Instances testInstances = new Instances(stream.getHeader(),
                    this.testSizeOption.getValue());
            while (testInstances.numInstances() < testSize) {
                testInstances.add((Instance) stream.nextInstance().getData());
                if (testInstances.numInstances()
                        % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
                    if (monitor.taskShouldAbort()) {
                        return null;
                    }
                    monitor.setCurrentActivityFractionComplete((double) testInstances.numInstances()
                            / (double) (this.testSizeOption.getValue()));
                }
            }
            testStream = new CachedInstancesStream(testInstances);
        } else {
            //testStream = (InstanceStream) stream.copy();
            testStream = stream;
            /*monitor.setCurrentActivity("Skipping test examples...", -1.0);
            for (int i = 0; i < testSize; i++) {
            stream.nextInstance();
            }*/
        }
        instancesProcessed = 0;
        TimingUtils.enablePreciseTiming();
        double totalTrainTime = 0.0;
        while ((this.trainSizeOption.getValue() < 1
                || instancesProcessed < this.trainSizeOption.getValue())
                && stream.hasMoreInstances() == true) {
            monitor.setCurrentActivityDescription("Training...");
            long instancesTarget = instancesProcessed
                    + this.sampleFrequencyOption.getValue();
            long trainStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
            while (instancesProcessed < instancesTarget && stream.hasMoreInstances() == true) {
                learner.trainOnInstance(stream.nextInstance());
                instancesProcessed++;
                if (instancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
                    if (monitor.taskShouldAbort()) {
                        return null;
                    }
                    monitor.setCurrentActivityFractionComplete((double) (instancesProcessed)
                            / (double) (this.trainSizeOption.getValue()));
                }
            }
            double lastTrainTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread()
                    - trainStartTime);
            totalTrainTime += lastTrainTime;
            if (totalTrainTime > this.trainTimeOption.getValue()) {
                break;
            }
	    if (this.cacheTestOption.isSet()) {
                testStream.restart();
            } 
            evaluator.reset();
            long testInstancesProcessed = 0;
            monitor.setCurrentActivityDescription("Testing (after "
                    + StringUtils.doubleToString(
                    ((double) (instancesProcessed)
                    / (double) (this.trainSizeOption.getValue()) * 100.0), 2)
                    + "% training)...");
            long testStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
            int instCount = 0 ;
            for (instCount = 0; instCount < testSize; instCount++) {
				if (stream.hasMoreInstances() == false) {
					break;
				}
                Example testInst = (Example) testStream.nextInstance(); //.copy();
                double trueClass = ((Instance) testInst.getData()).classValue();
                //testInst.setClassMissing();
                double[] prediction = learner.getVotesForInstance(testInst);
                //testInst.setClassValue(trueClass);
                evaluator.addResult(testInst, prediction);
                testInstancesProcessed++;
                if (testInstancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
                    if (monitor.taskShouldAbort()) {
                        return null;
                    }
                    monitor.setCurrentActivityFractionComplete((double) testInstancesProcessed
                            / (double) (testSize));
                }
            }
        	if ( instCount != testSize) {
				break;
			}
            double testTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread()
                    - testStartTime);
            List<Measurement> measurements = new ArrayList<Measurement>();
            measurements.add(new Measurement("evaluation instances",            		
                    instancesProcessed));
            measurements.add(new Measurement("total train time", totalTrainTime));
            measurements.add(new Measurement("total train speed",
                    instancesProcessed / totalTrainTime));
            measurements.add(new Measurement("last train time", lastTrainTime));
            measurements.add(new Measurement("last train speed",
                    this.sampleFrequencyOption.getValue() / lastTrainTime));
            measurements.add(new Measurement("test time", testTime));
            measurements.add(new Measurement("test speed", this.testSizeOption.getValue()
                    / testTime));
            Measurement[] performanceMeasurements = evaluator.getPerformanceMeasurements();
            for (Measurement measurement : performanceMeasurements) {
                measurements.add(measurement);
            }
            Measurement[] modelMeasurements = learner.getModelMeasurements();
            for (Measurement measurement : modelMeasurements) {
                measurements.add(measurement);
            }
            learningCurve.insertEntry(new LearningEvaluation(measurements.toArray(new Measurement[measurements.size()])));
            if (immediateResultStream != null) {
                if (firstDump) {
                    immediateResultStream.println(learningCurve.headerToString());
                    firstDump = false;
                }
                immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));
                immediateResultStream.flush();
            }
            if (monitor.resultPreviewRequested()) {
                monitor.setLatestResultPreview(learningCurve.copy());
            }
            // if (learner instanceof HoeffdingTree
            // || learner instanceof HoeffdingOptionTree) {
            // int numActiveNodes = (int) Measurement.getMeasurementNamed(
            // "active learning leaves",
            // modelMeasurements).getValue();
            // // exit if tree frozen
            // if (numActiveNodes < 1) {
            // break;
            // }
            // int numNodes = (int) Measurement.getMeasurementNamed(
            // "tree size (nodes)", modelMeasurements)
            // .getValue();
            // if (numNodes == lastNumNodes) {
            // noGrowthCount++;
            // } else {
            // noGrowthCount = 0;
            // }
            // lastNumNodes = numNodes;
            // } else if (learner instanceof OzaBoost || learner instanceof
            // OzaBag) {
            // double numActiveNodes = Measurement.getMeasurementNamed(
            // "[avg] active learning leaves",
            // modelMeasurements).getValue();
            // // exit if all trees frozen
            // if (numActiveNodes == 0.0) {
            // break;
            // }
            // int numNodes = (int) (Measurement.getMeasurementNamed(
            // "[avg] tree size (nodes)",
            // learner.getModelMeasurements()).getValue() * Measurement
            // .getMeasurementNamed("ensemble size",
            // modelMeasurements).getValue());
            // if (numNodes == lastNumNodes) {
            // noGrowthCount++;
            // } else {
            // noGrowthCount = 0;
            // }
            // lastNumNodes = numNodes;
            // }
        }
        if (immediateResultStream != null) {
            immediateResultStream.close();
        }
        return learningCurve;
    }

    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }


    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == EvaluatePeriodicHeldOutTest.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
