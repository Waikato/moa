
package moa.classifiers.bayes;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.GaussianNumericAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NominalAttributeClassObserver;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Naive Bayes incremental learner.
 *
 * <p>Performs classic bayesian prediction while making naive assumption that
 * all inputs are independent.<br /> Naive Bayes is a classiﬁer algorithm known
 * for its simplicity and low computational cost. Given n different classes, the
 * trained Naive Bayes classiﬁer predicts for every unlabelled instance I the
 * class C to which it belongs with high accuracy.</p>
 *
 * <p>Parameters:</p> <ul> <li>-r : Seed for random behaviour of the
 * classifier</li> </ul>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class NaiveBayes extends AbstractClassifier {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Naive Bayes classifier: performs classic bayesian prediction while making naive assumption that all inputs are independent.";
    }
    protected DoubleVector observedClassDistribution;

    protected AutoExpandVector<AttributeClassObserver> attributeObservers;

    @Override
    public void resetLearningImpl() {
        this.observedClassDistribution = new DoubleVector();
        this.attributeObservers = new AutoExpandVector<AttributeClassObserver>();
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        this.observedClassDistribution.addToValue((int) inst.classValue(), inst.weight());
        for (int i = 0; i < inst.numAttributes() - 1; i++) {
            int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
            AttributeClassObserver obs = this.attributeObservers.get(i);
            if (obs == null) {
                obs = inst.attribute(instAttIndex).isNominal() ? newNominalClassObserver()
                        : newNumericClassObserver();
                this.attributeObservers.set(i, obs);
            }
            obs.observeAttributeClass(inst.value(instAttIndex), (int) inst.classValue(), inst.weight());
        }
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {

        return doNaiveBayesPrediction(inst, this.observedClassDistribution, this.attributeObservers);
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        for (int i = 0; i < this.observedClassDistribution.numValues(); i++) {
            StringUtils.appendIndented(out, indent, "Observations for ");
            out.append(getClassNameString());
            out.append(" = ");
            out.append(getClassLabelString(i));
            out.append(":");
            StringUtils.appendNewlineIndented(out, indent + 1,
                    "Total observed weight = ");
            out.append(this.observedClassDistribution.getValue(i));
            out.append(" / prob = ");
            out.append(this.observedClassDistribution.getValue(i)
                    / this.observedClassDistribution.sumOfValues());
            for (int j = 0; j < this.attributeObservers.size(); j++) {
                StringUtils.appendNewlineIndented(out, indent + 1,
                        "Observations for ");
                out.append(getAttributeNameString(j));
                out.append(": ");
                // TODO: implement observer output
                out.append(this.attributeObservers.get(j));
            }
            StringUtils.appendNewline(out);
        }
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    protected AttributeClassObserver newNominalClassObserver() {
        return new NominalAttributeClassObserver();
    }

    protected AttributeClassObserver newNumericClassObserver() {
        return new GaussianNumericAttributeClassObserver();
    }

    public static double[] doNaiveBayesPrediction(Instance inst,DoubleVector observedClassDistribution,AutoExpandVector<AttributeClassObserver> attributeObservers) {
        
       
        //RS
        //double[] votes = new double[observedClassDistribution.numValues()];
        double[] votes = new double[inst.numClasses()];
        double observedClassSum = observedClassDistribution.sumOfValues();
        for (int classIndex = 0; classIndex < votes.length; classIndex++) {
            votes[classIndex] = observedClassDistribution.getValue(classIndex)
                    / observedClassSum;
            for (int attIndex = 0; attIndex < inst.numAttributes() - 1; attIndex++) {
                int instAttIndex = modelAttIndexToInstanceAttIndex(attIndex,
                        inst);
                AttributeClassObserver obs = attributeObservers.get(attIndex);
                if ((obs != null) && !inst.isMissing(instAttIndex)) {
                    votes[classIndex] *= obs.probabilityOfAttributeValueGivenClass(inst.value(instAttIndex), classIndex);
                }
            }
        }
        // TODO: need logic to prevent underflow?
        return votes;
    }

    // Naive Bayes Prediction using log10 for VFDR rules 
    public static double[] doNaiveBayesPredictionLog(Instance inst,
            DoubleVector observedClassDistribution,
            AutoExpandVector<AttributeClassObserver> observers, AutoExpandVector<AttributeClassObserver> observers2) {
        AttributeClassObserver obs;
        double[] votes = new double[observedClassDistribution.numValues()];
        double observedClassSum = observedClassDistribution.sumOfValues();
        for (int classIndex = 0; classIndex < votes.length; classIndex++) {
            votes[classIndex] = Math.log10(observedClassDistribution.getValue(classIndex)
                    / observedClassSum);
            for (int attIndex = 0; attIndex < inst.numAttributes() - 1; attIndex++) {
                int instAttIndex = modelAttIndexToInstanceAttIndex(attIndex,
                        inst);
                if (inst.attribute(instAttIndex).isNominal()) {
                    obs = observers.get(attIndex);
                } else {
                    obs = observers2.get(attIndex);
                }

                if ((obs != null) && !inst.isMissing(instAttIndex)) {
                    votes[classIndex] += Math.log10(obs.probabilityOfAttributeValueGivenClass(inst.value(instAttIndex), classIndex));

                }
            }
        }
        return votes;

    }

    public void manageMemory(int currentByteSize, int maxByteSize) {
        // TODO Auto-generated method stub
    }
}
