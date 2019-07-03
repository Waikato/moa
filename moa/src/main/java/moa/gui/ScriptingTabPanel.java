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
 * ScriptingTabPanel.java
 * Copyright (C) 2018-2019 University of Waikato, Hamilton, NZ
 */

package moa.gui;

import com.github.fracpete.jshell.JShellPanel;

import java.awt.BorderLayout;
import java.util.Arrays;

/**
 * Tab for performing scripting via jshell. Requires Java 9.
 *
 * See https://docs.oracle.com/javase/9/jshell/
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class ScriptingTabPanel
  extends AbstractTabPanel {

    /** the panel to use. */
    protected JShellPanel m_PanelJShell;

    /**
     * Initializes the tab.
     */
    public ScriptingTabPanel() {
        super();
        setLayout(new BorderLayout());

        m_PanelJShell = new JShellPanel();
        String sizeOfAg = locateSizeOfAg();
        if (sizeOfAg != null)
            m_PanelJShell.setRemoteRuntimeFlags(Arrays.asList("-javaagent:" + sizeOfAg));
        add(m_PanelJShell, BorderLayout.CENTER);
    }

    /**
     * Locates the sizeofag jar in the classpath.
     *
     * @return the jar, null if not found
     */
    protected String locateSizeOfAg() {
        String      result;
        String[]      cp;

        result = null;
        cp     = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
        for (String part: cp) {
            if (part.toLowerCase().contains("sizeofag")) {
                result = part;
                break;
            }
        }

        return result;
    }

    /**
     * Returns the string to display as title of the tab.
     *
     * @return the string to display as title of the tab
     */
    @Override
    public String getTabTitle() {
        return "Scripting";
    }

    /**
     * Returns a short description (can be used as tool tip) of the tab, or contributor, etc.
     *
     * @return a short description of this tab panel.
     */
    @Override
    public String getDescription() {
        return "Offers scripting via jshell (Java 9+)";
    }
}
