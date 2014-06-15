/*
 *    EWMAChartDM.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Manuel Baena (mbaena@lcc.uma.es)
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
 */
package moa.classifiers.core.driftdetection;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

/**
 * Drift detection method based in EWMA Charts of Ross, Adams, Tasoulis and Hand
 * 2012
 *
 *
 * @author Manuel Baena (mbaena@lcc.uma.es)
 * @version $Revision: 7 $
 */
public class EWMAChartDM extends AbstractChangeDetector {

    private static final long serialVersionUID = -3518369648142099719L;

    //private static final int DDM_MINNUMINST = 30;
    public IntOption minNumInstancesOption = new IntOption(
            "minNumInstances",
            'n',
            "The minimum number of instances before permitting detecting change.",
            30, 0, Integer.MAX_VALUE);

    public FloatOption lambdaOption = new FloatOption("lambda", 'l',
            "Lambda parameter of the EWMA Chart Method", 0.2, 0.0, Float.MAX_VALUE);

    private double m_n;

    private double m_sum;
    
    private double m_p;
    
    private double m_s;
    
    private double lambda;
    
    private double z_t;

    public EWMAChartDM() {
        resetLearning();
    }

    @Override
    public void resetLearning() {
        m_n = 1.0;
        m_sum = 0.0;
        m_p = 0.0;
        m_s = 0.0;
        z_t = 0.0;
        lambda = this.lambdaOption.getValue();
    }

    @Override
    public void input(double prediction) {
        // prediction must be 1 or 0
        // It monitors the error rate
         if (this.isChangeDetected == true || this.isInitialized == false) {
            resetLearning();
            this.isInitialized = true;
        }

        m_sum += prediction;
        
        m_p = m_sum/m_n; // m_p + (prediction - m_p) / (double) (m_n+1);

        m_s = Math.sqrt(  m_p * (1.0 - m_p)* lambda * (1.0 - Math.pow(1.0 - lambda, 2.0 * m_n)) / (2.0 - lambda));

        m_n++;

        z_t += lambda * (prediction - z_t);

        //double L_t = 2.76 - 6.23 * m_p + 18.12 * Math.pow(m_p, 3) - 312.45 * Math.pow(m_p, 5) + 1002.18 * Math.pow(m_p, 7); //%1 FP
        double L_t = 3.97 - 6.56 * m_p + 48.73 * Math.pow(m_p, 3) - 330.13 * Math.pow(m_p, 5) + 848.18 * Math.pow(m_p, 7); //%1 FP
        //double L_t = 1.17 + 7.56 * m_p - 21.24 * Math.pow(m_p, 3) + 112.12 * Math.pow(m_p, 5) - 987.23 * Math.pow(m_p, 7); //%1 FP

        // System.out.print(prediction + " " + m_n + " " + (m_p+m_s) + " ");
        this.estimation = m_p;
        this.isChangeDetected = false;
        this.isWarningZone = false;
        this.delay = 0;

        if (m_n < this.minNumInstancesOption.getValue()) {
            return;
        }
            
        if (m_n > this.minNumInstancesOption.getValue() && z_t > m_p + L_t * m_s) {
            //System.out.println(m_p + ",D");
            this.isChangeDetected = true;
            //resetLearning();
        } else if (z_t > m_p + 0.5 *  L_t * m_s) {
            //System.out.println(m_p + ",W");
            this.isWarningZone = true;
        } else {
            this.isWarningZone = false;
            //System.out.println(m_p + ",N");
        }
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