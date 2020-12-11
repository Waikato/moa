/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *   VisualizeFeaturesPanel.java
 *   Copyright (C) 2003-2020 University of Waikato, Hamilton, New Zealand
 */

package moa.gui.featureanalysis;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.SamoaToWekaInstanceConverter;
import com.yahoo.labs.samoa.instances.WekaToSamoaInstanceConverter;
import moa.streams.ArffFileStream;
import weka.core.*;
import weka.core.converters.AbstractFileSaver;
import weka.filters.AllFilter;
import weka.filters.Filter;
import weka.filters.SupervisedFilter;
import weka.filters.unsupervised.attribute.Remove;
import weka.gui.*;
import weka.gui.explorer.PreprocessPanel;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is VisualizeFeatures tab main panel which loads data stream and shows other sub panels.
 * This panel refers to weka.gui.explorer.PreprocessPanel.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Yongheng Ma (2560653665@qq.com)
 */
public class VisualizeFeaturesPanel extends AbstractPerspective {

    /** for serialization */
    private static final long serialVersionUID = 1L;

    /** Displays simple stats on the working instances */
    protected InstancesSummaryPanel m_InstSummaryPanel =
            new InstancesSummaryPanel();

    /** Click to load base instances from a file */
    protected JButton m_OpenFileBut = new JButton("Open file...");

    /** Panel to let the user toggle attributes */
    protected AttributeSelectionPanel m_AttPanel = new AttributeSelectionPanel();

    /** Button for removing attributes */
    protected JButton m_RemoveButton = new JButton("Remove");

    /** Click to apply filters and save the results */
    protected JButton m_SaveBut = new JButton("Save...");

    /** Click to amplify line graph or scatter diagram so that user can see plot more clearly*/
    protected JButton m_plotAmplify=new JButton("plotAmplify");

    /**The start instance index label to prompt user to input start index number */
    protected JLabel m_startIndex = new JLabel("startIndex:");

    /**The start instance index label to prompt user to input end index number */
    protected JLabel m_endIndex = new JLabel("endIndex:");

    /**The start instance index of x axis for line graph or scatter diagram */
    protected int m_intStartIndex;

    /**The end instance index of x axis for line graph or scatter diagram */
    protected int m_intEndIndex;

    /**Visualize all line graphs or scatter diagrams, not histograms or bar charts*/
    protected JButton m_visAllGraphBut = new JButton("Visualize All");

    /**Format m_intStartIndex*/
    protected JFormattedTextField m_startInstanceInput=new JFormattedTextField(NumberFormat.getIntegerInstance());

    /**Format m_intEndIndex*/
    protected JFormattedTextField m_endInstanceInput=new JFormattedTextField(NumberFormat.getIntegerInstance());

    /** Displays summary stats on the selected attribute */
    protected AttributeSummaryPanel m_AttSummaryPanel =
            new AttributeSummaryPanel();

    /** The file chooser for selecting data files */
    protected ConverterFileChooser m_FileChooser;

    /** The working instances */
    protected Instances m_Instances;

    /** The visualization of the attribute values */
    protected AttributeVisualizationPanel m_AttVisualizePanel =
            new AttributeVisualizationPanel();

    /**
     * Manages sending notifications to people when we change the set of working
     * instances.
     */
    protected PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

    /** A thread for loading/saving instances from a file or URL */
    protected Thread m_IOThread;

    /** The message logger */
    protected Logger m_Log = new SysErrLog();

    /**Instance converter from Samoa instance to Weak Instance*/
    protected SamoaToWekaInstanceConverter m_samoaToWekaInstanceConverter;

    /**Instance converter from Weak instance to Samoa Instance*/
    protected WekaToSamoaInstanceConverter m_wekaToSamoaInstanceConverter;

    /** plot type drop list:
     *          "plot type: Line graph"
     *          "plot type: Scatter diagram"
     *          "No plot type"
     */
    protected JComboBox m_plotTypeBox;

    /** The index of the selected plot type index*/
    protected int m_selectedPlotTypeIndex;

    /** Set feature range shown in a popup window
     *  In default, Nine plots is shown in every popup window at most.
     */
    protected JComboBox m_featureRangeBox;

    /**
     * For line graph or scatter diagram, the default max instance index is 500.
     * In other words, the default x axis is from 1 to 500 if the total instance number is bigger than 500,
     * or the default x axis is from 1 to numInstance if the total instance number is no more than 500.
     * Why: let the graph be more clear.
     */
    private static final int m_defaultMaxInstanceIndex=500;

    /** This panel is used to draw line graphs or scatter diagrams */
    protected LineAndScatterPanel m_graphPanel =new LineAndScatterPanel();

    /**This is the FeatureImportance Tab panel */
    protected FeatureImportancePanel fip=FeatureImportancePanel.getInstance();

    /** For sending instances to various perspectives/tabs */
    protected JMenu m_sendToPerspective;

    /**
     * Creates the instances panel with no initial instances.
     */
    public VisualizeFeaturesPanel() {

        this.m_samoaToWekaInstanceConverter=new SamoaToWekaInstanceConverter();
        this.m_wekaToSamoaInstanceConverter =new WekaToSamoaInstanceConverter();
        m_FileChooser = new ConverterFileChooser();
        m_FileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        m_OpenFileBut.setToolTipText("Open a set of instances from a file");
        m_OpenFileBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setInstancesFromFileQ();
            }
        });

        m_SaveBut.setToolTipText("Save the working relation to a file");
        m_SaveBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveWorkingInstancesToFileQ();
            }
        });

        m_AttPanel.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            ListSelectionModel lm = (ListSelectionModel) e.getSource();
                            for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
                                if (lm.isSelectedIndex(i)) {
                                    m_AttSummaryPanel.setAttribute(i);
                                    m_AttVisualizePanel.setAttribute(i);
                                    break;
                                }
                            }
                        }
                    }
                });

        m_InstSummaryPanel.setBorder(BorderFactory
                .createTitledBorder("Current relation"));

        JPanel attStuffHolderPanel = new JPanel();
        attStuffHolderPanel.setBorder(BorderFactory
                .createTitledBorder("Attributes"));
        attStuffHolderPanel.setLayout(new BorderLayout());
        attStuffHolderPanel.add(m_AttPanel, BorderLayout.CENTER);

        m_RemoveButton.setEnabled(false);
        m_RemoveButton.setToolTipText("Remove selected attributes.");
        m_RemoveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Remove r = new Remove();
                    int[] selected = m_AttPanel.getSelectedAttributes();
                    if (selected.length == 0) {
                        return;
                    }

                    /** SamoaToWekaInstanceConverter.wekaInstances() method does not support class attribute**/
                    for(int i=0; i<selected.length; i++ ){
                        if(selected[i]==m_Instances.classIndex()){
                            JOptionPane.showMessageDialog(VisualizeFeaturesPanel.this,
                                    "Can't remove the class attribute from dataset!\n", "Remove Attributes",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }else{
                            continue;
                        }

                    }

                    if (selected.length == m_Instances.numAttributes()) {
                        // Pop up an error optionpane
                        JOptionPane.showMessageDialog(VisualizeFeaturesPanel.this,
                                "Can't remove all attributes from data!\n", "Remove Attributes",
                                JOptionPane.ERROR_MESSAGE);
                        m_Log.logMessage("Can't remove all attributes from data!");
                        m_Log.statusMessage("Problem removing attributes");
                        return;
                    }
                    r.setAttributeIndicesArray(selected);
                    applyFilter(r);
                    m_RemoveButton.setEnabled(false);
                } catch (Exception ex) {
                    if (m_Log instanceof TaskLogger) {
                        ((TaskLogger) m_Log).taskFinished();
                    }
                    // Pop up an error optionpane
                    JOptionPane.showMessageDialog(VisualizeFeaturesPanel.this,
                            "Problem filtering instances:\n" + ex.getMessage(),
                            "Remove Attributes", JOptionPane.ERROR_MESSAGE);
                    m_Log.logMessage("Problem removing attributes: " + ex.getMessage());
                    m_Log.statusMessage("Problem removing attributes");
                    ex.printStackTrace();
                }
            }
        });

        JPanel p1 = new JPanel();
        p1.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        p1.setLayout(new BorderLayout());
        p1.add(m_RemoveButton, BorderLayout.CENTER);
        attStuffHolderPanel.add(p1, BorderLayout.SOUTH);

        m_AttSummaryPanel.setBorder(BorderFactory.createTitledBorder("Selected attribute"));
        m_SaveBut.setEnabled(false);

        // Set up the GUI layout
        JPanel buttons = new JPanel();
        buttons.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        buttons.setLayout(new GridLayout(1, 6, 5, 5));
        buttons.add(m_OpenFileBut);
        buttons.add(m_SaveBut);


        JPanel attInfo = new JPanel();
        attInfo.setLayout(new BorderLayout());
        attInfo.add(attStuffHolderPanel, BorderLayout.CENTER);

        JPanel attVis = new JPanel();
        attVis.setLayout(new GridLayout(3, 1));
        attVis.add(m_AttSummaryPanel);

        m_plotTypeBox = new JComboBox();

        JComboBox colorBox = m_AttVisualizePanel.getColorBox();
        colorBox.setToolTipText("The chosen attribute will also be used as the "
                + "class attribute when a filter is applied.");
        colorBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
                if (ie.getStateChange() == ItemEvent.SELECTED) {

                    String item=colorBox.getSelectedItem().toString();

                    if(item.equalsIgnoreCase("No class")){
                        m_plotAmplify.setEnabled(false);
                    }else{
                        m_plotAmplify.setEnabled(true);
                    }

                    if(item.endsWith("(numeric)")){//Nom,Num,No class
                        m_plotTypeBox.removeAllItems();
                        m_plotTypeBox.addItem("plot type: Line graph");
                        m_plotTypeBox.addItem("plot type: Scatter diagram");
                    }else if(item.endsWith("(nominal)")){
                        m_plotTypeBox.removeAllItems();
                        m_plotTypeBox.addItem("plot type: Scatter diagram");
                        m_plotTypeBox.addItem("No plot type");
                    }else{
                        m_plotTypeBox.removeAllItems();
                    }
                }
            }
        });

        m_startInstanceInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                try {
                    m_startInstanceInput.commitEdit();

                    Object o= m_startInstanceInput.getValue();
                    if(o==null){
                        m_startInstanceInput.setValue(1);//set default startIndex
                    }else {
                        int i = Integer.parseInt(o.toString());

                        /**
                         * m_intStartIndex range: [1,numInstance-1]
                         */
                        if (i < 1 || i>=m_Instances.numInstances()) {
                            m_startInstanceInput.setValue(1);
                            Toolkit.getDefaultToolkit().beep();
                            JOptionPane.showMessageDialog(m_startInstanceInput.getParent(), "The instance start index must be positive integer, \n" +
                                            "more than 0 and less than total instance number!",
                                    "Instance startIndex input error prompt!",JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (ParseException parseException) {
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(m_startInstanceInput.getParent(), "The instance start index must be positive integer, \n" +
                                    "more than 0 and less than total instance number!",
                            "Instance startIndex input error prompt!",JOptionPane.WARNING_MESSAGE);

                }
            }
        });

        m_endInstanceInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                if(m_Instances!=null){
                    int numInst=m_Instances.numInstances();
                    try {
                        m_endInstanceInput.commitEdit();

                        Object o= m_endInstanceInput.getValue();
                        if(o==null){
                            m_endInstanceInput.setValue(numInst);//Default is set so this could not happen.
                        }else {
                            int inputInt = Integer.parseInt(o.toString());

                            /**
                             * m_intEndIndex range: [2, numInstance]
                             */
                            if (inputInt < 2 || inputInt > numInst) {
                                //set default endIndex
                                if(numInst<=m_defaultMaxInstanceIndex){
                                    if(numInst<=m_defaultMaxInstanceIndex){
                                        m_endInstanceInput.setValue(numInst);
                                    }else if(numInst>m_defaultMaxInstanceIndex){
                                        m_endInstanceInput.setValue(m_defaultMaxInstanceIndex);
                                    }
                                }
                                //warning
                                Toolkit.getDefaultToolkit().beep();
                                JOptionPane.showMessageDialog(m_endInstanceInput.getParent(), "The instance end index must be positive integer, \n" +
                                                "more than 1 and not more than total instance number \""+numInst+"\"!",
                                        "Instance startIndex input error prompt!",JOptionPane.WARNING_MESSAGE);

                            }
                        }
                    } catch (ParseException parseException) {
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(m_endInstanceInput.getParent(), "The instance end index must be positive integer, \n" +
                                        "more than 1 and not more than total instance number \""+numInst+"\"!",
                                "Instance startIndex input error prompt!",JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        });


        /**
         * start index must be positive integer, bigger than 0 and less than instance number.
         *  numInstance>startIndex>=1
         */
        m_startInstanceInput.setToolTipText("Start index must be positive integer, bigger than 0 and less than instance number!");

        /**
         *  numInstance=>endIndex>1
         */
        m_endInstanceInput.setToolTipText("End index must be positive integer, bigger than 1 and no than instance number!");

        m_plotTypeBox.setEditable(false);
        m_plotTypeBox.setToolTipText("The chosen item will determine the feature plot type.");
        m_plotTypeBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED){
                    if(colorBox.getSelectedItem()!=null){
                        if(m_Instances!=null){
                            Object objectStartInstanceInput=m_startInstanceInput.getValue();
                            Object objectEndInstanceInput=m_endInstanceInput.getValue();
                            m_intStartIndex=Integer.parseInt(objectStartInstanceInput.toString());
                            m_intEndIndex=Integer.parseInt(objectEndInstanceInput.toString());

                            if(m_intEndIndex>m_intStartIndex){
                                m_graphPanel.setIntStartIndex(m_intStartIndex);
                                m_graphPanel.setIntEndIndex(m_intEndIndex);
                                m_selectedPlotTypeIndex=m_plotTypeBox.getSelectedIndex();
                                String selectedPlotItem=m_plotTypeBox.getSelectedItem().toString();
                                if(selectedPlotItem.equalsIgnoreCase("No plot type")){
                                    m_plotAmplify.setEnabled(false);
                                    m_visAllGraphBut.setEnabled(false);
                                }else{
                                    m_plotAmplify.setEnabled(true);
                                    m_visAllGraphBut.setEnabled(true);
                                }
                                String attributeName=colorBox.getSelectedItem().toString();
                                int attributeIndex=colorBox.getSelectedIndex();
                                if(attributeIndex==0){//
                                    /**
                                     * if the class is "no class",set the first class whose index is 0 as default class index.
                                     */
                                    m_graphPanel.setSelectedPlotInfo(m_selectedPlotTypeIndex,selectedPlotItem,0,colorBox.getItemAt(1).toString());
                                }else{
                                    m_graphPanel.setSelectedPlotInfo(m_selectedPlotTypeIndex,selectedPlotItem,attributeIndex-1,attributeName);
                                }
                            }else{
                                Toolkit.getDefaultToolkit().beep();
                                JOptionPane.showMessageDialog(m_plotTypeBox.getParent(), "EndIndex must be bigger than StartIndex",
                                        "Instance index range error prompt!",JOptionPane.WARNING_MESSAGE);
                            }
                        }
                    }
                }

            }
        });


        final JButton visAllBut = new JButton("Visualize All");
        visAllBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (m_Instances != null) {
                    try {
                        final weka.gui.beans.AttributeSummarizer as =
                                new weka.gui.beans.AttributeSummarizer();
                        as.setColoringIndex(m_AttVisualizePanel.getColoringIndex());
                        as.setInstances(m_samoaToWekaInstanceConverter.wekaInstances(m_Instances));

                        final JFrame jf = Utils.getWekaJFrame("All attributes", VisualizeFeaturesPanel.this);
                        jf.getContentPane().setLayout(new BorderLayout());
                        jf.getContentPane().add(as, BorderLayout.CENTER);
                        jf.addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosing(WindowEvent e) {
                                visAllBut.setEnabled(true);
                                jf.dispose();
                            }
                        });
                        jf.pack();
                        jf.setSize(1000, 600);
                        jf.setLocationRelativeTo(SwingUtilities.getWindowAncestor(VisualizeFeaturesPanel.this));
                        jf.setVisible(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        m_plotAmplify.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(m_Instances!=null){
                    int numIns=m_Instances.numInstances();

                    Object objectStartInstanceInput=m_startInstanceInput.getValue();
                    Object objectEndInstanceInput=m_endInstanceInput.getValue();
                    m_intStartIndex=Integer.parseInt(objectStartInstanceInput.toString());
                    m_intEndIndex=Integer.parseInt(objectEndInstanceInput.toString());

                    if(m_intEndIndex>m_intStartIndex){
                        m_graphPanel.setIntStartIndex(m_intStartIndex);
                        m_graphPanel.setIntEndIndex(m_intEndIndex);
                        m_graphPanel.paintAmplifiedPlot();
                    }else{
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(m_plotAmplify.getParent(), "EndIndex must be bigger than StartIndex",
                                "Instance index range error prompt!",JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        });

        m_visAllGraphBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(m_Instances!=null){
                    Object objectStartInstanceInput=m_startInstanceInput.getValue();
                    Object objectEndInstanceInput=m_endInstanceInput.getValue();
                    m_intStartIndex=Integer.parseInt(objectStartInstanceInput.toString());
                    m_intEndIndex=Integer.parseInt(objectEndInstanceInput.toString());

                    if(m_intEndIndex>m_intStartIndex){
                        m_graphPanel.visualizeAll();
                    }else{
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(m_visAllGraphBut.getParent(), "EndIndex must be bigger than StartIndex",
                                "Instance index range error prompt!",JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        });

        /** Histogram includes "dropdown List","visualize all button" and AttVisualizePanel */
        JPanel histoHolder = new JPanel();
        JPanel histoControls = new JPanel();
        histoControls.setLayout(new BorderLayout());
        histoControls.add(colorBox, BorderLayout.CENTER);//dropdown List
        histoControls.add(visAllBut, BorderLayout.EAST);//visualize all button

        histoHolder.setLayout(new BorderLayout());
        histoHolder.add(histoControls, BorderLayout.NORTH);
        histoHolder.add(m_AttVisualizePanel, BorderLayout.CENTER);

        attVis.add(histoHolder);

        JPanel lineScatterController = new JPanel();

        //lineScatterController.setLayout(new FlowLayout());
        /** BoxLayout is better than lowLayout on linux machine. */
        lineScatterController.setLayout(new BoxLayout(lineScatterController,BoxLayout.X_AXIS));
        lineScatterController.add(m_startIndex);
        m_startInstanceInput.setPreferredSize(new Dimension(80, 18));
        lineScatterController.add(m_startInstanceInput);
        lineScatterController.add(m_endIndex);
        m_endInstanceInput.setPreferredSize(new Dimension(80, 18));
        lineScatterController.add(m_endInstanceInput);
        lineScatterController.add(m_plotTypeBox);

        JPanel lineScatterHolder = new JPanel();
        lineScatterHolder.setLayout(new BorderLayout());
        lineScatterHolder.add(lineScatterController, BorderLayout.NORTH);
        lineScatterHolder.add(m_graphPanel, BorderLayout.CENTER);

        JPanel graphButtonPanel=new JPanel();
        graphButtonPanel.setLayout(new FlowLayout());
        graphButtonPanel.add(m_plotAmplify);

        m_featureRangeBox=new JComboBox();
        graphButtonPanel.add(m_featureRangeBox);
        m_featureRangeBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED){
                    m_graphPanel.setFeatureRange(e.getItem().toString());
                }
            }
        });

        graphButtonPanel.add(m_visAllGraphBut);
        lineScatterHolder.add(graphButtonPanel,BorderLayout.SOUTH);

        attVis.add(lineScatterHolder);

        JPanel lhs = new JPanel();//left
        lhs.setLayout(new BorderLayout());
        lhs.add(m_InstSummaryPanel, BorderLayout.NORTH);
        lhs.add(attInfo, BorderLayout.CENTER);

        JPanel rhs = new JPanel();//right
        rhs.setLayout(new BorderLayout());
        rhs.add(attVis, BorderLayout.CENTER);

        JPanel relation = new JPanel();
        relation.setLayout(new GridLayout(1, 2));
        relation.add(lhs);
        relation.add(rhs);

        JPanel middle = new JPanel();
        middle.setLayout(new BorderLayout());
        middle.add(relation, BorderLayout.CENTER);

        LogPanel lp = new LogPanel();//Log + Status
        this.setLog(lp);

        setLayout(new BorderLayout());
        add(buttons, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        //add(lp,BorderLayout.SOUTH);

    }

    public boolean startIndexValidation(int startIndex){
        if(startIndex>=1){
            return true;
        }else{
            JOptionPane.showMessageDialog(this, "The instance start index must be integer and not less than 1:\n"
                    , "Instance Index", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean endIndexValidation(int endIndex){
        if(endIndex<=m_Instances.numInstances()){
            return true;
        }else{
            JOptionPane.showMessageDialog(this, "The instance end index must be integer and not bigger than \""+m_Instances.numInstances() + "\""
                    , "Instance Index", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Sets the Logger to receive informational messages
     *
     * @param newLog the Logger that will now get info messages
     */
    public void setLog(Logger newLog) {
        m_Log = newLog;
    }

    /**
     * Tells the panel to use a new base set of instances.
     *
     * @param inst a set of Instances
     */
    public void setInstances(Instances inst) {

        m_Instances = inst;
        try {
            Runnable r = new Runnable() {
                public void run() {
                    boolean first = (m_AttPanel.getTableModel() == null);
                    m_graphPanel.setInstances(m_Instances);

                    /**
                     * When new data is loaded, set default value for m_intStartIndex and m_intEndIndex
                     */
                    if(m_Instances!=null){
                        /** set default startIndex, endIndex */
                        m_startInstanceInput.setValue(1);

                        /** set default endIndex */
                        int numInstances=m_Instances.numInstances();
                        if(numInstances<=m_defaultMaxInstanceIndex){
                            m_endInstanceInput.setValue(numInstances);
                            m_graphPanel.setIntEndIndex(numInstances);
                        }else{
                            m_endInstanceInput.setValue(m_defaultMaxInstanceIndex);
                            m_graphPanel.setIntEndIndex(m_defaultMaxInstanceIndex);
                        }
                    }

                    /** The suggested best set for maxPlotsNumberInOneScreen is 9 for 3 rows * 3 columns plots. */
                    int maxPlotsNumberInOneScreen=9;//
                    if(m_Instances!=null && m_Instances.numAttributes()>0){
                        m_featureRangeBox.removeAllItems();//when user changes dataset, this code will be used.
                        featureRangeBoxSet(m_Instances.numAttributes(),maxPlotsNumberInOneScreen);
                    }

                    m_InstSummaryPanel.setInstances(m_Instances);
                    m_AttPanel.setInstances(m_Instances);

                    if (first) {
                        TableModel model = m_AttPanel.getTableModel();
                        model.addTableModelListener(new TableModelListener() {
                            public void tableChanged(TableModelEvent e) {
                                if (m_AttPanel.getSelectedAttributes() != null
                                        && m_AttPanel.getSelectedAttributes().length > 0) {
                                    m_RemoveButton.setEnabled(true);
                                } else {
                                    m_RemoveButton.setEnabled(false);
                                }
                            }
                        });
                    }

                    m_AttSummaryPanel.setInstances(m_Instances);
                    m_AttVisualizePanel.setInstances(m_samoaToWekaInstanceConverter.wekaInstances(m_Instances));

                    /** select the first attribute in the list */
                    m_AttPanel.getSelectionModel().setSelectionInterval(0, 0);
                    m_AttSummaryPanel.setAttribute(0);
                    m_AttVisualizePanel.setAttribute(0);

                    /**
                     * The default selected item of component colorBox in this class is the class attribute,
                     * so the default attribute shown in scatter diagram is the class attribute too.
                     * In other words, the initial attribute shown in histogram and scatter diagram both is the class attribute
                     * which is always the last attribute in a dataset.
                     */
                    m_graphPanel.setAttributeIndex(m_Instances.classIndex());


                    m_Log.logMessage("Base relation is now " + m_Instances.getRelationName()
                            + " (" + m_Instances.numInstances() + " instances)");
                    m_SaveBut.setEnabled(true);
                    m_Log.statusMessage("OK");

                    // Fire a propertychange event
                    m_Support.firePropertyChange("", null, null);

                    fip.setInstances(m_Instances);
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                SwingUtilities.invokeAndWait(r);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Problem setting base instances:\n"
                    + ex, "Instances", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void featureRangeBoxSet(int numAttributes,int maxFeaturesInOneScreen){
        if(numAttributes>=maxFeaturesInOneScreen){
            int itemNum= (int) Math.ceil(numAttributes/maxFeaturesInOneScreen);
            int remainder=numAttributes % maxFeaturesInOneScreen;

            for(int i=0; i<itemNum; i++){
                m_featureRangeBox.addItem("feature range: "+ (i * maxFeaturesInOneScreen+1) + " -- " + (i+1)*maxFeaturesInOneScreen);
            }
            if(remainder>0 && remainder==1 ){
                m_featureRangeBox.addItem("feature range: "+(itemNum * maxFeaturesInOneScreen+1));
            }else if(remainder>1){
                m_featureRangeBox.addItem("feature range: "+(itemNum * maxFeaturesInOneScreen+1)+ " -- " + (itemNum * maxFeaturesInOneScreen+remainder));
            }
        }else if(numAttributes<maxFeaturesInOneScreen && numAttributes>1){
            /** Then number of attributes is more than 1 but less than max features whose plots are shown in "visualizeAll" screen.*/
            m_featureRangeBox.addItem("feature range: "+1+ " -- " + numAttributes);
        }else if(numAttributes==1){
            /** There is only one attribute in the dataset */
            m_featureRangeBox.addItem("feature range: "+1);
        }

    }

    /**
     * Gets the working set of instances.
     *
     * @return the working instances
     */
    public Instances getInstances() {
        return m_Instances;
    }

    /**
     * Adds a PropertyChangeListener who will be notified of value changes.
     *
     * @param l a value of type 'PropertyChangeListener'
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (m_Support != null && l != null) {
            m_Support.addPropertyChangeListener(l);
        }
    }

    /**
     * Removes a PropertyChangeListener.
     *
     * @param l a value of type 'PropertyChangeListener'
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (m_Support != null && l != null) {
            m_Support.removePropertyChangeListener(l);
        }
    }


    /**
     * Passes the dataset through the filter that has been configured for use.
     *
     * @param filter the filter to apply
     */
    protected void applyFilter(final Filter filter) {
        if (m_IOThread == null) {
            m_IOThread = new Thread() {
                @Override
                public void run() {
                    try {

                        if (filter != null) {
                            if (m_Log instanceof TaskLogger) {
                                ((TaskLogger) m_Log).taskStarted();
                            }
                            m_Log.statusMessage("Passing dataset through filter "
                                    + filter.getClass().getName());
                            String cmd = filter.getClass().getName();
                            if (filter instanceof OptionHandler)
                                cmd +=
                                        " "
                                                + Utils.joinOptions(((OptionHandler) filter).getOptions());
                            m_Log.logMessage("Command: " + cmd);
                            int classIndex = m_AttVisualizePanel.getColoringIndex();
                            if ((classIndex < 0) && (filter instanceof SupervisedFilter)) {
                                throw new IllegalArgumentException("Class (colour) needs to "
                                        + "be set for supervised " + "filter.");
                            }
                            Instances copy = new Instances(m_Instances);
                            copy.setClassIndex(classIndex);

                            Filter filterCopy = Filter.makeCopy(filter);
                            weka.core.Instances copyToWeka=m_samoaToWekaInstanceConverter.wekaInstances(copy) ;
                            filterCopy.setInputFormat(copyToWeka);
                            weka.core.Instances newInstances = Filter.useFilter(copyToWeka, filterCopy);

                            if (newInstances == null || newInstances.numAttributes() < 1) {
                                throw new Exception("Dataset is empty.");
                            }
                            m_Log.statusMessage("Saving undo information");


                            int debugClassIndex=copy.classIndex();
                            int debugClassIndex2=copyToWeka.classIndex();
                            m_AttVisualizePanel.setColoringIndex(copy.classIndex());
                            // if class was not set before, reset it again after use of filter
              /* if (m_Instances.classIndex() < 0)
                newInstances.setClassIndex(-1); */
                            m_Instances = m_wekaToSamoaInstanceConverter.samoaInstances(newInstances);
                            setInstances(m_Instances);
                            if (m_Log instanceof TaskLogger) {
                                ((TaskLogger) m_Log).taskFinished();
                            }
                        }

                    } catch (Exception ex) {

                        if (m_Log instanceof TaskLogger) {
                            ((TaskLogger) m_Log).taskFinished();
                        }
                        // Pop up an error optionpane
                        JOptionPane.showMessageDialog(VisualizeFeaturesPanel.this,
                                "Problem filtering instances:\n" + ex.getMessage(),
                                "Apply Filter", JOptionPane.ERROR_MESSAGE);
                        m_Log.logMessage("Problem filtering instances: " + ex.getMessage());
                        m_Log.statusMessage("Problem filtering instances");
                        ex.printStackTrace();
                    }
                    m_IOThread = null;
                }
            };
            m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
            m_IOThread.start();
        } else {
            JOptionPane.showMessageDialog(this, "Can't apply filter at this time,\n"
                            + "currently busy with other IO", "Apply Filter",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Queries the user for a file to load instances from, then loads the
     * instances in a background process. This is done in the IO thread, and an
     * error message is popped up if the IO thread is busy.
     */
    public void setInstancesFromFileQ() {

        if (m_IOThread == null) {
            int returnVal = m_FileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                if (m_FileChooser.getLoader() == null) {
                    JOptionPane.showMessageDialog(this,
                            "Cannot determine file loader automatically, please choose one.",
                            "Load Instances", JOptionPane.ERROR_MESSAGE);
                } else {
                    setInstancesFromFile2(m_FileChooser.getSelectedFile().toString());
                }

            }
        } else {
            JOptionPane.showMessageDialog(this, "Can't load at this time,\n"
                            + "currently busy with other IO", "Load Instances",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    public void setInstancesFromFile2(String selectedFile) {

        if (m_IOThread == null) {
            m_IOThread = new Thread() {
                @Override
                public void run() {
                    try {
                        m_Log.statusMessage("Reading from file...");
                        ArffFileStream afs=new ArffFileStream(selectedFile,-1);
                        Instance instance=null;
                        java.io.Reader reader = new java.io.BufferedReader(new java.io.FileReader(selectedFile));
                        Instances insts=new Instances(reader,1,-1);//parameter size is not used.


                        while(afs.hasMoreInstances()){
                            instance=afs.nextInstance().getData();
                            insts.add(instance);
                        }
                        setInstances(insts);
                    } catch (Exception ex) {
                    }
                    m_IOThread = null;
                }
            };
            m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
            m_IOThread.start();
        } else {
            JOptionPane.showMessageDialog(this, "Can't load at this time,\n"
                            + "currently busy with other IO", "Load Instances",
                    JOptionPane.WARNING_MESSAGE);
        }
    }


    /**
     * Queries the user for a file to save instances as, then saves the instances
     * in a background process. This is done in the IO thread, and an error
     * message is popped up if the IO thread is busy.
     */
    public void saveWorkingInstancesToFileQ() {

        if (m_IOThread == null) {

            m_FileChooser.setAcceptAllFileFilterUsed(false);
            int returnVal = m_FileChooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                weka.core.Instances inst=m_samoaToWekaInstanceConverter.wekaInstances(m_Instances) ;
                inst.setClassIndex(m_AttVisualizePanel.getColoringIndex());
                saveInstancesToFile(m_FileChooser.getSaver(), inst);
            }
            FileFilter temp = m_FileChooser.getFileFilter();
            m_FileChooser.setAcceptAllFileFilterUsed(true);
            m_FileChooser.setFileFilter(temp);
        } else {
            JOptionPane.showMessageDialog(this, "Can't save at this time,\n"
                            + "currently busy with other IO", "Save Instances",
                    JOptionPane.WARNING_MESSAGE);
        }
    }


    /**
     * saves the data with the specified saver
     *
     * @param saver the saver to use for storing the data
     * @param inst the data to save
     */
    public void saveInstancesToFile(final AbstractFileSaver saver,
                                    final weka.core.Instances inst) {
        if (m_IOThread == null) {
            m_IOThread = new Thread() {
                @Override
                public void run() {
                    try {
                        m_Log.statusMessage("Saving to file...");
                        saver.setInstances(inst);
                        saver.writeBatch();

                        m_Log.statusMessage("OK");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        m_Log.logMessage(ex.getMessage());
                    }
                    m_IOThread = null;
                }
            };
            m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
            m_IOThread.start();
        } else {
            JOptionPane.showMessageDialog(this, "Can't save at this time,\n"
                            + "currently busy with other IO", "Saving instances",
                    JOptionPane.WARNING_MESSAGE);
        }
    }



    /**
     * We've been instantiated and now have access to the main application and
     * PerspectiveManager
     */
    @Override
    public void instantiationComplete() {
        // overridden here so that we know if we're operating within
        // a non-legacy UI, and can thus set up our send data to perspective
        // menu (if necessary)
        boolean sendToAll =
                getMainApplication().getApplicationSettings().getSetting(
                        getPerspectiveID(),
                        PreprocessPanel.PreprocessDefaults.ALWAYS_SEND_INSTANCES_TO_ALL_KEY,
                        PreprocessPanel.PreprocessDefaults.ALWAYS_SEND_INSTANCES_TO_ALL,
                        Environment.getSystemWide());

        // install a menu item to allow the user to choose to send to all or
        // send to a specific perspective
        final List<Perspective> perspectivesThatAcceptInstances =
                new ArrayList<Perspective>();
        List<Perspective> visiblePerspectives =
                getMainApplication().getPerspectiveManager().getVisiblePerspectives();
        for (Perspective p : visiblePerspectives) {
            if (p.acceptsInstances()
                    && !p.getPerspectiveID().equals(getPerspectiveID())) {
                perspectivesThatAcceptInstances.add(p);
            }
        }

        if (perspectivesThatAcceptInstances.size() > 0) {

            m_sendToPerspective = new JMenu();
            m_sendToPerspective.setText("Send to perspective");

            if (!sendToAll) {
                m_sendToPerspective.setEnabled(false);
            }

            JMenuItem sendToAllItem = new JMenuItem("All perspectives");
            sendToAllItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (Perspective p : perspectivesThatAcceptInstances) {
                        if (getInstances() != null && p.acceptsInstances()) {
                            p.setInstances(m_samoaToWekaInstanceConverter.wekaInstances(getInstances()));
                            getMainApplication().getPerspectiveManager()
                                    .setEnablePerspectiveTab(p.getPerspectiveID(), true);
                        }
                    }
                }
            });
            m_sendToPerspective.add(sendToAllItem);

            for (final Perspective p : perspectivesThatAcceptInstances) {
                JMenuItem item = new JMenuItem(p.getPerspectiveTitle());
                m_sendToPerspective.add(item);
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (getInstances() != null) {
                            p.setInstances(m_samoaToWekaInstanceConverter.wekaInstances(getInstances()));
                            getMainApplication().getPerspectiveManager()
                                    .setEnablePerspectiveTab(p.getPerspectiveID(), true);
                            getMainApplication().getPerspectiveManager()
                                    .setActivePerspective(p.getPerspectiveID());
                        }
                    }
                });
            }
        }
    }


    public static class PreprocessDefaults extends Defaults {
        public static final String ID = "weka.gui.explorer.preprocesspanel";

        public static final Settings.SettingKey INITIAL_DIR_KEY =
                new Settings.SettingKey(ID + ".initialDir",
                        "Initial directory for opening datasets", "");
        public static final File INITIAL_DIR = new File("${user.dir}");

        public static final Settings.SettingKey UNDO_DIR_KEY =
                new Settings.SettingKey(ID + ".undoDir",
                        "Directory for storing undo files", "");
        public static final File UNDO_DIR = new File("${java.io.tmpdir}");

        public static final Settings.SettingKey FILTER_KEY =
                new Settings.SettingKey(ID + ".initialFilter", "Initial filter", "");
        public static final Filter FILTER = new AllFilter();

        public static final Settings.SettingKey ENABLE_UNDO_KEY =
                new Settings.SettingKey(ID + ".enableUndo", "Enable undo", "");
        public static final Boolean ENABLE_UNDO = true;

        public static final Settings.SettingKey ALWAYS_SEND_INSTANCES_TO_ALL_KEY =
                new Settings.SettingKey(ID + ".alwaysSendInstancesToAllPerspectives",
                        "Always send instances to all perspectives", "");
        public static boolean ALWAYS_SEND_INSTANCES_TO_ALL = true;

        public PreprocessDefaults() {
            super(ID);

            INITIAL_DIR_KEY.setMetadataElement("java.io.File.fileSelectionMode", ""
                    + JFileChooser.DIRECTORIES_ONLY);
            INITIAL_DIR_KEY.setMetadataElement("java.io.File.dialogType", ""
                    + JFileChooser.OPEN_DIALOG);
            UNDO_DIR_KEY.setMetadataElement("java.io.File.fileSelectionMode", ""
                    + JFileChooser.DIRECTORIES_ONLY);
            UNDO_DIR_KEY.setMetadataElement("java.io.File.dialogType", ""
                    + JFileChooser.DIRECTORIES_ONLY);
            m_defaults.put(INITIAL_DIR_KEY, INITIAL_DIR);
            m_defaults.put(UNDO_DIR_KEY, UNDO_DIR);
            m_defaults.put(FILTER_KEY, FILTER);
            m_defaults.put(ENABLE_UNDO_KEY, ENABLE_UNDO);
            m_defaults.put(ALWAYS_SEND_INSTANCES_TO_ALL_KEY,
                    ALWAYS_SEND_INSTANCES_TO_ALL);
        }
    }

    /**
     * Tests out the instance-preprocessing panel from the command line.
     *
     * @param args ignored
     */
    public static void main(String[] args) {

        try {
            final JFrame jf = new JFrame("MOA: Data Analysis");
            jf.getContentPane().setLayout(new BorderLayout());
            final VisualizeFeaturesPanel sp = new VisualizeFeaturesPanel();
            jf.getContentPane().add(sp, BorderLayout.CENTER);
            LogPanel lp = new LogPanel();
            sp.setLog(lp);
            jf.getContentPane().add(lp, BorderLayout.SOUTH);
            jf.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    jf.dispose();
                    System.exit(0);
                }
            });
            jf.pack();
            jf.setSize(800, 600);
            jf.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println(ex.getMessage());
        }
    }
}