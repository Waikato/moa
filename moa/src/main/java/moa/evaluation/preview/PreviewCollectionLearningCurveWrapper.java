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
package moa.evaluation.preview;

/**
 * Class used to wrap LearningCurve so that it can be used in 
 * conjunction with a PreviewCollection
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class PreviewCollectionLearningCurveWrapper extends Preview {

	private static final long serialVersionUID = 1L;
	
	// the learning curve which should be wrapped
	LearningCurve learningCurveToBeWrapped;
	Class<?> taskClass;
	
	public PreviewCollectionLearningCurveWrapper(LearningCurve learningCurveToBeWrapped, Class<?> taskClass)
	{
		this.learningCurveToBeWrapped = learningCurveToBeWrapped;	
		this.taskClass = taskClass;
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

	@Override
	public Class<?> getTaskClass() {
		return taskClass;
	}

	@Override
	public double[] getEntryData(int entryIndex) {
		// get the number of measurements
		int numMeasurements = getMeasurementNameCount();

		int numEntryMeasurements = learningCurveToBeWrapped.getEntryMeasurementCount(entryIndex);
		// preallocate the array to store all measurements
		double[] data = new double[numMeasurements];
		// get measuements from the learning curve
		for(int measurementIdx = 0; measurementIdx < numMeasurements; ++measurementIdx)
		{
			if(measurementIdx < numEntryMeasurements)
			{
				data[measurementIdx] = learningCurveToBeWrapped.getMeasurement(entryIndex, measurementIdx);	
			}
			else
			{
				data[measurementIdx] = Double.NaN;
			}
		}
		return data;
	}
}
