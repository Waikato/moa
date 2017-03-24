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
package moa.gui.active;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import moa.evaluation.ALMeasureCollection;
import moa.evaluation.LearningCurve;
import moa.evaluation.MeasureCollection;
import moa.evaluation.Preview;
import moa.evaluation.PreviewCollection;
import moa.evaluation.PreviewCollectionLearningCurveWrapper;
import moa.gui.FileExtensionFilter;
import moa.gui.GUIUtils;
import moa.gui.PreviewTableModel;
import moa.gui.clustertab.ClusteringVisualEvalPanel;
import moa.gui.visualization.ParamGraphCanvas;
import moa.gui.visualization.ProcessGraphCanvas;
import moa.tasks.FailedTaskReport;
import moa.tasks.active.ALCrossValidationTask;
import moa.tasks.active.ALMultiParamTask;
import moa.tasks.active.ALPrequentialEvaluationTask;

/**
 * This panel displays text. Used to output the results of tasks. In contrast to
 * TastTextViewerPanel, this class supports multiple curves and additionally
 * provides a second graph showing the budget-accuracy relationship.
 *
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 */
public class ALTaskTextViewerPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private static final String EXPORT_FILE_EXTENSION = "txt";

	private JSplitPane mainPane;
	
	private JPanel topWrapper;
	
	private PreviewTableModel previewTableModel;
	
	private JTable previewTable;
	
	private JTextArea errorTextField;
	
	private JScrollPane scrollPaneTable;
	
	private JScrollPane scrollPaneText;
	
	private JButton exportButton;
	
	private JPanel panelEvalOutput;
	
	private MeasureCollection[] measures;
	
	private ClusteringVisualEvalPanel clusteringVisualEvalPanel1;
	
	private GridBagConstraints gridBagConstraints;
	
	private JPanel graphPanel;
	
	private JPanel graphPanelControlLeft;
	
	private JButton buttonZoomInY;
	
	private JButton buttonZoomOutY;
	
	private JLabel labelEvents;
	
	private JTabbedPane graphPanelTabbedPane;
	
	private JScrollPane graphScrollPanel;
	
	private ProcessGraphCanvas graphCanvas;
	
	private JScrollPane paramGraphScrollPanel;
	
	private ParamGraphCanvas paramGraphCanvas;
	
	private JPanel graphPanelControlRight;
	
	private JButton buttonZoomInX;
	
	private JButton buttonZoomOutX;
	
	private double[] variedParamValues;
	
	private Color[] colors;

	public ALTaskTextViewerPanel() {

		setLayout(new GridBagLayout());

		// mainPane contains the two main components of the text viewer panel:
		// top component: text preview panel
		// bottom component: interactive graph panel
		this.mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.mainPane.setDividerLocation(200);

		// topWrapper is the wrapper of the top component of mainPane
		this.topWrapper = new JPanel();
		this.topWrapper.setLayout(new BorderLayout());

		// textArea displays live results in text form
		this.previewTableModel = new PreviewTableModel();
		this.previewTable = new JTable(this.previewTableModel);
		this.previewTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
		this.previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        this.errorTextField = new JTextArea();
        this.errorTextField.setEditable(false);
        this.errorTextField.setFont(new Font("Monospaced", Font.PLAIN, 12));
		
		// scrollPane enables scroll support for textArea
		this.scrollPaneTable = new JScrollPane(this.previewTable, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);
		this.scrollPaneText = new JScrollPane(this.errorTextField, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);
        this.scrollPaneText.setVisible(false);

		this.topWrapper.add(this.scrollPaneTable, BorderLayout.CENTER);

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

		this.topWrapper.add(exportButtonWrapper, BorderLayout.SOUTH);

		this.mainPane.setTopComponent(this.topWrapper);

		// panelEvalOutput contains the bottom component of the mainPane. It consists of a left
		// area showing several performance measures and an area on the right side with a live
		// performance graph
		this.panelEvalOutput = new JPanel();
		this.panelEvalOutput.setLayout(new GridBagLayout());
		this.panelEvalOutput.setBorder(BorderFactory.createTitledBorder("Evaluation"));

		// measures contains the current information about the selected task
		this.measures = new ALMeasureCollection[]{new ALMeasureCollection()};

		this.clusteringVisualEvalPanel1 = new ClusteringVisualEvalPanel();
		this.clusteringVisualEvalPanel1.setMinimumSize(new Dimension(280, 118));
		this.clusteringVisualEvalPanel1.setPreferredSize(new Dimension(290, 115));
		this.clusteringVisualEvalPanel1.setMeasures(this.measures, null, this);

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
					graphCanvas.scaleYResolution(2);
				} else {
					paramGraphCanvas.scaleYResolution(2);
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
					graphCanvas.scaleYResolution(0.5);
				} else {
					paramGraphCanvas.scaleYResolution(0.5);
				}	
			}
		});

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
		graphCanvas = new ProcessGraphCanvas();
		graphCanvas.setPreferredSize(new Dimension(500, 111));
		graphCanvas.setGraph(null, 0, null, 1000, null);

		GroupLayout graphCanvasLayout = new GroupLayout(graphCanvas);
		graphCanvas.setLayout(graphCanvasLayout);
		graphCanvasLayout.setHorizontalGroup(graphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 515, Short.MAX_VALUE));
		graphCanvasLayout.setVerticalGroup(graphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 128, Short.MAX_VALUE));

		graphScrollPanel.setViewportView(graphCanvas);
		graphPanelTabbedPane.addTab("Time", graphScrollPanel);
		
		// budgetGraphScrollPanel is a scroll wrapper for the live budget graph
		paramGraphScrollPanel = new JScrollPane();

		// budgetGraphCanvas displays the live budget graph
		paramGraphCanvas = new ParamGraphCanvas();
		paramGraphCanvas.setPreferredSize(new Dimension(500, 111));
		paramGraphCanvas.setGraph(null, 0, null, null);

		GroupLayout budgetGraphCanvasLayout = new GroupLayout(paramGraphCanvas);
		paramGraphCanvas.setLayout(budgetGraphCanvasLayout);
		budgetGraphCanvasLayout.setHorizontalGroup(budgetGraphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 515, Short.MAX_VALUE));
		budgetGraphCanvasLayout.setVerticalGroup(budgetGraphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 128, Short.MAX_VALUE));

		paramGraphScrollPanel.setViewportView(paramGraphCanvas);
		graphPanelTabbedPane.addTab("Param", paramGraphScrollPanel);
		
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new Insets(2, 2, 2, 2);
		graphPanel.add(graphPanelTabbedPane, gridBagConstraints);
		

		// graphPanelControlRight contains two buttons allowing to zoom the x-axis in and out
		graphPanelControlRight = new JPanel();

		buttonZoomInX = new JButton();
		buttonZoomInX.setText("Zoom in X");
		buttonZoomInX.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// update the currently open graph
				int currentTab = graphPanelTabbedPane.getSelectedIndex();
				if (currentTab == 0) {
					graphCanvas.scaleXResolution(2);
				} else {
					paramGraphCanvas.scaleXResolution(2);
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
					graphCanvas.scaleXResolution(0.5);
				} else {
					paramGraphCanvas.scaleXResolution(0.5);
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
	 * Updates the preview table based on the information given by preview.
	 * @param preview the new information used to update text
	 */
	public void setText(Preview preview) {
		Point p = this.scrollPaneTable.getViewport().getViewPosition();

		previewTableModel.setPreview(preview);
		SwingUtilities.invokeLater(
			new Runnable(){
				boolean structureChanged = previewTableModel.structureChanged();
				public void run(){
					if(!scrollPaneTable.isVisible())
					{
						topWrapper.remove(scrollPaneText);
						scrollPaneText.setVisible(false);
						topWrapper.add(scrollPaneTable, BorderLayout.CENTER);
						scrollPaneTable.setVisible(true);
						topWrapper.validate();
					}
					
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
		
		this.scrollPaneTable.getViewport().setViewPosition(p);
		this.exportButton.setEnabled(preview != null);
	}
	
	/**
	 * Updates the preview table based on the information given by preview.
	 * @param preview the new information used to update text
	 */
	public void setErrorText(FailedTaskReport failedTaskReport) {
		Point p = this.scrollPaneText.getViewport().getViewPosition();


		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					if(!scrollPaneText.isVisible())
					{
						topWrapper.remove(scrollPaneTable);
						scrollPaneTable.setVisible(false);
						topWrapper.add(scrollPaneText, BorderLayout.CENTER);
						scrollPaneText.setVisible(true);
						topWrapper.validate();
					}
					errorTextField.setText(failedTaskReport.toString());
					errorTextField.repaint();
				}
			}
		);
		
		
		this.scrollPaneText.getViewport().setViewPosition(p);
		this.exportButton.setEnabled(failedTaskReport != null);
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
			// set the maximum width which was calculated
			column.setPreferredWidth(width);
		}
	}
	
	/**
	 * Updates the graph based on the information given by the preview.
	 * @param preview information used to update the graph
	 */
	@SuppressWarnings("unchecked")
	public void setGraph(Preview preview, Color[] colors) {
		if (preview == null) {
			// no preview received
			this.graphCanvas.setGraph(null, this.graphCanvas.getMeasureSelected(), null, 1000, null);
			return;
		}

		GraphCanvasMultiParams gcmp = new GraphCanvasMultiParams();
		
		// check which type of task it is
		Class<?> c = preview.getTaskClass();
		if (c == ALCrossValidationTask.class || c == ALMultiParamTask.class) {
			//PreviewCollection
			PreviewCollection<Preview> pc = (PreviewCollection<Preview>) preview;
			
			// set param tab name as the actual name
			this.graphPanelTabbedPane.setTitleAt(1, pc.getVariedParamName());
			// get varied parameter values
			this.variedParamValues = pc.getVariedParamValues();
			
			if (c == ALCrossValidationTask.class) {
				// calculate mean preview collection for each parameter value
				pc = this.calculateMeanPreview(
						(PreviewCollection<PreviewCollection<Preview>>) preview);
			}
			gcmp = readPreviewCollection(pc);
    		
    		if (!this.graphPanelTabbedPane.isEnabledAt(1)) {
    			// enable budget view on multi budget task
    			this.graphPanelTabbedPane.setEnabledAt(1, true);
    		}
    	} else if (c == ALPrequentialEvaluationTask.class) {
    		// Preview
    		gcmp = readPreview(preview);
    		
    		// reset param tab name to default
    		this.graphPanelTabbedPane.setTitleAt(1, "Param");
    		
    		if (this.graphPanelTabbedPane.getSelectedIndex() == 1) {
    			// switch to Time tab
    			this.graphPanelTabbedPane.setSelectedIndex(0);
    		}
    		
    		if (this.graphPanelTabbedPane.isEnabledAt(1)) {
    			// disable budget view on single budget task
    			this.graphPanelTabbedPane.setEnabledAt(1, false);
    		}
    	} else {
    		// sth went wrong
    		this.graphCanvas.setGraph(null, this.graphCanvas.getMeasureSelected(), null, 1000, null);
			return;
    	}
		
		int[] pfs = gcmp.getProcessFrequenciesArray();
		this.measures = gcmp.getMeasureCollectionsArray();
		int min_pf = min(pfs);
		
		this.colors = colors;
		
		this.graphCanvas.setGraph(this.measures, 
				this.graphCanvas.getMeasureSelected(), pfs, min_pf, colors);
		this.paramGraphCanvas.setGraph(this.measures, 
				this.paramGraphCanvas.getMeasureSelected(), 
				this.variedParamValues, colors);
		this.clusteringVisualEvalPanel1.update();

	}

	private static double round(double d) {
		return (Math.rint(d * 100) / 100);
	}
	
	private static int min(int[] l) {
		if (l.length == 0) {
			return 0;
		}
		
		int min = l[0];
		for (int i = 0; i < l.length; i++) {
			if (l[i] < min) {
				min = l[i];
			}
		}
		return min;
	}
	
	/**
	 * Parses a PreviewCollection and return the resulting 
	 * GraphCanvasMultiParams. If the PreviewCollection contains
	 * PreviewCollections again, it recursively adds their results. If it
	 * contains simple Previews, it adds their properties to the result.
	 * @param pc PreviewCollection
	 * @return relevant information contained in the PreviewCollection
	 */
	public GraphCanvasMultiParams readPreviewCollection(PreviewCollection<Preview> pc) {	
		GraphCanvasMultiParams gcmp = new GraphCanvasMultiParams();
		List<Preview> sps = pc.getPreviews();

		if (sps.size() > 0 && sps.get(0) instanceof PreviewCollection) {
			// members are PreviewCollections again
			// NOTE: this assumes that all elements in sps are of the same type
			for (Preview sp: sps) {
				@SuppressWarnings("unchecked")
				GraphCanvasMultiParams tmp = readPreviewCollection((PreviewCollection<Preview>) sp);
				gcmp.add(tmp);
			}
		} else {
			// members are simple previews
			for (Preview sp: sps) {
				GraphCanvasMultiParams tmp = readPreview(sp);
				gcmp.add(tmp);
			}
		}
		
		return gcmp;
	}
	
	/**
	 * GraphCanvasMultiParams represents the parsing results of a preview,
	 * namely the process frequencies and the measure collections.
	 * @author Tim Sabsch (tim.sabsch@ovgu.de)
	 * @version $Revision: 1 $
	 */
	private class GraphCanvasMultiParams {
		private List<Integer> processFrequencies;
		private List<MeasureCollection> measureCollections;
		
		public GraphCanvasMultiParams() {
			this.processFrequencies = new ArrayList<Integer>();
			this.measureCollections = new ArrayList<MeasureCollection>();
		}
		
		public void add(GraphCanvasMultiParams other) {
			this.processFrequencies.addAll(other.getProcessFrequencies());
			this.measureCollections.addAll(other.getMeasureCollections());
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
	 * Parses a preview with respect to the process frequency and several
	 * measurements.
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
		int budgetColumn = -1;

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
			case "Rel Number of Label Acquisitions":
				budgetColumn = i;
				break;
			default:
				break;
			}
		}
		
		List<double[]> data = p.getData();
		MeasureCollection m = new ALMeasureCollection();
		
		// set entries
		for (double[] entry: data) {
			m.addValue(0, round(entry[accuracyColumn]));
			m.addValue(1, round(entry[kappaColumn]));
			m.addValue(2, round(entry[kappaTempColumn]));
			m.addValue(3, Math.abs(entry[ramColumn]));
			m.addValue(4, round(entry[timeColumn]));
			m.addValue(5, round(entry[memoryColumn] / (1024 * 1024)));
			m.addValue(6, round(entry[budgetColumn]));
		}
		
		// determine process frequency
		int processFrequency = (int) data.get(0)[processFrequencyColumn];
		
		GraphCanvasMultiParams gcmp = new GraphCanvasMultiParams();
		gcmp.addMeasureCollection(m);
		gcmp.addProcessFrequency(processFrequency);
		return gcmp;
	}
	
	private PreviewCollection<Preview> calculateMeanPreview(
			PreviewCollection<PreviewCollection<Preview>> rawPreviews) 
	{
		// create new preview collection for mean previews
		PreviewCollection<Preview> meanPreviews = 
				new PreviewCollection<Preview>(
						"mean preview entry id",
						"parameter value id",
						ALCrossValidationTask.class,
						null,
						this.variedParamValues);
		List<PreviewCollection<Preview>> foldPreviews = rawPreviews.getPreviews(); 
		
		// calculate maximal number of entries that each Preview can provide
		int numFolds = foldPreviews.size();
		int numParamValues = this.variedParamValues.length;
		int numEntriesPerPreview = rawPreviews.numEntries() / numFolds / numParamValues;
				
		for (int paramValue = 0; paramValue < numParamValues; paramValue++)
		{
			// initialize list for summing up all measurements
			List<double[]> paramMeasurementsSum = 
					new ArrayList<double[]>(numEntriesPerPreview);
			
			int numCompleteFolds = 0;
			
			for (PreviewCollection<Preview> subPreview : foldPreviews) {
				// check if there is a preview for each parameter value
				// TODO: handle partial cases
				if (subPreview.getPreviews().size() == numParamValues) {
					numCompleteFolds++;
					
					Preview foldParamPreview = subPreview.getPreviews().get(paramValue);
					
					List<double[]> foldParamMeasurements = foldParamPreview.getData();
					
					if (paramMeasurementsSum.isEmpty()) {
						paramMeasurementsSum.addAll(foldParamMeasurements);
					}
					else {
						// add values for each measurement in each entry
						for (int entry = 0; entry < numEntriesPerPreview; entry++) {
							double[] entrySum = paramMeasurementsSum.get(entry);
							double[] foldParamEntry = foldParamMeasurements.get(entry);
							
							for (int measure = 0; measure < entrySum.length; measure++) {
								entrySum[measure] += foldParamEntry[measure];
							}
						}
					}
				}
			}
			
			// divide measurementsSum by number of folds:
			for (double[] entry : paramMeasurementsSum) {
				for (int m = 0; m < entry.length; m++) {
					entry[m] /= numCompleteFolds;
				}
			}
			
			// get actual measurement names (first four are only additional IDs)
			String[] cvMeasurementNames = rawPreviews.getMeasurementNames();
			List<String> measurementNames = 
					new ArrayList<String>(cvMeasurementNames.length - 4);
			for (int m = 4; m < cvMeasurementNames.length; m++) {
				measurementNames.add(cvMeasurementNames[m]);
			}
			
			// wrap into LearningCurve
			LearningCurve meanLearningCurve = 
					new LearningCurve("learning evaluation instances");
			meanLearningCurve.setData(measurementNames, paramMeasurementsSum);
			
			// wrap into PreviewCollectionLearningCurveWrapper
			Preview meanParamValuePreview = 
					new PreviewCollectionLearningCurveWrapper(
							meanLearningCurve,
							rawPreviews.getTaskClass());
			
			meanPreviews.setPreview(paramValue, meanParamValuePreview);
		}
		
		return meanPreviews;
	}

	//TODO understand this
	@Override
	public void actionPerformed(ActionEvent e) {
		int selected = Integer.parseInt(e.getActionCommand());
		int counter = selected;
		int m_select_offset = 0;
		boolean found = false;
		for (int i = 0; i < this.measures.length; i++) {
			for (int j = 0; j < this.measures[i].getNumMeasures(); j++) {
				if (this.measures[i].isEnabled(j)) {
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
		this.graphCanvas.setGraph(this.measures, m_select_offset, this.graphCanvas.getProcessFrequencies(),
				this.graphCanvas.getMinProcessFrequency(), this.colors);
		this.paramGraphCanvas.setGraph(this.measures, m_select_offset, 
				this.variedParamValues, this.colors);
	}
}
