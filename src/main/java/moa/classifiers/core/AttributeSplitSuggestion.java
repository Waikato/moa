/*
 *    AttributeSplitSuggestion.java
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
package moa.classifiers.core;

import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.AbstractMOAObject;

/**
 * Class for computing attribute split suggestions given a split test.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
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

    @Override
    public int compareTo(AttributeSplitSuggestion comp) {
        return Double.compare(this.merit, comp.merit);
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
