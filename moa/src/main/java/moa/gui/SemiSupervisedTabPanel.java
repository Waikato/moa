package moa.gui;

import java.awt.*;

public class SemiSupervisedTabPanel extends AbstractTabPanel {

    protected SemiSupervisedTaskManagerPanel taskManagerPanel;

    protected PreviewPanel previewPanel;

    public SemiSupervisedTabPanel() {
        this.taskManagerPanel = new SemiSupervisedTaskManagerPanel();
        this.previewPanel = new PreviewPanel();
        this.taskManagerPanel.setPreviewPanel(this.previewPanel);
        setLayout(new BorderLayout());
        add(this.taskManagerPanel, BorderLayout.NORTH);
        add(this.previewPanel, BorderLayout.CENTER);
    }

    @Override
    public String getTabTitle() {
        return "Semi-Supervised Learning";
    }

    @Override
    public String getDescription() {
        return "MOA Semi-Supervised Learning";
    }
}
