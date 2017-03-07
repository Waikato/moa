/*
 *    PreviewCollectionLearingCurveWrapper.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tuan Pham Minh (tuan.pham@ovgu.de)
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
package moa.evaluation;

import moa.AbstractMOAObject;

/**
 * Class used to wrap LearningCurve so that it can be used in 
 * conjunction with a PreviewCollection
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class PreviewCollectionLearingCurveWrapper extends AbstractMOAObject implements PreviewCollectionElement {

	private static final long serialVersionUID = 1L;
	
	// the learning curve which should be wrapped
	LearningCurve learningCurveToBeWrapped;
	
	public PreviewCollectionLearingCurveWrapper(LearningCurve learningCurveToBeWrapped)
	{
		this.learningCurveToBeWrapped = learningCurveToBeWrapped;	
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		learningCurveToBeWrapped.getDescription(sb, indent);
	}

	@Override
	public int getMeasurementNameCount() {
		return learningCurveToBeWrapped.getMeasurementNameCount();
	}

	@Override
	public String getMeasurementName(int measurementIndex) {
		return learningCurveToBeWrapped.getMeasurementName(measurementIndex);
	}

	@Override
	public int numEntries() {
		return learningCurveToBeWrapped.numEntries();
	}

	@Override
	public String entryToString(int entryIndex) {
		return learningCurveToBeWrapped.entryToString(entryIndex);
	}

	public LearningCurve getLearningCurve( ) {
		return learningCurveToBeWrapped;
	}

}
