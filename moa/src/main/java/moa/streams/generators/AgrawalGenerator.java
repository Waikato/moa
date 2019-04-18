/*
 *    AgrawalGenerator.java
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
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

/**
 * Stream generator for Agrawal dataset.
 * Generator described in paper:<br/>
 *   Rakesh Agrawal, Tomasz Imielinksi, and Arun Swami,
 *    "Database Mining: A Performance Perspective",
 *     IEEE Transactions on Knowledge and Data Engineering,
 *      5(6), December 1993. <br/><br/>
 * 
 * Public C source code available at:<br/>
 *   <a href="http://www.almaden.ibm.com/cs/projects/iis/hdb/Projects/data_mining/datasets/syndata.html">
 * http://www.almaden.ibm.com/cs/projects/iis/hdb/Projects/data_mining/datasets/syndata.html</a><br/><br/>
 *
 * Notes:<br/>
 * The built in functions are based on the paper (page 924),
 *  which turn out to be functions pred20 thru pred29 in the public C implementation.
 * Perturbation function works like C implementation rather than description in paper.
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class AgrawalGenerator extends AbstractOptionHandler implements
        InstanceStream, CapabilitiesHandler {

    @Override
    public String getPurposeString() {
        return "Generates one of ten different pre-defined loan functions.";
    }

    private static final long serialVersionUID = 1L;

    public IntOption functionOption = new IntOption("function", 'f',
            "Classification function used, as defined in the original paper.",
            1, 1, 10);

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public FloatOption peturbFractionOption = new FloatOption("peturbFraction",
            'p',
            "The amount of peturbation (noise) introduced to numeric values.",
            0.05, 0.0, 1.0);

    public FlagOption balanceClassesOption = new FlagOption("balanceClasses",
            'b', "Balance the number of instances of each class.");

    protected interface ClassFunction {

        public int determineClass(double salary, double commission, int age,
                int elevel, int car, int zipcode, double hvalue, int hyears,
                double loan);
    }

    protected static ClassFunction[] classificationFunctions = {
        // function 1
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        return ((age < 40) || (60 <= age)) ? 0 : 1;
    }
},
        // function 2
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        if (age < 40) {
            return ((50000 <= salary) && (salary <= 100000)) ? 0
                    : 1;
        } else if (age < 60) {// && age >= 40
            return ((75000 <= salary) && (salary <= 125000)) ? 0
                    : 1;
        } else {// age >= 60
            return ((25000 <= salary) && (salary <= 75000)) ? 0 : 1;
        }
    }
},
        // function 3
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        if (age < 40) {
            return ((elevel == 0) || (elevel == 1)) ? 0 : 1;
        } else if (age < 60) { // && age >= 40
            return ((elevel == 1) || (elevel == 2) || (elevel == 3)) ? 0
                    : 1;
        } else { // age >= 60
            return ((elevel == 2) || (elevel == 3) || (elevel == 4)) ? 0
                    : 1;
        }
    }
},
        // function 4
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        if (age < 40) {
            if ((elevel == 0) || (elevel == 1)) {
                return ((25000 <= salary) && (salary <= 75000)) ? 0
                        : 1;
            }
            return ((50000 <= salary) && (salary <= 100000)) ? 0
                    : 1;
        } else if (age < 60) {// && age >= 40
            if ((elevel == 1) || (elevel == 2) || (elevel == 3)) {
                return ((50000 <= salary) && (salary <= 100000)) ? 0
                        : 1;
            }
            return ((75000 <= salary) && (salary <= 125000)) ? 0
                    : 1;
        } else {// age >= 60
            if ((elevel == 2) || (elevel == 3) || (elevel == 4)) {
                return ((50000 <= salary) && (salary <= 100000)) ? 0
                        : 1;
            }
            return ((25000 <= salary) && (salary <= 75000)) ? 0 : 1;
        }
    }
},
        // function 5
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        if (age < 40) {
            if ((50000 <= salary) && (salary <= 100000)) {
                return ((100000 <= loan) && (loan <= 300000)) ? 0
                        : 1;
            }
            return ((200000 <= loan) && (loan <= 400000)) ? 0 : 1;
        } else if (age < 60) {// && age >= 40
            if ((75000 <= salary) && (salary <= 125000)) {
                return ((200000 <= loan) && (loan <= 400000)) ? 0
                        : 1;
            }
            return ((300000 <= loan) && (loan <= 500000)) ? 0 : 1;
        } else {// age >= 60
            if ((25000 <= salary) && (salary <= 75000)) {
                return ((300000 <= loan) && (loan <= 500000)) ? 0
                        : 1;
            }
            return ((100000 <= loan) && (loan <= 300000)) ? 0 : 1;
        }
    }
},
        // function 6
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        double totalSalary = salary + commission;
        if (age < 40) {
            return ((50000 <= totalSalary) && (totalSalary <= 100000)) ? 0
                    : 1;
        } else if (age < 60) {// && age >= 40
            return ((75000 <= totalSalary) && (totalSalary <= 125000)) ? 0
                    : 1;
        } else {// age >= 60
            return ((25000 <= totalSalary) && (totalSalary <= 75000)) ? 0
                    : 1;
        }
    }
},
        // function 7
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        double disposable = (2.0 * (salary + commission) / 3.0
                - loan / 5.0 - 20000.0);
        return disposable > 0 ? 0 : 1;
    }
},
        // function 8
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        double disposable = (2.0 * (salary + commission) / 3.0
                - 5000.0 * elevel - 20000.0);
        return disposable > 0 ? 0 : 1;
    }
},
        // function 9
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        double disposable = (2.0 * (salary + commission) / 3.0
                - 5000.0 * elevel - loan / 5.0 - 10000.0);
        return disposable > 0 ? 0 : 1;
    }
},
        // function 10
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        double equity = 0.0;
        if (hyears >= 20) {
            equity = hvalue * (hyears - 20.0) / 10.0;
        }
        double disposable = (2.0 * (salary + commission) / 3.0
                - 5000.0 * elevel + equity / 5.0 - 10000.0);
        return disposable > 0 ? 0 : 1;
    }
}};

    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    protected boolean nextClassShouldBeZero;

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // generate header
        FastVector attributes = new FastVector();
        attributes.addElement(new Attribute("salary"));
        attributes.addElement(new Attribute("commission"));
        attributes.addElement(new Attribute("age"));
        FastVector elevelLabels = new FastVector();
        for (int i = 0; i < 5; i++) {
            elevelLabels.addElement("level" + i);
        }
        attributes.addElement(new Attribute("elevel", elevelLabels));
        FastVector carLabels = new FastVector();
        for (int i = 0; i < 20; i++) {
            carLabels.addElement("car" + (i + 1));
        }
        attributes.addElement(new Attribute("car", carLabels));
        FastVector zipCodeLabels = new FastVector();
        for (int i = 0; i < 9; i++) {
            zipCodeLabels.addElement("zipcode" + (i + 1));
        }
        attributes.addElement(new Attribute("zipcode", zipCodeLabels));
        attributes.addElement(new Attribute("hvalue"));
        attributes.addElement(new Attribute("hyears"));
        attributes.addElement(new Attribute("loan"));
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
        double salary = 0, commission = 0, hvalue = 0, loan = 0;
        int age = 0, elevel = 0, car = 0, zipcode = 0, hyears = 0, group = 0;
        boolean desiredClassFound = false;
        while (!desiredClassFound) {
            // generate attributes
            salary = 20000.0 + 130000.0 * this.instanceRandom.nextDouble();
            commission = (salary >= 75000.0) ? 0
                    : (10000.0 + 65000.0 * this.instanceRandom.nextDouble());
            // true to c implementation:
            // if (instanceRandom.nextDouble() < 0.5 && salary < 75000.0)
            // commission = 10000.0 + 65000.0 * instanceRandom.nextDouble();
            age = 20 + this.instanceRandom.nextInt(61);
            elevel = this.instanceRandom.nextInt(5);
            car = this.instanceRandom.nextInt(20);
            zipcode = this.instanceRandom.nextInt(9);
            hvalue = (9.0 - zipcode) * 100000.0
                    * (0.5 + this.instanceRandom.nextDouble());
            hyears = 1 + this.instanceRandom.nextInt(30);
            loan = this.instanceRandom.nextDouble() * 500000.0;
            // determine class
            group = classificationFunctions[this.functionOption.getValue() - 1].determineClass(salary, commission, age, elevel, car,
                    zipcode, hvalue, hyears, loan);
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
        // perturb values
        if (this.peturbFractionOption.getValue() > 0.0) {
            salary = perturbValue(salary, 20000, 150000);
            if (commission > 0) {
                commission = perturbValue(commission, 10000, 75000);
            }
            age = (int) Math.round(perturbValue(age, 20, 80));
            hvalue = perturbValue(hvalue, (9.0 - zipcode) * 100000.0, 0, 135000);
            hyears = (int) Math.round(perturbValue(hyears, 1, 30));
            loan = perturbValue(loan, 0, 500000);
        }
        // construct instance
        InstancesHeader header = getHeader();
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setValue(0, salary);
        inst.setValue(1, commission);
        inst.setValue(2, age);
        inst.setValue(3, elevel);
        inst.setValue(4, car);
        inst.setValue(5, zipcode);
        inst.setValue(6, hvalue);
        inst.setValue(7, hyears);
        inst.setValue(8, loan);
        inst.setDataset(header);
        inst.setClassValue(group);
        return new InstanceExample(inst);
    }

    protected double perturbValue(double val, double min, double max) {
        return perturbValue(val, max - min, min, max);
    }

    protected double perturbValue(double val, double range, double min,
            double max) {
        val += range * (2.0 * (this.instanceRandom.nextDouble() - 0.5))
                * this.peturbFractionOption.getValue();
        if (val < min) {
            val = min;
        } else if (val > max) {
            val = max;
        }
        return val;
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
        if (this.getClass() == AgrawalGenerator.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
