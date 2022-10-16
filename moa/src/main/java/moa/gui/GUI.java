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
import nz.ac.waikato.cms.gui.core.BaseFlatSplitButton;
import nz.ac.waikato.cms.gui.core.GUIHelper;
import nz.ac.waikato.cms.gui.core.MultiPagePane;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The main class for the MOA gui. Lets the user configure
 * tasks, learners, streams, and perform data stream analysis.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class GUI extends JPanel {

    private static final long serialVersionUID = 1L;

    private MultiPagePane pagePane;

    public GUI() {
        initGUI();
    }

    private void initGUI() {
        setLayout(new BorderLayout());

        // Create and set up tabs
        pagePane = new MultiPagePane();
        pagePane.setMaxPageCloseUndo(GUIDefaults.getMaxTabUndo());
        add(pagePane, BorderLayout.CENTER);

        // sub-menu for adding tabs via action button
        BaseFlatSplitButton buttonAdd = new BaseFlatSplitButton(GUIHelper.getIcon("add.gif"));
        buttonAdd.setFont(getFont().deriveFont(Font.PLAIN));
        pagePane.getButtonPanel().add(buttonAdd);

        // the default tabs
        Set<String> defaultTabs = new HashSet<>(Arrays.asList(GUIDefaults.getDefaultTabs()));

        // initialize additional panels
        String[] tabs = GUIDefaults.getTabs();
        boolean first = true;
        for (int i = 0; i < tabs.length; i++) {
            try {
                // determine classname
                String[] optionsStr = tabs[i].split(":");
                final String classname = optionsStr[0];
                // setup panel
                AbstractTabPanel tabPanel = (AbstractTabPanel) Class.forName(classname).newInstance();
                if ((defaultTabs.size() == 0) || defaultTabs.contains(classname))
                    pagePane.addPage(tabPanel.getTabTitle(), tabPanel);

                // add tab
                Action action = new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            AbstractTabPanel tab = (AbstractTabPanel) Class.forName(classname).newInstance();
                            pagePane.addPage(tab.getTabTitle(), tab);
                        }
                        catch (Exception ex) {
                            // ignored
                        }
                    }
                };
                action.putValue(Action.NAME, tabPanel.getTabTitle());
                if (first) {
                    buttonAdd.setAction(action);
                    first = false;
                }
                else {
                    buttonAdd.add(action);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        pagePane.setSelectedIndex(0);
    }

    public static void main(String[] args) {
        try {
            if (!DoTask.isJavaVersionOK() || !WekaUtils.isWekaVersionOK()) {
                return;
            }
            javax.swing.SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {

                    // Create and set up the window.
                    JFrame frame = new JFrame("MOA Graphical User Interface");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    LookAndFeel.install();

                    GUI gui = new GUI();
                    frame.getContentPane().setLayout(new BorderLayout());
                    frame.getContentPane().add(gui);

                    // Display the window.
                    frame.setSize(GUIDefaults.getFrameWidth(), GUIDefaults.getFrameHeight());
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
