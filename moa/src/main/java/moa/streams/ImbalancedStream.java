/*
 * Copyright (c) 2018.
 * @author Jean Paul Barddal (jean.barddal@ppgia.pucpr.br)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package moa.streams;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import java.util.Random;


/**
 * Imbalanced Stream.
 *
 * This is a meta-generator that produces class imbalance in a stream.
 * Only two parameters are required to be set:
 * - The original stream
 * - The proportion of the stream with the minority label (1)
 *
 *
 * ------------------------------------------------------------------
 * |                          Disclaimer                            |
 * ------------------------------------------------------------------
 * | The current version of the meta-generator                      |
 * | works under the following assumptions:                         |
 * | 1. The original stream is balanced (or close to being balanced)|
 * | 2. The majority class is represented by index 0 and the        |
 * |  minority class is represented by 1.                           |
 * |                                                                |
 * ------------------------------------------------------------------
 *
 * @author Jean Paul Barddal (jean.barddal@ppgia.pucpr.br)
 * @version 1.0
 */

public class ImbalancedStream extends AbstractOptionHandler implements
        InstanceStream {


    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to imbalance.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public FloatOption minorityClassProportionOption = new FloatOption("minorityClassProportion", 'm',
            "Approximate proportion of the output stream that should belong to the minority class.",
            0.1, 0.0, 1.0);

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    protected ExampleStream originalStream    = null;
    protected Instances     instancesBuffer[] = null;
    protected Random        random            = null;

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        originalStream = (ExampleStream) getPreparedClassOption(streamOption);
        // TODO: Make the overall procedure multi-class
        instancesBuffer = new Instances[2];
        // initializes the buffers using the original header
        for(int i = 0; i < instancesBuffer.length; i++){
            instancesBuffer[i] = new Instances(originalStream.getHeader());
        }
        // initializes the random generator
        random = new Random(instanceRandomSeedOption.getValue());
    }

    @Override
    public InstancesHeader getHeader() {
        return originalStream.getHeader();
    }

    @Override
    public long estimatedRemainingInstances() {
        return originalStream.estimatedRemainingInstances();
    }

    @Override
    public boolean hasMoreInstances() {
        return originalStream.hasMoreInstances();
    }

    @Override
    public Example<Instance> nextInstance() {
        // randomly defines whether the next instance should be from the minority class or not.
        double p = random.nextDouble();
        int iClass = p <= minorityClassProportionOption.getValue() ? 1 : 0;

        // keeps on creating instances until we have an instance for the desired class
        while(instancesBuffer[iClass].size() == 0){
            Example<Instance> inst = originalStream.nextInstance();
            instancesBuffer[(int) inst.getData().classValue()].add(inst.getData());
        }

        // retrieves the instance from the desired class
        Instance instance = instancesBuffer[iClass].get(0);
        // and also removes it from the buffer
        instancesBuffer[iClass].delete(0);

        return new InstanceExample(instance);
    }

    @Override
    public boolean isRestartable() {
        return originalStream.isRestartable();
    }

    @Override
    public void restart() {
        this.random = new Random(instanceRandomSeedOption.getValue());
        this.originalStream.restart();
        // initializes the buffers using the original header
        for(int i = 0; i < instancesBuffer.length; i++){
            instancesBuffer[i] = new Instances(originalStream.getHeader());
        }
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {}

}
