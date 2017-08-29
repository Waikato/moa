/*
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

/*
 * GUIDefaults.java
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */
package moa.gui;

import moa.core.PropertiesReader;
import moa.core.Utils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * This class offers get methods for the default GUI settings in 
 * the props file <code>moa/gui/GUI.props</code>.
 *
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 6103 $
 */
public class GUIDefaults
        implements Serializable {

    /** for serialization. */
    private static final long serialVersionUID = 4954795757927524225L;

    /** The name of the properties file. */
    public final static String PROPERTY_FILE = "moa/gui/GUI.props";

    /** Properties associated with the GUI options. */
    protected static Properties PROPERTIES;

    static {
        try {
            PROPERTIES = PropertiesReader.readProperties(PROPERTY_FILE);
        } catch (Exception e) {
            System.err.println("Problem reading properties. Fix before continuing.");
            e.printStackTrace();
            PROPERTIES = new Properties();
        }
    }

    /**
     * returns the value for the specified property, if non-existent then the
     * default value.
     *
     * @param property      the property to retrieve the value for
     * @param defaultValue  the default value for the property
     * @return              the value of the specified property
     */
    public static String get(String property, String defaultValue) {
        return PROPERTIES.getProperty(property, defaultValue);
    }

    /**
     * returns the associated properties file.
     *
     * @return              the props file
     */
    public final static Properties getProperties() {
        return PROPERTIES;
    }

    /**
     * Tries to instantiate the class stored for this property, optional
     * options will be set as well. Returns null if unsuccessful.
     *
     * @param property      the property to get the object for
     * @param defaultValue  the default object spec string
     * @return              if successful the fully configured object, null
     *                      otherwise
     */
    protected static Object getObject(String property, String defaultValue) {
        return getObject(property, defaultValue, Object.class);
    }

    /**
     * Tries to instantiate the class stored for this property, optional
     * options will be set as well. Returns null if unsuccessful.
     *
     * @param property      the property to get the object for
     * @param defaultValue  the default object spec string
     * @param cls           the class the object must be derived from
     * @return              if successful the fully configured object, null
     *                      otherwise
     */
    protected static Object getObject(String property, String defaultValue, Class cls) {
        Object result;
        String tmpStr;
        String[] tmpOptions;

        result = null;

        try {
            tmpStr = get(property, defaultValue);
            tmpOptions = Utils.splitOptions(tmpStr);
            if (tmpOptions.length != 0) {
                tmpStr = tmpOptions[0];
                tmpOptions[0] = "";
                result = Utils.forName(cls, tmpStr, tmpOptions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }

        return result;
    }

    /**
     * returns an array with the classnames of all the additional panels to
     * display as tabs in the GUI.
     *
     * @return		the classnames
     */
    public static String[] getTabs() {
        String[] result;
        String tabs;

        // read and split on comma
        tabs = get("Tabs", "moa.gui.ClassificationTabPanel,moa.gui.RegressionTabPanel,moa.gui.MultiLabelTabPanel,moa.gui.MultiTargetTabPanel,moa.gui.clustertab.ClusteringTabPanel,moa.gui.outliertab.OutlierTabPanel,moa.gui.ConceptDriftTabPanel,moa.gui.AuxiliarTabPanel");
        result = tabs.split(",");

        return result;
    }

    /**
     * Returns the initial directory for the file chooser used for opening
     * datasets.
     * <p/>
     * The following placeholders are recognized:
     * <pre>
     *   %t - the temp directory
     *   %h - the user's home directory
     *   %c - the current directory
     *   %% - gets replaced by a single percentage sign
     * </pre>
     *
     * @return		the default directory
     */
    public static String getInitialDirectory() {
        String result;

        result = get("InitialDirectory", "%c");
        result = result.replaceAll("%t", System.getProperty("java.io.tmpdir"));
        result = result.replaceAll("%h", System.getProperty("user.home"));
        result = result.replaceAll("%c", System.getProperty("user.dir"));
        result = result.replaceAll("%%", System.getProperty("%"));

        return result;
    }

    /**
     * only for testing - prints the content of the props file.
     *
     * @param args	commandline parameters - ignored
     */
    public static void main(String[] args) {
        Enumeration names;
        String name;
        Vector sorted;

        System.out.println("\nMOA defaults:");
        names = PROPERTIES.propertyNames();

        // sort names
        sorted = new Vector();
        while (names.hasMoreElements()) {
            sorted.add(names.nextElement());
        }
        Collections.sort(sorted);
        names = sorted.elements();

        // output
        while (names.hasMoreElements()) {
            name = names.nextElement().toString();
            System.out.println("- " + name + ": " + PROPERTIES.getProperty(name, ""));
        }
        System.out.println();
    }
}
