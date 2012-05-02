/*
 *    DDM.java
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
import moa.options.IntOption;
import moa.tasks.TaskMonitor;

/**
 * Drift detection method based in DDM method of Joao Gama SBIA 2004.
 *
 * <p>João Gama, Pedro Medas, Gladys Castillo, Pedro Pereira Rodrigues: Learning
 * with Drift Detection. SBIA 2004: 286-295 </p>
 *
 * @author Manuel Baena (mbaena@lcc.uma.es)
 * @version $Revision: 7 $
 */
public class DDM extends AbstractOptionHandler implements DriftDetectionMethod {

    private static final long serialVersionUID = -3518369648142099719L;

    public IntOption minNumInstancesOption = new IntOption(
            "minNumInstances",
            'n',
            "The minimum number of instances before permitting detecting change.",
            30, 0, Integer.MAX_VALUE);

    private int m_n;

    private double m_p;

    private double m_s;

    private double m_psmin;

    private double m_pmin;

    private double m_smin;

    public DDM() {
        initialize();
    }

    private void initialize() {
        m_n = 1;
        m_p = 1;
        m_s = 0;
        m_psmin = Double.MAX_VALUE;
        m_pmin = Double.MAX_VALUE;
        m_smin = Double.MAX_VALUE;
    }

    @Override
    public int computeNextVal(boolean prediction) {
        if (prediction == false) {
            m_p = m_p + (1.0 - m_p) / (double) m_n;
        } else {
            m_p = m_p - (m_p) / (double) m_n;
        }
        m_s = Math.sqrt(m_p * (1 - m_p) / (double) m_n);

        m_n++;

        // System.out.print(prediction + " " + m_n + " " + (m_p+m_s) + " ");

        if (m_n < minNumInstancesOption.getValue()) {
            return DDM_INCONTROL_LEVEL;
        }

        if (m_p + m_s <= m_psmin) {
            m_pmin = m_p;
            m_smin = m_s;
            m_psmin = m_p + m_s;
        }



        if (m_n > minNumInstancesOption.getValue() && m_p + m_s > m_pmin + 3 * m_smin) {
            System.out.println(m_p + ",D");
            initialize();
            return DDM_OUTCONTROL_LEVEL;
        } else if (m_p + m_s > m_pmin + 2 * m_smin) {
            System.out.println(m_p + ",W");
            return DDM_WARNING_LEVEL;
        } else {
            System.out.println(m_p + ",N");
            return DDM_INCONTROL_LEVEL;
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

    @Override
    public DriftDetectionMethod copy() {
        return (DriftDetectionMethod) super.copy();
    }
}