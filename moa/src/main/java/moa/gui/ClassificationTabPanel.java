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
public class ClassificationTabPanel extends MOATabPanel  {

	private static final long serialVersionUID = 1L;

	protected PreviewPanel previewPanel = new PreviewPanel();

	protected TaskManagerPanel taskManagerPanel = new TaskManagerPanel();

	JPanel content = new JPanel(new BorderLayout());

	public ClassificationTabPanel() {
		super();

		this.taskManagerPanel.setPreviewPanel(this.previewPanel);

		content.add(this.taskManagerPanel, BorderLayout.NORTH);
		content.add(this.previewPanel, BorderLayout.CENTER);


	}

	@Override
	JComponent getContentPanel() {
		return content;
	}

	@Override
	JComponent getOptionsPanel() {
		return taskManagerPanel.getOptionsPanel();
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



