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


import com.github.javacliparser.FloatOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;


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

/**
 * This filter is to standardise instances in a stream.
 *
 * Z-SCORE is used to standardise the values of a normal distribution.
 * For more information: https://en.wikipedia.org/wiki/Standard_score. The formula is:
 * z=(z-μ)/σ
 * μ is the mean of the population.
 * σ is the standard deviation of the population, as the square root of variance.
 *
 * There are three algorithms for calculating variance.
 * For more information: https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Computing_shifted_data
 * 1. Naive algorithm
 * 2. Welford's online algorithm
 * 3. Two-pass algorithm
 *
 * @author Ethan Wang
 */
public class StandardisationFilter extends AbstractStreamFilter {

    @Override
    public String getPurposeString() {
        return "Standardise or Normalise instances in a stream.";
    }

    public MultiChoiceOption AlgorithmOption = new MultiChoiceOption(
            "AlgorithmOption", 'a', "Standardisation Algorithm Option",
            new String[]{"Naive", "Welford", "Two-pass", "Weighted"},
            new String[]{"Naive(default)",
                    "Welford", "Two-pass", "Weighted"}
            , 0);
    public FloatOption WeightedOptionFloat = new FloatOption("WeightedOptionFloat", 'w', "The weight of weighted incremental algorithm", 0.0);
    private static final long serialVersionUID = 1L;

    // Keep tracking statistics
    private static double[] sum;
    private static double[] sumOfSquare;
    private static int count = 0;
    private static double[] delta;
    private static double[] delta2;
    private static double[] M2;
    private static double[] mean;
    private static double[] meanOld;

    protected int AlgorithmIndex = 0;

    @Override
    protected void restartImpl() {
        //reset all variables
        sum = null;
        sumOfSquare = null;
        delta = null;
        delta2 = null;
        count = 0;
        M2 = null;
        mean = null;
        meanOld = null;
    }

    @Override
    public InstancesHeader getHeader() {
        return this.inputStream.getHeader();
    }

    public Instance filterInstance(Instance inst) {
        // Initiate the variables when first arrive
        // The variable names below are meaningless sometimes to reduce the amount of them.
        if (sum == null) sum = new double[inst.numAttributes() - 1];
        if (sumOfSquare == null) sumOfSquare = new double[inst.numAttributes() - 1];
        if (delta == null) delta = new double[inst.numAttributes() - 1];
        if (delta2 == null) delta2 = new double[inst.numAttributes() - 1];
        if (M2 == null) M2 = new double[inst.numAttributes() - 1];
        if (mean == null) mean = new double[inst.numAttributes() - 1];
        if (meanOld == null) meanOld = new double[inst.numAttributes() - 1];
        AlgorithmIndex = this.AlgorithmOption.getChosenIndex();
        Instance standardisedInstance = inst.copy();
        count++;

        for (int i = 0; i < inst.numAttributes() - 1; i++) {
            // Ignore the nominal attributes
            if (!inst.attribute(i).isNominal()) {
                switch (AlgorithmIndex) {
                    case 0://Naive
                        // Update the statistics
                        sum[i] += inst.value(i);
                        sumOfSquare[i] += inst.value(i) * inst.value(i);
                        //When sum or sumofSquare overflow that is infinity
//                        System.out.println(sumOfSquare[i]);
                        // Assign the new values if it's not infinity
                        if (sumOfSquare[i] / count != 0)
                            //Standardisation
                            standardisedInstance.setValue(i, (inst.value(i) - sum[i] / count) / Math.sqrt((sumOfSquare[i] - (sum[i] * sum[i]) / count) / (count-1)));
                            //Standard deviation
//                            standardisedInstance.setValue(i, Math.sqrt((sumOfSquare[i] - (sum[i] * sum[i]) / count) / (count - 1)));
                        else {
                            standardisedInstance.setValue(i, 0);
                        }
                        break;
                    case 1: //Welford
                        delta[i] = inst.value(i) - mean[i];
                        mean[i] += delta[i] / count;
                        delta2[i] = inst.value(i) - mean[i];
                        M2[i] += delta[i] * delta2[i];

                        if (M2[i] / count != 0)
                            //Standardisation
                            standardisedInstance.setValue(i, (inst.value(i) - mean[i]) / (Math.sqrt(M2[i] / (count-1))));
                            //Standard deviation
//                            standardisedInstance.setValue(i, Math.sqrt(M2[i] / (count-1)));
                        else
                            standardisedInstance.setValue(i, 0);
                        break;
                    case 2: //Two-pass
                        delta[i] += inst.value(i);
                        mean[i] = delta[i] / count;
                        delta2[i] += (inst.value(i) - mean[i]) * (inst.value(i) - mean[i]);

                        if (delta2[i] / count != 0)
                            //Standardisation
                            standardisedInstance.setValue(i, (inst.value(i) - mean[i]) / Math.sqrt(delta2[i] / (count-1)));
                            //Standard deviation
//                            standardisedInstance.setValue(i, Math.sqrt(delta2[i] / (count-1)));
                        else
                            standardisedInstance.setValue(i, 0);
                        break;
                }
            }
        }
        return standardisedInstance;
    }


    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
