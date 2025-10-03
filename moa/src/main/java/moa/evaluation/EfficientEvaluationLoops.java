package moa.evaluation;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.SemiSupervisedLearner;
import moa.classifiers.semisupervised.ClusterAndLabelClassifier;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.Utils;
import moa.learners.Learner;
import moa.streams.ArffFileStream;
import moa.streams.ExampleStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.trees.FIMTDD;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

public class EfficientEvaluationLoops {

    public static class PrequentialResult {
        public ArrayList<double[]> windowedResults;
        public double[] cumulativeResults;
        public ArrayList<Integer> targets;
        public ArrayList<Integer> predictions;
        public HashMap<String, Double> otherMeasurements;

        public PrequentialResult(
            ArrayList<double[]> windowedResults,
            double[] cumulativeResults,
            ArrayList<Integer> targets,
            ArrayList<Integer> predictions,
            HashMap<String, Double> otherMeasurements
        ) {
            this.windowedResults = windowedResults;
            this.cumulativeResults = cumulativeResults;
            this.targets = targets;
            this.predictions = predictions;
            this.otherMeasurements = otherMeasurements;
        }

        public PrequentialResult(
            ArrayList<double[]> windowedResults,
            double[] cumulativeResults,
            ArrayList<Integer> targets,
            ArrayList<Integer> predictions
        ) {
            this(windowedResults, cumulativeResults, targets, predictions, null);
        }

        public PrequentialResult(
            ArrayList<double[]> windowedResults,
            double[] cumulativeResults,
            HashMap<String, Double> otherMeasurements
        ) {
            this(windowedResults, cumulativeResults, null, null, otherMeasurements);
        }
    }

    /***
     * This method can be used to calculate the test-then-train metrics and prequential windowed metrics at once.
     * It can also be used just to calculate the prequential windowed metrics (set basic_evaluator to null)
     * It can also be used just to calculate the test-then-train metrics (set windowed_evaluator to null)
     * Finally, it can also be used to calculate test-then-train metrics and sample them over time,
     * just set basic_evaluator to null and specify a BasicClassificationPerformanceEvaluator as the windowed_evaluator.
     * @param stream
     * @param learner
     * @param basicEvaluator
     * @param windowedEvaluator
     * @param maxInstances
     * @param windowSize
     * @param storeY
     * @param storePredictions
     * @return the return has to be an ArrayList because we don't know ahead of time how many windows will be produced
     */
    public static PrequentialResult PrequentialEvaluation(ExampleStream stream, Learner learner,
                                                          LearningPerformanceEvaluator<Example<Instance>> basicEvaluator,
                                                          LearningPerformanceEvaluator<Example<Instance>> windowedEvaluator,
                                                          long maxInstances, long windowSize,
                                                          boolean storeY, boolean storePredictions) {
        int instancesProcessed = 0;

        if (!stream.hasMoreInstances())
            stream.restart();

        ArrayList<double[]> windowed_results = new ArrayList<>();
        ArrayList<Integer> targetValues = new ArrayList<>();
        ArrayList<Integer> predictions = new ArrayList<>();


        while (stream.hasMoreInstances() &&
                (maxInstances == -1 || instancesProcessed < maxInstances)) {

            Example<Instance> instance = stream.nextInstance();

            double[] prediction = learner.getVotesForInstance(instance);

            // Update evaluators and store predictions if requested
            if (basicEvaluator != null)
                basicEvaluator.addResult(instance, prediction);
            if (windowedEvaluator != null)
                windowedEvaluator.addResult(instance, prediction);
            if (storePredictions)
                predictions.add(Utils.maxIndex(prediction));
            if (storeY)
                targetValues.add((int)Math.round(instance.getData().classValue()));

            learner.trainOnInstance(instance);
            instancesProcessed++;

            // Store windowed results if requested
            if (windowedEvaluator != null)
                if (instancesProcessed % windowSize == 0) {
                    Measurement[] measurements = windowedEvaluator.getPerformanceMeasurements();
                    double[] values = new double[measurements.length];
                    for (int i = 0; i < values.length; ++i)
                        values[i] = measurements[i].getValue();
                    windowed_results.add(values);
                }
        }
        if (windowedEvaluator != null)
            if (instancesProcessed % windowSize != 0) {
                Measurement[] measurements = windowedEvaluator.getPerformanceMeasurements();
                double[] values = new double[measurements.length];
                for (int i = 0; i < values.length; ++i)
                    values[i] = measurements[i].getValue();
                windowed_results.add(values);
            }

        double[] cumulative_results = null;

        if (basicEvaluator != null) {
            Measurement[] measurements = basicEvaluator.getPerformanceMeasurements();
            cumulative_results = new double[measurements.length];
            for (int i = 0; i < cumulative_results.length; ++i)
                cumulative_results[i] = measurements[i].getValue();
        }

        return new PrequentialResult(
            windowed_results,
            cumulative_results,
            targetValues,
            predictions
        );
    }

    public static PrequentialResult PrequentialSSLEvaluation(
            ExampleStream<Example<Instance>> stream,
            Learner learner,
            LearningPerformanceEvaluator basicEvaluator,
            LearningPerformanceEvaluator windowedEvaluator,
            long maxInstances,
            long windowSize,
            long initialWindowSize,
            long delayLength,
            double labelProbability,
            int randomSeed,
            boolean debugPseudoLabels,
            boolean storeY,
            boolean storePredictions
        ) {
//        int delayLength = this.delayLengthOption.getValue();
//        double labelProbability = this.labelProbabilityOption.getValue();

        RandomGenerator taskRandom = new MersenneTwister(randomSeed);
//        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
//        Learner learner = getLearner(stream);

        int instancesProcessed = 0;
        int numCorrectPseudoLabeled = 0;
        int numUnlabeledData = 0;
        int numInstancesTested = 0;

        if (!stream.hasMoreInstances())
            stream.restart();

        ArrayList<double[]> windowed_results = new ArrayList<>();

        ArrayList<Integer> targetValues = new ArrayList<>();
        ArrayList<Integer> predictions = new ArrayList<>();
        HashMap<String, Double> other_measures = new HashMap<>();

        // The buffer is a list of tuples. The first element is the index when
        // it should be emitted. The second element is the instance itself.
        List<Pair<Long, Example<Instance>>> delayBuffer = new ArrayList<Pair<Long, Example<Instance>>>();

        while (stream.hasMoreInstances() &&
                (maxInstances == -1 || instancesProcessed < maxInstances)) {

            // TRAIN on delayed instances
            while (delayBuffer.size() > 0
                    && delayBuffer.get(0).getKey() == instancesProcessed) {
                Example delayedExample = delayBuffer.remove(0).getValue();
//                System.out.println("[TRAIN][DELAY] "+delayedExample.getData().toString());
                learner.trainOnInstance(delayedExample);
            }

            Example<Instance> instance = stream.nextInstance();
            Example<Instance> unlabeledExample = instance.copy();
            int trueClass = (int) ((Instance) instance.getData()).classValue();

            // In case it is set, then the label is not removed. We want to pass the
            // labelled data to the learner even in trainOnUnlabeled data to generate statistics such as number
            // of correctly pseudo-labeled instances.
            if (!debugPseudoLabels) {
                // Remove the label of the unlabeledExample indirectly through
                // unlabeledInstanceData.
                Instance __instance = (Instance) unlabeledExample.getData();
                __instance.setMissing(__instance.classIndex());
            }

            // WARMUP
            // Train on the initial instances. These are not used for testing!
            if (instancesProcessed < initialWindowSize) {
//                if (learner instanceof SemiSupervisedLearner)
//                    ((SemiSupervisedLearner) learner).addInitialWarmupTrainingInstances();
//                System.out.println("[TRAIN][INITIAL_WINDOW] "+instance.getData().toString());
                learner.trainOnInstance(instance);
                instancesProcessed++;
                continue;
            }

            Boolean is_labeled = labelProbability > taskRandom.nextDouble();
            if (!is_labeled) {
                numUnlabeledData++;
            }

            // TEST
            // Obtain the prediction for the testInst (i.e. no label)
//            System.out.println("[TEST] " + unlabeledExample.getData().toString());
            double[] prediction = learner.getVotesForInstance(unlabeledExample);
            numInstancesTested++;

            if (basicEvaluator != null)
                basicEvaluator.addResult(instance, prediction);
            if (windowedEvaluator != null)
                windowedEvaluator.addResult(instance, prediction);
            if (storeY)
                targetValues.add((int)Math.round(instance.getData().classValue()));
            if (storePredictions)
                predictions.add(Utils.maxIndex(prediction));

            int pseudoLabel = -1;
            // TRAIN
            if (is_labeled && delayLength >= 0) {
                // The instance will be labeled but has been delayed
                if (learner instanceof SemiSupervisedLearner) {
//                    System.out.println("[TRAIN_UNLABELED][DELAYED] " + unlabeledExample.getData().toString());
                    pseudoLabel = ((SemiSupervisedLearner) learner).trainOnUnlabeledInstance((Instance) unlabeledExample.getData());
                }
                delayBuffer.add(new MutablePair<>(1 + instancesProcessed + delayLength, instance));
            } else if (is_labeled) {
//                System.out.println("[TRAIN] " + instance.getData().toString());
                // The instance will be labeled and is not delayed e.g delayLength = -1
                learner.trainOnInstance(instance);
            } else {
                // The instance will never be labeled
                if (learner instanceof SemiSupervisedLearner) {
//                    System.out.println("[TRAIN_UNLABELED][IMMEDIATE] " + unlabeledExample.getData().toString());
                    pseudoLabel = ((SemiSupervisedLearner) learner).trainOnUnlabeledInstance((Instance) unlabeledExample.getData());
                }
            }
            if(trueClass == pseudoLabel)
                numCorrectPseudoLabeled++;

            instancesProcessed++;

            if (windowedEvaluator != null)
                if (instancesProcessed % windowSize == 0) {
                    Measurement[] measurements = windowedEvaluator.getPerformanceMeasurements();
                    double[] values = new double[measurements.length];
                    for (int i = 0; i < values.length; ++i)
                        values[i] = measurements[i].getValue();
                    windowed_results.add(values);
                }
        }
        if (windowedEvaluator != null)
            if (instancesProcessed % windowSize != 0) {
                Measurement[] measurements = windowedEvaluator.getPerformanceMeasurements();
                double[] values = new double[measurements.length];
                for (int i = 0; i < values.length; ++i)
                    values[i] = measurements[i].getValue();
                windowed_results.add(values);
            }

        double[] cumulative_results = null;

        if (basicEvaluator != null) {
            Measurement[] measurements = basicEvaluator.getPerformanceMeasurements();
            cumulative_results = new double[measurements.length];
            for (int i = 0; i < cumulative_results.length; ++i)
                cumulative_results[i] = measurements[i].getValue();
        }

        // TODO: Add this measures in a windowed way.
        other_measures.put("num_unlabeled_instances", (double) numUnlabeledData);
        other_measures.put("num_correct_pseudo_labeled", (double) numCorrectPseudoLabeled);
        other_measures.put("num_instances_tested", (double) numInstancesTested);
        other_measures.put("pseudo_label_accuracy", (double) numCorrectPseudoLabeled/numInstancesTested);


        return new PrequentialResult(
            windowed_results,
            cumulative_results,
            targetValues,
            predictions,
            other_measures
        );
    }

    /******************************************************************************************************************/
    /******************************************************************************************************************/
    /***************************************** TESTS ******************************************************************/
    /******************************************************************************************************************/
    /******************************************************************************************************************/

    private static void testPrequentialSSL(String file_path, Learner learner,
                                           long maxInstances,
                                           long windowSize,
                                           long initialWindowSize,
                                           long delayLength,
                                           double labelProbability) {
        System.out.println(
                "maxInstances: " + maxInstances + ", " +
                        "windowSize: " + windowSize + ", " +
                        "initialWindowSize: " + initialWindowSize + ", " +
                        "delayLength: " + delayLength + ", " +
                        "labelProbability: " + labelProbability
        );

        // Record the start time
        long startTime = System.currentTimeMillis();

        ArffFileStream stream = new ArffFileStream(file_path, -1);
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        WindowClassificationPerformanceEvaluator windowed_evaluator = new WindowClassificationPerformanceEvaluator();
        windowed_evaluator.widthOption.setValue((int) windowSize);
        windowed_evaluator.prepareForUse();

        PrequentialResult result = PrequentialSSLEvaluation(stream, learner,
                basic_evaluator,
                windowed_evaluator,
                maxInstances,
                windowSize,
                initialWindowSize,
                delayLength,
                labelProbability,
                1,
                true,
                false,
                false
        );

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");
        System.out.println("Number of unlabeled instances: " + result.otherMeasurements.get("num_unlabeled_instances"));

        System.out.println("\tBasic performance");
        for (int i = 0; i < result.cumulativeResults.length; ++i)
            System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + result.cumulativeResults[i]);

        System.out.println("\tWindowed performance");
        for (int j = 0; j < result.windowedResults.size(); ++j) {
            System.out.print("Window: " + j + ", ");
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(windowed_evaluator.getPerformanceMeasurements()[i].getName() + ": " + result.windowedResults.get(j)[i]);
        }
    }

    public static void main(String[] args) {
        String hyper_arff = "/Users/gomeshe/Desktop/data/Hyper100k.arff";
        String debug_arff = "/Users/gomeshe/Desktop/data/debug_prequential_SSL.arff";
        String ELEC_arff = "/Users/gomeshe/Dropbox/ciencia_computacao/lecturer/research/ssl_disagreement/datasets/ELEC/elecNormNew.arff";

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

//        testPrequentialSSL(debug_arff, learner, 100, 10, 0, 0, 1.0); // OK
//        testPrequentialSSL(debug_arff, learner, 100, 10, 1, 0, 1.0); //OK
//        testPrequentialSSL(debug_arff, learner, 10, 10, 5, 0, 1.0); // OK
//        testPrequentialSSL(debug_arff, learner, 10, 10, -1, 1, 1.0); // OK
//        testPrequentialSSL(debug_arff, learner, 20, 10, -1, 10, 1.0); // OK
//        testPrequentialSSL(debug_arff, learner, 20, 10, -1, 2, 0.5); // OK
//        testPrequentialSSL(debug_arff, learner, 100, 10, 50, 2, 0.0); // OK
//        testPrequentialSSL(debug_arff, learner, 100, 10, 0, 90, 1.0); // OK
//        testPrequentialSSL(debug_arff, learner, 100, 10, 0, -1, 0.5); // OK

//        testPrequentialSSL(hyper_arff, learner, -1, 1000, -1, -1, 1.0);
//        testPrequentialSSL(hyper_arff, learner, -1, 1000, -1, -1, 0.5); // OK

//        testPrequentialSSL(hyper_arff, learner, -1, 1000, 1000, -1, 0.5);

        ClusterAndLabelClassifier ssl_learner = new ClusterAndLabelClassifier();
        ssl_learner.prepareForUse();

        testPrequentialSSL(ELEC_arff, ssl_learner, 10000, 1000, -1, -1, 0.01);

//        testWindowedEvaluation();
//        testTestThenTrainEvaluation();
//        testPrequentialEvaluation();
//
//        StreamingRandomPatches learner = new StreamingRandomPatches();
//        learner.getOptions().setViaCLIString("-s 100"); // 10 learners
////        learner.setRandomSeed(5);
//        learner.prepareForUse();
//        testPrequentialEfficiency1(learner);

//        testPrequentialEvaluation_edge_cases1();
//        testPrequentialEvaluation_edge_cases2();
//        testPrequentialEvaluation_edge_cases3();
//        testPrequentialEvaluation_edge_cases4();
//        testPrequentialEvaluation_SampleFrequency_TestThenTrain();

//        testPrequentialRegressionEvaluation();
    }


    private static void testPrequentialEfficiency1(Learner learner) {
        // Record the start time
        long startTime = System.currentTimeMillis();

        ArffFileStream stream = new ArffFileStream("/Users/gomeshe/Desktop/data/Hyper100k.arff", -1);
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, null, 100000, 1, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        System.out.println("\tBasic performance");
        for (int i = 0; i < results.cumulativeResults.length; ++i)
            System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.cumulativeResults[i]);
    }

    private static void testPrequentialEvaluation_edge_cases1() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        ArffFileStream stream = new ArffFileStream("/Users/gomeshe/Desktop/data/Hyper100k.arff", -1);
        stream.prepareForUse();

//        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
//        basic_evaluator.recallPerClassOption.setValue(true);
//        basic_evaluator.prepareForUse();
//
//        WindowClassificationPerformanceEvaluator windowed_evaluator = new WindowClassificationPerformanceEvaluator();
//        windowed_evaluator.widthOption.setValue(1000);
//        windowed_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, null, null, 100000, 1000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

//        System.out.println("\tBasic performance");
//        for (int i = 0; i < results.basicResults.length; ++i)
//            System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.basicResults[i]);

//        System.out.println("\tWindowed performance");
//        for (int j = 0; j < results.windowedResults.size(); ++j) {
//            System.out.println("\t" + j);
//            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
//                System.out.println(windowed_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.windowedResults.get(j)[i]);
//        }
    }


    private static void testPrequentialEvaluation_edge_cases2() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        ArffFileStream stream = new ArffFileStream("/Users/gomeshe/Desktop/data/Hyper100k.arff", -1);
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        WindowClassificationPerformanceEvaluator windowed_evaluator = new WindowClassificationPerformanceEvaluator();
        windowed_evaluator.widthOption.setValue(1000);
        windowed_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, windowed_evaluator, 1000, 10000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        System.out.println("\tBasic performance");
        for (int i = 0; i < results.cumulativeResults.length; ++i)
            System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.cumulativeResults[i]);

        System.out.println("\tWindowed performance");
        for (int j = 0; j < results.windowedResults.size(); ++j) {
            System.out.println("\t" + j);
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(windowed_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.windowedResults.get(j)[i]);
        }
    }

    private static void testPrequentialEvaluation_edge_cases3() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        ArffFileStream stream = new ArffFileStream("/Users/gomeshe/Desktop/data/Hyper100k.arff", -1);
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        WindowClassificationPerformanceEvaluator windowed_evaluator = new WindowClassificationPerformanceEvaluator();
        windowed_evaluator.widthOption.setValue(1000);
        windowed_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, windowed_evaluator, 10, 1, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        System.out.println("\tBasic performance");
        for (int i = 0; i < results.cumulativeResults.length; ++i)
            System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.cumulativeResults[i]);

        System.out.println("\tWindowed performance");
        for (int j = 0; j < results.windowedResults.size(); ++j) {
            System.out.println("\t" + j);
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(windowed_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.windowedResults.get(j)[i]);
        }
    }

    private static void testPrequentialEvaluation_edge_cases4() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        ArffFileStream stream = new ArffFileStream("/Users/gomeshe/Desktop/data/Hyper100k.arff", -1);
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        WindowClassificationPerformanceEvaluator windowed_evaluator = new WindowClassificationPerformanceEvaluator();
        windowed_evaluator.widthOption.setValue(10000);
        windowed_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, windowed_evaluator, -1, 10000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        System.out.println("\tBasic performance");
        for (int i = 0; i < results.cumulativeResults.length; ++i)
            System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.cumulativeResults[i]);

        System.out.println("\tWindowed performance");
        for (int j = 0; j < results.windowedResults.size(); ++j) {
            System.out.println("\t" + j);
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(windowed_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.windowedResults.get(j)[i]);
        }
    }


    private static void testPrequentialEvaluation_SampleFrequency_TestThenTrain() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        ArffFileStream stream = new ArffFileStream("/Users/gomeshe/Desktop/data/Hyper100k.arff", -1);
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

//        WindowClassificationPerformanceEvaluator windowed_evaluator = new WindowClassificationPerformanceEvaluator();
//        windowed_evaluator.widthOption.setValue(10000);
//        windowed_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, null, basic_evaluator, -1, 10000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

//        System.out.println("\tBasic performance");
//        for (int i = 0; i < results.basicResults.length; ++i)
//            System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.basicResults[i]);

        System.out.println("\tWindowed performance");
        for (int j = 0; j < results.windowedResults.size(); ++j) {
            System.out.println("\t" + j);
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.windowedResults.get(j)[i]);
        }
    }


    private static void testPrequentialRegressionEvaluation() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        FIMTDD learner = new FIMTDD();
//        learner.getOptions().setViaCLIString("-s 10"); // 10 learners
//        learner.setRandomSeed(5);
        learner.prepareForUse();

        ArffFileStream stream = new ArffFileStream("/Users/gomeshe/Desktop/data/metrotraffic_with_nominals.arff", -1);
        stream.prepareForUse();

        BasicRegressionPerformanceEvaluator basic_evaluator = new BasicRegressionPerformanceEvaluator();

        WindowRegressionPerformanceEvaluator windowed_evaluator = new WindowRegressionPerformanceEvaluator();
        windowed_evaluator.widthOption.setValue(1000);
//        windowed_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, windowed_evaluator, 100000, 1000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        System.out.println("\tBasic performance");
        for (int i = 0; i < results.cumulativeResults.length; ++i)
            System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.cumulativeResults[i]);

        System.out.println("\tWindowed performance");
        for (int j = 0; j < results.windowedResults.size(); ++j) {
            System.out.println("\t" + j);
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(windowed_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.windowedResults.get(j)[i]);
        }
    }

    private static void testPrequentialEvaluation() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
//        learner.getOptions().setViaCLIString("-s 10"); // 10 learners
//        learner.setRandomSeed(5);
        learner.prepareForUse();

        ArffFileStream stream = new ArffFileStream("/Users/gomeshe/Desktop/data/Hyper100k.arff", -1);
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        WindowClassificationPerformanceEvaluator windowed_evaluator = new WindowClassificationPerformanceEvaluator();
        windowed_evaluator.widthOption.setValue(1000);
        windowed_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, windowed_evaluator, 100000, 1000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        System.out.println("\tBasic performance");
        for (int i = 0; i < results.cumulativeResults.length; ++i)
            System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.cumulativeResults[i]);

        System.out.println("\tWindowed performance");
        for (int j = 0; j < results.windowedResults.size(); ++j) {
            System.out.println("\t" + j);
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(windowed_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.windowedResults.get(j)[i]);
        }
    }

    private static void testTestThenTrainEvaluation() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
//        learner.getOptions().setViaCLIString("-s 10"); // 10 learners
//        learner.setRandomSeed(5);
        learner.prepareForUse();

        ArffFileStream stream = new ArffFileStream("/Users/gomeshe/Desktop/data/Hyper100k.arff", -1);
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator evaluator = new BasicClassificationPerformanceEvaluator();
        evaluator.recallPerClassOption.setValue(true);
        evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, evaluator, null, 100000, 100000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        for (int i = 0; i < results.cumulativeResults.length; ++i)
            System.out.println(evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.cumulativeResults[i]);
    }

    private static void testWindowedEvaluation() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
//        learner.getOptions().setViaCLIString("-s 10"); // 10 learners
//        learner.setRandomSeed(5);
        learner.prepareForUse();

        ArffFileStream stream = new ArffFileStream("/Users/gomeshe/Desktop/data/Hyper100k.arff", -1);
        stream.prepareForUse();

        WindowClassificationPerformanceEvaluator evaluator = new WindowClassificationPerformanceEvaluator();
        evaluator.widthOption.setValue(10000);
        evaluator.recallPerClassOption.setValue(true);
        evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, null, evaluator, 100000, 10000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        for (int j = 0; j < results.windowedResults.size(); ++j) {
            System.out.println("\t" + j);
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.windowedResults.get(j)[i]);
        }
    }
}