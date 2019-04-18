/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.gui.experimentertab;

import java.awt.BorderLayout;
import javax.swing.JTabbedPane;
import moa.gui.AbstractTabPanel;

/**
 *
 * @author Alberto
 */
public class ExperimenterTabPanel extends AbstractTabPanel {

    private static final long serialVersionUID = 1L;
    
    protected TaskManagerTabPanel taskTabManagerPanel;

    protected ExpPreviewPanel previewPanel;

    protected JTabbedPane tabs = new JTabbedPane();

    /**
     *Initializes the different tabs of the application
     */
    public ExperimenterTabPanel() {
        //this.taskManagerPanel = new TaskManagerPanel();
        this.taskTabManagerPanel = new TaskManagerTabPanel();
        tabs.addTab("Experiments", this.taskTabManagerPanel);
        tabs.addTab("Summary", this.taskTabManagerPanel.summary);
        tabs.addTab("Plot", this.taskTabManagerPanel.plot);
        tabs.addTab("Analyze", this.taskTabManagerPanel.analizeTab);
        this.previewPanel = new ExpPreviewPanel();
        //this.taskTabManagerPanel.setPreviewPanel(this.previewPanel);
        //this.taskManagerPanel.setPreviewPanel(this.previewPanel);
       // tabs.addTab("Analyze", this.previewPanel);
        setLayout(new BorderLayout());
        add(this.tabs);
        //add(this.previewPanel, BorderLayout.CENTER);
    }

    //returns the string to display as title of the tab
    @Override
    public String getTabTitle() {
        return "Experimenter";
    }

    //a short description (can be used as tool tip) of the tab, or contributor, etc.
    @Override
    public String getDescription() {
        return "MOA Classification";
    }
}
