/*
 *    MultilabelArffFileStream.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jmr30@cs.waikato.ac.nz)
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
package moa.streams.generators.multilabel;

import moa.streams.ArffFileStream;
import moa.streams.MultiTargetInstanceStream;

import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.MultilabelInstancesHeader;

import com.github.javacliparser.IntOption;

/**
 * Stream reader for ARFF files of multilabel data.
 *
 * @author Jesse Read (jmr30@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class MultilabelArffFileStream extends ArffFileStream implements MultiTargetInstanceStream{

    @Override
    public String getPurposeString() {
        return "A stream read from an ARFF file.";
    }

    private static final long serialVersionUID = 1L;

    public IntOption numLabelsOption = new IntOption("numLabels", 'l',
            "The number of labels. e.g. n = 10 : the first 10 binary attributes are the labels; n = -10 the last 10 binary attributes are the labels.", -1, -1, Integer.MAX_VALUE);

    public MultilabelArffFileStream() {
    }

    public MultilabelArffFileStream(String arffFileName, int numLabels) {
        this.arffFileOption.setValue(arffFileName);
        this.numLabelsOption.setValue(numLabels);
        restart();
    }

    @Override
    public InstancesHeader getHeader() {
        return new MultilabelInstancesHeader(this.instances, numLabelsOption.getValue());
    }
}
