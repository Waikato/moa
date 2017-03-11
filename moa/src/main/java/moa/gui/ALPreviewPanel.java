/*
 *    ALPreviewPanel.java
 *    Original Work: Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    Modified Work: Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de)
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
import moa.evaluation.Preview;
import moa.tasks.ResultPreviewListener;
import moa.tasks.active.ALTaskThread;

/**
 * ALPreviewPanel provides a graphical interface to display the latest preview
 * of a task thread.
 *
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @See PreviewPanel
 */
public class ALPreviewPanel extends JPanel implements ResultPreviewListener {

    private static final long serialVersionUID = 1L;

    protected ALTaskThread previewedThread;

    protected JLabel previewLabel;

    protected JButton refreshButton;

    protected JLabel autoRefreshLabel;
    
    protected JComboBox<String> autoRefreshComboBox;

    protected ALTaskTextViewerPanel textViewerPanel;

    protected javax.swing.Timer autoRefreshTimer;

    /**
     * Initialises the underlying ALTaskTextViewerPanel and the refresh components.
     */
    public ALPreviewPanel() {
    	this.textViewerPanel = new ALTaskTextViewerPanel(); 
    	this.previewLabel = new JLabel("No preview available");
    	this.refreshButton = new JButton("Refresh");
    	this.autoRefreshLabel = new JLabel("Auto refresh: ");
    	this.autoRefreshComboBox = new JComboBox<String>(PreviewPanel.autoFreqStrings);
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

    /**
     * Refreshes the preview.
     */
    private void refresh() {
        if (this.previewedThread != null) {
            if (this.previewedThread.isComplete()) {
                setLatestPreview();
                disableRefresh();
            } else {
                this.previewedThread.getPreview(ALPreviewPanel.this);
            }
        }
    }

    /**
     * Sets the TaskThread that will be previewed.
     * @param thread TaskThread to be previewed
     */
	public void setTaskThreadToPreview(ALTaskThread thread) {
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
    	
    	Preview preview;

		if ((this.previewedThread != null) && this.previewedThread.isComplete()) {
			// cancelled, completed or failed task
			// TODO if the task is failed, the finalResult is a FailedTaskReport, which is not a Preview
			preview = (Preview) this.previewedThread.getFinalResult();
			this.previewLabel.setText("Final result");
			disableRefresh();
		} else if (this.previewedThread != null){
			// running task
			preview = (Preview) this.previewedThread.getLatestResultPreview();
			double grabTime = this.previewedThread.getLatestPreviewGrabTimeSeconds();
			String grabString = " (" + StringUtils.secondsToDHMSString(grabTime) + ")";
			if (preview == null) {
				this.previewLabel.setText("No preview available" + grabString);
			} else {
				this.previewLabel.setText("Preview" + grabString);
			}
		} else {
			// no thread
			this.previewLabel.setText("No preview available");
			preview = null;
		}
		
		this.textViewerPanel.setText(preview);
		this.textViewerPanel.setGraph(preview);
    }

    /**
     * Updates the refresh timer by the setting of the "Auto refresh" box.
     */
    private void updateAutoRefreshTimer() {
        int autoDelay = PreviewPanel.autoFreqTimeSecs[this.autoRefreshComboBox.getSelectedIndex()];
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

    /**
     * Disables refreshing.
     */
    private void disableRefresh() {
        this.refreshButton.setEnabled(false);
        this.autoRefreshLabel.setEnabled(false);
        this.autoRefreshComboBox.setEnabled(false);
        this.autoRefreshTimer.stop();
    }

    /**
     * Enables refreshing.
     */
    private void enableRefresh() {
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
