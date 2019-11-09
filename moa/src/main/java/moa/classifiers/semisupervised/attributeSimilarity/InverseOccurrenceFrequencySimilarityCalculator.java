package moa.classifiers.semisupervised.attributeSimilarity;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Computes the similarity between categorical attributes using Inverse Occurrence Frequency (IOF)
 */
public class InverseOccurrenceFrequencySimilarityCalculator extends AttributeSimilarityCalculator {
    @Override
    public double computePerAttributeSimilarity(Attribute attr, double X_k, double Y_k) {
        if (X_k == Y_k) return 1.0;
        double fX = Math.max(attributeStats.get(attr).getFrequencyOfValue((int)X_k), SMALL_VALUE);
        double fY = Math.max(attributeStats.get(attr).getFrequencyOfValue((int)Y_k), SMALL_VALUE);
        double logX = fX > 0 ? Math.log(fX) : 0.0;
        double logY = fY > 0 ? Math.log(fY) : 0.0;
        return 1 / (1 + logX * logY);
    }

    @Override
    public double computeWeightOfAttribute(Attribute attr, Instance X, Instance Y) {
        return 0;
    }
}
