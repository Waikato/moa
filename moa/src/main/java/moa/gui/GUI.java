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

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import moa.DoTask;
import moa.core.WekaUtils;

/**
 * The main class for the MOA gui. Lets the user configure
 * tasks, learners, streams, and perform data stream analysis.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class GUI extends JPanel {

    private static final long serialVersionUID = 1L;

    private javax.swing.JTabbedPane panel;

    public GUI() {
        initGUI();
    }

    private void initGUI() {
        setLayout(new BorderLayout());

        // Create and set up tabs
        panel = new javax.swing.JTabbedPane();
        add(panel, BorderLayout.CENTER);

        // initialize additional panels
        String[] tabs = GUIDefaults.getTabs();
        for (int i = 0; i < tabs.length; i++) {
            try {
                // determine classname
                String[] optionsStr = tabs[i].split(":");
                String classname = optionsStr[0];
                // setup panel
                AbstractTabPanel tabPanel = (AbstractTabPanel) Class.forName(classname).newInstance();
                panel.addTab(tabPanel.getTabTitle(), null, (JPanel) tabPanel, tabPanel.getDescription());
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
                    JFrame frame = new JFrame("MOA Graphical User Interface");
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
                    frame.getContentPane().add(gui);

                    // Display the window.
                    frame.pack();
                    frame.setVisible(true);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
