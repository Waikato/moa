package moa.classifiers.semisupervised.attributeSimilarity;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Computes the similarity of categorical attributes using Lin formula:
 * if X_k == Y_k:   S_k = 2 * log[p_k(X_k)]
 * else:            S_k = 2 * log[p_k(X_k) + p_k(Y_k)]
 */
public class LinSimilarityCalculator extends AttributeSimilarityCalculator {

    @Override
    public double computePerAttributeSimilarity(Attribute attr, double X_k, double Y_k) {
        double pX = getSampleProbabilityOfAttributeByValue(attr, (int)X_k);
        double pY = getSampleProbabilityOfAttributeByValue(attr, (int)Y_k);
        if (X_k == Y_k) return 2.0 * Math.log(pX);
        return 2.0 * Math.log(pX + pY);
    }

    @Override
    public double computeWeightOfAttribute(Attribute attr, Instance X, Instance Y) {
        double deno = 0;
        for (int i = 0; i < d; i++) {
            double pX = getSampleProbabilityOfAttributeByValue(attr, (int)X.value(i));
            double pY = getSampleProbabilityOfAttributeByValue(attr, (int)Y.value(i));
            deno += Math.log(pX) + Math.log(pY);
        }
        if (deno == 0) return 1.0;
        return 1.0 / deno;
    }
}
