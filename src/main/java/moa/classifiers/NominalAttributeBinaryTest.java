/*
 *    NominalAttributeBinaryTest.java
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
package moa.classifiers;

import moa.core.InstancesHeader;
import weka.core.Instance;

public class NominalAttributeBinaryTest extends InstanceConditionalBinaryTest {

	private static final long serialVersionUID = 1L;

	protected int attIndex;

	protected int attValue;

	public NominalAttributeBinaryTest(int attIndex, int attValue) {
		this.attIndex = attIndex;
		this.attValue = attValue;
	}

	@Override
	public int branchForInstance(Instance inst) {
		int instAttIndex = this.attIndex < inst.classIndex() ? this.attIndex
				: this.attIndex + 1;
		return inst.isMissing(instAttIndex) ? -1 : ((int) inst
				.value(instAttIndex) == this.attValue ? 0 : 1);
	}

	@Override
	public String decribeConditionForBranch(int branch, InstancesHeader context) {
		if ((branch == 0) || (branch == 1)) {
			return InstancesHeader.getAttributeNameString(context,
					this.attIndex)
					+ (branch == 0 ? " = " : " != ")
					+ InstancesHeader.getNominalValueString(context,
							this.attIndex, this.attValue);
		}
		throw new IndexOutOfBoundsException();
	}

	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	public int[] getAttsTestDependsOn() {
		return new int[] { this.attIndex };
	}

}
