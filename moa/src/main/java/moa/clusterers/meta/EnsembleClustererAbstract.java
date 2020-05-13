package moa.clusterers.meta;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.javacliparser.FileOption;
import com.google.gson.Gson;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.meta.AdaptiveRandomForestRegressor;
import moa.cluster.Clustering;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.Clusterer;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.evaluation.MeasureCollection;
import moa.gui.visualization.DataPoint;
import moa.options.ClassOption;
import moa.streams.clustering.RandomRBFGeneratorEvents;
import moa.tasks.TaskMonitor;

// The main flow is as follow:
// A json is read which contains the main settings and starting configurations / algorithms
// The json is used to initialize the three configuration classes below (same structure as json)
// From the json, we create the Algorithm and Parameter classes (depending on the type of the parameter) which form the ensemble of clusterers
// These classes are then used to cluster and evaluate the configurations
// When a new configuration is required, a parameter configuration is copied and the parameters manipulated

// these classes are initialised by gson and contain the starting configurations
// This class contains the individual parameter settings (such as limits and current value)
class ParameterConfiguration {
	public String parameter;
	public Object value;
	public Object[] range;
	public String type;
	public boolean optimise = true;
}

// This class contains the settings of an algorithm (such as name) as well as an
// array of Parameter Settings
class AlgorithmConfiguration {
	public String algorithm;
	public ParameterConfiguration[] parameters;
}

// This contains the general settings (such as the max ensemble size) as well as
// an array of Algorithm Settings
class GeneralConfiguration {
	public int windowSize = 1000;
	public int ensembleSize = 20;
	public int newConfigurations = 10;
	public AlgorithmConfiguration[] algorithms;
	public boolean keepCurrentModel = true;
	public double lambda = 0.05;
	public boolean preventAlgorithmDeath = true;
	public boolean reinitialiseWithClusters = true;
	public boolean evaluateMacro = false;
	public boolean keepGlobalIncumbent = true;
	public boolean keepAlgorithmIncumbents = true;
	public boolean keepInitialConfigurations = true;
	public boolean useTestEnsemble = true;
	public double resetProbability = 0.01;
	public int numberOfCores = 1;
	public String performanceMeasure = "SilhouetteCoefficient";
	public boolean performanceMeasureMaximisation = true;
}

public abstract class EnsembleClustererAbstract extends AbstractClusterer {

	private static final long serialVersionUID = 1L;

	int iteration;
	int instancesSeen;
	int iter;
	public int bestModel;
	public ArrayList<Algorithm> ensemble;
	public ArrayList<Algorithm> candidateEnsemble;
	public ArrayList<DataPoint> windowPoints;
	HashMap<String, AdaptiveRandomForestRegressor> ARFregs = new HashMap<String, AdaptiveRandomForestRegressor>();
	GeneralConfiguration settings;
	ArrayList<Double> performanceMeasures;
	int verbose = 0;
	protected ExecutorService executor;
	int numberOfCores;

	// the file option dialogue in the UI
	public FileOption fileOption = new FileOption("ConfigurationFile", 'f', "Configuration file in json format.",
			"settings.json", ".json", false);

	public void init() {
		this.fileOption.getFile();
	}

	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		return null;
	}

	@Override
	public Clustering getClusteringResult() {
		return null;
	}

	@Override
	public void resetLearningImpl() {

		this.instancesSeen = 0;
		this.bestModel = 0;
		this.iter = 0;
		this.windowPoints = new ArrayList<DataPoint>(this.settings.windowSize);

		// reset ARFrefs
		for (AdaptiveRandomForestRegressor ARFreg : this.ARFregs.values()) {
			ARFreg.resetLearning();
		}

		// reset individual clusterers
		for (int i = 0; i < this.ensemble.size(); i++) {
			// this.ensemble.get(i).clusterer.resetLearning();
			this.ensemble.get(i).init();
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

		// it appears to use numAttributes as the index when no class exists
		if (inst.classIndex() < inst.numAttributes()) {
			inst.deleteAttributeAt(inst.classIndex()); // remove class label
		}

		DataPoint point = new DataPoint(inst, instancesSeen); // create data points from instance
		this.windowPoints.add(point); // remember points of the current window
		this.instancesSeen++;

		if (this.numberOfCores == 1) {
			// train all models with the instance
			for (int i = 0; i < this.ensemble.size(); i++) {
				this.ensemble.get(i).clusterer.trainOnInstance(inst);
			}
			if (this.settings.useTestEnsemble && this.candidateEnsemble.size() > 0) {
				// train all models with the instance
				for (int i = 0; i < this.candidateEnsemble.size(); i++) {
					this.candidateEnsemble.get(i).clusterer.trainOnInstance(inst);
				}
			}
		} else {
			ArrayList<EnsembleRunnable> trainers = new ArrayList<EnsembleRunnable>();
			for (int i = 0; i < this.ensemble.size(); i++) {
				EnsembleRunnable trainer = new EnsembleRunnable(this.ensemble.get(i).clusterer, inst);
				trainers.add(trainer);
			}
			if (this.settings.useTestEnsemble && this.candidateEnsemble.size() > 0) {
				// train all models with the instance
				for (int i = 0; i < this.candidateEnsemble.size(); i++) {
					EnsembleRunnable trainer = new EnsembleRunnable(this.candidateEnsemble.get(i).clusterer, inst);
					trainers.add(trainer);
				}
			}
			try {
				this.executor.invokeAll(trainers);
			} catch (InterruptedException ex) {
				throw new RuntimeException("Could not call invokeAll() on training threads.");
			}
		}

		// every windowSize we update the configurations
		if (this.instancesSeen % this.settings.windowSize == 0) {
			if (this.verbose >= 1) {
				System.out.println(" ");
				System.out.println("-------------- Processed " + instancesSeen + " Instances --------------");
			}

			updateConfiguration(); // update configuration
		}

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
			System.out.println("Clusterer " + this.bestModel + " ("
					+ this.ensemble.get(this.bestModel).clusterer.getCLICreationString(Clusterer.class)
					+ ") is the active clusterer with performance: " + this.performanceMeasures.get(this.bestModel));
		}

		generateNewConfigurations();

		this.windowPoints.clear(); // flush the current window
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

			double performance = computePerformanceMeasure(this.ensemble.get(i));
			this.performanceMeasures.add(performance);
			if (performance > bestPerformance) {
				this.bestModel = i;
				bestPerformance = performance;
			}

			if (this.verbose >= 1) {
				System.out.println(i + ") " + this.ensemble.get(i).clusterer.getCLICreationString(Clusterer.class)
						+ "\t => \t performance: " + performance);
			}

			String algorithm = this.ensemble.get(i).algorithm;
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

	protected double computePerformanceMeasure(Algorithm algorithm) {

		ClassOption opt = new ClassOption("", ' ', "", MeasureCollection.class, this.settings.performanceMeasure);
		MeasureCollection performanceMeasure = (MeasureCollection) opt.materializeObject(null, null);

		// compare micro-clusters
		Clustering result = null;
		if (!this.settings.evaluateMacro) {
			result = algorithm.clusterer.getMicroClusteringResult();
		}
		// compare macro-clusters
		if (this.settings.evaluateMacro || result == null) {
			// this is also the fallback for algorithms which dont export micro clusters
			// Note: This is not a fair comparison but otherwise we would have to discard
			// these algorithms entirely.
			if (this.verbose >= 2)
				System.out.println("Micro-Cluster not available for "
						+ algorithm.clusterer.getCLICreationString(Clusterer.class) + ". Try Macro-Clusters instead.");
			result = algorithm.clusterer.getClusteringResult();
		}

		double performance;
		if (result == null) {
			throw new RuntimeException("Neither micro- nor macro clusters available for "
					+ algorithm.clusterer.getCLICreationString(Clusterer.class));
		} else if (result.size() == 0 || result.size() == 1) {
			performance = -1.0; // discourage solutions with no or a single cluster
		} else {
			// evaluate clustering using evaluation measure
			try {
				performanceMeasure.evaluateClusteringPerformance(result, null, windowPoints);
			} catch (Exception e) {
				throw new RuntimeException("Could not compute clustering performance.");
			}
			performance = performanceMeasure.getLastValue(0);
			// e.g., if ownDistance == otherDistance == 0 the Silhouette will return NaN
			if (Double.isNaN(performance)) {
				performance = -1.0;
			}
		}
		algorithm.performanceMeasure = performance;

		return performance;
	}

	protected void promoteCandidatesIntoEnsemble() {

		for (int i = 0; i < this.candidateEnsemble.size(); i++) {

			Algorithm newAlgorithm = this.candidateEnsemble.get(i);

			// predict performance just for evaluation
			predictPerformance(newAlgorithm);

			// evaluate
			double performance = computePerformanceMeasure(newAlgorithm);

			if (this.verbose >= 1) {
				System.out.println("Test " + i + ") " + newAlgorithm.clusterer.getCLICreationString(Clusterer.class)
						+ "\t => \t Performance: " + performance);
			}

			// replace if better than existing
			if (this.ensemble.size() < this.settings.ensembleSize) {
				if (this.verbose >= 1) {
					System.out.println("Promote " + newAlgorithm.clusterer.getCLICreationString(Clusterer.class)
							+ " from test ensemble to the ensemble as new configuration");
				}

				this.performanceMeasures.add(newAlgorithm.performanceMeasure);

				this.ensemble.add(newAlgorithm);

			} else if (performance > EnsembleClustererAbstract.getWorstSolution(this.performanceMeasures)) {

				HashMap<Integer, Double> replace = getReplaceMap(this.performanceMeasures);

				if (replace.size() == 0) {
					return;
				}

				int replaceIdx = EnsembleClustererAbstract.sampleProportionally(replace,
						!this.settings.performanceMeasureMaximisation); // false

				if (this.verbose >= 1) {
					System.out.println("Promote " + newAlgorithm.clusterer.getCLICreationString(Clusterer.class)
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
			Algorithm newAlgorithm = sampleNewConfiguration(this.performanceMeasures, parentIdx);

			if (this.settings.useTestEnsemble) {
				if (this.verbose >= 1) {
					System.out.println("Based on " + parentIdx + " add "
							+ newAlgorithm.clusterer.getCLICreationString(Clusterer.class) + " to test ensemble");
				}
				candidateEnsemble.add(newAlgorithm);
			} else {
				double prediction = predictPerformance(newAlgorithm);

				if (this.verbose >= 1) {
					System.out.println("Based on " + parentIdx + " predict: "
							+ newAlgorithm.clusterer.getCLICreationString(Clusterer.class) + "\t => \t Performance: "
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

				} else if (prediction > EnsembleClustererAbstract.getWorstSolution(this.performanceMeasures)) {
					// if the predicted performance is better than the one we have in the ensemble
					HashMap<Integer, Double> replace = getReplaceMap(this.performanceMeasures);

					if (replace.size() == 0) {
						return;
					}

					int replaceIdx = EnsembleClustererAbstract.sampleProportionally(replace,
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

	protected int sampleParent(ArrayList<Double> silhs) {
		// copy existing clusterer configuration
		HashMap<Integer, Double> parents = new HashMap<Integer, Double>();
		for (int i = 0; i < silhs.size(); i++) {
			parents.put(i, silhs.get(i));
		}
		int parentIdx = EnsembleClustererAbstract.sampleProportionally(parents,
				this.settings.performanceMeasureMaximisation); // true

		return parentIdx;
	}

	protected Algorithm sampleNewConfiguration(ArrayList<Double> silhs, int parentIdx) {

		if (this.verbose >= 2) {
			System.out.println("Selected Configuration " + parentIdx + " as parent: "
					+ this.ensemble.get(parentIdx).clusterer.getCLICreationString(Clusterer.class));
		}
		Algorithm newAlgorithm = new Algorithm(this.ensemble.get(parentIdx), this.settings.lambda,
				this.settings.resetProbability, this.settings.keepCurrentModel, this.settings.reinitialiseWithClusters,
				this.verbose);

		return newAlgorithm;
	}

	protected double predictPerformance(Algorithm newAlgorithm) {
		// create a data point from new configuration
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
	HashMap<Integer, Double> getReplaceMap(ArrayList<Double> silhs) {
		HashMap<Integer, Double> replace = new HashMap<Integer, Double>();

		double worst = EnsembleClustererAbstract.getWorstSolution(silhs);

		// replace solutions that cannot get worse first
		if (worst <= -1.0) {
			for (int i = 0; i < this.ensemble.size(); i++) {
				if (silhs.get(i) <= -1.0 && !this.ensemble.get(i).preventRemoval) {
					replace.put(i, silhs.get(i));
				}
			}
		}

		if (replace.size() == 0) {
			for (int i = 0; i < this.ensemble.size(); i++) {
				if (!this.ensemble.get(i).preventRemoval) {
					replace.put(i, silhs.get(i));
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
			return (EnsembleClustererAbstract.rouletteWheelSelection(vals));
		}

		return (EnsembleClustererAbstract.rouletteWheelSelection(values));
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
			this.windowPoints = new ArrayList<DataPoint>(this.settings.windowSize);

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
		final private AbstractClusterer clusterer;
		final private Instance instance;

		public EnsembleRunnable(AbstractClusterer clusterer, Instance instance) {
			this.clusterer = clusterer;
			this.instance = instance;
		}

		@Override
		public void run() {
			clusterer.trainOnInstance(this.instance);
		}

		@Override
		public Integer call() throws Exception {
			run();
			return 0;
		}
	}

	public static void main(String[] args) throws Exception {

		// create a stream
		RandomRBFGeneratorEvents stream = new RandomRBFGeneratorEvents();
		stream.prepareForUse();

		// create confStream algorithm
		ConfStream confStream = new ConfStream();
		confStream.fileOption.setValue(System.getProperty("user.dir") + "/moa/src/main/java/moa/clusterers/meta/settings.json");
		confStream.prepareForUse();

		// train the algorithm
		for (int i = 1; i < 5000; i++) {
			Instance inst = stream.nextInstance().getData();
			confStream.trainOnInstanceImpl(inst);
		}

		// get micro clusters
		Clustering micro = confStream.getMicroClusteringResult();
	}
}
