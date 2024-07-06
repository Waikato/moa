package moa.evaluation;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.meta.StreamingRandomPatches;
import moa.classifiers.trees.FIMTDD;
import moa.core.Example;
import moa.core.Measurement;
import moa.learners.Learner;
import moa.streams.ArffFileStream;
import moa.streams.ExampleStream;
import moa.streams.generators.AgrawalGenerator;
import moa.streams.generators.HyperplaneGenerator;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class provides functionality for executing experiments programmatically, bypassing the need for a GUI or CLI.
 * It offers the same capabilities as running Tasks, such as EvaluatePrequential, but is intended for use directly from
 * code. This allows users to seamlessly integrate this class with other Java codebases.
 *
 * Key Features:
 * - Enables execution of experiments without requiring a graphical user interface (GUI) or command line interface (CLI).
 * - Provides the same functionality as Task-based evaluations (e.g., EvaluatePrequential) for ease of use in code.
 * - Facilitates exploration and execution of MOA learners programmatically.
 *
 * IMPORTANT: This class was initially designed to offer efficient evaluation loops for MOA learners within the
 * CapyMOA framework.
 * Any modifications to this class should be made with caution, considering its integration with CapyMOA.
 * For more details, refer to the CapyMOA website: www.capymoa.org.
 */
public class EfficientEvaluationLoops {

    /**
     * This inner class encapsulates the results of various evaluation types, such as Prequential,
     * Cumulative, or Windowed evaluations. It is primarily used to store Prequential evaluation results,
     * including cumulative and windowed results. Additionally, this class can store the ground truth and
     * prediction values, which are valuable for plotting or debugging purposes.
     *
     * Key Features:
     * - Holds results from Prequential, Cumulative, or Windowed evaluations.
     * - Primarily designed for storing Prequential evaluation outcomes, thus both cumulative and windowed results.
     * - When used to store only Windowed results, the Cumulative results will be null.
     * - When used to store only Cumulative results, the Windowed results will be null.
     * - Capable of storing ground truth and prediction values for further analysis, plotting, or debugging.
     */
    public static class PrequentialResult {
        public ArrayList<double[]> windowedResults;
        public double[] cumulativeResults;
        public ArrayList<Double> targets;
        public ArrayList<Double> predictions;

        public HashMap<String, Double> otherMeasurements;

        public PrequentialResult(ArrayList<double[]> windowedResults, double[] cumulativeResults) {
            this.windowedResults = windowedResults;
            this.cumulativeResults = cumulativeResults;
            this.targets = null;
            this.predictions = null;
        }

        public PrequentialResult(ArrayList<double[]> windowedResults, double[] cumulativeResults,
                                 ArrayList<Double> targets, ArrayList<Double> predictions) {
            this.windowedResults = windowedResults;
            this.cumulativeResults = cumulativeResults;
            this.targets = targets;
            this.predictions = predictions;
        }

        /***
         * This constructor is useful to store metrics beyond the evaluation metrics available through the evaluators.
         * @param windowedResults
         * @param cumulativeResults
         * @param otherMeasurements
         */
        public PrequentialResult(ArrayList<double[]> windowedResults, double[] cumulativeResults,
                                 HashMap<String, Double> otherMeasurements) {
            this(windowedResults, cumulativeResults);
            this.otherMeasurements = otherMeasurements;
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
     * @return PrequentialResult is a custom class that holds the respective results from the execution
     */
    public static PrequentialResult PrequentialEvaluation(ExampleStream stream, Learner learner,
                                                          LearningPerformanceEvaluator basicEvaluator,
                                                          LearningPerformanceEvaluator windowedEvaluator,
                                                          long maxInstances, long windowSize,
                                                          boolean storeY, boolean storePredictions) {
        int instancesProcessed = 0;

        if (!stream.hasMoreInstances())
            stream.restart();

        ArrayList<double[]> windowed_results = new ArrayList<>();
        ArrayList<Double> targetValues = new ArrayList<>();
        ArrayList<Double> predictions = new ArrayList<>();


        while (stream.hasMoreInstances() &&
                (maxInstances == -1 || instancesProcessed < maxInstances)) {

            Example<Instance> instance = stream.nextInstance();
            if (storeY)
                targetValues.add(instance.getData().classValue());

            double[] prediction = learner.getVotesForInstance(instance);
            if (basicEvaluator != null)
                basicEvaluator.addResult(instance, prediction);
            if (windowedEvaluator != null)
                windowedEvaluator.addResult(instance, prediction);

            if (storePredictions)
                predictions.add(prediction.length == 0? 0 : prediction[0]);

            learner.trainOnInstance(instance);

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
        if (!storePredictions && !storeY)
            return new PrequentialResult(windowed_results, cumulative_results);
        else
            return new PrequentialResult(windowed_results, cumulative_results, targetValues, predictions);
    }


    /***
     * The following code can be used to provide examples of how to use the class.
     * In the future, some of these examples can be turned into tests.
     * @param args
     */
    public static void main(String[] args) {
        examplePrequentialEvaluation_edge_cases1();
        examplePrequentialEvaluation_edge_cases2();
        examplePrequentialEvaluation_edge_cases3();
        examplePrequentialEvaluation_edge_cases4();
        examplePrequentialEvaluation_SampleFrequency_TestThenTrain();
        examplePrequentialRegressionEvaluation();
        examplePrequentialEvaluation();
        exampleTestThenTrainEvaluation();
        exampleWindowedEvaluation();

        // Run time efficiency evaluation examples
        StreamingRandomPatches srp10 = new StreamingRandomPatches();
        srp10.getOptions().setViaCLIString("-s 10"); // 10 learners
        srp10.setRandomSeed(5);
        srp10.prepareForUse();

        StreamingRandomPatches srp100 = new StreamingRandomPatches();
        srp100.getOptions().setViaCLIString("-s 100"); // 100 learners
        srp100.setRandomSeed(5);
        srp100.prepareForUse();

        int maxInstances = 100000;
        examplePrequentialEfficiency(srp10, maxInstances);
        examplePrequentialEfficiency(srp100, maxInstances);
    }


    private static void examplePrequentialEfficiency(Learner learner, int maxInstances) {
        System.out.println("Assessing efficiency for " + learner.getCLICreationString(learner.getClass()) +
                " maxInstances: " + maxInstances);

        // Record the start time
        long startTime = System.currentTimeMillis();

        AgrawalGenerator stream = new AgrawalGenerator();
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, null,
                maxInstances, 1, false, false);

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

    private static void examplePrequentialEvaluation_edge_cases1() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        AgrawalGenerator stream = new AgrawalGenerator();
        stream.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, null, null,
                100000, 1000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");
    }

    private static void examplePrequentialEvaluation_edge_cases2() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        AgrawalGenerator stream = new AgrawalGenerator();
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        WindowClassificationPerformanceEvaluator windowed_evaluator = new WindowClassificationPerformanceEvaluator();
        windowed_evaluator.widthOption.setValue(1000);
        windowed_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, windowed_evaluator,
                1000, 10000, false, false);

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

    private static void examplePrequentialEvaluation_edge_cases3() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        AgrawalGenerator stream = new AgrawalGenerator();
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        WindowClassificationPerformanceEvaluator windowed_evaluator = new WindowClassificationPerformanceEvaluator();
        windowed_evaluator.widthOption.setValue(1000);
        windowed_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, windowed_evaluator,
                10, 1, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        System.out.println("\tBasic performance");
        for (int i = 0; i < results.cumulativeResults.length; ++i)
            System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " +
                    results.cumulativeResults[i]);

        System.out.println("\tWindowed performance");
        for (int j = 0; j < results.windowedResults.size(); ++j) {
            System.out.println("\t" + j);
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(windowed_evaluator.getPerformanceMeasurements()[i].getName() + ": " +
                        results.windowedResults.get(j)[i]);
        }
    }

    private static void examplePrequentialEvaluation_edge_cases4() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        AgrawalGenerator stream = new AgrawalGenerator();
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        WindowClassificationPerformanceEvaluator windowed_evaluator = new WindowClassificationPerformanceEvaluator();
        windowed_evaluator.widthOption.setValue(10000);
        windowed_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, windowed_evaluator,
                100000, 10000, false, false);

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

    private static void examplePrequentialEvaluation_SampleFrequency_TestThenTrain() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        AgrawalGenerator stream = new AgrawalGenerator();
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator basic_evaluator = new BasicClassificationPerformanceEvaluator();
        basic_evaluator.recallPerClassOption.setValue(true);
        basic_evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, null, basic_evaluator,
                100000, 10000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        System.out.println("\tWindowed performance");
        for (int j = 0; j < results.windowedResults.size(); ++j) {
            System.out.println("\t" + j);
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(basic_evaluator.getPerformanceMeasurements()[i].getName() + ": " + results.windowedResults.get(j)[i]);
        }
    }

    private static void examplePrequentialRegressionEvaluation() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        FIMTDD learner = new FIMTDD();
        learner.prepareForUse();

        HyperplaneGenerator stream = new HyperplaneGenerator();
        stream.prepareForUse();

        BasicRegressionPerformanceEvaluator basic_evaluator = new BasicRegressionPerformanceEvaluator();

        WindowRegressionPerformanceEvaluator windowed_evaluator = new WindowRegressionPerformanceEvaluator();
        windowed_evaluator.widthOption.setValue(1000);

        PrequentialResult results = PrequentialEvaluation(stream, learner, basic_evaluator, windowed_evaluator,
                10000, 1000, false, false);

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

    private static void examplePrequentialEvaluation() {
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

    private static void exampleTestThenTrainEvaluation() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        HyperplaneGenerator stream = new HyperplaneGenerator();
        stream.prepareForUse();

        BasicClassificationPerformanceEvaluator evaluator = new BasicClassificationPerformanceEvaluator();
        evaluator.recallPerClassOption.setValue(true);
        evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, evaluator, null,
                100000, 100000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        for (int i = 0; i < results.cumulativeResults.length; ++i)
            System.out.println(evaluator.getPerformanceMeasurements()[i].getName() + ": " +
                    results.cumulativeResults[i]);
    }

    private static void exampleWindowedEvaluation() {
        // Record the start time
        long startTime = System.currentTimeMillis();

        NaiveBayes learner = new NaiveBayes();
        learner.prepareForUse();

        HyperplaneGenerator stream = new HyperplaneGenerator();
        stream.prepareForUse();

        WindowClassificationPerformanceEvaluator evaluator = new WindowClassificationPerformanceEvaluator();
        evaluator.widthOption.setValue(10000);
        evaluator.recallPerClassOption.setValue(true);
        evaluator.prepareForUse();

        PrequentialResult results = PrequentialEvaluation(stream, learner, null, evaluator,
                100000, 10000, false, false);

        // Record the end time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time in milliseconds
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed Time: " + elapsedTime / 1000 + " seconds");

        for (int j = 0; j < results.windowedResults.size(); ++j) {
            System.out.println("\t" + j);
            for (int i = 0; i < 2; ++i) // results.get(results.size()-1).length; ++i)
                System.out.println(evaluator.getPerformanceMeasurements()[i].getName() + ": " +
                        results.windowedResults.get(j)[i]);
        }
    }

}