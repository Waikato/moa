/*
 *    ADDM: Adwin-based Drift Detection Method for NA.
 *    Copyright (C) 2020 Suarez-Cetrulo, A.
 *    @authors Andres L. Suarez-Cetrulo (andres.suarez-cetrulo@ucd.ie) 
 *    @version $Version: 1 $
 *    
 *    Evolved from ADWINChangeDetector.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *    @version $Revision: 7 $
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
 */

/**
 * Adwin-based Drift Detection Method for NA. (ADDM) 
 * published as:
 *     TBD
 */

package moa.classifiers.core.driftdetection;

import moa.core.ObjectRepository;
import com.github.javacliparser.FloatOption;
import moa.tasks.TaskMonitor;

/**
 * ADDM: Adwin-based Drift Detection Method for NA.
 * */

public class ADDM extends AbstractChangeDetector {
	private static final long serialVersionUID = -686319151837368086L;
	
	protected ADWIN adwinWarning; 
    protected ADWIN adwinDrift; 

    public FloatOption deltaAdwinWarningOption = new FloatOption("deltaAdwinWarning", 'p',
            "Delta of Adwin warning detection", 0.0001, 0.0, 1.0);
    
    public FloatOption deltaAdwinDriftOption = new FloatOption("deltaAdwinDrift", 'a',
            "Delta of Adwin change detection", 0.00001, 0.0, 1.0);
    
    protected boolean isWarningDetected;
    
    @Override
    public void input(double inputValue) {
    	this.isWarningDetected = false;  // is there a warning starting from this input value?
//    	if (this.adwinWarning != null) System.out.println("width warning detector slide window: "+adwinWarning.getWidth());
//    	else System.out.println("warning detector is NULL");
    	
        if (this.adwinWarning == null) {
        	resetWarningDetector();
        }
        if (this.adwinDrift == null) {
            resetLearning();
        }
        double ErrEstimWarn = this.adwinWarning.getEstimation();
        if(adwinWarning.setInput(inputValue)) {
            if (this.adwinWarning.getEstimation() > ErrEstimWarn) {
                this.isWarningZone = true;
                this.isWarningDetected = true;
            }
        }
        
        double ErrEstimDrift = this.adwinDrift.getEstimation();
        if(adwinDrift.setInput(inputValue)) {
            if (this.adwinDrift.getEstimation() > ErrEstimDrift) {
                this.isChangeDetected = true;
                this.isWarningZone = false;
            }
        }
        this.delay = 0.0;
        this.estimation = adwinDrift.getEstimation();
    }
    
    /*
     * It returns if there has been a warning in the last iteration.
     * */
    public boolean getWarning() {
    	return this.isWarningDetected;
    }
    
    public void resetWarningDetector() {
    	adwinWarning = new ADWIN((double) this.deltaAdwinWarningOption.getValue());
    }
    
	public void resetDriftDetector() {
        adwinDrift = new ADWIN((double) this.deltaAdwinDriftOption.getValue());
        this.isChangeDetected = false;
	}
	
    @Override
    public void resetLearning() {
    	adwinWarning = new ADWIN((double) this.deltaAdwinWarningOption.getValue());
        adwinDrift = new ADWIN((double) this.deltaAdwinDriftOption.getValue());
    }
    
    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // TODO Auto-generated method stub
    }

}