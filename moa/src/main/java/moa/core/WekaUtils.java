/*
 *    WekaUtils.java
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
package moa.core;

import weka.core.Version;

/**
 * Class implementing some Weka utility methods.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class WekaUtils {

 /**
     * Checks if the Weka version is recent enough to run MOA.
     * For example, if the Weka version is not recent, there may be problems
     * due to the fact that <code>Instance</code> was a class before 3.7.1 and
     * now is an interface.
     *
     * @return true if the Weka version is recent.
     */
    public static boolean isWekaVersionOK() {
        try {
            Class.forName("weka.core.Version");
            Version version = new Version();
            if (version.isOlder("3.7.1")) {
                System.err.println();
                System.err.println(Globals.getWorkbenchInfoString());
                System.err.println();
                System.err.print("Weka 3.7.1 or higher is required to run MOA. ");
                System.err.println("Weka version " + Version.VERSION + " found");
                return false;
            } else {
                return true;
            }
        } catch (ClassNotFoundException exception) {
            // It is not available
            return true;
        }
    }
}

