/*
 *    MCPAL.java
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
import java.util.Stack;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.active.budget.BudgetManager;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;


/**
 * This classifier is based on Multi-Class Probabilistic Active Learning (MCPAL)
 * proposed in [1] and uses the budget manager shown in [2].
 * 
 * This implementation is based on Christian's implementation of OPAL
 * and is modified to implement MCPAL
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
public class MCPAL extends AbstractClassifier implements ALClassifier {

	private static final long serialVersionUID = 1L;

	private MCPALEstimatorMultivariate[] kernelEstimators; // used only for labeled data
	private MCPALEstimatorMultivariate kernelEstimatorAllClasses;

	private int numClasses;
	
	private List<List<int[]>> distributions;
	
	private Classifier classifier;
	
	private int mMax;
	
	private double bandwidth;
	
	private BudgetManager budgetManager;
	
    public ClassOption classifierOption = new ClassOption("classifier", 'c',
            "Classifier to train.", Classifier.class, "drift.SingleClassifierDrift");
	
    public ClassOption budgetManagerOption = new ClassOption("budgetManager",
            'b', "BudgetManager that should be used.",
            BudgetManager.class, "ThresholdBM");
    
    public FloatOption mMaxOption = new FloatOption("M",
            'm', "The maximum number of hypothetic label.", 0, 0, Integer.MAX_VALUE);
    
    public FloatOption bandwidthOption = new FloatOption("bandWidth",
            'w', "The bandwidth to use for density estimation.", 1);
    
	public MCPAL() {
		resetLearningImpl();
	}
	
	@Override
	public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		super.prepareForUseImpl(monitor, repository);

		classifier = (Classifier) getPreparedClassOption(classifierOption);
		budgetManager = (BudgetManager) getPreparedClassOption(budgetManagerOption);
		mMax = (int)mMaxOption.getValue();
		bandwidth = bandwidthOption.getValue();
	}
	
	
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

	// ___________________________________MCPAL______________________________________

	

	/**
	 * calculate the frequency estimates (k) for a given instance
	 * 
	 * @param inst the instance for which the frequency estimates should be calculated for
	 * @return frequency estimates for each class
	 */
	private double[] getK(double[] inst) {
		double[] k = new double[numClasses];
		for(int cIdx = 0; cIdx < numClasses; ++cIdx)
		{
			k[cIdx] = kernelEstimators[cIdx].getFrequencyEstimate(inst);
		}
		return k;
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
				for(int newLastIndex = lastIdx; lastIdx < numClasses; ++lastIdx)
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
		// calculate d and n from the given k
		int[] d = new int[numClasses];
		int maxKIdx = 0;
		double n = 0;
		// iterate over all classes
		for(int i = 0; i < numClasses; ++i)
		{
			// initialize all ds with zero
			d[i] = 0;
			// check if the k for class i is higher than the current
			// assumed maximum of k
			if(k[maxKIdx] < k[i])
			{
				// replace the assumed maximum if it is
				maxKIdx = i;
			}
			// sum all k up to obtain n
			n+= k[i];
		}
		// set the di to 1 where ki = max(k)
		d[maxKIdx] = 1;
		
		// call overloaded method with the calculated d and n 
		return getExpPerf(k, n, m, d, 1);
	}

	/**
	 * calculate the expected performance for a given k and m according to the 
	 * closed form solution given in equation 34 in [1]
	 * 
	 * @param k vector of frequency estimates
	 * @param n number of observed labels (sum of all ks)
	 * @param m number of hypothetically considered labels
	 * @param d decision vector (a vector where di = 1 if ki = max(k) otherwise 0)
	 * @param sumD the sum of all ds (usually 1)
	 * @return the expected performance
	 */
	private double getExpPerf(double[] k, double n, int m, int[] d, int sumD)
	{
		// initialize the expected performance with 0
		double expPerf = 0;
		
		List<int[]> distributionsForM = distributions.get(m);
		
		// iterate over all distributions of hypothetic labels
		for(int distributionIdx = 0; distributionIdx < distributionsForM.size(); ++distributionIdx)
		{
			// calculate the three terms in equation 34 in [1]
			int[] l = distributionsForM.get(distributionIdx);
			double firstTerm = calculateFistTermClosedForm(n, l, m, d, sumD);
			double secondTerm = calculateSecondTermClosedForm(k, l, d);
			double thirdTerm = calculateThirdTermClosedForm(l, m);
			
			// multiply all results and add it to the expected performance
			expPerf += firstTerm * secondTerm * thirdTerm;
		}
		
		return expPerf;
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
			result /= n+j_;
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
				result *= j_ + k[i];
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

	private double getPerfGain(double[] k)
	{
		
		double maxGain = 0;
		
		double expCurPerf = getExpPerf(k, 0);
		
		for(int m = 1; m <= mMax; ++m)
		{
			double gain = (getExpPerf(k, m) - expCurPerf)/m;
			maxGain = Math.max(maxGain, gain);
		}
		
		return maxGain;
	}
	
	private double getAlScore(double[] point)
	{
		double density = kernelEstimatorAllClasses.getDensity(point, true);
		double[] k = getK(point);
		double perfGain = getPerfGain(k);
		return density * perfGain;	
	}
	

	private double[] normalizeVotes(double[] votes) {
		double[] normVotes = new double[numClasses];
		double sum = 0;
		for (int i = 0; i < votes.length; i++)
			sum += votes[i];
		if (sum != 0)
			for (int i = 0; i < votes.length; i++)
				normVotes[i] = votes[i] / sum;
		else {
			for (int i = 0; i < numClasses; i++)
				normVotes[i] = 1.0 / numClasses;
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
		numClasses = 0;
		classifier = null;
		budgetManager = null;
		distributions = new ArrayList<>();
		mMax = 0;
		kernelEstimatorAllClasses = null;
		kernelEstimators = null;
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		// TODO ADD AL & BUDGET
		double[] point = new double[inst.numAttributes() - 1];
		for(int i = 0; i < point.length; ++i)
		{
			point[i] = inst.value(i);
		}
		
		double alScore = getAlScore(point);
		
		boolean acquireLabel = budgetManager.isAbove(alScore);
		
		if(acquireLabel)
		{
			classifier.trainOnInstance(inst);
			
			int c = (int)inst.classValue();
			kernelEstimators[c].addValue(point);
			kernelEstimatorAllClasses.addValue(point);
		}
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
		
		numClasses = ih.getInstanceInformation().numClasses();
		for(int m = 0; m <= mMax; ++m)
		{
			distributions.add(getAllDistributionPossibilities(m, numClasses));
		}
		
		kernelEstimatorAllClasses = new MCPALEstimatorMultivariate(bandwidth);
		kernelEstimators = new MCPALEstimatorMultivariate[numClasses];
		for(int cIdx = 0; cIdx < numClasses; ++cIdx)
		{
			kernelEstimators[cIdx] = new MCPALEstimatorMultivariate(bandwidth);
		}
	}
	
}
