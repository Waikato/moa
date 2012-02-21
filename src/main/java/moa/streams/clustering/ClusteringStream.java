/*
 *    ClusteringStream.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

package moa.streams.clustering;

import moa.options.AbstractOptionHandler;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.streams.InstanceStream;

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
