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

import moa.evaluation.ALMeasureCollection;
import moa.evaluation.MeasureCollection;
import moa.evaluation.preview.MeanPreviewCollection;
import moa.evaluation.preview.Preview;
import moa.evaluation.preview.PreviewCollection;
import moa.gui.FileExtensionFilter;
import moa.gui.GUIUtils;
import moa.gui.PreviewTableModel;
import moa.gui.visualization.ParamGraphCanvas;
import moa.gui.visualization.ProcessGraphCanvas;
import moa.tasks.FailedTaskReport;
import moa.tasks.meta.ALMultiParamTask;
import moa.tasks.meta.ALPartitionEvaluationTask;
import moa.tasks.meta.ALPrequentialEvaluationTask;
import nz.ac.waikato.cms.gui.core.BaseFileChooser;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
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

/**
 * This panel displays text. Used to output the results of tasks. In contrast to
 * TastTextViewerPanel, this class supports multiple curves, standard deviations
 * and two additional views, which display a varied parameter and the relative
 * label acquisition rate (usually called budget).
 *
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 */
public class ALTaskTextViewerPanel extends JPanel {

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

	private MeasureOverview measureOverview;

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

	private JScrollPane budgetGraphScrollPanel;

	private ParamGraphCanvas budgetGraphCanvas;

	private JPanel graphPanelControlRight;

	private JButton buttonZoomInX;

	private JButton buttonZoomOutX;

	private String variedParamName;

	private double[] variedParamValues;

	private double[] budgets;

	public ALTaskTextViewerPanel() {

		setLayout(new GridBagLayout());

		// mainPane contains the two main components of the text viewer panel:
		// top component: preview table panel
		// bottom component: interactive graph panel
		this.mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.mainPane.setDividerLocation(200);

		// topWrapper is the wrapper of the top component of mainPane
		this.topWrapper = new JPanel();
		this.topWrapper.setLayout(new BorderLayout());

		// previewTable displays live results in table form 
		// (or in text form, if an error occurs)
		this.previewTableModel = new PreviewTableModel();
		this.previewTable = new JTable(this.previewTableModel);
		this.previewTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
		this.previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        this.errorTextField = new JTextArea();
        this.errorTextField.setEditable(false);
        this.errorTextField.setFont(new Font("Monospaced", Font.PLAIN, 12));

		// scrollPane enables scroll support for previewTable
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
				BaseFileChooser fileChooser = new BaseFileChooser();
				fileChooser.setAcceptAllFileFilterUsed(true);
				fileChooser.addChoosableFileFilter(new FileExtensionFilter(EXPORT_FILE_EXTENSION));
				if (fileChooser.showSaveDialog(ALTaskTextViewerPanel.this) == BaseFileChooser.APPROVE_OPTION) {
					File chosenFile = fileChooser.getSelectedFile();
					String fileName = chosenFile.getPath();
					if (!chosenFile.exists() && !fileName.endsWith(EXPORT_FILE_EXTENSION)) {
						fileName = fileName + "." + EXPORT_FILE_EXTENSION;
					}
					try {
						PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

						String text = "";

						if(scrollPaneTable.isVisible())
						{
							text = previewTableModel.toString();
						}
						else
						{
							text = errorTextField.getText();
						}

						out.write(text);
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

		// init the measure overview. the dummy ALMeasureCollection[] is needed to properly init the measure names
		this.measureOverview = new MeasureOverview(new ALMeasureCollection[]{new ALMeasureCollection()}, "", null);
        this.measureOverview.setMinimumSize(new Dimension(280, 118));
        this.measureOverview.setPreferredSize(new Dimension(290, 115));
        this.measureOverview.setActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int selected = Integer.parseInt(e.getActionCommand());
                graphCanvas.setMeasureSelected(selected);
                graphCanvas.updateCanvas(true);
                paramGraphCanvas.setMeasureSelected(selected);
                paramGraphCanvas.updateCanvas(true);
                budgetGraphCanvas.setMeasureSelected(selected);
                budgetGraphCanvas.updateCanvas(true);
            }
        });

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.weighty = 1.0;

		panelEvalOutput.add(measureOverview, gridBagConstraints);

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
                switch (currentTab){
                    case 0: graphCanvas.scaleYResolution(2); 
                            break;
                    case 1: paramGraphCanvas.scaleYResolution(2); 
                            break;
                    case 2: budgetGraphCanvas.scaleYResolution(2);
                            break;
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
                switch (currentTab){
                    case 0: graphCanvas.scaleYResolution(0.5); 
                            break;
                    case 1: paramGraphCanvas.scaleYResolution(0.5); 
                            break;
                    case 2: budgetGraphCanvas.scaleYResolution(0.5);
                            break;
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
		graphCanvas.setGraph(null, null, null, 1000, null);

		GroupLayout graphCanvasLayout = new GroupLayout(graphCanvas);
		graphCanvas.setLayout(graphCanvasLayout);
		graphCanvasLayout.setHorizontalGroup(graphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 515, Short.MAX_VALUE));
		graphCanvasLayout.setVerticalGroup(graphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 128, Short.MAX_VALUE));

		graphScrollPanel.setViewportView(graphCanvas);
		graphPanelTabbedPane.addTab("Processed Instances", graphScrollPanel);

		// paramGraphScrollPanel is a scroll wrapper for the live param graph
		paramGraphScrollPanel = new JScrollPane();

		// paramGraphCanvas displays the live varied parameter graph
		paramGraphCanvas = new ParamGraphCanvas();
		paramGraphCanvas.setPreferredSize(new Dimension(500, 111));
		paramGraphCanvas.setGraph(null, null, null, null);

		GroupLayout paramGraphCanvasLayout = new GroupLayout(paramGraphCanvas);
		paramGraphCanvas.setLayout(paramGraphCanvasLayout);
		paramGraphCanvasLayout.setHorizontalGroup(paramGraphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 515, Short.MAX_VALUE));
		paramGraphCanvasLayout.setVerticalGroup(paramGraphCanvasLayout
				.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 128, Short.MAX_VALUE));

		paramGraphScrollPanel.setViewportView(paramGraphCanvas);
		graphPanelTabbedPane.addTab("Varied Parameter", paramGraphScrollPanel);

		// budgetGraphScrollPanel is a scroll wrapper for the live budget graph
        budgetGraphScrollPanel = new JScrollPane();

		// budgetGraphCanvas displays the live actually used budget graph
		budgetGraphCanvas = new ParamGraphCanvas();
		budgetGraphCanvas.setPreferredSize(new Dimension(500, 111));
        budgetGraphCanvas.setGraph(null, null, null, null);

        GroupLayout budgetGraphCanvasLayout = new GroupLayout(budgetGraphCanvas);
        budgetGraphCanvas.setLayout(budgetGraphCanvasLayout);
        budgetGraphCanvasLayout.setHorizontalGroup(budgetGraphCanvasLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 515, Short.MAX_VALUE));
        budgetGraphCanvasLayout.setVerticalGroup(budgetGraphCanvasLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 128, Short.MAX_VALUE));

        budgetGraphScrollPanel.setViewportView(budgetGraphCanvas);
        graphPanelTabbedPane.addTab("Label Acq. Rate", budgetGraphScrollPanel);

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
                switch (currentTab){
                    case 0: graphCanvas.scaleXResolution(2); 
                            break;
                    case 1: paramGraphCanvas.scaleXResolution(2); 
                            break;
                    case 2: budgetGraphCanvas.scaleXResolution(2);
                            break;
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
                switch (currentTab){
                    case 0: graphCanvas.scaleXResolution(0.5); 
                            break;
                    case 1: paramGraphCanvas.scaleXResolution(0.5); 
                            break;
                    case 2: budgetGraphCanvas.scaleXResolution(0.5);
                            break;
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
	 * @param preview the new information used to update the table
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
	 * Displays the error message.
	 * @param failedTaskReport error message
	 */
	public void setErrorText(FailedTaskReport failedTaskReport) {
		Point p = this.scrollPaneText.getViewport().getViewPosition();
		
		final String failedTaskReportString = failedTaskReport == null ? 
				"Failed Task Report is null" : failedTaskReport.toString();

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
					errorTextField.setText(failedTaskReportString);
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
	 * @param colors color coding used to connect the tasks with the graphs
	 */
	@SuppressWarnings("unchecked")
	public void setGraph(Preview preview, Color[] colors) {
		if (preview == null) {
			// no preview received
		    this.measureOverview.update(null, "", null);
			this.graphCanvas.setGraph(null, null, null, 1000, null);
			this.paramGraphCanvas.setGraph(null, null, null, null);
			this.budgetGraphCanvas.setGraph(null, null, null, null);
			return;
		}

		ParsedPreview pp;
		ParsedPreview ppStd;

		// check which type of task it is
		Class<?> c = preview.getTaskClass();
		if (c == ALPartitionEvaluationTask.class || c == ALMultiParamTask.class) {
			// PreviewCollection
			PreviewCollection<Preview> pc = (PreviewCollection<Preview>) preview;

	         // get varied parameter name and values
			this.variedParamName = pc.getVariedParamName();
            this.variedParamValues = pc.getVariedParamValues();

			if (c == ALPartitionEvaluationTask.class) {
				// calculate mean preview collection for each parameter value
				MeanPreviewCollection mpc = new MeanPreviewCollection(
						(PreviewCollection<PreviewCollection<Preview>>) preview);
				pp = readCollection(mpc.getMeanPreviews());
				ppStd = readCollection(mpc.getStdPreviews());

				this.graphCanvas.setStandardDeviationPainted(true);
				this.paramGraphCanvas.setStandardDeviationPainted(true);
			} else {
				pp = readCollection(pc);
				ppStd = null;
			    this.graphCanvas.setStandardDeviationPainted(false);
			    this.paramGraphCanvas.setStandardDeviationPainted(false);
			}

			// enable param and budget graph
			this.graphPanelTabbedPane.setEnabledAt(1, true);
            this.graphPanelTabbedPane.setEnabledAt(2, true);

    	} else if (c == ALPrequentialEvaluationTask.class) {
    		// read Preview (in this case, no special preview is required)
    		pp = read(preview);
    		ppStd = null;

    		// reset varied param name and values
    		this.variedParamName = "";
    		this.variedParamValues = null;

   			// switch to Processed Instances tab
    		this.graphPanelTabbedPane.setSelectedIndex(0);

			// disable param and budget view
			this.graphPanelTabbedPane.setEnabledAt(1, false);
            this.graphPanelTabbedPane.setEnabledAt(2, false);

            // disable painting standard deviation
    		this.graphCanvas.setStandardDeviationPainted(false);
            this.paramGraphCanvas.setStandardDeviationPainted(false);

    	} else {
    		// sth went wrong
    	    this.measureOverview.update(null, "", null);
    		this.graphCanvas.setGraph(null, null, null, 1000, null);
    		this.paramGraphCanvas.setGraph(null, null, null, null);
    		this.budgetGraphCanvas.setGraph(null, null, null, null);
			return;
    	}

		MeasureCollection[] measures = pp.getMeasureCollectionsArray();
		MeasureCollection[] measuresStd = null;
		if (ppStd != null) {
			measuresStd = ppStd.getMeasureCollectionsArray();
		}

		// restructure latest budgets to make them readable for the
		// GraphScatter class
		this.budgets = new double[measures.length];
		for (int i = 0; i < measures.length; i++) {
			this.budgets[i] = measures[i].getLastValue(6);
		}

		int[] pfs = pp.getProcessFrequenciesArray();
		int min_pf = min(pfs);

		this.measureOverview.update(measures, this.variedParamName, this.variedParamValues);
		this.graphCanvas.setGraph(measures, measuresStd, pfs, min_pf, colors);
		this.paramGraphCanvas.setGraph(measures, measuresStd, this.variedParamValues, colors);
		this.budgetGraphCanvas.setGraph(measures, measuresStd, this.budgets, colors);
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
	 * ParsedPreview object. If the PreviewCollection contains
	 * PreviewCollections again, it recursively adds their results. If it
	 * contains simple Previews, it adds their properties to the result.
	 * @param pc PreviewCollection
	 * @return relevant information contained in the PreviewCollection
	 */
	public ParsedPreview readCollection(PreviewCollection<Preview> pc) {	
		ParsedPreview pp = new ParsedPreview();
		List<Preview> sps = pc.getPreviews();

		if (sps.size() > 0 && sps.get(0) instanceof PreviewCollection) {
			// members are PreviewCollections again
			// NOTE: this assumes that all elements in sps are of the same type
			for (Preview sp: sps) {
				@SuppressWarnings("unchecked")
				ParsedPreview tmp = readCollection((PreviewCollection<Preview>) sp);
				pp.add(tmp);
			}
		} else {
			// members are simple previews
			for (Preview sp: sps) {
				ParsedPreview tmp = read(sp);
				pp.add(tmp);
			}
		}

		return pp;
	}

	/**
	 * ParsedPreview represents the parsing results of a preview,
	 * namely the process frequencies and the measure collections.
	 * @author Tim Sabsch (tim.sabsch@ovgu.de)
	 * @version $Revision: 1 $
	 */
	private class ParsedPreview {
		private List<Integer> processFrequencies;
		private List<MeasureCollection> measureCollections;

		public ParsedPreview() {
			this.processFrequencies = new ArrayList<Integer>();
			this.measureCollections = new ArrayList<MeasureCollection>();
		}

		public void add(ParsedPreview other) {
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
			// Convert Integer list of process frequencies to int Array
			int[] processFrequenciesArray = new int[this.processFrequencies.size()];
			int i = 0;
			for (Integer freq : this.processFrequencies) {
				processFrequenciesArray[i] = freq.intValue();
				++i;
			}
			return processFrequenciesArray;
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
	private ParsedPreview read(Preview p) {

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
			case "[std] classifications correct (percent)":
				accuracyColumn = i; 
				break;
			case "Kappa Statistic (percent)":
			case "[avg] Kappa Statistic (percent)":
			case "[std] Kappa Statistic (percent)":
				kappaColumn = i;
				break;
			case "Kappa Temporal Statistic (percent)":
			case "[avg] Kappa Temporal Statistic (percent)":
			case "[std] Kappa Temporal Statistic (percent)":
				kappaTempColumn = i;
				break;
			case "model cost (RAM-Hours)":
			case "[std] model cost (RAM-Hours)":
				ramColumn = i;
				break;
			case "evaluation time (cpu seconds)":
			case "total train time":
			case "[std] evaluation time (cpu seconds)":
				timeColumn = i;
				break;
			case "model serialized size (bytes)":
			case "[std] model serialized size (bytes)":
				memoryColumn = i;
				break;
			case "Rel Number of Label Acquisitions":
			case "[std] Rel Number of Label Acquisitions":
				budgetColumn = i;
				break;
			default:
				break;
			}
		}

		List<double[]> data = p.getData();

		MeasureCollection m = new ALMeasureCollection();

		// set entries
		// TODO: obviously this should be changed into a dict
		for (double[] entry: data) {
			m.addValue(0, entry[accuracyColumn]);
			m.addValue(1, entry[kappaColumn]);
			m.addValue(2, entry[kappaTempColumn]);
			m.addValue(3, Math.abs(entry[ramColumn]));
			m.addValue(4, entry[timeColumn]);
			m.addValue(5, entry[memoryColumn] / (1024 * 1024));
			m.addValue(6, entry[budgetColumn]);
    	}

		// determine process frequency
		int processFrequency = (int) data.get(0)[processFrequencyColumn];

		ParsedPreview pp = new ParsedPreview();
		pp.addMeasureCollection(m);
		pp.addProcessFrequency(processFrequency);
		return pp;
	}
}
