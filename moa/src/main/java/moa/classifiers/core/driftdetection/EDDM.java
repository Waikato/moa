/*
 *    EDDM.java
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
 * Drift detection method based in EDDM method of Manuel Baena et al.
 *
 * <p>Early Drift Detection Method. Manuel Baena-Garcia, Jose Del Campo-Avila,
 * Raúl Fidalgo, Albert Bifet, Ricard Gavalda, Rafael Morales-Bueno. In Fourth
 * International Workshop on Knowledge Discovery from Data Streams, 2006.</p>
 *
 * @author Manuel Baena (mbaena@lcc.uma.es)
 * @version $Revision: 7 $
 */
public class EDDM extends AbstractChangeDetector {

    /**
     *
     */
    private static final long serialVersionUID = 140980267062162000L;

    public IntOption minNumInstancesOption = new IntOption(
            "minNumInstances",
            'n',
            "The minimum number of instances before permitting detecting change.",
            30, 0, Integer.MAX_VALUE);

    public FloatOption warningLevelOption = new FloatOption(
            "warningLevel", 'w',
            "Warning level (alpha). Must be greater than or equal to outcontrolLevel.",
            0.95, 0.0, 1.0);

    public FloatOption outcontrolLevelOption = new FloatOption(
            "outcontrolLevel", 'o',
            "Outcontrol level (beta). Must be less than or equal to warningLevel.",
            0.9, 0.0, 1.0);

    public IntOption minNumErrorsOption = new IntOption(
            "minNumErrors",
            'e',
            "The minimum number of errors before permitting detecting change or warning.",
            30, 0, Integer.MAX_VALUE);

    private int minNumInstances;

    private double warningLevel;

    private double outcontrolLevel;

    private double m_numErrors;

    private int m_minNumErrors;

    private int m_n;

    private int m_d;

    private int m_lastd;

    private double m_mean;

    private double m_stdTemp;

    private double m_m2smax;

    private int m_lastLevel;

    public EDDM() {
        resetLearning();
    }

    @Override
    public void resetLearning() {
        m_n = 1;
        m_numErrors = 0;
        m_d = 0;
        m_lastd = 0;
        m_mean = 0.0;
        m_stdTemp = 0.0;
        m_m2smax = 0.0;
        //m_lastLevel = DDM_INCONTROL_LEVEL;
        this.estimation = 0.0;
        minNumInstances = this.minNumInstancesOption.getValue();
        warningLevel = this.warningLevelOption.getValue();
        outcontrolLevel = this.outcontrolLevelOption.getValue();
        m_minNumErrors = this.minNumErrorsOption.getValue();
    }

    @Override
    public void input(double prediction) {
        // prediction must be 1 or 0
        // It monitors the error rate
        // System.out.print(prediction + " " + m_n + " " + probability + " ");
        if (this.isChangeDetected == true || this.isInitialized == false) {
            resetLearning();
            this.isInitialized = true;
        }
        
        this.isChangeDetected = false;
        
        m_n++;
        if (prediction == 1.0) {
            this.isWarningZone = false;
            this.delay = 0;
            m_numErrors += 1;
            m_lastd = m_d;
            m_d = m_n - 1;
            int distance = m_d - m_lastd;
            double oldmean = m_mean;
            m_mean = m_mean + ((double) distance - m_mean) / m_numErrors;
            this.estimation = m_mean;
            m_stdTemp = m_stdTemp + (distance - m_mean) * (distance - oldmean);
            double std = Math.sqrt(m_stdTemp / m_numErrors);
            double m2s = m_mean + 2 * std;

            // System.out.print(m_numErrors + " " + m_mean + " " + std + " " +
            // m2s + " " + m_m2smax + " ");

            if (m2s > m_m2smax) {
                if (m_n > minNumInstances) {
                    m_m2smax = m2s;
                }
                //m_lastLevel = DDM_INCONTROL_LEVEL;
                // System.out.print(1 + " ");
            } else {
                double p = m2s / m_m2smax;
                // System.out.print(p + " ");
                if (m_n > minNumInstances && m_numErrors > m_minNumErrors
                        && p < outcontrolLevel) {
                    //System.out.println(m_mean + ",D");
                    this.isChangeDetected = true;
                    //resetLearning();
                    //return DDM_OUTCONTROL_LEVEL;
                } else if (m_n > minNumInstances
                        && m_numErrors > m_minNumErrors && p < warningLevel) {
                    //System.out.println(m_mean + ",W");
                    //m_lastLevel = DDM_WARNING_LEVEL;
                    this.isWarningZone = true;
                    //return DDM_WARNING_LEVEL;
                } else {
                    this.isWarningZone = false;
                    //System.out.println(m_mean + ",N");
                    //m_lastLevel = DDM_INCONTROL_LEVEL;
                    //return DDM_INCONTROL_LEVEL;
                }
            }
        } else {
            // System.out.print(m_numErrors + " " + m_mean + " " +
            // Math.sqrt(m_stdTemp/m_numErrors) + " " + (m_mean +
            // 2*Math.sqrt(m_stdTemp/m_numErrors)) + " " + m_m2smax + " ");
            // System.out.print(((m_mean +
            // 2*Math.sqrt(m_stdTemp/m_numErrors))/m_m2smax) + " ");
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