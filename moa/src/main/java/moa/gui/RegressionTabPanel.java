/*
 *    RegressionTabPanel.java
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
import java.util.Objects;

import moa.gui.PreviewPanel.TypePanel;

/**
 * This panel allows the user to select and configure a task, and run it.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class RegressionTabPanel extends AbstractTabPanel {

	private static final long serialVersionUID = 1L;

	protected RegressionTaskManagerPanel taskManagerPanel;

	protected PreviewPanel previewPanel;

	public RegressionTabPanel() {
		this.taskManagerPanel = new RegressionTaskManagerPanel();
		if (Objects.equals(this.taskManagerPanel.currentTask.getTaskName(),
				"EvaluatePrequentialPredictionIntervals")){
			this.previewPanel = new PreviewPanel(TypePanel.PREDICTIONINTERVAL);
		}
		else{
			this.previewPanel = new PreviewPanel(TypePanel.REGRESSION);
		}
		this.taskManagerPanel.setPreviewPanel(this.previewPanel);
		setLayout(new BorderLayout());
		add(this.taskManagerPanel, BorderLayout.NORTH);
		add(this.previewPanel, BorderLayout.CENTER);
	}

	//returns the string to display as title of the tab
    @Override
	public String getTabTitle() {
		return "Regression";
	}

	//a short description (can be used as tool tip) of the tab, or contributor, etc.
    @Override
	public String getDescription(){
		return "MOA Regression";
	}

}



