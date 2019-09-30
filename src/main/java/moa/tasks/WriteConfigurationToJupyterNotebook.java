package moa.tasks;

import java.io.*;
import java.util.ArrayList;

import com.github.javacliparser.FlagOption;
import moa.core.ObjectRepository;
import moa.evaluation.preview.LearningCurve;
import moa.options.ClassOption;
import com.github.javacliparser.FileOption;
import org.apache.commons.math3.util.Pair;
import moa.evaluation.WindowClassificationPerformanceEvaluator;
import moa.evaluation.EWMAClassificationPerformanceEvaluator;
import moa.evaluation.FadingFactorClassificationPerformanceEvaluator;

/**
 * Export the configuration of an training method form MOA to a IPYNB file
 *
 * @author Truong To (todinhtruong at gmail dot com)
 */
public class WriteConfigurationToJupyterNotebook extends AuxiliarMainTask {
    @Override
    /**
     * Gets the purpose of this object
     *
     * @return the string with the purpose of this object
     */
    public String getPurposeString() {
        return "Outputs a task to a Jupyter Notebook.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption taskOption = new ClassOption("task", 't',
            "Task to do.", Task.class, "EvaluateInterleavedTestThenTrain -l trees.HoeffdingTree -s (ArffFileStream -f H:/MLProject/Dataset/elecNormNew.arff) -i 1000000 -f 10000");

    public FileOption notebookOutputFile = new FileOption("NotebookFile", 'j',
            "Destination Jupyter Notebook file.", null, "ipynb", true);

    public FlagOption runConfig = new FlagOption("runConfig", 'r', "If checked, run the configuration beside exporting the it to Notebook file.");

    public FlagOption exportAdvancedNotebook = new FlagOption("exportAdvancedNotebook", 'e', "Check this if you want to export the code advanced notebook");

    protected Task task;

    protected String cells = "";

    /**
     * Class for creating a IPYNB file
     *
     *
     */
    public class JSONCell {
        private String cell_type = "code";  //1
        private int execution_count = 1;    //2
        private String metadata = "";      //3
        private String outputs = "";       //4
        private String source = "";       //5
        private String result = "";

        public JSONCell() {

        }

        public JSONCell(String str) {
            this.cell_type = str;
        }

        /**
         * Adds a string to cell in a new separate line
         * @param st the string to be added
         * @param mode
         */
        public void addNewLineToCell(String st, int mode) {
            switch (mode) {
                case 1:
                    if (cell_type != "")
                        this.cell_type = this.cell_type + ",\n";
                    this.cell_type = this.cell_type + "\"" + st + "\\n\"";
                    break;

                case 2:
                    this.execution_count = Integer.valueOf(st);
                    break;

                case 3:
                    if (metadata != "")
                        this.metadata = this.metadata + ",\n";
                    this.metadata = this.metadata + "\"" + st + "\\n\"";
                    break;

                case 4:
                    if (outputs != "")
                        this.outputs = this.outputs + ",\n";
                    this.outputs = this.outputs + "\"" + st + "\\n\"";
                    break;

                case 5:
                    if (source != "")
                        this.source = this.source + ",\n";
                    this.source = this.source + "\"" + st + "\\n\"";
                    break;
            }
        }

        /**
         * Adds a string to cell in a current line at the last position before the return (\n) character
         * @param st the string to be added
         * @param mode
         */
        void addToCell(String st, int mode) {
            StringBuffer newString;
            switch (mode) {
                case 1:
                    newString = new StringBuffer(this.cell_type);
                    newString.insert(this.cell_type.length() - 3, st);
                    this.cell_type = newString.toString();
                    break;

                case 2:
                    this.execution_count = Integer.valueOf(st);
                    break;

                case 3:
                    newString = new StringBuffer(this.metadata);
                    newString.insert(this.metadata.length() - 3, st);
                    this.metadata = newString.toString();
                    break;

                case 4:
                    newString = new StringBuffer(this.outputs);
                    newString.insert(this.outputs.length() - 3, st);
                    this.outputs = newString.toString();
                    break;

                case 5:
                    newString = new StringBuffer(this.source);
                    newString.insert(this.source.length() - 3, st);
                    this.source = newString.toString();
                    break;
            }
        }

        /**
         * Creates a cell with the right format
         */
        public void createCell() {
            this.result = this.result + "\"" + "cell_type" + "\"" + ": " + "\"" + this.cell_type + "\",\n";
            if (!this.cell_type.equals("markdown"))
                this.result = this.result + "\"" + "execution_count" + "\"" + ": " + this.execution_count + ",\n";
            this.result = this.result + "\"" + "metadata" + "\"" + ": " + "{" + this.metadata + "},\n";
            if (!this.cell_type.equals("markdown"))
                this.result = this.result + "\"" + "outputs" + "\"" + ": " + "[" + this.outputs + "],\n";
            this.result = this.result + "\"" + "source" + "\"" + ": " + "[" + this.source + "]\n";
            this.result = "{\n" + this.result + "}";
        }

        public String getCell() {
            return this.result;
        }
    }

    /**
     * This class get input string of learner, stream and evaluator then process them
     * the output will be name of learner, stream, or evaluator besides their options
     **/
    public class optionsString {

        private String classShortName = "";

        private String classFullName = "";

        private String classOptionsString = "";

        private ArrayList<Pair<String, String>> outputClassObjectOptions = new ArrayList();

        private ArrayList<Pair<String, String>> outputClassOptions = new ArrayList();

        private String inputString = "";

        public optionsString(String str) {
            this.inputString = str;
            int i = this.inputString.indexOf(" ");
            if (i > 0) {
                this.classOptionsString = this.inputString.substring(i + 1);
                this.classFullName = this.inputString.substring(0, i);
            } else {
                this.classFullName = this.inputString;
            }
        }

        public String getInputString() {
            return this.inputString;
        }

        public String getClassShortName() {
            return this.classShortName;
        }

        public String getClassFullName() {
            return this.classFullName;
        }

        public void addOptionsStringToCell(JSONCell cell) {
            cell.addNewLineToCell(this.getClassShortName() + " " + this.getClassShortName().substring(0, 4).toLowerCase() +
                    " = new " + this.getClassShortName() + "();", 5);
            if (!this.classOptionsString.equals(""))
                cell.addNewLineToCell(this.getClassShortName().substring(0, 4).toLowerCase() +
                        ".getOptions().setViaCLIString(\\\"" + this.classOptionsString + "\\\");", 5);
        }

        /**
         * Separates out options from command strings
         */
        public void createOptionsList() {
            int j;
            int i;
            String tempClassOptionsString = this.classOptionsString;
            while (tempClassOptionsString.length() > 0) {
                char cliChar = ' ';
                String optionValue = "";
                String str = "";
                tempClassOptionsString = tempClassOptionsString.trim();

                i = tempClassOptionsString.indexOf("-");
                if (i >= 0) {
                    cliChar = tempClassOptionsString.charAt(i + 1);
                    tempClassOptionsString = tempClassOptionsString.substring(i + 2).trim();
                    if (tempClassOptionsString.length() == 0) {
                        optionValue = "true";
                        Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
                        this.outputClassOptions.add(optionPair);
                    } else {
                        if (tempClassOptionsString.charAt(0) == '-') {
                            optionValue = "true";
                            Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
                            this.outputClassOptions.add(optionPair);
                        } else if (tempClassOptionsString.charAt(0) == '(') {
                            int openBracket = 0;
                            int closeBracket = 0;
                            StringBuffer temp = new StringBuffer("");
                            for (int k = 0; k < tempClassOptionsString.length(); k++) {
                                char cTemp = tempClassOptionsString.charAt(k);
                                temp.append(cTemp);
                                switch (cTemp) {
                                    case '(': {
                                        openBracket += 1;
                                        break;
                                    }
                                    case ')': {
                                        closeBracket += 1;
                                        if (closeBracket == openBracket) {
                                            tempClassOptionsString = tempClassOptionsString.substring(k + 1).trim();
                                            optionValue = temp.toString().trim();
                                            optionValue = optionValue.substring(1, optionValue.length() - 1);
                                            Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
                                            this.outputClassObjectOptions.add(optionPair);
                                            optionsString subObject = new optionsString(optionValue);
                                        }
                                        break;
                                    }
                                }
                            }


                        } else {
                            j = tempClassOptionsString.indexOf(" ");
                            if (j > 0) {
                                optionValue = tempClassOptionsString.substring(0, j);
                                tempClassOptionsString = tempClassOptionsString.substring(j + 1).trim();
                                Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
                                this.outputClassOptions.add(optionPair);
                            } else {
                                optionValue = tempClassOptionsString;
                                tempClassOptionsString = "";
                                Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
                                this.outputClassOptions.add(optionPair);
                            }
                        }
                    }

                }
            }

            i = this.classFullName.lastIndexOf('.');
            if (i > 0) {
                this.classShortName = this.classFullName.substring(i + 1);
            } else
                this.classShortName = this.classFullName;
        }
    }

    /**
     * Initialises the first state of flags
     */
    public WriteConfigurationToJupyterNotebook() {
        this.runConfig.unset();
        this.exportAdvancedNotebook.unset();
    }

    /**
     * Creates all cells of the IPYNB file
     * @param jsonCells
     */
    public void createAllCells(ArrayList<JSONCell> jsonCells) {
        for (int i = 0; i < jsonCells.size(); i++) {
            jsonCells.get(i).addToCell(Integer.toString(i + 1), 2);
            this.cells = this.cells + "\n" + jsonCells.get(i).getCell() + ",\n";
        }
        this.cells = this.cells.substring(0, this.cells.length() - 2);
        this.cells = "{\n" + "\"" + "cells" + "\": [" + this.cells + "],\n";
        this.cells = this.cells + "\"metadata\": {},\n\"nbformat\": 4,\n\"nbformat_minor\": 0\n}\n";
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        Object result = null;
        try {
            this.task = (Task) getPreparedClassOption(this.taskOption);

            if ((this.task instanceof EvaluateInterleavedTestThenTrain) || (this.task instanceof EvaluatePrequential) ||
                    (this.task instanceof EvaluatePrequentialCV) || (this.task instanceof EvaluatePrequentialDelayed) ||
                    (this.task instanceof EvaluatePrequentialDelayedCV) || (this.task instanceof EvaluatePrequentialRegression)) {
                LearningCurve learningCurve = new LearningCurve(
                        "learning evaluation instances");
                File destFile = this.notebookOutputFile.getFile();
                if (destFile != null) {
                    try {
                        Writer w = new BufferedWriter(new FileWriter(destFile));
                        monitor.setCurrentActivityDescription("Writing configuration to IPYNB file");

                        Task currentTask = this.task;
                        String streamString = null, learnerString = null, evaluatorString = null;
                        File dumpFile = null;
                        File outputPredictionFile = null;
                        int randomSeed = 1;
                        int instanceLimit = 100000000;
                        int timeLimit = -1;
                        int sampleFrequency = 100000;
                        int numFolds = 10;
                        int validationMethodology = 0;
                        int initialWindowSize = 1000;
                        int delayLength = 1000;
                        boolean trainOnInitialWindow = false;
                        boolean trainInBatches = false;
                        int widthOption = 1000;
                        double alphaOption = .01;
                        int cellNum = 1;

                        /** Gets out the values of all parameters necessary for creating detailed version of IPYNB file **/
                        if (currentTask instanceof EvaluateInterleavedTestThenTrain) {
                            String classifier = ((EvaluateInterleavedTestThenTrain) currentTask).learnerOption.getValueAsCLIString();
                            ((EvaluateInterleavedTestThenTrain) currentTask).learnerOption.setValueViaCLIString(classifier);

                            streamString = ((EvaluateInterleavedTestThenTrain) currentTask).streamOption.getValueAsCLIString().replace('\\', '/');
                            learnerString = ((EvaluateInterleavedTestThenTrain) currentTask).learnerOption.getValueAsCLIString().replace('\\', '/');
                            evaluatorString = ((EvaluateInterleavedTestThenTrain) currentTask).evaluatorOption.getValueAsCLIString().replace('\\', '/');
                            dumpFile = ((EvaluateInterleavedTestThenTrain) currentTask).dumpFileOption.getFile();
                            randomSeed = ((EvaluateInterleavedTestThenTrain) currentTask).randomSeedOption.getValue();
                            sampleFrequency = ((EvaluateInterleavedTestThenTrain) currentTask).sampleFrequencyOption.getValue();
                            instanceLimit = ((EvaluateInterleavedTestThenTrain) currentTask).instanceLimitOption.getValue();
                        } else if (currentTask instanceof EvaluatePrequential) {
                            String classifier = ((EvaluatePrequential) currentTask).learnerOption.getValueAsCLIString();
                            ((EvaluatePrequential) currentTask).learnerOption.setValueViaCLIString(classifier);
                            streamString = ((EvaluatePrequential) currentTask).streamOption.getValueAsCLIString().replace('\\', '/');
                            learnerString = ((EvaluatePrequential) currentTask).learnerOption.getValueAsCLIString().replace('\\', '/');
                            evaluatorString = ((EvaluatePrequential) currentTask).evaluatorOption.getValueAsCLIString().replace('\\', '/');
                            dumpFile = ((EvaluatePrequential) currentTask).dumpFileOption.getFile();
                            outputPredictionFile = ((EvaluatePrequential) currentTask).outputPredictionFileOption.getFile();
                            sampleFrequency = ((EvaluatePrequential) currentTask).sampleFrequencyOption.getValue();
                            instanceLimit = ((EvaluatePrequential) currentTask).instanceLimitOption.getValue();

                            //New for prequential methods
                            if (getPreparedClassOption(((EvaluatePrequential) currentTask).evaluatorOption) instanceof WindowClassificationPerformanceEvaluator) {
                                //((WindowClassificationPerformanceEvaluator) evaluator).setWindowWidth(widthOption.getValue());
                                if (((EvaluatePrequential) currentTask).widthOption.getValue() != 1000) {
                                    System.out.println("DEPRECATED! Use EvaluatePrequential -e (WindowClassificationPerformanceEvaluator -w "
                                            + ((EvaluatePrequential) currentTask).widthOption.getValue() + ")");
                                    return learningCurve;
                                }
                            }
                            if (getPreparedClassOption(((EvaluatePrequential) currentTask).evaluatorOption) instanceof EWMAClassificationPerformanceEvaluator) {
                                //((EWMAClassificationPerformanceEvaluator) evaluator).setalpha(alphaOption.getValue());
                                if (((EvaluatePrequential) currentTask).alphaOption.getValue() != .01) {
                                    System.out.println("DEPRECATED! Use EvaluatePrequential -e (EWMAClassificationPerformanceEvaluator -a "
                                            + ((EvaluatePrequential) currentTask).alphaOption.getValue() + ")");
                                    return learningCurve;
                                }
                            }
                            if (getPreparedClassOption(((EvaluatePrequential) currentTask).evaluatorOption) instanceof FadingFactorClassificationPerformanceEvaluator) {
                                //((FadingFactorClassificationPerformanceEvaluator) evaluator).setalpha(alphaOption.getValue());
                                if (((EvaluatePrequential) currentTask).alphaOption.getValue() != .01) {
                                    System.out.println("DEPRECATED! Use EvaluatePrequential -e (FadingFactorClassificationPerformanceEvaluator -a "
                                            + ((EvaluatePrequential) currentTask).alphaOption.getValue() + ")");
                                    return learningCurve;
                                }
                            }
                            //End New for prequential methods
                        } else if (currentTask instanceof EvaluatePrequentialCV) {
                            String classifier = ((EvaluatePrequentialCV) currentTask).learnerOption.getValueAsCLIString();
                            ((EvaluatePrequentialCV) currentTask).learnerOption.setValueViaCLIString(classifier);
                            streamString = ((EvaluatePrequentialCV) currentTask).streamOption.getValueAsCLIString().replace('\\', '/');
                            learnerString = ((EvaluatePrequentialCV) currentTask).learnerOption.getValueAsCLIString().replace('\\', '/');
                            evaluatorString = ((EvaluatePrequentialCV) currentTask).evaluatorOption.getValueAsCLIString().replace('\\', '/');
                            dumpFile = ((EvaluatePrequentialCV) currentTask).dumpFileOption.getFile();
                            sampleFrequency = ((EvaluatePrequentialCV) currentTask).sampleFrequencyOption.getValue();
                            instanceLimit = ((EvaluatePrequentialCV) currentTask).instanceLimitOption.getValue();
                            randomSeed = ((EvaluatePrequentialCV) currentTask).randomSeedOption.getValue();
                            numFolds = ((EvaluatePrequentialCV) currentTask).numFoldsOption.getValue();
                            validationMethodology = ((EvaluatePrequentialCV) currentTask).validationMethodologyOption.getChosenIndex();

                        } else if (currentTask instanceof EvaluatePrequentialDelayed) {
                            String classifier = ((EvaluatePrequentialDelayed) currentTask).learnerOption.getValueAsCLIString();
                            ((EvaluatePrequentialDelayed) currentTask).learnerOption.setValueViaCLIString(classifier);
                            streamString = ((EvaluatePrequentialDelayed) currentTask).streamOption.getValueAsCLIString().replace('\\', '/');
                            learnerString = ((EvaluatePrequentialDelayed) currentTask).learnerOption.getValueAsCLIString().replace('\\', '/');
                            evaluatorString = ((EvaluatePrequentialDelayed) currentTask).evaluatorOption.getValueAsCLIString().replace('\\', '/');
                            dumpFile = ((EvaluatePrequentialDelayed) currentTask).dumpFileOption.getFile();
                            outputPredictionFile = ((EvaluatePrequentialDelayed) currentTask).outputPredictionFileOption.getFile();
                            sampleFrequency = ((EvaluatePrequentialDelayed) currentTask).sampleFrequencyOption.getValue();
                            instanceLimit = ((EvaluatePrequentialDelayed) currentTask).instanceLimitOption.getValue();
                            trainOnInitialWindow = ((EvaluatePrequentialDelayed) currentTask).trainOnInitialWindowOption.isSet();
                            initialWindowSize = ((EvaluatePrequentialDelayed) currentTask).initialWindowSizeOption.getValue();
                            delayLength = ((EvaluatePrequentialDelayed) currentTask).delayLengthOption.getValue();
                            trainInBatches = ((EvaluatePrequentialDelayed) currentTask).trainInBatches.isSet();

                            //New for prequential methods
                            if (getPreparedClassOption(((EvaluatePrequentialDelayed) currentTask).evaluatorOption) instanceof WindowClassificationPerformanceEvaluator) {
                                //((WindowClassificationPerformanceEvaluator) evaluator).setWindowWidth(widthOption.getValue());
                                if (((EvaluatePrequentialDelayed) currentTask).widthOption.getValue() != 1000) {
                                    System.out.println("DEPRECATED! Use EvaluatePrequential -e (WindowClassificationPerformanceEvaluator -w "
                                            + ((EvaluatePrequentialDelayed) currentTask).widthOption.getValue() + ")");
                                    return learningCurve;
                                }
                            }
                            if (getPreparedClassOption(((EvaluatePrequentialDelayed) currentTask).evaluatorOption) instanceof EWMAClassificationPerformanceEvaluator) {
                                //((EWMAClassificationPerformanceEvaluator) evaluator).setalpha(alphaOption.getValue());
                                if (((EvaluatePrequentialDelayed) currentTask).alphaOption.getValue() != .01) {
                                    System.out.println("DEPRECATED! Use EvaluatePrequential -e (EWMAClassificationPerformanceEvaluator -a "
                                            + ((EvaluatePrequentialDelayed) currentTask).alphaOption.getValue() + ")");
                                    return learningCurve;
                                }
                            }
                            if (getPreparedClassOption(((EvaluatePrequentialDelayed) currentTask).evaluatorOption) instanceof FadingFactorClassificationPerformanceEvaluator) {
                                //((FadingFactorClassificationPerformanceEvaluator) evaluator).setalpha(alphaOption.getValue());
                                if (((EvaluatePrequentialDelayed) currentTask).alphaOption.getValue() != .01) {
                                    System.out.println("DEPRECATED! Use EvaluatePrequential -e (FadingFactorClassificationPerformanceEvaluator -a "
                                            + ((EvaluatePrequentialDelayed) currentTask).alphaOption.getValue() + ")");
                                    return learningCurve;
                                }
                            }
                            //End New for prequential methods

                        } else if (currentTask instanceof EvaluatePrequentialDelayedCV) {
                            String classifier = ((EvaluatePrequentialDelayedCV) currentTask).learnerOption.getValueAsCLIString();
                            ((EvaluatePrequentialDelayedCV) currentTask).learnerOption.setValueViaCLIString(classifier);
                            streamString = ((EvaluatePrequentialDelayedCV) currentTask).streamOption.getValueAsCLIString().replace('\\', '/');
                            learnerString = ((EvaluatePrequentialDelayedCV) currentTask).learnerOption.getValueAsCLIString().replace('\\', '/');
                            evaluatorString = ((EvaluatePrequentialDelayedCV) currentTask).evaluatorOption.getValueAsCLIString().replace('\\', '/');
                            dumpFile = ((EvaluatePrequentialDelayedCV) currentTask).dumpFileOption.getFile();
                            randomSeed = ((EvaluatePrequentialDelayedCV) currentTask).randomSeedOption.getValue();
                            sampleFrequency = ((EvaluatePrequentialDelayedCV) currentTask).sampleFrequencyOption.getValue();
                            instanceLimit = ((EvaluatePrequentialDelayedCV) currentTask).instanceLimitOption.getValue();
                            numFolds = ((EvaluatePrequentialDelayedCV) currentTask).numFoldsOption.getValue();
                            delayLength = ((EvaluatePrequentialDelayedCV) currentTask).delayLengthOption.getValue();
                            validationMethodology = ((EvaluatePrequentialDelayedCV) currentTask).validationMethodologyOption.getChosenIndex();
                        } else if (currentTask instanceof EvaluatePrequentialRegression) {
                            String classifier = ((EvaluatePrequentialRegression) currentTask).learnerOption.getValueAsCLIString();
                            ((EvaluatePrequentialRegression) currentTask).learnerOption.setValueViaCLIString(classifier);
                            streamString = ((EvaluatePrequentialRegression) currentTask).streamOption.getValueAsCLIString().replace('\\', '/');
                            learnerString = ((EvaluatePrequentialRegression) currentTask).learnerOption.getValueAsCLIString().replace('\\', '/');
                            evaluatorString = ((EvaluatePrequentialRegression) currentTask).evaluatorOption.getValueAsCLIString().replace('\\', '/');
                            sampleFrequency = ((EvaluatePrequentialRegression) currentTask).sampleFrequencyOption.getValue();
                            instanceLimit = ((EvaluatePrequentialRegression) currentTask).instanceLimitOption.getValue();
                            dumpFile = ((EvaluatePrequentialRegression) currentTask).dumpFileOption.getFile();
                            outputPredictionFile = ((EvaluatePrequentialRegression) currentTask).outputPredictionFileOption.getFile();

                            //New for prequential methods
                            if (getPreparedClassOption(((EvaluatePrequentialRegression) currentTask).evaluatorOption) instanceof WindowClassificationPerformanceEvaluator) {
                                //((WindowClassificationPerformanceEvaluator) evaluator).setWindowWidth(widthOption.getValue());
                                if (((EvaluatePrequentialRegression) currentTask).widthOption.getValue() != 1000) {
                                    System.out.println("DEPRECATED! Use EvaluatePrequential -e (WindowClassificationPerformanceEvaluator -w "
                                            + ((EvaluatePrequentialRegression) currentTask).widthOption.getValue() + ")");
                                    return learningCurve;
                                }
                            }
                            if (getPreparedClassOption(((EvaluatePrequentialRegression) currentTask).evaluatorOption) instanceof EWMAClassificationPerformanceEvaluator) {
                                //((EWMAClassificationPerformanceEvaluator) evaluator).setalpha(alphaOption.getValue());
                                if (((EvaluatePrequentialRegression) currentTask).alphaOption.getValue() != .01) {
                                    System.out.println("DEPRECATED! Use EvaluatePrequential -e (EWMAClassificationPerformanceEvaluator -a "
                                            + ((EvaluatePrequentialRegression) currentTask).alphaOption.getValue() + ")");
                                    return learningCurve;
                                }
                            }
                            if (getPreparedClassOption(((EvaluatePrequentialRegression) currentTask).evaluatorOption) instanceof FadingFactorClassificationPerformanceEvaluator) {
                                //((FadingFactorClassificationPerformanceEvaluator) evaluator).setalpha(alphaOption.getValue());
                                if (((EvaluatePrequentialRegression) currentTask).alphaOption.getValue() != .01) {
                                    System.out.println("DEPRECATED! Use EvaluatePrequential -e (FadingFactorClassificationPerformanceEvaluator -a "
                                            + ((EvaluatePrequentialRegression) currentTask).alphaOption.getValue() + ")");
                                    return learningCurve;
                                }
                            }
                            //End New for prequential methods
                        }

                        optionsString learnerOptionsString = new optionsString(learnerString);
                        learnerOptionsString.createOptionsList();
                        optionsString streamOptionsString = new optionsString(streamString);
                        streamOptionsString.createOptionsList();
                        optionsString evaluatorOptionsString = new optionsString(evaluatorString);
                        evaluatorOptionsString.createOptionsList();

                        String dumpFilePath = null;
                        if (dumpFile != null) {
                            dumpFilePath = dumpFile.getAbsolutePath().replace('\\', '/');
                            ;
                        }

                        //create array of JSON cells
                        ArrayList<JSONCell> jsonCells = new ArrayList<JSONCell>();
                        if (this.exportAdvancedNotebook.isSet()) {
                            /**---------------------------------------------------------------
                             * beginning of the content of the IPYNB file**/

                            //import all necessary library files
                            JSONCell md1 = new JSONCell("markdown");
                            md1.addNewLineToCell("This IPYNB file was generated automatically by MOA GUI.<br>", 5);
                            md1.addNewLineToCell("Task name: " + currentTask.getClass().getName() + "<br>", 5);
                            md1.addNewLineToCell("## Libraries importing", 5);
                            JSONCell c1 = new JSONCell();
                            c1.addNewLineToCell(String.valueOf(cellNum), 2);
                            c1.addNewLineToCell("%maven nz.ac.waikato.cms.moa:moa:2019.05.0", 5);
                            c1.addNewLineToCell("%classpath \\\"H:/MOA/jbutils.jar\\\"", 5);
                            c1.addNewLineToCell("import moa.classifiers." + learnerOptionsString.getClassFullName() + ";", 5);
                            c1.addNewLineToCell("import moa.streams." + streamOptionsString.getClassFullName() + ";", 5);
                            c1.addNewLineToCell("import moa.evaluation." + evaluatorOptionsString.getClassFullName() + ";", 5);
                            c1.addNewLineToCell("import moa.evaluation.LearningEvaluation;", 5);
                            c1.addNewLineToCell("import moa.evaluation.preview.LearningCurve;", 5);
                            c1.addNewLineToCell("import moa.core.TimingUtils;", 5);
                            c1.addNewLineToCell("import moa.tasks.MainTask;", 5);
                            c1.addNewLineToCell("import com.yahoo.labs.samoa.instances.Instance;", 5);
                            c1.addNewLineToCell("import moa.core.Example;", 5);
                            c1.addNewLineToCell("import moa.core.Measurement;", 5);
                            c1.addNewLineToCell("import moa.jbutils.*;", 5);
                            /**-----------------Create second cell for initializing the learner, the stream and the evaluator----------------- **/
                            JSONCell md2 = new JSONCell("markdown");
                            md2.addNewLineToCell("## Configuring learner, stream and evaluator", 5);

                            JSONCell c2 = new JSONCell();
                            cellNum++;
                            c2.addNewLineToCell(String.valueOf(cellNum), 2);
                            c2.addNewLineToCell("String learnerString = \\\"" + learnerString + "\\\";", 5);

                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV)) {
                                c1.addNewLineToCell("import java.util.Random;", 5);
                                c2.addNewLineToCell("int randomSeed =" + randomSeed + ";", 5);
                                c2.addNewLineToCell("Random random = new Random(randomSeed);", 5);
                                /**-----------------create learner----------------- **/
                                c2.addNewLineToCell("int numFolds = " + numFolds + ";", 5);
                                c2.addNewLineToCell(learnerOptionsString.getClassShortName() + "[] " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array = new " + learnerOptionsString.getClassShortName() + "[numFolds];", 5);
                                learnerOptionsString.addOptionsStringToCell(c2);
                                c2.addNewLineToCell(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();", 5);
                                c2.addNewLineToCell(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".resetLearning();", 5);
                                //Learner[] learners = new Learner[numFoldsOption];
                                //Learner baseLearner = (Learner) getPreparedClassOption(this.learnerOption);
                                //baseLearner.resetLearning();
                                /**--------------create stream------------**/
                                c2.addNewLineToCell("", 5);
                                c2.addNewLineToCell("String streamString = \\\"" + streamString + "\\\";", 5);
                                streamOptionsString.addOptionsStringToCell(c2);
                                c2.addNewLineToCell(streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();", 5);
                                c2.addNewLineToCell(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".setModelContext("
                                        + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getHeader());", 5);
                                /**--------------create evaluator-----------**/
                                c2.addNewLineToCell("", 5);
                                c2.addNewLineToCell("String evaluatorString = \\\"" + evaluatorString + "\\\";", 5);
                                c2.addNewLineToCell(evaluatorOptionsString.getClassShortName() + "[] " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array = new " + evaluatorOptionsString.getClassShortName() + "[numFolds];", 5);
                                evaluatorOptionsString.addOptionsStringToCell(c2);
                                c2.addNewLineToCell("for (int i = 0; i <" + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length; i++) {", 5);
                                c2.addNewLineToCell("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array[i] = (" + learnerOptionsString.getClassShortName() + ") " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".copy();", 5);
                                c2.addNewLineToCell("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array[i].setModelContext(" + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getHeader());", 5);
                                c2.addNewLineToCell("    " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "Array[i] = (" +
                                        evaluatorOptionsString.getClassShortName() + ") " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".copy();", 5);
                                c2.addNewLineToCell("}", 5);
                            } else {
                                /**-----------------create learner----------------- **/
                                learnerOptionsString.addOptionsStringToCell(c2);
                                c2.addNewLineToCell(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();", 5);
                                if (currentTask instanceof EvaluateInterleavedTestThenTrain) {
                                    c2.addNewLineToCell("int randomSeed = " + randomSeed + ";", 5);
                                    c2.addNewLineToCell("if (" + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".isRandomizable()) {", 5);
                                    c2.addNewLineToCell("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".setRandomSeed(randomSeed);", 5);
                                    c2.addNewLineToCell("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".resetLearning();", 5);
                                    c2.addNewLineToCell("}", 5);
                                }
                                /**--------------create stream------------**/
                                c2.addNewLineToCell("", 5);
                                c2.addNewLineToCell("String streamString = \\\"" + streamString + "\\\";", 5);
                                streamOptionsString.addOptionsStringToCell(c2);
                                c2.addNewLineToCell(streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();", 5);
                                c2.addNewLineToCell(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".setModelContext("
                                        + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getHeader());", 5);
                                /**--------------create evaluator-----------**/
                                c2.addNewLineToCell("", 5);
                                c2.addNewLineToCell("String evaluatorString = \\\"" + evaluatorString + "\\\";", 5);
                                evaluatorOptionsString.addOptionsStringToCell(c2);
                            }
                            md1.createCell();
                            c1.createCell();
                            md2.createCell();
                            c2.createCell();
                            jsonCells.add(md1);
                            jsonCells.add(c1);
                            jsonCells.add(md2);
                            jsonCells.add(c2);

                            /**--------------set environment parameters----------- **/
                            JSONCell md3 = new JSONCell("markdown");
                            md3.addNewLineToCell("## Setting environmental parameters", 5);
                            JSONCell c3 = new JSONCell();
                            cellNum++;
                            c3.addNewLineToCell(String.valueOf(cellNum), 2);
                            c3.addNewLineToCell("int maxInstances = " + instanceLimit + ";", 5);
                            c3.addNewLineToCell("long instancesProcessed = 0;", 5);
                            c3.addNewLineToCell("int maxSeconds = " + timeLimit + ";", 5);
                            c3.addNewLineToCell("int secondsElapsed = 0;", 5);
                            c3.addNewLineToCell("LearningCurve learningCurve = new LearningCurve(\\\"learning evaluation instances\\\");", 5);
                            c3.addNewLineToCell("File dumpFile = new File(\\\"" + dumpFilePath + "\\\");", 5);
                            c3.addNewLineToCell("PrintStream immediateResultStream = null;", 5);
                            c3.addNewLineToCell("if (dumpFile != null) {", 5);
                            c3.addNewLineToCell("    try {", 5);
                            c3.addNewLineToCell("        if (dumpFile.exists()) {", 5);
                            c3.addNewLineToCell("            immediateResultStream = new PrintStream(", 5);
                            c3.addNewLineToCell("                    new FileOutputStream(dumpFile, true), true);", 5);
                            c3.addNewLineToCell("        } else {", 5);
                            c3.addNewLineToCell("            immediateResultStream = new PrintStream(", 5);
                            c3.addNewLineToCell("                    new FileOutputStream(dumpFile), true);", 5);
                            c3.addNewLineToCell("        }", 5);
                            c3.addNewLineToCell("    } catch (Exception ex) {", 5);
                            c3.addNewLineToCell("        throw new RuntimeException(", 5);
                            c3.addNewLineToCell("                \\\"Unable to open immediate result file: \\\" + dumpFile, ex);", 5);
                            c3.addNewLineToCell("    }", 5);
                            c3.addNewLineToCell("}", 5);

                            if ((currentTask instanceof EvaluatePrequential) || (currentTask instanceof EvaluatePrequentialDelayed) ||
                                    (currentTask instanceof EvaluatePrequentialRegression)) {
                                //File for output predictions
                                if (outputPredictionFile != null) {
                                    c3.addNewLineToCell("File outputPredictionFile = new File(\\\"" +
                                            outputPredictionFile.getAbsolutePath().replace('\\', '/') + "\\\");", 5);
                                } else c3.addNewLineToCell("File outputPredictionFile = null;", 5);

                                c3.addNewLineToCell("PrintStream outputPredictionResultStream = null;", 5);
                                c3.addNewLineToCell("if (outputPredictionFile != null) {", 5);
                                c3.addNewLineToCell("    try {", 5);
                                c3.addNewLineToCell("        if (outputPredictionFile.exists()) {", 5);
                                c3.addNewLineToCell("            outputPredictionResultStream = new PrintStream(", 5);
                                c3.addNewLineToCell("                    new FileOutputStream(outputPredictionFile, true), true);", 5);
                                c3.addNewLineToCell("        } else {", 5);
                                c3.addNewLineToCell("            outputPredictionResultStream = new PrintStream(", 5);
                                c3.addNewLineToCell("                    new FileOutputStream(outputPredictionFile), true);", 5);
                                c3.addNewLineToCell("        }", 5);
                                c3.addNewLineToCell("    } catch (Exception ex) {", 5);
                                c3.addNewLineToCell("        throw new RuntimeException(", 5);
                                c3.addNewLineToCell("                \\\"Unable to open prediction result file: \\\" + outputPredictionFile, ex);", 5);
                                c3.addNewLineToCell("    }", 5);
                                c3.addNewLineToCell("}", 5);
                                if (currentTask instanceof EvaluatePrequentialDelayed) {
                                    c1.addNewLineToCell("import java.util.LinkedList;", 5);
                                    c3.addNewLineToCell("LinkedList<Example> trainInstances = new LinkedList<Example>();", 5);
                                }
                            } else if (currentTask instanceof EvaluatePrequentialDelayedCV) {
                                c1.addNewLineToCell("import java.util.LinkedList;", 5);
                                c3.addNewLineToCell("LinkedList<LinkedList<Example>> trainInstances = new LinkedList<LinkedList<Example>>();", 5);

                                c3.addNewLineToCell("for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length; i++) {", 5);
                                c3.addNewLineToCell("   trainInstances.add(new LinkedList<Example>());", 5);
                                c3.addNewLineToCell("}", 5);
                            }
                            c3.addNewLineToCell("boolean firstDump = true;", 5);
                            c3.addNewLineToCell("boolean firstPrint = true;", 5);
                            c3.addNewLineToCell("boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();", 5);
                            c3.addNewLineToCell("long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();", 5);
                            c3.addNewLineToCell("long lastEvaluateStartTime = evaluateStartTime;", 5);
                            c3.addNewLineToCell("double RAMHours = 0.0;", 5);
                            // begin the loop
                            JSONCell md5 = new JSONCell("markdown");
                            md5.addNewLineToCell("## Testing & training, exporting result", 5);
                            JSONCell c5 = new JSONCell();
                            cellNum++;
                            c5.addNewLineToCell(String.valueOf(cellNum), 2);
                            c5.addNewLineToCell("TableDraw table = new TableDraw();", 5);
                            c5.addNewLineToCell("System.out.println(\\\"Evaluating learner...\\\");", 5);
                            c5.addNewLineToCell("while (" + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".hasMoreInstances()", 5);
                            c5.addNewLineToCell("        && ((maxInstances < 0) || (instancesProcessed < maxInstances))", 5);
                            c5.addNewLineToCell("        && ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {", 5);

                            if (currentTask instanceof EvaluatePrequentialCV) {
                                c1.addNewLineToCell("import moa.core.MiscUtils;", 5);
                                c5.addNewLineToCell("    Example trainInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".nextInstance();", 5);
                                c5.addNewLineToCell("    Example testInst = trainInst; //.copy();", 5);
                                //testInst.setClassMissing();

                                c5.addNewLineToCell("    for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length; i++) {", 5);
                                c5.addNewLineToCell("       " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array[i].addResult(testInst, " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array[i].getVotesForInstance(testInst));", 5);
                                c5.addNewLineToCell("    }", 5);

                                c5.addNewLineToCell("    for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length; i++) {", 5);
                                c5.addNewLineToCell("        int k = 1;", 5);
                                c5.addNewLineToCell("        int validationMethodology = " + validationMethodology + ";", 5);
                                c5.addNewLineToCell("        switch (validationMethodology) {", 5);
                                c5.addNewLineToCell("            case 0: //Cross-Validation;", 5);
                                c5.addNewLineToCell("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length == i ? 0: 1; //Test all except one", 5);
                                c5.addNewLineToCell("                break;", 5);
                                c5.addNewLineToCell("            case 1: //Bootstrap;", 5);
                                c5.addNewLineToCell("                k = MiscUtils.poisson(1, random);", 5);
                                c5.addNewLineToCell("                break;", 5);
                                c5.addNewLineToCell("            case 2: //Split-Validation;", 5);
                                c5.addNewLineToCell("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length == i ? 1: 0; //Test only one", 5);
                                c5.addNewLineToCell("                break;", 5);
                                c5.addNewLineToCell("        }", 5);
                                c5.addNewLineToCell("        if (k > 0) {", 5);
                                c5.addNewLineToCell("            Example weightedInst = (Example) trainInst.copy();", 5);
                                c5.addNewLineToCell("            weightedInst.setWeight(trainInst.weight() * k);", 5);
                                c5.addNewLineToCell("            " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array[i].trainOnInstance(weightedInst);", 5);
                                c5.addNewLineToCell("        }", 5);
                                c5.addNewLineToCell("    }", 5);
                                c5.addNewLineToCell("    instancesProcessed++;", 5);
                            } else if (currentTask instanceof EvaluatePrequentialDelayed) {
                                c1.addNewLineToCell("import moa.core.Utils;", 5);
                                c1.addNewLineToCell("import moa.core.InstanceExample;", 5);
                                c5.addNewLineToCell("    instancesProcessed++;", 5);
                                c5.addNewLineToCell("    Example currentInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + ".nextInstance();", 5);
                                c5.addNewLineToCell("    boolean trainOnInitialWindow = " + trainOnInitialWindow + ";", 5);
                                c5.addNewLineToCell("    boolean trainInBatches = " + trainInBatches + ";", 5);
                                c5.addNewLineToCell("    int initialWindowSize = " + initialWindowSize + ";", 5);
                                c5.addNewLineToCell("    int delayLength = " + delayLength + ";", 5);
                                c5.addNewLineToCell("    if (instancesProcessed <= initialWindowSize) {", 5);
                                c5.addNewLineToCell("        if (trainOnInitialWindow) {", 5);
                                c5.addNewLineToCell("            " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + ".trainOnInstance(currentInst);", 5);
                                c5.addNewLineToCell("        } else if ((initialWindowSize - instancesProcessed) < delayLength) {", 5);
                                c5.addNewLineToCell("            trainInstances.addLast(currentInst);", 5);
                                c5.addNewLineToCell("        }", 5);
                                c5.addNewLineToCell("    } else {", 5);
                                c5.addNewLineToCell("        trainInstances.addLast(currentInst);", 5);

                                c5.addNewLineToCell("        if (delayLength < trainInstances.size()) {", 5);
                                c5.addNewLineToCell("            if (trainInBatches) {", 5);
                                c5.addNewLineToCell("                // Do not train on the latest instance, otherwise", 5);
                                c5.addNewLineToCell("                // it would train on k+1 instances", 5);
                                c5.addNewLineToCell("                while (trainInstances.size() > 1) {", 5);
                                c5.addNewLineToCell("                    Example trainInst = trainInstances.removeFirst();", 5);
                                c5.addNewLineToCell("                    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + ".trainOnInstance(trainInst);", 5);
                                c5.addNewLineToCell("                }", 5);
                                c5.addNewLineToCell("            } else {", 5);
                                c5.addNewLineToCell("                Example trainInst = trainInstances.removeFirst();", 5);
                                c5.addNewLineToCell("                " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + ".trainOnInstance(trainInst);", 5);
                                c5.addNewLineToCell("            }", 5);
                                c5.addNewLineToCell("        }", 5);

                                c5.addNewLineToCell("        // Remove class label from test instances.", 5);
                                c5.addNewLineToCell("        Instance testInstance = ((Instance) currentInst.getData()).copy();", 5);
                                c5.addNewLineToCell("        testInstance.setMissing(testInstance.classAttribute());", 5);
                                c5.addNewLineToCell("        testInstance.setClassValue(0.0);", 5);

                                c5.addNewLineToCell("        Example testInst = new InstanceExample(testInstance);", 5);
                                c5.addNewLineToCell("        double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + ".getVotesForInstance(testInst);", 5);
                                //          reinstate the testInstance as it is used in evaluator.addResult
                                c5.addNewLineToCell("        testInstance = ((Instance) currentInst.getData()).copy();", 5);
                                c5.addNewLineToCell("        testInst = new InstanceExample(testInstance);", 5);

                                c5.addNewLineToCell("        // Output prediction", 5);
                                c5.addNewLineToCell("        if (outputPredictionFile != null) {", 5);
                                c5.addNewLineToCell("            int trueClass = (int) ((Instance) currentInst.getData()).classValue();", 5);
                                c5.addNewLineToCell("            outputPredictionResultStream.println(Utils.maxIndex(prediction) + \\\",\\\" + (", 5);
                                c5.addNewLineToCell("                    ((Instance) testInst.getData()).classIsMissing() == true ? \\\" ? \\\" : trueClass));", 5);
                                c5.addNewLineToCell("        }", 5);
                                c5.addNewLineToCell("        " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + ".addResult(testInst, prediction);", 5);
                            } else if (currentTask instanceof EvaluatePrequentialDelayedCV) {
                                c1.addNewLineToCell("import moa.core.MiscUtils;", 5);
                                c5.addNewLineToCell("    instancesProcessed++;", 5);
                                c5.addNewLineToCell("    Example trainInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".nextInstance();", 5);
                                c5.addNewLineToCell("    Example testInst = trainInst; //.copy();", 5);
                                c5.addNewLineToCell("    for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length; i++) {", 5);

                                c5.addNewLineToCell("        double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array[i].getVotesForInstance(testInst);", 5);
                                c5.addNewLineToCell("        " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array[i].addResult(testInst, prediction);", 5);

                                c5.addNewLineToCell("        int k = 1;", 5);
                                c5.addNewLineToCell("        int validationMethodology = " + validationMethodology + ";", 5);
                                c5.addNewLineToCell("        switch (validationMethodology) {", 5); /**------------check here----------------**/
                                c5.addNewLineToCell("            case 0: //Cross-Validation;", 5);
                                c5.addNewLineToCell("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length == i ? 0: 1; //Test all except one", 5);
                                c5.addNewLineToCell("                break;", 5);
                                c5.addNewLineToCell("            case 1: //Bootstrap;", 5);
                                c5.addNewLineToCell("                k = MiscUtils.poisson(1, random);", 5);
                                c5.addNewLineToCell("                break;", 5);
                                c5.addNewLineToCell("            case 2: //Split-Validation;", 5);
                                c5.addNewLineToCell("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length == i ? 1: 0; //Test only one", 5);
                                c5.addNewLineToCell("                break;", 5);
                                c5.addNewLineToCell("        }", 5);
                                c5.addNewLineToCell("        if (k > 0) {", 5);
                                c5.addNewLineToCell("            trainInstances.get(i).addLast(trainInst);", 5);
                                c5.addNewLineToCell("        }", 5);
                                c5.addNewLineToCell("        int delayLength = " + delayLength + ";", 5);
                                c5.addNewLineToCell("        if(delayLength < trainInstances.get(i).size()) {", 5);
                                c5.addNewLineToCell("        Example trainInstI = trainInstances.get(i).removeFirst();", 5);
                                c5.addNewLineToCell("        " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array[i].trainOnInstance(trainInstI);", 5);
                                c5.addNewLineToCell("        }", 5);
                                c5.addNewLineToCell("    }", 5);
                            } else {
                                c5.addNewLineToCell("    Example trainInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".nextInstance();", 5);
                                c5.addNewLineToCell("    Example testInst = trainInst; //.copy();", 5);
                                if (currentTask instanceof EvaluatePrequentialRegression) {
                                    c1.addNewLineToCell("import com.yahoo.labs.samoa.instances.Prediction;", 5);
                                    c5.addNewLineToCell("    Prediction prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                            + ".getPredictionForInstance(testInst);", 5);
                                    c5.addNewLineToCell("    if (outputPredictionFile != null) {", 5);
                                    c5.addNewLineToCell("       double trueClass = ((Instance) trainInst.getData()).classValue();", 5);
                                    c5.addNewLineToCell("       outputPredictionResultStream.println(prediction + \\\",\\\" + trueClass);", 5);
                                    c5.addNewLineToCell("    }", 5);
                                } else if (currentTask instanceof EvaluatePrequential) {
                                    c1.addNewLineToCell("import moa.core.Utils;", 5);
                                    c5.addNewLineToCell("    double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getVotesForInstance(testInst);", 5);
                                    c5.addNewLineToCell("    // Output prediction", 5);
                                    c5.addNewLineToCell("    if (outputPredictionFile != null) {", 5);
                                    c5.addNewLineToCell("       int trueClass = (int) ((Instance) trainInst.getData()).classValue();", 5);
                                    c5.addNewLineToCell("       outputPredictionResultStream.println(Utils.maxIndex(prediction) + \\\",\\\" + (", 5);
                                    c5.addNewLineToCell("       ((Instance) testInst.getData()).classIsMissing() == true ? \\\" ? \\\" : trueClass));", 5);
                                    c5.addNewLineToCell("    }", 5);
                                } else
                                    c5.addNewLineToCell("    double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getVotesForInstance(testInst);", 5);

                                c5.addNewLineToCell("    " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".addResult(testInst, prediction);", 5);
                                c5.addNewLineToCell("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".trainOnInstance(trainInst);", 5);
                                c5.addNewLineToCell("    instancesProcessed++;", 5);
                            }
                            c5.addNewLineToCell("    if (instancesProcessed % " + sampleFrequency + " == 0", 5);
                            c5.addNewLineToCell("            ||  " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".hasMoreInstances() == false) {", 5);
                            c5.addNewLineToCell("        long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();", 5);
                            c5.addNewLineToCell("        double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);", 5);
                            c5.addNewLineToCell("        double timeIncrement = TimingUtils.nanoTimeToSeconds(evaluateTime - lastEvaluateStartTime);", 5);
                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV)) {
                                c5.addNewLineToCell("        for (int i = 0; i <" + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length; i++) {", 5);
                                c5.addNewLineToCell("        double RAMHoursIncrement = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array[i].measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs", 5);
                                c5.addNewLineToCell("        RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours", 5);
                                c5.addNewLineToCell("        RAMHours += RAMHoursIncrement;", 5);
                                c5.addNewLineToCell("        }", 5);
                            } else {
                                c5.addNewLineToCell("        double RAMHoursIncrement = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + ".measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs", 5);
                                c5.addNewLineToCell("        RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours", 5);
                                c5.addNewLineToCell("        RAMHours += RAMHoursIncrement;", 5);
                            }
                            c5.addNewLineToCell("        lastEvaluateStartTime = evaluateTime;", 5);

                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV)) {
                                c5.addNewLineToCell("        Measurement[] modelMeasurements = new Measurement[]{", 5);
                                c5.addNewLineToCell("                                           new Measurement(", 5);
                                c5.addNewLineToCell("                                           \\\"learning evaluation instances\\\",", 5);
                                c5.addNewLineToCell("                                           instancesProcessed),", 5);
                                c5.addNewLineToCell("                                           new Measurement(", 5);
                                c5.addNewLineToCell("                                           \\\"evaluation time (\\\"", 5);
                                c5.addNewLineToCell("                                           + (preciseCPUTiming ? \\\"cpu \\\"", 5);
                                c5.addNewLineToCell("                                           : \\\"\\\") + \\\"seconds)\\\",", 5);
                                c5.addNewLineToCell("                                           time),", 5);
                                c5.addNewLineToCell("                                           new Measurement(", 5);
                                c5.addNewLineToCell("                                           \\\"model cost (RAM-Hours)\\\",", 5);
                                c5.addNewLineToCell("                                           RAMHours)", 5);
                                c5.addNewLineToCell("                                        };", 5);
                                c5.addNewLineToCell("        List<Measurement> measurementList = new LinkedList<>();", 5);
                                c5.addNewLineToCell("        if (modelMeasurements != null) {", 5);
                                c5.addNewLineToCell("            measurementList.addAll(Arrays.asList(modelMeasurements));", 5);
                                c5.addNewLineToCell("        }", 5);
                                c5.addNewLineToCell("        // add average of sub-model measurements", 5);
                                c5.addNewLineToCell("        if ((windArray != null) && (windArray.length > 0)) {", 5);
                                c5.addNewLineToCell("            //System.out.println(windArray.length);", 5);
                                c5.addNewLineToCell("            List<Measurement[]> subMeasurements = new LinkedList<>();", 5);
                                c5.addNewLineToCell("            for (" + evaluatorOptionsString.getClassShortName() + " subEvaluator : "
                                        + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "Array) {", 5);
                                c5.addNewLineToCell("                if (subEvaluator != null) {", 5);
                                c5.addNewLineToCell("                   subMeasurements.add(subEvaluator.getPerformanceMeasurements());", 5);
                                c5.addNewLineToCell("                }", 5);
                                c5.addNewLineToCell("            }", 5);
                                c5.addNewLineToCell("            Measurement[] avgMeasurements = Measurement.averageMeasurements(" +
                                        "subMeasurements.toArray(new Measurement[subMeasurements.size()][]));", 5);
                                c5.addNewLineToCell("            measurementList.addAll(Arrays.asList(avgMeasurements));", 5);
                                c5.addNewLineToCell("        }", 5);
                                c5.addNewLineToCell("           learningCurve.insertEntry(new LearningEvaluation(measurementList.toArray(new Measurement[measurementList.size()])));", 5);
                            } else {
                                c5.addNewLineToCell("        learningCurve.insertEntry(new LearningEvaluation(", 5);
                                c5.addNewLineToCell("                new Measurement[]{", 5);
                                c5.addNewLineToCell("                        new Measurement(", 5);
                                c5.addNewLineToCell("                                \\\"learning evaluation instances\\\",", 5);
                                c5.addNewLineToCell("                                instancesProcessed),", 5);
                                c5.addNewLineToCell("                        new Measurement(", 5);
                                c5.addNewLineToCell("                                \\\"evaluation time (\\\"", 5);
                                c5.addNewLineToCell("                                        + (preciseCPUTiming ? \\\"cpu \\\"", 5);
                                c5.addNewLineToCell("                                        : \\\"\\\") + \\\"seconds)\\\",", 5);
                                c5.addNewLineToCell("                                time),", 5);
                                c5.addNewLineToCell("                        new Measurement(", 5);
                                c5.addNewLineToCell("                                \\\"model cost (RAM-Hours)\\\",", 5);
                                c5.addNewLineToCell("                                RAMHours)", 5);
                                c5.addNewLineToCell("                },", 5);
                                c5.addNewLineToCell("                " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ", "
                                        + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "));", 5);
                            }

                            c5.addNewLineToCell("        if (immediateResultStream != null) {", 5);
                            c5.addNewLineToCell("            if (firstDump) {", 5);
                            c5.addNewLineToCell("                immediateResultStream.println(learningCurve.headerToString());", 5);
                            c5.addNewLineToCell("                firstDump = false;", 5);
                            c5.addNewLineToCell("            }", 5);
                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV) ||
                                    (currentTask instanceof EvaluateInterleavedTestThenTrain)) {
                                c5.addNewLineToCell("            immediateResultStream.print(learnerString + \\\",\\\" + streamString + " +
                                        "\\\",\\\"" + "+ Integer.toString(randomSeed) +" + "\\\",\\\");", 5);
                            } else {
                                c5.addNewLineToCell("            immediateResultStream.print(learnerString + \\\",\\\" + streamString);", 5);
                            }

                            c5.addNewLineToCell("            immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));", 5);
                            c5.addNewLineToCell("            immediateResultStream.flush();", 5);
                            c5.addNewLineToCell("        }", 5);
                            c5.addNewLineToCell("        if (firstPrint) {", 5);
                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV) ||
                                    (currentTask instanceof EvaluateInterleavedTestThenTrain)) {
                                c5.addNewLineToCell("           System.out.println(learnerString + \\\",\\\" + streamString + \\\", randomSeed =\\\" "
                                        + "+ Integer.toString(randomSeed) +" + "\\\".\\\");", 5);
                            } else {
                                c5.addNewLineToCell("           System.out.println(learnerString + \\\",\\\" + streamString);", 5);
                            }
                            c5.addNewLineToCell("           /**uncomment this line and comment out the next line if you want printing out without the table**/", 5);
                            c5.addNewLineToCell("           //System.out.println(learningCurve.headerToString());", 5);
                            c5.addNewLineToCell("           System.out.print(table.headerLine(learningCurve.headerToString()));", 5);
                            c5.addNewLineToCell("           firstPrint = false;", 5);
                            c5.addNewLineToCell("        }", 5);
                            c5.addNewLineToCell("        /**uncomment this line and comment out the next line if you want printing out without the table**/", 5);
                            c5.addNewLineToCell("        //System.out.println(learningCurve.entryToString(learningCurve.numEntries() - 1));", 5);
                            c5.addNewLineToCell("        System.out.print(table.bodyLine(learningCurve.entryToString(learningCurve.numEntries() - 1)));", 5);
                            c5.addNewLineToCell("        System.out.flush();", 5);

                            c5.addNewLineToCell("    }", 5);
                            c5.addNewLineToCell("}", 5);
                            if (currentTask instanceof EvaluatePrequentialDelayed) c5.addNewLineToCell("}", 5);
                            c5.addNewLineToCell("double time = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread()- evaluateStartTime);", 5);
                            c5.addNewLineToCell("System.out.println(instancesProcessed + \\\" instances processed in \\\" + time +\\\" seconds.\\\");", 5);
                            c5.addNewLineToCell("if (immediateResultStream != null) {", 5);
                            c5.addNewLineToCell("    immediateResultStream.close();", 5);
                            c5.addNewLineToCell("}", 5);
                            //draw the chart
                            JSONCell md6 = new JSONCell("markdown");
                            md6.addNewLineToCell("## Drawing the chart", 5);
                            JSONCell c6 = new JSONCell();
                            cellNum++;
                            c6.addNewLineToCell(String.valueOf(cellNum), 2);
                            c6.addNewLineToCell("/**you can change the index columns of eStr[] and hStr[] arrays according to what to be displayed on the chart", 5);
                            c6.addNewLineToCell(" change the elements index of eStr[3] and eStr[4] for data according to hStr[3], hStr[4] for axis name", 5);
                            c6.addNewLineToCell(" **/", 5);
                            c6.addNewLineToCell("%maven org.knowm.xchart:xchart:3.5.2", 5);
                            c6.addNewLineToCell("import org.knowm.xchart.*;", 5);
                            c6.addNewLineToCell("double[] xData = new double[learningCurve.numEntries()];", 5);
                            c6.addNewLineToCell("double[] yData = new double[learningCurve.numEntries()];", 5);
                            c6.addNewLineToCell("String[] hStr = learningCurve.headerToString().split(\\\",\\\");", 5);
                            c6.addNewLineToCell("for (int i = 0; i < learningCurve.numEntries(); i++) {", 5);
                            c6.addNewLineToCell("    String[] eStr = learningCurve.entryToString(i).split(\\\",\\\");", 5);
                            c6.addNewLineToCell("    xData[i]= Double.valueOf(eStr[3]);", 5);
                            c6.addNewLineToCell("    yData[i]= Double.valueOf(eStr[4]);", 5);
                            c6.addNewLineToCell("}", 5);

                            c6.addNewLineToCell("XYChart chart = QuickChart.getChart(\\\"" + currentTask.getClass().getSimpleName() + "\\\", hStr[3], hStr[4], \\\"y(x)\\\", xData, yData);", 5);
                            c6.addNewLineToCell("BitmapEncoder.getBufferedImage(chart);", 5);
                            /**---------------------------------------------------------------
                             end content of Notebook **/
                            md3.createCell();
                            c3.createCell();
                            md5.createCell();
                            c5.createCell();
                            md6.createCell();
                            c6.createCell();
                            jsonCells.add(md3);
                            jsonCells.add(c3);
                            jsonCells.add(md5);
                            jsonCells.add(c5);
                            jsonCells.add(md6);
                            jsonCells.add(c6);
                        }
                        /**
                         * here is the short version of IPYNB file, that need to run with moa-flow
                         */
                        else {
                            /**-----------------Imports all necessary libraries----------------- **/
                            JSONCell md1 = new JSONCell("markdown");
                            md1.addNewLineToCell("This IPYNB file was generated automatically by MOA GUI.<br>", 5);
                            md1.addNewLineToCell("Task name: " + currentTask.getClass().getName() + "<br>", 5);
                            md1.addNewLineToCell("## Libraries importing", 5);
                            JSONCell c1 = new JSONCell();
                            c1.addNewLineToCell(String.valueOf(cellNum), 2);
                            c1.addNewLineToCell("%maven nz.ac.waikato.cms.moa:moa:2019.05.0", 5);
                            c1.addNewLineToCell("%classpath \\\"H:/MOA/moa-flow-core.jar\\\"", 5);
                            c1.addNewLineToCell("import moaflow.transformer.*;", 5);
                            c1.addNewLineToCell("import moaflow.core.Utils;", 5);
                            c1.addNewLineToCell("import moaflow.sink.*;", 5);
                            c1.addNewLineToCell("import moaflow.source.*;", 5);

                            /**-----------------Prints out the configuration of learner, stream and evaluator----------------- **/
                            JSONCell md2 = new JSONCell("markdown");
                            md2.addNewLineToCell("## Configuring learner, stream and evaluator", 5);
                            JSONCell c2 = new JSONCell();
                            cellNum++;
                            c2.addNewLineToCell(String.valueOf(cellNum), 2);
                            c2.addNewLineToCell("String learnerString = \\\"" + learnerString + "\\\";", 5);
                            c2.addNewLineToCell("String streamString = \\\"" + streamString + "\\\";", 5);
                            c2.addNewLineToCell("String evaluatorString = \\\"" + evaluatorString + "\\\";", 5);

                            /**-----------------Creates flow for testing, training and exporting result----------------- **/
                            JSONCell md3 = new JSONCell("markdown");
                            md3.addNewLineToCell("## Testing & training, exporting result", 5);
                            JSONCell c3 = new JSONCell();
                            cellNum++;
                            c3.addNewLineToCell(String.valueOf(cellNum), 2);
                            c3.addNewLineToCell("InstanceSource source;", 5);
                            c3.addNewLineToCell("source = new InstanceSource();", 5);
                            c3.addNewLineToCell("source.setGenerator(streamString);;", 5);
                            c3.addNewLineToCell("source.numInstances.setValue("+ instanceLimit +");", 5);
                            c3.addNewLineToCell("", 5);

                            if (currentTask instanceof EvaluateInterleavedTestThenTrain || currentTask instanceof EvaluatePrequential || currentTask instanceof EvaluatePrequentialCV
                            || currentTask instanceof EvaluatePrequentialDelayed || currentTask instanceof EvaluatePrequentialDelayedCV) {

                                c3.addNewLineToCell("EvaluateClassifier eval = new EvaluateClassifier();", 5);
                                c3.addNewLineToCell("eval.setClassifier(learnerString);", 5);
                                if (currentTask instanceof EvaluateInterleavedTestThenTrain)
                                    c3.addNewLineToCell("eval.setEvaluationScheme(\\\"Prequential\\\");", 5);
                                else
                                    c3.addNewLineToCell("eval.setEvaluationScheme(\\\""+currentTask.getClass().getName().substring(18,currentTask.getClass().getName().length())+"\\\");", 5);

                            } else if (currentTask instanceof EvaluatePrequentialRegression) {
                                c3.addNewLineToCell("EvaluateRegressor eval = new EvaluateRegressor();", 5);
                                c3.addNewLineToCell("eval.setRegressor(learnerString);", 5);
                            }
                            c3.addNewLineToCell("eval.setEvaluator(evaluatorString);", 5);
                            c3.addNewLineToCell("eval.everyNth.setValue("+sampleFrequency+");", 5);
                            c3.addNewLineToCell("source.subscribe(eval);", 5);
                            c3.addNewLineToCell("", 5);
                            c3.addNewLineToCell("MeasurementTableSawPlot plot = new MeasurementTableSawPlot();", 5);
                            if (currentTask instanceof EvaluateInterleavedTestThenTrain || currentTask instanceof EvaluatePrequential ||
                                    currentTask instanceof EvaluatePrequentialDelayed) {
                                c3.addNewLineToCell("plot.measurement.setValue(\\\"classifications correct (percent)\\\");", 5);
                            }
                            else if (currentTask instanceof EvaluatePrequentialCV || currentTask instanceof EvaluatePrequentialDelayedCV) {
                                c3.addNewLineToCell("plot.measurement.setValue(\\\"[avg] classifications correct (percent)\\\");", 5);
                            }
                            else if (currentTask instanceof EvaluatePrequentialRegression) {
                                c3.addNewLineToCell("plot.measurement.setValue(\\\"mean absolute error\\\");", 5);
                            }
                            c3.addNewLineToCell("plot.maxPoints.setValue(-1);", 5);
                            c3.addNewLineToCell("eval.subscribe(plot);", 5);
                            c3.addNewLineToCell("", 5);
                            c3.addNewLineToCell("OutputLearningCurve curve = new OutputLearningCurve();", 5);
                            c3.addNewLineToCell("eval.subscribe(curve);", 5);
                            c3.addNewLineToCell("", 5);
                            c3.addNewLineToCell("System.out.println(Utils.toTree(source));", 5);
                            c3.addNewLineToCell("", 5);
                            c3.addNewLineToCell("source.start();", 5);
                            md1.createCell();
                            c1.createCell();
                            md2.createCell();
                            c2.createCell();
                            md3.createCell();
                            c3.createCell();
                            jsonCells.add(md1);
                            jsonCells.add(c1);
                            jsonCells.add(md2);
                            jsonCells.add(c2);
                            jsonCells.add(md3);
                            jsonCells.add(c3);
                        }

                        this.createAllCells(jsonCells);

                        w.write(cells);
                        w.close();
                    } catch (Exception ex) {
                        throw new RuntimeException(
                                "Failed writing to file " + destFile, ex);
                    }
                } else {
                    throw new IllegalArgumentException("No destination file to write to.");
                }
            }

            if (this.runConfig.isSet())
                result = this.task.doTask(monitor, repository);
            else if (((MainTask) this.task).outputFileOption != null)
                result = "If you want to save the task result, please tick the runConfig check box first.";
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed implementing task ", ex);
        }

        return result;
    }

    @Override
    public Class<?> getTaskResultType() {
        return String.class;
    }
}