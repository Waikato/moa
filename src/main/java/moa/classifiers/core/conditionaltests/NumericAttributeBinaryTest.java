/*
 *    NumericAttributeBinaryTest.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.classifiers.core.conditionaltests;

import moa.core.InstancesHeader;
import weka.core.Instance;

/**
 * Numeric binary conditional test for instances to use to split nodes in Hoeffding trees.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class NumericAttributeBinaryTest extends InstanceConditionalBinaryTest {

    private static final long serialVersionUID = 1L;

    protected int attIndex;

    protected double attValue;

    protected boolean equalsPassesTest;

    public NumericAttributeBinaryTest(int attIndex, double attValue,
            boolean equalsPassesTest) {
        this.attIndex = attIndex;
        this.attValue = attValue;
        this.equalsPassesTest = equalsPassesTest;
    }

    @Override
    public int branchForInstance(Instance inst) {
        int instAttIndex = this.attIndex < inst.classIndex() ? this.attIndex
                : this.attIndex + 1;
        if (inst.isMissing(instAttIndex)) {
            return -1;
        }
        double v = inst.value(instAttIndex);
        if (v == this.attValue) {
            return this.equalsPassesTest ? 0 : 1;
        }
        return v < this.attValue ? 0 : 1;
    }

    @Override
    public String describeConditionForBranch(int branch, InstancesHeader context) {
        if ((branch == 0) || (branch == 1)) {
            char compareChar = branch == 0 ? '<' : '>';
            int equalsBranch = this.equalsPassesTest ? 0 : 1;
            return InstancesHeader.getAttributeNameString(context,
                    this.attIndex)
                    + ' '
                    + compareChar
                    + (branch == equalsBranch ? "= " : " ")
                    + InstancesHeader.getNumericValueString(context,
                    this.attIndex, this.attValue);
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    public int[] getAttsTestDependsOn() {
        return new int[]{this.attIndex};
    }

    public double getSplitValue() {
        return this.attValue;
    }
}
