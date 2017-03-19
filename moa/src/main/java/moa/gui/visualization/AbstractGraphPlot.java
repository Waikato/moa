/*
 *    AbstractGraphPlot.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */
package moa.gui.visualization;

import java.awt.Color;

import javax.swing.JPanel;

import moa.evaluation.MeasureCollection;

/**
 * AbstractGraphPlot is an abstract class defining the structure of a Plot
 * class.
 * 
 * This class is partially based on GraphCurve.
 * 
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCurve
 */
public abstract class AbstractGraphPlot extends JPanel{
	
	private static final long serialVersionUID = 1L;
	
    protected double max_value;
    protected double upper_value;
    protected MeasureCollection[] measures;
    protected int measureSelected = 0;

    protected double x_resolution;
    
    protected Color[] colors;

	public AbstractGraphPlot() {
		this.max_value = 1;
		this.upper_value = 1;
    	this.measureSelected = 0;
    	
    	setOpaque(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1000, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
	}
	
	/**
	 * Sets the graph by updating the measures and currently measure index.
	 * This method should not be directly called, but may be used by subclasses
	 * to save space.
	 * @param measures measure information
	 * @param mSelect index of the currently selected measure
	 */
	protected void setGraph(MeasureCollection[] measures, int mSelect, Color[] colors) {
		this.measures = measures;
	    this.measureSelected = mSelect;
	    this.colors = colors;
	    repaint();
	}
	
    /**
     * Sets minimum and maximum y value.
     * @param min minimum y value
     * @param max maximum y value
     */
    protected void setYMaxValue(double max){
        this.max_value = max;
    }
    
    protected void setYUpperValue(double value) {
    	this.upper_value = value;
    }

    /**
     * Sets the resolution on the x-axis
     * @param x_resolution resolution on the x-axis
     */
    protected void setXResolution(double x_resolution) {
        this.x_resolution = x_resolution;
    }

}
