/*
 *    RemoveDiscreteAttributeFilter.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.streams.filters;

import java.util.ArrayList;
import java.util.List;
import moa.core.InstanceExample;
import moa.streams.InstanceStream;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.SparseInstance;
import moa.core.FastVector;

/**
 * Filter for removing discrete attributes in instances of a stream.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class RemoveDiscreteAttributeFilter extends AbstractStreamFilter {

    @Override
    public String getPurposeString() {
        return "Removes discrete attribute examples in a stream.";
    }
    private static final long serialVersionUID = 1L;

    protected InstancesHeader streamHeader;

    protected List<Integer> numericAttributes;
    
    @Override
    protected void restartImpl() {
        this.streamHeader = null;
    }

    @Override
    public InstancesHeader getHeader() {
        return streamHeader;
    }

    @Override
    public InstanceExample nextInstance() {
        Instance inst = (Instance) this.inputStream.nextInstance().getData();

        if (streamHeader == null) {
            //Create a new header
            FastVector attributes = new FastVector();
            numericAttributes = new ArrayList<Integer>();
            for (int i = 0; i < inst.numAttributes(); i++) {
                if (inst.attribute(i).isNumeric()) {
                    numericAttributes.add(i);
                    attributes.addElement(inst.attribute(i));
                }
            }
            attributes.addElement(inst.classAttribute());
            numericAttributes.add(inst.classIndex());
            this.streamHeader = new InstancesHeader(new Instances(
                    getCLICreationString(InstanceStream.class), attributes, 0));
            this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);

        }

        //Create a new instance
        double[] vals = new double[getHeader().numAttributes()];
        for (int i = 0; i < numericAttributes.size(); i++) {
                vals[i] = inst.value(numericAttributes.get(i));
        }

        Instance instance = null;
        if (inst instanceof SparseInstance) {
            instance = new SparseInstance(inst.weight(), vals);
        } else {
            instance = new DenseInstance(inst.weight(), vals);
        }


        return new InstanceExample(instance);
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
