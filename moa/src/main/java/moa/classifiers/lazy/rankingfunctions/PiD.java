package moa.classifiers.lazy.rankingfunctions;

import java.lang.reflect.Array;
import java.util.Arrays;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.NumericAttributeClassObserver;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class PiD extends AbstractOptionHandler implements NumericAttributeClassObserver {

	public FloatOption alphaOption = new FloatOption(
			"alpha", 
			'a', 
			"Threshold to split an interval", 
			2.0);
	
	public IntOption sizeOption = new IntOption(
			"size", 
			'n', 
			"Number of bins", 
			10, 2, 10000);
	
	private double min = Double.MAX_VALUE;
	private double max = Double.MIN_VALUE;
	private double[] breaks;
	private double[][] counts;

	private double tempBreak;

	
	private double numObservations;
	private int summarySize;
	private int nbClasses;


	int bzz = 0;
	int totalMerge = 0;
	int totalSplit = 0;

	public void remove(final double value, final int classValue, final double weight)
	{
		//max
		if (value > max)
		{
			max = value;
		}
		//min
		if (value < min)
		{
			min = value;
		}

		//find insertion position
		final int pos = findPosCounts(value);
		counts[pos][classValue] -= weight;
		numObservations -= weight;

	}

	public void insert(final double value, final int classValue, final double weight)
	{
		//max
		if (value > max)
		{
			max = value;
		}
		//min
		if (value < min)
		{
			min = value;
		}
		
		//first insertion
		if (breaks == null)
		{
			//we use one more space for temporary work
			breaks = new double[sizeOption.getValue()];
			counts = new double[sizeOption.getValue()+1][classValue+1];
			nbClasses = 1;
		}

		//new class found
		if (classValue >= nbClasses)
		{
			nbClasses = classValue + 1;
			//extend counts arrays size in the summary
			for (int i=0; i!= sizeOption.getValue(); ++i)
			{
				final double newCount[] = new double[nbClasses];
				System.arraycopy(counts[i], 0, newCount, 0, counts[i].length);
				counts[i] = newCount;
			}
		}
		
		//find insertion position
		final int pos = findPosCounts(value);

		//insert 
//		System.out.println("Pos: " + pos + " - attVal: " + attVal);
		if (summarySize < sizeOption.getValue())
		{
			insertInterval(value, pos);
			summarySize++;
		}
		counts[pos][classValue] += weight;

		if (summarySize == sizeOption.getValue() && Utils.sum(counts[pos])+1 >((numObservations + 2)* alphaOption.getValue())/sizeOption.getValue())
		{
			//System.out.println("pos split = " + pos);


			tempBreak = breaks[breaks.length-1];
			//need to split this interval
			split(pos);
			//now recover space by merging 2 intervals
			mergeBestInterval();

			breaks[breaks.length-1] = tempBreak;
			counts[counts.length-1] = new double[nbClasses];
		}
		numObservations += weight;
		//System.out.println("insertion " + numObservations + " VALUE " + value);
		//System.out.println("final\n" + toString());
	}
	
	/**
	 * Split an interval in 2
	 * Assumptions: equi repartition in an interval
	 * @param pos
	 */
	private void split(final int pos)
	{
		totalSplit++;

		//System.out.println("split before\n" + toString());
		//System.out.println("total obs " + numObservations);

		// something wrong here
		double breaK;
		final int size = sizeOption.getValue();

//		System.out.println("Split pos: " + pos);
		//keep Gama logics
		// if spliting last break
		if (pos == size-1)
		{
			breaks[size-1] = breaks[size-2] + (max-min)/size;
			counts[size] = new double[nbClasses];
			splitCounts(counts[size-1], counts[size]);
			
		}
		else
		{
			// if spliting first break
			if (pos == 0)
			{
				breaK = breaks[0] - (max-min)/size;
			}
			else
			{
				breaK = breaks[pos-1] + (breaks[pos] - breaks[pos-1])/2;
			}
			//insert new interval
			insertInterval(breaK, pos);
			splitCounts(counts[pos+1], counts[pos]);
		}

		//System.out.println("split after\n" + toString());
		//System.out.println("total obs " + numObservations);
		//System.console().readLine();
	}
	
	private void insertInterval(final double breaK, final int pos) {
		System.arraycopy(breaks, pos, breaks, pos+1, breaks.length-pos-1);
		breaks[pos] = breaK;
		System.arraycopy(counts, pos, counts, pos+1, counts.length-pos-1);
		counts[pos] = new double[nbClasses];
	}

	private void splitCounts(final double toSplit[], final double split1[])
	{
		for (int i=0; i!=nbClasses ; ++i)
		{
			split1[i] = toSplit[i] / 2;
			toSplit[i] -= split1[i];
		}
	}
	
	private int findPosCounts(final double value)
	{
		int pos = Arrays.binarySearch(breaks, 0, Math.min(summarySize, sizeOption.getValue()-1), value);
		
		//not found but we can get the position
		if (pos < 0) {
			pos = -pos-1;
		}
		return pos;
	}

	// probably something wrong here
	private void mergeBestInterval()
	{
		totalMerge++;

		//System.out.println("before merging\n" + toString());
		//System.out.println("total obs " + numObservations);
		double smallestCount = Double.MAX_VALUE;
		int bestPosToMerge = 1;
		
		double previousCount = Utils.sum(counts[0]);
		double nextCount;
		
		//find best position to merge
		for (int i=1; i != counts.length; ++i)
		{
			nextCount = Utils.sum(counts[i]);
			final double total = previousCount + nextCount;
			if (total < smallestCount)
			{
				smallestCount = total;
				bestPosToMerge = i;
			}
			previousCount = nextCount;
		}

		//merge counts
		for (int k=0; k!=nbClasses; ++k)
		{
			counts[bestPosToMerge-1][k] += counts[bestPosToMerge][k];
		}

		//		System.out.println("Best pos: " + bestPosToMerge);
		System.arraycopy(counts, bestPosToMerge+1, counts, bestPosToMerge, counts.length-bestPosToMerge-1);


		//remove break
		System.arraycopy(breaks, bestPosToMerge, breaks, bestPosToMerge-1, breaks.length-bestPosToMerge);
		//System.out.println("merged " + bestPosToMerge);

		//System.out.println("after merging\n" + toString());
		//System.out.println("total obs " + numObservations);
		//System.console().readLine();
	}

	@Override
	public String toString()
	{
		final StringBuffer buf = new StringBuffer();
		double total = 0.0;
		for (int k=0;k!=nbClasses;k++)
		{
			buf.append(counts[0][k] + "\t");
			total += counts[0][k];
		}
		for (int i=0;i!=summarySize;++i)
		{
			buf.append("\n" + breaks[i] + "\t");
			for (int k=0;k!=nbClasses;k++)
			{
				buf.append(counts[i+1][k] + "\t");
				total += counts[i+1][k];	
			}
		}
		buf.append("\nTotal: " + total);
		return buf.toString();
	}
	
	private double getCounts(final int classValue) {
		double count = 0;
		for (int i=0;i<summarySize; i++)
			count += counts[i][classValue];
		return count;
	}

	public int[][] generateContingencyTable() {
		final int contingencyTable[][] = new int[summarySize][nbClasses];

		for (int i=0; i<summarySize; i++)
		{
			for (int k=0; k<nbClasses; k++)
			{
				contingencyTable[i][k] = (int)counts[i][k];
			}
		}
		return contingencyTable;
	}
	public double[][] generateContingencyTableDouble()
	{
		final double contingencyTable[][] = new double[summarySize][nbClasses];

		for (int i=0; i<summarySize; i++)
		{
			for (int k=0; k<nbClasses; k++)
			{
				contingencyTable[i][k] = counts[i][k];
			}
		}
		return contingencyTable;
	}

	public double[] getSuggestedCutPoints() {
		final double cutPoints[] = new double[summarySize];
		System.arraycopy(breaks, 0, cutPoints, 0, summarySize);
		return cutPoints;
	}

	public double getNumObservations() {
		return numObservations;
	}

	public double getDensity(final double value, final int classValue) {

		if (numObservations < 0)
			return 0.0;
		
		final int pos = findPosCounts(value);
    	return counts[pos][classValue] / Utils.sum(counts[pos]);
		
	}
	
	public double getCountInInterval(final double value, final int classValue) {
		if (classValue >= nbClasses)
			return 0;
//		System.out.println(findPosCounts(value) + " - " + classValue + " - " + nbClasses);
    	return counts[findPosCounts(value)][classValue];
	}
	
	public double getCountBelow(final double splitValue, final int classValue) {
		int pos = 0;
		//counts before the first cut point
		double count = counts[0][classValue];
		
		//iterate on break
		while(pos < summarySize-1 && breaks[pos] <= splitValue) {
			count += counts[pos+1][classValue];
			pos++;
		}
		return count;
	}

	public String getMethodName() {
		return "PiD";
	}

	@Override
	public void observeAttributeClass(final double attVal, final int classVal, final double weight) {
		insert(attVal, classVal, weight);
		
	}

	@Override
	public double probabilityOfAttributeValueGivenClass(final double attVal, final int classVal) {
		// TODO Auto-generated method stub
		return getDensity(attVal, classVal);
	}

	@Override
	public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(final SplitCriterion criterion, final double[] preSplitDist,
			final int attIndex, final boolean binaryOnly) {
		
        AttributeSplitSuggestion bestSuggestion = null;
        final double[] cutpoints = getSuggestedCutPoints();
        for (final double cutpoint : cutpoints) {
            final double[][] postSplitDists = getClassDistsResultingFromBinarySplit(cutpoint);
            final double merit = criterion.getMeritOfSplit(preSplitDist,
                    postSplitDists);
            if ((bestSuggestion == null)
                    || (merit > bestSuggestion.merit)) {
                bestSuggestion = new AttributeSplitSuggestion(
                        new NumericAttributeBinaryTest(attIndex,
                        cutpoint, true), postSplitDists, merit);
            }
        }
        return bestSuggestion;
	}
	
    public double[][] getClassDistsResultingFromBinarySplit(final double splitValue) {
        final DoubleVector lhsDist = new DoubleVector();
        final DoubleVector rhsDist = new DoubleVector();
        for (int i = 0; i < nbClasses; i++) {
            final double countBelow = getCountBelow(splitValue, i);
            lhsDist.addToValue(i, countBelow);
            rhsDist.addToValue(i, getCounts(i) - countBelow);
        }
        return new double[][]{lhsDist.getArrayRef(), rhsDist.getArrayRef()};
    }

	@Override
	public void observeAttributeTarget(final double attVal, final double target) {
		//TODO?
		
	}

	/* (non-Javadoc)
	 * @see moa.MOAObject#getDescription(java.lang.StringBuilder, int)
	 */
	@Override
	public void getDescription(final StringBuilder sb, final int indent) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see moa.options.AbstractOptionHandler#prepareForUseImpl(moa.tasks.TaskMonitor, moa.core.ObjectRepository)
	 */
	@Override
	protected void prepareForUseImpl(final TaskMonitor monitor, final ObjectRepository repository) {
		// TODO Auto-generated method stub
		
	}

}
