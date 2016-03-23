/*
 *    TaskTextViewerPanel.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Jansen (moa@cs.rwth-aachen.de)
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
import java.awt.Dimension;
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
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import moa.evaluation.Accuracy;
import moa.evaluation.MeasureCollection;
import moa.evaluation.RegressionAccuracy;
import moa.gui.PreviewPanel.TypePanel;
import moa.gui.conceptdrift.CDTaskManagerPanel;
import moa.streams.clustering.ClusterEvent;
import moa.tasks.ConceptDriftMainTask;

/**
 * This panel displays text. Used to output the results of tasks.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class TaskTextViewerPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    public static String exportFileExtension = "txt";

    protected JTextArea textArea;

    protected JScrollPane scrollPane;

    protected JButton exportButton;

    private javax.swing.JPanel topWrapper;

    private javax.swing.JSplitPane jSplitPane1;

   //Added for stream events
    protected CDTaskManagerPanel taskManagerPanel;
    
    protected TypePanel typePanel;
    
    public void initVisualEvalPanel() {
        acc1[0] = getNewMeasureCollection();
        acc2[0] = getNewMeasureCollection();
        if (clusteringVisualEvalPanel1 != null) {
            panelEvalOutput.remove(clusteringVisualEvalPanel1);
        }
        clusteringVisualEvalPanel1 = new moa.gui.clustertab.ClusteringVisualEvalPanel();
        clusteringVisualEvalPanel1.setMeasures(acc1, acc2, this);
        this.graphCanvas.setGraph(acc1[0], acc2[0], 0, 1000); 
        this.graphCanvas.forceAddEvents();
        clusteringVisualEvalPanel1.setMinimumSize(new java.awt.Dimension(280, 118));
        clusteringVisualEvalPanel1.setPreferredSize(new java.awt.Dimension(290, 115));
         panelEvalOutput.add(clusteringVisualEvalPanel1, gridBagConstraints);
    }
        public TaskTextViewerPanel() {
           this(TypePanel.CLASSIFICATION, null);
        }
        
        public java.awt.GridBagConstraints gridBagConstraints;
        
        public TaskTextViewerPanel(PreviewPanel.TypePanel typePanel, CDTaskManagerPanel taskManagerPanel) { 
        this.typePanel = typePanel;
        this.taskManagerPanel = taskManagerPanel;
        jSplitPane1 = new javax.swing.JSplitPane();
        topWrapper = new javax.swing.JPanel();

        this.textArea = new JTextArea();
        this.textArea.setEditable(false);
        this.textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.exportButton = new JButton("Export as .txt file...");
        this.exportButton.setEnabled(false);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2));
        buttonPanel.add(this.exportButton);
        topWrapper.setLayout(new BorderLayout());
        this.scrollPane = new JScrollPane(this.textArea);
        topWrapper.add(this.scrollPane, BorderLayout.CENTER);
        topWrapper.add(buttonPanel, BorderLayout.SOUTH);
        this.exportButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setAcceptAllFileFilterUsed(true);
                fileChooser.addChoosableFileFilter(new FileExtensionFilter(
                        exportFileExtension));
                if (fileChooser.showSaveDialog(TaskTextViewerPanel.this) == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = fileChooser.getSelectedFile();
                    String fileName = chosenFile.getPath();
                    if (!chosenFile.exists()
                            && !fileName.endsWith(exportFileExtension)) {
                        fileName = fileName + "." + exportFileExtension;
                    }
                    try {
                        PrintWriter out = new PrintWriter(new BufferedWriter(
                                new FileWriter(fileName)));
                        out.write(TaskTextViewerPanel.this.textArea.getText());
                        out.close();
                    } catch (IOException ioe) {
                        GUIUtils.showExceptionDialog(
                                TaskTextViewerPanel.this.exportButton,
                                "Problem saving file " + fileName, ioe);
                    }
                }
            }
        });
        //topWrapper.add(this.scrollPane);
        //topWrapper.add(buttonPanel);

        panelEvalOutput = new javax.swing.JPanel();
        //clusteringVisualEvalPanel1 = new moa.gui.clustertab.ClusteringVisualEvalPanel();
        graphPanel = new javax.swing.JPanel();
        graphPanelControlTop = new javax.swing.JPanel();
        buttonZoomInY = new javax.swing.JButton();
        buttonZoomOutY = new javax.swing.JButton();
        labelEvents = new javax.swing.JLabel();
        graphScrollPanel = new javax.swing.JScrollPane();
        graphCanvas = new moa.gui.visualization.GraphCanvas();
        // New EventClusters
        //clusterEvents = new  ArrayList<ClusterEvent>();
        //clusterEvents.add(new ClusterEvent(this,100,"Change", "Drift"));
        //graphCanvas.setClusterEventsList(clusterEvents);

        graphPanelControlBottom = new javax.swing.JPanel();
        buttonZoomInX = new javax.swing.JButton();
        buttonZoomOutX = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        //topWrapper.setPreferredSize(new java.awt.Dimension(688, 500));
        //topWrapper.setLayout(new java.awt.GridBagLayout());

        jSplitPane1.setLeftComponent(topWrapper);

        panelEvalOutput.setBorder(javax.swing.BorderFactory.createTitledBorder("Evaluation"));
        panelEvalOutput.setLayout(new java.awt.GridBagLayout());

        //clusteringVisualEvalPanel1.setMinimumSize(new java.awt.Dimension(280, 118));
        //clusteringVisualEvalPanel1.setPreferredSize(new java.awt.Dimension(290, 115));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        //panelEvalOutput.add(clusteringVisualEvalPanel1, gridBagConstraints);
        initVisualEvalPanel();


        graphPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Plot"));
        graphPanel.setPreferredSize(new java.awt.Dimension(530, 115));
        graphPanel.setLayout(new java.awt.GridBagLayout());

        graphPanelControlTop.setLayout(new java.awt.GridBagLayout());

        buttonZoomInY.setText("Zoom in Y");
        buttonZoomInY.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonZoomInYActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        graphPanelControlTop.add(buttonZoomInY, gridBagConstraints);

        buttonZoomOutY.setText("Zoom out Y");
        buttonZoomOutY.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonZoomOutYActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        graphPanelControlTop.add(buttonZoomOutY, gridBagConstraints);

        labelEvents.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        graphPanelControlTop.add(labelEvents, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        graphPanel.add(graphPanelControlTop, gridBagConstraints);

        graphCanvas.setPreferredSize(new java.awt.Dimension(500, 111));

        javax.swing.GroupLayout graphCanvasLayout = new javax.swing.GroupLayout(graphCanvas);
        graphCanvas.setLayout(graphCanvasLayout);
        graphCanvasLayout.setHorizontalGroup(
                graphCanvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 515, Short.MAX_VALUE));
        graphCanvasLayout.setVerticalGroup(
                graphCanvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 128, Short.MAX_VALUE));

        graphScrollPanel.setViewportView(graphCanvas);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        graphPanel.add(graphScrollPanel, gridBagConstraints);

        buttonZoomInX.setText("Zoom in X");
        buttonZoomInX.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonZoomInXActionPerformed(evt);
            }
        });
        graphPanelControlBottom.add(buttonZoomInX);

        buttonZoomOutX.setText("Zoom out X");
        buttonZoomOutX.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonZoomOutXActionPerformed(evt);
            }
        });
        graphPanelControlBottom.add(buttonZoomOutX);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        graphPanel.add(graphPanelControlBottom, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 2.0;
        gridBagConstraints.weighty = 1.0;
        panelEvalOutput.add(graphPanel, gridBagConstraints);

        jSplitPane1.setRightComponent(panelEvalOutput);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jSplitPane1, gridBagConstraints);

       // acc1[0] = getNewMeasureCollection();

        //acc2[0] = getNewMeasureCollection();
        //clusteringVisualEvalPanel1.setMeasures(acc1, acc2, this);
        //this.graphCanvas.setGraph(acc1[0], acc2[0], 0, 1000);

    }

    public void setText(String newText) {
        Point p = this.scrollPane.getViewport().getViewPosition();
        this.textArea.setText(newText);
        this.scrollPane.getViewport().setViewPosition(p);
        this.exportButton.setEnabled(newText != null);
        setGraph(newText);
    }

    protected MeasureCollection[] acc1 = new MeasureCollection[1];

    protected MeasureCollection[] acc2 = new MeasureCollection[1];

    protected String secondLine = "";

    protected double round(double d) {
        return (Math.rint(d * 100) / 100);
    }

    protected MeasureCollection getNewMeasureCollection() {
        /*if (this.taskManagerPanel != null) {
            return new ChangeDetectionMeasures();
        } else {
            return new Accuracy();
        }*/
        return this.typePanel.getMeasureCollection();
    }

    public void setGraph(String preview) {
        //Change the graph when there is change in the text
        double processFrequency = 1000;
        if (preview != null && !preview.equals("")) {
            MeasureCollection oldAccuracy = acc1[0];
            acc1[0] = getNewMeasureCollection();
            Scanner scanner = new Scanner(preview);
            String firstLine = scanner.nextLine();
            boolean isSecondLine = true;

            boolean isPrequential = firstLine.startsWith("learning evaluation instances,evaluation time");
            boolean isHoldOut = firstLine.startsWith("evaluation instances,to");
            int accuracyColumn = 6;
            int kappaColumn = 4;
            int RamColumn = 2;
            int timeColumn = 1;
            int memoryColumn = 9;
            int kappaTempColumn = 5;
            if (this.taskManagerPanel instanceof CDTaskManagerPanel) {
                accuracyColumn = 6;
                kappaColumn = 4;
                RamColumn = 2;
                timeColumn = 1;
                memoryColumn = 9;
            } else if (isPrequential || isHoldOut) {
                accuracyColumn = 4;
                kappaColumn = 5;
                RamColumn = 2;
                timeColumn = 1;
                memoryColumn = 7;
                kappaTempColumn = 5;
                String[] tokensFirstLine = firstLine.split(",");
                int i = 0;
                for (String s : tokensFirstLine) {
                    if (s.equals("classifications correct (percent)") ||  s.equals("[avg] classifications correct (percent)")) {
                        accuracyColumn = i;
                    } else if (s.equals("Kappa Statistic (percent)") || s.equals("[avg] Kappa Statistic (percent)")) {
                        kappaColumn = i;
                    } else if (s.equals("Kappa Temporal Statistic (percent)") || s.equals("[avg] Kappa Temporal Statistic (percent)")) {
                        kappaTempColumn = i;
                    } else if (s.equals("model cost (RAM-Hours)")) {
                        RamColumn = i;
                    } else if (s.equals("evaluation time (cpu seconds)")
                            || s.equals("total train time")) {
                        timeColumn = i;
                    } else if (s.equals("model serialized size (bytes)")) {
                        memoryColumn = i;
                    }
                    i++;
                }
            }
            if (isPrequential || isHoldOut || this.taskManagerPanel instanceof CDTaskManagerPanel) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] tokens = line.split(",");
                    this.acc1[0].addValue(0, round(parseDouble(tokens[accuracyColumn])));
                    this.acc1[0].addValue(1, round(parseDouble(tokens[kappaColumn])));
                    this.acc1[0].addValue(2, round(parseDouble(tokens[kappaTempColumn])));
                    if (!isHoldOut) {
                        this.acc1[0].addValue(3, Math.abs(parseDouble(tokens[RamColumn])));
                    }
                    this.acc1[0].addValue(4, round(parseDouble(tokens[timeColumn])));
                    this.acc1[0].addValue(5, round(parseDouble(tokens[memoryColumn]) / (1024 * 1024)));

                    if (isSecondLine == true) {
                        processFrequency = Math.abs(parseDouble(tokens[0]));
                        isSecondLine = false;
                        if (acc1[0].getValue(0, 0) != oldAccuracy.getValue(0, 0)) { //(!line.equals(secondLine)) {
                            //If we are in a new task, compare with the previous
                            secondLine = line;
                            if (processFrequency == this.graphCanvas.getProcessFrequency()) {
                                acc2[0] = oldAccuracy;
                            }
                        }
                    }
                }
            } else {
                this.acc2[0] = getNewMeasureCollection();
            }

        } else {
            this.acc1[0] = getNewMeasureCollection();
            this.acc2[0] = getNewMeasureCollection();
        }


        if (this.taskManagerPanel instanceof CDTaskManagerPanel) {
            ConceptDriftMainTask cdTask = this.taskManagerPanel.getSelectedCurrenTask();
            ArrayList<ClusterEvent> clusterEvents = cdTask.getEventsList();
            this.graphCanvas.setClusterEventsList(clusterEvents);
        }
        this.graphCanvas.setGraph(acc1[0], acc2[0], this.graphCanvas.getMeasureSelected(), (int) processFrequency);
        this.graphCanvas.updateCanvas(true);
        this.graphCanvas.forceAddEvents();
        this.clusteringVisualEvalPanel1.update();

    }

    private double parseDouble(String s) {
        double ret = 0;
        if (s.equals("?") == false) {
            ret = Double.parseDouble(s);
        }
        return ret;
    }

    private void scrollPane0MouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_scrollPane0MouseWheelMoved
        streamPanel0.setZoom(evt.getX(), evt.getY(), (-1) * evt.getWheelRotation(), scrollPane0);
    }//GEN-LAST:event_scrollPane0MouseWheelMoved

    private void buttonZoomInXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomInXActionPerformed
        graphCanvas.scaleXResolution(false);
    }//GEN-LAST:event_buttonZoomInXActionPerformed

    private void buttonZoomOutYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomOutYActionPerformed
        graphCanvas.setSize(new Dimension(graphCanvas.getWidth(), (int) (graphCanvas.getHeight() * 0.8)));
        graphCanvas.setPreferredSize(new Dimension(graphCanvas.getWidth(), (int) (graphCanvas.getHeight() * 0.8)));
        this.graphCanvas.updateCanvas(true);
    }//GEN-LAST:event_buttonZoomOutYActionPerformed

    private void buttonZoomOutXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomOutXActionPerformed
        graphCanvas.scaleXResolution(true);
    }//GEN-LAST:event_buttonZoomOutXActionPerformed

    private void buttonZoomInYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomInYActionPerformed
        graphCanvas.setSize(new Dimension(graphCanvas.getWidth(), (int) (graphCanvas.getHeight() * 1.2)));
        graphCanvas.setPreferredSize(new Dimension(graphCanvas.getWidth(), (int) (graphCanvas.getHeight() * 1.2)));
        this.graphCanvas.updateCanvas(true);
    }//GEN-LAST:event_buttonZoomInYActionPerformed

    private void buttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRunActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_buttonRunActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonRun;

    private javax.swing.JButton buttonScreenshot;

    private javax.swing.JButton buttonStop;

    private javax.swing.JButton buttonZoomInX;

    private javax.swing.JButton buttonZoomInY;

    private javax.swing.JButton buttonZoomOutX;

    private javax.swing.JButton buttonZoomOutY;

    private javax.swing.JCheckBox checkboxDrawClustering;

    private javax.swing.JCheckBox checkboxDrawGT;

    private javax.swing.JCheckBox checkboxDrawMicro;

    private javax.swing.JCheckBox checkboxDrawPoints;

    private moa.gui.clustertab.ClusteringVisualEvalPanel clusteringVisualEvalPanel1;

    private javax.swing.JComboBox comboX;

    private javax.swing.JComboBox comboY;

    private moa.gui.visualization.GraphCanvas graphCanvas;

    private javax.swing.JPanel graphPanel;

    private javax.swing.JPanel graphPanelControlBottom;

    private javax.swing.JPanel graphPanelControlTop;

    private javax.swing.JScrollPane graphScrollPanel;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel labelEvents;

    private javax.swing.JLabel labelNumPause;

    private javax.swing.JLabel labelX;

    private javax.swing.JLabel labelY;

    private javax.swing.JLabel label_processed_points;

    private javax.swing.JLabel label_processed_points_value;

    private javax.swing.JTextField numPauseAfterPoints;

    private javax.swing.JPanel panelControl;

    private javax.swing.JPanel panelEvalOutput;

    private javax.swing.JPanel panelVisualWrapper;

    private javax.swing.JScrollPane scrollPane0;

    private javax.swing.JScrollPane scrollPane1;

    private javax.swing.JSlider speedSlider;

    private javax.swing.JSplitPane splitVisual;

    private moa.gui.visualization.StreamPanel streamPanel0;

    private moa.gui.visualization.StreamPanel streamPanel1;

    @Override
    public void actionPerformed(ActionEvent e) {
        //reacte on graph selection and find out which measure was selected
        int selected = Integer.parseInt(e.getActionCommand());
        int counter = selected;
        int m_select = 0;
        int m_select_offset = 0;
        boolean found = false;
        for (int i = 0; i < acc1.length; i++) {
            for (int j = 0; j < acc1[i].getNumMeasures(); j++) {
                if (acc1[i].isEnabled(j)) {
                    counter--;
                    if (counter < 0) {
                        m_select = i;
                        m_select_offset = j;
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                break;
            }
        }
        this.graphCanvas.setGraph(acc1[m_select], acc2[m_select], m_select_offset, this.graphCanvas.getProcessFrequency());
        this.graphCanvas.forceAddEvents();
    }
}
