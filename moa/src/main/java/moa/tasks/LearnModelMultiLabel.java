/*
 *    LearnModelMultiTarget.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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

import moa.classifiers.Classifier;
import moa.classifiers.MultiLabelClassifier;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.rules.multilabel.functions.MultiLabelNaiveBayes;
import moa.core.ObjectRepository;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.streams.InstanceStream;
import moa.streams.MultiTargetInstanceStream;

import com.github.javacliparser.IntOption;

/**
 * Task for learning a model without any evaluation.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class LearnModelMultiLabel extends MultiLabelMainTask {

    @Override
    public String getPurposeString() {
        return "Learns a model from a stream.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Learner to train.", MultiLabelClassifier.class, MultiLabelNaiveBayes.class.getName());

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", MultiTargetInstanceStream.class,
            "MultiTargetArffFileStream");

    public IntOption maxInstancesOption = new IntOption("maxInstances", 'm',
            "Maximum number of instances to train on per pass over the data.",
            10000000, 0, Integer.MAX_VALUE);

    public IntOption numPassesOption = new IntOption("numPasses", 'p',
            "The number of passes to do over the data.", 1, 1,
            Integer.MAX_VALUE);

    public IntOption memCheckFrequencyOption = new IntOption(
            "memCheckFrequency", 'q',
            "How many instances between memory bound checks.", 100000, 0,
            Integer.MAX_VALUE);

    public LearnModelMultiLabel() {
    }

    public LearnModelMultiLabel(Classifier learner, InstanceStream stream,
                                int maxInstances, int numPasses) {
        this.learnerOption.setCurrentObject(learner);
        this.streamOption.setCurrentObject(stream);
        this.maxInstancesOption.setValue(maxInstances);
        this.numPassesOption.setValue(numPasses);
    }

    @Override
    public Class<?> getTaskResultType() {
        return MultiTargetRegressor.class;
    }

    @Override
    public Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        Learner learner = (Learner) getPreparedClassOption(this.learnerOption);
        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
        learner.setModelContext(stream.getHeader());
        int numPasses = this.numPassesOption.getValue();
        int maxInstances = this.maxInstancesOption.getValue();
        for (int pass = 0; pass < numPasses; pass++) {
            long instancesProcessed = 0;
            monitor.setCurrentActivity("Training learner"
                    + (numPasses > 1 ? (" (pass " + (pass + 1) + "/"
                    + numPasses + ")") : "") + "...", -1.0);
            if (pass > 0) {
                stream.restart();
            }
            while (stream.hasMoreInstances()
                    && ((maxInstances < 0) || (instancesProcessed < maxInstances))) {
                learner.trainOnInstance(stream.nextInstance());
                instancesProcessed++;
                if (instancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
                    if (monitor.taskShouldAbort()) {
                        return null;
                    }
                    long estimatedRemainingInstances = stream.estimatedRemainingInstances();
                    if (maxInstances > 0) {
                        long maxRemaining = maxInstances - instancesProcessed;
                        if ((estimatedRemainingInstances < 0)
                                || (maxRemaining < estimatedRemainingInstances)) {
                            estimatedRemainingInstances = maxRemaining;
                        }
                    }
                    monitor.setCurrentActivityFractionComplete(estimatedRemainingInstances < 0 ? -1.0
                            : (double) instancesProcessed
                            / (double) (instancesProcessed + estimatedRemainingInstances));
                    if (monitor.resultPreviewRequested()) {
                        monitor.setLatestResultPreview(learner.copy());
                    }
                }
            }
        }
        learner.setModelContext(stream.getHeader());
        return learner;
    }
}
