/*
 *    WaveformGenerator.java
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
package moa.streams.generators;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import moa.core.FastVector;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import java.util.Random;
import moa.core.InstanceExample;

import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

/**
 * Stream generator for the problem of predicting one of three waveform types.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class WaveformGenerator extends AbstractOptionHandler implements
        InstanceStream {

    @Override
    public String getPurposeString() {
        return "Generates a problem of predicting one of three waveform types.";
    }

    private static final long serialVersionUID = 1L;

    public static final int NUM_CLASSES = 3;

    public static final int NUM_BASE_ATTRIBUTES = 21;

    public static final int TOTAL_ATTRIBUTES_INCLUDING_NOISE = 40;

    protected static final int hFunctions[][] = {
        {0, 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0},
        {0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0, 0, 0, 0, 0}};

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public FlagOption addNoiseOption = new FlagOption("addNoise", 'n',
            "Adds noise, for a total of 40 attributes.");

    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // generate header
        FastVector attributes = new FastVector();
        int numAtts = this.addNoiseOption.isSet() ? TOTAL_ATTRIBUTES_INCLUDING_NOISE
                : NUM_BASE_ATTRIBUTES;
        for (int i = 0; i < numAtts; i++) {
            attributes.addElement(new Attribute("att" + (i + 1)));
        }
        FastVector classLabels = new FastVector();
        for (int i = 0; i < NUM_CLASSES; i++) {
            classLabels.addElement("class" + (i + 1));
        }
        attributes.addElement(new Attribute("class", classLabels));
        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
        restart();
    }

    @Override
    public long estimatedRemainingInstances() {
        return -1;
    }

    @Override
    public InstancesHeader getHeader() {
        return this.streamHeader;
    }

    @Override
    public boolean hasMoreInstances() {
        return true;
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public InstanceExample nextInstance() {
        InstancesHeader header = getHeader();
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setDataset(header);
        int waveform = this.instanceRandom.nextInt(NUM_CLASSES);
        int choiceA = 0, choiceB = 0;
        switch (waveform) {
            case 0:
                choiceA = 0;
                choiceB = 1;
                break;
            case 1:
                choiceA = 0;
                choiceB = 2;
                break;
            case 2:
                choiceA = 1;
                choiceB = 2;
                break;

        }
        double multiplierA = this.instanceRandom.nextDouble();
        double multiplierB = 1.0 - multiplierA;
        for (int i = 0; i < NUM_BASE_ATTRIBUTES; i++) {
            inst.setValue(i, (multiplierA * hFunctions[choiceA][i])
                    + (multiplierB * hFunctions[choiceB][i])
                    + this.instanceRandom.nextGaussian());
        }
        if (this.addNoiseOption.isSet()) {
            for (int i = NUM_BASE_ATTRIBUTES; i < TOTAL_ATTRIBUTES_INCLUDING_NOISE; i++) {
                inst.setValue(i, this.instanceRandom.nextGaussian());
            }
        }
        inst.setClassValue(waveform);
        return new InstanceExample(inst);
    }

    @Override
    public void restart() {
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
