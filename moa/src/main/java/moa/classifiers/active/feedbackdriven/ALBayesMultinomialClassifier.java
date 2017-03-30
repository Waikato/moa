package moa.classifiers.active.feedbackdriven;

import java.util.Random;

import moa.core.DoubleVector;
import moa.core.GaussianEstimator;


/*
 * A multinomial bayes classifier that observes one dimension of the input space
 */
public class ALBayesMultinomialClassifier {
	private GaussianClassObserver 	attributeClassObserver;	
	private DoubleVector			classDistribution;
	private double					sumOfDistribution;
	
	// For pulling instances from normal distribution
	private Random rand;
	
	
	/*
	 * for every class c: return p(x=instancex | y=c)
	 */
	public double[] getLikelihoodForEveryClass(double instancex) {
		double[] likelihoods = new double[classDistribution.numValues()];
		
		for(int c=0; c<classDistribution.numValues(); ++c) {
			likelihoods[c] = attributeClassObserver.probabilityOfAttributeValueGivenClass(instancex, c);
		}
		
		return likelihoods;
	}
	
	/*
	 * calculates p(x) = p(x|y=1) + p(x|y=2) + ... + p(x|y=n)
	 */
	public double getProbabilityForInstance(double instancex) {
		if(sumOfDistribution == 0) return 0.0;
		
		double px = 0.0;
		
		double[] likelihoods = getLikelihoodForEveryClass(instancex);
		
		for(int i=0; i<likelihoods.length; ++i) {
			double py = classDistribution.getValue(i) / sumOfDistribution;
			px += likelihoods[i] * py;
		}
		
		return px;
	}
	

	public double[] getVotesForInstance(double instancex) {
		double[] votes = new double[classDistribution.numValues()];
		
		// trained with no instances -> return zeros
		if(sumOfDistribution == 0) return votes;
		
		
		for(int c=0; c<classDistribution.numValues(); ++c) {
			
			// p(y=c)
			double py = classDistribution.getValue(c) / sumOfDistribution; 
			
			// p(x=instancex | y=c)
			double pxgiveny = attributeClassObserver.probabilityOfAttributeValueGivenClass(instancex, c);
			
			// ~p(y=c | x=instancex)
			votes[c] = py * pxgiveny;
		}
		
		return votes;
	}

	/*
	 * Return randomly sampled instances from the distribution
	 */
	public double[] pullInstancesFromDistribution(int numberOfInstances) {
		double[] instances = new double[numberOfInstances];
		
		for(int i=0; i<numberOfInstances; ++i) {
			// pick a random class from the class distribution
			int randomClass = GetRandomClassFromClassDistribution();
		
			GaussianEstimator attDist = attributeClassObserver.GetDistributionForClass(randomClass);
			instances[i] = rand.nextGaussian() * attDist.getStdDev() + attDist.getMean();
		}
		
		return instances;
	}
	
	/*
	 * Each class has the probability corresponding to the class distributions
	 */
	public int GetRandomClassFromClassDistribution() {
		if(sumOfDistribution == 0) return 0;
		
		double randNum = rand.nextDouble();
		double sum = 0.0;
		int randomClass = Integer.MAX_VALUE;

		for(int c=0; c<classDistribution.numValues(); ++c) {
			sum += classDistribution.getValue(c) / sumOfDistribution;
			
			if(randNum <= sum) {
				randomClass = c;
				break;
			}
		}
		
		return randomClass;
	}
	
	public void resetLearningImpl(int numClasses) {
		double[] zeros = new double[numClasses];
		classDistribution = new DoubleVector(zeros);
		attributeClassObserver = new GaussianClassObserver();
		rand = new Random();
		sumOfDistribution = 0.0;
	}

	public void trainOnInstanceImpl(double attVal, int classVal, double weight) {
		attributeClassObserver.observeAttributeClass(attVal, classVal, weight);
		classDistribution.addToValue(classVal, weight);
		sumOfDistribution += weight;
	}

	
	
}