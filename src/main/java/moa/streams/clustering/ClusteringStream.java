/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package moa.streams.clustering;

import moa.options.AbstractOptionHandler;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.streams.InstanceStream;

/**
 *
 * @author jansen
 */
public abstract class ClusteringStream extends AbstractOptionHandler implements InstanceStream{
    public IntOption decayHorizonOption = new IntOption("decayHorizon", 'h',
                    "Decay horizon", 1000, 0, Integer.MAX_VALUE);

    public FloatOption decayThresholdOption = new FloatOption("decayThreshold", 't',
                    "Decay horizon threshold", 0.01, 0, 1);

    public IntOption evaluationFrequencyOption = new IntOption("evaluationFrequency", 'e',
                    "Evaluation frequency", 1000, 0, Integer.MAX_VALUE);

    public IntOption numAttsOption = new IntOption("numAtts", 'a',
                    "The number of attributes to generate.", 2, 0, Integer.MAX_VALUE);

    public int getDecayHorizon(){
        return decayHorizonOption.getValue();
    }

    public double getDecayThreshold(){
        return decayThresholdOption.getValue();
    }

    public int getEvaluationFrequency(){
        return evaluationFrequencyOption.getValue();
    }


}
