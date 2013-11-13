/*
 *    ConceptDriftRealStream.java
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
package moa.streams;

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
import moa.options.ClassOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.tasks.TaskMonitor;

/**
 * Stream generator that adds concept drift to examples in a stream with
 * different classes and attributes. Example: real datasets.
 *<br/><br/>
 * Example:
 *<br/><br/>
 * <code>ConceptDriftRealStream -s (ArffFileStream -f covtype.arff) \ <br/>
 *    -d (ConceptDriftRealStream -s (ArffFileStream -f PokerOrig.arff) \<br/>
 *    -d (ArffFileStream -f elec.arff) -w 5000 -p 1000000 ) -w 5000 -p 581012</code>
 *<br/><br/>
 * s : Stream <br/>
 * d : Concept drift Stream<br/>
 * p : Central position of concept drift change<br/>
 * w : Width of concept drift change<br/>
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class ConceptDriftRealStream extends AbstractOptionHandler implements
        InstanceStream {

    @Override
    public String getPurposeString() {
        return "Adds Concept Drift to examples in a stream.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to add concept drift.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public ClassOption driftstreamOption = new ClassOption("driftstream", 'd',
            "Concept drift Stream.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public FloatOption alphaOption = new FloatOption("alpha",
            'a', "Angle alpha of change grade.", 0.0, 0.0, 90.0);

    public IntOption positionOption = new IntOption("position",
            'p', "Central position of concept drift change.", 0);

    public IntOption widthOption = new IntOption("width",
            'w', "Width of concept drift change.", 1000);

    public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
            "Seed for random noise.", 1);

    protected InstanceStream inputStream;

    protected InstanceStream driftStream;

    protected Random random;

    protected int numberInstanceStream;

    protected InstancesHeader streamHeader;

    protected Instance inputInstance;

    protected Instance driftInstance;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {

        this.inputStream = (InstanceStream) getPreparedClassOption(this.streamOption);
        this.driftStream = (InstanceStream) getPreparedClassOption(this.driftstreamOption);
        this.random = new Random(this.randomSeedOption.getValue());
        numberInstanceStream = 0;
        if (this.alphaOption.getValue() != 0.0) {
            this.widthOption.setValue((int) (1 / Math.tan(this.alphaOption.getValue() * Math.PI / 180)));
        }

        // generate header
        Instances first = this.inputStream.getHeader();
        Instances second = this.driftStream.getHeader();
        FastVector newAttributes = new FastVector();
        for (int i = 0; i < first.numAttributes() - 1; i++) {
            newAttributes.addElement(first.attribute(i));
        }
        for (int i = 0; i < second.numAttributes() - 1; i++) {
            newAttributes.addElement(second.attribute(i));
        }


        Attribute classLabels;
        if (first.numClasses() < second.numClasses()) {
            classLabels = second.classAttribute();
        } else {
            classLabels = first.classAttribute();
        }
        newAttributes.addElement(classLabels);

        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), newAttributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
        restart();

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
    public InstancesHeader getHeader() {
        return this.streamHeader;
    }

    @Override
    public boolean isRestartable() {
        return (this.inputStream.isRestartable() && this.driftStream.isRestartable());
    }

    @Override
    public InstanceExample nextInstance() {
        numberInstanceStream++;
        double numclass = 0.0;
        double x = -4.0 * (double) (numberInstanceStream - this.positionOption.getValue()) / (double) this.widthOption.getValue();
        double probabilityDrift = 1.0 / (1.0 + Math.exp(x));
        if (this.random.nextDouble() > probabilityDrift) {
            if (this.inputStream.hasMoreInstances() == false) {
                this.inputStream.restart();
            }
            this.inputInstance = this.inputStream.nextInstance().getData();
            numclass = this.inputInstance.classValue();
        } else {
            if (this.driftStream.hasMoreInstances() == false) {
                this.driftStream.restart();
            }
            this.driftInstance = this.driftStream.nextInstance().getData();
            numclass = this.driftInstance.classValue();
        }
        int m = 0;
        double[] newVals = new double[this.inputInstance.numAttributes() + this.driftInstance.numAttributes() - 1];
        for (int j = 0; j < this.inputInstance.numAttributes() - 1; j++, m++) {
            newVals[m] = this.inputInstance.value(j);
        }
        for (int j = 0; j < this.driftInstance.numAttributes() - 1; j++, m++) {
            newVals[m] = this.driftInstance.value(j);
        }
        newVals[m] = numclass;
        //return new Instance(1.0, newVals);
        Instance inst = new DenseInstance(1.0, newVals);
        inst.setDataset(this.getHeader());
        inst.setClassValue(numclass);
        return new InstanceExample(inst);

    }

    @Override
    public void restart() {
        this.inputStream.restart();
        this.driftStream.restart();
        numberInstanceStream = 0;
        this.inputInstance = this.inputStream.nextInstance().getData();
        this.driftInstance = this.driftStream.nextInstance().getData();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
