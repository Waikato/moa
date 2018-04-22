/*
 *    Utils.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */
package moa.classifiers.rules.core;

import moa.classifiers.rules.multilabel.attributeclassobservers.SingleVector;
import moa.core.DoubleVector;

/**
 * Class that contains several utilities
 * Variance
 * Standard deviation
 * Vector operations(copy, etc)
 * Entropy
 * Complementary set
 */


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
	
	public static double computeEntropy(DoubleVector statistics){
      return computeEntropy(statistics.getValue(0),statistics.getValue(1));
	}
        
  public static double computeEntropy(double count, double sum){
            if(sum/count==1 || sum==0)
                return 0;
            else
                return -(sum/count*Math.log(sum/count) + (1-sum/count)*Math.log(1-sum/count) );
            
	}
}
