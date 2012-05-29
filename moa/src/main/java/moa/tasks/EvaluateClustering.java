/*
 *    EvaluateClustering.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet@cs.waikato.ac.nz)
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
package moa.tasks;

import moa.core.ObjectRepository;
import moa.evaluation.LearningCurve;
import moa.options.ClassOption;
import moa.options.FileOption;
import moa.options.IntOption;
import moa.gui.BatchCmd;
import moa.clusterers.AbstractClusterer;
import moa.streams.clustering.ClusteringStream;
import moa.streams.clustering.RandomRBFGeneratorEvents;

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
            "File to append intermediate csv reslts to.", "outputClustering.csv", "csv", true);

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

        LearningCurve learningCurve = new LearningCurve(
                "learning evaluation instances");
        //System.out.println(learner.toString());
        return learningCurve;
    }
}
