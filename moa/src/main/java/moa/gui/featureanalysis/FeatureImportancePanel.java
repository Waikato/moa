/*
 *    FeatureImportancePanel.java
 *    Copyright (C) 2020 University of Waikato, Hamilton, New Zealand
 *    @author Yongheng Ma (2560653665@qq.com)
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
package moa.gui.featureanalysis;

import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.WekaToSamoaInstanceConverter;
import moa.gui.ClassOptionSelectionPanel;
import moa.gui.GUIUtils;
import moa.options.ClassOption;
import moa.tasks.*;
import org.math.plot.Plot2DPanel;
import weka.gui.AbstractPerspective;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * This panel is the FeatureImportance tab which provides config GUI for feature importance algorithm,
 * run button to trigger the execution of the algorithm, table line graphs to display scores of the
 * the execution result.
 *
 */
public class FeatureImportancePanel extends AbstractPerspective {
    /**
     * This holds the current set of instances
     */
    protected Instances m_Instances;

    /**
     * Configure GUI so that user can set parameters for feature importance algorithm and
     *  trigger task execution to compute scores of feature importance.
     */
    protected MainTask currentTask = new FeatureImportanceConfig();

    /**
     * Tasks are encapsulated in task thread to execute.
     * This List Stores tasks threads which include the execution results of the threads.
     */
    protected List<TaskThread> taskList = new ArrayList<TaskThread>();

    /** Store user's preference parameters for feature importance algorithm.*/
    private Preferences prefs;

    private final String PREF_NAME = "currentTask";

    protected JButton configureTaskButton = new JButton("Configure");

    protected JTextField taskDescField = new JTextField();

    protected JButton runTaskButton = new JButton("Run");

    /**
     * Use progress bar to show the progress of computing scores of feature importance.
     */
    protected JProgressBar progressBar =new JProgressBar();

    protected JLabel progressLabel= new JLabel("Running progress:");

    /** Feature importance data model includes two parts, the dataset and scores which will be shown in table
     *  so user can view data and choose which feature importance scores to be shown in line graph.
     */
    protected FeatureImportanceDataModelPanel featureImportanceDataModelPanel= new FeatureImportanceDataModelPanel();

    private static FeatureImportancePanel fip=null;

    /** THe drawing tool provided by jmathplot.jar */
    protected Plot2DPanel plot;

    /** Feature importance scores produced by feature importance algorithm. */
    protected double[][] scores;

    /**The selected attribute indices.*/
    protected int[] m_selectedAttributeIndices;

    /** Show line graphs for user selected feature importance. */
    protected FeatureImportanceGraph featureImportanceGraph= new FeatureImportanceGraph();

    /** The default windowSize parameter for feature importance algorithm. */
    protected int m_windowSize=500;

    public int getWindowSize() {
        return m_windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.m_windowSize = windowSize;
    }

    /** Singleton design pattern */
    public static synchronized FeatureImportancePanel getInstance(){
        if(fip == null){
            fip=new FeatureImportancePanel();
        }
        return fip;
    }


    private FeatureImportancePanel(){
        prefs = Preferences.userRoot().node(this.getClass().getName());

        String taskText = this.currentTask.getCLICreationString(MainTask.class);
        String propertyValue = prefs.get(PREF_NAME, taskText);

        if(propertyValue!=null){
            setParameters(propertyValue);
        }

        setTaskString(propertyValue, false); //Not store preference
        this.taskDescField.setEditable(false);

        final Component comp = this.taskDescField;
        this.taskDescField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 1) {
                    if ((evt.getButton() == MouseEvent.BUTTON3)
                            || ((evt.getButton() == MouseEvent.BUTTON1) && evt.isAltDown() && evt.isShiftDown())) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem item;

                        item = new JMenuItem("Copy configuration to clipboard");
                        item.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                copyClipBoardConfiguration();
                            }
                        });
                        menu.add(item);

                        item = new JMenuItem("Enter configuration...");
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent arg0) {
                                String newTaskString = JOptionPane.showInputDialog("Insert command line");
                                if (newTaskString != null) {
                                    setTaskString(newTaskString);
                                }
                            }
                        });
                        menu.add(item);

                        menu.show(comp, evt.getX(), evt.getY());
                    }
                }
            }
        });

        this.setLayout(new BorderLayout());

        JPanel configPanel = new JPanel();
        configPanel.setBorder(BorderFactory.createTitledBorder("Feature importance algorithm parameters config:"));
        configPanel.setLayout(new BorderLayout());
        configPanel.add(this.configureTaskButton, BorderLayout.WEST);
        configPanel.add(this.taskDescField, BorderLayout.CENTER);
        JPanel runViewPanel = new JPanel();
        runViewPanel.setLayout(new BorderLayout());
        runViewPanel.add(this.runTaskButton, BorderLayout.WEST);

        configPanel.add(runViewPanel, BorderLayout.EAST);


        JPanel barPanel=new JPanel();
        barPanel.setBorder(BorderFactory.createEmptyBorder(10,2,5,2));
        barPanel.setLayout(new BorderLayout());
        barPanel.setSize(super.getWidth(),super.getHeight());
        barPanel.add(progressLabel,BorderLayout.WEST);
        barPanel.add(progressBar,BorderLayout.CENTER);

        configPanel.add(barPanel, BorderLayout.SOUTH);

        this.add(configPanel,BorderLayout.NORTH);

        this.configureTaskButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String newTaskString = ClassOptionSelectionPanel.showSelectClassDialog(FeatureImportancePanel.this,
                        "Configure task", ClassificationMainTask.class,
                        FeatureImportancePanel.this.currentTask.getCLICreationString(ClassificationMainTask.class),
                        null);

                if(newTaskString!=null){
                    /** Parse parameter windowSize from user's configuration or preference. */
                    setParameters(newTaskString);
                }
                setTaskString(newTaskString);
                ClassOptionSelectionPanel.setRequiredCapabilities(null);
            }
        });


        this.runTaskButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {

                if(m_Instances!=null) {
                    int numInstances = m_Instances.numInstances();

                    int windowSize =getWindowSize();
                    if(numInstances<=windowSize){
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(runTaskButton.getParent(),
                                "Parameter windowSize of feature importance algorithm must be smaller than numInstances.\n" +
                                        "The current set: windowSize is \""+windowSize+"\" and numInstances is \""+numInstances+"\"."
                                , "Feature importance configuration", JOptionPane.ERROR_MESSAGE);

                    }else{
                        int numAttributes = m_Instances.numAttributes();
                        String[] attributeNames = new String[numAttributes - 1];
                        for (int i = 0; i < numAttributes - 1; i++) {
                            attributeNames[i] = m_Instances.attribute(i).name();
                        }

                        ((FeatureImportanceConfig)currentTask).setInstances(m_Instances);
                        //runTask((Task) FeatureImportancePanel.this.currentTask.copy());//Can not be used in this case.
                        runTask((Task) FeatureImportancePanel.this.currentTask);

                        TaskThread thread = taskList.get(0);
                        thread.addTaskCompletionListener(new TaskCompletionListener() {
                            @Override
                            public void taskCompleted(TaskThread task) {
                                scores= (double[][]) thread.getFinalResult();

                                /**
                                 * Feature importance score values may be like 0.053471126481504741, 0.0, 1.0, NAN.
                                 * If the value is like 0.053471126481504741, keep just 4 rounding numbers after point.
                                 */
                                for(int row=0; row<scores.length; row++ ){
                                    for(int col=0; col<scores[row].length;col++ ){
                                        if(!Double.isNaN(scores[row][col]) && Double.toString(scores[row][col]).length()>=6){
                                            scores[row][col]=new BigDecimal(scores[row][col]).setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
                                        }
                                    }
                                }

                                /**
                                 * pass copied feature importance data to table panel
                                 */
                                double[][] scoresCopyForDataModel=scores.clone();
                                featureImportanceDataModelPanel.setFeatureImportanceScores(scoresCopyForDataModel);

                                /**
                                 * pass attributes names and feature importance data to line graph panel
                                 */
                                featureImportanceGraph.setAttributeNames(attributeNames);

                                int rows=scores.length;
                                int columns=scores[0].length;

                                double[][] scoresCopyForGraph=new double[rows][columns];
                                for(int k=0;k<scores.length;k++){
                                    /**
                                     * This is deep value copy so that the object scoresCopyForGraph and scoresCopyForDataModel
                                     * don't interact each other.
                                     * For FeatureImportanceGraph, if feature importance is NaN, the line graph shows 0.0;
                                     * For FeatureImportanceDataModelPanel, if feature importance is NaN, the table shows NaN;
                                     */
                                    scoresCopyForGraph[k]=scores[k].clone();
                                }

                                double naNSubstitute= ((FeatureImportanceConfig)currentTask).getNaNSubstitute();
                                for(int i=0; i<rows; i++){
                                    for(int j=0; j<columns; j++){
                                        if(Double.isNaN(scoresCopyForGraph[i][j])){
                                            /**
                                             * Plot2DPanel object can only show double data values, so if feature importance value is "NaN",
                                             * the value "NaN" is replaced by a user-set double value which will be show in line graph.
                                             */
                                            scoresCopyForGraph[i][j]=naNSubstitute;
                                        }
                                    }
                                }
                                featureImportanceGraph.setFeatureImportance(scoresCopyForGraph);
                            }
                        });

                        JProgressBar sourceProgressBar =((FeatureImportanceConfig) currentTask).getProgressBar();

                        sourceProgressBar.addChangeListener(new ChangeListener() {
                            @Override
                            public void stateChanged(ChangeEvent evt) {
                                JProgressBar comp = (JProgressBar)evt.getSource();
                                int value = comp.getValue();
                                int max = comp.getMaximum();
                                progressBar.setMinimum(0);
                                progressBar.setMaximum(max);
                                progressBar.setValue(value);
                            }
                        });
                    }
                }else{
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(runTaskButton.getParent(), "Please open data stream file from \n" +
                                        "VisualizerFeatures tab first!",
                                "Not open file error prompt!", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        JPanel tabAndGraphHolder=new JPanel();
        tabAndGraphHolder.setLayout(new GridLayout(2,1));

        JPanel featureImportanceTablePanel=new JPanel();
        featureImportanceTablePanel.setBorder(BorderFactory.createTitledBorder("Feature importance table"));
        featureImportanceTablePanel.setLayout(new BorderLayout());
        featureImportanceTablePanel.add(featureImportanceDataModelPanel,BorderLayout.CENTER);
        tabAndGraphHolder.add(featureImportanceTablePanel);


        featureImportanceDataModelPanel.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {//True means that user finish one row selection with mouse down and up

                    m_selectedAttributeIndices=featureImportanceDataModelPanel.m_Model.getSelectedAttributes();//probably produce empty int array like "[]"
                    featureImportanceGraph.setSelectedAttributeIndices(m_selectedAttributeIndices);
                }
            }
        });

        JPanel featureImportanceLineGraphPanel=new JPanel();
        featureImportanceLineGraphPanel.setLayout(new BorderLayout());
        featureImportanceLineGraphPanel.setBorder(BorderFactory.createTitledBorder("Feature importance line graph"));
        featureImportanceLineGraphPanel.add(featureImportanceGraph,BorderLayout.CENTER);
        tabAndGraphHolder.add(featureImportanceLineGraphPanel);
        this.add(tabAndGraphHolder,BorderLayout.CENTER);
    }

    /**
     * Parse parameter windowSize from user's configuration or preference。
     * The parameter windowSize is used to check whether the total instance number is bigger
     * than windowSize after user click the Run button and before the feature importance task
     * being executed.
     *
     * @param commandLineText
     */
    public void setParameters(String commandLineText){
        String[] cli=commandLineText.split(" ");
        for(int i=0;i<cli.length;i++){
            if(cli[i].trim().equalsIgnoreCase("-w")){

                try{
                    setWindowSize(Integer.parseInt(cli[i+1].trim()));
                } catch (NumberFormatException e) {
                    //e.printStackTrace();
                    String subString=cli[i+1].trim();
                    int subStringLength=subString.length();
                    setWindowSize(Integer.parseInt(subString.substring(0,subStringLength-1)));//Deal with sting like "505)".
                }
            }
        }
    }

    public void setTaskString(String cliString) {
        setTaskString(cliString, true);
    }

    public void setTaskString(String cliString, boolean storePreference) {
        try {
            this.currentTask = (MainTask) ClassOption.cliStringToObject(
                    cliString, MainTask.class, null);
            String taskText = this.currentTask.getCLICreationString(MainTask.class);
            this.taskDescField.setText(taskText);
            if (storePreference == true){
                //Save task text as a preference
                prefs.put(PREF_NAME, taskText);
            }
        } catch (Exception ex) {
            GUIUtils.showExceptionDialog(this, "Problem with task", ex);
        }
    }

    public void copyClipBoardConfiguration() {
        StringSelection selection = new StringSelection(this.taskDescField.getText().trim());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }



    /**
     * Tells the panel to use a new base set of instances.
     *
     * @param inst a set of Instances
     */
    public void setInstances(Instances inst) {
        m_Instances = inst;
        featureImportanceDataModelPanel.setInstances(inst);
    }


    /**
     * We can accept instances
     *
     * @return true
     */
    @Override
    public boolean acceptsInstances() {
        return true;
    }

    public static void main(String[] args) {
        WekaToSamoaInstanceConverter m_wekaToSamoaInstanceConverter = new WekaToSamoaInstanceConverter();
        try {
            if (args.length == 0) {
                throw new Exception("supply the name of an arff file");
            }
            weka.core.Instances i = new weka.core.Instances(new java.io.BufferedReader(
                    new java.io.FileReader(args[0])));
            FeatureImportancePanel fip = new FeatureImportancePanel();
            final JFrame jf = new JFrame(
                    "Feature importance data model panel");
            jf.setLayout(new BorderLayout());
            jf.add(fip, BorderLayout.CENTER);
            jf.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    jf.dispose();
                    System.exit(0);
                }
            });
            jf.setSize(800,400);
            //jf.pack();
            jf.setVisible(true);
            fip.setInstances(m_wekaToSamoaInstanceConverter.samoaInstances(i));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println(ex.getMessage());
        }
    }

    public void runTask(Task task) {
        TaskThread thread = new TaskThread(task);
        this.taskList.add(0, thread);
        thread.start();
    }

}
