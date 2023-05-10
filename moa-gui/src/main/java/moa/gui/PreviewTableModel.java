/*
 *    PreviewTableModel.java
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
package moa.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import moa.evaluation.preview.Preview;

/**
 * Class to display the latest preview in a table
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class PreviewTableModel extends AbstractTableModel {
	// TODO add implementation
	private static final long serialVersionUID = 1L;
	
	List<String> names;
	List<double[]> data;
	Preview latestPreview;
	boolean structureChangeFlag;
	
	public PreviewTableModel()
	{
		names = new ArrayList<>();
		data = new ArrayList<>();
		latestPreview = null;
		structureChangeFlag = false;
	}
	
	@Override
	public String getColumnName(int column) {
		return names.get(column);
	}
	
	@Override
	public int getColumnCount() {
		return names.size();
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public Object getValueAt(int row, int column) {
		if(row >= data.size() || column >= data.get(row).length)
		{
			return 0.0;
		}
		return data.get(row)[column];
	}
	
	public void setPreview(Preview preview)
	{
		structureChangeFlag = false;
		
		if(preview == null)
		{
			if(latestPreview != null)
			{
				names = new ArrayList<>();
				data = new ArrayList<>();
				structureChangeFlag = true;
			}
		}
		else
		{
			structureChangeFlag |= latestPreview == null;
			structureChangeFlag |= latestPreview != null && latestPreview.numEntries() == 0 && preview.numEntries() > 0;
			structureChangeFlag |= latestPreview != null && latestPreview.getTaskClass() != preview.getTaskClass();
		}
		latestPreview = preview;
		if(preview != null)
		{
			data = preview.getData();
			
			if(structureChangeFlag)
			{
				copyMeasurementNames(preview);
			}
		}
	}
	
	@Override
	public String toString() {
		return latestPreview == null? "" : latestPreview.toString();
	}
	
	public boolean structureChanged()
	{
		return structureChangeFlag;
	}
	
	private void copyMeasurementNames(Preview preview)
	{
		names = new ArrayList<>();
		int newMeasurementNameCount = preview.getMeasurementNameCount();
		for(int measurementNameIdx = 0; measurementNameIdx < newMeasurementNameCount; ++measurementNameIdx)
		{
			names.add(preview.getMeasurementName(measurementNameIdx));
		}
	}
}
