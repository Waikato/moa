package moa.streams.generators;

/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.*;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

import java.util.ArrayList;
import java.util.Random;

/**
 * Text generator that simulates sentiment analysis on tweets.
 */
public class TextGenerator extends AbstractOptionHandler implements InstanceStream {

    private static final long serialVersionUID = 3028905554604259131L;

    public IntOption numAttsOption = new IntOption("numAtts", 'a',
            "The number of attributes to generate.", 1000, 0, Integer.MAX_VALUE);

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    protected int[] wordTwitterGenerator;
    protected double[] freqTwitterGenerator;
    protected double[] sumFreqTwitterGenerator;
    protected int[] classTwitterGenerator;

    protected int sizeTable;
    protected double probPositive = 0.1;
    protected double probNegative = 0.1;
    protected double zipfExponent = 1.5;
    protected double lengthTweet = 15;

    protected int countTweets = 0;

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
        int[] votes;
        double[] attVals;
        attVals = new double[this.numAttsOption.getValue() + 1];

        do {
            int length = (int) (lengthTweet * (1.0 + this.instanceRandom.nextGaussian()));
            if (length < 1) length = 1;
            votes = new int[3];
            for (int j = 0; j < length; j++) {
                double rand = this.instanceRandom.nextDouble();
                //binary search
                int i = 0;
                int min = 0;
                int max = sizeTable - 1;
                int mid;
                do {
                    mid = (min + max) / 2;
                    if (rand > this.sumFreqTwitterGenerator[mid]) {
                        min = mid + 1;
                    } else {
                        max = mid - 1;
                    }
                } while ((this.sumFreqTwitterGenerator[mid] != rand) && (min <= max));

                attVals[this.wordTwitterGenerator[mid]] = 1;
                votes[this.classTwitterGenerator[mid]]++;

            }
        } while (votes[1] == votes[2]);

        Instance inst = new SparseInstance(1.0, attVals);
        inst.setDataset(getHeader());
        inst.setClassValue((votes[1] > votes[2]) ? 0 : 1);
        this.countTweets++;
        return new InstanceExample(inst);
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void restart() {

        this.sizeTable = this.numAttsOption.getValue();

        //Prepare table of words to generate tweets
        this.wordTwitterGenerator = new int[sizeTable];
        this.freqTwitterGenerator = new double[sizeTable];
        this.sumFreqTwitterGenerator = new double[sizeTable];
        this.classTwitterGenerator = new int[sizeTable];

        this.countTweets = 0;

        double sum = 0;
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
        for (int i = 0; i < this.sizeTable; i++) {
            this.wordTwitterGenerator[i] = i + 1;
            this.freqTwitterGenerator[i] = 1.0 / Math.pow(i + 1, zipfExponent);
            sum += this.freqTwitterGenerator[i];
            this.sumFreqTwitterGenerator[i] = sum;
            double rand = this.instanceRandom.nextDouble();
            this.classTwitterGenerator[i] = (rand < probPositive ? 1 : (rand < probNegative + probPositive ? 2 : 0));
        }
        for (int i = 0; i < this.sizeTable; i++) {
            this.freqTwitterGenerator[i] /= sum;
            this.sumFreqTwitterGenerator[i] /= sum;
        }

    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        generateHeader();
        restart();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }
    private void generateHeader() {
        ArrayList<String>  classLabels = new ArrayList();
        for (int i = 0; i < 2; i++) {
            classLabels.add("class" + (i + 1));
        }
        ArrayList<Attribute> attributes = new ArrayList();
        for (int i = 0; i < this.numAttsOption.getValue(); i++) {
            attributes.add(new Attribute("att" + (i + 1), classLabels));
        }
        attributes.add(new Attribute("class", classLabels));
        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
    }


    public void changePolarity(int numberWords) {
        for (int i = 0; i < numberWords; ) {
            int randWord = this.instanceRandom.nextInt(this.sizeTable);
            int polarity = this.classTwitterGenerator[randWord];
            if (polarity == 1) {
                this.classTwitterGenerator[i] = 2;
                i++;
            }
            if (polarity == 2) {
                this.classTwitterGenerator[i] = 1;
                i++;
            }
        }
    }

    public void changeFreqWords(int numberWords) {
        for (int i = 0; i < numberWords; i++) {
            int randWordTo = this.instanceRandom.nextInt(this.sizeTable);
            int randWordFrom = this.instanceRandom.nextInt(this.sizeTable);
            this.wordTwitterGenerator[randWordTo] = randWordFrom;
            this.wordTwitterGenerator[randWordFrom] = randWordTo;
        }
    }


}
