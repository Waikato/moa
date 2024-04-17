package moa.classifiers.semisupervised.attributeSimilarity;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Does nothing, just ignores the categorical attributes
 */
public class IgnoreSimilarityCalculator extends AttributeSimilarityCalculator {

    @Override
    public double computePerAttributeSimilarity(Attribute attr, double X_k, double Y_k) {
        return 0;
    }

    @Override
    public double computeWeightOfAttribute(Attribute attr, Instance X, Instance Y) {
        return 0;
    }
}
