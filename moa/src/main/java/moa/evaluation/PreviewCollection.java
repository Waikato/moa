/*
 *    PreviewCollection.java
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
 * Class that stores and keeps the history of multiple previews
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class PreviewCollection <CollectionElementType extends PreviewCollectionElement> extends AbstractMOAObject implements PreviewCollectionElement{
	
	private static final long serialVersionUID = 1L;

	// the name representing the ordering
	String orderingName;
	// the name representing the index of each preview
	String indexName;
	// a list of all previews which should be stored
	List<CollectionElementType> subPreview;
	// the measurement names a preview has to contain to be added in this collection
	List<String> requiredMmeasurementNames;
	// all measurement names used by this collection
	List<String> measurementNames;
	// the minimum of entries of all included previews
	int minEntryNum;
	// the type of Task which uses this preview collection
	Class<?> taskClass;
	
	public PreviewCollection(String orderingName, String indexName, Class<?> taskClass) {
		this.indexName = indexName;
		requiredMmeasurementNames = new ArrayList<>();
		measurementNames = new ArrayList<>();
		measurementNames.add(orderingName);
		measurementNames.add(indexName);
		subPreview = new ArrayList<>();
		this.taskClass = taskClass;
	}
	
	public void setPreview(int previewIndex, CollectionElementType preview) throws IllegalArgumentException
	{
		// ignore the preview if it has no entries
		if(preview.numEntries() > 0)
		{
			// copy the measurement names from the first preview
			if(subPreview.size() == 0)
			{
				for(int i = 0; i < preview.getMeasurementNameCount(); ++i)
				{
					String name = preview.getMeasurementName(i);
					measurementNames.add(name);
					requiredMmeasurementNames.add(name);
				}
			}
			
			// resize the array if new previews are added to the collection
			if(subPreview.size() <= previewIndex)
			{
				if(subPreview.size() < previewIndex)
				{
		            throw new IndexOutOfBoundsException("The given index (" + String.valueOf(previewIndex) + ") is invalid.");				
				}
				else
				{
					subPreview.add(null);
				}
			}
			// check if the measurement names are the same
			boolean hasSameMeasurementNamesCount = requiredMmeasurementNames.size() == preview.getMeasurementNameCount();
			if(hasSameMeasurementNamesCount)
			{
				boolean hasSameMeasurementNames = true;
	
				for(int i = 0; i < requiredMmeasurementNames.size(); ++i)
				{
					hasSameMeasurementNames &= requiredMmeasurementNames.get(i).equals(preview.getMeasurementName(i));
				}
				if(hasSameMeasurementNames)
				{
					// check if the number of entries of the new preview is consistent with the existing ones
					int newMinEntryNum = preview.numEntries();
					int newMaxEntryNum = preview.numEntries();
					
					for(int i = 0; i < subPreview.size(); ++i)
					{
						if(i != previewIndex)
						{
							int entryNum = subPreview.get(i).numEntries();
							newMinEntryNum = Math.min(newMinEntryNum, entryNum);
							newMaxEntryNum = Math.max(newMaxEntryNum, entryNum);
						}
					}
					
					minEntryNum = newMinEntryNum;
				
					int numEntryDifference = newMaxEntryNum - newMaxEntryNum;
					
					if(numEntryDifference >= 0 && numEntryDifference <= 1)
					{
						// replace the preview
						subPreview.set(previewIndex, preview);
					}
					else
					{
			            throw new IllegalArgumentException("The number of entries are invalid");
					}
				}
				else
				{
		            throw new IllegalArgumentException("The measurement names of all previews have to be equal");
				}
			}
			else
			{
	            throw new IllegalArgumentException("The number measurement names of all previews have to be equal");
			}
			
		}
	}
	
	public int numEntries()
	{
		// use the minimal number of entries to guarantee that all previews have enough values
		return minEntryNum * subPreview.size();
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

	@Override
	public String entryToString(int entryIndex) {
		if(subPreview.size() > 0)
		{
			return entryToString(entryIndex%subPreview.size(), entryIndex/subPreview.size());
		}
		else
		{
			return "";
		}
	}

    public String entryToString(int subPreviewIndex, int interlacedEntryIndex)
    {
        StringBuilder sb = new StringBuilder();
        // use the row index as ordering value
        int orderingValue = interlacedEntryIndex * subPreview.size() + subPreviewIndex;
        // append the ordering value
        sb.append(orderingValue);
        sb.append(",");
        // append the index of the preview to differentiate between those
        sb.append(subPreviewIndex);
        sb.append(",");
        // append the content of the entry from the wanted preview
        sb.append(subPreview.get(subPreviewIndex).entryToString(interlacedEntryIndex));
    	return sb.toString();
    }
    
    public void getDescription(StringBuilder sb, int indent) {
        sb.append(headerToString());
        
        // iterate over all entries and previews to get an interlaced output
        for (int entryIdx = 0; entryIdx < numEntries(); entryIdx++) {
            StringUtils.appendNewlineIndented(sb, indent, entryToString(entryIdx));
        }
    }
    
    public List<CollectionElementType> getPreview()
    {
    	return subPreview;
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	getDescription(sb, 0);
    	return sb.toString();
    }

	@Override
	public int getMeasurementNameCount() {
		return measurementNames.size();
	}

	@Override
	public String getMeasurementName(int measurementIndex) {
		return measurementNames.get(measurementIndex);
	}

	public Class<?> getTaskClass() {
		return taskClass;
	}
}
