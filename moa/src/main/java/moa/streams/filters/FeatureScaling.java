package moa.streams.filters;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import meka.core.F;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

/**
 * Filter for standardising and normalising instances in a stream.
 *
 * <p>Standardisation is scaling technique where the values are centered around the mean with a unit standard deviation.
 * This means that the mean of the attribute becomes zero and the resultant distribution has a unit standard deviation.</p>
 *
 * <p>Normalisation is another scaling technique in which values are shifted and rescaled so that they end up ranging between 0 and 1.
 * It is also known as Min-Max scaling.</p>
 *
 * <p>For further information about the scaling technique included in this filter, please view the following website:
 * https://www.analyticsvidhya.com/blog/2020/04/feature-scaling-machine-learning-normalization-standardization/</p>
 *
 * @author Yibin Sun (ys388@students.waikato.ac.nz)
 * @version 03.2021
 */
public class FeatureScaling extends AbstractStreamFilter {

    @Override
    public String getPurposeString() {
        return "Standardise or Normalise instances in a stream.";
    }

    public FlagOption standardisationOption = new FlagOption
            ("Standardisation", 's', "Standardise instances");

    public FlagOption normalisationOption = new FlagOption
            ("Normalisation", 'n', "Normalise instances");

    private static final long serialVersionUID = 1L;

    // Keep tracking statistics
    double[] sum;
    double[] sumOfSquare;
    int count = 0;

    // Keep tracking the max and min for each feature
    double[] maximums;
    double[] minimums;



    @Override
    protected void restartImpl() {
        //reset all variables
        this.sum = null;
        this.sumOfSquare = null;
        this.count = 0;

        this.maximums = null;
        this.minimums = null;
    }

    @Override
    public InstancesHeader getHeader() {
        return this.inputStream.getHeader();
    }


    public Instance filterInstance(Instance inst) {

        // Return the original instance if the standardisation and normalisation options are selected at the same time
        if(standardisationOption.isSet() && normalisationOption.isSet()) return inst;

        /** For standardisation
         *  Scale every numeric feature's values to a state where the mean is 0 and the standard deviation is 1.
         *  The formula used here is :    X' = (X - µ) / σ
         *  NOTE: Since we are in a stream, we don't know the overall situation.
         *        Therefore, we can only scale the instance with the most current information.
         */
        if (standardisationOption.isSet()) {

            // Initiate the variables when first arrive
            if (this.sum == null) this.sum = new double[inst.numAttributes() - 1];
            if (this.sumOfSquare == null) this.sumOfSquare = new double[inst.numAttributes() - 1];

            Instance standardisedInstance = inst.copy();
            count++;

            for (int i = 0; i < inst.numAttributes() - 1; i++) {
                // Ignore the nominal attributes
                if (!inst.attribute(i).isNominal()) {

                    // Update the statistics
                    sum[i] += inst.value(i);
                    sumOfSquare[i] += inst.value(i) * inst.value(i);

                    // Assign the new values if it's not infinity
                    if (sumOfSquare[i] / count != 0)
                        standardisedInstance.setValue(i, (inst.value(i) - sum[i] / count) / Math.sqrt(sumOfSquare[i] / count));
                    else
                        standardisedInstance.setValue(i, 0);
                }
            }
            return standardisedInstance;

        }

        /** For normalisation
         *  Scale every numeric feature's values to the range between 0 and 1.
         *  The formula used here is :    X' = (X - X_min) / (X_max - X_min)
         *  NOTE: Similar to the standardisation, the information is evolving over time in a stream.
         *        We can only scale the instances with the most current information instead of the overall information.
         */
        if (normalisationOption.isSet()) {

            // Initiate the variables when first arrive
            if (this.maximums == null && this.minimums == null){
                this.maximums = new double[inst.numAttributes() - 1];
                this.minimums = new double[inst.numAttributes() - 1];
                for(int i = 0; i < inst.numAttributes() - 1; i++){
                    maximums[i] = inst.value(i);
                    minimums[i] = inst.value(i);
                }
            }

            Instance normalisedInstance = inst.copy();

            for (int i = 0; i < inst.numAttributes() - 1; i++) {
                // Ignore the nominal attributes
                if (!inst.attribute(i).isNominal()) {

                    // Update the extreme values
                    if (this.minimums[i] > inst.value(i)) this.minimums[i] = inst.value(i);
                    if (this.maximums[i] < inst.value(i)) this.maximums[i] = inst.value(i);

                    // Assign new values if it's not infinity
                    if (this.maximums[i] - this.minimums[i] != 0)
                        normalisedInstance.setValue(i,
                                (inst.value(i) - this.minimums[i]) / (this.maximums[i] - this.minimums[i]));
                    else normalisedInstance.setValue(i, 0);

                }
            }
            return normalisedInstance;
        }
        // If no option is selected, return the original instance
        return inst;

    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

}
