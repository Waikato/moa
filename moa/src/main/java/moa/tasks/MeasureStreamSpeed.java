/*
 *    MeasureStreamSpeed.java
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

import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.LearningEvaluation;
import moa.options.ClassOption;
import com.github.javacliparser.IntOption;
import moa.streams.ExampleStream;
import moa.streams.InstanceStream;

/**
 * Task for measuring the speed of the stream.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class MeasureStreamSpeed extends AuxiliarMainTask {

    @Override
    public String getPurposeString() {
        return "Measures the speed of a stream.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to measure.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public IntOption generateSizeOption = new IntOption("generateSize", 'g',
            "Number of examples.", 10000000, 0, Integer.MAX_VALUE);

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        TimingUtils.enablePreciseTiming();
        int numInstances = 0;
        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
        long genStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        while (numInstances < this.generateSizeOption.getValue()) {
            stream.nextInstance();
            numInstances++;
        }
        double genTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread()
                - genStartTime);
        return new LearningEvaluation(
                new Measurement[]{
                    new Measurement("Number of instances generated",
                    numInstances),
                    new Measurement("Time elapsed", genTime),
                    new Measurement("Instances per second", numInstances
                    / genTime)});
    }

    @Override
    public Class<?> getTaskResultType() {
        return LearningEvaluation.class;
    }
}
