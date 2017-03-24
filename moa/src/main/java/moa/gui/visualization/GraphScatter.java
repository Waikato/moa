/*
 *    GraphScatter.java
 *    Copyright (C) Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de)
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
package moa.gui.visualization;

import java.awt.Color;
import java.awt.Graphics;

import moa.evaluation.MeasureCollection;

/**
 * GraphScatter is an implementation of AbstractGraphPlot that draws a scatter
 * plot.
 * 
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see AbstractGraphPlot
 */
public class GraphScatter extends AbstractGraphPlot {

	private static final long serialVersionUID = 1L;
    
    private static final int DOT_SIZE = 6;
    
    private double[] variedParamValues;
	
    /**
     * Draws a scatter graph based on the varied parameter and the measures.
     * @param measures  list of measure collections, one for each task
     * @param mSelect currently selected measure
     * @param variedParamName name of the varied parameter
     * @param variedParamValues values of the varied parameter
     */
    public void setGraph(MeasureCollection[] measures, int mSelect, 
    		double[] variedParamValues, Color[] colors) {
        this.variedParamValues = variedParamValues;
        super.setGraph(measures, mSelect, colors);
    }

    @Override
    protected void paintComponent(Graphics g) {
    	super.paintComponent(g);
    	
        if (this.measures == null || this.variedParamValues == null) { 
        	// no measures received yet -> nothing to paint
        	return; 
        }
        
        // scatter current budgets
        for (int i = 0; i < this.measures.length; i++) {
        	this.scatter(g, this.measures[i], this.variedParamValues[i], this.colors[i]);
        }
    }
    
    /**
     * Paint a dot onto the panel.
     * @param g graphics object
     * @param m MeasureCollection containing the data
     */
    private void scatter(Graphics g, MeasureCollection m, double variedParamValue, Color color) {

    	int height = getHeight();
    	int width = getWidth();
    	
    	int x = (int)((variedParamValue / this.upper_x_value) * width); 
   		double value = m.getLastValue(this.measureSelected);  
        if(Double.isNaN(value)){
        	// no result for this budget yet
            return;
        }

        int y = (int)(height - (value / this.upper_y_value) * height); 
        
        g.setColor(color);
    	g.fillOval(x - DOT_SIZE/2, y - DOT_SIZE/2, DOT_SIZE, DOT_SIZE);
    }

}
 