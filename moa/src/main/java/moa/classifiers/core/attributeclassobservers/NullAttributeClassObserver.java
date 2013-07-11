/*
 *    NullAttributeClassObserver.java
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
package moa.classifiers.core.attributeclassobservers;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

/**
 * Class for observing the class data distribution for a null attribute.
 * This method is used to disable the observation for an attribute.
 * Used in decision trees to monitor data statistics on leaves.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class NullAttributeClassObserver extends AbstractOptionHandler implements
        AttributeClassObserver {

    private static final long serialVersionUID = 1L;

    @Override
    public void observeAttributeClass(double attVal, int classVal, double weight) {
    }

    @Override
    public double probabilityOfAttributeValueGivenClass(double attVal,
            int classVal) {
        return 0.0;
    }

    public double totalWeightOfClassObservations() {
        return 0.0;
    }

    public double weightOfObservedMissingValues() {
        return 0.0;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(
            SplitCriterion criterion, double[] preSplitDist, int attIndex,
            boolean binaryOnly) {
        return null;
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }

    @Override
    public void observeAttributeTarget(double attVal, double target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
