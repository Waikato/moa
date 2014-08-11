/*
 *    MultilabelInstance.java
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
package moa.core;

import com.yahoo.labs.samoa.instances.InstanceImpl;

/**
 * Multilabel instance.
 *
 * @author Jesse Read (jmr30@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class MultilabelInstance extends InstanceImpl {

    public MultilabelInstance(double d, double[] res) {
         super(d,res);
    }
    public MultilabelInstance(InstanceImpl inst) {
        super(inst);
    }

    int L = -1;

    public void setNumLabels(int n) {
        this.L = n;
    }

    public int getNumLabels() {
        return this.L;
    }
}
