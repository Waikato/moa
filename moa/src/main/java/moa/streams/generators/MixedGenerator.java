/*
 *    MixedGenerator.java
 *    Copyright (C) 2016 Instituto Federal de Pernambuco
 *    @author Paulo Gonçalves (paulogoncalves@recife.ifpe.edu.br)
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

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import moa.core.InstanceExample;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

/**
 * Abrupt concept drift, boolean noise-free examples. Four relevant attributes, 
 * two boolean attributes v,w and two numeric attributes from [0; 1]. The 
 * examples are classified positive if two of three conditions are satisfied: 
 * v,w, y &lt; 0,5 + 0,3 sin(3 * PI * x). After each context change the 
 * classification is reversed. Proposed by "Gama, Joao, et al. "Learning with 
 * drift detection." Advances in artificial intelligence–SBIA 2004. Springer 
 * Berlin Heidelberg, 2004. 286-295."
 *
 * @author Paulo Gonçalves (paulogoncalves@recife.ifpe.edu.br)
 * @version $Revision: 1 $
 */
public class MixedGenerator extends AbstractOptionHandler implements
        InstanceStream {

    public IntOption functionOption = new IntOption("function", 'f',
            "Classification function used, as defined in the original paper.",
            1, 1, 2);

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public FlagOption balanceClassesOption = new FlagOption("balanceClasses",
            'b', "Balance the number of instances of each class.");

    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    protected boolean nextClassShouldBeZero;

    protected interface ClassFunction {

        public int determineClass(double v, double w, double x, double y);
    }

    protected static ClassFunction[] classificationFunctions = {
        new ClassFunction() {
            @Override
            public int determineClass(double v, double w, double x, double y) {
                boolean z = y < 0.5 + 0.3 * Math.sin(3 * Math.PI * x);
                if ((v == 1 && w == 1) || (v == 1 && z) || (w == 1 && z)) {
                    return 0;
                } else {
                    return 1;
                }
            }
        },
        new ClassFunction() {
            @Override
            public int determineClass(double v, double w, double x, double y) {
                boolean z = y < 0.5 + 0.3 * Math.sin(3 * Math.PI * x);
                if ((v == 1 && w == 1) || (v == 1 && z) || (w == 1 && z)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        },};

    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }

    @Override
    public InstancesHeader getHeader() {
        return this.streamHeader;
    }

    @Override
    public long estimatedRemainingInstances() {
        return -1;
    }

    @Override
    public boolean hasMoreInstances() {
        return true;
    }

    @Override
    public InstanceExample nextInstance() {
        double v = 0, w = 0, x = 0, y = 0, group = 0;
        boolean desiredClassFound = false;
        while (!desiredClassFound) {
            v = (this.instanceRandom.nextDouble() < 0.5) ? 0 : 1;
            w = (this.instanceRandom.nextDouble() < 0.5) ? 0 : 1;
            x = this.instanceRandom.nextDouble();
            y = this.instanceRandom.nextDouble();
            group = classificationFunctions[this.functionOption.getValue() - 1].determineClass(v, w, x, y);
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
        inst.setValue(0, v);
        inst.setValue(1, w);
        inst.setValue(2, x);
        inst.setValue(3, y);
        inst.setDataset(header);
        inst.setClassValue(group);
        return new InstanceExample(inst);
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void restart() {
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
        this.nextClassShouldBeZero = false;
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        List booleanLabels = new ArrayList();
        booleanLabels.add("0");
        booleanLabels.add("1");

        ArrayList<Attribute> attributes = new ArrayList();
        Attribute attribute1 = new Attribute("v", booleanLabels);
        Attribute attribute2 = new Attribute("w", booleanLabels);

        Attribute attribute3 = new Attribute("x");
        Attribute attribute4 = new Attribute("y");

        List classLabels = new ArrayList();
        classLabels.add("positive");
        classLabels.add("negative");
        Attribute classAtt = new Attribute("class", classLabels);

        attributes.add(attribute1);
        attributes.add(attribute2);
        attributes.add(attribute3);
        attributes.add(attribute4);
        attributes.add(classAtt);

        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
        restart();
    }
}
