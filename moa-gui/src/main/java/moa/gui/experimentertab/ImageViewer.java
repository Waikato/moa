/*
 *    ImageViewer.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand 
 *    @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
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
package moa.gui.experimentertab;

import nz.ac.waikato.cms.gui.core.SimpleDirectoryChooser;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;

/**
 * This class creates a window where images generated with JFreeChart are
 * displayed.
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class ImageViewer extends JFrame {

    private ImageTreePanel imgPanel;

    private String resultsPath;

    private JButton btn;

    private JComboBox imgType;

    /**
     * Class constructor.
     *
     * @param imgPanel
     * @param resultsPath
     * @throws HeadlessException
     */
    public ImageViewer(ImageTreePanel imgPanel, String resultsPath) throws HeadlessException {
        super("Preview");
        this.imgPanel = imgPanel;
        this.resultsPath = resultsPath;
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        // Create and set up the content pane.
        JPanel panel = new JPanel();
        JPanel main = new JPanel();
        JLabel label = new JLabel("Output format");
        String op[] = {"PNG", "JPG", "SVG"};
        imgType = new JComboBox(op);
        imgType.setSelectedIndex(0);
        btn = new JButton("Save all images as...");
        btn.addActionListener(this::btnMenuActionPerformed);
        panel.add(label);
        panel.add(imgType);
        panel.add(btn);

        main.setLayout(new BorderLayout());
        main.add(this.imgPanel, BorderLayout.CENTER);
        main.add(panel, BorderLayout.SOUTH);

        setContentPane(main);

        // Display the window.
        pack();
        setSize(700, 500);

        setVisible(true);
    }

    private void btnMenuActionPerformed(java.awt.event.ActionEvent evt) {

        String path = "";
        SimpleDirectoryChooser propDir = new SimpleDirectoryChooser();
        propDir.setCurrentDirectory(new File(resultsPath));
        int selection = propDir.showSaveDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            path = propDir.getSelectedFile().getAbsolutePath();
            if (!path.equals("")) {
                for (ImageChart chart : this.imgPanel.getChart()) {
                    try {
                        chart.exportIMG(path, this.imgType.getSelectedItem().toString());

                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Error creating image " + chart.getName());
                        return;
                    }

                }
                JOptionPane.showMessageDialog(this, "Images saved at: " + path);
            }
        }

    }
}
