/*
 *    OutlierVisualTab.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.gui.outliertab;

import moa.gui.FileExtensionFilter;
import moa.gui.visualization.GraphCanvas;
import moa.gui.visualization.RunOutlierVisualizer;
import moa.gui.visualization.StreamOutlierPanel;
import nz.ac.waikato.cms.gui.core.BaseFileChooser;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ToolTipManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;

public class OutlierVisualTab extends javax.swing.JPanel implements ActionListener, ComponentListener{
    private RunOutlierVisualizer visualizer = null;
    private Thread visualizerThread = null;
    private Boolean running = false;
    private OutlierSetupTab outlierSetupTab = null;
    private String exportFile;
    private String screenshotFilebase;

    /** Creates new form OutlierVisualTab */
    public OutlierVisualTab() {
        resetComponents();
    }

    private void resetComponents(){
        initComponents();
        comboY.setSelectedIndex(1);
        graphCanvas.setViewport(graphScrollPanel.getViewport());

        //TODO this needs to only affect the visual Panel
        ToolTipManager.sharedInstance().setDismissDelay(20000);
        ToolTipManager.sharedInstance().setInitialDelay(100);
    }

    public void setOutlierSetupTab(OutlierSetupTab outlierSetupTab){
        this.outlierSetupTab = outlierSetupTab;
    }


    private void createVisualiterThread(){
        visualizer = new RunOutlierVisualizer(this, outlierSetupTab);
        visualizerThread = new Thread(visualizer);
    }

    public void setDimensionComobBoxes(int numDimensions){
        String[] dimensions = new String[numDimensions];
        for (int i = 0; i < dimensions.length; i++) {
            dimensions[i] = "Dim "+(i+1);

        }
        comboX.setModel(new javax.swing.DefaultComboBoxModel(dimensions));
        comboY.setModel(new javax.swing.DefaultComboBoxModel(dimensions));
        comboY.setSelectedIndex(1);
    }

    public StreamOutlierPanel getLeftStreamPanel(){
        return streamPanel0;
    }

    public StreamOutlierPanel getRightStreamPanel(){
        return streamPanel1;
    }

    public GraphCanvas getGraphCanvas(){
        return graphCanvas;
    }

    public OutlierVisualEvalPanel getEvalPanel(){
        return null;
    }

    public boolean isEnabledDrawPoints(){
        return checkboxDrawPoints.isSelected();
    }
    
    public boolean isEnabledDrawOutliers(){
        return checkboxDrawOutliers.isSelected();
    }

    public void setProcessedPointsCounter(int value){
        label_processed_points_value.setText(Integer.toString(value));
    }

    public int getPauseInterval(){
        return Integer.parseInt(numPauseAfterPoints.getText());
    }

    public void setPauseInterval(int pause){
        numPauseAfterPoints.setText(Integer.toString(pause));
    }
    
    private void UpdateSplitVisualDivider() {
        if(splitVisual != null)
            splitVisual.setDividerLocation(splitVisual.getWidth()/2);
    }

    @Override
    public void repaint() {                
        super.repaint();
        UpdateSplitVisualDivider();
    }
    
    @Override
    public void componentResized(ComponentEvent ce) {
    }

    @Override
    public void componentMoved(ComponentEvent ce) {
    }

    @Override
    public void componentShown(ComponentEvent ce) {
    }

    @Override
    public void componentHidden(ComponentEvent ce) {
    }

    public void toggleVisualizer(boolean internal){
        if(visualizer == null)
            createVisualiterThread();
        
        if(!visualizerThread.isAlive()){
            visualizerThread.start();
        }        
        
        //pause
        if(running){
            running = false;
            visualizer.pause();
            buttonRun.setText("Resume");
        }
        else{
            running = true;
            visualizer.resume();
            buttonRun.setText("Pause");
        }
        if(internal)
            outlierSetupTab.toggleRunMode();        
    }

    public void stopVisualizer(){
        if (visualizer == null) return;
        
        visualizer.stop();
        running = false;
        visualizer = null;
        visualizerThread = null;
        removeAll();
        resetComponents();
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jSplitPane1 = new javax.swing.JSplitPane();
        topWrapper = new javax.swing.JPanel();
        panelVisualWrapper = new javax.swing.JPanel();
        splitVisual = new javax.swing.JSplitPane();
        scrollPane1 = new javax.swing.JScrollPane();
        scrollPane0 = new javax.swing.JScrollPane();
        streamPanel0 = new moa.gui.visualization.StreamOutlierPanel(Color.RED);
        streamPanel1 = new moa.gui.visualization.StreamOutlierPanel(Color.BLUE);
        panelControl = new javax.swing.JPanel();
        buttonRun = new javax.swing.JButton();
        buttonStop = new javax.swing.JButton();
        buttonRedraw = new javax.swing.JButton();
        buttonScreenshot = new javax.swing.JButton();
        speedSlider = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        comboX = new javax.swing.JComboBox();
        labelX = new javax.swing.JLabel();
        comboY = new javax.swing.JComboBox();
        labelY = new javax.swing.JLabel();
        checkboxDrawPoints = new javax.swing.JCheckBox();
        checkboxDrawOutliers = new javax.swing.JCheckBox();
        checkboxWaitWinFull = new javax.swing.JCheckBox();
        label_processed_points = new javax.swing.JLabel();
        label_processed_points_value = new javax.swing.JLabel();
        labelNumPause = new javax.swing.JLabel();
        numPauseAfterPoints = new javax.swing.JTextField();
        panelEvalOutput = new javax.swing.JPanel();
        // outlierVisualEvalPanel1 = new moa.gui.outliertab.OutlierVisualEvalPanel();
        graphPanel = new javax.swing.JPanel();
        graphPanelControlTop = new javax.swing.JPanel();
        buttonZoomInY = new javax.swing.JButton();
        buttonZoomOutY = new javax.swing.JButton();
        labelEvents = new javax.swing.JLabel();
        graphScrollPanel = new javax.swing.JScrollPane();
        graphCanvas = new moa.gui.visualization.GraphCanvas();
        graphPanelControlBottom = new javax.swing.JPanel();
        buttonZoomInX = new javax.swing.JButton();
        buttonZoomOutX = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        jSplitPane1.setDividerLocation(400);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        topWrapper.setPreferredSize(new java.awt.Dimension(688, 500));
        topWrapper.setLayout(new java.awt.GridBagLayout());

        //panelVisualWrapper.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelVisualWrapper.setLayout(new java.awt.BorderLayout());

        splitVisual.setDividerLocation(400);
        splitVisual.setResizeWeight(1.0);
        
        scrollPane0.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                scrollPane0MouseWheelMoved(evt);
            }
        });
        
        streamPanel1.setPreferredSize(new java.awt.Dimension(400, 250));

        javax.swing.GroupLayout streamPanel1Layout = new javax.swing.GroupLayout(streamPanel1);
        streamPanel1.setLayout(streamPanel1Layout);
        streamPanel1Layout.setHorizontalGroup(
            streamPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 428, Short.MAX_VALUE)
        );
        streamPanel1Layout.setVerticalGroup(
            streamPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 339, Short.MAX_VALUE)
        );

        scrollPane1.setViewportView(streamPanel1);
        
        splitVisual.setRightComponent(scrollPane1);

        scrollPane0.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                scrollPane0MouseWheelMoved(evt);
            }
        });

        streamPanel0.setPreferredSize(new java.awt.Dimension(400, 250));

        javax.swing.GroupLayout streamPanel0Layout = new javax.swing.GroupLayout(streamPanel0);
        streamPanel0.setLayout(streamPanel0Layout);
        streamPanel0Layout.setHorizontalGroup(
            streamPanel0Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        streamPanel0Layout.setVerticalGroup(
            streamPanel0Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 339, Short.MAX_VALUE)
        );

        scrollPane0.setViewportView(streamPanel0);
        
        splitVisual.setLeftComponent(scrollPane0);
        
        splitVisual.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent ce) {
                UpdateSplitVisualDivider();
            }

            @Override
            public void componentMoved(ComponentEvent ce) {
            }

            @Override
            public void componentShown(ComponentEvent ce) {
            }

            @Override
            public void componentHidden(ComponentEvent ce) {
            }
        });

        panelVisualWrapper.add(splitVisual, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 400;
        gridBagConstraints.ipady = 200;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        topWrapper.add(panelVisualWrapper, gridBagConstraints);

        panelControl.setMinimumSize(new java.awt.Dimension(600, 76));
        panelControl.setPreferredSize(new java.awt.Dimension(2000, 76));
        panelControl.setLayout(new java.awt.GridBagLayout());
        
        int iBtnHeight = 27;
        int iInsetW = 2;
        int iInsetH = 2;

        buttonRun.setText("Start");
        buttonRun.setMinimumSize(new java.awt.Dimension(90, iBtnHeight));
        buttonRun.setPreferredSize(new java.awt.Dimension(90, iBtnHeight));
        buttonRun.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                buttonRunMouseClicked(evt);
            }
        });
        buttonRun.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRunActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(iInsetW, iInsetH, iInsetW, iInsetH);
        panelControl.add(buttonRun, gridBagConstraints);

        buttonStop.setText("Stop");
        buttonStop.setMinimumSize(new java.awt.Dimension(90, iBtnHeight));
        buttonStop.setPreferredSize(new java.awt.Dimension(90, iBtnHeight));
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(iInsetW, iInsetH, iInsetW, iInsetH);
        panelControl.add(buttonStop, gridBagConstraints);
        
        buttonRedraw.setText("Redraw");
        buttonRedraw.setMinimumSize(new java.awt.Dimension(100, iBtnHeight));
        buttonRedraw.setPreferredSize(new java.awt.Dimension(100, iBtnHeight));
        buttonRedraw.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                buttonRedrawMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(iInsetW, iInsetH, iInsetW, iInsetH);
        panelControl.add(buttonRedraw, gridBagConstraints);

        buttonScreenshot.setText("Screenshot");
        buttonScreenshot.setMinimumSize(new java.awt.Dimension(100, iBtnHeight));
        buttonScreenshot.setPreferredSize(new java.awt.Dimension(100, iBtnHeight));
        buttonScreenshot.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                buttonScreenshotMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(iInsetW, iInsetH, iInsetW, iInsetH);
        panelControl.add(buttonScreenshot, gridBagConstraints);

        speedSlider.setValue(100);
        speedSlider.setBorder(javax.swing.BorderFactory.createTitledBorder("Visualisation Speed"));
        speedSlider.setMinimumSize(new java.awt.Dimension(160, 68));
        speedSlider.setPreferredSize(new java.awt.Dimension(170, 68));
        speedSlider.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                speedSliderMouseDragged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 16, 1, 5);
        panelControl.add(speedSlider, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        panelControl.add(jLabel1, gridBagConstraints);        

        comboX.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Dim 1", "Dim 2", "Dim 3", "Dim 4" }));
        comboX.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboXActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        panelControl.add(comboX, gridBagConstraints);

        labelX.setText("X");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 0, 5);
        panelControl.add(labelX, gridBagConstraints);

        comboY.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Dim 1", "Dim 2", "Dim 3", "Dim 4" }));
        comboY.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboYActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        panelControl.add(comboY, gridBagConstraints);

        labelY.setText("Y");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 0, 5);
        panelControl.add(labelY, gridBagConstraints);

        checkboxDrawPoints.setSelected(true);
        checkboxDrawPoints.setText("Points");
        checkboxDrawPoints.setMargin(new java.awt.Insets(0, 0, 0, 0));
        checkboxDrawPoints.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkboxDrawPointsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;   
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        panelControl.add(checkboxDrawPoints, gridBagConstraints);

        checkboxDrawOutliers.setSelected(true);
        checkboxDrawOutliers.setText("Outliers");
        checkboxDrawOutliers.setMargin(new java.awt.Insets(0, 0, 0, 0));
        checkboxDrawOutliers.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkboxDrawOutlierActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;        
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        panelControl.add(checkboxDrawOutliers, gridBagConstraints);
        
        checkboxWaitWinFull.setSelected(true);
        checkboxWaitWinFull.setText("WaitWinFull");
        checkboxWaitWinFull.setMargin(new java.awt.Insets(0, 0, 0, 0));
        checkboxWaitWinFull.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkboxWaitWinFullActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;        
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
        panelControl.add(checkboxWaitWinFull, gridBagConstraints);

        label_processed_points.setText("Processed:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        panelControl.add(label_processed_points, gridBagConstraints);

        label_processed_points_value.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelControl.add(label_processed_points_value, gridBagConstraints);

        labelNumPause.setText("Pause in:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        panelControl.add(labelNumPause, gridBagConstraints);

        numPauseAfterPoints.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        numPauseAfterPoints.setText(Integer.toString(RunOutlierVisualizer.initialPauseInterval));
        numPauseAfterPoints.setMinimumSize(new java.awt.Dimension(70, 25));
        numPauseAfterPoints.setPreferredSize(new java.awt.Dimension(70, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelControl.add(numPauseAfterPoints, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        topWrapper.add(panelControl, gridBagConstraints);

        jSplitPane1.setLeftComponent(topWrapper);

        panelEvalOutput.setBorder(javax.swing.BorderFactory.createTitledBorder("Evaluation"));
        panelEvalOutput.setLayout(new java.awt.GridBagLayout());

        graphPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Process time per object"));
        graphPanel.setPreferredSize(new java.awt.Dimension(530, 115));
        graphPanel.setLayout(new java.awt.GridBagLayout());

        graphPanelControlTop.setLayout(new java.awt.GridBagLayout());

        buttonZoomInY.setText("Zoom in Y");
        buttonZoomInY.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonZoomInYActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        graphPanelControlTop.add(buttonZoomInY, gridBagConstraints);

        buttonZoomOutY.setText("Zoom out Y");
        buttonZoomOutY.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
            graphCanvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 515, Short.MAX_VALUE)
        );
        graphCanvasLayout.setVerticalGroup(
            graphCanvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 128, Short.MAX_VALUE)
        );

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
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonZoomInXActionPerformed(evt);
            }
        });
        graphPanelControlBottom.add(buttonZoomInX);

        buttonZoomOutX.setText("Zoom out X");
        buttonZoomOutX.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
        panelEvalOutput.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent ce) {
                //System.out.println("panelEvalOutput componentResized");
                graphCanvas.updateCanvas(true);
            }

            @Override
            public void componentMoved(ComponentEvent ce) {
            }

            @Override
            public void componentShown(ComponentEvent ce) {
            }

            @Override
            public void componentHidden(ComponentEvent ce) {
            }
        });

        jSplitPane1.setRightComponent(panelEvalOutput);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jSplitPane1, gridBagConstraints);
        //add(topWrapper, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void buttonRedrawMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonRedrawMouseClicked
        if (visualizer != null) 
            visualizer.redraw();
    }//GEN-LAST:event_buttonScreenshotMouseClicked

    private void buttonScreenshotMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonScreenshotMouseClicked
        BaseFileChooser fileChooser = new BaseFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(true);
        if(screenshotFilebase!=null)
            fileChooser.setSelectedFile(new File(screenshotFilebase));
        if (fileChooser.showSaveDialog(this) == BaseFileChooser.APPROVE_OPTION) {
        	screenshotFilebase = fileChooser.getSelectedFile().getPath();
        	streamPanel0.screenshot(screenshotFilebase+"_"+label_processed_points_value.getText()+"_0", true, true);
        }
    }//GEN-LAST:event_buttonScreenshotMouseClicked

    private void buttonRunMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonRunMouseClicked
        toggleVisualizer(true);
    }//GEN-LAST:event_buttonRunMouseClicked

    private void speedSliderMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_speedSliderMouseDragged
        if (speedSlider == null) return;
        if (visualizer == null)  return;
        
        visualizer.setSpeed(speedSlider.getValue());

    }//GEN-LAST:event_speedSliderMouseDragged
    
    public int GetSpeed() {
        return speedSlider.getValue();
    }

    private void scrollPane0MouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_scrollPane0MouseWheelMoved
        streamPanel0.setZoom(evt.getX(),evt.getY(),(-1)*evt.getWheelRotation(),scrollPane0);
    }//GEN-LAST:event_scrollPane0MouseWheelMoved

    private void buttonZoomInXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomInXActionPerformed
        graphCanvas.scaleXResolution(false);
        graphCanvas.updateCanvas(true);
    }//GEN-LAST:event_buttonZoomInXActionPerformed

    private void buttonZoomOutYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomOutYActionPerformed
        graphCanvas.setSize(new Dimension(graphCanvas.getWidth(), (int)(graphCanvas.getHeight()*0.8)));
        graphCanvas.setPreferredSize(new Dimension(graphCanvas.getWidth(), (int)(graphCanvas.getHeight()*0.8)));
        graphCanvas.updateCanvas(true);
    }//GEN-LAST:event_buttonZoomOutYActionPerformed

    private void buttonZoomOutXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomOutXActionPerformed
        graphCanvas.scaleXResolution(true);
        graphCanvas.updateCanvas(true);
    }//GEN-LAST:event_buttonZoomOutXActionPerformed

    private void buttonZoomInYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomInYActionPerformed
        graphCanvas.setSize(new Dimension(graphCanvas.getWidth(), (int)(graphCanvas.getHeight()*1.2)));
        graphCanvas.setPreferredSize(new Dimension(graphCanvas.getWidth(), (int)(graphCanvas.getHeight()*1.2)));
        graphCanvas.updateCanvas(true);
    }//GEN-LAST:event_buttonZoomInYActionPerformed

    public boolean getPointVisibility() {
        return checkboxDrawPoints.isSelected();
    }
    
    public boolean getOutliersVisibility() {
        return checkboxDrawOutliers.isSelected();
    }
    
    private void checkboxDrawPointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkboxDrawPointsActionPerformed
        if (visualizer == null) return;
        visualizer.setPointsVisibility(checkboxDrawPoints.isSelected());
    }//GEN-LAST:event_checkboxDrawPointsActionPerformed

    private void checkboxDrawOutlierActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkboxDrawOutlierActionPerformed
        if (visualizer == null) return;
        
        if (running) {
            // outliers layer is hidden when running
            return;
        }
        // visualizer.redrawOutliers();
        visualizer.setOutliersVisibility(checkboxDrawOutliers.isSelected());
    }//GEN-LAST:event_checkboxDrawOutlierActionPerformed
    
    public boolean getWaitWinFull() {
        return checkboxWaitWinFull.isSelected();
    }

    private void checkboxWaitWinFullActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkboxWaitWinFullActionPerformed
        if (visualizer == null) return;        
        visualizer.setWaitWinFull(checkboxWaitWinFull.isSelected());
    }//GEN-LAST:checkboxWaitWinFullActionPerformed
            
    private void comboXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboXActionPerformed
        if (visualizer == null) return;
        JComboBox cb = (JComboBox)evt.getSource();
        int dim = cb.getSelectedIndex();
        streamPanel0.setActiveXDim(dim);
        streamPanel1.setActiveXDim(dim);
        if(visualizer!=null)
            visualizer.redraw();
    }//GEN-LAST:event_comboXActionPerformed

    private void comboYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboYActionPerformed
        if (visualizer == null) return;
        JComboBox cb = (JComboBox)evt.getSource();
        int dim = cb.getSelectedIndex();
        streamPanel0.setActiveYDim(dim);
        streamPanel1.setActiveYDim(dim);
        if(visualizer!=null)
            visualizer.redraw();
    }//GEN-LAST:event_comboYActionPerformed

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        stopVisualizer();
        outlierSetupTab.stopRun();
    }//GEN-LAST:event_buttonStopActionPerformed

    private void buttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRunActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_buttonRunActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonRun;
    private javax.swing.JButton buttonScreenshot;
    private javax.swing.JButton buttonRedraw;
    private javax.swing.JButton buttonStop;
    private javax.swing.JButton buttonZoomInX;
    private javax.swing.JButton buttonZoomInY;
    private javax.swing.JButton buttonZoomOutX;
    private javax.swing.JButton buttonZoomOutY;
    private javax.swing.JCheckBox checkboxDrawOutliers;
    private javax.swing.JCheckBox checkboxDrawPoints;
    private javax.swing.JCheckBox checkboxWaitWinFull;
    // ### private moa.gui.outliertab.OutlierVisualEvalPanel outlierVisualEvalPanel1;
    private javax.swing.JComboBox comboX;
    private javax.swing.JComboBox comboY;
    private moa.gui.visualization.GraphCanvas graphCanvas;
    private javax.swing.JPanel graphPanel;
    private javax.swing.JPanel graphPanelControlBottom;
    private javax.swing.JPanel graphPanelControlTop;
    private javax.swing.JScrollPane graphScrollPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JSplitPane jSplitPane1;
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
    private moa.gui.visualization.StreamOutlierPanel streamPanel0;
    private moa.gui.visualization.StreamOutlierPanel streamPanel1;
    private javax.swing.JPanel topWrapper;
    // End of variables declaration//GEN-END:variables

    @Override
    public void actionPerformed(ActionEvent e) {
        if (visualizer == null) return;
        if(e.getSource() instanceof JButton){
            if(e.getActionCommand().equals("csv export")){
                BaseFileChooser fileChooser = new BaseFileChooser();
                fileChooser.setAcceptAllFileFilterUsed(true);
                fileChooser.addChoosableFileFilter(new FileExtensionFilter("csv"));
                if(exportFile!=null)
                    fileChooser.setSelectedFile(new File(exportFile));
                if (fileChooser.showSaveDialog(this) == BaseFileChooser.APPROVE_OPTION) {
                    exportFile = fileChooser.getSelectedFile().getPath();
                    visualizer.exportCSV(exportFile);
                }
            }
            if(e.getActionCommand().equals("weka export")){
                visualizer.weka();
            }
        }
    }
}
