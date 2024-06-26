package moa.classifiers.semisupervised.attributeSimilarity;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.DoubleVector;

import java.util.HashMap;
import java.util.Map;

/**
 * An observer that collects statistics for similarity computation of categorical attributes.
 * This observer observes the categorical attributes of one dataset.
 */
public abstract class AttributeSimilarityCalculator {

    /**
     * <p></p>Collection of statistics of one attribute, including:</p>
     * <ul>
     *     <li>ID: index of the attribute</li>
     *     <li>f_k: frequency of a value of an attribute</li>
     * </ul>
     */
    class AttributeStatistics extends Attribute {
        /** ID of the attribute */
        private int id;

        /** Frequency of the values of the attribute */
        private DoubleVector fk;

        /** The decorated attribute */
        private Attribute attribute;

        /**
         * Creates a new collection of statistics of an attribute
         * @param id ID of the attribute
         */
        AttributeStatistics(int id) {
            this.id = id;
            this.fk = new DoubleVector();
        }

        AttributeStatistics(Attribute attr, int id) {
            this.attribute = attr;
            this.id = id;
            this.fk = new DoubleVector();
        }

        /** Gets the ID of the attribute */
        int getId() { return this.id; }

        /** Gets the decorated attribute */
        Attribute getAttribute() { return this.attribute; }

        /**
         * Gets f_k(x) i.e. the number of times x is a value of the attribute of ID k
         * @param value the attribute value
         * @return the number of times x is a value of the attribute of ID k
         */
        int getFrequencyOfValue(int value) {
            return (int) this.fk.getValue(value);
        }

        /**
         * Updates the frequency of a value
         * @param value the value X_k
         * @param frequency the frequency
         */
        void updateFrequencyOfValue(int value, int frequency) {
            this.fk.addToValue((int)value, frequency);
        }
    }

    /** Size of the dataset (number of instances) */
    protected int N;

    /** Dimension of the dataset (number of attributes) */
    protected int d;

    /** Storing the statistics of each attribute */
    //private AttributeStatistics[] attrStats;
    protected Map<Attribute, AttributeStatistics> attributeStats;

    /** A small value to avoid division by 0 */
    protected static double SMALL_VALUE = 1e-5;

    /** Creates a new observer */
    public AttributeSimilarityCalculator() {
        this.N = this.d = 0;
        this.attributeStats = new HashMap<>();
    }

    /**
     * Creates a new observer with a predefined number of attributes
     * @param d number of attributes
     */
    public AttributeSimilarityCalculator(int d) {
        this.d = d;
        this.attributeStats = new HashMap<>();
    }

    /**
     * Returns the size of the dataset
     * @return the size of the dataset (number of instances)
     */
    public int getSize() { return this.N; }

    /**
     * Increases the number of instances seen so far
     * @param amount the amount to increase
     */
    public void increaseSize(int amount) { this.N += amount; }

    /**
     * Returns the dimension size
     * @return the dimension size (number of attributes)
     */
    public int getDimension() { return this.d; }

    /**
     * Specifies the dimension of the dataset
     * @param d the dimension
     */
    public void setDimension(int d) { this.d = d; }

    /**
     * Returns the number of values taken by A_k collected online i.e. n_k
     * @param attr the attribute A_k
     * @return number of values taken by A_k (n_k)
     */
    public int getNumberOfAttributes(Attribute attr) {
        if (attributeStats.containsKey(attr)) return attributeStats.get(attr).numValues();
        return 0;
    }

    /**
     * Gets the frequency of value x of attribute A_k i.e. f_k(x)
     * @param attr the attribute
     * @param value the value
     * @return the number of times x occurs as value of attribute A_k; 0 if attribute k has not been observed so far
     */
    public double getFrequencyOfValueByAttribute(Attribute attr, int value) {
        if (attributeStats.containsKey(attr)) return attributeStats.get(attr).getFrequencyOfValue(value);
        return 0;
    }

    /**
     * Gets the sample probability of attribute A_k to take the value x in the dataset
     * i.e. p_k(x) = f_k(x) / N
     * @param attr the attribute A_k
     * @param value the value x
     * @return the sample probability p_k(x)
     */
    public double getSampleProbabilityOfAttributeByValue(Attribute attr, int value) {
        return this.getFrequencyOfValueByAttribute(attr, value) / this.N;
    }

    /**
     * Gets another probability estimate of attribute A_k to take the value x in the dataset
     * i.e. p_k^2 = f_k(x) * [ f_k(x) - 1 ] / [ N * (N - 1) ]
     * @param attr the attribute A_k
     * @param value the value x
     * @return the sample probability p_k^2(x)
     */
    public double getProbabilityEstimateOfAttributeByValue(Attribute attr, int value) {
        double fX = getFrequencyOfValueByAttribute(attr, value);
        if (N == 1) return 0;
        return (fX * (fX - 1)) / (N * (N - 1));
    }

    /**
     * Updates the statistics of an attribute A_k, e.g. frequency of the value (f_k)
     * @param id ID of the attribute A_k
     * @param attr the attribute A_k
     * @param value the value of A_k
     */
    public void updateAttributeStatistics(int id, Attribute attr, int value) {
        if (!attributeStats.containsKey(attr)) {
            AttributeStatistics stat = new AttributeStatistics(attr, id);
            stat.updateFrequencyOfValue(value, 1);
            attributeStats.put(attr, stat);
        } else {
//            System.out.println("attributeStats.get(attr).updateFrequencyOfValue(value, 1);" + attr + " " + value);
            if(value >= 0)
                attributeStats.get(attr).updateFrequencyOfValue(value, 1);
            else
                System.out.println("if(value < 0)");
        }
    }

    /**
     * Computes the similarity of categorical attributes of two instances X and Y, denoted S(X, Y).
     * S(X, Y) = Sum of [w_k * S_k(X_k, Y_k)] for k from 1 to d,
     * X_k and Y_k are from A_k (attribute k of the dataset).
     *
     * Note that X and Y must come from the same dataset, contain the same set of attributes,
     * and numeric attributes will not be taken into account.
     * @param X instance X
     * @param Y instance Y
     * @return the similarity of categorical attributes of X and Y
     */
    public double computeSimilarityOfInstance(Instance X, Instance Y) {
        // for k from 1 to d
        double S = 0;
        for (int i = 0; i < X.numAttributes(); i++) {
            // sanity check
            if (!X.attribute(i).equals(Y.attribute(i))) continue; // if X and Y's attributes are not aligned
            Attribute Ak = X.attribute(i);
            if (Ak.isNumeric() || !attributeStats.containsKey(Ak) || i == X.classIndex()) continue;
            // computation
            double wk = computeWeightOfAttribute(Ak, X, Y);
            double Sk = computePerAttributeSimilarity(Ak, (int)X.value(Ak), (int)Y.value(Ak));
            S += (wk * Sk);
        }
        return S;
    }

    /**
     * Computes the per-attribute similarity S_k(X_k, Y_k) between two value X_k and Y_k
     * of the attribute A_k. X_k and Y_k must be from A_k.
     *
     * To be overriden by subclasses.
     * @param attr the attribute A_k
     * @param X_k the value of X_k
     * @param Y_k the value of Y_k
     * @return the per-attribute similarity S_k(X_k, Y_k)
     */
    public abstract double computePerAttributeSimilarity(Attribute attr, double X_k, double Y_k);

    /**
     * Computes the weight w_k of an attribute A_k. To be overriden by subclasses.
     * @param attr the attribute A_k
     * @return the weight w_k of A_k
     */
    public abstract double computeWeightOfAttribute(Attribute attr, Instance X, Instance Y);
}
