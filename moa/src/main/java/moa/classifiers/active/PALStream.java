/*
 *    PALStream.java
 *    
 *    OPAL Code:
 *    Copyright (C) 2016 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Christian Beyer (christian.beyer@ovgu.de)
 *    Implementation of MCPAL based on OPAL Code:
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tuan Pham Minh (tuan.pham@ovgu.de)
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
package moa.classifiers.active;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Stack;

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



/**
 * PALStream is an active learner based on Multi-Class Probabilistic 
 * Active Learning (MCPAL) proposed in [1] and uses the budget manager 
 * shown in [2].
 * 
 * This implementation is based on Christian Beyer's implementation of 
 * OPAL and is modified to implement PALStream
 *
 *
 * [1]	Kottke, D., Krempl, G., Lang, D., Teschner, J. & Spiliopoulou, M. (2016). 
 * 		Multi-Class Probabilistic Active Learning. 
 * 		In G. A. Kaminka, M. Fox, P. Bouquet, E. Hüllermeier, 
 * 		V. Dignum, F. Dignum & F. van Harmelen (eds.), 
 * 		ECAI (p./pp. 586-594), : IOS Press. ISBN: 978-1-61499-672-9 
 * [2]	Kottke D., Krempl G., Spiliopoulou M. 
 * 		(2015) Probabilistic Active Learning in Datastreams. 
 * 		In: Fromont E., De Bie T., van Leeuwen M. (eds) 
 * 		Advances in Intelligent Data Analysis XIV. 
 * 		Lecture Notes in Computer Science, vol 9385. Springer, Cham
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class PALStream extends AbstractClassifier implements ALClassifier {

	private static final long serialVersionUID = 1L;

	private PALStreamEstimatorMultivariate labeledDataKernelEstimator; // used only for labeled data
	private StandardDeviationEstimator labeledDataStandartDeviationEstimator;
	
	private PALStreamEstimatorMultivariate allDataKernelEstimator; // used for labeled and unlabeled data
	private StandardDeviationEstimator allDataStandartDeviationEstimator;

	private int numClasses;
	private int numAttributes;
	
	private List<List<int[]>> distributions;
	
	private Classifier classifier;
	
	private int mMax;
	
	private boolean useDensityWeight;
	
	private double bandwidth;
	
	private BudgetManager budgetManager;
	
    public ClassOption classifierOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "drift.SingleClassifierDrift");
	
    public ClassOption budgetManagerOption = new ClassOption("budgetManager",
            'u', "BudgetManager that should be used.",
            BudgetManager.class, "BalancedIncrementalQuantileFilter");
    
    public IntOption mMaxOption = new IntOption("M",
            'm', "The maximum number of hypothetic label.", 3, 0, Integer.MAX_VALUE);
    
    public IntOption labeledDataKernelDensityEstimatorWindowOption = new IntOption("labeledDataKFEWindow",
            's', "The size of the window used for the kernel frequency estimation for the labeled data.", 200, 1, Integer.MAX_VALUE);
    
    public IntOption allDataKernelDensityEstimatorWindowOption = new IntOption("allDataKFEWindow",
            'a', "The size of the window used for the kernel frequency estimation for unlabeled and labeled data.", 200, 1, Integer.MAX_VALUE);

    public FloatOption bandwidthOption = new FloatOption("bandWidth",
            'w', "The bandwidth to use for density estimation.", 0.2, Double.MIN_VALUE, Double.MAX_VALUE);
    
    public FlagOption useDensityWeightOption = new FlagOption("useDensityWeighting",
            'd', "If set to true the gain will be weighted by the density for that instance.");
	
	
	//____________________________MODIFIED_CODE_FROM_OPAL__________________________________________________

	/*
	 * GAMMA function
	 * http://introcs.cs.princeton.edu/java/91float/Gamma.java.html
	 */
	private double logGamma(double x) {
		double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
		double ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1) + 24.01409822 / (x + 2) - 1.231739516 / (x + 3)
				+ 0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
		return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
	}

	private double gamma(double x) {
		return Math.exp(logGamma(x));
	}

	// End Gamma

	/**
	 * calculate the frequency estimates (k) for a given instance
	 * 
	 * @param inst the instance for which the frequency estimates should be calculated for
	 * @param posterior the posterior for the given instance
	 * @return frequency estimates for each class
	 */
	private double[] getK(double[] inst, double[] posterior) {
		double[] std = labeledDataStandartDeviationEstimator.getStd();
		double n = labeledDataKernelEstimator.getFrequencyEstimate(inst, std);

		double[] k = new double[numClasses];
		for(int cIdx = 0; cIdx < numClasses; ++cIdx)
		{
			k[cIdx] = posterior[cIdx] * n;
		}

		return k;
	}

	/**
	 * calculate the density for a given instance
	 *
	 * @param inst the instance the density should be calculated for
	 * @return density for the given frequency estimate
	 */
	private double getDensity(double[] inst) {
		double[] std = allDataStandartDeviationEstimator.getStd();
		double sumFrequencies = allDataKernelEstimator.getFrequencyEstimate(inst, std);
		int numInstances = allDataKernelEstimator.getNumPoints();
		return numInstances == 0 ? 0 : sumFrequencies / numInstances;
	}
	
	/**
	 * generate a list with all distribution possibilities of numInstances instances over numClasses classes
	 * @param numInstances the number of instances to distribute
	 * @param numClasses the number of classes the instances have to be distributed over
	 * @return list of all distributions
	 */
	private List<int[]> getAllDistributionPossibilities(int numInstances, int numClasses)
	{
		// create list to store all distributions 
		List<int[]> allDistributionPossibilities = new ArrayList<>();
		// create stacks so recursion is not needed
		Stack<int[]> stackDistribution = new Stack<>();
		Stack<Integer> stackLastIndex = new Stack<>();
		Stack<Integer> stackInstanceCount = new Stack<>();
		// start with a distribution which is empty
		int[] initialDistribution = new int[numClasses];
		for(int cIdx = 0; cIdx < numClasses; ++cIdx)
		{
			initialDistribution[cIdx] = 0;
		}
		// add the initial distribution to the stack which stores the distributions to process
		stackDistribution.push(initialDistribution);
		stackLastIndex.push(0);
		stackInstanceCount.push(0);
		// iterate while the stack is not empty
		while(!stackDistribution.empty())
		{
			// get data for the distribution to process from the stack
			int[] distribution = stackDistribution.pop();
			int lastIdx = stackLastIndex.pop();
			int instanceCount = stackInstanceCount.pop();
			// check the number of instances in the distribution
			if(instanceCount == numInstances)
			{
				// if there are enough instances the distribution is added to the list
				allDistributionPossibilities.add(distribution);
			}
			else
			{
				// if there are less new distributions will be added to the stack
				// new distributions are generated by adding a new instance
				// the new instance can only be added to the rightmost class with
				// instances or the classes on the right side of it
				// (if there are no instances all classes are valid)
				// this is done to avoid duplications
				for(int newLastIndex = lastIdx; newLastIndex < numClasses; ++newLastIndex)
				{
					// copy the distribution to not override it accidently
					int[] newDistribution = distribution.clone();
					// add a new instance
					newDistribution[newLastIndex] += 1;
					// add data of the new distribution to the stack
					stackDistribution.add(newDistribution);
					stackLastIndex.add(newLastIndex);
					stackInstanceCount.add(instanceCount + 1);
				}
			}
		}
		return allDistributionPossibilities;
	}

	/**
	 * calculate the expected performance for a given k and m according to the 
	 * closed form solution given in equation 34 in [1]
	 * 
	 * @param k vector of frequency estimates
	 * @param m number of hypothetically considered labels
	 * @return the expected performance
	 */
	private double getExpPerf(double[] k, int m)
	{
		// calculate n from the given k
		double n = 0;	
		// iterate over all classes
		for(int i = 0; i < numClasses; ++i)
		{
			// sum all k up to obtain n
			n+= k[i];
		}
		
		// call overloaded method with the calculated d and n 
		return getExpPerf(k, n, m, 1);
	}

	/**
	 * calculate the expected performance for a given k and m according to the 
	 * closed form solution given in equation 34 in [1]
	 * 
	 * @param k vector of frequency estimates
	 * @param n number of observed labels (sum of all ks)
	 * @param m number of hypothetically considered labels
	 * @param sumD the sum of all ds (usually 1)
	 * @return the expected performance
	 */
	private double getExpPerf(double[] k, double n, int m, int sumD)
	{
		// initialize the expected performance with 0
		double expPerf = 0;
		
		List<int[]> distributionsForM = distributions.get(m);
		
		// iterate over all distributions of hypothetic labels
		for(int distributionIdx = 0; distributionIdx < distributionsForM.size(); ++distributionIdx)
		{
			// calculate the three terms in equation 34 in [1]
			int[] l = distributionsForM.get(distributionIdx);
			int[] d = getD(k, l);
			double firstTerm = calculateFistTermClosedForm(n, l, m, d, sumD);
			double secondTerm = calculateSecondTermClosedForm(k, l, d);
			double thirdTerm = calculateThirdTermClosedForm(l, m);
			
			// multiply all results and add it to the expected performance
			expPerf += firstTerm * secondTerm * thirdTerm;
		}
		
		return expPerf;
	}
	
	/**
	 * get the decision vector from the frequency estimations 
	 * and hypothetic label distribution
	 * 
	 * @param k vector of frequency estimates
	 * @param l	the distribution of hypothetic labels
	 * @return	the decision vector
	 */
	private int[] getD(double[] k, int[] l)
	{
		int maxIdx = 0;
		double maxVal = k[0] + l[0];
		int[] d = new int[numClasses];
		for(int i = 1; i < numClasses; ++i)
		{
			double val = k[i] + l[i];
			if(val > maxVal)
			{
				maxVal = val;
				maxIdx = i;
			}
			d[i] = 0;
		}
		
		d[maxIdx] = 1;
		
		return d;
	}
	
	/**
	 * calculate the first term needed for equation 34 in [1]
	 * 
	 * @param n	number of observed labels (sum of all ks)
	 * @param l	the distribution of hypothetic labels
	 * @param sumL sum of all ls (= m)
	 * @param d	decision vector (a vector where di = 1 if ki = max(k) otherwise 0)
	 * @param sumD the sum of all ds (usually 1)
	 * @return	the result for the first term
	 */
	private double calculateFistTermClosedForm(double n, int[] l, int sumL, int[] d, int sumD)
	{
		double result = 1;
		for(int j_ = 0; j_ < (sumL + sumD); ++j_)
		{
			result /= n + numClasses + j_;
		}
		
		return result;
	}
	
	/**
	 * calculate the second term needed for equation 34 in [1]
	 * 
	 * @param k vector of frequency estimates
	 * @param l	the distribution of hypothetic labels
	 * @param d	decision vector (a vector where di = 1 if ki = max(k) otherwise 0)
	 * @return	the result for the second term
	 */
	private double calculateSecondTermClosedForm(double[] k, int[] l, int[] d)
	{
		double result = 1;
		
		for(int i = 0; i < numClasses; ++i)
		{
			for(int j_ = 0; j_ < l[i] + d[i]; ++j_)
			{
				result *= j_ + k[i] + 1;
			}
		}
		
		return result;
	}
	
	/**
	 * calculate the third term needed for equation 34 in [1]
	 * 
	 * @param l	the distribution of hypothetic labels
	 * @param sumL sum of all ls (= m)
	 * @return	the result for the third term
	 */
	private double calculateThirdTermClosedForm(int[] l, int sumL)
	{
		double result = gamma(sumL + 1);
		
		for(int i = 0; i < numClasses; ++i)
		{
			result /= gamma(l[i] + 1);
		}
		
		return result;
	}

	/**
	 * calculate the performance gain by calculating the expected performances for all m <= M
	 * @param k vector of frequency estimates
	 * @return the performance gain
	 */
	private double getPerfGain(double[] k)
	{
		
		double maxGain = 0;
		
		double expCurPerf = getExpPerf(k, 0);
		
		for(int m = 1; m <= mMax; ++m)
		{
			double currentPerfForM = getExpPerf(k, m); 
			double gain = (currentPerfForM - expCurPerf)/m;
			maxGain = Math.max(maxGain, gain);
		}
		
		return maxGain;
	}
	
	/**
	 * calculate the score by weighting the performance gain with the density
	 * @param point the point the score should be calculated for
	 * @return the score
	 */
	private double getAlScore(Instance inst)
	{

		double[] point = new double[inst.numAttributes() - 1];
		for(int i = 0; i < point.length; ++i)
		{
			point[i] = inst.value(i);
		}
		double[] cp = normalizeVotes(classifier.getVotesForInstance(inst));
		double[] k = getK(point, cp);
		double density = useDensityWeight? getDensity(k) : 1.0;
		
		double perfGain = getPerfGain(k);
		
		return density * perfGain;	
	}
	
	/**
	 * normalize the output of classifiers such that
	 * all classes have at least one prediction
	 * @param votes the prediction for each class
	 * @return the normalized prediction
	 */
	private double[] normalizeVotes(double[] votes) {
		double[] normVotes = new double[numClasses];
		double sum = 0.0;

		for (int i = 0; i < votes.length; i++)
			sum += votes[i];
		
		
		for (int i = 0; i < votes.length; i++)
		{
			normVotes[i] = votes[i]/sum;	
			if(sum <= 0)
			{
				normVotes[i] = 0;
			}
		}
			
		return normVotes;
	}
	
	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public int getLastLabelAcqReport() {
		return budgetManager.getLastLabelAcqReport();
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		return normalizeVotes(classifier.getVotesForInstance(inst));
	}

	@Override
	public void resetLearningImpl() {
		distributions = new ArrayList<>();
		for(int m = 0; m <= mMax; ++m)
		{
			distributions.add(getAllDistributionPossibilities(m, numClasses));
		}

		labeledDataKernelEstimator = new PALStreamEstimatorMultivariate(bandwidth, labeledDataKernelDensityEstimatorWindowOption.getValue());
		allDataKernelEstimator = new PALStreamEstimatorMultivariate(bandwidth, allDataKernelDensityEstimatorWindowOption.getValue());

		labeledDataStandartDeviationEstimator = new StandardDeviationEstimator(numAttributes);
		allDataStandartDeviationEstimator = new StandardDeviationEstimator(numAttributes);

		classifier = (Classifier) getPreparedClassOption(classifierOption);
		budgetManager = (BudgetManager) getPreparedClassOption(budgetManagerOption);
		budgetManager.resetLearning();
		mMax = (int)mMaxOption.getValue();
		useDensityWeight = useDensityWeightOption.isSet();
		bandwidth = bandwidthOption.getValue();
	}
	
	@Override
	public void trainOnInstanceImpl(Instance inst) {
		double[] point = new double[inst.numAttributes() - 1];
		for(int i = 0; i < point.length; ++i)
		{
			point[i] = inst.value(i);
		}
		double alScore = getAlScore(inst);

		boolean acquireLabel = budgetManager.isAbove(alScore);

		if(acquireLabel)
		{
			classifier.trainOnInstance(inst);
			
			double[] removedInstance = labeledDataKernelEstimator.addValue(point);
			labeledDataStandartDeviationEstimator.addPoint(removedInstance, point);
		}

		double[] removedInstance = allDataKernelEstimator.addValue(point);
		allDataStandartDeviationEstimator.addPoint(removedInstance, point);
		
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		List<Measurement> mergedMeasurements = new ArrayList<>();
		
		mergedMeasurements.addAll(Arrays.asList(classifier.getModelMeasurements()));
		return mergedMeasurements.toArray(new Measurement[mergedMeasurements.size()]);
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		if(classifier instanceof AbstractClassifier)
		{
			((AbstractClassifier) classifier).getModelDescription(out, indent);
		}
	}
	
	@Override
	public void setModelContext(InstancesHeader ih) {
		super.setModelContext(ih);
		
		this.numClasses = ih.numClasses();
		this.numAttributes = ih.numAttributes() - 1;
		
		resetLearning();
	}
}
