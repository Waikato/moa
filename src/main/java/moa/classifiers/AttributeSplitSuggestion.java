/*
 *    AttributeSplitSuggestion.java
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

import moa.AbstractMOAObject;

public class AttributeSplitSuggestion extends AbstractMOAObject implements
		Comparable<AttributeSplitSuggestion> {

	private static final long serialVersionUID = 1L;

	public InstanceConditionalTest splitTest;

	public double[][] resultingClassDistributions;

	public double merit;

	public AttributeSplitSuggestion(InstanceConditionalTest splitTest,
			double[][] resultingClassDistributions, double merit) {
		this.splitTest = splitTest;
		this.resultingClassDistributions = resultingClassDistributions.clone();
		this.merit = merit;
	}

	public int numSplits() {
		return this.resultingClassDistributions.length;
	}

	public double[] resultingClassDistributionFromSplit(int splitIndex) {
		return this.resultingClassDistributions[splitIndex].clone();
	}

	public int compareTo(AttributeSplitSuggestion comp) {
		return Double.compare(this.merit, comp.merit);
	}

	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
