/*
 *    ConceptDriftStream.java
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

import java.util.Random;
import moa.core.Example;

import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.tasks.TaskMonitor;

/**
 * Stream generator that adds concept drift to examples in a stream.
 *<br/><br/>
 * Example:
 *<br/><br/>
 * <code>ConceptDriftStream -s (generators.AgrawalGenerator -f 7) <br/>
 *    -d (generators.AgrawalGenerator -f 2) -w 1000000 -p 900000</code>
 *<br/><br/>
 * s : Stream <br/>
 * d : Concept drift Stream<br/>
 * p : Central position of concept drift change<br/>
 * w : Width of concept drift change<br/>
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class ConceptDriftStream extends AbstractOptionHandler implements
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

    protected ExampleStream inputStream;

    protected ExampleStream driftStream;

    protected Random random;

    protected int numberInstanceStream;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {

        this.inputStream = (ExampleStream) getPreparedClassOption(this.streamOption);
        this.driftStream = (ExampleStream) getPreparedClassOption(this.driftstreamOption);
        this.random = new Random(this.randomSeedOption.getValue());
        numberInstanceStream = 0;
        if (this.alphaOption.getValue() != 0.0) {
            this.widthOption.setValue((int) (1 / Math.tan(this.alphaOption.getValue() * Math.PI / 180)));
        }
    }

    @Override
    public long estimatedRemainingInstances() {
        return this.inputStream.estimatedRemainingInstances() + this.driftStream.estimatedRemainingInstances();
    }

    @Override
    public InstancesHeader getHeader() {
        return this.inputStream.getHeader();
    }

    @Override
    public boolean hasMoreInstances() {
        return (this.inputStream.hasMoreInstances() || this.driftStream.hasMoreInstances());
    }

    @Override
    public boolean isRestartable() {
        return (this.inputStream.isRestartable() && this.driftStream.isRestartable());
    }

    @Override
    public Example nextInstance() {
        numberInstanceStream++;
        double x = -4.0 * (double) (numberInstanceStream - this.positionOption.getValue()) / (double) this.widthOption.getValue();
        double probabilityDrift = 1.0 / (1.0 + Math.exp(x));
        if (this.random.nextDouble() > probabilityDrift) {
            return this.inputStream.nextInstance();
        } else {
            return this.driftStream.nextInstance();
        }

    }

    @Override
    public void restart() {
        this.inputStream.restart();
        this.driftStream.restart();
        numberInstanceStream = 0;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
