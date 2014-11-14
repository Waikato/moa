package moa.classifiers.rules.core;

import moa.classifiers.rules.multilabel.attributeclassobservers.SingleVector;
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
	
	public static SingleVector[]copyAsFloatVector(DoubleVector[] toCopy) {
		SingleVector[] toCopyFloat = new SingleVector[toCopy.length];
        	for (int i=0; i<toCopy.length; i++)
        		toCopyFloat[i]=new SingleVector(toCopy[i].getArrayRef());
        return toCopyFloat;
	}
	
	public static SingleVector[]copy(SingleVector[] toCopy) {
		SingleVector[] copy = new SingleVector[toCopy.length];
        	for (int i=0; i<toCopy.length; i++)
        		copy[i]=new SingleVector(toCopy[i]);
        return copy;
	}
	
	public static DoubleVector floatToDoubleVector(SingleVector toCopy) {
		float [] ref=toCopy.getArrayRef();
		double [] array= new double[ref.length];
		for(int i=0; i<ref.length;i++){
			array[i]=(double)ref[i];
		}
		return new DoubleVector(array);
	}
	
	public static int [] complementSet(int[] referenceSet, int[] comparingSet) {
		int [] indices= new int[referenceSet.length-comparingSet.length];
		int j=0,l=0,i;
		for (i=0; i<referenceSet.length && l<comparingSet.length;i++){
			if(referenceSet[i]!=comparingSet[l])
			{
				indices[j]=referenceSet[i];
				j++;
			}
			else
				l++;
		}
		while(i<referenceSet.length){
			indices[j]=referenceSet[i];
			i++; j++;
		}		
		return indices;
	}
	
	public static int[] getIndexCorrespondence(int[] originalSet, int[] subSet) {
		int [] indices= new int[subSet.length];
		int j=0;
		for (int i=0; i<subSet.length;i++){
			while(originalSet[j]!=subSet[i])
				j++;
			indices[i]=j;
		}
		return indices;
	}
	
}
