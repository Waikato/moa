/*
 *    TaskLauncher.java
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

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * The old main class for the MOA gui, now the main class is <code>GUI</code>.
 * Lets the user configure
 * tasks, learners, streams, and perform data stream analysis.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class TaskLauncher extends JPanel {

	private static final long serialVersionUID = 1L;

	protected TaskManagerPanel taskManagerPanel;

	protected PreviewPanel previewPanel;

	public TaskLauncher() {
		this.taskManagerPanel = new TaskManagerPanel();
		this.previewPanel = new PreviewPanel();
		this.taskManagerPanel.setPreviewPanel(this.previewPanel);
		setLayout(new BorderLayout());
		add(this.taskManagerPanel, BorderLayout.NORTH);
		add(this.previewPanel, BorderLayout.CENTER);
	}

	private static void createAndShowGUI() {

		// Create and set up the window.
		JFrame frame = new JFrame("MOA Task Launcher");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create and set up the content pane.
		JPanel panel = new TaskLauncher();
		panel.setOpaque(true); // content panes must be opaque
		frame.setContentPane(panel);

		// Display the window.
		frame.pack();
		frame.setSize(640, 480);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		try {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					createAndShowGUI();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
