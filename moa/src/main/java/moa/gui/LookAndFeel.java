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
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.gui;

import com.jidesoft.plaf.LookAndFeelFactory;

import javax.swing.UIManager;

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

    /** the LnF for JIDE property in the GUI defaults. */
    public static final String KEY_JIDELOOKANDFEEL = "JideLookAndFeel";

    /** the Windows LnF classname. */
    public static final String WINDOWS_LNF = LookAndFeelFactory.WINDOWS_LNF;

    /** the cross-platform LnF classname. */
    public static final String CROSSPLATFORM_LNF = LookAndFeelFactory.METAL_LNF;

    /** for using the system's default LnF. */
    public static final String VALUE_SYSTEM = "system";

    /** for using the cross-platform LnF (= metal). */
    public static final String VALUE_CROSSPLATFORM = "crossplatform";

    /**
     * Installs the look and feel.
     */
    public static void install() {
        String lnf = GUIDefaults.get(KEY_LOOKANDFEEL, "").trim();
        boolean success = true;

        switch (lnf) {
            case "":
                System.err.println("Using built-in strategy for setting Look'n'Feel...");
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (Throwable t) {
                    try {
                        UIManager.setLookAndFeel(WINDOWS_LNF);
                    }
                    catch (Throwable th) {
                        success = false;
                    }
                }
                break;

            case VALUE_SYSTEM:
                System.err.println("Using system Look'n'Feel...");
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (Throwable t) {
                    success = false;
                }
                break;

            case VALUE_CROSSPLATFORM:
                System.err.println("Using cross-platform Look'n'Feel...");
                try {
                    UIManager.setLookAndFeel(CROSSPLATFORM_LNF);
                }
                catch (Throwable t) {
                    success = false;
                }
                break;

            default:
                System.err.println("Using custom Look'n'Feel class: " + lnf);
                try {
                    UIManager.setLookAndFeel(lnf);
                }
                catch (Throwable t) {
                    System.err.println("Failed to instantiate Look'n'Feel class: " + lnf);
                    success = false;
                }
                break;
        }

        // fall back on metal LnF
        if (!success) {
            System.err.println("Falling back on cross-platform Look'n'Feel...");
            try {
                UIManager.setLookAndFeel(CROSSPLATFORM_LNF);
            }
            catch (Throwable t) {
                System.err.println("Failed to set cross-platform Look'n'Feel (" + CROSSPLATFORM_LNF + "), which should always succeed!");
                // ignored
            }
        }

        // configuring JIDE
        lnf = GUIDefaults.get(KEY_JIDELOOKANDFEEL, "").trim();
        if (lnf.isEmpty()) {
            System.err.println("Using built-in strategy for setting JIDE Look'n'Feel...");
            try {
                LookAndFeelFactory.installJideExtension();
            }
            catch (Throwable t) {
                System.err.println("Failed to install JIDE Look'n'Feel based on built-in strategy, attempting to use style: " + LookAndFeelFactory.VSNET_STYLE_WITHOUT_MENU);
                LookAndFeelFactory.installJideExtension(LookAndFeelFactory.VSNET_STYLE_WITHOUT_MENU);
            }
        }
        else {
            try {
                int style = Integer.parseInt(lnf);
                System.err.println("Using JIDE Look'n'Feel style: " + style);
                LookAndFeelFactory.installJideExtension(style);
            }
            catch (Throwable t) {
                System.err.println("JIDE Look'n'Feel must be an integer, found: " + lnf);
                System.err.println("Attempting to use JIDE Look'n'Feel style: " + LookAndFeelFactory.VSNET_STYLE_WITHOUT_MENU);
                LookAndFeelFactory.installJideExtension(LookAndFeelFactory.VSNET_STYLE_WITHOUT_MENU);
            }
        }
    }
}
