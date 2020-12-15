package moa.tasks;

import java.io.*;

import moa.tasks.ipynb.*;

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
    /**
     * Gets the purpose of this object
     *
     * @return the string with the purpose of this object
     */
    @Override
    public String getPurposeString() {
        return "Outputs a task to a Jupyter NotebookBuilder.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption taskOption = new ClassOption("task", 't',
            "Task to do.", Task.class, "EvaluateInterleavedTestThenTrain -l trees.HoeffdingTree -s (ArffFileStream -f H:/MLProject/Dataset/elecNormNew.arff) -i 1000000 -f 10000");

    public FileOption notebookOutputFile = new FileOption("NotebookFile", 'j',
            "Destination Jupyter NotebookBuilder file.", null, "ipynb", true);

    public FlagOption runConfig = new FlagOption("runConfig", 'r', "If checked, run the configuration beside exporting the it to NotebookBuilder file.");

    public FlagOption exportAdvancedNotebook = new FlagOption("exportAdvancedNotebook", 'e', "Check this if you want to export the code advanced notebook");

    protected Task task;

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

                        /* Gets out the values of all parameters necessary for creating detailed version of IPYNB file **/
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

                        OptionsString learnerOptionsString = new OptionsString(learnerString);
                        learnerOptionsString.createOptionsList();
                        OptionsString streamOptionsString = new OptionsString(streamString);
                        streamOptionsString.createOptionsList();
                        OptionsString evaluatorOptionsString = new OptionsString(evaluatorString);
                        evaluatorOptionsString.createOptionsList();

                        String dumpFilePath = null;
                        if (dumpFile != null) {
                            dumpFilePath = dumpFile.getAbsolutePath().replace('\\', '/');
                        }

                        //create array of JSON cells
                        NotebookBuilder nb = new NotebookBuilder();
                        if (this.exportAdvancedNotebook.isSet()) {
                            /*---------------------------------------------------------------
                             * beginning of the content of the IPYNB file**/

                            //import all necessary library files
                            nb.addMarkdown()
                                    .addSource("This IPYNB file was generated automatically by MOA GUI.<br>")
                                    .addSource("Task name: " + currentTask.getClass().getName() + "<br>")
                                    .addSource("## Libraries importing");
                            nb.addCode()
                                    .addSource("%maven nz.ac.waikato.cms.moa:moa:2019.05.0")
                                    .addSource("%classpath \"H:/MOA/moa-flow-core.jar\"")
                                    .addSource("import moa.classifiers." + learnerOptionsString.getClassFullName() + ";")
                                    .addSource("import moa.streams." + streamOptionsString.getClassFullName() + ";")
                                    .addSource("import moa.evaluation." + evaluatorOptionsString.getClassFullName() + ";")
                                    .addSource("import moa.evaluation.LearningEvaluation;")
                                    .addSource("import moa.evaluation.preview.LearningCurve;")
                                    .addSource("import moa.core.TimingUtils;")
                                    .addSource("import moa.tasks.MainTask;")
                                    .addSource("import com.yahoo.labs.samoa.instances.Instance;")
                                    .addSource("import moa.core.Example;")
                                    .addSource("import moa.core.Measurement;");
                            /*-----------------Create second cell for initializing the learner, the stream and the evaluator----------------- **/
                            nb.addMarkdown()
                                    .addSource("## Configuring learner, stream and evaluator");

                            nb.addCode()
                                    .addSource("String learnerString = \""+ learnerString +"\";");

                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV)) {
                                nb.getCellByIndex(1).addSource("import java.util.Random;");
                                nb.getLastCell().addSource("int randomSeed =" + randomSeed + ";")
                                        .addSource("Random random = new Random(randomSeed);")
                                        /*-----------------create learner----------------- **/
                                        .addSource("int numFolds = " + numFolds + ";")
                                        .addSource(learnerOptionsString.getClassShortName() + "[] " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array = new " + learnerOptionsString.getClassShortName() + "[numFolds];")
                                        .addSource(learnerOptionsString.generateOptionsString())
                                        .addSource(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();")
                                        .addSource(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".resetLearning();")
                                        //Learner[] learners = new Learner[numFoldsOption];
                                        //Learner baseLearner = (Learner) getPreparedClassOption(this.learnerOption);
                                        //baseLearner.resetLearning();
                                        /*--------------create stream------------**/
                                        .addSource("")
                                        .addSource("String streamString = \""+ streamString +"\";")
                                        .addSource(streamOptionsString.generateOptionsString())
                                        .addSource(streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();")
                                        .addSource(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".setModelContext("
                                                + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getHeader());")
                                        /*--------------create evaluator-----------**/
                                        .addSource("")
                                        .addSource("String evaluatorString = \""+ evaluatorString + "\";")
                                        .addSource(evaluatorOptionsString.getClassShortName() + "[] " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array = new " + evaluatorOptionsString.getClassShortName() + "[numFolds];")
                                        .addSource(evaluatorOptionsString.generateOptionsString());
                                nb.getLastCell().addSource("for (int i = 0; i <" + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length; i++) {")
                                        .addSource("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i] = (" + learnerOptionsString.getClassShortName() + ") " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".copy();")
                                        .addSource("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].setModelContext(" + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getHeader());")
                                        .addSource("    " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "Array[i] = (" +
                                                evaluatorOptionsString.getClassShortName() + ") " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".copy();")
                                        .addSource("}");
                            } else {
                                /*-----------------create learner----------------- **/
                                nb.getLastCell().addSource(learnerOptionsString.generateOptionsString())
                                        .addSource(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();");
                                if (currentTask instanceof EvaluateInterleavedTestThenTrain) {
                                    nb.getLastCell().addSource("int randomSeed = " + randomSeed + ";")
                                            .addSource("if (" + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".isRandomizable()) {")
                                            .addSource("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".setRandomSeed(randomSeed);")
                                            .addSource("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".resetLearning();")
                                            .addSource("}");
                                }
                                /*--------------create stream------------**/
                                nb.getLastCell().addSource("")
                                        .addSource("String streamString = \""+ streamString +"\";")
                                        .addSource(streamOptionsString.generateOptionsString())
                                        .addSource(streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".prepareForUse();")
                                        .addSource(learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".setModelContext("
                                                + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getHeader());")
                                        /*--------------create evaluator-----------**/
                                        .addSource("")
                                        .addSource("String evaluatorString = \""+ evaluatorString +"\";")
                                        .addSource(evaluatorOptionsString.generateOptionsString());
                            }


                            /*--------------set environment parameters----------- **/
                            nb.addMarkdown()
                                    .addSource("## Setting environmental parameters");
                            nb.addCode()
                                    .addSource("int maxInstances = " + instanceLimit + ";")
                                    .addSource("long instancesProcessed = 0;")
                                    .addSource("int maxSeconds = " + timeLimit + ";")
                                    .addSource("int secondsElapsed = 0;")
                                    .addSource("LearningCurve learningCurve = new LearningCurve(\"learning evaluation instances\");")
                                    .addSource("File dumpFile = new File(\""+dumpFilePath+"\");")
                                    .addSource("PrintStream immediateResultStream = null;")
                                    .addSource("if (dumpFile != null) {")
                                    .addSource("    try {")
                                    .addSource("        if (dumpFile.exists()) {")
                                    .addSource("            immediateResultStream = new PrintStream(")
                                    .addSource("                    new FileOutputStream(dumpFile, true), true);")
                                    .addSource("        } else {")
                                    .addSource("            immediateResultStream = new PrintStream(")
                                    .addSource("                    new FileOutputStream(dumpFile), true);")
                                    .addSource("        }")
                                    .addSource("    } catch (Exception ex) {")
                                    .addSource("        throw new RuntimeException(")
                                    .addSource("                \"Unable to open immediate result file: \"  + dumpFile, ex);")
                                    .addSource("    }")
                                    .addSource("}");

                            if ((currentTask instanceof EvaluatePrequential) || (currentTask instanceof EvaluatePrequentialDelayed) ||
                                    (currentTask instanceof EvaluatePrequentialRegression)) {
                                //File for output predictions
                                if (outputPredictionFile != null) {
                                    nb.getLastCell().addSource("File outputPredictionFile = new File(\"" +
                                            outputPredictionFile.getAbsolutePath().replace('\\', '/')+ "\");");
                                } else nb.getLastCell().addSource("File outputPredictionFile = null;");

                                nb.getLastCell().addSource("PrintStream outputPredictionResultStream = null;")
                                        .addSource("if (outputPredictionFile != null) {")
                                        .addSource("    try {")
                                        .addSource("        if (outputPredictionFile.exists()) {")
                                        .addSource("            outputPredictionResultStream = new PrintStream(")
                                        .addSource("                    new FileOutputStream(outputPredictionFile, true), true);")
                                        .addSource("        } else {")
                                        .addSource("            outputPredictionResultStream = new PrintStream(")
                                        .addSource("                    new FileOutputStream(outputPredictionFile), true);")
                                        .addSource("        }")
                                        .addSource("    } catch (Exception ex) {")
                                        .addSource("        throw new RuntimeException(")
                                        .addSource("                 \"Unable to open prediction result file: \" + outputPredictionFile, ex);")
                                        .addSource("    }")
                                        .addSource("}");
                                if (currentTask instanceof EvaluatePrequentialDelayed) {
                                    nb.getCellByIndex(1).addSource("import java.util.LinkedList;");
                                    nb.getLastCell().addSource("LinkedList<Example> trainInstances = new LinkedList<Example>();");
                                }
                            } else if (currentTask instanceof EvaluatePrequentialDelayedCV) {
                                nb.getCellByIndex(1).addSource("import java.util.LinkedList;");
                                nb.getLastCell().addSource("LinkedList<LinkedList<Example>> trainInstances = new LinkedList<LinkedList<Example>>();")

                                        .addSource("for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length; i++) {")
                                        .addSource("   trainInstances.add(new LinkedList<Example>());")
                                        .addSource("}");
                            }
                            nb.getLastCell().addSource("boolean firstDump = true;")
                                    .addSource("boolean firstPrint = true;")
                                    .addSource("boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();")
                                    .addSource("long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();")
                                    .addSource("long lastEvaluateStartTime = evaluateStartTime;")
                                    .addSource("double RAMHours = 0.0;");
                            // begin the loop
                            nb.addMarkdown()
                                    .addSource("## Testing & training, exporting result");

                            nb.addCode()
                                    .addSource("DrawTable table = new DrawTable();");
                            nb.getCellByIndex(1).addSource("import moaflow.sink.DrawTable;");
                            nb.getLastCell().addSource("System.out.println( \"Evaluating learner...\");")
                                    .addSource("while (" + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".hasMoreInstances()")
                                    .addSource("        && ((maxInstances < 0) || (instancesProcessed < maxInstances))")
                                    .addSource("        && ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {");

                            if (currentTask instanceof EvaluatePrequentialCV) {
                                nb.getCellByIndex(1).addSource("import moa.core.MiscUtils;");
                                nb.getLastCell().addSource("    Example trainInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".nextInstance();")
                                        .addSource("    Example testInst = trainInst; //.copy();")
                                        //testInst.setClassMissing();

                                        .addSource("    for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length; i++) {")
                                        .addSource("       " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].addResult(testInst, " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].getVotesForInstance(testInst));")
                                        .addSource("    }")

                                        .addSource("    for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length; i++) {")
                                        .addSource("        int k = 1;")
                                        .addSource("        int validationMethodology = " + validationMethodology + ";")
                                        .addSource("        switch (validationMethodology) {")
                                        .addSource("            case 0: //Cross-Validation;")
                                        .addSource("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length == i ? 0: 1; //Test all except one")
                                        .addSource("                break;")
                                        .addSource("            case 1: //Bootstrap;")
                                        .addSource("                k = MiscUtils.poisson(1, random);")
                                        .addSource("                break;")
                                        .addSource("            case 2: //Split-Validation;")
                                        .addSource("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length == i ? 1: 0; //Test only one")
                                        .addSource("                break;")
                                        .addSource("        }")
                                        .addSource("        if (k > 0) {")
                                        .addSource("            Example weightedInst = (Example) trainInst.copy();")
                                        .addSource("            weightedInst.setWeight(trainInst.weight() * k);")
                                        .addSource("            " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].trainOnInstance(weightedInst);")
                                        .addSource("        }")
                                        .addSource("    }")
                                        .addSource("    instancesProcessed++;");
                            } else if (currentTask instanceof EvaluatePrequentialDelayed) {
                                nb.getCellByIndex(1).addSource("import moa.core.Utils;")
                                        .addSource("import moa.core.InstanceExample;");
                                nb.getLastCell().addSource("    instancesProcessed++;")
                                        .addSource("    Example currentInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".nextInstance();")
                                        .addSource("    boolean trainOnInitialWindow = " + trainOnInitialWindow + ";")
                                        .addSource("    boolean trainInBatches = " + trainInBatches + ";")
                                        .addSource("    int initialWindowSize = " + initialWindowSize + ";")
                                        .addSource("    int delayLength = " + delayLength + ";")
                                        .addSource("    if (instancesProcessed <= initialWindowSize) {")
                                        .addSource("        if (trainOnInitialWindow) {")
                                        .addSource("            " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".trainOnInstance(currentInst);")
                                        .addSource("        } else if ((initialWindowSize - instancesProcessed) < delayLength) {")
                                        .addSource("            trainInstances.addLast(currentInst);")
                                        .addSource("        }")
                                        .addSource("    } else {")
                                        .addSource("        trainInstances.addLast(currentInst);")

                                        .addSource("        if (delayLength < trainInstances.size()) {")
                                        .addSource("            if (trainInBatches) {")
                                        .addSource("                // Do not train on the latest instance, otherwise")
                                        .addSource("                // it would train on k+1 instances")
                                        .addSource("                while (trainInstances.size() > 1) {")
                                        .addSource("                    Example trainInst = trainInstances.removeFirst();")
                                        .addSource("                    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".trainOnInstance(trainInst);")
                                        .addSource("                }")
                                        .addSource("            } else {")
                                        .addSource("                Example trainInst = trainInstances.removeFirst();")
                                        .addSource("                " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".trainOnInstance(trainInst);")
                                        .addSource("            }")
                                        .addSource("        }")

                                        .addSource("        // Remove class label from test instances.")
                                        .addSource("        Instance testInstance = ((Instance) currentInst.getData()).copy();")
                                        .addSource("        testInstance.setMissing(testInstance.classAttribute());")
                                        .addSource("        testInstance.setClassValue(0.0);")

                                        .addSource("        Example testInst = new InstanceExample(testInstance);")
                                        .addSource("        double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".getVotesForInstance(testInst);")
                                        //          reinstate the testInstance as it is used in evaluator.addResult
                                        .addSource("        testInstance = ((Instance) currentInst.getData()).copy();")
                                        .addSource("        testInst = new InstanceExample(testInstance);")

                                        .addSource("        // Output prediction")
                                        .addSource("        if (outputPredictionFile != null) {")
                                        .addSource("            int trueClass = (int) ((Instance) currentInst.getData()).classValue();")
                                        .addSource("            outputPredictionResultStream.println(Utils.maxIndex(prediction) + \",\"  + ( ")
                                        .addSource("                    ((Instance) testInst.getData()).classIsMissing() == true ? \" ? \" : trueClass));")
                                        .addSource("        }")
                                        .addSource("        " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + ".addResult(testInst, prediction);");
                            } else if (currentTask instanceof EvaluatePrequentialDelayedCV) {
                                nb.getCellByIndex(1).addSource("import moa.core.MiscUtils;");
                                nb.getLastCell().addSource("    instancesProcessed++;")
                                        .addSource("    Example trainInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".nextInstance();")
                                        .addSource("    Example testInst = trainInst; //.copy();")
                                        .addSource("    for(int i = 0; i < " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length; i++) {")

                                        .addSource("        double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].getVotesForInstance(testInst);")
                                        .addSource("        " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].addResult(testInst, prediction);")

                                        .addSource("        int k = 1;")
                                        .addSource("        int validationMethodology = " + validationMethodology + ";")
                                        .addSource("        switch (validationMethodology) {")
                                        .addSource("            case 0: //Cross-Validation;")
                                        .addSource("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length == i ? 0: 1; //Test all except one")
                                        .addSource("                break;")
                                        .addSource("            case 1: //Bootstrap;")
                                        .addSource("                k = MiscUtils.poisson(1, random);")
                                        .addSource("                break;")
                                        .addSource("            case 2: //Split-Validation;")
                                        .addSource("                k = instancesProcessed % " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array.length == i ? 1: 0; //Test only one")
                                        .addSource("                break;")
                                        .addSource("        }")
                                        .addSource("        if (k > 0) {")
                                        .addSource("            trainInstances.get(i).addLast(trainInst);")
                                        .addSource("        }")
                                        .addSource("        int delayLength = " + delayLength + ";")
                                        .addSource("        if(delayLength < trainInstances.get(i).size()) {")
                                        .addSource("        Example trainInstI = trainInstances.get(i).removeFirst();")
                                        .addSource("        " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].trainOnInstance(trainInstI);")
                                        .addSource("        }")
                                        .addSource("    }");
                            } else {
                                nb.getLastCell().addSource("    Example trainInst = " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".nextInstance();")
                                        .addSource("    Example testInst = trainInst; //.copy();");
                                if (currentTask instanceof EvaluatePrequentialRegression) {
                                    nb.getCellByIndex(1).addSource("import com.yahoo.labs.samoa.instances.Prediction;");
                                    nb.getLastCell().addSource("    Prediction prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                            + ".getPredictionForInstance(testInst);")
                                            .addSource("    if (outputPredictionFile != null) {")
                                            .addSource("       double trueClass = ((Instance) trainInst.getData()).classValue();")
                                            .addSource("       outputPredictionResultStream.println(prediction + \",\" + trueClass);")
                                            .addSource("    }");
                                } else if (currentTask instanceof EvaluatePrequential) {
                                    nb.getCellByIndex(1).addSource("import moa.core.Utils;");
                                    nb.getLastCell().addSource("    double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getVotesForInstance(testInst);")
                                            .addSource("    // Output prediction")
                                            .addSource("    if (outputPredictionFile != null) {")
                                            .addSource("       int trueClass = (int) ((Instance) trainInst.getData()).classValue();")
                                            .addSource("       outputPredictionResultStream.println(Utils.maxIndex(prediction) + \",\" + (")
                                            .addSource("       ((Instance) testInst.getData()).classIsMissing() == true ? \" ? \" : trueClass));")
                                            .addSource("    }");
                                } else
                                    nb.getLastCell().addSource("    double[] prediction = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".getVotesForInstance(testInst);");

                                nb.getLastCell().addSource("    " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".addResult(testInst, prediction);")
                                        .addSource("    " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".trainOnInstance(trainInst);")
                                        .addSource("    instancesProcessed++;");
                            }
                            nb.getLastCell().addSource("    if (instancesProcessed % " + sampleFrequency + " == 0")
                                    .addSource("            ||  " + streamOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ".hasMoreInstances() == false) {")
                                    .addSource("        long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();")
                                    .addSource("        double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);")
                                    .addSource("        double timeIncrement = TimingUtils.nanoTimeToSeconds(evaluateTime - lastEvaluateStartTime);");
                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV)) {
                                nb.getLastCell().addSource("        for (int i = 0; i <" + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + "Array.length; i++) {")
                                        .addSource("        double RAMHoursIncrement = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                                + "Array[i].measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs")
                                        .addSource("        RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours")
                                        .addSource("        RAMHours += RAMHoursIncrement;")
                                        .addSource("        }");
                            } else {
                                nb.getLastCell().addSource("        double RAMHoursIncrement = " + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase()
                                        + ".measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs")
                                        .addSource("        RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours")
                                        .addSource("        RAMHours += RAMHoursIncrement;");
                            }
                            nb.getLastCell().addSource("        lastEvaluateStartTime = evaluateTime;");

                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV)) {
                                nb.getLastCell().addSource("        Measurement[] modelMeasurements = new Measurement[]{")
                                        .addSource("                                           new Measurement(")
                                        .addSource("                                           \"learning evaluation instances\",")
                                        .addSource("                                           instancesProcessed),")
                                        .addSource("                                           new Measurement(")
                                        .addSource("                                           \"evaluation time (\"")
                                        .addSource("                                           + (preciseCPUTiming ? \"cpu \"")
                                        .addSource("                                           : \"\") + \"seconds)\",")
                                        .addSource("                                           time),")
                                        .addSource("                                           new Measurement(")
                                        .addSource("                                           \"model cost (RAM-Hours)\",")
                                        .addSource("                                           RAMHours)")
                                        .addSource("                                        };")
                                        .addSource("        List<Measurement> measurementList = new LinkedList<>();")
                                        .addSource("        if (modelMeasurements != null) {")
                                        .addSource("            measurementList.addAll(Arrays.asList(modelMeasurements));")
                                        .addSource("        }")
                                        .addSource("        // add average of sub-model measurements")
                                        .addSource("        if ((" + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() +
                                                "Array != null) && (" + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "Array.length > 0)) {")
                                        .addSource("            List<Measurement[]> subMeasurements = new LinkedList<>();")
                                        .addSource("            for (" + evaluatorOptionsString.getClassShortName() + " subEvaluator : "
                                                + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "Array) {")
                                        .addSource("                if (subEvaluator != null) {")
                                        .addSource("                   subMeasurements.add(subEvaluator.getPerformanceMeasurements());")
                                        .addSource("                }")
                                        .addSource("            }")
                                        .addSource("            Measurement[] avgMeasurements = Measurement.averageMeasurements(" +
                                                "subMeasurements.toArray(new Measurement[subMeasurements.size()][]));")
                                        .addSource("            measurementList.addAll(Arrays.asList(avgMeasurements));")
                                        .addSource("        }")
                                        .addSource("           learningCurve.insertEntry(new LearningEvaluation(measurementList.toArray(new Measurement[measurementList.size()])));");
                            } else {
                                nb.getLastCell().addSource("        learningCurve.insertEntry(new LearningEvaluation(")
                                        .addSource("                new Measurement[]{")
                                        .addSource("                        new Measurement(")
                                        .addSource("                                \"learning evaluation instances\",")
                                        .addSource("                                instancesProcessed),")
                                        .addSource("                        new Measurement(")
                                        .addSource("                                \"evaluation time (\"")
                                        .addSource("                                        + (preciseCPUTiming ? \"cpu \"")
                                        .addSource("                                        : \"\") + \"seconds)\",")
                                        .addSource("                                time),")
                                        .addSource("                        new Measurement(")
                                        .addSource("                                \"model cost (RAM-Hours)\",")
                                        .addSource("                                RAMHours)")
                                        .addSource("                },")
                                        .addSource("                " + evaluatorOptionsString.getClassShortName().substring(0, 4).toLowerCase() + ", "
                                                + learnerOptionsString.getClassShortName().substring(0, 4).toLowerCase() + "));");
                            }

                            nb.getLastCell().addSource("        if (immediateResultStream != null) {")
                                    .addSource("            if (firstDump) {")
                                    .addSource("                immediateResultStream.println(learningCurve.headerToString());")
                                    .addSource("                firstDump = false;")
                                    .addSource("            }");
                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV) ||
                                    (currentTask instanceof EvaluateInterleavedTestThenTrain)) {
                                nb.getLastCell().addSource("            immediateResultStream.print(learnerString + \",\" + streamString +" +
                                        " \",\"+ Integer.toString(randomSeed) +\",\" );");
                            } else {
                                nb.getLastCell().addSource("            immediateResultStream.print(learnerString + \",\" + streamString);");
                            }

                            nb.getLastCell().addSource("            immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));")
                                    .addSource("            immediateResultStream.flush();")
                                    .addSource("        }")
                                    .addSource("        if (firstPrint) {");
                            if ((currentTask instanceof EvaluatePrequentialCV) || (currentTask instanceof EvaluatePrequentialDelayedCV) ||
                                    (currentTask instanceof EvaluateInterleavedTestThenTrain)) {
                                nb.getLastCell().addSource("           System.out.println(learnerString + \",\" + streamString + \", randomSeed = \"" +
                                        "+ Integer.toString(randomSeed) +\".\");");

                            } else {
                                nb.getLastCell().addSource("           System.out.println(learnerString + \",\" + streamString);");
                            }
                            nb.getLastCell().addSource("           System.out.print(table.headerLine(learningCurve.headerToString()));")
                                    .addSource("           firstPrint = false;")
                                    .addSource("        }")
                                    .addSource("        System.out.print(table.bodyLine(learningCurve.entryToString(learningCurve.numEntries() - 1)));")
                                    .addSource("        System.out.flush();")

                                    .addSource("    }")
                                    .addSource("}");
                            if (currentTask instanceof EvaluatePrequentialDelayed) nb.getLastCell().addSource("}");
                            nb.getLastCell().addSource("double time = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread()- evaluateStartTime);")
                                    .addSource("System.out.println(instancesProcessed + \" instances processed in \" + time + \" seconds.\");")
                                    .addSource("if (immediateResultStream != null) {")
                                    .addSource("    immediateResultStream.close();")
                                    .addSource("}");
                            /*---------------------------------------------------------------
                             end content of NotebookBuilder **/
                        }
                        /*
                         * here is the short version of IPYNB file, that need to run with moa-flow
                         */
                        else {
                            /*-----------------Imports all necessary libraries----------------- **/
                            nb.addMarkdown()
                                    .addSource("This IPYNB file was generated automatically by MOA GUI.<br>")
                                    .addSource("Task name: " + currentTask.getClass().getName() + "<br>")
                                    .addSource("## Libraries importing");
                            nb.addCode()
                                    .addSource("%maven nz.ac.waikato.cms.moa:moa:2019.05.0")
                                    .addSource("%classpath \"H:/MOA/moa-flow-core.jar\"")
                                    .addSource("import moaflow.transformer.*;")
                                    .addSource("import moaflow.core.Utils;")
                                    .addSource("import moaflow.sink.*;")
                                    .addSource("import moaflow.source.*;");

                            /*-----------------Prints out the configuration of learner, stream and evaluator----------------- **/
                            nb.addMarkdown()
                                    .addSource("## Configuring learner, stream and evaluator");
                            nb.addCode()
                                    .addSource("String learnerString = \""+learnerString+"\";")
                                    .addSource("String streamString = \""+streamString+"\";")
                                    .addSource("String evaluatorString = \""+evaluatorString+"\";");

                            /*-----------------Creates flow for testing, training and exporting result----------------- **/
                            nb.addMarkdown()
                                    .addSource("## Testing & training, exporting result");
                            nb.addCode()
                                    .addSource("InstanceSource source;")
                                    .addSource("source = new InstanceSource();")
                                    .addSource("source.setGenerator(streamString);;")
                                    .addSource("source.numInstances.setValue(" + instanceLimit + ");")
                                    .addSource("");

                            if (currentTask instanceof EvaluateInterleavedTestThenTrain || currentTask instanceof EvaluatePrequential || currentTask instanceof EvaluatePrequentialCV
                                    || currentTask instanceof EvaluatePrequentialDelayed || currentTask instanceof EvaluatePrequentialDelayedCV) {

                                nb.getLastCell().addSource("EvaluateClassifier eval = new EvaluateClassifier();")
                                        .addSource("eval.setClassifier(learnerString);");
                                if (currentTask instanceof EvaluateInterleavedTestThenTrain)
                                    nb.getLastCell().addSource("eval.setEvaluationScheme(\"Prequential\");");
                                else
                                    nb.getLastCell().addSource("eval.setEvaluationScheme(\""
                                            +currentTask.getClass().getName().substring(18)+"\");");

                            } else if (currentTask instanceof EvaluatePrequentialRegression) {
                                nb.getLastCell().addSource("EvaluateRegressor eval = new EvaluateRegressor();")
                                        .addSource("eval.setRegressor(learnerString);");
                            }
                            nb.getLastCell().addSource("eval.setEvaluator(evaluatorString);")
                                    .addSource("eval.everyNth.setValue(" + sampleFrequency + ");")
                                    .addSource("source.subscribe(eval);")
                                    .addSource("")
                                    .addSource("MeasurementTableSawPlot plot = new MeasurementTableSawPlot();");
                            if (currentTask instanceof EvaluateInterleavedTestThenTrain || currentTask instanceof EvaluatePrequential ||
                                    currentTask instanceof EvaluatePrequentialDelayed) {
                                nb.getLastCell().addSource("plot.measurement.setValue(\"classifications correct (percent)\");");

                            } else if (currentTask instanceof EvaluatePrequentialCV || currentTask instanceof EvaluatePrequentialDelayedCV) {
                                nb.getLastCell().addSource("plot.measurement.setValue(\"[avg] classifications correct (percent)\");");
                            } else if (currentTask instanceof EvaluatePrequentialRegression) {
                                nb.getLastCell().addSource("plot.measurement.setValue(\"mean absolute error\");");
                            }
                            nb.getLastCell().addSource("plot.maxPoints.setValue(-1);")
                                    .addSource("eval.subscribe(plot);")
                                    .addSource("")
                                    .addSource("OutputLearningCurve curve = new OutputLearningCurve();")
                                    .addSource("eval.subscribe(curve);")
                                    .addSource("")
                                    .addSource("System.out.println(Utils.toTree(source));")
                                    .addSource("")
                                    .addSource("source.start();");
                        }

                        w.write(nb.build());
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
