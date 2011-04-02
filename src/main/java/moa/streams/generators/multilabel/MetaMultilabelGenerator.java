/*
 *    MetaMultilabelGenerator.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jmr30@cs.waikato.ac.nz)
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
package moa.streams.generators.multilabel;

import java.util.*;

import moa.core.InstancesHeader;
import moa.core.MultilabelInstancesHeader;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.FloatOption;
import moa.options.ClassOption;
import moa.options.IntOption;
import moa.streams.*;
import moa.tasks.TaskMonitor;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.Utils;

/**
 * Stream generator for multilabel data.
 *
 * @author Jesse Read (jmr30@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class MetaMultilabelGenerator extends AbstractOptionHandler implements InstanceStream {

    private static final long serialVersionUID = 1L;

    public ClassOption binaryGeneratorOption = new ClassOption(
            "binaryGenerator", 's', "Binary Generator (use this option to specify the number of attributes, but specify two classes only).", InstanceStream.class, "generators.RandomTreeGenerator");

    public IntOption metaRandomSeedOption = new IntOption(
            "metaRandomSeed", 'm', "Random seed (for the meta process).", 1);

    public IntOption numLabelsOption = new IntOption(
            "numLabels", 'c', "Number of labels.", 1);

    public IntOption skewOption = new IntOption(
            "skew", 'k', "Skewed label distribution: 1 (default) = yes; 0 = no (relatively uniform).", 1, 0, 1);

    public FloatOption labelCardinalityOption = new FloatOption(
            "labelCardinality", 'z', "Target label cardinality of resulting set", 1.5, 0.0, Integer.MAX_VALUE);

    protected MultilabelInstancesHeader m_MultilabelInstancesHeader = null;

    protected InstanceStream m_BinaryGenerator = null;

    protected Instances multilabelStreamTemplate = null;

    protected Random m_MetaRandom = null;

    protected int m_N = 0, m_A = 0;

    protected double m_Z = 0.0;

    protected double skew[] = null, skew_n[] = null;

    protected double matrix[][] = null;

    protected ArrayList m_FeatureEffects[] = null;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.restart();
    }

    @Override
    public void restart() {

        // Extract option 'c' (number of classes(labels))
        this.m_N = numLabelsOption.getValue();

        // Binary generator
        this.m_BinaryGenerator = (InstanceStream) getPreparedClassOption(this.binaryGeneratorOption);
        this.m_BinaryGenerator.restart();

        // Extract number of attributes (minus class-attribute)
        this.m_A = this.m_BinaryGenerator.getHeader().numAttributes() - 1;

        // Random seed
        this.m_MetaRandom = new Random(this.metaRandomSeedOption.getValue());

        // Setup queue system (so that generated binary instances aren't 'wasted')
        this.queue = new LinkedList[2];
        for (int i = 0; i < this.queue.length; i++) {
            this.queue[i] = new LinkedList<Instance>();
        }

        // Generate the multi-label header
        this.m_MultilabelInstancesHeader = generateMultilabelHeader(this.m_BinaryGenerator.getHeader());

        // Determine Z : label cardinality as a percentage of |L| (m_N)
        m_Z = labelCardinalityOption.getValue();
        double z = m_Z;

        // Chceck that the label sets we generate fit the label cardinality we specified
        while (true) {
            // Create the label skew
            this.skew = fillSkew(m_MetaRandom, z);
            // Create a normalised version of the skew (for wwhen we choose at least one label)
            this.skew_n = Arrays.copyOf(skew, skew.length);
            Utils.normalize(this.skew_n);
            // Create a matrix from the label skew
            this.matrix = fillMatrix(skew, m_Z / (double) m_N, m_MetaRandom);
            double total = 0.0;
            for (int i = 0; i < 10000; i++) {
                total += (generateSet(discreteRandomIndex(this.skew_n))).size();
            }
            total /= 10000.0;
            if (total - m_Z < -0.1) {
                z += 0.1;
            } else if (total - m_Z > 0.1) {
                z -= 0.1;
            } else {
                break;
            }
        }

        // Create the feature-label mappings
        m_FeatureEffects = getTopCombinations(m_N * 2);

    }

    /**
    GenerateMultilabelHeader.
     */
    protected MultilabelInstancesHeader generateMultilabelHeader(Instances si) {
        Instances mi = new Instances(si, 0, 0);
        mi.setClassIndex(-1);
        mi.deleteAttributeAt(mi.numAttributes() - 1);
        FastVector bfv = new FastVector();
        bfv.addElement("0");
        bfv.addElement("1");
        for (int i = 0; i < this.m_N; i++) {
            mi.insertAttributeAt(new Attribute("class" + i, bfv), i);
        }
        this.multilabelStreamTemplate = mi;
        this.multilabelStreamTemplate.setRelationName("SYN_Z" + this.labelCardinalityOption.getValue() + "L" + this.m_N + "X" + m_A + "S" + metaRandomSeedOption.getValue() + ": -C " + this.m_N);
        this.multilabelStreamTemplate.setClassIndex(this.m_N);
        return new MultilabelInstancesHeader(multilabelStreamTemplate, m_N);
    }

    /**
    GenSkew.
    Generate a label skew (given desired lcard z)
    @param	z		desired label cardinality
    @param	r		random generator
     */
    private double[] fillSkew(Random r, double z) {
        double d[] = new double[m_N];
        for (int i = 0; i < m_N; i++) {
            if (skewOption.getValue() >= 1) {
                d[i] = m_MetaRandom.nextDouble();
            } else {
                d[i] = 1.0;
            }
        }
        Utils.normalize(d, Utils.sum(d) / z);
        for (int i = 0; i < m_N; i++) {
            if (Double.isNaN(d[i])) {
                d[i] = 0.01;
            }
        }
        return d;
    }

    /**
     * GetNextWithBinary.
     * Get the next instance with binary class i
     * @param	i	the class to generate (0,1)
     */
    LinkedList<Instance> queue[] = null;

    private Instance getNextWithBinary(int i) {
        int lim = 1000;
        if (queue[i].size() <= 0) {
            int c = -1;
            while (lim-- > 0) {
                Instance tinst = this.m_BinaryGenerator.nextInstance();
                //System.err.println("next binary : "+tinst);
                c = (int) Math.round(tinst.classValue());
                if (i == c) {
                    return tinst;
                } else if (queue[c].size() < 100) {
                    queue[c].add(tinst);
                }
            }
            System.err.println("[Overflow] The binary stream is too skewed, could not get an example of class " + i + "");
            System.exit(1);
            return null;
        } else {
            return queue[i].remove();
        }
    }

    /**
     * LabelCorrelation.
     * @param	lbls	existing labels (indices) in the set
     * @return	a random label (index) to be associated with these labels (-1 if none)
     */
    private int labelCorrelation(ArrayList<Integer> lbls) {
        double r[] = new double[m_N];
        Arrays.fill(r, 1.0);
        for (int l : lbls) {
            //get row
            for (int j = 0; j < matrix[l].length; j++) {
                // *= P(j|l) (probability of label 'j', given that label 'l' is in the set
                r[j] = (j == l) ? 0.0 : r[j] * matrix[j][l];
            }
        }
        return discreteRandomIndex(r);
    }

    /**
     * GenerateML.
     * Generates a multi-label example.
     */
    @Override
    public Instance nextInstance() {

        try {
            return generateMLInstance(generateSet(discreteRandomIndex(this.skew_n)));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    private ArrayList generateSet(int l) {
        ArrayList<Integer> lbls = new ArrayList<Integer>();
        while (l >= 0) {
            lbls.add(l);
            l = labelCorrelation(lbls);
        }
        return lbls;
    }

    /**
     * GenerateMLInstance.
     */
    private Instance generateMLInstance(ArrayList<Integer> lbls) throws Exception {

        // create a multi-label instance   :
        Instance ml_x = new SparseInstance(this.multilabelStreamTemplate.numAttributes());
        ml_x.setDataset(this.multilabelStreamTemplate);

        // set classes
        for (int i = 0; i < m_N; i++) {
            ml_x.setValue(i, 0.0);
        }
        for (int i = 0; i < lbls.size(); i++) {
            ml_x.setValue(lbls.get(i), 1.0);
        }

        // generate binary instances
        Instance binary0 = getNextWithBinary(0);
        Instance binary1 = getNextWithBinary(1);

        // Loop through each feature attribute @warning: assumes class is last index
        for (int a = 0; a < m_A; a++) {

            // The combination is present: use a positive value
            if (lbls.containsAll(m_FeatureEffects[a % m_FeatureEffects.length])) {
                ml_x.setValue(m_N + a, binary1.value(a));
            } // The combination is absent: use a negative value
            else {
                ml_x.setValue(m_N + a, binary0.value(a));
            }
        }

        return ml_x;

    }

    /**
    DiscreteRandomIndex.
    Pick a random index i of p, based on the weight of the doubles each p[i] contains
    @note: expecting data to be normalised first
     */
    private int discreteRandomIndex(double p[]) {

        double r = m_MetaRandom.nextDouble();

        if (Utils.sum(p) <= r || Double.isNaN(Utils.sum(p))) {
            return -1; //m_MetaRandom.nextInt(p.length);
        }
        int i = 0;
        double sum = 0.0;
        while (r > sum) {
            // won't be selecting anything
            if (i >= p.length) {
                return -1;
            }
            sum += p[i++];
        }
        //System.out.println("i="+i);
        return i - 1;
    }

    protected static double genE(int i, double L) {
        return L * Math.pow(Math.E, -L * i);
    }

    /**
     * genMatrix.
     *  P(i) = matrix[i][i]
     *  P(i|j) = matrix[i][j]
     *	@param	m		the matrix with skew stored along the diagonal
     *	@param	z		goal label cardinality
     *	@param	r		random seed
     */
    protected double[][] fillMatrix(double skew[], double Z, Random r) {

        this.matrix = new double[skew.length][skew.length];

        //System.out.println("skew "+Arrays.toString(skew));

        for (int i = 0; i < skew.length; i++) {
            matrix[i][i] = Utils.roundDouble(skew[i], 3);
        }

        for (int i = 0; i < matrix.length; i++) {
            for (int j = i + 1; j < matrix[i].length; j++) {
                // label-dependence factors
                if (r.nextDouble() <= (Z * 2.0)) {
                    matrix[i][j] = randFromRange(min(P(i), P(j)), max(P(i), P(j)));
                    matrix[j][i] = (matrix[i][j] * matrix[i][i]) / matrix[j][j]; // Bayes Rule
                } // label-exclusivity factors
                else {
                    matrix[i][j] = min(P(i), P(j));
                    matrix[j][i] = (matrix[i][j] * matrix[j][j]) / matrix[i][i]; // Bayes Rule
                }
                // this is just rounding
                matrix[i][j] = Utils.roundDouble(matrix[i][j], 3);
                matrix[j][i] = Utils.roundDouble(matrix[j][i], 3);
            }
        }

        return matrix;
    }

    protected double randFromRange(double min, double max) {
        return min + genE(m_MetaRandom.nextInt(5), (max - min));
    }

    // P(i)
    protected double P(int i) {
        return matrix[i][i];
    }

    // P(i|j)
    protected double P(int i, int j) {
        return matrix[i][j];
    }

    // the highest possible prob. of P(A|B) given A and B
    protected double max(double A, double B) {
        return Math.min(1.0, (B / A));
    }

    // the lowest possible prob. of P(A|B) given A and B
    protected double min(double A, double B) {
        return Math.max(0.0, (-1.0 + A + B));
    }

    /**
     * GetTopCombinations.
     * Return the top n occurring combinations (we just measure the 10000 for this)
     */
    private ArrayList[] getTopCombinations(int n) {

        HashMap<String, Integer> top = new HashMap<String, Integer>();

        for (int i = 0; i < 10000; i++) {
            String s = arrayToString(generateSet(discreteRandomIndex(this.skew_n)), m_N);
            top.put(s, top.get(s) != null ? top.get(s) + 1 : 1);
        }

        HashMap<String, Integer> rating = getAsReverseSortedHashMap(top);

        ArrayList al[] = new ArrayList[rating.size()];
        int i = 0;
        for (String s : rating.keySet()) {
            al[i++] = stringToArray(s);
        }
        return al;
    }

    // auxilliary functions follow
    private static HashMap<String, Integer> getAsReverseSortedHashMap(HashMap<String, Integer> c) {

        Map<String, Integer> tempMap = new HashMap<String, Integer>();
        for (String wsState : c.keySet()) {
            tempMap.put(wsState, c.get(wsState));
        }

        List<String> mapKeys = new ArrayList<String>(tempMap.keySet());
        List<Integer> mapValues = new ArrayList<Integer>(tempMap.values());
        HashMap<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        TreeSet<Integer> sortedSet = new TreeSet<Integer>(mapValues);
        Object[] sortedArray = sortedSet.toArray();
        int size = sortedArray.length;
        for (int i = 0; i < size; i++) {
            sortedMap.put(mapKeys.get(mapValues.indexOf(sortedArray[size - 1 - i])), (Integer) sortedArray[size - 1 - i]);
        }
        return sortedMap;
    }

    private static ArrayList stringToArray(String s) {
        ArrayList al = new ArrayList();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '1') {
                al.add(i);
            }
        }
        return al;
    }

    private static String arrayToString(ArrayList<Integer> lbls, int N) {
        StringBuilder sb = new StringBuilder(N);
        for (int i = 0; i < N; i++) {
            sb.append('0');
        }
        for (int l : lbls) {
            sb.setCharAt(l, '1');
        }
        return sb.toString();
    }

    @Override
    public InstancesHeader getHeader() {
        return m_MultilabelInstancesHeader;
    }

    @Override
    public String getPurposeString() {
        return "Generates a multi-label stream using a binary generator.";
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
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
