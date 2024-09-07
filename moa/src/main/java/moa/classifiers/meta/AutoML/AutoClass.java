package moa.classifiers.meta.AutoML;

import com.github.javacliparser.FileOption;
import com.google.gson.Gson;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.meta.AdaptiveRandomForestRegressor;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


    public class AutoClass extends HeterogeneousEnsembleAbstract implements MultiClassClassifier {

        private static final long serialVersionUID = 1L;

        int instancesSeen;
        int iter;
        public int bestModel;
        public ArrayList<Algorithm> ensemble;
        public ArrayList<Algorithm> candidateEnsemble;
        public Instances windowPoints;
        HashMap<String, AdaptiveRandomForestRegressor> ARFregs = new HashMap<String, AdaptiveRandomForestRegressor>();
        GeneralConfiguration settings;
        ArrayList<Double> performanceMeasures;
        int verbose = 0;
        protected ExecutorService executor;
        int numberOfCores;
        int corrects;
        protected boolean[][] onlineHistory;
        // the file option dialogue in the UI
        public FileOption fileOption = new FileOption("ConfigurationFile", 'f', "Configuration file in json format.",
                "/Users/mbahri/Desktop/Dell/moa/src/main/java/moa/classifiers/meta/AutoML/settings.json", ".json", false);

        @Override
        public String getPurposeString() {
            return "Autoclass: Automl for data stream classification. "+
                    "Bahri, Maroua, and Nikolaos Georgantas. " +
                    "2023 IEEE International Conference on Big Data (BigData). IEEE, 2023.";
        }

        public void init() {
            this.fileOption.getFile();
        }


        @Override
        public boolean isRandomizable() {
            return false;
        }

       // @Override
        //public double[] getVotesForInstance(Instance inst) {
            //return null; }


        @Override
        public void resetLearningImpl() {
            this.performanceMeasures = new ArrayList<>();
            this.onlineHistory = new boolean[this.settings.ensembleSize][this.settings.windowSize];
            this.historyTotal = new double[this.settings.ensembleSize];
            this.instancesSeen = 0;
            this.corrects = 0;
            this.bestModel = 0;
            this.iter = 0;
            this.windowPoints = null ;

            // reset ARFrefs
            for (AdaptiveRandomForestRegressor ARFreg : this.ARFregs.values()) {
                ARFreg.resetLearning();
            }

            // reset individual classifiers
            for (int i = 0; i < this.ensemble.size(); i++) {
                //this.ensemble.get(i).init();
                this.ensemble.get(i).classifier.resetLearning();
            }

            if (this.settings.numberOfCores == -1) {
                this.numberOfCores = Runtime.getRuntime().availableProcessors();
            } else {
                this.numberOfCores = this.settings.numberOfCores;
            }
            this.executor = Executors.newFixedThreadPool(this.numberOfCores);
        }

        @Override
        public void trainOnInstanceImpl(Instance inst) {

            if (this.windowPoints == null) {
                this.windowPoints = new Instances(inst.dataset());
            }

            if (this.settings.windowSize <= this.windowPoints.numInstances()) {
                this.windowPoints.delete(0);
            }
            this.windowPoints.add(inst); // remember points of the current window
            this.instancesSeen++;

            int wValue = this.settings.windowSize;

            for (int i = 0; i < this.ensemble.size(); i++) {
                // Online performance estimation
                double [] votes = ensemble.get(i).classifier.getVotesForInstance(inst);
                boolean correct = (maxIndex(votes) * 1.0 == inst.classValue());


                // Maroua: boolean correct = (votes[maxIndex(votes)] * 1.0 == inst.classValue());
                if (correct && ! this.onlineHistory[i][instancesSeen % wValue]) {
                    // performance estimation increases
                    this.onlineHistory[i][instancesSeen % wValue] = true;

                    this.historyTotal[i] += 1.0/wValue;

                } else if (!correct &&  this.onlineHistory[i][instancesSeen % wValue]) {
                    // performance estimation decreases
                    this.onlineHistory[i][instancesSeen % wValue] = false;
                    this.historyTotal[i] -= 1.0/wValue;

                } else {
                    // nothing happens
                }
                }

            if (this.numberOfCores == 1) {
                // train all models with the instance
                this.performanceMeasures = new ArrayList<Double>(this.ensemble.size());
                double bestPerformance = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < this.ensemble.size(); i++) {
                    this.ensemble.get(i).classifier.trainOnInstance(inst);

                    // To extract the best performing mode
                    double performance =  this.historyTotal[i];
                    this.performanceMeasures.add(performance);
                    if (performance > bestPerformance) {
                        this.bestModel = i;
                        bestPerformance = performance;
                        }
                }

                if (this.settings.useTestEnsemble && this.candidateEnsemble.size() > 0) {
                    // train all models with the instance
                    for (int i = 0; i < this.candidateEnsemble.size(); i++) {  //Maroua; cétait Ensemble
                        this.candidateEnsemble.get(i).classifier.trainOnInstance(inst);
                    }
                }
            } else {
                //EnsembleClassifierAbstractAUTO.EnsembleRunnable
                ArrayList<EnsembleRunnable> trainers = new ArrayList<EnsembleRunnable>();
                for (int i = 0; i < this.ensemble.size(); i++) {
                    EnsembleRunnable trainer = new EnsembleRunnable(this.ensemble.get(i).classifier, inst);
                    trainers.add(trainer);
                }
                if (this.settings.useTestEnsemble && this.candidateEnsemble.size() > 0) {
                    // train all models with the instance
                    for (int i = 0; i < this.candidateEnsemble.size(); i++) {
                        EnsembleRunnable trainer = new EnsembleRunnable(this.candidateEnsemble.get(i).classifier, inst);
                        trainers.add(trainer);
                    }
                }
                try {
                    this.executor.invokeAll(trainers);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Could not call invokeAll() on training threads.");
                }
            }

            // every windowSize, we update the configurations
            if (this.instancesSeen % this.settings.windowSize == 0) {

                if (this.verbose >= 1) {
                    System.out.println(" ");
                    System.out.println("-------------- Processed " + instancesSeen + " Instances --------------");
                }

                updateConfiguration(); // update configuration
            }

        }

        /**
         * Returns votes using the best performing method
         * @param inst
         * @return votes
         */
        @Override
        public double[] getVotesForInstance(Instance inst) {


            return this.ensemble.get(this.bestModel).classifier.getVotesForInstance(inst);
//            DoubleVector combinedVote = new DoubleVector();
//
//            for(int i = 0 ; i < this.ensemble.size() ; ++i) {
//                DoubleVector vote = new DoubleVector(this.ensemble.get(i).classifier.getVotesForInstance(inst));
//                if (vote.sumOfValues() > 0.0) {
//                    vote.normalize();
//
//                    combinedVote.addValues(vote);
//                }
//            }
//            return combinedVote.getArrayRef();
        }


        protected void updateConfiguration() {
            // init evaluation measure
            if (this.verbose >= 2) {
                System.out.println(" ");
                System.out.println("---- Evaluate performance of current ensemble:");
            }
            evaluatePerformance();

            if (this.settings.useTestEnsemble) {
                promoteCandidatesIntoEnsemble();
            }

            if (this.verbose >= 1) {
                System.out.println("Classifier " + this.bestModel + " ("
                        + this.ensemble.get(this.bestModel).classifier.getCLICreationString(Classifier.class)
                        + ") is the active classifier with performance: " + this.performanceMeasures.get(this.bestModel));
            }

            generateNewConfigurations();

           // this.windowPoints.delete(); // flush the current window
            this.iter++;
        }

        protected void evaluatePerformance() {

            HashMap<String, Double> bestPerformanceValMap = new HashMap<String, Double>();
            HashMap<String, Integer> bestPerformanceIdxMap = new HashMap<String, Integer>();
            HashMap<String, Integer> algorithmCount = new HashMap<String, Integer>();

            this.performanceMeasures = new ArrayList<Double>(this.ensemble.size());
            double bestPerformance = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < this.ensemble.size(); i++) {

                // predict performance just for evaluation
                predictPerformance(this.ensemble.get(i));

                double performance =  this.historyTotal[i];
                this.performanceMeasures.add(performance);
                if (performance > bestPerformance) {
                    this.bestModel = i;
                    bestPerformance = performance;
                }

                if (this.verbose >= 1) {
                    System.out.println(i + ") " + this.ensemble.get(i).classifier.getCLICreationString(Classifier.class)
                            + "\t => \t performance: " + performance);
                }

//                String algorithm = this.ensemble.get(i).classifier.getPurposeString();
                String algorithm = this.ensemble.get(i).classifier.getClass().getName();
                if (!bestPerformanceIdxMap.containsKey(algorithm) || performance > bestPerformanceValMap.get(algorithm)) {
                    bestPerformanceValMap.put(algorithm, performance); // best performance per algorithm
                    bestPerformanceIdxMap.put(algorithm, i); // index of best performance per algorithm
                }

                // number of instances per algorithm in ensemble

                algorithmCount.put(algorithm, algorithmCount.getOrDefault(algorithm, 0) + 1);
                trainRegressor(this.ensemble.get(i), performance);
            }

            updateRemovalFlags(bestPerformanceValMap, bestPerformanceIdxMap, algorithmCount);
        }


        /**
         * Computes the accuracy of a learner for a given window of instances.
         * @param algorithm classifier to compute error
         * @return the computed accuracy.
         */
        protected double computePerformanceMeasure(Algorithm algorithm) {
            double acc = 0;
            this.trainOnChunk(algorithm);
            for (int i = 0; i < this.windowPoints.numInstances(); i++) {
                try {

                    double[] votes = algorithm.classifier.getVotesForInstance(this.windowPoints.instance(i));
                    boolean correct =  (maxIndex(votes)* 1.0 == this.windowPoints.instance(i).classValue());

                    if (correct){
                        acc += 1.0/this.windowPoints.numInstances();
                    }else
                        acc -= 1.0/this.windowPoints.numInstances();
                   // algorithm.classifier.trainOnInstance(this.windowPoints.instance(i));....
                } catch (Exception e) {
                    System.out.println("computePerformanceMeasure Error");
                }
            }
            algorithm.performanceMeasure = acc;
            return acc;
        }
        /**
         * Trains a classifier on the most recent window of data.
         *
         * @param algorithm
         *            Classifier being trained.
         */
        private void trainOnChunk(Algorithm algorithm) {
            for (int i = 0; i < this.windowPoints.numInstances(); i++) {
                algorithm.classifier.trainOnInstance(this.windowPoints.instance(i));
            }
        }


        protected void promoteCandidatesIntoEnsemble() {
            for (int i = 0; i < this.candidateEnsemble.size(); i++) {

                Algorithm newAlgorithm = this.candidateEnsemble.get(i);

                // predict performance just for evaluation
                predictPerformance(newAlgorithm);

                // evaluate
                double performance =  computePerformanceMeasure(newAlgorithm);

                if (this.verbose >= 1) {
                    System.out.println("Test " + i + ") " + newAlgorithm.classifier.getCLICreationString(Classifier.class)
                            + "\t => \t Performance: " + performance);
                }

                // replace if better than existing

                if (this.ensemble.size() < this.settings.ensembleSize) {
                    if (this.verbose >= 1) {
                        System.out.println("Promote " + newAlgorithm.classifier.getCLICreationString(Classifier.class)
                                + " from test ensemble to the ensemble as new configuration");
                    }

                    this.performanceMeasures.add(newAlgorithm.performanceMeasure);

                    this.ensemble.add(newAlgorithm);

                } else if (performance > AutoClass.getWorstSolution(this.performanceMeasures)) {

                    HashMap<Integer, Double> replace = getReplaceMap(this.performanceMeasures);

                    if (replace.size() == 0) {
                        return;
                    }

                    int replaceIdx = AutoClass.sampleProportionally(replace,
                            !this.settings.performanceMeasureMaximisation); // false
                    if (this.verbose >= 1) {
                        System.out.println("Promote " + newAlgorithm.classifier.getCLICreationString(Classifier.class)
                                + " from test ensemble to the ensemble by replacing " + replaceIdx);
                    }

                    // update performance measure
                    this.performanceMeasures.set(replaceIdx, newAlgorithm.performanceMeasure);

                    // replace in ensemble

                    this.ensemble.set(replaceIdx, newAlgorithm);
                }

            }
        }

        protected void trainRegressor(Algorithm algortihm, double performance) {
            double[] params = algortihm.getParamVector(1);
            params[params.length - 1] = performance; // add performance as class
            Instance inst = new DenseInstance(1.0, params);

            // add header to dataset TODO: do we need an attribute for the class label?
            Instances dataset = new Instances(null, algortihm.attributes, 0);
            dataset.setClassIndex(dataset.numAttributes()); // set class index to our performance feature
            inst.setDataset(dataset);

            // train adaptive random forest regressor based on performance of model
            this.ARFregs.get(algortihm.algorithm).trainOnInstanceImpl(inst);
        }

        protected void updateRemovalFlags(HashMap<String, Double> bestPerformanceValMap,
                                          HashMap<String, Integer> bestPerformanceIdxMap, HashMap<String, Integer> algorithmCount) {

            // reset flags
            for (Algorithm algorithm : ensemble) {
                algorithm.preventRemoval = false;
            }

            // only keep best overall algorithm
            if (this.settings.keepGlobalIncumbent) {
                this.ensemble.get(this.bestModel).preventRemoval = true;
            }

            // keep best instance per algorithm
            if (this.settings.keepAlgorithmIncumbents) {
                for (int idx : bestPerformanceIdxMap.values()) {
                    this.ensemble.get(idx).preventRemoval = true;
                }
            }

            // keep all default configurations
            if (this.settings.keepInitialConfigurations) {
                for (Algorithm algorithm : this.ensemble) {
                    if (algorithm.isDefault) {
                        algorithm.preventRemoval = true;
                    }
                }
            }

            // keep at least one instance per algorithm
            if (this.settings.preventAlgorithmDeath) {
                for (Algorithm algorithm : this.ensemble) {
                    if (algorithmCount.get(algorithm.algorithm) == 1) {
                        algorithm.preventRemoval = true;
                    }
                }
            }
        }

        // predict performance of new configuration
        protected void generateNewConfigurations() {

            // get performance values
            if (this.settings.useTestEnsemble) {
                candidateEnsemble.clear();
            }

            for (int z = 0; z < this.settings.newConfigurations; z++) {

                if (this.verbose == 2) {
                    System.out.println(" ");
                    System.out.println("---- Sample new configuration " + z + ":");
                }

                int parentIdx = sampleParent(this.performanceMeasures);
                Algorithm newAlgorithm = sampleNewConfiguration(parentIdx);

                if (this.settings.useTestEnsemble) {
                    if (this.verbose >= 1) {
                        System.out.println("Based on " + parentIdx + " add "
                                + newAlgorithm.classifier.getCLICreationString(Classifier.class) + " to test ensemble");
                    }
                    candidateEnsemble.add(newAlgorithm);
                } else {
                    double prediction = predictPerformance(newAlgorithm);

                    if (this.verbose >= 1) {
                        System.out.println("Based on " + parentIdx + " predict: "
                                + newAlgorithm.classifier.getCLICreationString(Classifier.class) + "\t => \t Performance: "
                                + prediction);
                    }

                    // the random forest only works with at least two training samples
                    if (Double.isNaN(prediction)) {
                        return;
                    }

                    // if we still have open slots in the ensemble (not full)
                    if (this.ensemble.size() < this.settings.ensembleSize) {
                        if (this.verbose >= 1) {
                            System.out.println("Add configuration as new algorithm.");
                        }

                        // add to ensemble
                        this.ensemble.add(newAlgorithm);

                        // update current performance with the prediction
                        this.performanceMeasures.add(prediction);
                    } else if (prediction > AutoClass.getWorstSolution(this.performanceMeasures)) {
                        // if the predicted performance is better than the one we have in the ensemble
                        HashMap<Integer, Double> replace = getReplaceMap(this.performanceMeasures);

                        if (replace.size() == 0) {
                            return;
                        }

                        int replaceIdx = AutoClass.sampleProportionally(replace,
                                !this.settings.performanceMeasureMaximisation); // false

                        if (this.verbose >= 1) {
                            System.out.println("Replace algorithm: " + replaceIdx);
                        }

                        // update current performance with the prediction
                        this.performanceMeasures.set(replaceIdx, prediction);

                        // replace in ensemble
                        this.ensemble.set(replaceIdx, newAlgorithm);
                    }
                }

            }

        }

        protected int sampleParent(ArrayList<Double> performM) {
            // copy existing classifier configuration
            HashMap<Integer, Double> parents = new HashMap<Integer, Double>();
            for (int i = 0; i < performM.size(); i++) {
                parents.put(i, performM.get(i));
            }
            int parentIdx = AutoClass.sampleProportionally(parents,
                    this.settings.performanceMeasureMaximisation); // true

            return parentIdx;
        }

        protected Algorithm sampleNewConfiguration(int parentIdx) {

            if (this.verbose >= 2) {
                System.out.println("Selected Configuration " + parentIdx + " as parent: "
                        + this.ensemble.get(parentIdx).classifier.getCLICreationString(Classifier.class));
            }
            Algorithm newAlgorithm = new Algorithm(this.ensemble.get(parentIdx), this.settings.lambda,
                    this.settings.resetProbability, this.settings.keepCurrentModel, this.verbose);

            return newAlgorithm;
        }

        protected double predictPerformance(Algorithm newAlgorithm) {
            // create instance from new configuration
            double[] params = newAlgorithm.getParamVector(0);
            Instance newInst = new DenseInstance(1.0, params);
            Instances newDataset = new Instances(null, newAlgorithm.attributes, 0);
            newDataset.setClassIndex(newDataset.numAttributes());
            newInst.setDataset(newDataset);

            // predict the performance of the new configuration using the trained adaptive
            // random forest
            double prediction = this.ARFregs.get(newAlgorithm.algorithm).getVotesForInstance(newInst)[0];

            newAlgorithm.prediction = prediction; // remember prediction

            return prediction;
        }

        // get mapping of algorithms and their performance that could be removed
        HashMap<Integer, Double> getReplaceMap(ArrayList<Double> performM) {
            HashMap<Integer, Double> replace = new HashMap<Integer, Double>();

            double worst = AutoClass.getWorstSolution(performM);

            // replace solutions that cannot get worse first
            if (worst <= -1.0) {
                for (int i = 0; i < this.ensemble.size(); i++) {
                    if (performM.get(i) <= -1.0 && !this.ensemble.get(i).preventRemoval) {
                        replace.put(i, performM.get(i));
                    }
                }
            }

            if (replace.size() == 0) {
                for (int i = 0; i < this.ensemble.size(); i++) {
                    if (!this.ensemble.get(i).preventRemoval) {
                        replace.put(i, performM.get(i));
                    }
                }
            }

            return replace;
        }

        // get lowest value in arraylist
        static double getWorstSolution(ArrayList<Double> values) {

            double min = Double.POSITIVE_INFINITY;
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) < min) {
                    min = values.get(i);
                }
            }
            return (min);
        }

        static int sampleProportionally(HashMap<Integer, Double> values, boolean maximisation) {

            // if we want to sample lower values with higher probability, we invert here
            if (!maximisation) {
                HashMap<Integer, Double> vals = new HashMap<Integer, Double>(values.size());

                for (int i : values.keySet()) {
                    vals.put(i, -1 * values.get(i));
                }
                return (AutoClass.rouletteWheelSelection(vals));
            }

            return (AutoClass.rouletteWheelSelection(values));
        }

        // sample an index from a list of values, proportionally to the respective value
        static int rouletteWheelSelection(HashMap<Integer, Double> values) {

            // get min
            double minVal = Double.POSITIVE_INFINITY;
            for (Double value : values.values()) {
                if (value < minVal) {
                    minVal = value;
                }
            }

            // to have a positive range we shift here
            double shift = Math.abs(minVal) - minVal;

            double completeWeight = 0.0;
            for (Double value : values.values()) {
                completeWeight += value + shift;
            }

            // sample random number within range of total weight
            double r = Math.random() * completeWeight;
            double countWeight = 0.0;

            for (int j : values.keySet()) {
                countWeight += values.get(j) + shift;
                if (countWeight >= r) {
                    return j;
                }
            }
            throw new RuntimeException("Sampling failed");
        }

        @Override
        protected Measurement[] getModelMeasurementsImpl() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void getModelDescription(StringBuilder out, int indent) {
            // TODO Auto-generated method stub
        }

        @Override
        public void setModelContext(InstancesHeader ih) {
            super.setModelContext(ih);

// This will cause issues in case setModelContext is invoked before the ensemble has been created.
//		It is likely safe to not perform this action due to how the context can be acquired later by the learners.
//		However, it is worth reviewing this in the future. See also HeterogeneousEnsembleAbstract.setModelContext
//            for (int i = 0; i < this.ensemble.size(); ++i) {
//                this.ensemble.get(i).classifier.setModelContext(ih);
//            }
        }

        @Override
        public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {

            try {

                // read settings from json
                BufferedReader bufferedReader = new BufferedReader(new FileReader(fileOption.getValue()));
                Gson gson = new Gson();
                // store settings in dedicated class structure
                this.settings = gson.fromJson(bufferedReader, GeneralConfiguration.class);

                this.instancesSeen = 0;
                this.bestModel = 0;
                this.iter = 0;
                this.windowPoints = null;

                // create the ensemble
                this.ensemble = new ArrayList<Algorithm>(this.settings.ensembleSize);
                // copy and initialise the provided starting configurations in the ensemble
                for (int i = 0; i < this.settings.algorithms.length; i++) {
                    this.ensemble.add(new Algorithm(this.settings.algorithms[i]));
                }

                if (this.settings.useTestEnsemble) {
                    this.candidateEnsemble = new ArrayList<Algorithm>(this.settings.newConfigurations);
                }

                // create one regressor per algorithm
                for (int i = 0; i < this.settings.algorithms.length; i++) {
                    AdaptiveRandomForestRegressor ARFreg = new AdaptiveRandomForestRegressor();
                    ARFreg.prepareForUse();
                    this.ARFregs.put(this.settings.algorithms[i].algorithm, ARFreg);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            super.prepareForUseImpl(monitor, repository);

        }

        // Modified from:
        // https://github.com/Waikato/moa/blob/master/moa/src/main/java/moa/classifiers/meta/AdaptiveRandomForest.java#L157
        // Helper class for parallelisation
        protected class EnsembleRunnable implements Runnable, Callable<Integer> {
            final private Classifier classifier;
            final private Instance instance;

            public EnsembleRunnable(Classifier classifier, Instance instance) {
                this.classifier = classifier;
                this.instance = instance;
            }

            @Override
            public void run() {
                classifier.trainOnInstance(this.instance);
            }

            @Override
            public Integer call() throws Exception {
                run();
                return 0;
            }
        }
    } // Close the EnsembleRunnable class

