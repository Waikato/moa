/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * LookAndFeel.java
 * Copyright (C) 2019-2022 University of Waikato, Hamilton, NZ
 */

package moa.gui;

import javax.swing.UIManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages setting the look and feel.
 * Uses the {@link #KEY_LOOKANDFEEL} property from {@link GUIDefaults} to
 * determine what Look'n'Feel to use. See examples in the GUI.props file
 * for more details.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class LookAndFeel {

    /** the LnF property in the GUI defaults. */
    public static final String KEY_LOOKANDFEEL = "LookAndFeel";

    /** the Windows LnF classname. */
    public static final String WINDOWS_LNF = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

    /** the cross-platform LnF classname. */
    public static final String CROSSPLATFORM_LNF = "javax.swing.plaf.metal.MetalLookAndFeel";

    /** for using the system's default LnF. */
    public static final String VALUE_SYSTEM = "system";

    /** for using the cross-platform LnF (= metal). */
    public static final String VALUE_CROSSPLATFORM = "crossplatform";

    /** for logging output. */
    protected static Logger LOGGER;
    static {
        LOGGER = Logger.getLogger(LookAndFeel.class.getName());
        LOGGER.setLevel(Level.INFO);
    }


    /**
     * Attempts to install the specified Java Look'n'Feel.
     *
     * @param lnf the look'n'feel classname
     * @return true if successfully installed
     */
    protected static boolean installJavaLookAndFeel(String lnf) {
        boolean result = true;
        switch (lnf) {
            case "":
                LOGGER.info("Using built-in strategy for setting Look'n'Feel...");
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (Throwable t) {
                    try {
                        UIManager.setLookAndFeel(WINDOWS_LNF);
                    }
                    catch (Throwable th) {
                        result = false;
                    }
                }
                break;

            case VALUE_SYSTEM:
                LOGGER.info("Using system Look'n'Feel...");
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (Throwable t) {
                    result = false;
                }
                break;

            case VALUE_CROSSPLATFORM:
                LOGGER.info("Using cross-platform Look'n'Feel...");
                try {
                    UIManager.setLookAndFeel(CROSSPLATFORM_LNF);
                }
                catch (Throwable t) {
                    result = false;
                }
                break;

            default:
                LOGGER.info("Using Look'n'Feel class: " + lnf);
                try {
                    UIManager.setLookAndFeel(lnf);
                }
                catch (Throwable t) {
                    LOGGER.severe("Failed to instantiate Look'n'Feel class: " + lnf);
                    result = false;
                }
                break;
        }
        return result;
    }

    /**
     * Attempts to install the specified Look'n'Feel, but falls back on
     * cross-platform look if it fails.
     *
     * @param lnf the look'n'feel classname
     * @return true if successful
     */
    protected static boolean attemptInstallJavaLookAndFeel(String lnf) {
        boolean result;
        result = installJavaLookAndFeel(lnf);
        if (!result) {
            LOGGER.info("Falling back on cross-platform Look'n'Feel...");
            result = installJavaLookAndFeel(CROSSPLATFORM_LNF);
            if (!result)
                LOGGER.severe("Failed to set cross-platform Look'n'Feel (" + CROSSPLATFORM_LNF + "), which should always succeed!");
        }
        return result;
    }

    /**
     * Installs the look and feel.
     */
    public static void install() {
        String lnf;
        boolean success;

        // Java
        lnf     = GUIDefaults.get(KEY_LOOKANDFEEL, "").trim();
        success = attemptInstallJavaLookAndFeel(lnf);
        LOGGER.info("Setting Java Look'n'Feel: " + success);
    }
}
