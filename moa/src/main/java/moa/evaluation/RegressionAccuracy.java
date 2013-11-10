/*
 *    RegressionAccuracy.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
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
package moa.evaluation;

public class RegressionAccuracy extends Accuracy {

    @Override
    public String[] getNames() {
        String[] names = {"mean abs. error", "root mean sq. er.", "", "Ram-Hours", "Time", "Memory"};
        return names;
    }

    @Override
    protected boolean[] getDefaultEnabled() {
        boolean[] defaults = {true, true, false, true, true, true};
        return defaults;
    }

}
