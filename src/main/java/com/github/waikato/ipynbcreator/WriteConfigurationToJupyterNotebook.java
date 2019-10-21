package moa.tasks;

import java.io.*;
import java.util.ArrayList;

import moa.tasks.JSON.*;

import com.github.javacliparser.FlagOption;
import moa.core.ObjectRepository;
import moa.evaluation.preview.LearningCurve;
import moa.options.ClassOption;
import com.github.javacliparser.FileOption;
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
     * Initialises the first state of flags
     */
    public WriteConfigurationToJupyterNotebook() {
        this.runConfig.unset();
        this.exportAdvancedNotebook.unset();
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
                        Notebook nb = new Notebook();
                        if (this.exportAdvancedNotebook.isSet()) {
                            /**---------------------------------------------------------------
                             * beginning of the content of the IPYNB file**/

                            //import all necessary library files
                            nb.addMarkdown()
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("This IPYNB file was generated automatically by MOA GUI.<br>")
                                    .addNewLine("Task name: " + currentTask.getClass().getName() + "<br>")
                                    .addNewLine("## Libraries importing");
                            nb.addCode()
                                    .setWorkingMode(NotebookCell.Mod.EXECUTION_COUNT)
                                    .addNewLine(String.valueOf(cellNum))
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("%maven nz.ac.waikato.cms.moa:moa:2019.05.0")
                                    .addNewLine("%classpath ")
                                    .addQuoted("H:/MOA/moa-flow-core.jar")
                                    .addNewLine("import moa.classifiers." + learnerOptionsString.getClassFullName() + ";")
                                    .addNewLine("import moa.streams." + streamOptionsString.getClassFullName() + ";")
                                    .addNewLine("import moa.evaluation." + evaluatorOptionsString.getClassFullName() + ";")
                                    .addNewLine("import moa.evaluation.LearningEvaluation;")
                                    .addNewLine("import moa.evaluation.preview.LearningCurve;")
                                    .addNewLine("import moa.core.TimingUtils;")
                                    .addNewLine("import moa.tasks.MainTask;")
                                    .addNewLine("import com.yahoo.labs.samoa.instances.Instance;")
                                    .addNewLine("import moa.core.Example;")
                                    .addNewLine("import moa.core.Measurement;");
                            /**-----------------Create second cell for initializing the learner, the stream and the evaluator----------------- **/
                            nb.addMarkdown()
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("## Configuring learner, stream and evaluator");

                            cellNum++;
                            nb.addCode()
                                    .setWorkingMode(NotebookCell.Mod.EXECUTION_COUNT)
                                    .addNewLine(String.valueOf(cellNum))
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("String learnerString = ")
                                    .addQuoted(learnerString)
                                    .add(";");

                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV)) {
                                nb.getCellByIndex(1).addNewLine("import java.util.Random;");
                                nb.getLastCell().addNewLine("int randomSeed =" + randomSeed + ";")
                                        .addNewLine("Random random = new Random(randomSeed);")
                                        /**-----------------create learner----------------- **/
                                        .addNewLine("int numFolds = " + numFolds + ";")
                                        .addNewLine(learnerOptionsString.getClassShortName() + "[] " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array = new " + learnerOptionsString.getClassShortName() + "[numFolds];")
                                        .addNewLine(learnerOptionsString.generateOptionsString())
                                        .addNewLine(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();")
                                        .addNewLine(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".resetLearning();")
                                        //Learner[] learners = new Learner[numFoldsOption];
                                        //Learner baseLearner = (Learner) getPreparedClassOption(this.learnerOption);
                                        //baseLearner.resetLearning();
                                        /**--------------create stream------------**/
                                        .addNewLine("")
                                        .addNewLine("String streamString = ")
                                        .addQuoted(streamString)
                                        .add(";")
                                        .addNewLine(streamOptionsString.generateOptionsString())
                                        .addNewLine(streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();")
                                        .addNewLine(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".setModelContext("
                                                + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getHeader());")
                                        /**--------------create evaluator-----------**/
                                        .addNewLine("")
                                        .addNewLine("String evaluatorString = ")
                                        .addQuoted(evaluatorString)
                                        .add(";")
                                        .addNewLine(evaluatorOptionsString.getClassShortName() + "[] " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array = new " + evaluatorOptionsString.getClassShortName() + "[numFolds];")
                                        .addNewLine(evaluatorOptionsString.generateOptionsString());
                                nb.getLastCell().addNewLine("for (int i = 0; i <" + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length; i++) {")
                                        .addNewLine("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i] = (" + learnerOptionsString.getClassShortName() + ") " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".copy();")
                                        .addNewLine("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].setModelContext(" + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getHeader());")
                                        .addNewLine("    " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "Array[i] = (" +
                                                evaluatorOptionsString.getClassShortName() + ") " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".copy();")
                                        .addNewLine("}");
                            } else {
                                /**-----------------create learner----------------- **/
                                nb.getLastCell().addNewLine(learnerOptionsString.generateOptionsString())
                                        .addNewLine(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();");
                                if (currentTask instanceof EvaluateInterleavedTestThenTrain) {
                                    nb.getLastCell().addNewLine("int randomSeed = " + randomSeed + ";")
                                            .addNewLine("if (" + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".isRandomizable()) {")
                                            .addNewLine("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".setRandomSeed(randomSeed);")
                                            .addNewLine("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".resetLearning();")
                                            .addNewLine("}");
                                }
                                /**--------------create stream------------**/
                                nb.getLastCell().addNewLine("")
                                        .addNewLine("String streamString = ")
                                        .addQuoted(streamString)
                                        .add(";")
                                        .addNewLine(streamOptionsString.generateOptionsString())
                                        .addNewLine(streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();")
                                        .addNewLine(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".setModelContext("
                                                + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getHeader());")
                                        /**--------------create evaluator-----------**/
                                        .addNewLine("")
                                        .addNewLine("String evaluatorString = ")
                                        .addQuoted(evaluatorString)
                                        .add(";")
                                        .addNewLine(evaluatorOptionsString.generateOptionsString());
                            }


                            /**--------------set environment parameters----------- **/
                            nb.addMarkdown()
                                    .addNewLine("## Setting environmental parameters");
                            cellNum++;
                            nb.addCode()
                                    .setWorkingMode(NotebookCell.Mod.EXECUTION_COUNT)
                                    .addNewLine(String.valueOf(cellNum))
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("int maxInstances = " + instanceLimit + ";")
                                    .addNewLine("long instancesProcessed = 0;")
                                    .addNewLine("int maxSeconds = " + timeLimit + ";")
                                    .addNewLine("int secondsElapsed = 0;")
                                    .addNewLine("LearningCurve learningCurve = new LearningCurve(")
                                    .addQuoted("learning evaluation instances")
                                    .add(");")
                                    .addNewLine("File dumpFile = new File(")
                                    .addQuoted(dumpFilePath)
                                    .add(");")
                                    .addNewLine("PrintStream immediateResultStream = null;")
                                    .addNewLine("if (dumpFile != null) {")
                                    .addNewLine("    try {")
                                    .addNewLine("        if (dumpFile.exists()) {")
                                    .addNewLine("            immediateResultStream = new PrintStream(")
                                    .addNewLine("                    new FileOutputStream(dumpFile, true), true);")
                                    .addNewLine("        } else {")
                                    .addNewLine("            immediateResultStream = new PrintStream(")
                                    .addNewLine("                    new FileOutputStream(dumpFile), true);")
                                    .addNewLine("        }")
                                    .addNewLine("    } catch (Exception ex) {")
                                    .addNewLine("        throw new RuntimeException(")
                                    .addNewLine("                ")
                                    .addQuoted("Unable to open immediate result file: ")
                                    .add(" + dumpFile, ex);")
                                    .addNewLine("    }")
                                    .addNewLine("}");

                            if ((currentTask instanceof EvaluatePrequential) || (currentTask instanceof EvaluatePrequentialDelayed) ||
                                    (currentTask instanceof EvaluatePrequentialRegression)) {
                                //File for output predictions
                                if (outputPredictionFile != null) {
                                    nb.getLastCell().addNewLine("File outputPredictionFile = new File()")
                                            .addQuoted(outputPredictionFile.getAbsolutePath().replace('\\', '/'))
                                            .add(");");
                                } else nb.getLastCell().addNewLine("File outputPredictionFile = null;");

                                nb.getLastCell().addNewLine("PrintStream outputPredictionResultStream = null;")
                                        .addNewLine("if (outputPredictionFile != null) {")
                                        .addNewLine("    try {")
                                        .addNewLine("        if (outputPredictionFile.exists()) {")
                                        .addNewLine("            outputPredictionResultStream = new PrintStream(")
                                        .addNewLine("                    new FileOutputStream(outputPredictionFile, true), true);")
                                        .addNewLine("        } else {")
                                        .addNewLine("            outputPredictionResultStream = new PrintStream(")
                                        .addNewLine("                    new FileOutputStream(outputPredictionFile), true);")
                                        .addNewLine("        }")
                                        .addNewLine("    } catch (Exception ex) {")
                                        .addNewLine("        throw new RuntimeException(")
                                        .addNewLine("                 ")
                                        .addQuoted("Unable to open prediction result file: ")
                                        .add(" + outputPredictionFile, ex);")
                                        .addNewLine("    }")
                                        .addNewLine("}");
                                if (currentTask instanceof EvaluatePrequentialDelayed) {
                                    nb.getCellByIndex(1).addNewLine("import java.util.LinkedList;");
                                    nb.getLastCell().addNewLine("LinkedList<Example> trainInstances = new LinkedList<Example>();");
                                }
                            } else if (currentTask instanceof EvaluatePrequentialDelayedCV) {
                                nb.getCellByIndex(1).addNewLine("import java.util.LinkedList;");
                                nb.getLastCell().addNewLine("LinkedList<LinkedList<Example>> trainInstances = new LinkedList<LinkedList<Example>>();")

                                        .addNewLine("for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length; i++) {")
                                        .addNewLine("   trainInstances.add(new LinkedList<Example>());")
                                        .addNewLine("}");
                            }
                            nb.getLastCell().addNewLine("boolean firstDump = true;")
                                    .addNewLine("boolean firstPrint = true;")
                                    .addNewLine("boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();")
                                    .addNewLine("long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();")
                                    .addNewLine("long lastEvaluateStartTime = evaluateStartTime;")
                                    .addNewLine("double RAMHours = 0.0;");
                            // begin the loop
                            nb.addMarkdown()
                                    .addNewLine("## Testing & training, exporting result");

                            cellNum++;
                            nb.addCode()
                                    .setWorkingMode(NotebookCell.Mod.EXECUTION_COUNT)
                                    .addNewLine(String.valueOf(cellNum))
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("DrawTable table = new DrawTable();");
                            nb.getCellByIndex(1).addNewLine("import moaflow.sink.DrawTable;");
                            nb.getLastCell().addNewLine("System.out.println(")
                                    .addQuoted("Evaluating learner...")
                                    .add(");")
                                    .addNewLine("while (" + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".hasMoreInstances()")
                                    .addNewLine("        && ((maxInstances < 0) || (instancesProcessed < maxInstances))")
                                    .addNewLine("        && ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {");

                            if (currentTask instanceof EvaluatePrequentialCV) {
                                nb.getCellByIndex(1).addNewLine("import moa.core.MiscUtils;");
                                nb.getLastCell().addNewLine("    Example trainInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".nextInstance();")
                                        .addNewLine("    Example testInst = trainInst; //.copy();")
                                        //testInst.setClassMissing();

                                        .addNewLine("    for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length; i++) {")
                                        .addNewLine("       " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].addResult(testInst, " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].getVotesForInstance(testInst));")
                                        .addNewLine("    }")

                                        .addNewLine("    for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length; i++) {")
                                        .addNewLine("        int k = 1;")
                                        .addNewLine("        int validationMethodology = " + validationMethodology + ";")
                                        .addNewLine("        switch (validationMethodology) {")
                                        .addNewLine("            case 0: //Cross-Validation;")
                                        .addNewLine("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length == i ? 0: 1; //Test all except one")
                                        .addNewLine("                break;")
                                        .addNewLine("            case 1: //Bootstrap;")
                                        .addNewLine("                k = MiscUtils.poisson(1, random);")
                                        .addNewLine("                break;")
                                        .addNewLine("            case 2: //Split-Validation;")
                                        .addNewLine("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length == i ? 1: 0; //Test only one")
                                        .addNewLine("                break;")
                                        .addNewLine("        }")
                                        .addNewLine("        if (k > 0) {")
                                        .addNewLine("            Example weightedInst = (Example) trainInst.copy();")
                                        .addNewLine("            weightedInst.setWeight(trainInst.weight() * k);")
                                        .addNewLine("            " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].trainOnInstance(weightedInst);")
                                        .addNewLine("        }")
                                        .addNewLine("    }")
                                        .addNewLine("    instancesProcessed++;");
                            } else if (currentTask instanceof EvaluatePrequentialDelayed) {
                                nb.getCellByIndex(1).addNewLine("import moa.core.Utils;")
                                        .addNewLine("import moa.core.InstanceExample;");
                                nb.getLastCell().addNewLine("    instancesProcessed++;")
                                        .addNewLine("    Example currentInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".nextInstance();")
                                        .addNewLine("    boolean trainOnInitialWindow = " + trainOnInitialWindow + ";")
                                        .addNewLine("    boolean trainInBatches = " + trainInBatches + ";")
                                        .addNewLine("    int initialWindowSize = " + initialWindowSize + ";")
                                        .addNewLine("    int delayLength = " + delayLength + ";")
                                        .addNewLine("    if (instancesProcessed <= initialWindowSize) {")
                                        .addNewLine("        if (trainOnInitialWindow) {")
                                        .addNewLine("            " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".trainOnInstance(currentInst);")
                                        .addNewLine("        } else if ((initialWindowSize - instancesProcessed) < delayLength) {")
                                        .addNewLine("            trainInstances.addLast(currentInst);")
                                        .addNewLine("        }")
                                        .addNewLine("    } else {")
                                        .addNewLine("        trainInstances.addLast(currentInst);")

                                        .addNewLine("        if (delayLength < trainInstances.size()) {")
                                        .addNewLine("            if (trainInBatches) {")
                                        .addNewLine("                // Do not train on the latest instance, otherwise")
                                        .addNewLine("                // it would train on k+1 instances")
                                        .addNewLine("                while (trainInstances.size() > 1) {")
                                        .addNewLine("                    Example trainInst = trainInstances.removeFirst();")
                                        .addNewLine("                    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".trainOnInstance(trainInst);")
                                        .addNewLine("                }")
                                        .addNewLine("            } else {")
                                        .addNewLine("                Example trainInst = trainInstances.removeFirst();")
                                        .addNewLine("                " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".trainOnInstance(trainInst);")
                                        .addNewLine("            }")
                                        .addNewLine("        }")

                                        .addNewLine("        // Remove class label from test instances.")
                                        .addNewLine("        Instance testInstance = ((Instance) currentInst.getData()).copy();")
                                        .addNewLine("        testInstance.setMissing(testInstance.classAttribute());")
                                        .addNewLine("        testInstance.setClassValue(0.0);")

                                        .addNewLine("        Example testInst = new InstanceExample(testInstance);")
                                        .addNewLine("        double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".getVotesForInstance(testInst);")
                                        //          reinstate the testInstance as it is used in evaluator.addResult
                                        .addNewLine("        testInstance = ((Instance) currentInst.getData()).copy();")
                                        .addNewLine("        testInst = new InstanceExample(testInstance);")

                                        .addNewLine("        // Output prediction")
                                        .addNewLine("        if (outputPredictionFile != null) {")
                                        .addNewLine("            int trueClass = (int) ((Instance) currentInst.getData()).classValue();")
                                        .addNewLine("            outputPredictionResultStream.println(Utils.maxIndex(prediction) + ")
                                        .addQuoted(",")
                                        .add(" + (")
                                        .addNewLine("                    ((Instance) testInst.getData()).classIsMissing() == true ? ")
                                        .addQuoted(" ? ")
                                        .add(" : trueClass));")
                                        .addNewLine("        }")
                                        .addNewLine("        " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".addResult(testInst, prediction);");
                            } else if (currentTask instanceof EvaluatePrequentialDelayedCV) {
                                nb.getCellByIndex(1).addNewLine("import moa.core.MiscUtils;");
                                nb.getLastCell().addNewLine("    instancesProcessed++;")
                                        .addNewLine("    Example trainInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".nextInstance();")
                                        .addNewLine("    Example testInst = trainInst; //.copy();")
                                        .addNewLine("    for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length; i++) {")

                                        .addNewLine("        double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].getVotesForInstance(testInst);")
                                        .addNewLine("        " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].addResult(testInst, prediction);")

                                        .addNewLine("        int k = 1;")
                                        .addNewLine("        int validationMethodology = " + validationMethodology + ";")
                                        .addNewLine("        switch (validationMethodology) {")
                                        .addNewLine("            case 0: //Cross-Validation;")
                                        .addNewLine("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length == i ? 0: 1; //Test all except one")
                                        .addNewLine("                break;")
                                        .addNewLine("            case 1: //Bootstrap;")
                                        .addNewLine("                k = MiscUtils.poisson(1, random);")
                                        .addNewLine("                break;")
                                        .addNewLine("            case 2: //Split-Validation;")
                                        .addNewLine("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length == i ? 1: 0; //Test only one")
                                        .addNewLine("                break;")
                                        .addNewLine("        }")
                                        .addNewLine("        if (k > 0) {")
                                        .addNewLine("            trainInstances.get(i).addLast(trainInst);")
                                        .addNewLine("        }")
                                        .addNewLine("        int delayLength = " + delayLength + ";")
                                        .addNewLine("        if(delayLength < trainInstances.get(i).size()) {")
                                        .addNewLine("        Example trainInstI = trainInstances.get(i).removeFirst();")
                                        .addNewLine("        " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].trainOnInstance(trainInstI);")
                                        .addNewLine("        }")
                                        .addNewLine("    }");
                            } else {
                                nb.getLastCell().addNewLine("    Example trainInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".nextInstance();")
                                        .addNewLine("    Example testInst = trainInst; //.copy();");
                                if (currentTask instanceof EvaluatePrequentialRegression) {
                                    nb.getCellByIndex(1).addNewLine("import com.yahoo.labs.samoa.instances.Prediction;");
                                    nb.getLastCell().addNewLine("    Prediction prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                            + ".getPredictionForInstance(testInst);")
                                            .addNewLine("    if (outputPredictionFile != null) {")
                                            .addNewLine("       double trueClass = ((Instance) trainInst.getData()).classValue();")
                                            .addNewLine("       outputPredictionResultStream.println(prediction + ")
                                            .addQuoted(",")
                                            .add(" + trueClass);")
                                            .addNewLine("    }");
                                } else if (currentTask instanceof EvaluatePrequential) {
                                    nb.getCellByIndex(1).addNewLine("import moa.core.Utils;");
                                    nb.getLastCell().addNewLine("    double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getVotesForInstance(testInst);")
                                            .addNewLine("    // Output prediction")
                                            .addNewLine("    if (outputPredictionFile != null) {")
                                            .addNewLine("       int trueClass = (int) ((Instance) trainInst.getData()).classValue();")
                                            .addNewLine("       outputPredictionResultStream.println(Utils.maxIndex(prediction) + ")
                                            .addQuoted(",")
                                            .add(" + (")
                                            .addNewLine("       ((Instance) testInst.getData()).classIsMissing() == true ? ")
                                            .addQuoted(" ? ")
                                            .add(" : trueClass));")
                                            .addNewLine("    }");
                                } else
                                    nb.getLastCell().addNewLine("    double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getVotesForInstance(testInst);");

                                nb.getLastCell().addNewLine("    " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".addResult(testInst, prediction);")
                                        .addNewLine("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".trainOnInstance(trainInst);")
                                        .addNewLine("    instancesProcessed++;");
                            }
                            nb.getLastCell().addNewLine("    if (instancesProcessed % " + sampleFrequency + " == 0")
                                    .addNewLine("            ||  " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".hasMoreInstances() == false) {")
                                    .addNewLine("        long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();")
                                    .addNewLine("        double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);")
                                    .addNewLine("        double timeIncrement = TimingUtils.nanoTimeToSeconds(evaluateTime - lastEvaluateStartTime);");
                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV)) {
                                nb.getLastCell().addNewLine("        for (int i = 0; i <" + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length; i++) {")
                                        .addNewLine("        double RAMHoursIncrement = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs")
                                        .addNewLine("        RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours")
                                        .addNewLine("        RAMHours += RAMHoursIncrement;")
                                        .addNewLine("        }");
                            } else {
                                nb.getLastCell().addNewLine("        double RAMHoursIncrement = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + ".measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs")
                                        .addNewLine("        RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours")
                                        .addNewLine("        RAMHours += RAMHoursIncrement;");
                            }
                            nb.getLastCell().addNewLine("        lastEvaluateStartTime = evaluateTime;");

                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV)) {
                                nb.getLastCell().addNewLine("        Measurement[] modelMeasurements = new Measurement[]{")
                                        .addNewLine("                                           new Measurement(")
                                        .addNewLine("                                           ")
                                        .addQuoted("learning evaluation instances")
                                        .add(",")
                                        .addNewLine("                                           instancesProcessed),")
                                        .addNewLine("                                           new Measurement(")
                                        .addNewLine("                                           ")
                                        .addQuoted("evaluation time (")
                                        .addNewLine("                                           + (preciseCPUTiming ? ")
                                        .addQuoted("cpu ")
                                        .addNewLine("                                           : ")
                                        .addQuoted("")
                                        .add(") + ")
                                        .addQuoted("seconds)")
                                        .add(",")
                                        .addNewLine("                                           time),")
                                        .addNewLine("                                           new Measurement(")
                                        .addNewLine("                                           ")
                                        .addQuoted("model cost (RAM-Hours)")
                                        .add(",")
                                        .addNewLine("                                           RAMHours)")
                                        .addNewLine("                                        };")
                                        .addNewLine("        List<Measurement> measurementList = new LinkedList<>();")
                                        .addNewLine("        if (modelMeasurements != null) {")
                                        .addNewLine("            measurementList.addAll(Arrays.asList(modelMeasurements));")
                                        .addNewLine("        }")
                                        .addNewLine("        // add average of sub-model measurements")
                                        .addNewLine("        if ((" + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() +
                                                "Array != null) && (" + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "Array.length > 0)) {")
                                        .addNewLine("            List<Measurement[]> subMeasurements = new LinkedList<>();")
                                        .addNewLine("            for (" + evaluatorOptionsString.getClassShortName() + " subEvaluator : "
                                                + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "Array) {")
                                        .addNewLine("                if (subEvaluator != null) {")
                                        .addNewLine("                   subMeasurements.add(subEvaluator.getPerformanceMeasurements());")
                                        .addNewLine("                }")
                                        .addNewLine("            }")
                                        .addNewLine("            Measurement[] avgMeasurements = Measurement.averageMeasurements(" +
                                                "subMeasurements.toArray(new Measurement[subMeasurements.size()][]));")
                                        .addNewLine("            measurementList.addAll(Arrays.asList(avgMeasurements));")
                                        .addNewLine("        }")
                                        .addNewLine("           learningCurve.insertEntry(new LearningEvaluation(measurementList.toArray(new Measurement[measurementList.size()])));");
                            } else {
                                nb.getLastCell().addNewLine("        learningCurve.insertEntry(new LearningEvaluation(")
                                        .addNewLine("                new Measurement[]{")
                                        .addNewLine("                        new Measurement(")
                                        .addNewLine("                                ")
                                        .addQuoted("learning evaluation instances")
                                        .add(",")
                                        .addNewLine("                                instancesProcessed),")
                                        .addNewLine("                        new Measurement(")
                                        .addNewLine("                                ")
                                        .addQuoted("evaluation time (")
                                        .addNewLine("                                        + (preciseCPUTiming ? ")
                                        .addQuoted("cpu ")
                                        .addNewLine("                                        : ")
                                        .addQuoted("")
                                        .add(") + ")
                                        .addQuoted("seconds)")
                                        .add(",")
                                        .addNewLine("                                time),")
                                        .addNewLine("                        new Measurement(")
                                        .addNewLine("                                ")
                                        .addQuoted("model cost (RAM-Hours)")
                                        .add(",")
                                        .addNewLine("                                RAMHours)")
                                        .addNewLine("                },")
                                        .addNewLine("                " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ", "
                                                + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "));");
                            }

                            nb.getLastCell().addNewLine("        if (immediateResultStream != null) {")
                                    .addNewLine("            if (firstDump) {")
                                    .addNewLine("                immediateResultStream.println(learningCurve.headerToString());")
                                    .addNewLine("                firstDump = false;")
                                    .addNewLine("            }");
                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV) ||
                                    (currentTask instanceof EvaluateInterleavedTestThenTrain)) {
                                nb.getLastCell().addNewLine("            immediateResultStream.print(learnerString + ")
                                        .addQuoted(",")
                                        .add(" + streamString + ")
                                        .addQuoted(",")
                                        .add("+ Integer.toString(randomSeed) +")
                                        .addQuoted(",")
                                        .add(");");
                            } else {
                                nb.getLastCell().addNewLine("            immediateResultStream.print(learnerString + ")
                                        .addQuoted(",")
                                        .add(" + streamString);");
                            }

                            nb.getLastCell().addNewLine("            immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));")
                                    .addNewLine("            immediateResultStream.flush();")
                                    .addNewLine("        }")
                                    .addNewLine("        if (firstPrint) {");
                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV) ||
                                    (currentTask instanceof EvaluateInterleavedTestThenTrain)) {
                                nb.getLastCell().addNewLine("           System.out.println(learnerString + ")
                                        .addQuoted(",")
                                        .add(" + streamString + ")
                                        .addQuoted(", randomSeed = ")
                                        .add("+ Integer.toString(randomSeed) +")
                                        .addQuoted(".")
                                        .add(");");
                            } else {
                                nb.getLastCell().addNewLine("           System.out.println(learnerString + ")
                                        .addQuoted(",")
                                        .add(" + streamString);");
                            }
                            nb.getLastCell().addNewLine("           System.out.print(table.headerLine(learningCurve.headerToString()));")
                                    .addNewLine("           firstPrint = false;")
                                    .addNewLine("        }")
                                    .addNewLine("        System.out.print(table.bodyLine(learningCurve.entryToString(learningCurve.numEntries() - 1)));")
                                    .addNewLine("        System.out.flush();")

                                    .addNewLine("    }")
                                    .addNewLine("}");
                            if (currentTask instanceof EvaluatePrequentialDelayed) nb.getLastCell().addNewLine("}");
                            nb.getLastCell().addNewLine("double time = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread()- evaluateStartTime);")
                                    .addNewLine("System.out.println(instancesProcessed + ")
                                    .addQuoted(" instances processed in ")
                                    .add(" + time + ")
                                    .addQuoted(" seconds.")
                                    .add(");")
                                    .addNewLine("if (immediateResultStream != null) {")
                                    .addNewLine("    immediateResultStream.close();")
                                    .addNewLine("}");
                            /**---------------------------------------------------------------
                             end content of Notebook **/
                        }
                        /**
                         * here is the short version of IPYNB file, that need to run with moa-flow
                         */
                        else {
                            /**-----------------Imports all necessary libraries----------------- **/
                            nb.addMarkdown()
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("This IPYNB file was generated automatically by MOA GUI.<br>")
                                    .addNewLine("Task name: " + currentTask.getClass().getName() + "<br>")
                                    .addNewLine("## Libraries importing");
                            nb.addCode()
                                    .setWorkingMode(NotebookCell.Mod.EXECUTION_COUNT)
                                    .addNewLine(String.valueOf(cellNum))
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("%maven nz.ac.waikato.cms.moa:moa:2019.05.0")
                                    .addNewLine("%classpath ")
                                    .addQuoted("H:/MOA/moa-flow-core.jar")
                                    .addNewLine("import moaflow.transformer.*;")
                                    .addNewLine("import moaflow.core.Utils;")
                                    .addNewLine("import moaflow.sink.*;")
                                    .addNewLine("import moaflow.source.*;");

                            /**-----------------Prints out the configuration of learner, stream and evaluator----------------- **/
                            nb.addMarkdown()
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("## Configuring learner, stream and evaluator");
                            cellNum++;
                            nb.addCode()
                                    .setWorkingMode(NotebookCell.Mod.EXECUTION_COUNT)
                                    .addNewLine(String.valueOf(cellNum))
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("String learnerString = ")
                                    .addQuoted(learnerString)
                                    .add(";")
                                    .addNewLine("String streamString = ")
                                    .addQuoted(streamString)
                                    .add(";")
                                    .addNewLine("String evaluatorString = ")
                                    .addQuoted(evaluatorString)
                                    .add(";");

                            /**-----------------Creates flow for testing, training and exporting result----------------- **/
                            nb.addMarkdown()
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("## Testing & training, exporting result");
                            cellNum++;
                            nb.addCode()
                                    .setWorkingMode(NotebookCell.Mod.EXECUTION_COUNT)
                                    .addNewLine(String.valueOf(cellNum))
                                    .setWorkingMode(NotebookCell.Mod.SOURCE)
                                    .addNewLine("InstanceSource source;")
                                    .addNewLine("source = new InstanceSource();")
                                    .addNewLine("source.setGenerator(streamString);;")
                                    .addNewLine("source.numInstances.setValue(" + instanceLimit + ");")
                                    .addNewLine("");

                            if (currentTask instanceof EvaluateInterleavedTestThenTrain || currentTask instanceof EvaluatePrequential || currentTask instanceof EvaluatePrequentialCV
                                    || currentTask instanceof EvaluatePrequentialDelayed || currentTask instanceof EvaluatePrequentialDelayedCV) {

                                nb.getLastCell().addNewLine("EvaluateClassifier eval = new EvaluateClassifier();")
                                        .addNewLine("eval.setClassifier(learnerString);");
                                if (currentTask instanceof EvaluateInterleavedTestThenTrain)
                                    nb.getLastCell().addNewLine("eval.setEvaluationScheme(")
                                            .addQuoted("Prequential")
                                            .add(");");
                                else
                                    nb.getLastCell().addNewLine("eval.setEvaluationScheme(")
                                            .addQuoted(currentTask.getClass().getName().substring(18, currentTask.getClass().getName().length()))
                                            .add(");");

                            } else if (currentTask instanceof EvaluatePrequentialRegression) {
                                nb.getLastCell().addNewLine("EvaluateRegressor eval = new EvaluateRegressor();")
                                        .addNewLine("eval.setRegressor(learnerString);");
                            }
                            nb.getLastCell().addNewLine("eval.setEvaluator(evaluatorString);")
                                    .addNewLine("eval.everyNth.setValue(" + sampleFrequency + ");")
                                    .addNewLine("source.subscribe(eval);")
                                    .addNewLine("")
                                    .addNewLine("MeasurementTableSawPlot plot = new MeasurementTableSawPlot();");
                            if (currentTask instanceof EvaluateInterleavedTestThenTrain || currentTask instanceof EvaluatePrequential ||
                                    currentTask instanceof EvaluatePrequentialDelayed) {
                                nb.getLastCell().addNewLine("plot.measurement.setValue(")
                                        .addQuoted("classifications correct (percent)")
                                        .add(");");
                            } else if (currentTask instanceof EvaluatePrequentialCV || currentTask instanceof EvaluatePrequentialDelayedCV) {
                                nb.getLastCell().addNewLine("plot.measurement.setValue(")
                                        .addQuoted("[avg] classifications correct (percent)")
                                        .add(");");
                            } else if (currentTask instanceof EvaluatePrequentialRegression) {
                                nb.getLastCell().addNewLine("plot.measurement.setValue(")
                                        .addQuoted("mean absolute error")
                                        .add(");");
                            }
                            nb.getLastCell().addNewLine("plot.maxPoints.setValue(-1);")
                                    .addNewLine("eval.subscribe(plot);")
                                    .addNewLine("")
                                    .addNewLine("OutputLearningCurve curve = new OutputLearningCurve();")
                                    .addNewLine("eval.subscribe(curve);")
                                    .addNewLine("")
                                    .addNewLine("System.out.println(Utils.toTree(source));")
                                    .addNewLine("")
                                    .addNewLine("source.start();");
                        }

                        w.write(nb.createNotebook().toString());
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