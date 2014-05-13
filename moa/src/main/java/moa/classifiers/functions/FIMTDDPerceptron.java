package moa.classifiers.functions;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Regressor;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;

import java.util.Random;

public class FIMTDDPerceptron extends AbstractClassifier implements Regressor {
    private static final long serialVersionUID = 1L;

    private final double SD_THRESHOLD = 0.0000001;
   
    public FloatOption learningRatioOption = new FloatOption("learningRatio", 'r', "Learning ratio to use for training the Perceptrons in the leaves.", 0.01);

    public FloatOption learningRateDecayFactorOption = new FloatOption("learningRatioDecayFactor", 'f', "Learning rate decay factor (not used when learning rate is constant).", 0.001);
   
    public FlagOption learningRatioConstOption = new FlagOption("learningRatioConst", 'c', "Keep learning rate constant instead of decaying (if kept constant learning ratio is suggested to be 0.001).");
   
   
   
    // The Perception weights
    protected double[] weightAttribute;

    // Statistics used for error calculations
    protected DoubleVector attributeStatistics = new DoubleVector();
    protected DoubleVector squaredAttributeStatistics = new DoubleVector();

protected double sumOfValues;
protected double sumOfSquares;
   
    // The number of instances contributing to this model
    protected int instancesSeen = 0;

    // If the model should be reset or not
    protected boolean reset;

    @Override
    public void getDescription(StringBuilder sb, int indent) {
    }
   
    public String getPurposeString() {
    return "A perceptron regressor as specified by Ikonomovska et al. used for FIMTDD";
    }

    public FIMTDDPerceptron(FIMTDDPerceptron original) {
        this.weightAttribute = original.weightAttribute.clone();
        this.reset = false;
    }

    public FIMTDDPerceptron() {
        this.reset = true;
    }

    public void setWeights(double[] w) {
        this.weightAttribute = w;
    }

    public double[] getWeights() {
        return this.weightAttribute;
    }

    /**
     * A method to reset the model
     */
    public void resetLearningImpl() {
        this.reset = true;
    }

    /**
     * Update the model using the provided instance
     */
    public void trainOnInstanceImpl(Instance inst) {
   
        // Initialize Perceptron if necessary  
        if (this.reset == true) {
        Random r = new Random();
        r.setSeed(1234);
            this.reset = false;
            this.weightAttribute = new double[inst.numAttributes()];
            this.instancesSeen = 0;
            this.attributeStatistics = new DoubleVector();
            this.squaredAttributeStatistics = new DoubleVector();
            for (int j = 0; j < inst.numAttributes(); j++) {
                weightAttribute[j] = 2 * r.nextDouble() - 1;
            }
        }

        // Update attribute statistics
        instancesSeen++;
        for(int j = 0; j < inst.numAttributes() -1; j++) {
            attributeStatistics.addToValue(j, inst.value(j));   
            squaredAttributeStatistics.addToValue(j, inst.value(j) * inst.value(j));
        }

        // Update weights
        double learningRatio = 0.0;
        if (learningRatioConstOption.isSet()) {
            learningRatio = learningRatioOption.getValue();
        } else {
            learningRatio = learningRatioOption.getValue() / (1 + instancesSeen * learningRateDecayFactorOption.getValue());
            //learningRatio = 0.05 * computeSD(sumOfSquares, sumOfValues, instancesSeen);
        }
      
        sumOfValues += inst.classValue();
        sumOfSquares += inst.classValue() * inst.classValue();

        updateWeights(inst, learningRatio);
           
    }
   
    public void updateWeights(Instance inst, double learningRatio) {
// Normalize Instance
double[] normalizedInstance = normalizedInstance(inst);
// Compute the Normalized Prediction of Perceptron
double normalizedPrediction = prediction(normalizedInstance);
double normalizedValue = normalizeClassValue(inst);
double delta = normalizedValue - normalizedPrediction;

        for (int j = 0; j < inst.numAttributes() - 1; j++) {
            if (inst.attribute(modelAttIndexToInstanceAttIndex(j, inst)).isNumeric()) {
                weightAttribute[j] += learningRatio * delta * normalizedInstance[j];
            }
}
this.weightAttribute[inst.numAttributes() - 1] += learningRatio * delta;
}

public double[] normalizedInstance(Instance inst){
// Normalize Instance
double[] normalizedInstance = new double[inst.numAttributes()];
for (int j = 0; j < inst.numAttributes() - 1; j++) {
int instAttIndex = modelAttIndexToInstanceAttIndex(j, inst);
double mean = attributeStatistics.getValue(j) / (instancesSeen > 0 ? instancesSeen : 1);
double sd = computeSD(squaredAttributeStatistics.getValue(j), attributeStatistics.getValue(j), instancesSeen);
if (inst.attribute(instAttIndex).isNumeric())
normalizedInstance[j] = (inst.value(instAttIndex) - mean) / ((sd > SD_THRESHOLD) ? sd : 1);
else
normalizedInstance[j] = 0;

}
return normalizedInstance;
}

public  double computeSD(double squaredVal, double val, int size) {
if (size > 1) {
return Math.sqrt((squaredVal - ((val * val) / size)) / (size - 1.0));
}
return 0.0;
}

  
    /**
     * Output the prediction made by this perceptron on the given instance
     */
public double prediction(double[] instanceValues)
{
double prediction = 0.0;
if( this.reset == false)
{
for (int j = 0; j < instanceValues.length - 1; j++) {
prediction += this.weightAttribute[j] * instanceValues[j];
}
prediction += this.weightAttribute[instanceValues.length - 1];
}
return prediction;
}

private double prediction(Instance inst) {
double[] normalizedInstance = normalizedInstance(inst);
double normalizedPrediction = prediction(normalizedInstance);
return denormalizedPrediction(normalizedPrediction);
}

private double denormalizedPrediction(double normalizedPrediction) {
if (!reset) {
double mean = sumOfValues / (instancesSeen > 0 ? instancesSeen : 1);
double sd = computeSD(sumOfSquares, sumOfValues, instancesSeen);
return normalizedPrediction * ((sd > SD_THRESHOLD) ? sd : 1) + mean;
} else
return normalizedPrediction; //Perceptron may have been reset. Use old weights to predict

}

private double normalizeClassValue(Instance inst) {
double mean = sumOfValues / instancesSeen;
double sd = computeSD(sumOfSquares, sumOfValues, instancesSeen);

double normalized = 0.0;
if (sd > SD_THRESHOLD) {
normalized = (inst.classValue() - mean) / (sd);
} else {
normalized = inst.classValue() - mean;
}
return normalized;
}

@Override
public boolean isRandomizable() {
return true;
}

@Override
public double[] getVotesForInstance(Instance inst) {
return new double[] {prediction(inst)};
}

@Override
protected Measurement[] getModelMeasurementsImpl() {
return null;
}

@Override
public void getModelDescription(StringBuilder out, int indent) {
        StringUtils.appendIndented(out, indent, getClassNameString() + " =");
if (getModelContext() != null) {
for (int j = 0; j < getModelContext().numAttributes() - 1; j++) {
if (getModelContext().attribute(j).isNumeric()) {
out.append((j == 0 || weightAttribute[j] < 0) ? " " : " + ");
                out.append(String.format("%.4f", weightAttribute[j]));
                out.append(" * ");
                out.append(getAttributeNameString(j));
            }
        }
out.append(" + " + weightAttribute[getModelContext().numAttributes() - 1]);
}
        StringUtils.appendNewline(out);
}

}