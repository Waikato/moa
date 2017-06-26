package moa.classifiers.active;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.active.budget.BudgetManager;
import moa.core.Measurement;
import moa.options.ClassOption;
import moa.core.Utils;

/**
 * Active learning with uncertainty sampling by measuring the sufficient weight as described in:
 * Mohamed-Rafik Bouguelia, Yolande Belad, Abdel Belad:
 * An adaptive streaming active learning strategy based on instance weighting.
 * Pattern Recognition Letters, 70:38-44, 2016
 */
public class ALSufficientWeight extends AbstractClassifier implements ALClassifier {
	
	private static final long serialVersionUID = 1L;
	private Classifier classifier;
	private double threshold;
	private int numInstancesClassified = 0;
	private int numLabelsAcq = 0;
	private int totalNumLabelsAcq = 0;
	private List<Instance> batch = new ArrayList<Instance>();
	private BudgetManager budgetManager = null;
	
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
    		"Classifier to train.", Classifier.class, "drift.SingleClassifierDrift");
    
    public IntOption batchSizeOption = new IntOption("batchSize", 's',
    		"The batch size. If batch size is greater than one, the classifier will select " +
    		"the most beneficial instance from each batch and the threshold will not be used.", 1, 1, 1000);
    
    public FloatOption sufficientWeightEpsilonOption = new FloatOption("sufficentWeightEpsilon", 'e',
    		"The tolerance for the sufficient weight approximation.", 0.0001, 0.0, 1.0);
    
    public FloatOption initialThresholdOption = new FloatOption("initialThreshold", 't',
    		"Initial threshold. The label of an instance is queried if the sufficient weight is below this threshold.", 1.0, 0.0, 1.0);
    
    public FloatOption thresholdLearningRateOption = new FloatOption("thresholdLearningRate", 'r',
    		"The learning rate to be used to adapt the threshold.", 0.01, 0.0, 1.0);
    
    public FlagOption decayLearningRateOption = new FlagOption("learningRateDecay", 'd',
    		"If enabled, the initial threshold learning rate will be divided by the total number of queried labels.");
    
    public FlagOption useBudgetManagerOption = new FlagOption("useBudgetManager", 'b',
    		"If enabled, the internal threshold will be replaced by the specified budget manager.");
    
    public ClassOption budgetManagerOption = new ClassOption("budgetManager", 'm',
    		"The budget manager to use.", BudgetManager.class, "ThresholdBM");
    
    public IntOption numInstancesInitOption = new IntOption("numInstancesInit", 'n',
    		"Number of instances at beginning without active learning.", 0, 0, Integer.MAX_VALUE);
    
    @Override
    public String getPurposeString() {
        return "Active learning classifier based on uncertainty sampling. " +
        	   "Uncertainty is measured as the minimum weight necessary to change a base classifiers prediction.";
    }
    
	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public int getLastLabelAcqReport() {
		if (useBudgetManagerOption.isSet()) {
			return budgetManager.getLastLabelAcqReport();
		} else {
			final int temp = numLabelsAcq;
			numLabelsAcq = 0;
			return temp;
		}
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		return classifier.getVotesForInstance(inst);
	}

	@Override
	public void resetLearningImpl() {
		classifier = ((Classifier) getPreparedClassOption(baseLearnerOption)).copy();
		threshold = initialThresholdOption.getValue();
		numInstancesClassified = 0;
		numLabelsAcq = 0;
		totalNumLabelsAcq = 0;
		batch.clear();
		if (useBudgetManagerOption.isSet()) {
			if (budgetManager == null) {
				budgetManager = (BudgetManager) getPreparedClassOption(budgetManagerOption);
			}
			budgetManager.resetLearning();
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		numInstancesClassified += 1;
		if (numInstancesClassified <= numInstancesInitOption.getValue()) {
			acquireLabel(inst);
		}
		else if (batchSizeOption.getValue() > 1) {
			// batch learning
			batch.add(inst);
			if (batch.size() >= batchSizeOption.getValue()) {
				acquireLabel(getMostUncertainInstance(batch));
				batch.clear();
			}
		} else { // one-by-one learning
			if (useBudgetManagerOption.isSet()) {
				final double w = getSufficientWeight(classifier, inst, sufficientWeightEpsilonOption.getValue());
				if (budgetManager.isAbove(1.0 - w)) {
					acquireLabel(inst);
				}
			} else {
				final double votes[] = classifier.getVotesForInstance(inst);
				if (votes.length < 2) {
					acquireLabel(inst);
				}
				else {
					final int y[] = getTwoMostProbableClasses(classifier.getVotesForInstance(inst));
					final Classifier modifiedClassifier = trainClassifierCopy(classifier, inst, y[1], threshold);
					if (Utils.maxIndex(modifiedClassifier.getVotesForInstance(inst)) != y[0]) {
						updateThreshold(inst, y[0]);
						acquireLabel(inst);
					}
				}
			}
		}
	}
	
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
        List<Measurement> measurementList = new LinkedList<Measurement>();
        measurementList.add(new Measurement("threshold", threshold));
        measurementList.add(new Measurement("thresholdLearningRate", getThresholdLearningRate()));
        Measurement[] modelMeasurements = ((AbstractClassifier) this.classifier).getModelMeasurements();
        if (modelMeasurements != null) {
            for (Measurement measurement : modelMeasurements) {
                measurementList.add(measurement);
            }
        }
        return measurementList.toArray(new Measurement[measurementList.size()]);
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		((AbstractClassifier) this.classifier).getModelDescription(out, indent);
	}
	
	private void acquireLabel(Instance inst) {
		classifier.trainOnInstance(inst);
		numLabelsAcq += 1;
		totalNumLabelsAcq += 1;
	}
	
	/**
	 * Get the most and second most probable classes.
	 * @param votes Votes from the classifier.
	 * @return Array with first and second most probable class.
	 */
	private static int[] getTwoMostProbableClasses(double votes[]) {
		assert(votes.length >= 2);
		final int classes[] = Utils.sort(votes);
		int y[] = new int[2];
		y[0] = classes[classes.length - 1];
		y[1] = classes[classes.length - 2];
		return y;
	}
	
	/**
	 * @param originalInst instance to train the classifier with
	 * @param originalClassifier is left unchanged
	 * @param label new label to assign to the instance
	 * @param weight weight to be used for training
	 * @return Copy of the classifier, trained on the given instance labeled as {@code label} and weighted with {@code weight}.
	 */
	private static Classifier trainClassifierCopy(Classifier originalClassifier, Instance originalnst, int label, double weight) {
		Classifier classifier = originalClassifier.copy();
		Instance inst = originalnst.copy();
		inst.setClassValue(label);
		inst.setWeight(weight);
		classifier.trainOnInstance(inst);
		return classifier;
	}
	
	/**
	 * Approximate the minimum weight needed to change the prediction of the classifier by training it with {@code inst}.
	 * @param originalClassifier is left unchanged
	 * @param epsilon tolerance interval in [0, 1]
	 * @return approximate sufficient weight
	 */
	private static double getSufficientWeight(Classifier originalClassifier, Instance inst, double epsilon) {
		assert(0.0 >= epsilon && epsilon <= 1.0);
		
		final double votes[] = originalClassifier.getVotesForInstance(inst);
		if (votes.length < 2) return 0;
		final int y[] = getTwoMostProbableClasses(votes);
		
		double low = 0, up = 1;
		double weight = 0;
		do {
			weight = 0.5 * (low + up);
			final Classifier c = trainClassifierCopy(originalClassifier, inst, y[1], weight);
			if (Utils.maxIndex(c.getVotesForInstance(inst)) == y[0]) {
				low = weight;
			} else {
				up = weight;
			}
		} while (up - low > epsilon);
		
		return weight;
	}
	
	/**
	 * Get the instance with the lowest sufficient weight from the batch.
	 */
	private Instance getMostUncertainInstance(List<Instance> batch) {
		double minWeight = Double.POSITIVE_INFINITY;
		Instance mostUncertainInst = null;
		
		for (Instance inst : batch) {
			final double weight = getSufficientWeight(classifier, inst, sufficientWeightEpsilonOption.getValue());
			if (weight < minWeight) {
				minWeight = weight;
				mostUncertainInst = inst;
			}
		}
		
		return mostUncertainInst;
	}
	
	/**
	 * @param prediction predicted class label for the instance
	 */
	private void updateThreshold(Instance inst, int prediction) {
		double thresholdLearningRate = getThresholdLearningRate();
		if (thresholdLearningRate != 0.0) {
			final double w = getSufficientWeight(classifier, inst, sufficientWeightEpsilonOption.getValue());
			if (inst.classValue() == prediction) {
				// prediction was correct
				threshold -= thresholdLearningRate * (threshold - w);
			} else {
				// prediction was incorrect
				threshold += thresholdLearningRate * w * (1.0 - threshold) / threshold;
			}
		}
	}
	
	private double getThresholdLearningRate() {
		if (decayLearningRateOption.isSet()) {
			return thresholdLearningRateOption.getValue() / totalNumLabelsAcq;
		} else {
			return thresholdLearningRateOption.getValue();
		}
	}

	
	@Override
	public void setModelContext(InstancesHeader ih) {
		super.setModelContext(ih);
		classifier.setModelContext(ih);
	}
}