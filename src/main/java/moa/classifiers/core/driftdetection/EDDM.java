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
 *    
 */
package moa.classifiers.core.driftdetection;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;


/**
 * Drift detection method based in EDDM method of Manuel Baena et al.
 * 
 * <p>Early Drift Detection Method. Manuel Baena-Garcia, Jose Del Campo-Avila,
 * Raúl Fidalgo, Albert Bifet, Ricard Gavalda, Rafael Morales-Bueno.
 * In Fourth International Workshop on Knowledge Discovery from Data Streams,
 * 2006.</p>
 *
 * @author Manuel Baena (mbaena@lcc.uma.es)
 * @version $Revision: 7 $
 */
public class EDDM extends AbstractOptionHandler implements DriftDetectionMethod {

    /**
     *
     */
    private static final long serialVersionUID = 140980267062162000L;

    private static final double FDDM_OUTCONTROL = 0.9;

    private static final double FDDM_WARNING = 0.95;

    private static final double FDDM_MINNUMINSTANCES = 30;

    private double m_numErrors;

    private int m_minNumErrors = 30;

    private int m_n;

    private int m_d;

    private int m_lastd;

    private double m_mean;

    private double m_stdTemp;

    private double m_m2smax;

    private int m_lastLevel;

    public EDDM() {
        initialize();
    }

    private void initialize() {
        m_n = 1;
        m_numErrors = 0;
        m_d = 0;
        m_lastd = 0;
        m_mean = 0.0;
        m_stdTemp = 0.0;
        m_m2smax = 0.0;
        m_lastLevel = DDM_INCONTROL_LEVEL;
    }

    @Override
    public int computeNextVal(boolean prediction) {
        // System.out.print(prediction + " " + m_n + " " + probability + " ");
        m_n++;
        if (prediction == false) {
            m_numErrors += 1;
            m_lastd = m_d;
            m_d = m_n - 1;
            int distance = m_d - m_lastd;
            double oldmean = m_mean;
            m_mean = m_mean + ((double) distance - m_mean) / m_numErrors;
            m_stdTemp = m_stdTemp + (distance - m_mean) * (distance - oldmean);
            double std = Math.sqrt(m_stdTemp / m_numErrors);
            double m2s = m_mean + 2 * std;

            // System.out.print(m_numErrors + " " + m_mean + " " + std + " " +
            // m2s + " " + m_m2smax + " ");

            if (m2s > m_m2smax) {
                if (m_n > FDDM_MINNUMINSTANCES) {
                    m_m2smax = m2s;
                }
                m_lastLevel = DDM_INCONTROL_LEVEL;
                // System.out.print(1 + " ");
            } else {
                double p = m2s / m_m2smax;
                // System.out.print(p + " ");
                if (m_n > FDDM_MINNUMINSTANCES && m_numErrors > m_minNumErrors
                        && p < FDDM_OUTCONTROL) {
                    System.out.println(m_mean + ",D");
                    initialize();
                    return DDM_OUTCONTROL_LEVEL;
                } else if (m_n > FDDM_MINNUMINSTANCES
                        && m_numErrors > m_minNumErrors && p < FDDM_WARNING) {
                    System.out.println(m_mean + ",W");
                    m_lastLevel = DDM_WARNING_LEVEL;
                    return DDM_WARNING_LEVEL;
                } else {
                    System.out.println(m_mean + ",N");
                    m_lastLevel = DDM_INCONTROL_LEVEL;
                    return DDM_INCONTROL_LEVEL;
                }
            }
        } else {
            // System.out.print(m_numErrors + " " + m_mean + " " +
            // Math.sqrt(m_stdTemp/m_numErrors) + " " + (m_mean +
            // 2*Math.sqrt(m_stdTemp/m_numErrors)) + " " + m_m2smax + " ");
            // System.out.print(((m_mean +
            // 2*Math.sqrt(m_stdTemp/m_numErrors))/m_m2smax) + " ");
        }
        return m_lastLevel;
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

    @Override
    public DriftDetectionMethod copy() {
        return (DriftDetectionMethod) super.copy();
    }
}