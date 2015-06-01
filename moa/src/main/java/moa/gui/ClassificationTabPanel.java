/*
 *    ClassificationTabPanel.java
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

import javax.swing.*;
import java.awt.*;

/**
 * This panel allows the user to select and configure a task, and run it.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class ClassificationTabPanel extends AbstractTabPanel implements GUI.OptionPanelAware {

	private static final long serialVersionUID = 1L;

	protected TaskManagerPanel taskManagerPanel;

	protected PreviewPanel previewPanel;

	public ClassificationTabPanel() {
		this.taskManagerPanel = new TaskManagerPanel();
		this.previewPanel = new PreviewPanel();
		this.taskManagerPanel.setPreviewPanel(this.previewPanel);
		setLayout(new BorderLayout());
		add(this.taskManagerPanel, BorderLayout.NORTH);
		add(this.previewPanel, BorderLayout.CENTER);
	}
    @Override
    public void setOptionsPanel(JPanel p) {
        taskManagerPanel.setOptionsPanel(p);
	}

	//returns the string to display as title of the tab
    @Override
	public String getTabTitle() {
		return "Classification";
	}

	//a short description (can be used as tool tip) of the tab, or contributor, etc.
    @Override
	public String getDescription(){
		return "MOA Classification";
	}

}



