package moa.classifiers.semisupervised.attributeSimilarity;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Computes the attribute similarity using Occurrence Frequency (OF):
 * if X_k == Y_k:   S_k(X_k, Y_k) = 1
 * else:            S_k(X_k, Y_k) = 1 / (1 + log(N / f_k(X_k)) * log(N / f_k(Y_k)))
 */
public class OccurrenceFrequencySimilarityCalculator extends AttributeSimilarityCalculator {

    @Override
    public double computePerAttributeSimilarity(Attribute attr, double X_k, double Y_k) {
        if (X_k == Y_k) return 1;
        if (attributeStats.get(attr) == null) return SMALL_VALUE;
        double fX = Math.max(attributeStats.get(attr).getFrequencyOfValue((int)X_k), SMALL_VALUE);
        double fY = Math.max(attributeStats.get(attr).getFrequencyOfValue((int)Y_k), SMALL_VALUE);
        return 1.0 / (1.0 + Math.log(N / fX) * Math.log(N / fY));
    }

    @Override
    public double computeWeightOfAttribute(Attribute attr, Instance X, Instance Y) {
        return 1.0 / (double) d;
    }
}
