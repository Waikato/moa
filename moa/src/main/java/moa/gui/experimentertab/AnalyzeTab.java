/*
 *    AnalyzeTab.java
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
import moa.gui.experimentertab.statisticaltests.PValuePerTwoAlgorithm;
import moa.gui.experimentertab.statisticaltests.RankPerAlgorithm;
import moa.gui.experimentertab.statisticaltests.StatisticalTest;
import nz.ac.waikato.cms.gui.core.SimpleDirectoryChooser;
import org.apache.commons.io.FilenameUtils;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * In this class are compared online learning algorithms on multiple datasets by
 * performing appropriate statistical tests.
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class AnalyzeTab extends javax.swing.JPanel {

    private String algorithms[];
    ArrayList<RankPerAlgorithm> rank = null;
    ArrayList<PValuePerTwoAlgorithm> pvalues = null;
    private DefaultTableModel algoritmModel;
    private DefaultTableModel streamModel;
    private String path = "";
    private LinkedList<String> measures = new LinkedList();
    ReadFile rf;
    /**
     * Creates new form Analize
     */
    public AnalyzeTab() {
        initComponents();
        this.algoritmModel = (DefaultTableModel) jTableAlgoritms.getModel();
        this.streamModel = (DefaultTableModel) jTableStreams.getModel();
        jTextAreaOut.addMouseListener(new PopClickListener());
    }

    private static void createAndShowGUI() {

        // Create and set up the window.
        JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane.
        JPanel panel = new AnalyzeTab();
        panel.setOpaque(true); // content panes must be opaque
        frame.setContentPane(panel);

        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPaneAlgorithms = new javax.swing.JScrollPane();
        jTableAlgoritms = new javax.swing.JTable();
        jScrollPaneStreams = new javax.swing.JScrollPane();
        jTableStreams = new javax.swing.JTable();
        jTextFieldResultsPath = new javax.swing.JTextField();
        jButtonResults = new javax.swing.JButton();
        jLabelDirectory = new javax.swing.JLabel();
        jButtonDelAlgoritm = new javax.swing.JButton();
        jButtonDelStream = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jComboBoxTest = new javax.swing.JComboBox();
        jButtonTest = new javax.swing.JButton();
        jButtonImage = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jSpinnerPvalue = new javax.swing.JSpinner();
        jButtonReset = new javax.swing.JButton();
        jComboBoxMeasure = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jComboBoxType = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaOut = new javax.swing.JTextArea();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Configuration"));

        jScrollPaneAlgorithms.setBorder(javax.swing.BorderFactory.createTitledBorder("Algorithm"));

        jTableAlgoritms.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        jTableAlgoritms.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Algorithm", "Algorithm ID"
            }
        ));
        jScrollPaneAlgorithms.setViewportView(jTableAlgoritms);

        jScrollPaneStreams.setBorder(javax.swing.BorderFactory.createTitledBorder("Stram"));

        jTableStreams.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        jTableStreams.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Stream", "Stream ID"
            }
        ));
        jScrollPaneStreams.setViewportView(jTableStreams);

        jButtonResults.setText("Browse");
        jButtonResults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResultsActionPerformed(evt);
            }
        });

        jLabelDirectory.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelDirectory.setText("Result  folder");

        jButtonDelAlgoritm.setText("Delete Algorithm");
        jButtonDelAlgoritm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDelAlgoritmActionPerformed(evt);
            }
        });

        jButtonDelStream.setText("Delete Stream");
        jButtonDelStream.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDelStreamActionPerformed(evt);
            }
        });

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
                            .addComponent(jScrollPaneAlgorithms, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPaneStreams, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jButtonDelStream)
                                .addGap(0, 216, Short.MAX_VALUE)))))
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
                    .addComponent(jScrollPaneAlgorithms, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
                    .addComponent(jScrollPaneStreams, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonDelStream)
                    .addComponent(jButtonDelAlgoritm)))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Statistical Test"));

        jLabel2.setText("Test");

        jComboBoxTest.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Holm", "Shaffer", "Nemenyi" }));

        jButtonTest.setText("Run Test");
        jButtonTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTestActionPerformed(evt);
            }
        });

        jButtonImage.setText("Image");
        jButtonImage.setEnabled(false);
        jButtonImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonImageActionPerformed(evt);
            }
        });

        jLabel3.setText("pValue");

        jSpinnerPvalue.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(0.05d), null, null, Double.valueOf(0.001d)));

        jButtonReset.setText("Reset to Default");
        jButtonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResetActionPerformed(evt);
            }
        });

        jLabel4.setText("measure");

        jComboBoxType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Mean", "Last" }));

        jLabel1.setText("type");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBoxTest, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSpinnerPvalue)
                            .addComponent(jComboBoxMeasure, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(49, 49, 49)
                        .addComponent(jButtonTest)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonImage)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonReset)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBoxType, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxTest, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinnerPvalue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(6, 6, 6)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxMeasure, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonTest)
                    .addComponent(jButtonImage)
                    .addComponent(jButtonReset)))
        );

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Output"));

        jTextAreaOut.setColumns(20);
        jTextAreaOut.setRows(5);
        jScrollPane1.setViewportView(jTextAreaOut);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 682, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(289, 289, 289)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(300, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonResultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResultsActionPerformed
        SimpleDirectoryChooser resultsFile = new SimpleDirectoryChooser();
        int selection = resultsFile.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            path = resultsFile.getSelectedFile().getAbsolutePath();
        }

        if (!path.equals("")) {
            reset();
            this.jTextFieldResultsPath.setText(path);
            rf = new ReadFile(path);
            String str = rf.processFiles();
            if (str.equals("")) {

                int algSize = rf.getAlgShortNames().size();
                int streamSize = rf.getStream().size();
                this.measures = rf.getMeasures();
                for (int i = 0; i < algSize; i++) {
                    this.algoritmModel.addRow(new Object[]{rf.getAlgNames().get(i), rf.getAlgShortNames().get(i)});
                }
                for (int i = 0; i < streamSize; i++) {
                    this.streamModel.addRow(new Object[]{rf.getStream().get(i), rf.getStream().get(i)});
                }

                String measuresNames[] = measures.getFirst().split(",");
                for (String measuresName : measuresNames) {
                    jComboBoxMeasure.addItem(measuresName);
                    if (measuresName.equals("classifications correct (percent)") == true
                            || measuresName.equals("[avg] classifications correct (percent)") == true) {
                        jComboBoxMeasure.setSelectedItem(measuresName);
                    }

                }
            } else {

                JOptionPane.showMessageDialog(this, str,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jButtonResultsActionPerformed

    private void jButtonDelAlgoritmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDelAlgoritmActionPerformed
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
                    if (measuresName.equals("classifications correct (percent)") == true
                            || measuresName.equals("[avg] classifications correct (percent)") == true) {
                        jComboBoxMeasure.setSelectedItem(measuresName);
                    }

                }
            }
        }
    }//GEN-LAST:event_jButtonDelAlgoritmActionPerformed

    private void jButtonDelStreamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDelStreamActionPerformed
        this.streamModel.removeRow(this.jTableStreams.getSelectedRow());
    }//GEN-LAST:event_jButtonDelStreamActionPerformed

    private void jButtonTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTestActionPerformed
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
        boolean type = true;
        type = this.jComboBoxType.getSelectedItem().toString().equals("Mean");
        Measure m = new Measure(this.jComboBoxMeasure.getSelectedItem().toString(),
                this.jComboBoxMeasure.getSelectedItem().toString(),type, 0);
        algmeasures.add(m);
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

        //Statistical Test 
        StatisticalTest test = new StatisticalTest(streams);
        try {
            // test.readCSV(this.jTextFieldCSV.getText());
            test.readData();
        } catch (Exception exp) {
            JOptionPane.showMessageDialog(this, "Problem with csv file",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        test.avgPerformance();
        this.jTextAreaOut.append("P-values involving all algorithms\n");
        this.jTextAreaOut.append(System.getProperty("line.separator"));
        this.jTextAreaOut.append("P-value computed by Friedman Test: " + test.getFriedmanPValue() + "\n");
        this.jTextAreaOut.append("P-value computed by Iman and Daveport Test: " + test.getImanPValue() + "\n");

        rank = test.getRankAlg();
        this.jTextAreaOut.append(System.getProperty("line.separator"));
        this.jTextAreaOut.append("Ranking of the algorithms\n");
        this.jTextAreaOut.append(System.getProperty("line.separator"));
        rank.stream().forEach((RankPerAlgorithm rank1) -> {
            this.jTextAreaOut.append(rank1.algName + ": " + rank1.rank + "\n");
        });

        switch (jComboBoxTest.getSelectedItem().toString()) {
            case "Holm":
                pvalues = test.holmTest();
                break;
            case "Shaffer":
                pvalues = test.shafferTest();
                break;
            case "Nemenyi":
                pvalues = test.nemenyiTest();
        }
        this.jTextAreaOut.append(System.getProperty("line.separator"));
        this.jTextAreaOut.append("P-values of classifiers against each other\n");
        this.jTextAreaOut.append(System.getProperty("line.separator"));
        pvalues.stream().forEach((pvalue) -> {
            this.jTextAreaOut.append(pvalue.algName1 + " vs " + pvalue.algName2 + ": " + pvalue.PValue + "\n");
        });
        jButtonImage.setEnabled(true);
        //
    }//GEN-LAST:event_jButtonTestActionPerformed

    private void jButtonImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonImageActionPerformed
        RankingGraph graf = new RankingGraph(rank, pvalues, jTextFieldResultsPath.getText(), Double.parseDouble(this.jSpinnerPvalue.getValue().toString()));
    }//GEN-LAST:event_jButtonImageActionPerformed

    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetActionPerformed
        reset();
    }//GEN-LAST:event_jButtonResetActionPerformed

    /**
     * Tables of algorithms and datasets are cleaned.
     */
    public void cleanTables() {
        try {
            DefaultTableModel algModel = (DefaultTableModel) jTableAlgoritms.getModel();
            DefaultTableModel strModel = (DefaultTableModel) jTableStreams.getModel();
            int rows = jTableAlgoritms.getRowCount();
            int srow = jTableStreams.getRowCount();
            for (int i = 0; i < rows; i++) {
                algModel.removeRow(0);
            }
            for (int i = 0; i < srow; i++) {
                strModel.removeRow(0);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error cleaning the table.");
        }
    }

    private void reset() {
        cleanTables();
        this.jTextFieldResultsPath.setText("");

        //Combobox
        this.jComboBoxMeasure.removeAllItems();
        this.jComboBoxTest.setSelectedItem("Holm");
        //spinner
        this.jSpinnerPvalue.setValue(0.05);

        this.jButtonTest.setSelected(false);
        this.jButtonImage.setSelected(false);

    }

    /**
     * Allows you to read the results file and update the corresponding fields.
     *
     * @param path
     */
    public void readData(String path) {
        reset();
        this.jTextFieldResultsPath.setText(path);
        this.path = path;
        rf = new ReadFile(path);
        String str = rf.processFiles();
        if (str.equals("")) {

            int algSize = rf.getAlgShortNames().size();
            int streamSize = rf.getStream().size();
            this.measures = rf.getMeasures();
            for (int i = 0; i < algSize; i++) {
                this.algoritmModel.addRow(new Object[]{rf.getAlgNames().get(i), rf.getAlgShortNames().get(i)});
            }
            for (int i = 0; i < streamSize; i++) {
                this.streamModel.addRow(new Object[]{rf.getStream().get(i), rf.getStream().get(i)});
            }
            String measuresNames[] = measures.getFirst().split(",");
            for (String measuresName : measuresNames) {
                jComboBoxMeasure.addItem(measuresName);
                if (measuresName.equals("classifications correct (percent)") == true
                        || measuresName.equals("[avg] classifications correct (percent)") == true) {
                    jComboBoxMeasure.setSelectedItem(measuresName);
                }

            }
        } else {

            JOptionPane.showMessageDialog(this, str,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

    /**
     * Main class method.
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

    class PopupMenu extends JPopupMenu {

        JMenuItem anItem;

        public PopupMenu() {
            anItem = new JMenuItem("Clear");
            add(anItem);
            ActionListener listener = (ActionEvent event) -> {
                jTextAreaOut.setText("");
            };
            anItem.addActionListener(listener);
        }

    }

    class PopClickListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                doPop(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                doPop(e);
            }
        }

        private void doPop(MouseEvent e) {
            PopupMenu menu = new PopupMenu();
            menu.show(e.getComponent(), e.getX(), e.getY());

        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonDelAlgoritm;
    private javax.swing.JButton jButtonDelStream;
    private javax.swing.JButton jButtonImage;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonResults;
    private javax.swing.JButton jButtonTest;
    private javax.swing.JComboBox jComboBoxMeasure;
    private javax.swing.JComboBox jComboBoxTest;
    private javax.swing.JComboBox jComboBoxType;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabelDirectory;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPaneAlgorithms;
    private javax.swing.JScrollPane jScrollPaneStreams;
    private javax.swing.JSpinner jSpinnerPvalue;
    private javax.swing.JTable jTableAlgoritms;
    private javax.swing.JTable jTableStreams;
    private javax.swing.JTextArea jTextAreaOut;
    private javax.swing.JTextField jTextFieldResultsPath;
    // End of variables declaration//GEN-END:variables
}
