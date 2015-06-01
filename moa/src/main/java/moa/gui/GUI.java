/*
 *    GUI.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *    @author FracPete (fracpete at waikato dot ac dot nz)
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
package moa.gui;

import moa.DoTask;
import moa.core.WekaUtils;

import javax.swing.*;
import java.awt.*;

/**
 * The main class for the MOA gui. Lets the user configure
 * tasks, learners, streams, and perform data stream analysis.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class GUI extends JTabbedPane {

    final static String AppTitle = "Massive Online Analysis - MOA GUI";
    private static final long serialVersionUID = 1L;

    private JPanel optionPanel;

    public GUI() {
        super();

        // Create and set up tabs


        // initialize additional panels
        String[] tabs = GUIDefaults.getTabs();
        for (int i = 0; i < tabs.length; i++) {
            try {
                // determine classname
                String[] optionsStr = tabs[i].split(":");
                String classname = optionsStr[0];
                // setup panel
                AbstractTabPanel tabPanel = (AbstractTabPanel) Class.forName(classname).newInstance();
                addTab(tabPanel.getTabTitle(), null, tabPanel, tabPanel.getDescription());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }



    public static void main(String[] args) {
        try {
            if (DoTask.isJavaVersionOK() == false || WekaUtils.isWekaVersionOK() == false) {
                return;
            }
            javax.swing.SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {

                    // Create and set up the window.
                    JFrame frame = new JFrame(AppTitle);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception e) {
                        try {
                            javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                        } catch (Exception ex) {
                        }
                    
                    }

                    GUI gui = new GUI();
                    frame.getContentPane().setLayout(new BorderLayout());
                    frame.getContentPane().add(gui, BorderLayout.CENTER);

                    frame.pack(); // Display the window.
                    frame.setVisible(true);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
