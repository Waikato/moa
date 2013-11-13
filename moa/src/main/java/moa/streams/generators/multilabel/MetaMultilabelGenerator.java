/*
 *    MetaMultilabelGenerator.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jesse@tsc.uc3m.es)
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
package moa.streams.generators.multilabel;

import java.util.*;
import moa.core.InstanceExample;
import moa.core.MultilabelInstancesHeader;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.SparseInstance;
import moa.core.FastVector;
import moa.core.Utils;

/**
 * Stream generator for multilabel data.
 *
 * @author Jesse Read ((jesse@tsc.uc3m.es))
 * @version $Revision: 7 $
 */
public class MetaMultilabelGenerator extends AbstractOptionHandler implements InstanceStream {

    private static final long serialVersionUID = 1L;

    public ClassOption binaryGeneratorOption = new ClassOption(
            "binaryGenerator", 's', "Binary Generator (specify the number of attributes here, but only two classes!).", InstanceStream.class, "generators.RandomTreeGenerator");

    public IntOption metaRandomSeedOption = new IntOption(
            "metaRandomSeed", 'm', "Random seed (for the meta process). Use two streams with the same seed and r > 0.0 in the second stream if you wish to introduce drift to the label dependencies without changing the underlying concept.", 1);

    public IntOption numLabelsOption = new IntOption(
            "numLabels", 'c', "Number of labels.", 10, 2, Integer.MAX_VALUE);

    public IntOption skewOption = new IntOption(
            "skew", 'k', "Skewed label distribution: 1 (default) = yes; 0 = no (relatively uniform) @NOTE: not currently implemented.", 1, 0, 1);

    public FloatOption labelCardinalityOption = new FloatOption(
            "labelCardinality", 'z', "Desired label cardinality (average number of labels per example).", 1.5, 0.0, Integer.MAX_VALUE);

    public FloatOption labelCardinalityVarOption = new FloatOption(
            "labelCardinalityVar", 'v', "Desired label cardinality variance (variance of z) @NOTE: not currently implemented.", 1.0, 0.0, Integer.MAX_VALUE);

    public FloatOption labelCardinalityRatioOption = new FloatOption(
            "labelDependency", 'u', "Specifies how much label dependency from 0 (total independence) to 1 (full dependence).", 0.25, 0.0, 1.0);

    public FloatOption labelDependencyChangeRatioOption = new FloatOption(
            "labelDependencyRatioChange", 'r', "Each label-pair dependency has a 'r' chance of being modified. Use this option on the second of two streams with the same random seed (-m) to introduce label-dependence drift.", 0.0, 0.0, 1.0);

    protected MultilabelInstancesHeader m_MultilabelInstancesHeader = null;

    protected InstanceStream m_BinaryGenerator = null;

    protected Instances multilabelStreamTemplate = null;

    protected Random m_MetaRandom = new Random();

    protected int m_L = 0, m_A = 0;

    protected double priors[] = null, priors_norm[] = null;

    protected double Conditional[][] = null;

    protected HashSet m_TopCombinations[] = null;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.restart();
    }

    @Override
    public void restart() {

        // The number of class labels L
        this.m_L = numLabelsOption.getValue();

        if (this.labelCardinalityOption.getValue() > m_L) {
            System.err.println("Error: Label cardinality (z) cannot be greater than the number of labels (c)!");
            System.exit(1);
        }

        // Initialise the chosen binary generator
        this.m_BinaryGenerator = (InstanceStream) getPreparedClassOption(this.binaryGeneratorOption);
        this.m_BinaryGenerator.restart();

        // The number of attributes A (not including class-attributes)
        this.m_A = this.m_BinaryGenerator.getHeader().numAttributes() - 1;

        // Random seed
        this.m_MetaRandom = new Random(this.metaRandomSeedOption.getValue());

        // Set up a queue system
        this.queue = new LinkedList[2];
        for (int i = 0; i < this.queue.length; i++) {
            this.queue[i] = new LinkedList<Instance>();
        }

        // Generate the multi-label header
        this.m_MultilabelInstancesHeader = generateMultilabelHeader(this.m_BinaryGenerator.getHeader());

        // Generate label prior distribution
        this.priors = generatePriors(m_MetaRandom, m_L, labelCardinalityOption.getValue(), (skewOption.getValue() >= 1));
        //printVector(this.priors);

        // Generate the matrix marking the label-dependencies
        boolean DependencyMatrix[][] = modifyDependencyMatrix(new boolean[m_L][m_L], labelCardinalityRatioOption.getValue(), m_MetaRandom);

        // Modify the dependency matrix (and the priors) if they are to change in this stream
        if (labelDependencyChangeRatioOption.getValue() > 0.0) {
            priors = modifyPriorVector(priors, labelDependencyChangeRatioOption.getValue(), m_MetaRandom, (skewOption.getValue() >= 1));
            modifyDependencyMatrix(DependencyMatrix, labelDependencyChangeRatioOption.getValue(), m_MetaRandom);
        }

        // Generate the conditional matrix, using this change matrix
        this.Conditional = generateConditional(priors, DependencyMatrix);
        //printMatrix(this.Conditional);

        // Make a normalised version of the priors
        this.priors_norm = Arrays.copyOf(priors, priors.length);
        Utils.normalize(this.priors_norm);

        // Create the feature-label mappings
        m_TopCombinations = getTopCombinations(m_A);
    }

    /**
     * GenerateMultilabelHeader.
     *
     * @param	si	single-label Instances
     */
    protected MultilabelInstancesHeader generateMultilabelHeader(Instances si) {
        Instances mi = new Instances(si, 0, 0);
        mi.setClassIndex(-1);
        mi.deleteAttributeAt(mi.numAttributes() - 1);
        FastVector bfv = new FastVector();
        bfv.addElement("0");
        bfv.addElement("1");
        for (int i = 0; i < this.m_L; i++) {
            mi.insertAttributeAt(new Attribute("class" + i, bfv), i);
        }
        this.multilabelStreamTemplate = mi;
        this.multilabelStreamTemplate.setRelationName("SYN_Z" + this.labelCardinalityOption.getValue() + "L" + this.m_L + "X" + m_A + "S" + metaRandomSeedOption.getValue() + ": -C " + this.m_L);
        this.multilabelStreamTemplate.setClassIndex(this.m_L);
        return new MultilabelInstancesHeader(multilabelStreamTemplate, m_L);
    }

    /**
     * Generate Priors. Generate the label priors.
     *
     * @param	L	number of labels
     * @param	z	desired label cardinality
     * @param	r	random number generator
     * @param	skew	whether to be very skewed or not (@NOTE not currently used)
     * @return	P	label prior distribution
     */
    private double[] generatePriors(Random r, int L, double z, boolean skew) {
        double P[] = new double[L];
        for (int i = 0; i < L; i++) {
            P[i] = r.nextDouble();
            //P[i] = 1.0; // @temp
        }
        // normalise to z
        do {
            double c = Utils.sum(P) / z;
            for (int i = 0; i < L; i++) {
                P[i] = Math.min(1.0, P[i] / c); // must be in [0,1]
            }
        } while (Utils.sum(P) < z);
        return P;
    }
    /**
     * GetNextWithBinary. Get the next instance with binary class i
     *
     * @param	i	the class to generate (0,1)
     */
    LinkedList<Instance> queue[] = null;

    private Instance getNextWithBinary(int i) {
        int lim = 1000;
        if (queue[i].size() <= 0) {
            int c = -1;
            while (lim-- > 0) {
                Instance tinst = this.m_BinaryGenerator.nextInstance().getData();
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
     * GenerateML. Generates a multi-label example.
     */
    @Override
    public InstanceExample nextInstance() {
        return new InstanceExample(generateMLInstance(generateSet()));
    }

    /**
     * Generate Set.
     *
     * @return	a label set Y
     */
    private HashSet generateSet() {

        int y[] = new int[m_L]; 			// [0,0,0]
        int k = samplePMF(priors_norm); 	// k = 1     // y[k] ~ p(k==1)
        y[k] = 1; 							// [0,1,0]
        ArrayList<Integer> indices = getShuffledListToLWithoutK(m_L, k);
        for (int j : indices) {
            //y[j] ~ p(j==1|y)
            y[j] = (joint(j, y) > m_MetaRandom.nextDouble()) ? 1 : 0;
        }
        return vector2set(y);
    }

    // P(y[] where y[k]==1)
    private double joint(int k, int y[]) {
        double p = 1.0; //priors[k];
        for (int j = 0; j < y.length; j++) {
            if (j != k && y[j] == 1) {
                p *= Conditional[k][j];
            }
        }
        return p;
    }

    /**
     * GenerateMLInstance.
     *
     * @param	Y	a set of label [indices]
     * @return a multit-labelled example
     */
    private Instance generateMLInstance(HashSet<Integer> Y) {

        // create a multi-label instance:
        Instance x_ml = new SparseInstance(this.multilabelStreamTemplate.numAttributes());
        x_ml.setDataset(this.multilabelStreamTemplate);

        // set classes
        for (int j = 0; j < m_L; j++) {
            x_ml.setValue(j, 0.0);
        }
        for (int l : Y) {
            x_ml.setValue(l, 1.0);
        }

        // generate binary instances
        Instance x_0 = getNextWithBinary(0);
        Instance x_1 = getNextWithBinary(1);

        // Loop through each feature attribute @warning: assumes class is last index
        for (int a = 0; a < m_A; a++) {

            // The combination is present: use a positive value
            if (Y.containsAll(m_TopCombinations[a])) {
                x_ml.setValue(m_L + a, x_1.value(a));
                //x_ml.setValue(m_L+a,1.0);
            } // The combination is absent: use a negative value
            else {
                x_ml.setValue(m_L + a, x_0.value(a));
                //x_ml.setValue(m_L+a,0.0);
            }
        }

        return x_ml;
    }

    /**
     * samplePMF.
     *
     * @param	p	a pmf
     * @return	an index i of p with probability p[i], and -1 with probability
     * 1.0-p[i]
     */
    private int samplePMF(double p[]) {

        double r = m_MetaRandom.nextDouble();

        double sum = 0.0;
        for (int i = 0; i < p.length; i++) {
            sum += p[i];
            if (r < sum) {
                return i;
            }
        }
        return -1;
    }

    /**
     * ModifyPriorVector. A certain number of values will be altered.
     *
     * @param	P[]	the prior distribution
     * @param	u	the probability of changing P[j]
     * @param	r	for random numbers
     * @param	skew	NOTE not currently used
     * @return	the modified P
     */
    protected double[] modifyPriorVector(double P[], double u, Random r, boolean skew) {
        for (int j = 0; j < P.length; j++) {
            if (r.nextDouble() < u) {
                P[j] = r.nextDouble();
            }
        }
        return P;
    }

    /**
     * ModifyDependencyMatrix. A certain number of values will be altered. @NOTE
     * a future improvement would be to detect cycles, since this may lead to
     * inconsistencies. However, due to the rarity of this occurring, and the
     * minimal problems it would cause (and considering that inconsistencies
     * also occr in real datasets) we don't implement this.
     *
     * @param	M[][]	a boolean matrix
     * @param	u	the probability of changing the relationship M[j][k]
     * @param	r	for random numbers
     * @return	the modified M
     */
    protected boolean[][] modifyDependencyMatrix(boolean M[][], double u, Random r) {
        //List<int[]> L = new ArrayList<int[]>();
        for (int j = 0; j < M.length; j++) {
            for (int k = j + 1; k < M[j].length; k++) {
                if (/*
                         * !hasCycle(L) &&
                         */r.nextDouble() <= u) {
                    M[j][k] ^= true;
                    //L.add(new int[]{j,k});
                }
            }
        }
        return M;
    }

    /**
     * GenerateConditional. Given the priors distribution and a matrix flagging
     * dependencies, generate a conditional distribution matrix Q; such that:
     * P(i) = Q[i][i] P(i|j) = Q[i][j]
     *
     * @param	P	prior distribution
     * @param	M	dependency matrix (where 1 == dependency)
     * @return	Q	conditional dependency matrix
     */
    protected double[][] generateConditional(double P[], boolean M[][]) {

        int L = P.length;

        double Q[][] = new double[L][L];

        // set priors
        for (int j = 0; j < L; j++) {
            Q[j][j] = P[j];
        }

        // create conditionals
        for (int j = 0; j < Q.length; j++) {
            for (int k = j + 1; k < Q[j].length; k++) {

                // dependence
                if (M[j][k]) {
                    // min = tending toward mutual exclusivity
                    // max = tending toward total co-occurence
                    // @NOTE it would also be an option to select in [min,max], but since
                    //       we are approximating the joint distribution, we can take
                    //       a stronger approach, and just take either min or max.
                    Q[j][k] = (m_MetaRandom.nextBoolean() ? min(P[j], P[k]) : max(P[j], P[k]));
                    Q[k][j] = (Q[j][k] * Q[j][j]) / Q[k][k]; // Bayes Rule
                } // independence
                else {
                    Q[j][k] = P[j];
                    Q[k][j] = (Q[j][k] * P[k]) / P[j]; // Bayes Rule
                }
            }
        }

        return Q;
    }

    /**
     * GetTopCombinations. Calculating the full joint probability distribution
     * is too complex. - sample from the approximate joint many times - record
     * the the n most commonly ocurring Y and their frequencies - create a map
     * based on these frequencies
     *
     * @param	n	the number of labelsets
     * @return	n labelsts
     */
    private HashSet[] getTopCombinations(int n) {

        final HashMap<HashSet, Integer> count = new HashMap<HashSet, Integer>();
        HashMap<HashSet, Integer> isets = new HashMap<HashSet, Integer>();

        int N = 100000;
        double lc = 0.0;
        for (int i = 0; i < N; i++) {
            HashSet Y = generateSet();
            lc += Y.size();
            count.put(Y, count.get(Y) != null ? count.get(Y) + 1 : 1);
        }
        lc = lc / N;

        // @TODO could generate closed frequent itemsets from 'count'

        List<HashSet> top_set = new ArrayList<HashSet>(count.keySet());
        // Sort the sets by their count
        Collections.sort(top_set, new Comparator<HashSet>() {

            @Override
            public int compare(HashSet Y1, HashSet Y2) {
                return count.get(Y2).compareTo(count.get(Y1));
            }
        });

        System.err.println("The most common labelsets (from which we will build the map) will likely be: ");
        HashSet map_set[] = new HashSet[n];
        double weights[] = new double[n];
        int idx = 0;
        for (HashSet Y : top_set) {
            System.err.println(" " + Y + " : " + (count.get(Y) * 100.0 / N) + "%");
            weights[idx++] = count.get(Y);
            if (idx == weights.length) {
                break;
            }
        }
        double sum = Utils.sum(weights);

        System.err.println("Estimated Label Cardinality:  " + lc + "\n\n");
        System.err.println("Estimated % Unique Labelsets: " + (count.size() * 100.0 / N) + "%\n\n");

        // normalize weights[]
        Utils.normalize(weights);

        // add sets to the map set, according to their weights
        for (int i = 0, k = 0; i < top_set.size() && k < map_set.length; i++) {   // i'th combination (pre)
            int num = (int) Math.round(Math.max(weights[i] * map_set.length, 1.0));	 // i'th weight
            for (int j = 0; j < num && k < map_set.length; j++) {
                map_set[k++] = top_set.get(i);
            }
        }

        // shuffle 
        Collections.shuffle(Arrays.asList(map_set));

        // return
        return map_set;
    }

    @Override
    public InstancesHeader getHeader() {
        return m_MultilabelInstancesHeader;
    }

    @Override
    public String getPurposeString() {
        return "Generates a multi-label stream based on a binary random generator.";
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
    }

    // ------- following are private utility functions -----------
    // convert set Y to an L-length vector y
    private int[] set2vector(HashSet<Integer> Y, int L) {
        int y[] = new int[L];
        for (int j : Y) {
            y[j] = 1;
        }
        return y;
    }
    // convert L-length vector y to set Y

    private HashSet<Integer> vector2set(int y[]) {
        HashSet<Integer> Y = new HashSet<Integer>();
        for (int j = 0; j < y.length; j++) {
            if (y[j] > 0) {
                Y.add(j);
            }
        }
        return Y;
    }

    // the highest possible prob. of P(A|B) given A and B
    private double max(double A, double B) {
        return Math.min(1.0, (B / A));
    }

    // the lowest possible prob. of P(A|B) given A and B
    private double min(double A, double B) {
        return Math.max(0.0, (-1.0 + A + B));
    }

    private ArrayList<Integer> getShuffledListToLWithoutK(int L, int k) {
        ArrayList<Integer> list = new ArrayList<Integer>(L - 1);
        for (int j = 0; j < L; j++) {
            if (j != k) {
                list.add(j);
            }
        }
        Collections.shuffle(list);
        return list;
    }

    // ------- following are private debugging functions -----------
    public static void main(String args[]) {
        // test routines
    }

    private void printMatrix(double M[][]) {
        System.out.println("--- MATRIX ---");
        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M[i].length; j++) {
                System.out.print(" " + Utils.doubleToString(M[i][j], 5, 3));
            }
            System.out.println("");
        }
    }

    private void printVector(double V[]) {
        System.out.println("--- VECTOR ---");
        for (int j = 0; j < V.length; j++) {
            System.out.print(" " + Utils.doubleToString(V[j], 5, 3));
        }
        System.out.println("");
    }
}
