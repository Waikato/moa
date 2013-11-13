/*
 *    Converter.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jesse@tsc.uc3m.es)
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
package moa.core.utils;

import java.util.LinkedList;
import java.util.List;
import moa.AbstractMOAObject;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

/**
 * Converter. This class can be used to convert a multi-label instance into a
 * single-label instance.
 * 
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public class Converter extends AbstractMOAObject {

    protected Instances m_InstancesTemplate = null;

    protected int m_L = -1;

    public int getL() {
        return this.m_L;
    }

    public Converter() {
    }

    public Converter(int n) {
        m_L = n;
    }

    public Instances createTemplate(Instances i) {
        this.m_InstancesTemplate = new Instances(i, 0, 0);
        return this.m_InstancesTemplate;
    }

    public Instance formatInstance(Instance original) {

        //Copy the original instance
        Instance converted = (Instance) original.copy();
        converted.setDataset(null);

        //Delete all class attributes
        for (int j = 0; j < m_L; j++) {
            converted.deleteAttributeAt(0);
        }

        //Add one of those class attributes at the begginning
        converted.insertAttributeAt(0);

        //Hopefully setting the dataset will configure that attribute properly
        converted.setDataset(m_InstancesTemplate);

        return converted;

    }

    public List<Integer> getRelevantLabels(Instance x) {
        List<Integer> classValues = new LinkedList<Integer>();
        //get all class attributes
        for (int j = 0; j < m_L; j++) {
            if (x.value(j) > 0.0) {
                classValues.add(j);
            }
        }
        return classValues;
    }

    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
