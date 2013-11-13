/*
 *    LEDGenerator.java
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
 * Stream generator for the problem of predicting the digit displayed on a 7-segment LED display.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class LEDGenerator extends AbstractOptionHandler implements
        InstanceStream {

    @Override
    public String getPurposeString() {
        return "Generates a problem of predicting the digit displayed on a 7-segment LED display.";
    }

    private static final long serialVersionUID = 1L;

    public static final int NUM_IRRELEVANT_ATTRIBUTES = 17;

    protected static final int originalInstances[][] = {
        {1, 1, 1, 0, 1, 1, 1}, {0, 0, 1, 0, 0, 1, 0},
        {1, 0, 1, 1, 1, 0, 1}, {1, 0, 1, 1, 0, 1, 1},
        {0, 1, 1, 1, 0, 1, 0}, {1, 1, 0, 1, 0, 1, 1},
        {1, 1, 0, 1, 1, 1, 1}, {1, 0, 1, 0, 0, 1, 0},
        {1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 0, 1, 1}};

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public IntOption noisePercentageOption = new IntOption("noisePercentage",
            'n', "Percentage of noise to add to the data.", 10, 0, 100);

    public FlagOption suppressIrrelevantAttributesOption = new FlagOption(
            "suppressIrrelevantAttributes", 's',
            "Reduce the data to only contain 7 relevant binary attributes.");

    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // generate header
        FastVector attributes = new FastVector();
        FastVector binaryLabels = new FastVector();
        binaryLabels.addElement("0");
        binaryLabels.addElement("1");
        int numAtts = 7;
        if (!this.suppressIrrelevantAttributesOption.isSet()) {
            numAtts += NUM_IRRELEVANT_ATTRIBUTES;
        }
        for (int i = 0; i < numAtts; i++) {
            attributes.addElement(new Attribute("att" + (i + 1), binaryLabels));
        }
        FastVector classLabels = new FastVector();
        for (int i = 0; i < 10; i++) {
            classLabels.addElement(Integer.toString(i));
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
        int selected = this.instanceRandom.nextInt(10);
        for (int i = 0; i < 7; i++) {
            if ((1 + (this.instanceRandom.nextInt(100))) <= this.noisePercentageOption.getValue()) {
                inst.setValue(i, originalInstances[selected][i] == 0 ? 1 : 0);
            } else {
                inst.setValue(i, originalInstances[selected][i]);
            }
        }
        if (!this.suppressIrrelevantAttributesOption.isSet()) {
            for (int i = 0; i < NUM_IRRELEVANT_ATTRIBUTES; i++) {
                inst.setValue(i + 7, this.instanceRandom.nextInt(2));
            }
        }
        inst.setClassValue(selected);
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
