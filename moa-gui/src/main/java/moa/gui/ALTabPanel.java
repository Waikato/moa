/*
 *    ALTabPanel.java
 *    Original Work: Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    Modified Work: Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim dot sabsch at ovgu dot de)
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

import moa.gui.active.ALPreviewPanel;
import moa.gui.active.ALTaskManagerPanel;

/**
 * This panel allows the user to select and configure a task, and run it.
 *
 * @author Tim Sabsch (tim dot sabsch at ovgu dot de)
 * @version $Revision: 1 $
 */
public class ALTabPanel extends AbstractTabPanel {

	private static final long serialVersionUID = 1L;

	protected ALTaskManagerPanel taskManagerPanel;

	protected ALPreviewPanel previewPanel;

	public ALTabPanel() {
		this.taskManagerPanel = new ALTaskManagerPanel();
		this.previewPanel = new ALPreviewPanel();
		this.taskManagerPanel.setPreviewPanel(this.previewPanel);
		setLayout(new BorderLayout());
		add(this.taskManagerPanel, BorderLayout.NORTH);
		add(this.previewPanel, BorderLayout.CENTER);
	}

	//returns the string to display as title of the tab
    @Override
	public String getTabTitle() {
		return "Active Learning";
	}

	//a short description (can be used as tool tip) of the tab, or contributor, etc.
    @Override
	public String getDescription(){
		return "MOA Active Learning";
	}

}



