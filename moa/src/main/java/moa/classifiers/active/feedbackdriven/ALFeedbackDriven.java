package moa.classifiers.active.feedbackdriven;


import java.util.LinkedList;
import java.util.List;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.active.ALClassifier;
import moa.classifiers.active.budget.BudgetManager;
import moa.classifiers.active.budget.ThresholdBM;
import moa.core.DoubleVector;
import moa.core.GaussianEstimator;
import moa.core.Measurement;
import moa.options.ClassOption;

/*
 * ABSTRACT
Active learning is a promising way to eﬃciently build up
training sets with minimal supervision. Most existing meth-
ods consider the learning problem in a pool-based setting.
However, in a lot of real-world learning tasks, such as crowd-
sourcing, the unlabeled samples, arrive sequentially in the
form of continuous rapid streams. Thus, preparing a pool
of unlabeled data for active learning is impractical. More-
over, performing exhaustive search in a data pool is expen-
sive, and therefore unsuitable for supporting on-the-ﬂy in-
teractive learning in large scale data. In this paper, we
pres ent a systematic framework for stream-based multi-class
active learning. Following the reinforcement learning frame-
work, we propose a feedback-driven active learning approach
by adaptively combining diﬀerent criteria in a time-varying
manner. Our method is able to balance exploration and ex-
ploitation during the learning process. Extensive evaluation
on various benchmark and real-world datasets demonstrates
the superiority of our framework over existing methods.
 */
public class ALFeedbackDriven extends AbstractClassifier implements ALClassifier {

	private static final long serialVersionUID = 1L;
	
	
	// ===== Parameters for the algorithm
	private double lemma;		// learning rate
	private double epsilon; 	// limits the beta to [epsilon; 1-epsilon] 
	private double beta;		// balances exploitation/exploration
	
    private ALBayesEnsemble	ensemble;
    private ALBayesEnsemble updatedEnsemble;	// used to calculate the KL Divergence
    private BudgetManager 	budgetManager;

    
    private int	numberOfTrainedInstances;
    private int numberOfOverallInstances;
    private int numberOfInstancesForInitialization;
    private int	numberOfInstancesToPullForDivergence;
    
    // evaluation
    private int gotLabeled;
    private int tp;
    private int fp;
    private int fn;
    private GaussianEstimator rewardDistribution;
    private GaussianEstimator explorationDistribution;
    private GaussianEstimator exploitationDistribution;
    private LinkedList<Double> lastXExploitationScores;
    private LinkedList<Double> lastXExplorationScores;
    
    private double minKLDivergence;
    private double maxKLDivergence;
    
    
	// ===== MOA Parameters
    public ClassOption budgetManagerOption = new ClassOption("budgetManager",
            'b', "BudgetManager that should be used.",
            BudgetManager.class, "ThresholdBM");
    
    public IntOption numberOfInstancesForInitializationOption = new IntOption("numberOfInstancesForInitialization",
    		'n', "The number of Instances used to initialize the Classifier.", 200, 5, 1000);
    
    public FloatOption lemmaOption = new FloatOption("LearningRate",
            'k', "Fixed learning rate. Influences how fast the beta is changing.",
            0.7, 0.1, 0.9);
    
    public FloatOption epsilonOption = new FloatOption("Epsilon",
            'w', "restricts the range of the beta.",
            0.15, 0.00, 1.00);
    
    public FloatOption betaOption = new FloatOption("Beta",
            'l', "Balance exploitation/exploration",
            0.7, 0.00, 1.00); 
    
    public IntOption numberOfInstancesToPullForDivergenceOption = new IntOption("InstancesToPullForDivergence",
    		'q', "The number of Instances that get sampled by the distributions to calculate the KL Divergence", 100, 10, 500);
    
    // TODO - remove this, only for testing
    DoubleVector test;
	
    
	// ===== trainOnInstance
	public void trainOnInstanceImpl(Instance inst) {	
		++numberOfOverallInstances;
		
		
		// evaluation ----------
		double exploitationScore = CalculateExploitationScore(ensemble.getClassDisagreementScores(inst));
		double explorationScore = CalculateExplorationScore(ensemble.getLikelihoodForEveryClass(inst));
		
		exploitationDistribution.addObservation(exploitationScore, 1.0);
		explorationDistribution.addObservation(explorationScore, 1.0);
		
		lastXExploitationScores.addLast(exploitationScore);
		lastXExplorationScores.addLast(explorationScore);
		if(lastXExploitationScores.size() > 10) {
			lastXExploitationScores.pop();
			lastXExplorationScores.pop();
		}
		///////////////////////
		
		// train a few instances for startup
		if(numberOfTrainedInstances < numberOfInstancesForInitialization) {
			this.ensemble.trainOnInstanceImpl(inst);
			this.updatedEnsemble.trainOnInstanceImpl(inst);
			++numberOfTrainedInstances;
			gotLabeled = 1;
			
			return;
		}
		
		
		double Q = CalculateOverallScore(inst);
		if(this.budgetManager.isAbove(Q)) {
			// Request label - already labeled
			
			gotLabeled = 1;
			this.updatedEnsemble.trainOnInstanceImpl(inst);
			
			// Update Query criterion
			double symmetricDivergence = CalculateSymmetricKLDivergence(updatedEnsemble, ensemble);
			if(symmetricDivergence < minKLDivergence) minKLDivergence = symmetricDivergence;
			if(symmetricDivergence > maxKLDivergence) maxKLDivergence = symmetricDivergence;
			
			double reward = CalculateRewardFunction(symmetricDivergence, minKLDivergence, maxKLDivergence);
			beta = UpdateBeta(reward, beta, epsilon, lemma);
			
			// TODO - remove testing
			rewardDistribution.addObservation(reward, 1.0);
			
			// Update classifier
			this.ensemble.trainOnInstanceImpl(inst);
			++numberOfTrainedInstances;
		} else {
			gotLabeled = 0;
		}
	}
	
	
	// ===== Calculate the scores
	
	/*
	 * equation (5)
	 */
	public double CalculateExploitationScore(double[] disagreementScores) {
		// find top two scores
		double maxScore				= 0.0;
		double secondHighestScore 	= 0.0;
		
		for(int i=0; i<disagreementScores.length; ++i) {
			if(disagreementScores[i] > maxScore) {
				secondHighestScore = maxScore;
				maxScore = disagreementScores[i];
			} else if(disagreementScores[i] > secondHighestScore) {
				secondHighestScore = disagreementScores[i];
			}
		}
		
		if(maxScore == 0.0) return 1.0;		// not enough data, return 1.0
		else return (1.0 / 2.0) * (maxScore + secondHighestScore);  // return 1/2 * (s1 + s2);
	}
	
	/*
	 * equation (6)
	 */
	public double CalculateExplorationScore(double[] likelihoodScores) {
		// changed version
		// find the maximum likelihood und return the respective exploration score
		double maxLikelihood = Double.MIN_VALUE;
		for(int i=0; i<likelihoodScores.length; ++i) {
			if(likelihoodScores[i] > maxLikelihood) maxLikelihood = likelihoodScores[i];
		}
		
		
		return 1.0 - maxLikelihood;
		
		// 1.0 - max
		// im report nochmal ausschreiben, was anders gemacht wurde
		// formeln 6 und 7 modifiziert, korrigierte fassung ins paper aufnehmen
		// vergleich mit original in performance
		
		
		
//		// this is the score calculated by the paper
//		double maxLikelihood = Double.MIN_VALUE;
//		for(int i=0; i<likelihoodScores.length; ++i) {
//			if(likelihoodScores[i] > maxLikelihood) maxLikelihood = likelihoodScores[i];
//		}
//	
//		return maxLikelihood;
		
		
//		// using the minimum
//		double minLikelihood = Double.MAX_VALUE;
//		for(int i=0; i<likelihoodScores.length; ++i) {
//			if(likelihoodScores[i] < minLikelihood) minLikelihood = likelihoodScores[i];
//		}
//	
//		if(minLikelihood == Double.MAX_VALUE) return 0.0;
//		else return 1.0 - minLikelihood;
	}
	
	/*
	 * Equation (7)
	 */
	public double CalculateOverallScore(Instance inst) {
		double exploitationScore = CalculateExploitationScore(ensemble.getClassDisagreementScores(inst));
		double explorationScore = CalculateExplorationScore(ensemble.getLikelihoodForEveryClass(inst));
		
		
		return beta*exploitationScore + (1.0 - beta)*explorationScore;
	}
	
	

	/*
	 * This is equation (8)
	 */
	public double UpdateBeta(double reward, double beta, double epsilon, double lemma) {
		double minimum = Math.min(beta * lemma * Math.exp(reward), 1-epsilon);
		double newBeta = Math.max(minimum, epsilon);
		
		return newBeta;
	}
	
	/*
	 * equation (10)
	 */
	public double CalculateSymmetricKLDivergence(ALBayesEnsemble e1, ALBayesEnsemble e2) {
		double KL1 = CalculateKLDivergence(e1, e2);
		double KL2 = CalculateKLDivergence(e2, e1);
		
		return (KL1 + KL2) / 2.0;
	}
	
	/*
	 * equation (9)
	 */
	public double CalculateKLDivergence(ALBayesEnsemble e1, ALBayesEnsemble e2) {
		// both ensembles have to have the same number of classifiers
		assert(e1.classifiers.size() == e2.classifiers.size());
		double summedDivergence = 0.0;
		
		// the overall divergence is the sum of the divergences between the ensemble members
		for(int i=0; i < e1.classifiers.size(); ++i) {
			ALBayesMultinomialClassifier clas1 = e1.classifiers.get(i);
			ALBayesMultinomialClassifier clas2 = e2.classifiers.get(i);
			
			// this can happen when the ensemble got trained with only a few instances
			if(clas1 == null || clas2 == null) continue;
			
			// pull a few instances and use them to calculate the divergence
			double[] instances = clas1.pullInstancesFromDistribution(numberOfInstancesToPullForDivergence);
			
			
			double divergenceForDimension = 0.0;
			
			// calculate the divergence 
			for(int j=0; j<instances.length; ++j) {
				double value = instances[j];
				
				double px = clas1.getProbabilityForInstance(value);
				double qx = clas2.getProbabilityForInstance(value);
				
				if(qx != 0.0) 	divergenceForDimension += px * Math.log(px / qx);
			}
			
			summedDivergence += divergenceForDimension;
		}
		
		return summedDivergence;
	}
	
	/*
	 * equation (11)
	 */
	public double CalculateRewardFunction(double st, double minKLDiv, double maxKLDiv) {
		if(minKLDiv == maxKLDiv) return 0.5;
		
		return (st - minKLDiv) / (maxKLDiv - minKLDiv);
	}


	// ===== AbstractClassifierImpl
	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public int getLastLabelAcqReport() {
		return this.budgetManager.getLastLabelAcqReport();
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		double[] res = this.ensemble.getVotesForInstance(inst);
		
		
		// see which class got predicted
		int maxIndex = 0;
		for (int i = 0; i < res.length; i++){
		   double newnumber = res[i];
		   if ((newnumber > res[maxIndex])){
		   maxIndex = i;
		  }
		} 
		
		// count true positives, false positives, false negatives
		int realClass = (int)(inst.classValue());
		if		(maxIndex == realClass) ++tp;
		else if (maxIndex == 1 && realClass == 0) ++fp;
		else if (maxIndex == 0 && realClass == 1) ++fn;
		

//		float percentTrainedInstances = (float)(numberOfTrainedInstances - numberOfInstancesForInitialization) / (numberOfOverallInstances - numberOfInstancesForInitialization);
//		
//		if(numberOfOverallInstances > numberOfInstancesForInitialization) {
//			System.out.format("percent of trained Instances: %.3f | exploitation(mean: %.3f; stdDev: %.3f) | exploration(mean: %.3f; stdDev: %.3f) | beta: %.3f", 
//							  percentTrainedInstances, 
//							  exploitationDistribution.getMean(), 
//							  exploitationDistribution.getStdDev(), 
//							  explorationDistribution.getMean(), 
//							  explorationDistribution.getStdDev(),
//							  beta);
//			System.out.println("");
//		}
		
//		// test for last exploration/exploitation scores
//		double avgExploitation = 0.0;
//		double avgExploration = 0.0;
//		for(double iter : lastXExploitationScores) {
//			avgExploitation += iter;
//		}
//		for(double iter : lastXExplorationScores) {
//			avgExploration += iter;
//		}
//		avgExploitation /= lastXExploitationScores.size();
//		avgExploration /= lastXExplorationScores.size();
//		
//		if(numberOfOverallInstances > numberOfInstancesForInitialization) {
//			System.out.format("percent of trained Instances: %.3f | avgExploitationOverLastTenInstances %.3f | avgExplorationOverLastTenInstances %.3f | beta: %.3f", 
//							  percentTrainedInstances, 
//							  avgExploitation,
//							  avgExploration,
//							  beta);
//			System.out.println("");
//		}
		
//		
//		if(numberOfOverallInstances > numberOfInstancesForInitialization) {
//		System.out.format("percent of trained Instances: %.3f | rewardMean: %.3f | rewardStd: %.3f | beta: %.3f", 
//						  percentTrainedInstances, 
//						  rewardDistribution.getMean(),
//						  rewardDistribution.getStdDev(),
//						  beta);
//		System.out.println("");
//	}
		
		return res;
	}

	@Override
	public void resetLearningImpl() {
        this.budgetManager = ((BudgetManager) getPreparedClassOption(this.budgetManagerOption));
        this.budgetManager.resetLearning();	

        numberOfInstancesForInitialization = numberOfInstancesForInitializationOption.getValue();
        numberOfInstancesToPullForDivergence = numberOfInstancesToPullForDivergenceOption.getValue();
        
        lemma = lemmaOption.getValue();
        epsilon = epsilonOption.getValue();
        beta = betaOption.getValue();
   
        resetNonOptionAttributes();
	}
	
	/*
	 * Gets called by the unittest class instead of resetLearningImpl
	 */
	public void resetForTesting() {
		budgetManager = new ThresholdBM();
		budgetManager.resetLearning();
		
		numberOfInstancesForInitialization = 50;
		numberOfInstancesToPullForDivergence = 50;
		
        lemma = 0.5;
        epsilon = 0.1;
        beta = 0.5;
        
        resetNonOptionAttributes();
	}
	
	/*
	 * Reset all attributes that do not depend on options
	 */
	public void resetNonOptionAttributes() {
		ensemble = new ALBayesEnsemble();
		ensemble.resetLearning();
		updatedEnsemble = new ALBayesEnsemble();
		updatedEnsemble.resetLearning();
        
        numberOfTrainedInstances = 0;
        numberOfOverallInstances = 0;
        
        minKLDivergence = Double.MAX_VALUE;
        maxKLDivergence = -Double.MAX_VALUE;
        
        gotLabeled = 0;
        tp = 0;
        fp = 0;
        fn = 0;
        rewardDistribution = new GaussianEstimator();
        exploitationDistribution = new GaussianEstimator();
        explorationDistribution = new GaussianEstimator();
        
        test = new DoubleVector();		
        lastXExploitationScores = new LinkedList<Double>();
        lastXExplorationScores = new LinkedList<Double>();
        lastXExploitationScores.add(0.0);
        lastXExplorationScores.add(0.0);
	}
	
	

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
        List<Measurement> measurementList = new LinkedList<Measurement>();

        double avgExploitation = 0;
        double avgExploration = 0;
        if(lastXExploitationScores.size() > 0) {
	        for(int i=0; i<lastXExploitationScores.size(); ++i) {
	        	avgExploitation += lastXExploitationScores.get(i);
	        	avgExploration  += lastXExplorationScores.get(i);
	        }
	        avgExploitation /= lastXExploitationScores.size();
	        avgExploration  /= lastXExplorationScores.size();  
        }
        
        double precision = (double)tp / (tp +fp);
        double recall = (double)tp / (tp + fn);
        
        measurementList.add(new Measurement("ExploitationScore", avgExploitation));
        measurementList.add(new Measurement("ExplorationScore", avgExploration));
        measurementList.add(new Measurement("PercentTrainedInstances", (double)(numberOfTrainedInstances) / numberOfOverallInstances));
        measurementList.add(new Measurement("GotLabeled", gotLabeled));
        measurementList.add(new Measurement("Precision", precision));
        measurementList.add(new Measurement("Recall", recall));
        measurementList.add(new Measurement("Beta", beta));
        
        return measurementList.toArray(new Measurement[measurementList.size()]);
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		((AbstractClassifier) this.ensemble).getModelDescription(out, indent);
	}
	
	
	public static double gaussianValue(double mean, double sigma, double x) {
		final double normconst = Math.sqrt(2.0 * Math.PI);
		double norm = 1/(sigma * normconst);
		double diff = x - mean;
		
		return norm * Math.exp(-(diff * diff / (2.0 * sigma * sigma)));
	}
} 