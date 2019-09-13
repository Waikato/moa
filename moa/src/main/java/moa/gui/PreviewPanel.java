/*
 *    PreviewPanel.java
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import moa.core.StringUtils;
import moa.evaluation.Accuracy;
import moa.evaluation.ChangeDetectionMeasures;
import moa.evaluation.MeasureCollection;
import moa.evaluation.preview.Preview;
import moa.evaluation.RegressionAccuracy;
import moa.gui.conceptdrift.CDTaskManagerPanel;
import moa.tasks.FailedTaskReport;
import moa.tasks.ResultPreviewListener;
import moa.tasks.TaskThread;

/**
 * This panel displays the running task preview text and buttons.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class PreviewPanel extends JPanel implements ResultPreviewListener {

    private static final long serialVersionUID = 1L;

    public static final String[] autoFreqStrings = {"never", "every second",
        "every 5 seconds", "every 10 seconds", "every 30 seconds",
        "every minute"};

    public static final int[] autoFreqTimeSecs = {0, 1, 5, 10, 30, 60};

    protected TaskThread previewedThread;

    protected JLabel previewLabel = new JLabel("No preview available");

    protected JButton refreshButton = new JButton("Refresh");

    protected JLabel autoRefreshLabel = new JLabel("Auto refresh: ");

    protected JComboBox<String> autoRefreshComboBox = new JComboBox<String>(autoFreqStrings);

    protected TaskTextViewerPanel textViewerPanel; // = new TaskTextViewerPanel();

    protected javax.swing.Timer autoRefreshTimer;
    
    public enum TypePanel {
        CLASSIFICATION(new Accuracy()),
        REGRESSION(new RegressionAccuracy()),
        CONCEPT_DRIFT(new ChangeDetectionMeasures());
        private final MeasureCollection measureCollection;
        //Constructor
        TypePanel(MeasureCollection measureCollection){
            this.measureCollection = measureCollection;
        }
        
        public MeasureCollection getMeasureCollection(){
            return (MeasureCollection) this.measureCollection.copy();
        }
    }

    public PreviewPanel() {
        this(TypePanel.CLASSIFICATION, null);
    }
    
    public PreviewPanel(TypePanel typePanel) {
        this(typePanel, null);
    }
    
    public PreviewPanel(TypePanel typePanel, CDTaskManagerPanel taskManagerPanel) {
        this.textViewerPanel = new TaskTextViewerPanel(typePanel,taskManagerPanel); 
        this.autoRefreshComboBox.setSelectedIndex(1); // default to 1 sec
        JPanel controlPanel = new JPanel();
        controlPanel.add(this.previewLabel);
        controlPanel.add(this.refreshButton);
        controlPanel.add(this.autoRefreshLabel);
        controlPanel.add(this.autoRefreshComboBox);
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(this.textViewerPanel, BorderLayout.CENTER);
        this.refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                refresh();
            }
        });
        this.autoRefreshTimer = new javax.swing.Timer(1000,
                new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        refresh();
                    }
                });
        this.autoRefreshComboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                updateAutoRefreshTimer();
            }
        });
        setTaskThreadToPreview(null);
    }

    public void refresh() {
        if (this.previewedThread != null) {
            if (this.previewedThread.isComplete()) {
                setLatestPreview();
                disableRefresh();
            } else {
                this.previewedThread.getPreview(PreviewPanel.this);
            }
        }
    }

    public void setTaskThreadToPreview(TaskThread thread) {
        this.previewedThread = thread;
        setLatestPreview();
        if (thread == null) {
            disableRefresh();
        } else if (!thread.isComplete()) {
            enableRefresh();
        }
    }

    /**
     * Requests the latest preview and sends it to the TextViewerPanel to
     * display it.
     */
    private void setLatestPreview() {
    	Object previewObject = null;
    	if (this.previewedThread != null) {
    	    if (this.previewedThread.isFailed()) {
                previewObject = this.previewedThread.getFinalResult();
                this.previewLabel.setText("Error");
                disableRefresh();
            }
            else if (this.previewedThread.isComplete()) {
                // cancelled or completed task
                previewObject = this.previewedThread.getFinalResult();
                this.previewLabel.setText("Final result");
                disableRefresh();
            }
            else {
                // running task
                previewObject = this.previewedThread.getLatestResultPreview();
                double grabTime = this.previewedThread.getLatestPreviewGrabTimeSeconds();
                String grabString = " (" + StringUtils.secondsToDHMSString(grabTime) + ")";
                this.previewLabel.setText("Preview" + grabString);
            }
	}

	if (previewObject == null){
            // no thread
            this.previewLabel.setText("No preview available");
	}

	this.textViewerPanel.setText(previewObject);
	if (previewObject instanceof Preview)
	    this.textViewerPanel.setGraph(previewObject.toString());
	else
	    this.textViewerPanel.setGraph(null);
    }
    
    public void updateAutoRefreshTimer() {
        int autoDelay = autoFreqTimeSecs[this.autoRefreshComboBox.getSelectedIndex()];
        if (autoDelay > 0) {
            if (this.autoRefreshTimer.isRunning()) {
                this.autoRefreshTimer.stop();
            }
            this.autoRefreshTimer.setDelay(autoDelay * 1000);
            this.autoRefreshTimer.start();
        } else {
            this.autoRefreshTimer.stop();
        }
    }

    public void disableRefresh() {
        this.refreshButton.setEnabled(false);
        this.autoRefreshLabel.setEnabled(false);
        this.autoRefreshComboBox.setEnabled(false);
        this.autoRefreshTimer.stop();
    }

    public void enableRefresh() {
        this.refreshButton.setEnabled(true);
        this.autoRefreshLabel.setEnabled(true);
        this.autoRefreshComboBox.setEnabled(true);
        updateAutoRefreshTimer();
    }

    @Override
    public void latestPreviewChanged() {
        setTaskThreadToPreview(this.previewedThread);
    }
}
