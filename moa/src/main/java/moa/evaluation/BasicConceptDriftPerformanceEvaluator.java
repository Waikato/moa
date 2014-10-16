/*
 *    BasicConceptDriftPerformanceEvaluator.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.evaluation;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.AbstractMOAObject;
import moa.core.Example;
import moa.core.Measurement;

public class BasicConceptDriftPerformanceEvaluator extends AbstractMOAObject
        implements LearningPerformanceEvaluator<Example<Instance>> {

    private static final long serialVersionUID = 1L;

    protected double weightObserved;

    protected double numberDetections;
    
    protected double numberDetectionsOccurred;

    protected double numberChanges;

    protected double numberWarnings;

    protected double delay;

    protected double errorPrediction;

    protected double totalDelay;
    
    protected boolean isWarningZone;
    
    protected double inputValues;

    @Override
    public void reset() {
        this.weightObserved = 0.0;
        this.numberDetections = 0.0;
        this.numberDetectionsOccurred = 0.0;
        this.errorPrediction = 0.0;
        this.numberChanges = 0.0;
        this.numberWarnings = 0.0;
        this.delay = 0.0;
        this.totalDelay = 0.0;
        this.isWarningZone = false;
        this.inputValues = 0.0;
        this.hasChangeOccurred = false;
    }

    private boolean hasChangeOccurred = false;
    
    @Override
    public void addResult(Example<Instance> example, double[] classVotes) {
        Instance inst = example.getData();
        //classVotes[0] -> is Change
        //classVotes[1] -> is in Warning Zone
        //classVotes[2] -> delay
        //classVotes[3] -> estimation

        this.inputValues = inst.value(2);
        if (inst.weight() > 0.0 && classVotes.length == 4) {
            if (inst.numAttributes() > 1) {
                //if there is ground truth we monitor delay
                this.delay++;
            }
            this.weightObserved += inst.weight();
            if ( classVotes[0] == 1.0) {
                //Change detected
                //System.out.println("Change detected with delay "+ this.delay );
                this.numberDetections += inst.weight();
                if (this.hasChangeOccurred == true) { 
                    this.totalDelay += this.delay - classVotes[2];
                    this.numberDetectionsOccurred += inst.weight();
                    this.hasChangeOccurred = false;
                }
            }
            if (this.hasChangeOccurred && classVotes[1] == 1.0) {
                //Warning detected
                //System.out.println("Warning detected at "+getTotalWeightObserved());
                if (this.isWarningZone == false) {
                    this.numberWarnings += inst.weight();
                    this.isWarningZone = true;
                } 
            } else {
                this.isWarningZone = false;
            }
            if (inst.numAttributes() > 1) {
                if (inst.value(inst.numAttributes() - 2) == 1.0) {//Attribute 1
                    //Ground truth Change
                    this.numberChanges += inst.weight();
                    this.delay = 0;
                    this.hasChangeOccurred = true;
                }
            }
            //Compute error prediction
            if (classVotes.length > 1) {
                this.errorPrediction += Math.abs(classVotes[3] - inst.value(0));
            }
        }
    }

    @Override
    public Measurement[] getPerformanceMeasurements() {
        Measurement[] measurement;
        //if (totalDelay == 0.0) { //No Ground Truth
            measurement = new Measurement[]{
                new Measurement("learned instances",
                getTotalWeightObserved()),
                new Measurement("detected changes",
                getNumberDetections()),
                new Measurement("detected warnings",
                getNumberWarnings()),
                new Measurement("prediction error (average)",
                getPredictionError() / getTotalWeightObserved()),
                new Measurement("true changes",
                getNumberChanges()),
                new Measurement("delay detection (average)",
                getTotalDelay() / getNumberChanges()),
                new Measurement("true changes detected",
                getNumberChangesOccurred()),
                new Measurement("input values",
                getInputValues())
            };
       /* } else {
            measurement = new Measurement[]{
                new Measurement("learned instances",
                getTotalWeightObserved()),
                new Measurement("detected changes",
                getNumberDetections()),
                new Measurement("detected warnings",
                getNumberWarnings()),
                new Measurement("prediction error (average)",
                getPredictionError() / getTotalWeightObserved()),
                new Measurement("true changes",
                getNumberChanges()),
                new Measurement("delay detection (average)",
                getTotalDelay() / getNumberChanges()),
                new Measurement("true changes detected",
                getNumberChangesOccurred()),
                new Measurement("input values",
                getInputValues())
            };
        }*/
        return measurement;
    }

    public double getTotalWeightObserved() {
        return this.weightObserved > 0 ? this.weightObserved : 1.0;
    }

    public double getNumberDetections() {
        return this.numberDetections;
    }
    
    public double getInputValues() {
        return this.inputValues;
    }

    public double getPredictionError() {
        return this.errorPrediction;
    }

    public double getNumberChanges() {
        return this.numberChanges;
    }
    
    public double getNumberChangesOccurred() {
        return  this.numberDetectionsOccurred;
    }

    public double getNumberWarnings() {
        return this.numberWarnings;
    }

    public double getTotalDelay() {
        return this.totalDelay;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        Measurement.getMeasurementsDescription(getPerformanceMeasurements(),
                sb, indent);
    }

	@Override
	public void addResult(Example<Instance> testInst, Prediction prediction) {
		// TODO Auto-generated method stub
		
	}
    
}
