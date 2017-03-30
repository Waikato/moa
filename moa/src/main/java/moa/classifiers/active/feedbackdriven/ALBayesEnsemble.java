package moa.classifiers.active.feedbackdriven;


import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.bayes.NaiveBayes;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;


/*
 * Each classifier in this ensemble is a multinomial naive bayes classifier.
 * There is a classifier for every dimension of the input space.
 * It is a naive bayes classifier with a few extensions for active learning
 */
public class ALBayesEnsemble extends AbstractClassifier {

	// naive bayes used for prediction
	public NaiveBayes naive;
	///
	
	private static final long serialVersionUID = 1L;
	
	// each classifier represents one input dimension
	public AutoExpandVector<ALBayesMultinomialClassifier> 	classifiers;
	public DoubleVector										overallClassDistribution;
	
	// normalization for the likelihood, see method; optional
	public DoubleVector	minLikelihoods;
	public DoubleVector	maxLikelihoods;
	
	private int counter;
	
	/* 
	 * For every class: return the highest disagreement between intern classifiers
	 */
	public double[] getClassDisagreementScores(Instance inst) {
		
		// each row represents one classifier; each column represents one class
		double[][] votesMat = new double[inst.numAttributes()-1][overallClassDistribution.numValues()];
		
		// calculate the votes for every classifier/class
		for(int attribute=0; attribute<inst.numAttributes()-1; ++attribute) {
			// get the attribute index for this instance, equivalent to the classifier index
			int 	attributeIndex = modelAttIndexToInstanceAttIndex(attribute, inst);
			double 	attributeValue = inst.value(attributeIndex);
			
			ALBayesMultinomialClassifier classifier = classifiers.get(attributeIndex);
			
			// if the classifier hasn't seen this attribute before, skip
			if((classifier != null) && !inst.isMissing(attributeIndex)) {
				votesMat[attribute] = classifier.getVotesForInstance(attributeValue);
			}
		}
		
		return CalculateDisagreementScoresHelper(votesMat);
	}
	
	/*
	 * Each row represents the votes from a classifier
	 * Each colum represents the votes for one class
	 * The result is a vector that contains the maximum disagreement for every class
	 */
	public double[] CalculateDisagreementScoresHelper(double[][] votesMatrix) {
		double[] scores = new double[votesMatrix[0].length];
		
		// for every class: compare the votes between every two classifiers
		// and save the maximal disagreement for every class
		for(int c=0; c<votesMatrix[0].length; ++c) {
			double maxDisagreement = 0.0;
			
			for(int cl1=0; cl1<votesMatrix.length; ++cl1) {
				for(int cl2=cl1+1; cl2<votesMatrix.length; ++cl2) {
					double disagreement = votesMatrix[cl1][c] - votesMatrix[cl2][c];
					disagreement = Math.sqrt(disagreement * disagreement);
					
					if(disagreement > maxDisagreement) maxDisagreement = disagreement;
				}
			}
			
			scores[c] = maxDisagreement;
		}
		
		return scores;
	}
	
	/*
	 * Return a likelihood score for every class
	 */
	public double[] getLikelihoodForEveryClass(Instance inst) {
		
		// Initialize the scores to 1.0 because the probabilities will get multiplied 
		double[] likelihoodScores = new double[overallClassDistribution.numValues()];
		for(int i=0; i<overallClassDistribution.numValues(); ++i) likelihoodScores[i] = 1.0;
				
		// iterate over all classifiers and multiply their probabilities with the current likelihood score
		for(int attribute=0; attribute<inst.numAttributes()-1; ++attribute) {
			// get the attribute index for this instance, equivalent to the classifier index
			int 	attributeIndex = modelAttIndexToInstanceAttIndex(attribute, inst);
			double 	attributeValue = inst.value(attributeIndex);
			
			ALBayesMultinomialClassifier classifier = classifiers.get(attributeIndex);
			
			// if the classifier hasn't seen this attribute before, skip
			if((classifier != null) && !inst.isMissing(attributeIndex)) {
				double[] likelihoodsForAttribute = classifier.getLikelihoodForEveryClass(attributeValue);
				
				for(int i=0; i<overallClassDistribution.numValues(); ++i) {
					likelihoodScores[i] *= likelihoodsForAttribute[i];
				}
			}
		}
	
		
		// optional: added this to make sure that the likelihoods are between 0.0 and 1.0
		// the likelihoods can become bigger than 1.0 because the normal distributions can have
		// a standarddeviation less than 1.0, points near the mean have a probability bigger
		// than 1.0
		//updateMinMaxLikelihoods(likelihoodScores);	
	
		//return normalizeLikelihoods(likelihoodScores);
		// ----------------------
		
		// this is the version from the paper
		return likelihoodScores;
	}
	
	/*
	 * update the min/max likelihoods for every class
	 */
	public void updateMinMaxLikelihoods(double[] likelihoodScores) {
		for(int i=0; i< likelihoodScores.length; ++i) {
			if(likelihoodScores[i] > maxLikelihoods.getValue(i)) {
				maxLikelihoods.setValue(i, likelihoodScores[i]);
			}
		
			// make sure the initialiized values are not zero
			if(i >= minLikelihoods.numValues()) {
				minLikelihoods.setValue(i, Double.MAX_VALUE);
			}
			
			if(likelihoodScores[i] < minLikelihoods.getValue(i)) {
				minLikelihoods.setValue(i, likelihoodScores[i]);
			}
		}
	}
	
	/*
	 * normalize the likelihoods to the range [0.0, 1.0]
	 */
	public double[] normalizeLikelihoods(double[] likelihoods) {
		double[] results = new double[likelihoods.length];
		
		for(int i=0; i<likelihoods.length; ++i) {
			results[i] = (likelihoods[i] - minLikelihoods.getValue(i)) / 
						 (maxLikelihoods.getValue(i) - minLikelihoods.getValue(i));
		}
		
		return results;
	}
	
	@Override
	public double[] getVotesForInstance(Instance inst) {
//		// naive bayes
//		double[] votes = new double[overallClassDistribution.numValues()];
//		
//		// p(y = c)
//		double ysum = overallClassDistribution.sumOfValues();
//		for(int i=0; i<overallClassDistribution.numValues(); ++i) {
//			votes[i] = overallClassDistribution.getValue(i) / ysum;	
//		}
//		
//		
//		for(int attribute=0; attribute<inst.numAttributes()-1; ++attribute) {
//			// get the attribute index for this instance, equivalent to the classifier index
//			int attributeIndex = modelAttIndexToInstanceAttIndex(attribute, inst);
//			double value = inst.value(attributeIndex);
//			
//			ALBayesMultinomialClassifier classifier = classifiers.get(attributeIndex);
//			
//			// if the classifier hasn't seen this attribute before, skip
//			if((classifier != null) && !inst.isMissing(attributeIndex)) {
//				double[] pforxgiveny = classifier.getLikelihoodForEveryClass(value);
//				
//				for(int i=0; i<pforxgiveny.length; ++i) {
//					votes[i] *= pforxgiveny[i];
//				}
//			}
//		}
//		
//		return votes;
		
		return naive.getVotesForInstance(inst);
	}
	
	
	@Override
	public boolean isRandomizable() {
		return false;
	}


	@Override
	public void resetLearningImpl() {
		naive = new NaiveBayes();
		naive.resetLearning();
		
		classifiers = new AutoExpandVector<ALBayesMultinomialClassifier>();	
		overallClassDistribution = new DoubleVector();
		
		minLikelihoods = new DoubleVector();
		maxLikelihoods = new DoubleVector();
		
		counter = 0;
	}


	@Override
	public void trainOnInstanceImpl(Instance inst) {
		naive.trainOnInstance(inst);
		
		// if this is the first round, make sure the classDistribution has an entry for every class
		if(overallClassDistribution.numValues() == 0) {
			double[] zeros = new double[inst.numClasses()];
			overallClassDistribution.addValues(zeros);
		}
		
		overallClassDistribution.addToValue((int)inst.classValue(), inst.weight());
		
		
		// iterate over the classifiers, everyone gets one instance at a time
		int classifierIndex = counter;
		counter = (counter + 1) % (inst.numAttributes()-1);
		
		int attributeIndex = modelAttIndexToInstanceAttIndex(classifierIndex, inst);
		
		ALBayesMultinomialClassifier classifier = classifiers.get(attributeIndex);
		if(classifier == null) {
			int numClasses = inst.numClasses();
			classifier = new ALBayesMultinomialClassifier();
			classifier.resetLearningImpl(numClasses);
			classifiers.set(attributeIndex, classifier);
		}
		
		classifier.trainOnInstanceImpl(inst.value(attributeIndex), (int)inst.classValue(), inst.weight());	
	}


	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}


	@Override
	public void getModelDescription(StringBuilder out, int indent) {}
}