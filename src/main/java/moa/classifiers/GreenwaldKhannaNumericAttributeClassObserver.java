/*
 *    GreenwaldKhannaNumericAttributeClassObserver.java
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
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.GreenwaldKhannaQuantileSummary;
import weka.core.Instance;

public class GreenwaldKhannaNumericAttributeClassObserver extends
		AbstractMOAObject implements AttributeClassObserver {

	private static final long serialVersionUID = 1L;

	protected int numTuples;

	protected AutoExpandVector<GreenwaldKhannaQuantileSummary> attValDistPerClass = new AutoExpandVector<GreenwaldKhannaQuantileSummary>();

	public GreenwaldKhannaNumericAttributeClassObserver(int numTuples) {
		this.numTuples = numTuples;
	}

	public void observeAttributeClass(double attVal, int classVal, double weight) {
		if (Instance.isMissingValue(attVal)) {

		} else {
			GreenwaldKhannaQuantileSummary valDist = this.attValDistPerClass
					.get(classVal);
			if (valDist == null) {
				valDist = new GreenwaldKhannaQuantileSummary(this.numTuples);
				this.attValDistPerClass.set(classVal, valDist);
			}
			// TODO: not taking weight into account
			valDist.insert(attVal);
		}
	}

	public double probabilityOfAttributeValueGivenClass(double attVal,
			int classVal) {
		// TODO: NaiveBayes broken until implemented
		return 0.0;
	}

	public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(
			SplitCriterion criterion, double[] preSplitDist, int attIndex,
			boolean binaryOnly) {
		AttributeSplitSuggestion bestSuggestion = null;
		for (GreenwaldKhannaQuantileSummary qs : this.attValDistPerClass) {
			if (qs != null) {
				double[] cutpoints = qs.getSuggestedCutpoints();
				for (double cutpoint : cutpoints) {
					double[][] postSplitDists = getClassDistsResultingFromBinarySplit(cutpoint);
					double merit = criterion.getMeritOfSplit(preSplitDist,
							postSplitDists);
					if ((bestSuggestion == null)
							|| (merit > bestSuggestion.merit)) {
						bestSuggestion = new AttributeSplitSuggestion(
								new NumericAttributeBinaryTest(attIndex,
										cutpoint, true), postSplitDists, merit);
					}
				}
			}
		}
		return bestSuggestion;
	}

	// assume all values equal to splitValue go to lhs
	public double[][] getClassDistsResultingFromBinarySplit(double splitValue) {
		DoubleVector lhsDist = new DoubleVector();
		DoubleVector rhsDist = new DoubleVector();
		for (int i = 0; i < this.attValDistPerClass.size(); i++) {
			GreenwaldKhannaQuantileSummary estimator = this.attValDistPerClass
					.get(i);
			if (estimator != null) {
				long countBelow = estimator.getCountBelow(splitValue);
				lhsDist.addToValue(i, countBelow);
				rhsDist.addToValue(i, estimator.getTotalCount() - countBelow);
			}
		}
		return new double[][] { lhsDist.getArrayRef(), rhsDist.getArrayRef() };
	}

	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
