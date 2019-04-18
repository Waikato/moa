/*
 *    STAGGERGenerator.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
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
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
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
 * Stream generator for STAGGER Concept functions.
 *
 *  Generator described in the paper:<br/>
 *   Jeffrey C. Schlimmer and Richard H. Granger Jr.
 *    "Incremental Learning from Noisy Data",
 *     Machine Learning 1: 317-354 1986.<br/><br/>
 *
 * Notes:<br/>
 * The built in functions are based on the paper (page 341).
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class STAGGERGenerator extends AbstractOptionHandler implements
        InstanceStream, CapabilitiesHandler {

    @Override
    public String getPurposeString() {
        return "Generates STAGGER Concept functions.";
    }

    private static final long serialVersionUID = 1L;

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public IntOption functionOption = new IntOption("function", 'f',
            "Classification function used, as defined in the original paper.",
            1, 1, 3);

    public FlagOption balanceClassesOption = new FlagOption("balanceClasses",
            'b', "Balance the number of instances of each class.");

    protected interface ClassFunction {

        public int determineClass(int size, int color, int shape);
    }

    protected static ClassFunction[] classificationFunctions = {
        // function 1
        new ClassFunction() {

    @Override
    public int determineClass(int size, int color, int shape) {
        return (size == 0 && color == 0) ? 1 : 0; //size==small && color==red
    }
},
        // function 2
        new ClassFunction() {

    @Override
    public int determineClass(int size, int color, int shape) {
        return (color == 2 || shape == 0) ? 1 : 0; //color==green || shape==circle
    }
},
        // function 3
        new ClassFunction() {

    @Override
    public int determineClass(int size, int color, int shape) {
        return (size == 1 || size == 2) ? 1 : 0; // size==medium || size==large
    }
}
    };

    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    protected boolean nextClassShouldBeZero;

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // generate header
        FastVector attributes = new FastVector();

        FastVector sizeLabels = new FastVector();
        sizeLabels.addElement("small");
        sizeLabels.addElement("medium");
        sizeLabels.addElement("large");
        attributes.addElement(new Attribute("size", sizeLabels));

        FastVector colorLabels = new FastVector();
        colorLabels.addElement("red");
        colorLabels.addElement("blue");
        colorLabels.addElement("green");
        attributes.addElement(new Attribute("color", colorLabels));

        FastVector shapeLabels = new FastVector();
        shapeLabels.addElement("circle");
        shapeLabels.addElement("square");
        shapeLabels.addElement("triangle");
        attributes.addElement(new Attribute("shape", shapeLabels));

        FastVector classLabels = new FastVector();
        classLabels.addElement("false");
        classLabels.addElement("true");
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

        int size = 0, color = 0, shape = 0, group = 0;
        boolean desiredClassFound = false;
        while (!desiredClassFound) {
            // generate attributes
            size = this.instanceRandom.nextInt(3);
            color = this.instanceRandom.nextInt(3);
            shape = this.instanceRandom.nextInt(3);

            // determine class
            group = classificationFunctions[this.functionOption.getValue() - 1].determineClass(size, color, shape);
            if (!this.balanceClassesOption.isSet()) {
                desiredClassFound = true;
            } else {
                // balance the classes
                if ((this.nextClassShouldBeZero && (group == 0))
                        || (!this.nextClassShouldBeZero && (group == 1))) {
                    desiredClassFound = true;
                    this.nextClassShouldBeZero = !this.nextClassShouldBeZero;
                } // else keep searching
            }
        }

        // construct instance
        InstancesHeader header = getHeader();
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setValue(0, size);
        inst.setValue(1, color);
        inst.setValue(2, shape);
        inst.setDataset(header);
        inst.setClassValue(group);
        return new InstanceExample(inst);
    }

    @Override
    public void restart() {
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
        this.nextClassShouldBeZero = false;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == STAGGERGenerator.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
