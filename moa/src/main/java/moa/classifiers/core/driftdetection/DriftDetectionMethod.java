/*
 *    DriftDetectionMethod.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Manuel Martín (manuelmartin@decsai.ugr.es)
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

import moa.options.OptionHandler;

/**
 * Drift detection interface to implement methods that detects change.
 *
 * @author Manuel Martín (manuelmartin@decsai.ugr.es)
 * @version $Revision: 7 $
 */
public interface DriftDetectionMethod extends OptionHandler {

    public static final int DDM_INCONTROL_LEVEL = 0;

    public static final int DDM_WARNING_LEVEL = 1;

    public static final int DDM_OUTCONTROL_LEVEL = 2;

    public int computeNextVal(boolean prediction);

    public void getDescription(StringBuilder sb, int indent);
    
     /**
     * Produces a copy of this drift detection method
     *
     * @return the copy of this drift detection method
     */
    public DriftDetectionMethod copy();
    
}