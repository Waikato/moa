/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
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
 * Filter for standardising instances in a stream.
 *
 * <p>Standardisation is a scaling technique where the values are centered around the mean with a unit standard deviation.
 * This means that the mean of the attribute becomes zero and the resultant distribution has a unit standard deviation.</p>
 *
 * <p>For further information about the scaling technique included in this filter, please view the following website:
 * https://www.analyticsvidhya.com/blog/2020/04/feature-scaling-machine-learning-normalization-standardization/</p>
 *
 * @author Yibin Sun (ys388@students.waikato.ac.nz)
 * @version 03.2021
 */
public class StandardisationFilter extends AbstractStreamFilter {

    @Override
    public String getPurposeString() {
        return "Standardise or Normalise instances in a stream.";
    }

    private static final long serialVersionUID = 1L;

    // Keep tracking statistics
    double[] sum;
    double[] sumOfSquare;
    int count = 0;


    @Override
    protected void restartImpl() {
        //reset all variables
        this.sum = null;
        this.sumOfSquare = null;
        this.count = 0;
    }

    @Override
    public InstancesHeader getHeader() { return this.inputStream.getHeader(); }

    public Instance filterInstance(Instance inst) {

        /**
         * For standardisation
         * Scale every numeric feature's values to a state where the mean is 0 and the standard deviation is 1.
         * The formula used here is :    X' = (X - µ) / σ
         * NOTE: Since we are in a stream, we don't know the overall situation.
         *       Therefore, we can only scale the instance with the most current information.
         */

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


    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
