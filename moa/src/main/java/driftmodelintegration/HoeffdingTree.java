package driftmodelintegration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import driftmodelintegration.core.Data;
import driftmodelintegration.core.Learner;
import driftmodelintegration.core.LearnerUtils;
import driftmodelintegration.core.QualityCriterion;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * Learns a {@link HoeffdingTreeModel} representing a Hoeffding Tree as described by<br/>
 * <br/>
 * <b>Domingos/Hulten/2000: Mining HighSpeed Data Streams</b>
 *
 * @author Tobias Beckers
 *
 */
public class HoeffdingTree implements Learner<Data, HoeffdingTreeModel> {

	/** The unique class ID*/
	private static final long serialVersionUID = -2578445642027682454L;

//	private static final Logger logger = LoggerFactory.getLogger(HoeffdingTree.class);

	// static fields to declare the quality criterion
	/** use this field to define the quality criterion to be information gain */
	public static final QualityCriterion INFORMATION_GAIN = QualityCriterion.INFO_GAIN;

	/** use this field to define the quality criterion to be Gini-Index gain*/
	public static final QualityCriterion GINI_INDEX = QualityCriterion.GINI_INDEX;

	// default values
	/** The default number of different values existing for one feature to declare the feature {@link FeatureType#NUMERIC}: 5 */
	public static final int DEFAULT_DECLARE_NUMERIC = 5;

	/** The default quantiles for finding thresholds for numeric features: 0.25, 0.5, 0.75, 1 */
	public static final Double[] DEFAULT_QUANTILES = {0.25, 0.5, 0.75, 1d};

	/** The default value for delta. Delta is '1 - the probability to choose the correct feature at each node': 0.0000001 */
	public static final double DEFAULT_DELTA = 0.0000001;

	/** defines the default quality criterion: information gain */
	public static final QualityCriterion DEFAULT_QUALITY_CRITERION = INFORMATION_GAIN;

	// variables to update the model
	/** The resulting HoeffdingTree which is incrementally updated */
	protected HoeffdingTreeModel tree;
	/** contains all possible features and for each feature all possible values */
	protected Map<String, List<Serializable>> featureValuePairs;
	/** all possible labels */
	protected List<Serializable> labels;
	/** contains all current leafs and for each leaf the corresponding counters */
	protected Map<HoeffdingTreeNode, NodeData> NodeDataPairs;

	// parameter
	/** the numerator of the fraction under the square root in the hoeffding bound (used for epsilon computation) */
	protected double numerator;
	/** the chosen {@link QualityCriterion} */
	protected QualityCriterion qualityCriterion;
	/** defines which value the quality function must deliver at least to split a node. Default: Worst value */
	protected double minQualityForSplit;

	// variables
	/** indicates if the learner is initialized so it has all necessary information for learning */
	protected boolean initialized;

	/**
	 * Empty Constructor to instantiate the learner without initializing. The call of {@link #learn(LabeledExample)} will
	 * have no effect until the learner is initialized.<br/>
	 * It is recommended to instantiate the leaner with the constructors
	 * {@link #HoeffdingTree(Collection)} or {@link #HoeffdingTree(Map, Map, List, double, QualityCriterion, double)}.
	 */

	protected String labelAttribute;


	public HoeffdingTree(){
		this.labelAttribute = null;
		this.initialized = false;
	}


	public HoeffdingTree( String label ) {
		this.labelAttribute = label;
		this.initialized = false;
	}


	/**
	 * @return the labelAttribute
	 */
	public String getLabelAttribute() {
		return labelAttribute;
	}


	/**
	 * @param labelAttribute the labelAttribute to set
	 */
	public void setLabelAttribute(String labelAttribute) {
		this.labelAttribute = labelAttribute;
	}


	/**
	 * Most extensive constructor to define all parameters by setting them manually
	 * @param featureTypes for each feature one of {@link HoeffdingTree#NOMINAL} or {@link HoeffdingTree#NUMERIC}.
	 * Use {@link HoeffdingTree#estimateFeatureTypes(Collection, int)} to create this automatically.
	 * @param featureValuePairs must contain all possible features and for each feature all possible values or thresholds if numeric.
	 * Use {@link HoeffdingTree#constructFeatureValuePairs(Collection, Map, double...)} to create this automatically.
	 * @param labels must contain all possible labels. Use {@link HoeffdingTree#getAllLabels(Collection)} to extract all labels automatically.
	 * @param delta the error bound parameter, is one minus the desired probability of choosing the correct
	 * attribute at any given node. Default value: {@link HoeffdingTree#DEFAULT_DELTA}
	 * @param qualityCriterion a {@link QualityCriterion}, e.g. {@link HoeffdingTree#INFORMATION_GAIN}.
	 * Default: {@link HoeffdingTree#DEFAULT_QUALITY_CRITERION}
	 * @param minQualityForSplit used for pre-pruning: A node is not split until the hoeffding bound guarantees
	 * that the best feature has a quality of at least the specified. Default should be the {@link QualityCriterion#getLowestGain()}
	 * for the chosen criterion. For default criterion: {@link HoeffdingTree#DEFAULT_QUALITY_CRITERION#getLowestGain()}
	 */
	public HoeffdingTree(Map<String, Class<?>> featureTypes, Map<String, List<Serializable>> featureValuePairs,
			List<Serializable> labels, double delta, QualityCriterion qualityCriterion, double minQualityForSplit) {
		this.initialize(featureTypes, featureValuePairs, labels, delta, qualityCriterion, minQualityForSplit);
	}

	/**
	 * Constructor sets all parameters to default
	 * @param examples a collection of {@link LabeledExample}s containing all possible features, all possible values for
	 * each nominal feature (and as much as possible for numeric features) and all possible labels.
	 */
	public HoeffdingTree(Collection<Data> examples) {
		this.initialize(examples);
	}





	/* (non-Javadoc)
	 * @see stream.learner.Learner#init()
	 */
	@Override
	public void init() {
		this.initialize( new HashSet<Data>() );
	}

	/**
	 * Initializes the learner with all necessary information. See constructor
	 * {@link #HoeffdingTree(Map, Map, List, double, QualityCriterion, double)} to get a description of all parameters.
	 * @see #HoeffdingTree(Map, Map, List, double, QualityCriterion, double)
	 */
	public void initialize(Map<String, Class<?>> featureTypes, Map<String, List<Serializable>> featureValuePairs,
			List<Serializable> labels, double delta, QualityCriterion qualityCriterion, double minQualityForSplit) {
		this.qualityCriterion = qualityCriterion;
		this.minQualityForSplit = minQualityForSplit;
		this.labels = labels;
		this.featureValuePairs = featureValuePairs;
		this.tree = new HoeffdingTreeModel(featureTypes, labels.get(0));
		this.NodeDataPairs = new HashMap<HoeffdingTreeNode, NodeData>();
		// let the remaining features for the root be the initial features
		this.NodeDataPairs.put(this.tree.getLeaf(null), new NodeData( this.labelAttribute, new ArrayList<String>(featureValuePairs.keySet())));
		double range = qualityCriterion.getHighestGain(labels.size());
		this.numerator = range*range * Math.log(1d/delta);
		this.initialized = true;

/*		logger.debug("HoeffdingTree learner initialized with\n quality criterion: "+this.qualityCriterion+" gain,\n " +
				"prepruning min quality value: "+this.minQualityForSplit+",\n " +
				"delta: "+delta+"\n " +
				"valid class labels: "+this.labels+",\n " +
				"feature types: "+featureTypes+",\n "+
				"valid (nominal) / threshold (numeric) feature values: "+this.featureValuePairs+",\n " +
				"highest value of quality criterion (for hoeffding bound computation): "+range);
*/	}

	/**
	 * Initializes the learner by extracting all necessary information from the specified initial example set.
	 * See constructor {@link #HoeffdingTree(Collection)} for more information.
	 * @see #HoeffdingTree(Collection)
	 */
	public void initialize(Collection<Data> examples) {
		this.initialize( LearnerUtils.getTypes( examples ), //estimateFeatureTypes(examples, DEFAULT_DECLARE_NUMERIC),
				constructFeatureValuePairs(examples, LearnerUtils.getTypes( examples ) ), //estimateFeatureTypes(examples, DEFAULT_DECLARE_NUMERIC), DEFAULT_QUANTILES),
				getAllValues(examples, labelAttribute), // getAllLabels(examples),
				DEFAULT_DELTA,
				DEFAULT_QUALITY_CRITERION,
				DEFAULT_QUALITY_CRITERION.getLowestGain());
	}

	/**
	 * Method for convenient initialization.<br/>
	 * Returns a list of all values - including duplicates - the specified feature can have in the specified collection of examples.
	 * @param examples all examples
	 * @param feature a feature which values are wanted
	 * @return a list of all values - including duplicates - the specified feature can have in the specified collection of examples.
	 */
	public List<Serializable> getAllValues(Collection<Data> examples, String feature) {
		List<Serializable> allValues = new ArrayList<Serializable>();
		for (Data example : examples) {
			if( ! LearnerUtils.isHidden( feature ) ){
				Serializable value = example.get(feature);
				if (value != null) {
					allValues.add(value);
				}
			}
		}
		return allValues;
	}

	/**
	 * Method for convenient initialization.<br/>
	 * Extracts all values from all nominal features and estimates threshold values for numeric features.<br/>
	 * Returns all (discretized) possible values for each feature.
	 * @param examples examples must contain all possible features and all possible values for nominal features
	 * @param featureTypes for each feature one of {@link HoeffdingTree#NOMINAL} or {@link HoeffdingTree#NUMERIC}
	 * use {@link HoeffdingTree#estimateFeatureTypes(Collection, int)} to create this automatically
	 * @param quantileThresholds quantiles to find the best thresholds. Default: {@link HoeffdingTree#DEFAULT_QUANTILES}
	 * @return Returns all (discretized) possible values for each feature.
	 */
	public Map<String, List<Serializable>> constructFeatureValuePairs(Collection<Data> examples, Map<String, Class<?>> featureTypes, Double ... quantileThresholds ) {
		Map<String, List<Serializable>> featureValuePairs = new HashMap<String, List<Serializable>>();
		Set<String> allFeatures = getAllFeatures(examples);
		for (String feature : allFeatures) {
			List<Serializable> allValuesDuplicate = getAllValues(examples, feature);
			if(featureTypes.get(feature) != Double.class) {
				List<Serializable> allValuesUnique = new ArrayList<Serializable>(new HashSet<Serializable>(allValuesDuplicate));
				featureValuePairs.put(feature, allValuesUnique);
			}
			else {
				featureValuePairs.put(feature, getNumericThresholds(allValuesDuplicate, quantileThresholds));
			}
		}
		return featureValuePairs;
	}


	/**
	 * Method for convenient initialization.<br/>
	 * Returns a set of all features seen at least once in the specified collection of examples.
	 * @param examples a collection of {@link Example}s to extract the features from
	 * @return a set of all features seen at least once in the specified collection of examples.
	 */
	public Set<String> getAllFeatures(Collection<Data> examples) {
		Set<String> features = new HashSet<String>();
		for (Data example : examples) {
			for( String feature : LearnerUtils.getAttributes( example ) ){
				if( ! feature.equals( labelAttribute ) && ! LearnerUtils.isHiddenOrSpecial( feature ))
					features.add( feature );
			}
		}
		return features;
	}

	/**
	 * Method for convenient initialization.<br/>
	 * Returns a list of thresholds for testing values of the specified domain.<br/>
	 * All seen values - including duplicate values - should be specified.<br/>
	 * Quantiles must reside in interval [0,1]. The Number of specified quantiles define the number of returned thresholds
	 * (if enough different values exist). Default quantiles: {@link HoeffdingTree#DEFAULT_QUANTILES}
	 * @param values all seen values, including duplicates
	 * @param quantileThresholds quantiles to find the best thresholds. Default: {@link HoeffdingTree#DEFAULT_QUANTILES}
	 * @return Returns a list of thresholds for numeric features
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<Serializable> getNumericThresholds(Collection<Serializable> values, Double ... quantileThresholds) {
		List<Serializable> thresholds = new ArrayList<Serializable>();
		Arrays.sort(quantileThresholds);
		List<Serializable> orderedValues = new ArrayList<Serializable>(values);
		Collections.sort((List)orderedValues);
		for (int i = 0; i < quantileThresholds.length; i++) {
			// index = quantile * #values - 1
			int nextThresholdPosition = Math.round( (float)( quantileThresholds[i]*values.size() ) ) - 1;
			if (nextThresholdPosition < 0) {
				nextThresholdPosition = 0;
			}
			if (nextThresholdPosition >= values.size()) {
				nextThresholdPosition = values.size() - 1;
			}
			Serializable nextThreshold = orderedValues.get(nextThresholdPosition);
			if (thresholds.isEmpty() || !thresholds.get(thresholds.size()-1).equals(nextThreshold)) {
				thresholds.add(nextThreshold);
			}
		}
		return thresholds;
	}


	/**
	 * Maps the specified numeric value to one of the specified nominal values.<br/>
	 * Numeric and nominal values must be directly comparable.
	 * @param nominalValues possible mappings
	 * @param numericValue a numeric value
	 * @return the nominal value to which the numeric value is mapped.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Comparable numericToNominal(Collection<Comparable> nominalValues, Comparable numericValue) {
		Comparable smallestNominal = null;
		for (Comparable nominal : nominalValues) {
			if ( numericValue.compareTo(nominal) <= 0 ) {
				if (smallestNominal == null || nominal.compareTo(smallestNominal) < 0) { smallestNominal = nominal; }
			}
		}
		if (smallestNominal == null) {
			// numeric value was higher than the highest nominal value, so return the highest nominal value
			for (Comparable nominal : nominalValues) {
				if (smallestNominal == null || smallestNominal.compareTo(nominal) < 0) { smallestNominal = nominal; }
			}
		}
		return smallestNominal;
	}

	/**
	 * Returns the so far learned tree. At any time through the learning process, this method returns a valid tree.
	 * @return the so far learned tree
	 */
	@Override
	public HoeffdingTreeModel getModel() {
		return this.tree;
	}

	/**
	 * This method implements 'The Hoeffding tree algorithm' as presented in Table 1 on page 3 in<br/>
	 * <b>Domingos/Hulten/2000: Mining HighSpeed Data Streams</b> as precisely as possible.<br/>
	 * The comments in the method are taken from the original pseudo code implementation and each describes the following line(s).
	 */
	@Override
	// For each example (x, y_k) in S
	public void learn(Data example) {
		if (!this.initialized) {
//			logger.warn( "Learner has not been initialized!" );
			return;
		}
		// Sort (x, y) into a leaf l using HT.
		HoeffdingTreeNode leaf = this.tree.getLeaf(example);
		NodeData leafData = this.NodeDataPairs.get(leaf);
		if (leafData.getRemainingFeatures().isEmpty()) {
			return;
		}
		// For each x_ij in x such that X_i is in X_l : Increment n_ijk(l).
		leafData.addExample(example);
		// Label l with the majority class among the examples seen so far at l.
		leaf.setLabel(leafData.getMajorityClass());
		// If the examples seen so far at l are not all of the same class, then
		if (!leafData.isUniformLabel()) {
			// Compute G_l(X_i) for each attribute X_i in X_l - {X_null} using the counts n_ijk(l).
			Map<String, Double> featureQuality = leafData.getQualityAllAttributes();
			// Let X_a be the attribute with highest G_l.
			String x_a = LearnerUtils.getMaximumKey( featureQuality );  //getBestFeature(featureQuality);
			double x_aG_lValue = featureQuality.get(x_a);

			// Let X_b be the attribute with second-highest G_l.
			featureQuality.remove(x_a);
			String x_b = LearnerUtils.getMaximumKey( featureQuality ); //getBestFeature(featureQuality);
			double x_bG_lValue = 0d;
			if (x_b != null) {
				x_bG_lValue = featureQuality.get(x_b);
			}

			// Compute epsilon using Equation 1.
			// Equation 1 - Hoeffding bound (or additive Chernoff bound):  epsilon = sqrt( (R^2 * ln(1/delta)) / 2n )
			// where R is the range of the estimated random variable (e.g. 1 for a probability or log2(#classes) for information gain).
			double epsilon = Math.sqrt( this.numerator / (2*leafData.getExampleCount()) );
			// If G_l(X_a) - G_l(X_b) > epsilon and X_a != X_null , then
			if ((x_aG_lValue - x_bG_lValue > epsilon) && ((x_aG_lValue - this.minQualityForSplit) >= epsilon)) {
				// Replace l by an internal node that splits on X_a.
				leaf.setFeature(x_a);
				leaf.setLabel(null);
				this.NodeDataPairs.remove(leaf);
				List<String> remainingFeatures = new ArrayList<String>(leafData.getRemainingFeatures());
				remainingFeatures.remove(x_a);
				// For each branch of the split
				for (Serializable value : this.featureValuePairs.get(x_a)) {
					// Add a new leaf l_m, and let X_m = X - {X_a} (I suppose it should be 'X_l' instead of 'X').
					// Let G_m(X_null) be the G obtained by predicting the most frequent class at l_m. (not explicitly implemented here)
					HoeffdingTreeNode newChild = new HoeffdingTreeNode(leafData.getMajorityClass(x_a, value));
					leaf.addChild(newChild, value);
					// For each class y_k and each value x_ij of each attribute X_i in X_m - {X_null}
					//   Let n_ijk(l_m) = 0.
					this.NodeDataPairs.put(newChild, new NodeData(this.labelAttribute, remainingFeatures));
				}
/*				logger.debug("split node on feature "+x_a+"\n " +
						"examples processed at this node: "+leafData.getExampleCount()+"\n " +
						"Epsilon="+epsilon+", G("+x_a+")="+x_aG_lValue+", 2nd best: G("+x_b+")="+x_bG_lValue+"\n " +
						"creating "+this.featureValuePairs.get(x_a).size()+" children for values: "+this.featureValuePairs.get(x_a)+"\n " +
						"remaining features for each child: "+remainingFeatures+"\n " +
						"new tree:\n"+this.tree.toString());
*/			}
		}
	}




	/**
	 * Gathers all necessary data for a corresponding node including remaining features and counters in one object.
	 * @author Tobias Beckers
	 *
	 */
	protected class NodeData implements Serializable{

		private static final long serialVersionUID = 1L;

		/** contains all remaining features */
		private final List<String> remainingFeatures;

		/**
		 * [remaining feature i][value j for feature i][label k] = number of examples arrived in the corresponding node
		 * where feature i had value j and class was k.
		 */
		private final int[][][] counter;

		/** the total number of examples arrived at the corresponding node */
		private int exampleCount;

		/** Contains the number of examples that will have to be seen to recompute the majority class for the corresponding node. */
		private int examplesTillNextLabelUpdate;

		/** contains the majority class of the last update */
		private Serializable lastMajorityClass;

		/** specifies the feature with the fewest values to speed up some feature independent computations */
		private final int fewestValuesFeature;

		/** specifies if all seen examples in the corresponding node have the same label */
		private boolean isUniLabel;

		/** specifies the label index, if {@link #isUniLabel} is true */
		private int uniLabelIndex;

		String labelAttribute;

		/**
		 * Constructs a node data object for a node with the specified remaining features. Sets all counters to '0'.
		 * @param remainingFeatures the remaining features for the associated node
		 */
		public NodeData(String labelAttribute, List<String> remainingFeatures) {
			this.labelAttribute = labelAttribute;
			this.exampleCount = 0;
			this.examplesTillNextLabelUpdate = 0;
			this.lastMajorityClass = null;
			this.isUniLabel = true;
			this.uniLabelIndex = -1;
			this.remainingFeatures = remainingFeatures;
			this.counter = new int[remainingFeatures.size()][][];
			int i = 0;
			int fewestValuesCount = -1;
			int tempFewestValuesFeature = 0;
			for (String feature : remainingFeatures) {
				int valuesCount = HoeffdingTree.this.featureValuePairs.get(feature).size();
				this.counter[i] = new int[valuesCount][HoeffdingTree.this.labels.size()];
				if (fewestValuesCount == -1 || valuesCount < fewestValuesCount) {
					tempFewestValuesFeature = i;
					fewestValuesCount = valuesCount;
				}
				i++;
			}
			this.fewestValuesFeature = tempFewestValuesFeature;
			this.initCounter();
		}

		/**
		 * Sets all counters to 0.
		 */
		private void initCounter() {
			for (int i = 0; i < this.counter.length; i++) {
				for (int j = 0; j < this.counter[i].length; j++) {
					for (int k = 0; k < this.counter[i][j].length; k++) {
						this.counter[i][j][k] = 0;
					}
				}
			}
		}

		/**
		 * Extracts the needed data of the specified example and adds it to the node data
		 * @param example the example which data should be added to the node data
		 */
		public void addExample( Data example ) {
			// increment example count
			this.exampleCount++;

			// update value for majority class recomputing time estimation

			// If the equals method is fast for the used labels, the estimation when to update the majority class can be improved
			// here to speed up the majority class computation.
			Serializable label = LearnerUtils.getLabel( example );
			if( label == null ){
//				logger.warn( "No label found for example: {}", example );
				return;
			}

			if ( LearnerUtils.getLabel(example).equals(this.lastMajorityClass)) {
				this.examplesTillNextLabelUpdate++;
			} else {
				this.examplesTillNextLabelUpdate--;
				// if the equals method is slow for the used labels, the above improvement of estimating the next majority class
				// update time will result in a contra-productive deceleration of majority class computation.
				// In this case using the following line (means poorer estimation) instead of the previous two code lines is recommended.
				// this.examplesTillNextLabelUpdate--;
			}

			// increment the counters
			int labelIndex = HoeffdingTree.this.labels.indexOf( label );
			for (String feature : this.remainingFeatures) {
				this.incrementCounter(feature, example.get(feature), labelIndex);
			}

			// update the uniform label flag
			if (this.isUniLabel) {
				if (this.uniLabelIndex == -1) {
					this.uniLabelIndex = labelIndex;
				} else {
					if (this.uniLabelIndex != labelIndex) {
						this.isUniLabel = false;
						this.uniLabelIndex = 0;
					}
				}
			}
		}

		/**
		 * Returns all features remaining for the associated node
		 * @return all remaining features
		 */
		public List<String> getRemainingFeatures() {
			return this.remainingFeatures;
		}

		/**
		 * Increments the counter associated with the specified parameters. Does nothing if the specified feature
		 * is not remaining
		 * @param feature a feature
		 * @param value a valid value for the specified feature
		 * @param labelIndex the index of the class label
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void incrementCounter(String feature, Serializable value, int labelIndex) {
			int featureIndex = this.remainingFeatures.indexOf(feature);
			if (featureIndex != -1) {
				Class<?> type = tree.getType( feature );
				if( type == Double.class ){
					this.counter[featureIndex]
					             [HoeffdingTree.this.featureValuePairs.get(feature).indexOf(numericToNominal((List)HoeffdingTree.this.featureValuePairs.get(feature), (Comparable<?>)value))]
					              [labelIndex]++;
					return;
				} else {
//				    System.out.print(HoeffdingTree.this.featureValuePairs.get(feature).indexOf(value));
					this.counter[featureIndex][HoeffdingTree.this.featureValuePairs.get(feature).indexOf(value)][labelIndex]++;
				}
			}
		}

		/**
		 * Returns the label of the most frequent class from all so far seen examples in the corresponding node.
		 * @return the most frequent class label
		 */
		public Serializable getMajorityClass() {
			// Speed up calculation by testing the difference between most and second most frequent label.
			if (this.examplesTillNextLabelUpdate <= 0) {

				if (this.isUniLabel) {
					this.lastMajorityClass = HoeffdingTree.this.labels.get(this.uniLabelIndex);
					this.examplesTillNextLabelUpdate = this.exampleCount + 1;
					return this.lastMajorityClass;
				}

				int majorityClass = 0;
				int highestFrequency = 0;
				int secondHighestFrequency = 0;
				for (int label = 0; label < HoeffdingTree.this.labels.size(); label++) {
					int currentFrequency = 0;
					for (int value = 0; value < this.counter[this.fewestValuesFeature].length; value++) {
						currentFrequency += this.counter[this.fewestValuesFeature][value][label];
					}
					if (currentFrequency > highestFrequency) {
						secondHighestFrequency = highestFrequency;
						highestFrequency = currentFrequency;
						majorityClass = label;
					}
					else {
						if (currentFrequency > secondHighestFrequency) {
							secondHighestFrequency = currentFrequency;
						}
					}
				}
				this.lastMajorityClass = HoeffdingTree.this.labels.get(majorityClass);
				this.examplesTillNextLabelUpdate = highestFrequency - secondHighestFrequency + 1;
			}
			return this.lastMajorityClass;
		}

		/**
		 * Returns the label of the most frequent class from the so far seen examples in the corresponding node where
		 * the specified feature had the specified value.
		 * @param feature a feature
		 * @param value a valid value of the feature
		 * @return the most frequent class label for a value of a feature
		 */
		public Serializable getMajorityClass(String feature, Serializable value) {
			int majorityClass = 0;
			int highestSum = 0;
			int featureIndex = this.remainingFeatures.indexOf(feature);
			int valueIndex = HoeffdingTree.this.featureValuePairs.get(feature).indexOf(value);
			for (int label = 0; label < HoeffdingTree.this.labels.size(); label++) {
				if (this.counter[featureIndex][valueIndex][label] > highestSum) {
					highestSum = this.counter[featureIndex][valueIndex][label];
					majorityClass = label;
				}
			}
			return HoeffdingTree.this.labels.get(majorityClass);
		}

		/**
		 * Returns the number of the so far seen examples
		 * @return the number of the so far seen examples
		 */
		public int getExampleCount() {
			return this.exampleCount;
		}

		/**
		 * Returns true iff all so far seen examples in the corresponding node had the same class
		 * @return true iff all examples had the same class
		 */
		public boolean isUniformLabel() {
			return this.isUniLabel;
		}

		/**
		 * Returns the value of the specified {@link QualityCriterion} for each of the remaining features
		 * @return the value of the specified {@link QualityCriterion} for each of the remaining features
		 */
		public Map<String, Double> getQualityAllAttributes() {
			Map<String, Double> featureQuality = new HashMap<String, Double>();
			double qualityNoSplit = this.getQualityNoSplit();
			for (String feature : this.getRemainingFeatures()) {
				featureQuality.put(feature, qualityNoSplit - this.getQualitySplitByFeature(feature));
			}
			return featureQuality;
		}

		/**
		 * Returns the quality for all examples without splitting
		 * @return the quality for all examples without splitting
		 */
		public double getQualityNoSplit() {
			int totalExamples = this.getExampleCount();
			// compute the probability for each class
			double[] totalLabel = this.initArray(new double[HoeffdingTree.this.labels.size()]);
			for (int value = 0; value < this.counter[this.fewestValuesFeature].length; value++) {
				for (int label = 0; label < totalLabel.length; label++) {
					totalLabel[label] += (double)this.counter[this.fewestValuesFeature][value][label];
				}
			}
			for (int i = 0; i < totalLabel.length; i++) {
				totalLabel[i] = (double)(totalLabel[i]) / totalExamples;
			}
			return HoeffdingTree.this.qualityCriterion.getQuality(totalLabel);
		}

		/**
		 * Returns the weighted averaged quality for splitting by the given feature
		 * @param feature a possible split feature
		 * @return the weighted averaged quality for splitting by the given feature
		 */
		public double getQualitySplitByFeature(String feature) {
			int totalExamples = this.getExampleCount();
			int featureIndex = this.remainingFeatures.indexOf(feature);
			// compute quality for each value of the feature and (weighted) sum them up
			double qualitySplitByFeature = 0d;
			for (int value = 0; value < this.counter[featureIndex].length; value++) {
				// compute total number of examples having this value for the feature
				int valueExamples = 0;
				for (int label = 0; label < this.counter[featureIndex][value].length; label++) {
					valueExamples += this.counter[featureIndex][value][label];
				}
				if (valueExamples > 0) {
					// compute the probability for each class
					double[] valueLabel = this.initArray(new double[this.counter[featureIndex][value].length]);
					for (int label = 0; label < valueLabel.length; label++) {
						valueLabel[label] = ((double)this.counter[featureIndex][value][label]) / valueExamples;
					}
					// add the weighted quality for this value
					qualitySplitByFeature += ((double)valueExamples / totalExamples) * HoeffdingTree.this.qualityCriterion.getQuality(valueLabel);
				}
			}
			return qualitySplitByFeature;
		}

		/**
		 * initializes the specified array by writing explicitly '0d' to each position.
		 * @param array the array to initialize
		 * @return the same (but initialized) Object as specified by the parameter
		 */
		public double[] initArray(double[] array) {
			for (int i = 0; i < array.length; i++) {
				array[i] = 0d;
			}
			return array;
		}

		/**
		 * Constructs a String representing the subtree beginning at the corresponding node
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("NodeData [counter=");
			for (int i = 0; i < this.counter.length; i++) {
				for (int j = 0; j < this.counter[i].length; j++) {
					for (int k = 0; k < this.counter[i][j].length; k++) {
						builder.append("\n["+i+"]["+j+"]["+k+"] = "+this.counter[i][j][k]);
					}
				}
			}

			builder.append("\n remainingFeatures=");
			builder.append(this.remainingFeatures);
			builder.append("]");
			return builder.toString();
		}
	}
}