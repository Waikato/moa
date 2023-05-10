/*
 *    SummaryTab.java
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

import moa.gui.LookAndFeel;
import nz.ac.waikato.cms.gui.core.SimpleDirectoryChooser;
import org.apache.commons.io.FilenameUtils;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Summarize the performance measurements of different learning algorithms over
 * time in LaTeX and HTML formats.
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class SummaryTab extends JPanel {

    private javax.swing.JButton jButtonResults;
    private javax.swing.JButton jButtonDelAlgoritm;
    private javax.swing.JButton jButtonDelStream;
    private javax.swing.JButton jButtonDelMeasure;
    private javax.swing.JButton jButtonDleteRow;
    private javax.swing.JButton jButtonSummarize;
    private javax.swing.JButton jButtonAddMeasure;
    private javax.swing.JButton jButtonShowSummary;
    private javax.swing.JPanel jPanelConfig;
    private javax.swing.JScrollPane jScrollPaneAlgorithms;
    private javax.swing.JScrollPane jScrollPaneStreams;
    private javax.swing.JScrollPane jScrollPaneMeasure;
    private javax.swing.JTable jTableAlgoritms;
    private javax.swing.JTable jTableStreams;
    private javax.swing.JTable jTableMeasures;
    private javax.swing.JTextField jTextFieldResultsPath;
    private javax.swing.JComboBox jComboBoxMeasure;
    private javax.swing.JComboBox option;
    private javax.swing.JLabel jLabelResults;
    private javax.swing.JLabel jLabelMeasure;
    private DefaultTableModel algoritmModel;
    private DefaultTableModel streamModel;
    private DefaultTableModel measureModel;
    public LinkedList<String> measures;
    private ArrayList<String> algotithmID;
    private LinkedList<String> algorithmNames;
    private LinkedList<String> streamNames;
    Summary summary;
    ReadFile rf;

    /**
     * SummaryTab Constructor
     */
    public SummaryTab() {
        initComponents();
        measures = new LinkedList<>();
        algotithmID = new ArrayList<>();
        algorithmNames = new LinkedList<>();
        streamNames    = new LinkedList<>();    
        this.algoritmModel = (DefaultTableModel) jTableAlgoritms.getModel();
        this.streamModel = (DefaultTableModel) jTableStreams.getModel();
        this.measureModel = (DefaultTableModel) jTableMeasures.getModel();
        TableColumn col = jTableMeasures.getColumnModel().getColumn(2);
        String op[] = {"Mean", "Last Value"};
        option = new JComboBox(op);
        option.setSelectedIndex(0);
        col.setCellEditor(new DefaultCellEditor(option));
        TableColumn col1 = jTableMeasures.getColumnModel().getColumn(0);
    }

    private void initComponents() {
        jPanelConfig = new javax.swing.JPanel();
        jScrollPaneAlgorithms = new javax.swing.JScrollPane();
        jTableAlgoritms = new javax.swing.JTable();
        jScrollPaneStreams = new javax.swing.JScrollPane();
        jTableStreams = new javax.swing.JTable();
        jTextFieldResultsPath = new javax.swing.JTextField();
        jButtonResults = new javax.swing.JButton();
        jButtonAddMeasure = new javax.swing.JButton();
        jButtonShowSummary = new javax.swing.JButton();
        jScrollPaneMeasure = new javax.swing.JScrollPane();
        jTableMeasures = new javax.swing.JTable();
        jButtonDelAlgoritm = new javax.swing.JButton();
        jButtonDelStream = new javax.swing.JButton();
        jButtonDleteRow = new javax.swing.JButton();
        jButtonSummarize = new javax.swing.JButton();
        jButtonDelMeasure = new javax.swing.JButton();
        jLabelResults = new javax.swing.JLabel();
        jLabelMeasure = new javax.swing.JLabel();
        jComboBoxMeasure = new javax.swing.JComboBox();

        jPanelConfig.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Configure", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12))); // NOI18N

        jScrollPaneAlgorithms.setBorder(javax.swing.BorderFactory.createTitledBorder("Algorithms"));

        jTableAlgoritms.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{
                    "Algorithm", "Algorithm ID"
                }
        ));
        jTableAlgoritms.setEditingColumn(1);
        jScrollPaneAlgorithms.setViewportView(jTableAlgoritms);

        jScrollPaneStreams.setBorder(javax.swing.BorderFactory.createTitledBorder("Streams"));

        jTableStreams.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{
                    "Stream", "Stream ID"
                }
        ));
        jScrollPaneStreams.setViewportView(jTableStreams);

        jTextFieldResultsPath.setEditable(true);
        jLabelResults.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelResults.setLabelFor(jTextFieldResultsPath);
        jLabelResults.setText("Result folder");
        jButtonResults.setText("Browse");
        jButtonResults.addActionListener(this::jButtonResultsActionPerformed);

        jButtonAddMeasure.setText("Add Measure");
        jButtonAddMeasure.setToolTipText("Add Measure");
        jButtonAddMeasure.addActionListener((java.awt.event.ActionEvent evt) -> {
            // jButtonRunActionPerformed(evt);
        });

        jTableMeasures.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{
                    "Measure", "Measure ID", "Option"
                }
        ));
       
        jScrollPaneMeasure.setBorder(javax.swing.BorderFactory.createTitledBorder("Performance measures"));
        jScrollPaneMeasure.setViewportView(jTableMeasures);

        jButtonDelAlgoritm.setText("Delete Algorithm");
        jButtonDelAlgoritm.addActionListener(this::jButtonDelAlgoritmActionPerformed);

        jButtonDelStream.setText("Delete Stream");
        jButtonDelStream.addActionListener(this::jButtonDelStreamActionPerformed);

        jButtonDelMeasure.setText("Delete Measure");
        jButtonDelMeasure.addActionListener(this::jButtonDelMeasureActionPerformed);

        jButtonSummarize.setText("Export Summary");
        jButtonSummarize.setEnabled(false);
        jButtonSummarize.addActionListener(this::jButtonSummaryActionPerformed);

        jButtonShowSummary.setText("Show Summary");
        jButtonShowSummary.setEnabled(true);
        jButtonShowSummary.addActionListener(this::jButtonShowSummaryActionPerformed);

        jLabelMeasure.setText("Measures");
        jComboBoxMeasure.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"--Slect--"}));
        jButtonAddMeasure.addActionListener(this::jButtonAddMeasureActionPerformed);
        /*prueba*/
        JPanel jPanel1 = new JPanel();
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Configuration"));
        JLabel jLabelDirectory = new JLabel("Result folder");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addGap(14, 14, 14)
                                        .addComponent(jLabelDirectory)
                                        .addGap(18, 18, 18)
                                        .addComponent(jTextFieldResultsPath)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jButtonResults))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jButtonDelAlgoritm)
                                                .addComponent(jScrollPaneAlgorithms, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jScrollPaneStreams, javax.swing.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                        .addComponent(jButtonDelStream)
                                                        .addGap(0, 0, Short.MAX_VALUE)))))
                        .addGap(16, 16, 16))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextFieldResultsPath, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButtonResults)
                                .addComponent(jLabelDirectory))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jScrollPaneAlgorithms, javax.swing.GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
                                .addComponent(jScrollPaneStreams, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButtonDelStream)
                                .addComponent(jButtonDelAlgoritm)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 741, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 443, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()))
        );
        /*fin prueba*/
        JPanel panelMeasure = new JPanel();
        // JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        panelMeasure.setLayout(new BorderLayout());
        panelMeasure.add(jLabelMeasure, BorderLayout.WEST);
        panelMeasure.add(jComboBoxMeasure, BorderLayout.CENTER);
        panelMeasure.add(jButtonAddMeasure, BorderLayout.EAST);
        //Configure task table panel
        JPanel panelMeasuresTable = new JPanel();
        JPanel panelTaskTableBtn = new JPanel();
        panelTaskTableBtn.add(jButtonShowSummary);
        //panelTaskTableBtn.add(jButtonSummarize);
        panelTaskTableBtn.add(jButtonDelMeasure);
        panelMeasuresTable.setLayout(new BorderLayout());
        panelMeasuresTable.add(panelMeasure, BorderLayout.NORTH);
        panelMeasuresTable.add(jScrollPaneMeasure, BorderLayout.CENTER);
        panelMeasuresTable.add(panelTaskTableBtn, BorderLayout.SOUTH);
        // panelMeasuresTable.setPreferredSize(new Dimension(200,200));
        //splitPane.setTopComponent(jPanel1);
        //  splitPane.setBottomComponent(panelMeasuresTable);
        //splitPane.setDividerLocation(100);
        this.setLayout(new BorderLayout());
        this.add(jPanel1, BorderLayout.NORTH);
        this.add(panelMeasuresTable, BorderLayout.CENTER);
        // this.add(splitPane);
    }

    private void jButtonDelAlgoritmActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.jTableAlgoritms.getSelectedRow() != -1) {
            
            this.algoritmModel.removeRow(this.jTableAlgoritms.getSelectedRow());
            String algorithms[] = new String[algoritmModel.getRowCount()];
            for (int i = 0; i < algoritmModel.getRowCount(); i++) {
                algorithms[i] = algoritmModel.getValueAt(i, 0).toString();
            }
            
            if (streamModel.getValueAt(0, 0).toString() != null) {
                rf.updateMeasures(algorithms, streamModel.getValueAt(0, 0).toString());
                this.measures = rf.getMeasures();
                String measuresNames[] = measures.getFirst().split(",");
                jComboBoxMeasure.removeAllItems();
                for (String measuresName : measuresNames) {
                    jComboBoxMeasure.addItem(measuresName);
                }
            }
        }

    }

    private void jButtonDelStreamActionPerformed(java.awt.event.ActionEvent evt) {
        this.streamModel.removeRow(this.jTableStreams.getSelectedRow());
    }

    private void jButtonDelMeasureActionPerformed(java.awt.event.ActionEvent evt) {
        this.measureModel.removeRow(this.jTableMeasures.getSelectedRow());
    }

    private void jButtonAddMeasureActionPerformed(java.awt.event.ActionEvent evt) {
        for (int i = 0; i < this.measureModel.getRowCount(); i++) {
            if (jComboBoxMeasure.getSelectedItem().equals(this.measureModel.getValueAt(i, 0))) {
                JOptionPane.showMessageDialog(this, "The value exist",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (jComboBoxMeasure.getSelectedItem().equals("--Slect--")) {
                JOptionPane.showMessageDialog(this, "There are not values",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        this.measureModel.addRow(new Object[]{jComboBoxMeasure.getSelectedItem(),
            jComboBoxMeasure.getSelectedItem(), "Mean"});

    }

    private void jButtonShowSummaryActionPerformed(java.awt.event.ActionEvent evt) {

        if (this.jTextFieldResultsPath.getText().equals("")) {
            JOptionPane.showMessageDialog(this, "Directory not found",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Measure> algmeasures = new ArrayList<>();
        List<Stream> streams = new ArrayList<>();
        List<String> algPath = new ArrayList<>();
        List<String> algShortNames = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < this.measureModel.getRowCount(); i++) {
            for (int j = 0; j < this.jComboBoxMeasure.getItemCount(); j++) {
                if (this.measureModel.getValueAt(i, 0).equals(this.jComboBoxMeasure.getItemAt(j))) {
                    count++;
                }
                if (this.measureModel.getValueAt(i, 0).equals("") || this.measureModel.getValueAt(i, 1).equals("")
                        || this.measureModel.getValueAt(i, 2) == null) {
                    JOptionPane.showMessageDialog(this, "There are fields incompleted in Table",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

            }
        }
        boolean type = true;
        for (int k = 0; k < this.measureModel.getRowCount(); k++) {
            type = this.measureModel.getValueAt(k, 2).equals("Mean");
            Measure m = new Measure(this.measureModel.getValueAt(k, 1).toString(),this.measureModel.getValueAt(k, 0).toString(), type,0);
            algmeasures.add(m);

        }
        String path = this.jTextFieldResultsPath.getText();
        for (int i = 0; i < streamModel.getRowCount(); i++) {
           
            algPath.clear();
            for (int j = 0; j < algoritmModel.getRowCount(); j++) {
                File inputFile = new File(FilenameUtils.separatorsToSystem(
                        path + "\\" + streamModel.getValueAt(i, 0) + "\\" + algoritmModel.getValueAt(j, 0)));
                File streamFile = new File(FilenameUtils.separatorsToSystem(
                        path + "\\" + streamModel.getValueAt(i, 0)));
                if (!inputFile.exists()) {
                    JOptionPane.showMessageDialog(this, "File not found: "
                            + inputFile.getAbsolutePath(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    String algorithmPath = FilenameUtils.separatorsToSystem(
                            path + "\\" + streamModel.getValueAt(i, 0).toString() + "\\"
                            + algoritmModel.getValueAt(j, 0).toString());
                    algPath.add(algorithmPath);
                    if (i == 0) {
                        algShortNames.add(algoritmModel.getValueAt(j, 1).toString());
                    }
                   
                }
            }
            Stream s = new Stream(streamModel.getValueAt(i, 1).toString(), algPath, algShortNames, algmeasures);
            streams.add(s);
        }

        //create summary
        try {
            summary = new Summary(streams, FilenameUtils.separatorsToSystem(path + "\\"));
            jButtonSummarize.setEnabled(true);
            SummaryTable[] table = summary.showSummary();
            SummaryViewer summaryViewer = new SummaryViewer(table, summary,jTextFieldResultsPath.getText());

        } catch (Exception exc) {
            JOptionPane.showMessageDialog(this, "Problems generating summaries",
                    "Error", JOptionPane.ERROR_MESSAGE);

        }

    }

    private void jButtonSummaryActionPerformed(java.awt.event.ActionEvent evt) {

    }

    private void jButtonResultsActionPerformed(java.awt.event.ActionEvent evt) {
        SimpleDirectoryChooser resultsDir = new SimpleDirectoryChooser();
        int selection = -1;
        String path = "";
        selection = resultsDir.showOpenDialog(this);

        if (selection == JFileChooser.APPROVE_OPTION) {

            try {
                path = resultsDir.getSelectedFile().getAbsolutePath();
                readData(path);

            } catch (Exception exp) {
                JOptionPane.showMessageDialog(this, "Problem with path",
                        "Error", JOptionPane.ERROR_MESSAGE);

            }

        }

    }
    
    public void summaryCMD(String[] measures, String[] types){
         List<Measure> algmeasures = new ArrayList<>();
        List<Stream> streams = new ArrayList<>();
        List<String> algPath = new ArrayList<>();
        List<String> algShortNames = new ArrayList<>();
       
     
        boolean type = true;
        for (int k = 0; k < measures.length; k++) {
            type = types[k].equals("Mean");
            Measure m = new Measure(measures[k],measures[k], type,0);
            algmeasures.add(m);

        }
        String path = this.jTextFieldResultsPath.getText();
        for (int i = 0; i < streamModel.getRowCount(); i++) {
           
            algPath.clear();
            for (int j = 0; j < algoritmModel.getRowCount(); j++) {
                File inputFile = new File(FilenameUtils.separatorsToSystem(
                        path + "\\" + streamModel.getValueAt(i, 0) + "\\" + algoritmModel.getValueAt(j, 0)));
                File streamFile = new File(FilenameUtils.separatorsToSystem(
                        path + "\\" + streamModel.getValueAt(i, 0)));
                if (!inputFile.exists()) {
                    System.out.println("File not found: "+ inputFile.getAbsolutePath());
                            
                    return;
                } else {
                    String algorithmPath = FilenameUtils.separatorsToSystem(
                            path + "\\" + streamModel.getValueAt(i, 0).toString() + "\\"
                            + algoritmModel.getValueAt(j, 0).toString());
                    algPath.add(algorithmPath);
                    if (i == 0) {
                        algShortNames.add(algoritmModel.getValueAt(j, 1).toString());
                    }
                   
                }
            }
            Stream s = new Stream(streamModel.getValueAt(i, 1).toString(), algPath, algShortNames, algmeasures);
            streams.add(s);
        }

        //create summary
        try {
            summary = new Summary(streams, FilenameUtils.separatorsToSystem(path + File.separator));
            jButtonSummarize.setEnabled(true);
            String summaryPath = this.jTextFieldResultsPath.getText()+File.separator;
            
                    summary.invertedSumariesPerMeasure(summaryPath);
                    summary.computeWinsTiesLossesHTML(summaryPath);
                    summary.computeWinsTiesLossesLatex(summaryPath);
                    summary.generateHTML(summaryPath);
                    summary.generateLatex(summaryPath);
                   System.out.println("Summaries created at: " + summaryPath);

        } catch (Exception exc) {
           //  System.err.println("Problems generating summaries");
                    

        }

    }
    
    /**
     * Allows to read the results file and update the corresponding fields.
     *
     * @param path
     */
    public void readData(String path) {
        jTextFieldResultsPath.setText(path);
        cleanTables();
        rf = new ReadFile(path);
        String str = rf.processFiles();
        if (str.equals("")) {
             algotithmID = rf.getAlgShortNames();
             algorithmNames = rf.getAlgNames();
             streamNames = rf.getStream();
            this.measures = rf.getMeasures();
            //Set algorithms an streams
            for (int i = 0; i < algotithmID.size(); i++) {
                this.algoritmModel.addRow(new Object[]{algorithmNames.get(i), algotithmID.get(i)});
            }
            streamNames.stream().forEach((streamName) -> {
                this.streamModel.addRow(new Object[]{streamName, streamName});
            });

            // set the measures into the combobox
            String measuresNames[] = measures.getFirst().split(",");
            jComboBoxMeasure.removeAllItems();
            for (String measuresName : measuresNames) {
                jComboBoxMeasure.addItem(measuresName);
            }

        } else {

            JOptionPane.showMessageDialog(this, str,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

    /**
     * Clean the tables
     */
    public void cleanTables() {
        try {
            DefaultTableModel algModel = (DefaultTableModel) jTableAlgoritms.getModel();
            DefaultTableModel strModel = (DefaultTableModel) jTableStreams.getModel();
            DefaultTableModel measureModel = (DefaultTableModel) jTableMeasures.getModel();
            int rows = jTableAlgoritms.getRowCount();
            int srow = jTableStreams.getRowCount();
            int trow = this.jTableMeasures.getRowCount();
            for (int i = 0; i < rows; i++) {
                algModel.removeRow(0);
            }
            for (int i = 0; i < srow; i++) {
                strModel.removeRow(0);
            }
            for (int i = 0; i < trow; i++) {
                this.measureModel.removeRow(0);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error cleaning the table.");
        }
        jButtonSummarize.setEnabled(false);
    }

    private static void createAndShowGUI() {

        // Create and set up the window.
        JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane.
        JPanel panel = new SummaryTab();
        panel.setOpaque(true); // content panes must be opaque
        frame.setContentPane(panel);

        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * The main method
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            LookAndFeel.install();
            javax.swing.SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    createAndShowGUI();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
