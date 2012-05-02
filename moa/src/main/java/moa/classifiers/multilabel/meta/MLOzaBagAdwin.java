/*
 *    MLOzaBagAdwin.java
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
import moa.classifiers.core.driftdetection.ADWIN;
import moa.classifiers.meta.OzaBagAdwin;
import moa.core.InstancesHeader;
import moa.core.MiscUtils;
import weka.core.Instance;

/**
 * MLOzaBagAdwin: Changes the way to compute accuracy as an input for Adwin
 * 
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public class MLOzaBagAdwin extends OzaBagAdwin {

    protected int m_L = -1;

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

    @Override
    public void trainOnInstanceImpl(Instance inst) {

        boolean Change = false;
        for (int i = 0; i < this.ensemble.length; i++) {
            int k = MiscUtils.poisson(1.0, this.classifierRandom);
            if (k > 0) {
                Instance weightedInst = (Instance) inst.copy();
                weightedInst.setWeight(inst.weight() * k);
                this.ensemble[i].trainOnInstance(weightedInst);
            }
            double[] prediction = this.ensemble[i].getVotesForInstance(inst);
            //Compute accuracy
            double actual[] = new double[prediction.length];
            for (short j = 0; j < prediction.length; j++) {
                actual[j] = inst.value(j);
            }
            // calculate
            int p_sum = 0, r_sum = 0;
            int set_union = 0;
            int set_inter = 0;
            double t = 0.01;
            for (int j = 0; j < prediction.length; j++) {
                int p = (prediction[j] >= t) ? 1 : 0;
                int R = (int) actual[j];
                if (p == 1) {
                    p_sum++;
                    // predt 1, real 1
                    if (R == 1) {
                        set_inter++;
                        set_union++;
                    } // predt 1, real 0
                    else {
                        set_union++;
                    }
                } else {
                    // predt 0, real 1
                    if (R == 1) {
                        set_union++;
                    } // predt 0, real 0
                    else {
                    }
                }
            }
            double accuracy = 0.0;
            if (set_union > 0) //avoid NaN
            {
                accuracy = ((double) set_inter / (double) set_union);
            }
            double ErrEstim = this.ADError[i].getEstimation();
            if (this.ADError[i].setInput(1.0 - accuracy)) {
                if (this.ADError[i].getEstimation() > ErrEstim) {
                    Change = true;
                }
            }
        }
        if (Change) {
            System.err.println("change!");
            double max = 0.0;
            int imax = -1;
            for (int i = 0; i < this.ensemble.length; i++) {
                if (max < this.ADError[i].getEstimation()) {
                    max = this.ADError[i].getEstimation();
                    imax = i;
                }
            }
            if (imax != -1) {

                this.ensemble[imax] = null;
                this.ensemble[imax] = (Classifier) getPreparedClassOption(this.baseLearnerOption);
                this.ensemble[imax].setModelContext(this.modelContext);
                this.ensemble[imax].trainOnInstance(inst);
                this.ADError[imax] = new ADWIN();
            }
        }
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
