/*
 *    LearningCurveCollection.java
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

import java.util.ArrayList;
import java.util.List;

import moa.AbstractMOAObject;
import moa.core.StringUtils;

/**
 * Class that stores and keeps the history of multiple learning curves
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class LearningCurveCollection extends AbstractMOAObject {
	
	private static final long serialVersionUID = 1L;

	// the name representing the ordering
	String orderingName;
	// the name representing the index of each curve
	String indexName;
	// a list of all learning curves which should be 
	List<LearningCurve> subLearningCurves;
	// the measurement names a learning curve have to contain to be added in this collection
	List<String> requiredMmeasurementNames;
	// all measurement names used by this collection
	List<String> measurementNames;
	// the minimum of entries of all included learning curves
	int minEntryNum;
	
	public LearningCurveCollection(String orderingName, String indexName) {
		this.indexName = indexName;
		requiredMmeasurementNames = new ArrayList<>();
		measurementNames = new ArrayList<>();
		measurementNames.add(orderingName);
		measurementNames.add(indexName);
		subLearningCurves = new ArrayList<>();
	}
	
	public void setLearningCurve(int learningCurveIndex, LearningCurve learningCurve) throws IllegalArgumentException
	{
		// ignore the learning curve if it has no entries
		if(learningCurve.numEntries() > 0)
		{
			// copy the measurement names from the first learningcurve
			if(subLearningCurves.size() == 0)
			{
				for(int i = 0; i < learningCurve.getMeasurementNameCount(); ++i)
				{
					String name = learningCurve.getMeasurementName(i);
					measurementNames.add(name);
					requiredMmeasurementNames.add(name);
				}
			}
			
			// resize the array if new learning curves are added to the collection
			if(subLearningCurves.size() <= learningCurveIndex)
			{
				if(subLearningCurves.size() < learningCurveIndex)
				{
		            throw new IndexOutOfBoundsException("The given index (" + String.valueOf(learningCurveIndex) + ") is invalid.");				
				}
				else
				{
					subLearningCurves.add(null);
				}
			}
			// check if the measurement names are the same
			boolean hasSameMeasurementNamesCount = requiredMmeasurementNames.size() == learningCurve.getMeasurementNameCount();
			if(hasSameMeasurementNamesCount)
			{
				boolean hasSameMeasurementNames = true;
	
				for(int i = 0; i < requiredMmeasurementNames.size(); ++i)
				{
					hasSameMeasurementNames &= requiredMmeasurementNames.get(i).equals(learningCurve.getMeasurementName(i));
				}
				if(hasSameMeasurementNames)
				{
					// check if the number of entries of the new learning curve is consistent with the existing ones
					int newMinEntryNum = learningCurve.numEntries();
					int newMaxEntryNum = learningCurve.numEntries();
					
					for(int i = 0; i < subLearningCurves.size(); ++i)
					{
						if(i != learningCurveIndex)
						{
							int entryNum = subLearningCurves.get(i).numEntries();
							newMinEntryNum = Math.min(newMinEntryNum, entryNum);
							newMaxEntryNum = Math.max(newMaxEntryNum, entryNum);
						}
					}
					
					minEntryNum = newMinEntryNum;
				
					int numEntryDifference = newMaxEntryNum - newMaxEntryNum;
					
					if(numEntryDifference >= 0 && numEntryDifference <= 1)
					{
						// replace the curve
						subLearningCurves.set(learningCurveIndex, learningCurve);
					}
					else
					{
			            throw new IllegalArgumentException("The number of entries are invalid");
					}
				}
				else
				{
		            throw new IllegalArgumentException("The measurement names of all learning curves have to be equal");
				}
			}
			else
			{
	            throw new IllegalArgumentException("The number measurement names of all learning curves have to be equal");
			}
			
		}
	}
	
	public int numEntries()
	{
		// use the minimal number of entries to guarantee that all learning curves have enough values
		return minEntryNum * subLearningCurves.size();
	}

    public String headerToString() {
    	// append all measurement names separated by a comma
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String name : this.measurementNames) {
            if (!first) {
                sb.append(',');
            } else {
                first = false;
            }
            sb.append(name);
        }
        return sb.toString();
    }

    public String entryToString(int subLearningCurveIndex, int entryIndex)
    {
    	
        StringBuilder sb = new StringBuilder();
        // use the row index as ordering value
        int orderingValue = entryIndex * subLearningCurves.size() + subLearningCurveIndex;
        // append the ordering value
        sb.append(orderingValue);
        sb.append(",");
        // append the index of the learning curve to differentiate between those
        sb.append(subLearningCurveIndex);
        sb.append(",");
        // append the content of the entry from the wanted learning curve
        sb.append(subLearningCurves.get(subLearningCurveIndex).entryToString(entryIndex));
    	return sb.toString();
    }
    
    @Override
    public void getDescription(StringBuilder sb, int indent) {
        sb.append(headerToString());
        
        // iterate over all entries and learning curves to get an interlaced output
        for (int entryIdx = 0; entryIdx < minEntryNum; entryIdx++) {
        	for(int subLearningCurveIdx = 0; subLearningCurveIdx < subLearningCurves.size(); ++subLearningCurveIdx)
            StringUtils.appendNewlineIndented(sb, indent, entryToString(subLearningCurveIdx, entryIdx));
        }
    }
    
    public List<LearningCurve> getLearningCurves()
    {
    	return subLearningCurves;
    }
    
    public int getNumCurves()
    {
    	return subLearningCurves.size();
    }
    
    @Override
    public String toString() {
    	// TODO Auto-generated method stub
    	StringBuilder sb = new StringBuilder();
    	getDescription(sb, 0);
    	return sb.toString();
    }
}
