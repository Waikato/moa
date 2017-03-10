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
import java.util.ArrayList;
import java.util.List;

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
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import moa.evaluation.MeasureCollection;
import moa.evaluation.Preview;
import moa.evaluation.PreviewCollection;
import moa.gui.PreviewPanel.TypePanel;
import moa.gui.clustertab.ClusteringVisualEvalPanel;
import moa.gui.visualization.BudgetGraphCanvas;
import moa.gui.visualization.GraphCanvasMulti;
import moa.tasks.active.ALCrossValidationTask;
import moa.tasks.active.ALMultiBudgetTask;
import moa.tasks.active.ALPrequentialEvaluationTask;

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
	
	private MeasureCollection[] acc1 = new MeasureCollection[1]; // TODO this is bad
	
	private MeasureCollection[] acc2 = new MeasureCollection[1]; //TODO I'll use this for now as a dummy MC. change later!
	
	private ClusteringVisualEvalPanel clusteringVisualEvalPanel1;
	
	private GridBagConstraints gridBagConstraints;
	
	private JPanel graphPanel;
	
	private JPanel graphPanelControlLeft;
	
	private JButton buttonZoomInY;
	
	private JButton buttonZoomOutY;
	
	private JLabel labelEvents;
	
	private JTabbedPane graphPanelTabbedPane;
	
	private JScrollPane graphScrollPanel;
	
	private GraphCanvasMulti graphCanvas;
	
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
		graphCanvas = new GraphCanvasMulti();
		graphCanvas.setPreferredSize(new Dimension(500, 111));
		// TODO consider not doing this here
		graphCanvas.setGraph(null, 0, null, 1000);

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

		previewTableModel.setPreview(preview);
		SwingUtilities.invokeLater(
			new Runnable(){
				boolean structureChanged = previewTableModel.structureChanged();
				public void run(){
					if(structureChanged)
					{
						previewTableModel.fireTableStructureChanged();
						rescaleTableColumns();
					}
					else
					{
						previewTableModel.fireTableDataChanged();
					}
					previewTable.repaint();
				}
			}
		);
		
		this.scrollPane.getViewport().setViewPosition(p);
		this.exportButton.setEnabled(preview != null);
	}

	private static double round(double d) {
		return (Math.rint(d * 100) / 100);
	}
	
	private static int min(int[] l) {
		if (l.length == 0) {
			return 0;
		}
		
		int min = l[0];
		for (int i: l) {
			if (i < min) {
				min = i;
			}
		}
		return min;
	}
	
	/**
	 * TODO javadoc
	 * @param pc
	 * @param colorOffset
	 * @return
	 */
	public GraphCanvasMultiParams setPreviewCollectionGraph(PreviewCollection<Preview> pc) {	
		GraphCanvasMultiParams gcmp = new GraphCanvasMultiParams();
		List<Preview> sps = pc.getPreviews();
		
		//TODO maybe check each instance?
		if (sps.get(0) instanceof PreviewCollection) {
			// NOTE: this assumes that all elements in sps are of the same class
			for (Preview sp: sps) {
				@SuppressWarnings("unchecked")
				PreviewCollection<Preview> spc = (PreviewCollection<Preview>) sp;
				//TODO
				GraphCanvasMultiParams tmp = setPreviewCollectionGraph(spc);
				gcmp.add(tmp);
			}
		} else {
			int n = sps.size();
			for (int i = 0; i < n; i++) {
				GraphCanvasMultiParams tmp = readPreview(sps.get(i));
				gcmp.add(tmp);
			}
		}
		
		return gcmp;
	}
	
	/**
	 * TODO javadoc
	 * only used to store a measurecollection and a processfrequency
	 * maybe find a better solution?
	 * @author tsabsch
	 *
	 */
	private class GraphCanvasMultiParams {
		private List<Integer> processFrequencies;
		private List<MeasureCollection> measureCollections;
		
		public GraphCanvasMultiParams() {
			this.processFrequencies = new ArrayList<Integer>();
			this.measureCollections = new ArrayList<MeasureCollection>();
		}
		
		public void add(GraphCanvasMultiParams g) {
			this.processFrequencies.addAll(g.getProcessFrequencies());
			this.measureCollections.addAll(g.getMeasureCollections());
		}
		
		public void addProcessFrequency(int pf) {
			this.processFrequencies.add(pf);
		}
		
		public void addMeasureCollection(MeasureCollection mc) {
			this.measureCollections.add(mc);
		}
		
		public List<Integer> getProcessFrequencies() {
			return this.processFrequencies;
		}

		public List<MeasureCollection> getMeasureCollections() {
			return this.measureCollections;
		}
		
		public int[] getProcessFrequenciesArray() {
			return this.processFrequencies.stream().mapToInt(i->i).toArray(); //NOTE: this is Java 8
		}
		public MeasureCollection[] getMeasureCollectionsArray() {
			return this.measureCollections.toArray(new MeasureCollection[this.measureCollections.size()]);
		}
	}
	
	/**
	 * TODO javadoc
	 * TODO consider making this static
	 * @param preview
	 */
	private GraphCanvasMultiParams readPreview(Preview p) {
		
		// find measure columns
		String[] measureNames = p.getMeasurementNames();
		int numMeasures = p.getMeasurementNameCount();
		
		int processFrequencyColumn = -1;
		int accuracyColumn = -1;
		int kappaColumn = -1;
		int kappaTempColumn = -1;
		int ramColumn = -1;
		int timeColumn = -1;
		int memoryColumn = -1;
		// TODO check why some measures have different possible descriptions
		for (int i = 0; i < numMeasures; i++) {
			switch (measureNames[i]) {
			case "learning evaluation instances":
				processFrequencyColumn = i;
				break;
			case "classifications correct (percent)":
			case "[avg] classifications correct (percent)":
				accuracyColumn = i; 
				break;
			case "Kappa Statistic (percent)":
			case "[avg] Kappa Statistic (percent)":
				kappaColumn = i;
				break;
			case "Kappa Temporal Statistic (percent)":
			case "[avg] Kappa Temporal Statistic (percent)":
				kappaTempColumn = i;
				break;
			case "model cost (RAM-Hours)":
				ramColumn = i;
				break;
			case "evaluation time (cpu seconds)":
			case "total train time":
				timeColumn = i;
				break;
			case "model serialized size (bytes)":
				memoryColumn = i;
				break;
			default:
//				System.err.println(measureNames[i]);
				break;
			}
		}
		
		List<double[]> data = p.getData();
		MeasureCollection acc = this.typePanel.getMeasureCollection();
		
		// set entries
		for (double[] entry: data) {
			acc.addValue(0, round(entry[accuracyColumn]));
			acc.addValue(1, round(entry[kappaColumn]));
			acc.addValue(2, round(entry[kappaTempColumn]));
			acc.addValue(3, Math.abs(entry[ramColumn]));
			acc.addValue(4, round(entry[timeColumn]));
			acc.addValue(5, round(entry[memoryColumn] / (1024 * 1024)));
		}
		
		// determine process frequency
		int processFrequency = (int) data.get(0)[processFrequencyColumn];
		
		GraphCanvasMultiParams gcmp = new GraphCanvasMultiParams();
		gcmp.addMeasureCollection(acc);
		gcmp.addProcessFrequency(processFrequency);
		return gcmp;
	}

	/**
	 * Updates the graph based on the information given by <code>preview</code>.
	 * TODO consider budgetgraphcanvas
	 * @param preview  string containing new information used to update the graph
	 */
	@SuppressWarnings("unchecked")
	public void setGraph(Preview preview) {
		if (preview == null) {
			// no preview received
			this.graphCanvas.setGraph(null, 0, null, 1000);
			return;
		}
		
		//TODO implement second measurecollection (new task)
		
		GraphCanvasMultiParams gcmp = new GraphCanvasMultiParams();
		
		// check which type of task it is
		// TODO this can probably be also solved otherwise with out explicit task names
		Class<?> c = preview.getTaskClass();
		if (c == ALCrossValidationTask.class || c == ALMultiBudgetTask.class) {
			// PreviewCollections
    		gcmp = setPreviewCollectionGraph((PreviewCollection<Preview>) preview);
    	} else if (c == ALPrequentialEvaluationTask.class) {
    		// simple Previews
    		gcmp = readPreview(preview);	
    	} else {
    		System.err.println(c.getName());
    	}
		
		int[] pfs = gcmp.getProcessFrequenciesArray();
		this.acc1 = gcmp.getMeasureCollectionsArray();
		int min_pf = min(pfs);
		
		this.graphCanvas.setGraph(this.acc1, this.graphCanvas.getMeasureSelected(), pfs, min_pf);
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

	//TODO understand this
	@Override
	public void actionPerformed(ActionEvent e) {
		int selected = Integer.parseInt(e.getActionCommand());
		int counter = selected;
		int m_select_offset = 0;
		boolean found = false;
		for (int i = 0; i < acc1.length; i++) {
			for (int j = 0; j < acc1[i].getNumMeasures(); j++) {
				if (acc1[i].isEnabled(j)) {
					counter--;
					if (counter < 0) {
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
		this.graphCanvas.setGraph(this.acc1, m_select_offset, this.graphCanvas.getProcessFrequencies(),
				this.graphCanvas.getMinProcessFrequency());
		this.graphCanvas.forceAddEvents();
	}
}
