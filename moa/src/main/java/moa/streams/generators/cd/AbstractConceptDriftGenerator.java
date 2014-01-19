/*
 *    AbstractConceptDriftGenerator.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.streams.generators.cd;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import java.util.ArrayList;
import java.util.Random;
import moa.core.FastVector;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.streams.clustering.ClusterEvent;
import moa.tasks.TaskMonitor;

public abstract class AbstractConceptDriftGenerator extends AbstractOptionHandler implements
        ConceptDriftGenerator {

    @Override
    public String getPurposeString() {
        return "Generates a stream problem of predicting concept drift.";
    }
    protected ArrayList<ClusterEvent> clusterEvents;

    public ArrayList<ClusterEvent> getEventsList() {
        return this.clusterEvents;
    }
    private static final long serialVersionUID = 1L;

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public FlagOption notBinaryStreamOption = new FlagOption("notBinaryStream",
            'b', "Don't convert to a binary stream of 0 and 1.");

    public IntOption numInstancesConceptOption = new IntOption("numInstancesConcept", 'p',
            "The number of instances for each concept.", 500, 0, Integer.MAX_VALUE);

    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    protected int period;

    protected int numInstances;

    protected boolean change;

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {

        restart();

        this.numInstances = 0;
        this.period = numInstancesConceptOption.getValue();
        // generate header
        FastVector attributes = new FastVector();

        FastVector binaryLabels = new FastVector();
        binaryLabels.addElement("0");
        binaryLabels.addElement("1");

        if (!this.notBinaryStreamOption.isSet()) {

            attributes.addElement(new Attribute("input", binaryLabels));
        } else {
            attributes.addElement(new Attribute("input"));
        }

        // Ground Truth
        attributes.addElement(new Attribute("change", binaryLabels));
        attributes.addElement(new Attribute("ground truth input"));

        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);

        this.clusterEvents = new ArrayList<ClusterEvent>();

        //this.clusterEvents.add(new ClusterEvent(this,100,"Change", "Drift"));
        //this.clusterEvents.add(new ClusterEvent(this,200,"Change2", "Drift2"));

    }

    public long estimatedRemainingInstances() {
        return -1;
    }

    public InstancesHeader getHeader() {
        return this.streamHeader;
    }

    public boolean hasMoreInstances() {
        return true;
    }

    public boolean isRestartable() {
        return true;
    }

    protected abstract double nextValue();

    private int nextbinaryValue( double num) {
        int res = 0;
        if (this.instanceRandom.nextDouble() <= num) {
            res = 1;
        }
        return res;
    }

    public boolean getChange() {
        return this.change;
    }

    public InstanceExample nextInstance() {
        this.numInstances++;
        InstancesHeader header = getHeader();
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setDataset(header);
        double nextValue = this.nextValue();
        if (this.notBinaryStreamOption.isSet()) {
            inst.setValue(0,  nextValue);
        } else {
            inst.setValue(0, this.nextbinaryValue(nextValue));
        }
        //Ground truth
        inst.setValue(1, this.getChange() ? 1 : 0);
        if (this.getChange() == true) {
            //this.clusterEvents.add(new ClusterEvent(this, this.numInstances, "Change", "Drift"));
        }
        inst.setValue(2,  nextValue);
        return new InstanceExample(inst);
    }

    public void restart() {
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
    }

    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
