/*
 *    ALTaskTextViewerPanel.java
 *    Original Work: Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Jansen (moa@cs.rwth-aachen.de)
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import moa.evaluation.MeasureCollection;
import moa.evaluation.Preview;
import moa.gui.PreviewPanel.TypePanel;
import moa.gui.clustertab.ClusteringVisualEvalPanel;
import moa.gui.visualization.BudgetGraphCanvas;
import moa.gui.visualization.GraphCanvas;

/*
 * TODO it would be nice if the graphs are reset by changing the tab. this
 * would probably require overriding an actionperformed on the jtabbedpane
 */

/*
 * TODO maybe make graphcanvas and budgetgraphcanvas extending an abstract
 * class graphcanvas so that the zoom button actions are shorter. but this
 * would also require renaming the graphcanvas class.
 */
/*
 * TODO everywhere 'this.'
 */

/*
 * TODO disable budget task if single budget task is evaluated.
 */

/*
 * TODO implement scaling on x axis for budgets
 */

/**
 * This panel displays text. Used to output the results of tasks. In contrast to
 * TastTextViewerPanel, this class additionally provides a second graph showing
 * the budget-accuracy relationship.
 *
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 */
public class ALTaskTextViewerPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private static final String EXPORT_FILE_EXTENSION = "txt";

	private TypePanel typePanel;

	private JSplitPane mainPane;
	
	private JPanel topWrapper;
	
	private PreviewTableModel previewTableModel;
	
	private JTable previewTable;
	
	private JScrollPane scrollPane;
	
	private JButton exportButton;
	
	private JPanel panelEvalOutput;
	
	private MeasureCollection[] acc1 = new MeasureCollection[1];
	
	private MeasureCollection[] acc2 = new MeasureCollection[1];
	
	private ClusteringVisualEvalPanel clusteringVisualEvalPanel1;
	
	private GridBagConstraints gridBagConstraints;
	
	private JPanel graphPanel;
	
	private JPanel graphPanelControlLeft;
	
	private JButton buttonZoomInY;
	
	private JButton buttonZoomOutY;
	
	private JLabel labelEvents;
	
	private JTabbedPane graphPanelTabbedPane;
	
	private JScrollPane graphScrollPanel;
	
	private GraphCanvas graphCanvas;
	
	private JScrollPane budgetGraphScrollPanel;
	
	private BudgetGraphCanvas budgetGraphCanvas;
	
	private JPanel graphPanelControlRight;
	
	private JButton buttonZoomInX;
	
	private JButton buttonZoomOutX;

	public ALTaskTextViewerPanel() {
		// TODO maybe smarter solution than this
		this.typePanel = TypePanel.ACTIVE;

		setLayout(new GridBagLayout());

		// mainPane contains the two main components of the text viewer panel:
		// top component: text preview panel
		// bottom component: interactive graph panel
		mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPane.setDividerLocation(200);

		// topWrapper is the wrapper of the top component of mainPane
		topWrapper = new JPanel();
		topWrapper.setLayout(new BorderLayout());

		// textArea displays live results in text form
		this.previewTableModel = new PreviewTableModel();
		this.previewTable = new JTable(previewTableModel);
		this.previewTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
		this.previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		// scrollPane enables scroll support for textArea
		this.scrollPane = new JScrollPane(this.previewTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		topWrapper.add(this.scrollPane, BorderLayout.CENTER);

		// exportButtonPanel is a wrapper for the export button
		JPanel exportButtonWrapper = new JPanel();
		exportButtonWrapper.setLayout(new GridLayout(1, 2));

		// exportButton provides the feature of exporting results to a .txt file
		this.exportButton = new JButton("Export as .txt file...");
		this.exportButton.setEnabled(false);

		this.exportButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setAcceptAllFileFilterUsed(true);
				fileChooser.addChoosableFileFilter(new FileExtensionFilter(EXPORT_FILE_EXTENSION));
				if (fileChooser.showSaveDialog(ALTaskTextViewerPanel.this) == JFileChooser.APPROVE_OPTION) {
					File chosenFile = fileChooser.getSelectedFile();
					String fileName = chosenFile.getPath();
					if (!chosenFile.exists() && !fileName.endsWith(EXPORT_FILE_EXTENSION)) {
						fileName = fileName + "." + EXPORT_FILE_EXTENSION;
					}
					try {
						PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
						out.write(previewTableModel.toString());
						out.close();
					} catch (IOException ioe) {
						GUIUtils.showExceptionDialog(ALTaskTextViewerPanel.this.exportButton,
								"Problem saving file " + fileName, ioe);
					}
				}
			}
		});

		exportButtonWrapper.add(this.exportButton);

		topWrapper.add(exportButtonWrapper, BorderLayout.SOUTH);

		mainPane.setTopComponent(topWrapper);

		// TODO rename. 
		// panelEvalOutput contains the bottom component of the mainPane. It consists of a left
		// area showing several performance measures and an area on the right side with a live
		// performance graph
		panelEvalOutput = new JPanel();
		panelEvalOutput.setLayout(new GridBagLayout());
		panelEvalOutput.setBorder(BorderFactory.createTitledBorder("Evaluation"));

		// TODO understand differences and usage of acc1 and acc2
		acc1[0] = typePanel.getMeasureCollection();
		acc2[0] = typePanel.getMeasureCollection();

		// TODO is this check necessary?
		// clusteringVisualEvalPanel1 is the left area of panelEvalOutput, showing several
		// performance measures
		if (clusteringVisualEvalPanel1 != null) {
			panelEvalOutput.remove(clusteringVisualEvalPanel1);
		}
		clusteringVisualEvalPanel1 = new ClusteringVisualEvalPanel();
		clusteringVisualEvalPanel1.setMinimumSize(new Dimension(280, 118));
		clusteringVisualEvalPanel1.setPreferredSize(new Dimension(290, 115));
		clusteringVisualEvalPanel1.setMeasures(acc1, acc2, this);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.weighty = 1.0;

		panelEvalOutput.add(clusteringVisualEvalPanel1, gridBagConstraints);

		// graphPanel is the right area of panelEvalOutput, showing a live preview of the
		// performance
		graphPanel = new JPanel();
		graphPanel.setLayout(new GridBagLayout());
		graphPanel.setBorder(BorderFactory.createTitledBorder("Plot"));
		graphPanel.setPreferredSize(new Dimension(530, 115));

		// graphPanelControlLeft contains two buttons allowing to zoom the y-axis in and out
		graphPanelControlLeft = new JPanel();
		graphPanelControlLeft.setLayout(new GridBagLayout());

		buttonZoomInY = new JButton();
		buttonZoomInY.setText("Zoom in Y");
		buttonZoomInY.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// update the currently open graph
				int currentTab = graphPanelTabbedPane.getSelectedIndex();
				if (currentTab == 0) {
					graphCanvas.setSize(new Dimension(
							graphCanvas.getWidth(), 
							(int) (graphCanvas.getHeight() * 1.2)));
					graphCanvas.setPreferredSize(new Dimension(
							graphCanvas.getWidth(), 
							(int) (graphCanvas.getHeight() * 1.2)));
					graphCanvas.updateCanvas(true);
				} else {
					budgetGraphCanvas.setSize(new Dimension(
							budgetGraphCanvas.getWidth(), 
							(int) (budgetGraphCanvas.getHeight() * 1.2)));
					budgetGraphCanvas.setPreferredSize(new Dimension(
							budgetGraphCanvas.getWidth(), 
							(int) (budgetGraphCanvas.getHeight() * 1.2)));
					budgetGraphCanvas.updateCanvas(true);
				}	
			}
		});

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.insets = new Insets(0, 2, 0, 2);
		graphPanelControlLeft.add(buttonZoomInY, gridBagConstraints);

		buttonZoomOutY = new JButton();
		buttonZoomOutY.setText("Zoom out Y");
		buttonZoomOutY.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// update the currently open graph
				int currentTab = graphPanelTabbedPane.getSelectedIndex();
				if (currentTab == 0) {
					graphCanvas.setSize(new Dimension(
							graphCanvas.getWidth(), 
							(int) (graphCanvas.getHeight() * 0.8)));
					graphCanvas.setPreferredSize(new Dimension(
							graphCanvas.getWidth(), 
							(int) (graphCanvas.getHeight() * 0.8)));
					graphCanvas.updateCanvas(true);
				} else {
					budgetGraphCanvas.setSize(new Dimension(
							budgetGraphCanvas.getWidth(), 
							(int) (budgetGraphCanvas.getHeight() * 0.8)));
					budgetGraphCanvas.setPreferredSize(new Dimension(
							budgetGraphCanvas.getWidth(), 
							(int) (budgetGraphCanvas.getHeight() * 0.8)));
					budgetGraphCanvas.updateCanvas(true);
				}	
			}
		});
		// TODO is redefinition of gridBagConstraints necessary?
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.insets = new Insets(0, 2, 0, 2);
		graphPanelControlLeft.add(buttonZoomOutY, gridBagConstraints);

		// dummy variable
		labelEvents = new JLabel();
		labelEvents.setHorizontalAlignment(SwingConstants.CENTER);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new Insets(0, 2, 0, 2);
		graphPanelControlLeft.add(labelEvents, gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.weightx = 1.0;
		graphPanel.add(graphPanelControlLeft, gridBagConstraints);
		
		graphPanelTabbedPane = new JTabbedPane();

		// graphScrollPanel is a scroll wrapper for the live graph
		graphScrollPanel = new JScrollPane();

		// graphCanvas displays the live graph
		graphCanvas = new GraphCanvas();
		graphCanvas.setPreferredSize(new Dimension(500, 111));
		graphCanvas.setGraph(acc1[0], acc2[0], 0, 1000);

		GroupLayout graphCanvasLayout = new GroupLayout(graphCanvas);
		graphCanvas.setLayout(graphCanvasLayout);
		graphCanvasLayout.setHorizontalGroup(graphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 515, Short.MAX_VALUE));
		graphCanvasLayout.setVerticalGroup(graphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 128, Short.MAX_VALUE));

		graphScrollPanel.setViewportView(graphCanvas);
		graphPanelTabbedPane.addTab("Time", graphScrollPanel);
		
		// budgetGraphScrollPanel is a scroll wrapper for the live budget graph
		budgetGraphScrollPanel = new JScrollPane();

		// budgetGraphCanvas displays the live budget graph
		budgetGraphCanvas = new BudgetGraphCanvas();
		budgetGraphCanvas.setPreferredSize(new Dimension(500, 111));
		// TODO check this
		budgetGraphCanvas.setGraph(acc1, acc2, 0);

		// TODO check necessity of this. maybe we can just take the layout above
		GroupLayout budgetGraphCanvasLayout = new GroupLayout(budgetGraphCanvas);
		budgetGraphCanvas.setLayout(budgetGraphCanvasLayout);
		budgetGraphCanvasLayout.setHorizontalGroup(budgetGraphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 515, Short.MAX_VALUE));
		budgetGraphCanvasLayout.setVerticalGroup(budgetGraphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 128, Short.MAX_VALUE));

		budgetGraphScrollPanel.setViewportView(budgetGraphCanvas);
		graphPanelTabbedPane.addTab("Budget", budgetGraphScrollPanel);
		
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new Insets(2, 2, 2, 2);
		graphPanel.add(graphPanelTabbedPane, gridBagConstraints);
		

		// TODO rename?
		// graphPanelControlBottom contains two buttons allowing to zoom the x-axis in and out
		graphPanelControlRight = new JPanel();

		buttonZoomInX = new JButton();
		buttonZoomInX.setText("Zoom in X");
		buttonZoomInX.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// update the currently open graph
				int currentTab = graphPanelTabbedPane.getSelectedIndex();
				if (currentTab == 0) {
					graphCanvas.scaleXResolution(false);
				} else {
//					budgetGraphCanvas.scaleXResolution(false);
				}
			}
		});
		graphPanelControlRight.add(buttonZoomInX);

		buttonZoomOutX = new JButton();
		buttonZoomOutX.setText("Zoom out X");
		buttonZoomOutX.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// update the currently open graph
				int currentTab = graphPanelTabbedPane.getSelectedIndex();
				if (currentTab == 0) {
					graphCanvas.scaleXResolution(true);
				} else {
//					budgetGraphCanvas.scaleXResolution(true);
				}
			}
		});
		graphPanelControlRight.add(buttonZoomOutX);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = GridBagConstraints.EAST;
		graphPanel.add(graphPanelControlRight, gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.weightx = 2.0;
		gridBagConstraints.weighty = 1.0;
		panelEvalOutput.add(graphPanel, gridBagConstraints);

		mainPane.setBottomComponent(panelEvalOutput);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		add(mainPane, gridBagConstraints);

	}
	
	/**
	 * Updates the TaskTextViewerPanel by adding the new text to the text area and updating the live
	 * graph.
	 * @param newText  the new information used to update text and graph
	 */
	public void setText(Preview preview) {
		Point p = this.scrollPane.getViewport().getViewPosition();
		this.previewTableModel.setPreview(preview);
		if(previewTableModel.structureChanged())
		{
			rescaleTableColumns();
		}
		this.previewTable.repaint();
		this.scrollPane.getViewport().setViewPosition(p);
		this.exportButton.setEnabled(preview != null);
	}

	private double round(double d) {
		return (Math.rint(d * 100) / 100);
	}

	/**
	 * Updates the graph based on the information given by <code>preview</code>.
	 * TODO consider budgetgraphcanvas
	 * @param preview  string containing new information used to update the graph
	 */
	public void setGraph(Preview _preview) {
//		// check which type of task it is
//		Class<?> c = preview.getTaskClass();
//		if (c == ALCrossValidationTask.class) {
//    		//TODO set text and graph
////    		this.pre
////    		setGraph(newText);
//    	} else if (c == ALMultiBudgetTask.class) {
//    		//TODO set text and graph
//    	} else if (c == ALPrequentialEvaluationTask.class) {
//    		//TODO set text and graph
//    	} else {
//    		System.err.println(c.getName());
//    	}
		String preview = _preview != null ? _preview.toString() : null;
		// Change the graph when there is change in the text
		double processFrequency = 1000;
		if (preview != null && !preview.equals("")) {
			MeasureCollection oldAccuracy = acc1[0];
			acc1[0] = this.typePanel.getMeasureCollection();
			Scanner scanner = new Scanner(preview);
			String firstLine = scanner.nextLine();
			boolean isSecondLine = true;

			boolean isPrequential = firstLine.startsWith("learning evaluation instances,evaluation time");
			boolean isHoldOut = firstLine.startsWith("evaluation instances,to");
			int accuracyColumn = 6;
			int kappaColumn = 4;
			int RamColumn = 2;
			int timeColumn = 1;
			int memoryColumn = 9;
			int kappaTempColumn = 5;
			
			if (isPrequential || isHoldOut) {
				accuracyColumn = 4;
				kappaColumn = 5;
				RamColumn = 2;
				timeColumn = 1;
				memoryColumn = 7;
				kappaTempColumn = 5;
				String[] tokensFirstLine = firstLine.split(",");

				// NOTE either the lines above or below are redundant?
				// TODO check necessity
				int i = 0;
				for (String s : tokensFirstLine) {
					if (s.equals("classifications correct (percent)")
							|| s.equals("[avg] classifications correct (percent)")) {
						accuracyColumn = i;
					} else if (s.equals("Kappa Statistic (percent)") || s.equals("[avg] Kappa Statistic (percent)")) {
						kappaColumn = i;
					} else if (s.equals("Kappa Temporal Statistic (percent)")
							|| s.equals("[avg] Kappa Temporal Statistic (percent)")) {
						kappaTempColumn = i;
					} else if (s.equals("model cost (RAM-Hours)")) {
						RamColumn = i;
					} else if (s.equals("evaluation time (cpu seconds)") || s.equals("total train time")) {
						timeColumn = i;
					} else if (s.equals("model serialized size (bytes)")) {
						memoryColumn = i;
					}
					i++;
				}
			}
			
			// update measures
			if (isPrequential || isHoldOut) {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					String[] tokens = line.split(",");
					this.acc1[0].addValue(0, round(parseDouble(tokens[accuracyColumn])));
					this.acc1[0].addValue(1, round(parseDouble(tokens[kappaColumn])));
					this.acc1[0].addValue(2, round(parseDouble(tokens[kappaTempColumn])));
					if (!isHoldOut) {
						this.acc1[0].addValue(3, Math.abs(parseDouble(tokens[RamColumn])));
					}
					this.acc1[0].addValue(4, round(parseDouble(tokens[timeColumn])));
					this.acc1[0].addValue(5, round(parseDouble(tokens[memoryColumn]) / (1024 * 1024)));

					if (isSecondLine == true) {
						processFrequency = Math.abs(parseDouble(tokens[0]));
						isSecondLine = false;
						if (acc1[0].getValue(0, 0) != oldAccuracy.getValue(0, 0)) {

							// If we are in a new task, compare with the
							// previous
							if (processFrequency == this.graphCanvas.getProcessFrequency()) {
								acc2[0] = oldAccuracy;
							}
						}
					}
				}
			} else {
				this.acc2[0] = this.typePanel.getMeasureCollection();
			}	
			scanner.close();
			
		} else {
			this.acc1[0] = this.typePanel.getMeasureCollection();
			this.acc2[0] = this.typePanel.getMeasureCollection();
		}

		this.graphCanvas.setGraph(acc1[0], acc2[0], this.graphCanvas.getMeasureSelected(), (int) processFrequency);
		this.graphCanvas.updateCanvas(true);
		this.clusteringVisualEvalPanel1.update();

	}
	
	private void rescaleTableColumns()
	{
		// iterate over all columns to resize them individually
		TableColumnModel columnModel = previewTable.getColumnModel();
		for(int columnIdx = 0; columnIdx < columnModel.getColumnCount(); ++columnIdx)
		{
			// get the current column
			TableColumn column = columnModel.getColumn(columnIdx);
			// get the renderer for the column header to calculate the preferred with for the header
			TableCellRenderer renderer = column.getHeaderRenderer();
			// check if the renderer is null
			if(renderer == null)
			{
				// if it is null use the default renderer for header
				renderer = previewTable.getTableHeader().getDefaultRenderer();
			}
			// create a cell to calculate its preferred size
			Component comp = renderer.getTableCellRendererComponent(previewTable, column.getHeaderValue(), false, false, 0, columnIdx);
			int width = comp.getPreferredSize().width;
//			// iterate over all rows to get the maximum with needed to show all entries completely
//			for(int rowIdx = 0; rowIdx < previewTable.getRowCount(); ++rowIdx)
//			{
//				// get the renderer used by the cell
//				renderer = previewTable.getCellRenderer(rowIdx, columnIdx);
//				// get the component for the cell
//				comp = previewTable.prepareRenderer(renderer, rowIdx, columnIdx);
//				// calculate the maximum of the preferred size of the current cell and the previously calculated width 
//				width = Math.max(width, comp.getPreferredSize().width + 1);
//			}
			// set the maximum width which was calculated
			column.setPreferredWidth(width);
		}
	}
	
	private double parseDouble(String s) {
		double ret = 0;
		if (s.equals("?") == false) {
			ret = Double.parseDouble(s);
		}
		return ret;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// react on graph selection and find out which measure was selected
		int selected = Integer.parseInt(e.getActionCommand());
		int counter = selected;
		int m_select = 0;
		int m_select_offset = 0;
		boolean found = false;
		for (int i = 0; i < acc1.length; i++) {
			for (int j = 0; j < acc1[i].getNumMeasures(); j++) {
				if (acc1[i].isEnabled(j)) {
					counter--;
					if (counter < 0) {
						m_select = i;
						m_select_offset = j;
						found = true;
						break;
					}
				}
			}
			if (found) {
				break;
			}
		}
		this.graphCanvas.setGraph(acc1[m_select], acc2[m_select], m_select_offset,
				this.graphCanvas.getProcessFrequency());
		this.graphCanvas.forceAddEvents();
	}
}
