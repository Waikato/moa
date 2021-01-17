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
 * @author Yibin Sun (ys388@students.waikato.ac.nz)
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

        // Return unchanged instance when both standardisation and normalisation are checked

        if(standardisationOption.isSet() && normalisationOption.isSet()) return inst;

        // For standardisation
        if (standardisationOption.isSet()) {

            // Initiate the variables when first arrive
            if (this.sum == null) this.sum = new double[inst.numAttributes() - 1];
            if (this.sumOfSquare == null) this.sumOfSquare = new double[inst.numAttributes() - 1];

            // Copy the original instance
            Instance standardisedInstance = inst.copy();

            // Update the counter
            count++;

            // Loop through the instance
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

            // Return the standardised instance
            return standardisedInstance;

        }

        // For normalisation
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

            // Copy the original instance
            Instance normalisedInstance = inst.copy();

            // Loop through the instance
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

            // Return the normalised instance
            return normalisedInstance;
        }
        // If no option was selected, return the original instance
        return inst;

    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

}
