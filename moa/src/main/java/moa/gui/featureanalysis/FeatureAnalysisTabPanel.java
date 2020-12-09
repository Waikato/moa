/*
 *    FeatureAnalysisTabPanel.java
 *    Copyright (C) 2020 University of Waikato, Hamilton, New Zealand
 *    @author Yongheng Ma (2560653665@qq.com)
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
package moa.gui.featureanalysis;

import moa.gui.AbstractTabPanel;
import javax.swing.*;
import java.awt.*;

/**
 * FeatureAnalysis module panel.
 * When user clicks module menu FeatureAnalysis in MOA, it shows VisualizeFeatures tab and FeatureImportance tab.
 */
public class FeatureAnalysisTabPanel extends AbstractTabPanel {

    private static final long serialVersionUID = 1L;

    protected VisualizeFeaturesPanel visualizeFeaturesPanel =new VisualizeFeaturesPanel();

    /**
     * Use Singleton design pattern to ensure the object created here and
     * the object created in DataAnalysisPanel.java are the same object in memory.
     */
    protected FeatureImportancePanel fip=FeatureImportancePanel.getInstance();

    protected JTabbedPane tabs = new JTabbedPane();

    public FeatureAnalysisTabPanel() {

        tabs.addTab("VisualizeFeatures", visualizeFeaturesPanel);
        tabs.addTab("FeatureImportance",fip);
        setLayout(new BorderLayout());
        add(tabs);
    }

    @Override
    public String getTabTitle() {
        return "Feature Analysis";
    }

    @Override
    public String getDescription() {
        return "MOA Feature Analysis";
    }
}
