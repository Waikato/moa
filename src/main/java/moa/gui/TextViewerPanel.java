/*
 *    TextViewerPanel.java
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
package moa.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * This panel displays text. Used to output the results of tasks.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class TextViewerPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public static String exportFileExtension = "txt";

    protected JTextArea textArea;

    protected JScrollPane scrollPane;

    protected JButton exportButton;

    public TextViewerPanel() {
        this.textArea = new JTextArea();
        this.textArea.setEditable(false);
        this.textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.exportButton = new JButton("Export as .txt file...");
        this.exportButton.setEnabled(false);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2));
        buttonPanel.add(this.exportButton);
        setLayout(new BorderLayout());
        this.scrollPane = new JScrollPane(this.textArea);
        add(this.scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        this.exportButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setAcceptAllFileFilterUsed(true);
                fileChooser.addChoosableFileFilter(new FileExtensionFilter(
                        exportFileExtension));
                if (fileChooser.showSaveDialog(TextViewerPanel.this) == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = fileChooser.getSelectedFile();
                    String fileName = chosenFile.getPath();
                    if (!chosenFile.exists()
                            && !fileName.endsWith(exportFileExtension)) {
                        fileName = fileName + "." + exportFileExtension;
                    }
                    try {
                        PrintWriter out = new PrintWriter(new BufferedWriter(
                                new FileWriter(fileName)));
                        out.write(TextViewerPanel.this.textArea.getText());
                        out.close();
                    } catch (IOException ioe) {
                        GUIUtils.showExceptionDialog(
                                TextViewerPanel.this.exportButton,
                                "Problem saving file " + fileName, ioe);
                    }
                }
            }
        });
    }

    public void setText(String newText) {
        Point p = this.scrollPane.getViewport().getViewPosition();
        this.textArea.setText(newText);
        this.scrollPane.getViewport().setViewPosition(p);
        this.exportButton.setEnabled(newText != null);
    }

    public void addText(String newText) {
        String text = textArea.getText();
        text += (!text.isEmpty()) ? "\n" : "";
        text += newText;
        setText(text);
    }
}
