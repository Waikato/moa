/*
 *    SDRSplitCriterionAMRules.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author A. Bifet, J. Duarte, J. Gama
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
package moa.classifiers.rules.driftdetection;

public class PageHinkleyFading extends PageHinkleyTest {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7110953184708812339L;
	private double fadingFactor=0.99;
	
	public PageHinkleyFading(double threshold, double alpha) {
		super(threshold, alpha);
	}
	protected double instancesSeen;

	@Override
	public void reset() {

		super.reset();
		this.instancesSeen=0;
		
	}

	@Override
	public boolean update(double error) {
		this.instancesSeen=1+fadingFactor*this.instancesSeen;
        double absolutError = Math.abs(error);
        
        this.sumAbsolutError = fadingFactor*this.sumAbsolutError + absolutError;
        if (this.instancesSeen > 30) {
        	double mT = absolutError - (this.sumAbsolutError / this.instancesSeen) - this.alpha;
        	this.cumulativeSum = this.cumulativeSum + mT; // Update the cumulative mT sum
        	if (this.cumulativeSum < this.minimumValue) { // Update the minimum mT value if the new mT is smaller than the current minimum
        		this.minimumValue = this.cumulativeSum;
        	}
        	return (((this.cumulativeSum - this.minimumValue) > this.threshold));
        }
        return false;
	}



}
