/*
 * Copyright (c) 2018
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

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.*;
import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import java.util.ArrayList;
import java.util.Random;


/**
 * IrrelevantFeatureAppender Stream.
 *
 * This is a meta-generator that appends irrelevant features in a stream.
 * The following parameters are required to be set:
 * - The original stream,
 * - The number of numeric features to be appended,
 * - The number of categorical features to be appended, and
 * - The number of values for each categorical feature.
 *
 * The values for each new feature are drawn from an uniform distribution
 * and are not related with the class anyhow. This means that no
 * correlation between each feature and the class will be observed,
 * and the entropy will be maximum.
 *
 * @author Jean Paul Barddal (jean.barddal@ppgia.pucpr.br)
 * @version 1.0
 */
public class IrrelevantFeatureAppenderStream extends AbstractOptionHandler implements InstanceStream {

    public ClassOption streamOption                    = new ClassOption("stream", 's',
            "Stream to imbalance.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public IntOption numNumericFeaturesOption          = new IntOption("numNumericFeatures", 'n',
            "Number of numeric features to be appended.", 0, 0, 1000);

    public IntOption numCategoricalFeaturesOption      = new IntOption("numCategoricalFeatures", 'c',
            "Number of categorical features to be appended.", 0, 0, 1000);

    public IntOption numValuesCategoricalFeatureOption = new IntOption("numValuesCategoricalFeature", 'v',
            "Number of values for each categorical feature.", 2, 1, 1000);

    public IntOption instanceRandomSeedOption          = new IntOption("instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    /**
     * The original stream.
     */
    protected ExampleStream originalStream = null;

    /**
     * The header with the new features appended.
     */
    protected InstancesHeader newHeader = null;

    /**
     * A pseudo-random number generator.
     */
    protected Random random = null;

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.originalStream = (ExampleStream) getPreparedClassOption(streamOption);
        this.random = new Random(instanceRandomSeedOption.getValue());
        buildHeader();
    }

    @Override
    public InstancesHeader getHeader() {
        return newHeader;
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
        Example<Instance> original = originalStream.nextInstance();

        // copies the original values
        double values[] = new double[this.newHeader.numAttributes()];
        int ix = 0;
        for(int i = 0; i < original.getData().dataset().numAttributes(); i++){
            if(original.getData().dataset().classIndex() != i) {
                values[ix] = original.getData().value(i);
                ix++;
            }
        }

        // appends the new values
        while(ix < values.length - 1){
            Attribute att = this.newHeader.attribute(ix);
            if(att.isNumeric()) values[ix] = this.random.nextDouble();
            else values[ix] = this.random.nextInt(numValuesCategoricalFeatureOption.getValue());
            ix++;
        }

        //copies the class value
        if(original.getData().classIndex() != -1) {
            values[values.length - 1] = original.getData().classValue();
        }

        // instantiates and returns the actual instance
        Instance instnc = new DenseInstance(1.0, values);
        instnc.setDataset(this.newHeader);

        return new InstanceExample(instnc);
    }

    @Override
    public boolean isRestartable() {
        return originalStream.isRestartable();
    }

    @Override
    public void restart() {
        originalStream.restart();
        this.random = new Random(instanceRandomSeedOption.getValue());
        this.buildHeader();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {}

    /**
     * Constructs the new header according to:
     * 1- The original header
     * 2- The number of numeric and categorical features given by the user.
     */
    private void buildHeader(){
        ArrayList<Attribute> attributes = new ArrayList<>();

        // copies the original attributes
        InstancesHeader originalHeader = originalStream.getHeader();
        for(int i = 0; i < originalHeader.numAttributes(); i++){
            Attribute att = originalHeader.attribute(i);
            if(att != originalHeader.classAttribute()) {
                attributes.add(att);
            }
        }

        // appends the new numeric features
        for(int i = 0; i < numNumericFeaturesOption.getValue(); i++){
            Attribute att = new Attribute(("irrelNum" + i));
            attributes.add(att);
        }

        // creates the values for categorical features
        ArrayList<String> catVals = new ArrayList<>();
        for(int i = 0; i < numValuesCategoricalFeatureOption.getValue(); i++) catVals.add("v" + i);

        // appends the new categorical features
        for(int i = 0; i < numCategoricalFeaturesOption.getValue(); i++){
            Attribute att = new Attribute(("irrelCat" + i), catVals);
            attributes.add(att);
        }

        // appends the class attribute
        if(originalHeader.classIndex() != -1){
            attributes.add(originalHeader.attribute(originalHeader.classIndex()));
        }

        // builds the new header
        Instances format = new Instances(getCLICreationString(InstanceStream.class), attributes, 0);
        format.setClassIndex(attributes.size() - 1);
        newHeader = new InstancesHeader(format);
    }

}
