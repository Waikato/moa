/*
 *    OzaBag.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.classifiers.meta;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractEnsembleLearner;
import moa.core.MiscUtils;
import moa.core.Utils;
import moa.learners.InstanceLearner;
import moa.options.ClassOption;

/**
 * Incremental on-line bagging of Oza and Russell.
 *
 * <p>Oza and Russell developed online versions of bagging and boosting for
 * Data Streams. They show how the process of sampling bootstrap replicates
 * from training data can be simulated in a data stream context. They observe
 * that the probability that any individual example will be chosen for a
 * replicate tends to a Poisson(1) distribution.</p>
 *
 * <p>[OR] N. Oza and S. Russell. Online bagging and boosting.
 * In Artiﬁcial Intelligence and Statistics 2001, pages 105–112.
 * Morgan Kaufmann, 2001.</p>
 *
 * <p>Parameters:</p> <ul>
 * <li>-l : Classiﬁer to train</li>
 * <li>-s : The number of models in the bag</li> </ul>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public abstract class AbstractOzaBag<MLTask extends InstanceLearner> extends AbstractEnsembleLearner<MLTask> {

	public FloatOption lambdaOption = new FloatOption("lambda", 'u', "Lambda parameter of Poisson distribution", 1);
	
	public AbstractOzaBag(Class<MLTask> task, String defaultCLIString) {
		super(task);
		this.baseLearnerOption = new ClassOption("baseLearner", 'l', "Classifier to train.", task, defaultCLIString);
	}

	@Override
	public String getPurposeString() {
		return "Incremental on-line bagging of Oza and Russell.";
	}

	private static final long serialVersionUID = 1L;

	@Override
	public boolean isRandomizable() {
		return true;
	}

	public void trainOnInstanceImpl(Instance inst) {
		for (int i = 0; i < this.ensemble.size(); i++) {
			int k = MiscUtils.poisson(lambdaOption.getValue(), this.classifierRandom);
			if (k > 0) {
				Instance weightedInst = (Instance) inst.copy();
				weightedInst.setWeight(inst.weight() * k);
				this.ensemble.get(i).trainOnInstance(weightedInst);
			}
		}
	}
	
	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean correctlyClassifies(Instance inst) {
		// This is here for easy implementation of ensemble classifiers.
        return Utils.maxIndex(getPredictionForInstance(inst)) == (int) inst.classValue();
	}

	
	//    @Override
	//    public Classifier[] getSubClassifiers() {
	//        return this.ensemble.clone();
	//    }
}
