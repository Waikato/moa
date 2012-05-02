/*
 *    MLOzaBag.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jesse@tsc.uc3m.es)
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
package moa.classifiers.multilabel.meta;

import moa.classifiers.Classifier;
import moa.classifiers.meta.OzaBag;
import moa.core.InstancesHeader;
import weka.core.Instance;

/**
 * OzaBag for Multi-label data.
 * 
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public class MLOzaBag extends OzaBag {

    protected int m_L = -1;

    //protected Random random = null;

    /*
     * @Override public void resetLearningImpl() { super.resetLearningImpl();
     * //this.random = new Random(randomSeedOption.getValue()); }
     */
    @Override
    public void resetLearningImpl() {
        this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        for (int i = 0; i < this.ensemble.length; i++) {
            this.ensemble[i] = baseLearner.copy();
        }
    }

    @Override
    public void setModelContext(InstancesHeader raw_header) {

        //set the multilabel model context
        this.modelContext = raw_header;

        m_L = raw_header.classIndex() + 1;

        // reset ensemble
        this.resetLearningImpl();

        for (int i = 0; i < this.ensemble.length; i++) {
            this.ensemble[i].setModelContext(raw_header);
            this.ensemble[i].resetLearning();
        }
    }

    @Override // @note don't need this here
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public double[] getVotesForInstance(Instance x) {

        int L = x.classIndex() + 1;
        if (m_L != L) {
            m_L = L;
        }

        double y[] = new double[m_L];

        for (int i = 0; i < this.ensemble.length; i++) {
            double w[] = this.ensemble[i].getVotesForInstance(x);
            for (int j = 0; j < w.length; j++) {
                y[j] += w[j];
            }
        }

        return y;
    }
}
