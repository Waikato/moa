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
package moa.classifiers.rules.multilabel.core;

import moa.AbstractMOAObject;
import moa.classifiers.rules.core.Predicate;
import moa.core.DoubleVector;

/**
 * Class for computing attribute split suggestions given a split test.
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 * @version $Revision: 1 $
 */
public class AttributeExpansionSuggestion extends AbstractMOAObject implements
        Comparable<AttributeExpansionSuggestion> {

    private static final long serialVersionUID = 1L;

    public Predicate predicate;

    public DoubleVector[][] resultingNodeStatistics;

    public double merit;

    public Predicate getPredicate() {
		return predicate;
	}
    
    public void setPredicate(Predicate predicate) {
    	this.predicate=predicate;
	}



	public DoubleVector[][] getResultingNodeStatistics() {
		return resultingNodeStatistics;
	}



	public double getMerit() {
		return merit;
	}



	public AttributeExpansionSuggestion(Predicate predicate,  DoubleVector[][] resultingNodeStatistics, double merit) {
        this.predicate = predicate;
        this.resultingNodeStatistics = resultingNodeStatistics;
        this.merit = merit;
    }





    @Override
    public int compareTo(AttributeExpansionSuggestion comp) {
        return Double.compare(this.merit, comp.merit);
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
    }
}
