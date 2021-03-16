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
 * Filter for standardising and normalising instances in a stream.
 *
 * <p>Normalisation is a scaling technique in which values are shifted and rescaled so that they end up ranging between 0 and 1.
 * It is also known as Min-Max scaling.</p>
 *
 * <p>For further information about the scaling technique included in this filter, please view the following website:
 * https://www.analyticsvidhya.com/blog/2020/04/feature-scaling-machine-learning-normalization-standardization/</p>
 *
 * @author Yibin Sun (ys388@students.waikato.ac.nz)
 * @version 03.2021
 */
public class NormalisationFilter extends AbstractStreamFilter {

    @Override
    public String getPurposeString() {
        return "Standardise or Normalise instances in a stream.";
    }

    private static final long serialVersionUID = 1L;

    // Keep tracking the max and min for each feature
    double[] maximums;
    double[] minimums;


    @Override
    protected void restartImpl() {
        //reset all variables
        this.maximums = null;
        this.minimums = null;
    }

    @Override
    public InstancesHeader getHeader() {
        return this.inputStream.getHeader();
    }


    public Instance filterInstance(Instance inst) {

        /** For normalisation
         *  Scale every numeric feature's values to the range between 0 and 1.
         *  The formula used here is :    X' = (X - X_min) / (X_max - X_min)
         *  NOTE: Similar to the standardisation, the information is evolving over time in a stream.
         *        We can only scale the instances with the most current information instead of the overall information.
         */

        // Initiate the variables when first arrive
        if (this.maximums == null && this.minimums == null) {
            this.maximums = new double[inst.numAttributes() - 1];
            this.minimums = new double[inst.numAttributes() - 1];
            for (int i = 0; i < inst.numAttributes() - 1; i++) {
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

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

}
