package moa.classifiers.functions;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Regressor;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;

import java.util.Random;

public class FIMTDDPerceptron extends AbstractClassifier implements Regressor {
    private static final long serialVersionUID = 1L;

    public FloatOption learningRatioOption = new FloatOption("learningRatio", 'r', "Learning ratio to use for training the Perceptrons in the leaves.", 0.1);

    public FloatOption learningRateDecayFactorOption = new FloatOption("learningRatioDecayFactor", 'f', "Learning rate decay factor (not used when learning rate is constant).", 0.001);
    
    public FlagOption learningRatioConstOption = new FlagOption("learningRatioConst", 'c', "Keep learning rate constant instead of decaying (if kept constant learning ratio is suggested to be 0.001).");
    
    // The Perception weights 
    protected double[] weightAttribute; 

    // Statistics used for error calculations
    protected DoubleVector attributeStatistics = new DoubleVector();
    protected DoubleVector squaredAttributeStatistics = new DoubleVector();

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
            squaredAttributeStatistics.addToValue(j, inst.value(j)*inst.value(j));
        }

        // Update weights
        double learningRatio = 0.0;
        if (learningRatioConstOption.isSet()) {
            learningRatio = learningRatioOption.getValue();
        } else {
            learningRatio = learningRatioOption.getValue() / (1 + instancesSeen * learningRateDecayFactorOption.getValue());
        }
        double actualClass = inst.classValue();
        double predictedClass = this.prediction(inst);

        // SET DELTA TO ACTUAL - PREDICTED, NOT PREDICTED - ACTUAL AS SAID IN PAPER
        double delta = actualClass - predictedClass;

        for (int j = 0; j < inst.numAttributes() - 1; j++) {

            if (inst.attribute(j).isNumeric()) {
                // Update weights. Ensure attribute values are normalized first
                double sd = Math.sqrt((squaredAttributeStatistics.getValue(j) - ((attributeStatistics.getValue(j) * attributeStatistics.getValue(j))/instancesSeen))/instancesSeen);
                double instanceValue = 0;
                if(sd > 0.0000001) // Limit found in implementation by Ikonomovska et al (2011)
                {
                    instanceValue = (inst.value(j) - (attributeStatistics.getValue(j) / instancesSeen)) / (3 * sd);
                }
                this.weightAttribute[j] += learningRatio * delta * instanceValue;
            }
        }
        this.weightAttribute[inst.numAttributes() - 1] += learningRatio * delta;
    }

    /**
     * Output the prediction made by this perceptron on the given instance
     */
    public double prediction(Instance inst) {
        double prediction = 0;
        if (this.reset == false) {
            for (int j = 0; j < inst.numAttributes() - 1; j++) {
                if (inst.attribute(j).isNumeric()) {    
                    prediction += this.weightAttribute[j] * inst.value(j);
                }
            } 
            prediction += this.weightAttribute[inst.numAttributes() - 1];
        }

        // Return prediction to 3dp
        return (double)Math.round(prediction * 1000) / 1000;
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
        StringUtils.appendIndented(out, indent, "");
		if (getModelContext() != null) {
			for (int j = 0; j < getModelContext().numAttributes() - 1; j++) {
				if (getModelContext().attribute(j).isNumeric()) {
	                out.append(String.format("%.4f", weightAttribute[j]));
	                out.append(" * ");
	                out.append(InstancesHeader.getAttributeNameString(getModelContext(), j));
	            }
	            if (j != getModelContext().numAttributes() - 2) {
	            	out.append(" + ");
	            }
	        }
		}
        StringUtils.appendNewline(out);
	}

}
