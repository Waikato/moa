package moa.classifiers.semisupervised.attributeSimilarity;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Computes the similarity of categorical attributes using GoodAll3:
 * if X_k == Y_k:   1 - p_k^2(x)
 * else:            0
 */
public class GoodAll3SimilarityCalculator extends AttributeSimilarityCalculator {

    @Override
    public double computePerAttributeSimilarity(Attribute attr, double X_k, double Y_k) {
        if (X_k == Y_k) return 1 - getProbabilityEstimateOfAttributeByValue(attr, (int)X_k);
        return 0;
    }

    @Override
    public double computeWeightOfAttribute(Attribute attr, Instance X, Instance Y) {
        return 1.0 / (float) d;
    }
}
