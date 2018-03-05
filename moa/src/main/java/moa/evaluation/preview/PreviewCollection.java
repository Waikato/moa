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
package moa.evaluation.preview;

import java.util.ArrayList;
import java.util.List;

import moa.core.StringUtils;

/**
 * Class that stores and keeps the history of multiple previews
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class PreviewCollection<CollectionElementType extends Preview> extends Preview {

	private static final long serialVersionUID = 1L;

	// the name representing the ordering
	String orderingName;
	// the name representing the index of each preview
	String indexName;
	// a list of all previews which should be stored
	List<CollectionElementType> subPreviews;
	// the measurement names a preview has to contain to be added in this
	// collection
	List<String> requiredMeasurementNames;
	// all measurement names used by this collection
	List<String> measurementNames;
	// the minimum of entries of all included previews
	int minEntryNum;
	// the type of Task which uses this preview collection
	Class<?> taskClass;
	// name of the varied parameter (can be null)
	String variedParamName;
	// values for the varied parameter (can be null)
	double[] variedParamValues;
	
	public PreviewCollection(String orderingName, String indexName, Class<?> taskClass, 
			String variedParamName, double[] variedParamValues) 
	{
		this.orderingName = orderingName;
		this.indexName = indexName;
		requiredMeasurementNames = new ArrayList<>();
		measurementNames = new ArrayList<>();
		measurementNames.add(orderingName);
		measurementNames.add(indexName);
		subPreviews = new ArrayList<>();
		this.taskClass = taskClass;
		this.variedParamName = variedParamName;
		this.variedParamValues = variedParamValues;
	}
	
	public PreviewCollection(String orderingName, String indexName, Class<?> taskClass) {
		this(orderingName, indexName, taskClass, null, null);
	}

	public void setPreview(int previewIndex, CollectionElementType preview) throws IllegalArgumentException {
		// ignore the preview if it has no entries
		if (preview.numEntries() > 0) {
			// copy the measurement names from the first preview

			requiredMeasurementNames = new ArrayList<>();
			measurementNames = new ArrayList<>();
			measurementNames.add(orderingName);
			measurementNames.add(indexName);
			
			for (int i = 0; i < preview.getMeasurementNameCount(); ++i) {
				String name = preview.getMeasurementName(i);
				measurementNames.add(name);
				requiredMeasurementNames.add(name);
			}

			// resize the array if new previews are added to the collection
			if (subPreviews.size() <= previewIndex) {
				if (subPreviews.size() < previewIndex) {
					throw new IndexOutOfBoundsException(
							"The given index (" + String.valueOf(previewIndex) + ") is invalid.");
				} else {
					subPreviews.add(null);
				}
			}

			// check if the new preview has more entries than the last one
			if (subPreviews.get(previewIndex) == null ||
				(subPreviews.get(previewIndex) != null && 
				 preview.numEntries() > subPreviews.get(previewIndex).numEntries())) 
			{
				// set the smallest number of entries
				minEntryNum = preview.numEntries();
				for (int i = 0; i < subPreviews.size(); ++i) {
					if (i != previewIndex) {
						int entryNum = subPreviews.get(i).numEntries();
						minEntryNum = Math.min(minEntryNum, entryNum);
					}
				}
				// set the preview
				subPreviews.set(previewIndex, preview);
			}
		}
	}

	public int numEntries() {
		// use the minimal number of entries to guarantee that all previews have
		// enough values
		return minEntryNum * subPreviews.size();
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
		if (subPreviews.size() > 0) {
			return entryToString(entryIndex % subPreviews.size(), entryIndex / subPreviews.size());
		} else {
			return "";
		}
	}

	public String entryToString(int subPreviewIndex, int interlacedEntryIndex) {
		StringBuilder sb = new StringBuilder();
		// use the row index as ordering value
		int orderingValue = interlacedEntryIndex * subPreviews.size() + subPreviewIndex;
		// append the ordering value
		sb.append(orderingValue);
		sb.append(",");
		// append the index of the preview to differentiate between those
		sb.append(subPreviewIndex);
		sb.append(",");
		// append the content of the entry from the wanted preview
		sb.append(subPreviews.get(subPreviewIndex).entryToString(interlacedEntryIndex));
		return sb.toString();
	}

	public void getDescription(StringBuilder sb, int indent) {
		sb.append(headerToString());

		// iterate over all entries and previews to get an interlaced output
		for (int entryIdx = 0; entryIdx < numEntries(); entryIdx++) {
			StringUtils.appendNewlineIndented(sb, indent, entryToString(entryIdx));
		}
	}

	public List<CollectionElementType> getPreviews() {
		return subPreviews;
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

	@Override
	public Class<?> getTaskClass() {
		return taskClass;
	}

	@Override
	public double[] getEntryData(int entryIndex) {
		// preallocate the array for the entry data
		double[] entry = new double[getMeasurementNameCount()];
		// get the number of previews in this collection to reduce the number of
		// calls for .size()
		int numSubPreviews = subPreviews.size();
		// calculate the index of the corresponding preview and the entry index
		// for that one
		int subPreviewIndex = entryIndex % numSubPreviews;
		int subPreviewEntryIndex = entryIndex / numSubPreviews;
		// get the entry of the preview
		double[] subPreviewEntry = subPreviews.get(subPreviewIndex).getEntryData(subPreviewEntryIndex);
		// fill the first two elements with the entry index and the index of the
		// preview
		entry[0] = entryIndex;
		entry[1] = subPreviewIndex;
		// fill the rest with the entry data of the preview
		for (int measurementIdx = 0; measurementIdx < subPreviewEntry.length; ++measurementIdx) {
			entry[2 + measurementIdx] = subPreviewEntry[measurementIdx];
		}

		return entry;
	}
	
	public String getOrderingName() {
		return this.orderingName;
	}
	
	public String getIndexName() {
		return this.indexName;
	}
	
	public String getVariedParamName() {
		return this.variedParamName;
	}
	
	public double[] getVariedParamValues() {
		return this.variedParamValues;
	}
}
