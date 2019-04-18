/*
 *    SEAGenerator.java
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
 * Stream generator for SEA concepts functions.
 * Generator described in the paper:<br/>
 * W. Nick Street and YongSeog Kim
 *    "A streaming ensemble algorithm (SEA) for large-scale classification",
 *     KDD '01: Proceedings of the seventh ACM SIGKDD international conference on Knowledge discovery and data mining
 *     377-382 2001.<br/><br/>
 *
 * Notes:<br/>
 * The built in functions are based on the paper.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class SEAGenerator extends AbstractOptionHandler implements
        InstanceStream, CapabilitiesHandler {

    @Override
    public String getPurposeString() {
        return "Generates SEA concepts functions.";
    }

    private static final long serialVersionUID = 1L;

    public IntOption functionOption = new IntOption("function", 'f',
            "Classification function used, as defined in the original paper.",
            1, 1, 4);

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public FlagOption balanceClassesOption = new FlagOption("balanceClasses",
            'b', "Balance the number of instances of each class.");

    public IntOption numInstancesConcept = new IntOption("numInstancesConcept", 'n',
            "The number of instances for each concept.", 0, 0, Integer.MAX_VALUE);

    public IntOption noisePercentageOption = new IntOption("noisePercentage",
            'p', "Percentage of noise to add to the data.", 10, 0, 100);

    protected interface ClassFunction {

        public int determineClass(double attrib1, double attrib2, double attrib3);
    }

    protected static ClassFunction[] classificationFunctions = {
        // function 1
        new ClassFunction() {

    @Override
    public int determineClass(double attrib1, double attrib2, double attrib3) {
        return (attrib1 + attrib2 <= 8) ? 0 : 1;
    }
},
        // function 2
        new ClassFunction() {

    @Override
    public int determineClass(double attrib1, double attrib2, double attrib3) {
        return (attrib1 + attrib2 <= 9) ? 0 : 1;
    }
},
        // function 3
        new ClassFunction() {

    public int determineClass(double attrib1, double attrib2, double attrib3) {
        return (attrib1 + attrib2 <= 7) ? 0 : 1;
    }
},
        // function 4
        new ClassFunction() {

    @Override
    public int determineClass(double attrib1, double attrib2, double attrib3) {
        return (attrib1 + attrib2 <= 9.5) ? 0 : 1;
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
        attributes.addElement(new Attribute("attrib1"));
        attributes.addElement(new Attribute("attrib2"));
        attributes.addElement(new Attribute("attrib3"));

        FastVector classLabels = new FastVector();
        classLabels.addElement("groupA");
        classLabels.addElement("groupB");
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
        double attrib1 = 0, attrib2 = 0, attrib3 = 0;
        int group = 0;
        boolean desiredClassFound = false;
        while (!desiredClassFound) {
            // generate attributes
            attrib1 = 10 * this.instanceRandom.nextDouble();
            attrib2 = 10 * this.instanceRandom.nextDouble();
            attrib3 = 10 * this.instanceRandom.nextDouble();

            // determine class
            group = classificationFunctions[this.functionOption.getValue() - 1].determineClass(attrib1, attrib2, attrib3);
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
        //Add Noise
        if ((1 + (this.instanceRandom.nextInt(100))) <= this.noisePercentageOption.getValue()) {
            group = (group == 0 ? 1 : 0);
        }

        // construct instance
        InstancesHeader header = getHeader();
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setValue(0, attrib1);
        inst.setValue(1, attrib2);
        inst.setValue(2, attrib3);
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
        if (this.getClass() == SEAGenerator.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
