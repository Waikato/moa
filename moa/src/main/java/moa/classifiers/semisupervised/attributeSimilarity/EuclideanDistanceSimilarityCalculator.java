package moa.classifiers.semisupervised.attributeSimilarity;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Computes the per-attribute similarity of categorical attributes with Euclidean distance,
 * i.e. to consider them as numeric attributes
 */
public class EuclideanDistanceSimilarityCalculator extends AttributeSimilarityCalculator {

    @Override
    public double computePerAttributeSimilarity(Attribute attr, double X_k, double Y_k) {
        // TODO NOT CORRECT !!! To fix!!!
        return Math.sqrt((X_k - Y_k) * (X_k - Y_k));
    }

    @Override
    public double computeWeightOfAttribute(Attribute attr, Instance X, Instance Y) {
        return 1;
    }

}
