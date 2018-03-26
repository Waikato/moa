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
import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import moa.streams.clustering.ClusteringStream;

/**
 * Task for evaluating a clusterer on a stream.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class EvaluateClustering extends AuxiliarMainTask {

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

    public FlagOption generalEvalOption = new FlagOption("General", 'g',
			"GPrecision, GRecall, Redundancy, numCluster, numClasses");
   
    public FlagOption f1Option = new FlagOption("F1", 'f', "F1-P, F1-R, Purity.");
    
    public FlagOption entropyOption = new FlagOption("Entropy", 'e',
			"GT cross entropy, FC cross entropy, Homogeneity, Completeness, V-Measure, VarInformation.");
    
    public FlagOption cmmOption = new FlagOption("CMM", 'c',
			"CMM, CMM Basic, CMM Missed, CMM Misplaced, CMM Noise, CA Seperability, CA Noise, CA Model.");

    public FlagOption ssqOption = new FlagOption("SSQ", 'q', "SSQ.");
    
    public FlagOption separationOption = new FlagOption("Separation", 'p', "BSS, BSS-GT, BSS-Ratio.");
    
    public FlagOption silhouetteOption = new FlagOption("Silhouette", 'h', "SilhCoeff.");
    
    public FlagOption statisticalOption = new FlagOption("Statistical", 't', "van Dongen, Rand statistic.");
       
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

    // Given an array summarizing selected measures, set the appropriate flag options
    protected void setMeasures(boolean[] measures)
    {
    	this.generalEvalOption.setValue(measures[0]);
    	this.f1Option.setValue(measures[1]);
    	this.entropyOption.setValue(measures[2]);
    	this.cmmOption.setValue(measures[3]);
    	this.ssqOption.setValue(measures[4]);
    	this.separationOption.setValue(measures[5]);
    	this.silhouetteOption.setValue(measures[6]);
    	this.statisticalOption.setValue(measures[7]);
    }
    
    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

    	// Create an array to summarize the selected measures
    	boolean[] measureCollection = new boolean[8];
    	measureCollection[0] = this.generalEvalOption.isSet();
    	measureCollection[1] = this.f1Option.isSet();
    	measureCollection[2] = this.entropyOption.isSet();
    	measureCollection[3] = this.cmmOption.isSet();
    	measureCollection[4] = this.ssqOption.isSet();
    	measureCollection[5] = this.separationOption.isSet();
    	measureCollection[6] = this.silhouetteOption.isSet();
    	measureCollection[7] = this.statisticalOption.isSet();
    	
        BatchCmd.runBatch((ClusteringStream) getPreparedClassOption(this.streamOption),
                (AbstractClusterer) getPreparedClassOption(this.learnerOption),
                measureCollection,
                (int) this.instanceLimitOption.getValue(),
                (String) dumpFileOption.getValue());

        LearningCurve learningCurve = new LearningCurve("EvaluateClustering does not support custom output file (> [filename]).\n" +
        												"Check out the dump file to see the results (if you haven't specified, dumpClustering.csv by default).");
        //System.out.println(learner.toString());
        return learningCurve;
    }
}
