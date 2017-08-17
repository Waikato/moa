/*
 *    ALRandom.java
 *    Copyright (C) 2016 Otto von Guericke University, Magdeburg, Germany
 *    @author Daniel Kottke (daniel dot kottke at ovgu dot de)
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

import java.util.LinkedList;
import java.util.List;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.active.budget.BudgetManager;
import moa.core.Measurement;
import moa.options.ClassOption;

public class ALRandom extends AbstractClassifier implements ALClassifier {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Random Active learning classifier for evolving data streams";
    }
    
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "drift.SingleClassifierDrift");

    public ClassOption budgetManagerOption = new ClassOption("budgetManager",
            'b', "BudgetManager that should be used.",
            BudgetManager.class, "FixedBM");

    
    public Classifier classifier;

    public BudgetManager budgetManager;
    
	@Override
	public int getLastLabelAcqReport() {
		return budgetManager.getLastLabelAcqReport();
	}
	
	@Override
	public boolean isRandomizable() {
        return true;
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		return this.classifier.getVotesForInstance(inst);
	}

	@Override
	public void resetLearningImpl() {
        this.classifier = ((Classifier) getPreparedClassOption(this.baseLearnerOption)).copy();
        this.classifier.resetLearning();
        this.budgetManager = ((BudgetManager) getPreparedClassOption(this.budgetManagerOption));
        this.budgetManager.resetLearning();
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		double value = this.classifierRandom.nextDouble();
		if (this.budgetManager.isAbove(value)){
            this.classifier.trainOnInstance(inst);
		}
		
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
        List<Measurement> measurementList = new LinkedList<Measurement>();
        return measurementList.toArray(new Measurement[measurementList.size()]);
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
        ((AbstractClassifier) this.classifier).getModelDescription(out, indent);
		
	}
	
	@Override
	public void setModelContext(InstancesHeader ih) {
		super.setModelContext(ih);
		classifier.setModelContext(ih);
	}
}
