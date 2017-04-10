/*
 *    FYGenerator.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Lanqin Yuan (fyempathy@gmail.com)
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

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.StringOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import moa.core.FastVector;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import moa.core.InstanceExample;

import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.AbstractOptionHandler;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import moa.recommender.rc.utils.Pair;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

/**
 * Conditional Generator
 * Generates a problem of predicting a class
 * A class is set based on whether relevant features are in accepted ranges/values
 * Includes option to add irrelevant attributes
 *
 * Should probably not use while loops but meh
 *
 * @author Lanqin Yuan (fyempathy@gmail.com)
 * @version 1
 */
public class ConditionalGenerator extends AbstractOptionHandler implements InstanceStream
{
    /**
     * concept data structure
     * contains class data structures
     *
     */
    protected class StreamConcept
    {
        ClassificationClass[] classArray;
        int[] relevantNomAttributeIndices;
        int[] relevantNumAttributeIndices;
        int[] irrelevantNomAttributeIndices;
        int[] irrelevantNumAttributeIndices;

        boolean[] relevantAttribute;
    }

    /**
     * class data structure
     */
    protected class ClassificationClass
    {
        int relevantNum  = 0;
        int relevantNom  = 0;

        double[][] numericDivisionsRange; // Divisions of the numeric attributes
        boolean numericStartDivBool; // initial boolean starting value for first range. true = first accepted, false = first non-accepted
        boolean[][] nominalValues; // boolean of accepted values for nominal attributes.

        public boolean evaluateRange(double d,int numericAtt)
        {
            int r = 0; // which range
            boolean bool = this.numericStartDivBool;
            for(int i = 0; i < numericDivisionsRange[numericAtt].length;i++)
            {
                if(i == numericDivisionsRange[numericAtt].length -1 ) // last division
                {
                    if(numericDivisionsRange[numericAtt][i] > d && bool || numericDivisionsRange[numericAtt][i] < d && !bool)
                    {
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }

                if(numericDivisionsRange[numericAtt][i] > d && bool)
                {
                    return true;
                }

                bool = !bool;

            }
            return false;
        }
    }

    @Override
    public String getPurposeString()
    {
        return "Generates a problem of classifying the class based on attributes.";
    }

    private static final long serialVersionUID = 1L;

    // alphabet array for nominal values
    protected static final String[] ALPHA = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};


    public IntOption seedOption = new IntOption("instanceRandomSeed", 's',"Seed for random generation of instances.", 1);
    public IntOption relevantNumericOption = new IntOption("relevantNum", 'u',"number of relevant numeric features.", 5,0,100);
    public IntOption relevantNominalOption = new IntOption("relevantNom", 'o',"number of irrelevant nominal features.", 5,0,100);
    public IntOption irrelevantNumericOption = new IntOption("irrelevantNum", 'h',"number of irrelevant numeric features.", 5,0,100);
    public IntOption irrelevantNominalOption = new IntOption("irrelevantNom", 'j',"number of irrelevant nominal features.", 5,0,100);
    public IntOption classOption = new IntOption("classNum", 'c',"number of classes.", 2,2,100);
    public IntOption nomOption = new IntOption("varNum", 'v',"max number of values a nominal attribute can have.", 2,2,26);
    public IntOption numOption = new IntOption("divNum", 'd',"max number of division ranges a numeric attribute can have.", 1,1,50);

    public IntOption noisePercentageOption = new IntOption("noisePercentage", 'n', "Percentage of noise to add to the data.", 0, 0, 100);

    public FlagOption driftOption = new FlagOption("drift", 'f', "Whether features drift in their positions. Drift is abrupt");
    public IntOption driftIntervalOption = new IntOption("driftInterval", 'i',"number of classes.", 100000);
    public StringOption outputNameOption = new StringOption("outputName",'t',"filename for output of generator dump","");

    public BufferedWriter bw;

    protected InstancesHeader streamHeader;
    protected Random instanceRandom;

    protected boolean[] attributeTypeArray; // true = numeric, false = nominal

    protected double[] numAttFactor; // the factor to multipy each numeric attribute (so its not all between 0 and 1)

    protected int totalAttributes;
    protected StreamConcept currentConcept;
    // protected ArrayList<StreamConcept> conceptList = new ArrayList<StreamConcept>();

    protected int[] tempRelevantNomArray = null;
    protected double[] tempRelevantNumArray = null;

    protected int driftInterval = 0;




    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository)
    {
        restart();
        // generate header
        FastVector attributes = new FastVector();
        FastVector classLabels = new FastVector();

        int nomAttsCount = relevantNominalOption.getValue() + irrelevantNominalOption.getValue();
        int numAttsCount = relevantNumericOption.getValue() + irrelevantNumericOption.getValue();

        numAttFactor = new double[numAttsCount];
        totalAttributes = numAttsCount + nomAttsCount;
        attributeTypeArray = new boolean[totalAttributes];

        // place numeric attributes in random columns
        for(int i = 0; i < numAttsCount;i++)
        {
            int randomIndex = instanceRandom.nextInt(totalAttributes);
            while (attributeTypeArray[randomIndex] == true)
            {
                randomIndex = instanceRandom.nextInt(totalAttributes);
            }
            attributeTypeArray[randomIndex] = true;
        }

        // all possible nominal values
        FastVector possibleValues = new FastVector();

        for(int n = 0; n < nomOption.getValue();n++)
        {
            possibleValues.addElement(ALPHA[n]);
        }

        //int nominalAttTrack = 0; // variable used to track which nominal attribute we currently at
        int numAttTrack = 0; // variable used to track which numeric attribute we currently at
        for (int i = 0; i < totalAttributes; i++)
        {
            String s;
            FastVector nomLabels = new FastVector();
            if(attributeTypeArray[i])
            {
                // if the attribute is numeric
                s = "num att" + (i + 1);
                attributes.addElement(new Attribute(s));
                numAttFactor[numAttTrack] = instanceRandom.nextDouble() * 100; // set factor as random
                numAttTrack++; // increment tracking
            }
            else
            {
                // nominal
                s = "nom att" + (i + 1);
                attributes.addElement(new Attribute(s, possibleValues));
                //nominalAttTrack++; // increment tracking
            }
        }



        // add class values into fastvector
        for(int i = 0; i < classOption.getValue();i++)
        {
            classLabels.add(Integer.toString(i));
        }
        // add class attribute
        attributes.addElement(new Attribute("class", classLabels));
        this.streamHeader = new InstancesHeader(new Instances(getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);

        currentConcept = generateStreamConcept();

        if(driftOption.isSet())
        {
            driftInterval = driftIntervalOption.getValue();
        }
    }


    @Override
    public InstanceExample nextInstance()
    {
        InstancesHeader header = getHeader();
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setDataset(header);

        int classSelected = instanceRandom.nextInt(classOption.getValue());

        double noisePercent = (double)noisePercentageOption.getValue() / 100;

        double noiseGen = instanceRandom.nextDouble();


        // clear global variables, these two variables will be set in generateInstanceOfClass @TODO probably bad practice
        tempRelevantNomArray = null;
        tempRelevantNumArray = null;
        generateInstanceOfClass(classSelected,currentConcept); // @TODO should really use a struct to return two values

        int nomRelevantIndex = 0;
        int numRelevantIndex = 0;

        int numAttTrack = 0;



        for(int i = 0; i < totalAttributes; i++)
        {
            if(attributeTypeArray[i])
            {
                // numeric
                if(currentConcept.relevantAttribute[i] && numRelevantIndex != -1 && noiseGen > noisePercent)
                {
                    // attribute is relevant
                    inst.setValue(i,tempRelevantNumArray[numRelevantIndex] * numAttFactor[numAttTrack]);
                    numRelevantIndex++;
                    // generate random stuff for other possibly relevant attributes
                    if(numRelevantIndex == tempRelevantNumArray.length)
                        numRelevantIndex = -1;
                }
                else
                {
                    inst.setValue(i,instanceRandom.nextDouble() * numAttFactor[numAttTrack]);
                }
                numAttTrack++;
            }
            else
            {
                // nominal
                if(currentConcept.relevantAttribute[i] && nomRelevantIndex != -1 && noiseGen > noisePercent)
                {
                    // attribute is relevant
                    inst.setValue(i,tempRelevantNomArray[nomRelevantIndex]);
                    nomRelevantIndex++;
                    // generate random stuff for other possibly relevant attributes
                    if(nomRelevantIndex == tempRelevantNomArray.length)
                        nomRelevantIndex = -1;
                }
                else
                {
                    inst.setValue(i,instanceRandom.nextInt(nomOption.getValue()));
                }
            }
        }
        inst.setClassValue(classSelected);

        if(driftOption.isSet())
        {
            driftInterval--;
            if(driftInterval <= 0)
            {
                currentConcept = generateStreamConcept(); // drift concept
                driftInterval = driftIntervalOption.getValue();
            }
        }
        return new InstanceExample(inst);
    }


    /**
     * generates a class in a concept and puts the values into the arrays
     * @param c class
     * @param concept concept
     */
    protected void generateInstanceOfClass(int c, StreamConcept concept)
    {
        ClassificationClass classificationClass = concept.classArray[c];

        // assign nominal values
        tempRelevantNomArray = new int[classificationClass.relevantNom];
        for(int i = 0; i < classificationClass.relevantNom; i++)
        {
            int index = 0;
            do
            {
                index = instanceRandom.nextInt(nomOption.getValue());
            } while (!classificationClass.nominalValues[i][index]);

            tempRelevantNomArray[i] = index;
        }

        tempRelevantNumArray = new double[classificationClass.relevantNum];
        for(int i = 0; i < classificationClass.relevantNum; i++)
        {
            double d = instanceRandom.nextDouble();
            while (!classificationClass.evaluateRange(d,i))
            {
                d = instanceRandom.nextDouble();
                //System.out.println("derpderpderp");

            }

            tempRelevantNumArray[i] = d;
        }
    }



    protected StreamConcept generateStreamConcept()
    {
        StreamConcept concept = new StreamConcept();

        concept.relevantAttribute = new boolean[totalAttributes];
        concept.relevantNomAttributeIndices = new int[relevantNominalOption.getValue()];
        concept.relevantNumAttributeIndices = new int[relevantNumericOption.getValue()];
        concept.irrelevantNomAttributeIndices = new int[irrelevantNominalOption.getValue()];
        concept.irrelevantNumAttributeIndices = new int[irrelevantNumericOption.getValue()];
        concept.classArray = new ClassificationClass[classOption.getValue()];

        // set indices of relevant attributes;
        boolean[] used = new boolean[totalAttributes];
        int index = -1;
        for(int i = 0; i < relevantNominalOption.getValue();i++)
        {
            do
            {
                index = getRandomNomIndex();
            }
            while (used[index] == true);
            used[index] = true;
            concept.relevantAttribute[index] = true;
            concept.relevantNomAttributeIndices[i] = index;
        }
        for(int i = 0; i < relevantNumericOption.getValue();i++)
        {
            do
            {
                index = getRandomNumIndex();
            }
            while (used[index] == true);
            used[index] = true;
            concept.relevantAttribute[index] = true;
            concept.relevantNumAttributeIndices[i] = index;
        }
        // set irrelevant attribute indices
        for(int i = 0; i < irrelevantNominalOption.getValue();i++)
        {
            do
            {
                index = getRandomNomIndex();
            }
            while (used[index] == true);
            used[index] = true;
            concept.irrelevantNomAttributeIndices[i] = index;
        }

        for(int i = 0; i < irrelevantNumericOption.getValue();i++)
        {
            do
            {
                index = getRandomNumIndex();
            }
            while (used[index] == true);
            used[index] = true;
            concept.irrelevantNumAttributeIndices[i] = index;
        }

        // add in classes
        for(int i = 0; i < classOption.getValue();i++)
        {
            concept.classArray[i] = generateClass();
        }

        String fileName = outputNameOption.getValue();
        if (!fileName.equals(""))
        {
            try
            {
                if (bw == null) 
				{
                    File file = new File(fileName);

                    // if file doesnt exists, then create it
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
                }
                printAllRelevantAttributes(concept,bw);


                for (int i = 0; i < concept.classArray.length; i++)
                {
                    bw.write("######################################################" + System.lineSeparator());
                    bw.write("Class " + i + System.lineSeparator());
                    bw.write("######################################################" + System.lineSeparator());
                    printClassificaitonClass(concept.classArray[i], concept,bw);
                }
                bw.flush();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return concept;
    }

    /**
     * generates the constraints for a class
     * @return a class
     */
    protected ClassificationClass generateClass()
    {
        ClassificationClass c = new ClassificationClass();
        // generate divisions for the class
        c.relevantNum = instanceRandom.nextInt(relevantNumericOption.getValue()) + 1; // number of relevant numeric features for this given class
        c.relevantNom = instanceRandom.nextInt(relevantNumericOption.getValue()) + 1; // number of relevant nominal features for this given class

        c.numericDivisionsRange = new double[c.relevantNum][numOption.getValue()];
        c.nominalValues = new boolean[c.relevantNom][nomOption.getValue()];

        // generating numeric attributes
        for(int u = 0; u < c.relevantNum;u++)
        {
            int numOfDivs = instanceRandom.nextInt(numOption.getValue()) + 1; // number of divisions in the numeric attributes for this numeric attribute

            double[] temp = new double[numOfDivs];
            for (int i = 0; i < numOfDivs; i++)
            {
                double div = instanceRandom.nextDouble();
                temp[i] = div;
            }
            // sort divisions in acceding then add to class structure
            Arrays.sort(temp);
            for (int i = 0; i < numOfDivs;i++)
            {
                c.numericDivisionsRange[u][i] = temp[i];
            }
            // number of divisions can be less than maximum, unused divisions are left at 0;
        }

        // set initial boolean;
        c.numericStartDivBool = instanceRandom.nextBoolean();

        // generating nominal attributes
        for(int u = 0; u < c.relevantNom;u++)
        {
            // random number of accepted values
            int i = instanceRandom.nextInt(nomOption.getValue() - 1) + 1; // least 1 value is always rejected and at least 1 value always accepted

            // allocate i number of attributes to be associated with the class
            while( i > 0)
            {
                int z = instanceRandom.nextInt(nomOption.getValue());
                if(!c.nominalValues[u][z])
                {
                    c.nominalValues[u][z] = true;
                    i--;
                }

            }
        }
        return c;
    }

    protected int getRandomNomIndex()
    {
        int i = -1;
        do
        {
            i = instanceRandom.nextInt(totalAttributes);
        }
        while (attributeTypeArray[i] == true);
        return i;
    }

    protected int getRandomNumIndex()
    {
        int i = -1;
        do
        {
            i = instanceRandom.nextInt(totalAttributes);
        }while (attributeTypeArray[i] == false);
        return i;
    }

    protected void printClassificaitonClass(ClassificationClass c, StreamConcept concept, BufferedWriter bw)
    {

        try
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Number of relevant nominal attributes for this class: ");
            sb.append(c.relevantNom);
            sb.append(System.lineSeparator());
            sb.append("Indices: ");

            for (int i = 0; i < c.relevantNom; i++) {
                sb.append(concept.relevantNomAttributeIndices[i]);
                sb.append(" ");
            }
            sb.append(System.lineSeparator());

            sb.append("Number of relevant numeric attributes for this class: ");
            sb.append(c.relevantNum);
            sb.append(System.lineSeparator());
            sb.append("Indices: ");
            for (int i = 0; i < c.relevantNum; i++) {
                sb.append(concept.relevantNumAttributeIndices[i]);
                sb.append(" ");
            }
            sb.append(System.lineSeparator());


            int numAttTrack = 0;
            int nomAttTrack = 0;

            for (int i = 0; i < totalAttributes; i++) {
                String t = "";
                String r = "irrelevant";
                String s = "";
                if (!attributeTypeArray[i]) {
                    // nominal
                    t = "nominal";
                    // attribute is relevant
                    if (concept.relevantAttribute[i]) {
                        int index = -1;
                        // find the index of relevant attribute
                        for (int z = 0; z < c.relevantNom; z++) {
                            if (concept.relevantNomAttributeIndices[z] == i) {
                                index = z;
                                break;
                            }
                        }
                        if (index != -1) {
                            r = "relevant";
                            s += ("Accepted values:" + System.lineSeparator());
                            for (int b = 0; b < c.nominalValues[index].length; b++) {
                                if (c.nominalValues[index][b])
                                    s += ALPHA[b] + System.lineSeparator();
                            }
                        }

                    }
                } else {
                    // numeric
                    t = "numeric";
                    // attribute is relevant
                    if (concept.relevantAttribute[i]) {

                        int index = -1;
                        // find the index of relevant attribute
                        for (int z = 0; z < c.relevantNum; z++) {
                            if (concept.relevantNumAttributeIndices[z] == i) {
                                index = z;
                                break;
                            }
                        }

                        // attribute is actually relevant to this class, not just to some class
                        if (index != -1) {
                            r = "relevant";

                            s += "accepted ranges: " + System.lineSeparator();
                            boolean currentRangeBool = c.numericStartDivBool;
                            double previousDiv = 0;
                            for (int b = 0; b < c.numericDivisionsRange[index].length; b++) {
                                if (c.numericDivisionsRange[index][b] == 0)
                                    break;
                                if (currentRangeBool) {
                                    s += "accepted range " + previousDiv + " to " + c.numericDivisionsRange[index][b] + System.lineSeparator();
                                    currentRangeBool = false;
                                } else {
                                    s += "non accepted range " + previousDiv + " to " + c.numericDivisionsRange[index][b] + System.lineSeparator();
                                    currentRangeBool = true;
                                }
                                previousDiv = c.numericDivisionsRange[index][b];
                            }
                            // add last range
                            if (currentRangeBool) {
                                s += "accepted range " + previousDiv + " to " + 1 + System.lineSeparator();
                            } else {
                                s += "non accepted range " + previousDiv + " to " + 1 + System.lineSeparator();
                            }
                        }
                    }
                    numAttTrack++;
                }
                sb.append("Attribute ");
                sb.append(i);
                sb.append(": ");
                sb.append(r);
                sb.append(" ");
                sb.append(t);
                sb.append(System.lineSeparator());
                sb.append(s);
            }

            bw.write(sb.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    protected void printAllRelevantAttributes(StreamConcept c, BufferedWriter bw)
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            sb.append("At least " ) ;
            sb.append((irrelevantNominalOption.getValue() + irrelevantNumericOption.getValue()));
            sb.append(" irrelevant features");
            sb.append(System.lineSeparator());

            sb.append("At most " ) ;
            sb.append((relevantNominalOption.getValue() + relevantNumericOption.getValue()));
            sb.append(" relevant features");
            sb.append(System.lineSeparator());

            for (int i = 0; i < totalAttributes; i++)
            {
                String s = "irrelevant";
                if (c.relevantAttribute[i])
                    s = "relevant";

                String t = "nominal";
                if (attributeTypeArray[i])
                    t = "numeric";
               sb.append("Attribute " + i + ": " + s + " " + t + System.lineSeparator());

            }
            bw.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void restart()
    {
        this.instanceRandom = new Random(this.seedOption.getValue());
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
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
}
