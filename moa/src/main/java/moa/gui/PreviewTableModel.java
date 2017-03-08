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

import moa.evaluation.Preview;

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
	Class<?> currentTaskClass;
	
	public PreviewTableModel()
	{
		names = new ArrayList<>();
		data = new ArrayList<>();
		latestPreview = null;
		structureChangeFlag = false;
		currentTaskClass = null;
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
		return data.get(row)[column];
	}
	
	public void setPreview(Preview preview)
	{
		structureChangeFlag = false;
		if(latestPreview != null && preview == null)
		{
			names = new ArrayList<>();
			data = new ArrayList<>();
			fireTableStructureChanged();
			structureChangeFlag = true;
		}
		else if(latestPreview == null && preview != null || latestPreview != null && preview != null && preview.getTaskClass() != currentTaskClass)
		{
			names = new ArrayList<>();
			for(int measurementNameIdx = 0; measurementNameIdx < preview.getMeasurementNameCount(); ++measurementNameIdx)
			{
				names.add(preview.getMeasurementName(measurementNameIdx));
			}
			fireTableStructureChanged();
			structureChangeFlag = true;
		}
		
		latestPreview = preview;
		if(preview != null)
		{
			currentTaskClass = preview.getTaskClass();
			data = preview.getData();
			fireTableDataChanged();
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
}
