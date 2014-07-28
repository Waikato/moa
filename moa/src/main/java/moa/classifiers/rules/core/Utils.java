package moa.classifiers.rules.core;

import moa.core.DoubleVector;

public final class Utils {

	public static double computeVariance(double count, double sum, double sumSquares)
	{
		return (sumSquares - ((sum * sum)/count))/count;
	}
	
	public static double computeVariance(DoubleVector statistics)
	{
		return computeVariance(statistics.getValue(0),statistics.getValue(1),statistics.getValue(2));
	}

	public static double computeSD(double squaredSum, double sum, double weightSeen) {
		if (weightSeen > 1) {
			return Math.sqrt((squaredSum - ((sum * sum) / weightSeen)) / (weightSeen - 1.0));
		}
		return 0.0;
	}
	
	public static double computeSD(DoubleVector statistics)
	{
		return computeSD(statistics.getValue(0),statistics.getValue(1),statistics.getValue(2));
	}

	public static DoubleVector[][] copy(DoubleVector[][] toCopy) {
		DoubleVector[][] copy = new DoubleVector[toCopy.length][];
        for (int i=0; i<toCopy.length; i++)
        	copy[i]=Utils.copy(toCopy[i]);
        return copy;
	}
	
	public static DoubleVector[]copy(DoubleVector[] toCopy) {
		DoubleVector[] copy = new DoubleVector[toCopy.length];
        	for (int i=0; i<toCopy.length; i++)
        		copy[i]=new DoubleVector(toCopy[i]);
        return copy;
	}

}
