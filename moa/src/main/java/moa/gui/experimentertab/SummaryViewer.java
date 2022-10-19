/*
 *    SummaryViewer.java
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.io.File;

/**
 * Class to display summaries in the gui.
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class SummaryViewer extends JFrame {

    public JTable tableSummary;
    public JScrollPane scroll;
    public JPanel jTablePanel;
    public JComboBox summaryType;
    public JButton bntExport;
    public SummaryTable[] summaryTable;
    public Summary summary;
    public String resultsPath;

    /**
     * Constructor.
     * @param summaryTable
     * @param summary
     * @throws HeadlessException
     */
    public SummaryViewer(SummaryTable[] summaryTable, Summary summary, String resultsPath) throws HeadlessException {
        super("Summary Viewer");

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.summaryTable = summaryTable;
        this.summary = summary;
        this.resultsPath = resultsPath;
        JLabel label = new JLabel("Summary");
        tableSummary = new JTable();
        String op[] = new String[this.summaryTable.length];
        for (int i = 0; i < this.summaryTable.length; i++) {
            op[i] = this.summaryTable[i].measureName;
        }
        tableSummary.setModel(new javax.swing.table.DefaultTableModel(
                this.summaryTable[0].value,
                this.summaryTable[0].algNames
        ));
        scroll = new JScrollPane();
        scroll.setViewportView(tableSummary);
        JPanel panel = new JPanel();
        JPanel main = new JPanel();
        jTablePanel = new JPanel();
        jTablePanel.setLayout(new GridLayout(1, 0));
        jTablePanel.add(scroll);
        summaryType = new JComboBox(op);
        summaryType.setSelectedIndex(0);
        bntExport = new JButton("Export Summaries");
      
        summaryType.addItemListener(this::summaryTypeItemStateChanged);
        bntExport.addActionListener(this::btnExportActionPerformed);
        panel.add(label);
        panel.add(summaryType);
        panel.add(bntExport);

        main.setLayout(new BorderLayout());
        main.add(this.jTablePanel, BorderLayout.CENTER);
        main.add(panel, BorderLayout.SOUTH);

        setContentPane(main);

        // Display the window.
        pack();
        setSize(700, 500);

        setVisible(true);
    }

   
    private void summaryTypeItemStateChanged(java.awt.event.ItemEvent evt) {                                                       
        for (SummaryTable summary1 : this.summaryTable) {
            if (summaryType.getSelectedItem().equals(summary1.measureName) == true) {
                tableSummary.setModel(new javax.swing.table.DefaultTableModel(summary1.value, summary1.algNames));
                break;
            }
        }
    }
     private void btnExportActionPerformed(java.awt.event.ActionEvent evt) {
        
           String path = "";
                SimpleDirectoryChooser propDir = new SimpleDirectoryChooser();
                propDir.setCurrentDirectory(new File(resultsPath));
                int selection = propDir.showSaveDialog(this);
                if (selection == JFileChooser.APPROVE_OPTION) {
                    path = propDir.getSelectedFile().getAbsolutePath();
                }
                if (!path.equals("")) {
                    path += File.separator;
                    summary.invertedSumariesPerMeasure(path);
                    summary.computeWinsTiesLossesHTML(path);
                    summary.computeWinsTiesLossesLatex(path);
                    summary.generateHTML(path);
                    summary.generateLatex(path);
                     JOptionPane.showMessageDialog(this, "Summaries created at: " + path,
                    "", JOptionPane.INFORMATION_MESSAGE);
                }
         
        
     }

}
