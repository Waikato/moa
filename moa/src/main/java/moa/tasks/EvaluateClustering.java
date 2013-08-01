/**
 * EvaluateClustering.java
 * 
 * @author Albert Bifet (abifet@cs.waikato.ac.nz)
 * @editor Yunsu Kim
 * 
 * Last edited: 2013/06/02
 */
package moa.tasks;

import moa.clusterers.AbstractClusterer;
import moa.core.ObjectRepository;
import moa.evaluation.LearningCurve;
import moa.gui.BatchCmd;
import moa.options.ClassOption;
import moa.options.FileOption;
import moa.options.IntOption;
import moa.streams.clustering.ClusteringStream;

/**
 * Task for evaluating a clusterer on a stream.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class EvaluateClustering extends MainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a clusterer on a stream.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Clusterer to train.", AbstractClusterer.class, "clustream.Clustream");

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.",  ClusteringStream.class,
            "RandomRBFGeneratorEvents");

    public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
            "Maximum number of instances to test/train on  (-1 = no limit).",
            100000, -1, Integer.MAX_VALUE);

    public IntOption measureCollectionTypeOption = new IntOption(
            "measureCollectionType", 'm',
            "Type of measure collection", 0, 0,
            Integer.MAX_VALUE);

    /*public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
    "Performance evaluation method.",
    LearningPerformanceEvaluator.class,
    "BasicClusteringPerformanceEvaluator");*/

    /*public IntOption timeLimitOption = new IntOption("timeLimit", 't',
    "Maximum number of seconds to test/train for (-1 = no limit).", -1,
    -1, Integer.MAX_VALUE);

    public IntOption sampleFrequencyOption = new IntOption("sampleFrequency",
    'f',
    "How many instances between samples of the learning performance.",
    100000, 0, Integer.MAX_VALUE);

    public IntOption maxMemoryOption = new IntOption("maxMemory", 'b',
    "Maximum size of model (in bytes). -1 = no limit.", -1, -1,
    Integer.MAX_VALUE);

    public IntOption memCheckFrequencyOption = new IntOption(
    "memCheckFrequency", 'q',
    "How many instances between memory bound checks.", 100000, 0,
    Integer.MAX_VALUE);*/
    public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
            "File to append intermediate csv reslts to.", "dumpClustering.csv", "csv", true);

    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

        BatchCmd.runBatch((ClusteringStream) getPreparedClassOption(this.streamOption),
                (AbstractClusterer) getPreparedClassOption(this.learnerOption),
                (int) measureCollectionTypeOption.getValue(),
                (int) this.instanceLimitOption.getValue(),
                (String) dumpFileOption.getValue());

        LearningCurve learningCurve = new LearningCurve("EvaluateClustering does not support custom output file (> [filename]).\n" +
        												"Check out the dump file to see the results (if you haven't specified, dumpClustering.csv by default).");
        //System.out.println(learner.toString());
        return learningCurve;
    }
}
