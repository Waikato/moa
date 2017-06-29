/*
 *    TargetMean.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author  J. Duarte, A. Bifet, J. Gama
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
package moa.classifiers.rules.functions;
/**
 * TargetMean - Returns the mean of the target variable of the training instances
 * 
 * @author João Duarte 
 * 
 *  */
import moa.classifiers.AbstractClassifier;
import moa.core.Measurement;
import moa.core.StringUtils;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;


public class TargetMean extends AbstractClassifier implements AMRulesRegressorFunction {

	/**
	 * 
	 */
	protected double n;
	protected double sum;
	protected double errorSum;
	protected double nError;
	private double fadingErrorFactor;
	
	private static final long serialVersionUID = 7152547322803559115L;

	public FloatOption fadingErrorFactorOption = new FloatOption(
			"fadingErrorFactor", 'e', 
			"Fading error factor for the TargetMean accumulated error", 0.99, 0, 1);
	
	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		double[] currentMean=new double[1];
		if (n>0)
			currentMean[0]=sum/n;
		else
			 currentMean[0]=0;
		return currentMean;
	}

	@Override
	public void resetLearningImpl() {
		sum=0;
		n=0;
		errorSum=Double.MAX_VALUE;
		nError=0;
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		updateAccumulatedError(inst);
		this.n+=inst.weight();
		this.sum+=inst.classValue()*inst.weight();
	}
	protected void updateAccumulatedError(Instance inst){
		double mean=0;
		nError=inst.weight()+fadingErrorFactor*nError;
		if(n>0)
			mean=sum/n;			
		errorSum=Math.abs(inst.classValue()-mean)*inst.weight()+fadingErrorFactor*errorSum;	
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		StringUtils.appendIndented(out, indent, "Current Mean: " + this.sum/this.n);
		StringUtils.appendNewline(out);	
		
	}
	/* JD
	 * Resets the learner but initializes with a starting point 
	 * */
	public void reset(double currentMean, double numberOfInstances) {
		this.sum=currentMean*numberOfInstances;
		this.n=numberOfInstances;
		this.resetError();
	}
	
	/* JD
	 * Resets the learner but initializes with a starting point 
	 * */
	public double getCurrentError(){
		if(this.nError>0)
			return this.errorSum/this.nError;
		else
			return Double.MAX_VALUE;
	}

	public TargetMean(TargetMean t) {
		super();
		this.n = t.n;
		this.sum = t.sum;
		this.errorSum = t.errorSum;
		this.nError = t.nError;
		this.fadingErrorFactor = t.fadingErrorFactor;
		this.fadingErrorFactorOption = t.fadingErrorFactorOption;
	}

	public TargetMean() {
		super();
		fadingErrorFactor=fadingErrorFactorOption.getValue();
	}

	public void resetError() {
		this.errorSum=0;
		this.nError=0;
	}
	


}
